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

package org.apache.uima.cas.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.test.Sentence;
import org.apache.uima.cas.test.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//@formatter:off
/**
 * The setup:
 *   Token (super = Annotation)
 *   Sentence (super = Annotation)
 *   
 *   Annotator:  (in descr) SubIteratorAnnotator
 */
//@formatter:on
public class SubiteratorTest {

  private static AnalysisEngine segmenter = null;

  private JCas jcas;

  @BeforeAll
  static void setupClass() throws Exception {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/TokensAndSentences.xml");

    assertThat(descriptorFile).exists();

    XMLParser parser = UIMAFramework.getXMLParser();
    ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
    segmenter = UIMAFramework.produceAnalysisEngine(spec);
  }

  @BeforeEach
  public void setUp() throws Exception {
    String text = contentOf(getClass().getResource("/CASTests/verjuice.txt"), UTF_8);

    jcas = segmenter.newJCas();
    jcas.setDocumentText(text);

    segmenter.process(jcas);
  }

  @Test
  public void testAnnotator() throws Exception {
    iterateAndCheck(jcas);

    iterateAndCheck(jcas);
  }

  @Test
  public void thatTemporaryAnnotationsAreNotRetained() throws Exception {
    var casImpl = ((CASImpl) jcas.getCas());
    try (var ctx = casImpl.ll_forceEnableV2IdRefs(true)) {
      var fsesBefore = new AllFSs(casImpl).getAllFSsAllViews_sofas_reachable().getAllFSsSorted();
      var maxId = fsesBefore.stream().mapToInt(fs -> fs._id).max().getAsInt();

      // This select creates a temporary annotation used to constrain the operation
      jcas.select(Token.class).covering(0, 10).asList();

      var fsesAfter = new AllFSs(casImpl).getAllFSsAllViews_sofas_reachable().getAllFSsSorted();
      assertThat(fsesAfter) //
              .extracting(fs -> fs.getType().getName())//
              .containsOnly(CAS.TYPE_NAME_SOFA, CAS.TYPE_NAME_DOCUMENT_ANNOTATION,
                      Sentence._TypeName, Token._TypeName);

      // The +1 here accounts for the temporary Annotation that was created.
      var t = new Token(jcas);
      assertThat(t._id).isEqualTo(maxId + 2 + 1);
    }
  }

  private void iterateAndCheck(JCas aJCas) {
    var tokenIndex = aJCas.getAnnotationIndex(Token.class);
    var firstSentence = aJCas.getAnnotationIndex(Sentence.class).iterator().next();
    var tokenIterator = tokenIndex.subiterator(firstSentence);
    var firstToken = tokenIndex.iterator().next();
    tokenIterator.moveTo(firstToken);

    // check unambiguous iterator creation
    FSIterator<Token> it = tokenIndex.iterator(false);
    it.moveTo(firstToken);
  }
}
