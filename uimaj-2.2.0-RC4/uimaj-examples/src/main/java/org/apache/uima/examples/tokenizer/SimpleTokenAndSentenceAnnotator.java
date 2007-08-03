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

package org.apache.uima.examples.tokenizer;

import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.Locale;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * An example annotator that annotates Tokens and Sentences.
 */
public class SimpleTokenAndSentenceAnnotator extends JCasAnnotator_ImplBase {

  static abstract class Maker {
    abstract Annotation newAnnotation(JCas jcas, int start, int end);
  }

  JCas jcas;

  String input;

  ParsePosition pp = new ParsePosition(0);

  // ****************************************
  // * Static vars holding break iterators
  // ****************************************
  static final BreakIterator sentenceBreak = BreakIterator.getSentenceInstance(Locale.US);

  static final BreakIterator wordBreak = BreakIterator.getWordInstance(Locale.US);

  // *********************************************
  // * function pointers for new instances *
  // *********************************************
  static final Maker sentenceAnnotationMaker = new Maker() {
    Annotation newAnnotation(JCas jcas, int start, int end) {
      return new Sentence(jcas, start, end);
    }
  };

  static final Maker tokenAnnotationMaker = new Maker() {
    Annotation newAnnotation(JCas jcas, int start, int end) {
      return new Token(jcas, start, end);
    }
  };

  // *************************************************************
  // * process *
  // *************************************************************
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    jcas = aJCas;
    input = jcas.getDocumentText();

    // Create Annotations
    makeAnnotations(sentenceAnnotationMaker, sentenceBreak);
    makeAnnotations(tokenAnnotationMaker, wordBreak);
  }

  // *************************************************************
  // * Helper Methods *
  // *************************************************************
  void makeAnnotations(Maker m, BreakIterator b) {
    b.setText(input);
    for (int end = b.next(), start = b.first(); end != BreakIterator.DONE; start = end, end = b
            .next()) {
      // eliminate all-whitespace tokens
      boolean isWhitespace = true;
      for (int i = start; i < end; i++) {
        if (!Character.isWhitespace(input.charAt(i))) {
          isWhitespace = false;
          break;
        }
      }
      if (!isWhitespace) {
        m.newAnnotation(jcas, start, end).addToIndexes();
      }
    }
  }
}
