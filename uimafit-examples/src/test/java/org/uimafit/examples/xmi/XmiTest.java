/* 
 Copyright 2009-2010	Regents of the University of Colorado.  
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
package org.uimafit.examples.xmi;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.component.xwriter.XWriter;
import org.uimafit.examples.tutorial.ExamplesTestBase;
import org.uimafit.examples.type.Sentence;
import org.uimafit.examples.type.Token;

/**
 * This test demonstrates testing a "downstream" AnalysisEngine by means of an
 * XMI-serialized CAS. Here we have two "upstream" analysis engines, Annotator1
 * and Annotator2, which add annotations used by Annotator3.
 * 
 * @author Philip
 * 
 */
public class XmiTest extends ExamplesTestBase {

	/**
	 * Here we are testing Annotator3 by setting up the "pipeline" and running
	 * it before testing the final annotator.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWithoutXmi() throws Exception {
		AnalysisEngine a1 = AnalysisEngineFactory.createPrimitive(Annotator1.class, typeSystemDescription);
		AnalysisEngine a2 = AnalysisEngineFactory.createPrimitive(Annotator2.class, typeSystemDescription);
		AnalysisEngine a3 = AnalysisEngineFactory.createPrimitive(Annotator3.class, typeSystemDescription);
		jCas.setDocumentText("betgetjetletmetnetpetsetvetwetyet");
		SimplePipeline.runPipeline(jCas, a1, a2, a3);

		Sentence sentence = JCasUtil.selectByIndex(jCas, Sentence.class, 0);
		assertEquals("metnetpetsetvetwetyet", sentence.getCoveredText());
	}

	/**
	 * In this test we have removed the dependency on running Annotator1 and
	 * Annotator2 before running Annotator3 by introducing an XMI file that
	 * contains the token annnotations created by Annotator1 and the pos tags
	 * added by Annotator2. This is nice because both Annotator1 and Annotator2
	 * do a pretty poor job at their tasks and you can imagine that in future
	 * versions their behavior might change. However, Annotator3 does a
	 * perfectly fine job doing what it does and tests for this analysis engine
	 * should not have to change just because the behavior of Annotator1 and
	 * Annotator2 will. Another option is to set up all the annotations required
	 * by Annotator3 manually, but this approach can be tedious, time consuming,
	 * error prone, and results in a lot of code.
	 * <p>
	 * The xmi file is generated once by running {@link #main(String[])}.
	 * Hopefully, it will not be necessary to regenerate the xmi file often.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWithXmi() throws Exception {
		jCas = JCasFactory.createJCas("src/main/resources/org/uimafit/examples/xmi/1.xmi", typeSystemDescription);
		AnalysisEngine a3 = AnalysisEngineFactory.createPrimitive(Annotator3.class, typeSystemDescription);
		a3.process(jCas);
		Sentence sentence = JCasUtil.selectByIndex(jCas, Sentence.class, 0);
		assertEquals("metnetpetsetvetwetyet", sentence.getCoveredText());
	}

	/**
	 * Here we generate an xmi file that will be used by {@link #testWithXmi()}.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		TypeSystemDescription tsd = createTypeSystemDescription("org.uimafit.examples.TypeSystem");
		AnalysisEngine a1 = AnalysisEngineFactory.createPrimitive(Annotator1.class, tsd);
		AnalysisEngine a2 = AnalysisEngineFactory.createPrimitive(Annotator2.class, tsd);
		AnalysisEngine xWriter = AnalysisEngineFactory.createPrimitive(XWriter.class, tsd, XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
				"src/main/resources/org/uimafit/examples/xmi");
		JCas jCas = JCasFactory.createJCas(tsd);
		jCas.setDocumentText("betgetjetletmetnetpetsetvetwetyet");
		a1.process(jCas);
		a2.process(jCas);
		xWriter.process(jCas);
		xWriter.collectionProcessComplete();
	}

	/**
	 * Creates a token for every three characters
	 */
	public static class Annotator1 extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			String text = jCas.getDocumentText();
			for (int i = 0; i < text.length() - 3; i += 3) {
				new Token(jCas, i, i + 3).addToIndexes();
			}
		}
	}

	/**
	 * sets the pos tag for each token to be the first letter of the token
	 */
	public static class Annotator2 extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Token token : select(jCas, Token.class)) {
				token.setPos(token.getCoveredText().substring(0, 1));
			}
		}
	}

	/**
	 * creates a sentence from the begining of each token whose pos tag is "m"
	 * to the end of the text.
	 */
	public static class Annotator3 extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Token token : select(jCas, Token.class)) {
				if (token.getPos().equals("m")) {
					new Sentence(jCas, token.getBegin(), jCas.getDocumentText().length()).addToIndexes();
				}
			}
		}
	}

}
