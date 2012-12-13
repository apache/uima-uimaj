/* 
   Copyright 2010 Regents of the University of Colorado.  
 All rights reserved. 

 Licensed under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License. 
 You may obtain a copy of the License at 

 http://www.apache.org/licenses/LICENSE-2.0 

 Unless required by applicable law or agreed to in writing, software 
 distributed under the License is distributed on an "AS IS" BASIS, 
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 See the License for the specific language governing permissions and 
 limitations under the License.
 */
package org.uimafit.component.xwriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.uimafit.ComponentTestBase;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.factory.testAes.Annotator1;
import org.uimafit.factory.testAes.Annotator2;
import org.uimafit.factory.testAes.Annotator3;
import org.uimafit.factory.testAes.ViewNames;

/**
 * @author Philip Ogren
 */
public class XWriterTest extends ComponentTestBase {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File outputDirectory;

	@Before
	public void setup() {
		outputDirectory = folder.newFolder("test/xmi-output");
	}

	@Test
	public void testXWriter() throws Exception {
		addDataToCas();

		AnalysisEngine xWriter = AnalysisEngineFactory.createPrimitive(XWriter.class,
				typeSystemDescription, XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
				outputDirectory.getPath());

		xWriter.process(jCas);

		File xmiFile = new File(outputDirectory, "1.xmi");
		assertTrue(xmiFile.exists());

		jCas.reset();
		JCasFactory.loadJCas(jCas, xmiFile.getPath());
		assertEquals("Anyone up for a game of Foosball?", jCas.getDocumentText());
		assertEquals("Any(o)n(e) (u)p f(o)r (a) g(a)m(e) (o)f F(oo)sb(a)ll?", jCas.getView("A")
				.getDocumentText());
		assertEquals("?AFaaabeeffgllmnnoooooprsuy", jCas.getView("B").getDocumentText());
		assertEquals("(((((((((())))))))))?AFaaabeeffgllmnnoooooprsuy", jCas.getView("C")
				.getDocumentText());
		assertEquals("yusrpooooonnmllgffeebaaaFA?", jCas.getView(ViewNames.REVERSE_VIEW)
				.getDocumentText());

		jCas.reset();
		addDataToCas();

		xWriter = AnalysisEngineFactory.createPrimitive(XWriter.class, typeSystemDescription,
				XWriter.PARAM_OUTPUT_DIRECTORY_NAME, outputDirectory.getPath(),
				IntegerFileNamer.PARAM_PREFIX, "myprefix-");

		xWriter.process(jCas);

		xmiFile = new File(outputDirectory, "myprefix-1.xmi");
		assertTrue(xmiFile.exists());

	}

	private void addDataToCas() throws UIMAException {
		tokenBuilder.buildTokens(jCas, "Anyone up for a game of Foosball?");

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription), ViewNames.PARENTHESES_VIEW, "A");
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator2.class,
				typeSystemDescription), ViewNames.SORTED_VIEW, "B",
				ViewNames.SORTED_PARENTHESES_VIEW, "C", ViewNames.PARENTHESES_VIEW, "A");
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator3.class,
				typeSystemDescription), ViewNames.INITIAL_VIEW, "B");
		AnalysisEngine aggregateEngine = builder.createAggregate();

		aggregateEngine.process(jCas);
	}

	@Test
	public void testXmi() throws Exception {
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(XWriter.class,
				typeSystemDescription, XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
				this.outputDirectory.getPath());
		tokenBuilder.buildTokens(jCas, "I like\nspam!", "I like spam !", "PRP VB NN .");
		engine.process(jCas);
		engine.collectionProcessComplete();

		File outputFile = new File(this.outputDirectory, "1.xmi");

		SAXBuilder builder = new SAXBuilder();
		builder.setDTDHandler(null);
		Element root = null;
		try {
			Document doc = builder.build(new StringReader(FileUtils.file2String(outputFile)));
			root = doc.getRootElement();
		}
		catch (JDOMException e) {
			throw new AnalysisEngineProcessException(e);
		}
		catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}

		List<?> elements = root.getChildren("Sentence", root.getNamespace("type"));
		Assert.assertEquals(1, elements.size());
		elements = root.getChildren("Token", root.getNamespace("type"));
		Assert.assertEquals(4, elements.size());

	}

	@Test
	public void testXcas() throws Exception {
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(XWriter.class,
				typeSystemDescription, XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
				this.outputDirectory.getPath(), XWriter.PARAM_XML_SCHEME_NAME, XWriter.XCAS);
		tokenBuilder.buildTokens(jCas, "I like\nspam!", "I like spam !", "PRP VB NN .");
		engine.process(jCas);
		engine.collectionProcessComplete();

		File outputFile = new File(this.outputDirectory, "1.xcas");

		SAXBuilder builder = new SAXBuilder();
		builder.setDTDHandler(null);
		Element root = null;
		try {
			Document doc = builder.build(new StringReader(FileUtils.file2String(outputFile)));
			root = doc.getRootElement();
		}
		catch (JDOMException e) {
			throw new AnalysisEngineProcessException(e);
		}
		catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}

		List<?> elements = root.getChildren("org.uimafit.type.Sentence");
		Assert.assertEquals(1, elements.size());
		elements = root.getChildren("org.uimafit.type.Token");
		Assert.assertEquals(4, elements.size());

	}

	@Test(expected = ResourceInitializationException.class)
	public void testBadXmlSchemeName() throws ResourceInitializationException {
		AnalysisEngineFactory.createPrimitive(XWriter.class, typeSystemDescription,
				XWriter.PARAM_XML_SCHEME_NAME, "xcas");
	}

}
