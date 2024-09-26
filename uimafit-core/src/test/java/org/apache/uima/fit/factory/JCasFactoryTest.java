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
package org.apache.uima.fit.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.jupiter.api.Test;

/**
 */

public class JCasFactoryTest extends ComponentTestBase {

  @Test
  public void testXMI() throws IOException {
    CasIOUtil.readJCas(jCas, new File("src/test/resources/data/docs/test.xmi"));
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
  }

  @Test
  public void testXCAS() throws IOException {
    CasIOUtil.readJCas(jCas, new File("src/test/resources/data/docs/test.xcas"));
    assertEquals(
            "... the more knowledge advances the more it becomes possible to condense it into little books.",
            jCas.getDocumentText());
  }

  @Test
  public void testFromPath() throws UIMAException {
    jCas = JCasFactory.createJCasFromPath(
            "src/test/resources/org/apache/uima/fit/type/AnalyzedText.xml",
            "src/test/resources/org/apache/uima/fit/type/Sentence.xml",
            "src/test/resources/org/apache/uima/fit/type/Token.xml");
    jCas.setDocumentText("For great 20 minute talks, check out TED.com.");
    AnnotationFactory.createAnnotation(jCas, 0, 3, Token.class);
    assertEquals("For", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
  }

  @Test
  public void testCreate() throws UIMAException {
    jCas = JCasFactory.createJCas();
    jCas.setDocumentText("For great 20 minute talks, check out TED.com.");
    AnnotationFactory.createAnnotation(jCas, 0, 3, Token.class);
    assertEquals("For", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
  }

  @Test
  public void testCreateText() throws UIMAException {
    jCas = JCasFactory.createText("For great 20 minute talks, check out TED.com.", "en");
    AnnotationFactory.createAnnotation(jCas, 0, 3, Token.class);
    assertEquals("For", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
    assertEquals("en", jCas.getDocumentLanguage());
  }
}
