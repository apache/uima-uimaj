/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.fit.component.xwriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.testAes.Annotator1;
import org.apache.uima.fit.factory.testAes.Annotator2;
import org.apache.uima.fit.factory.testAes.Annotator3;
import org.apache.uima.fit.factory.testAes.ViewNames;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
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
            typeSystemDescription, XWriter.PARAM_OUTPUT_DIRECTORY_NAME, outputDirectory.getPath());

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
            typeSystemDescription), ViewNames.SORTED_VIEW, "B", ViewNames.SORTED_PARENTHESES_VIEW,
            "C", ViewNames.PARENTHESES_VIEW, "A");
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

    CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
    InputStream is = null;
    try {
      is = new FileInputStream(new File(this.outputDirectory, "1.xmi"));
      XmiCasDeserializer.deserialize(is, cas);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
    
    Assert.assertEquals(1, JCasUtil.select(cas.getJCas(), Sentence.class).size());
    Assert.assertEquals(4, JCasUtil.select(cas.getJCas(), Token.class).size());
  }

  @Test
  public void testXcas() throws Exception {
    AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(XWriter.class,
            typeSystemDescription, XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
            this.outputDirectory.getPath(), XWriter.PARAM_XML_SCHEME_NAME, XWriter.XCAS);
    tokenBuilder.buildTokens(jCas, "I like\nspam!", "I like spam !", "PRP VB NN .");
    engine.process(jCas);
    engine.collectionProcessComplete();

    CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
    InputStream is = null;
    try {
      is = new FileInputStream(new File(this.outputDirectory, "1.xcas"));
      XCASDeserializer.deserialize(is, cas);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
    
    Assert.assertEquals(1, JCasUtil.select(cas.getJCas(), Sentence.class).size());
    Assert.assertEquals(4, JCasUtil.select(cas.getJCas(), Token.class).size());
  }

  @Test(expected = ResourceInitializationException.class)
  public void testBadXmlSchemeName() throws ResourceInitializationException {
    AnalysisEngineFactory.createPrimitive(XWriter.class, typeSystemDescription,
            XWriter.PARAM_XML_SCHEME_NAME, "xcas");
  }

}
