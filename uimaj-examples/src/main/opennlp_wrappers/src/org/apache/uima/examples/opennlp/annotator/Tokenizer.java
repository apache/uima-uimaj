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

package org.apache.uima.examples.opennlp.annotator;

import java.io.IOException;

import opennlp.tools.util.Span;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.opennlp.Sentence;
import org.apache.uima.examples.opennlp.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * UIMA Analysis Engine that invokes the OpenNLP Tokenizer. The OpenNLP Tokenizer generates a
 * PennTreeBank style tokenization. This annotator assumes that sentences have already been
 * annotated in the CAS with Sentence annotations. We iterate over sentences and invoke the OpenNLP
 * Tokenizer on each sentence. For each token, a Token annotation is created in the CAS. The model
 * file for the OpenNLP Tokenizer is specified as a parameter (MODEL_FILE_PARAM).
 * 
 */
public class Tokenizer extends JCasAnnotator_ImplBase {

  public static final String MODEL_FILE_PARAM = "ModelFile";

  public static final String COMPONENT_ID = "OpenNLP Tokenizer";

  private opennlp.tools.lang.english.Tokenizer tokenizer;

  /**
   * Initialize the Annotator.
   * 
   * @see JCasAnnotator_ImplBase#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    String modelFile;

    modelFile = (String) aContext.getConfigParameterValue(MODEL_FILE_PARAM);

    try {
      tokenizer = new opennlp.tools.lang.english.Tokenizer(modelFile);
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Process a CAS.
   * 
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {

    AnnotationIndex sentenceIndex = aJCas.getAnnotationIndex(Sentence.type);

    // iterate over Sentences
    FSIterator sentenceIterator = sentenceIndex.iterator();
    while (sentenceIterator.hasNext()) {
      Sentence sentence = (Sentence) sentenceIterator.next();

      String text = sentence.getCoveredText();
      Span[] tokenSpans = tokenizer.tokenizePos(text);
      for (int i = 0; i < tokenSpans.length; i++) {
        Span span = tokenSpans[i];
        Token token = new Token(aJCas);
        token.setBegin(sentence.getBegin() + span.getStart());
        token.setEnd(sentence.getBegin() + span.getEnd());
        token.setComponentId(COMPONENT_ID);
        token.addToIndexes();
      }
    }

  }
}
