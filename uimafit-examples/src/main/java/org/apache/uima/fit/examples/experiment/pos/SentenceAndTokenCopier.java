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
package org.apache.uima.fit.examples.experiment.pos;

import static org.apache.uima.fit.examples.experiment.pos.ViewNames.VIEW1;
import static org.apache.uima.fit.examples.experiment.pos.ViewNames.VIEW2;
import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.examples.type.Sentence;
import org.apache.uima.fit.examples.type.Token;
import org.apache.uima.jcas.JCas;

/**
 * This simple AE copies tokens and sentences from one view to another.
 */
@SofaCapability(inputSofas = { VIEW1, VIEW2 })
public class SentenceAndTokenCopier extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    try {
      JCas view1 = jCas.getView(VIEW1);
      JCas view2 = jCas.getView(VIEW2);

      for (Token token1 : select(view1, Token.class)) {
        new Token(view2, token1.getBegin(), token1.getEnd()).addToIndexes();
      }

      for (Sentence sentence1 : select(view1, Sentence.class)) {
        new Sentence(view2, sentence1.getBegin(), sentence1.getEnd()).addToIndexes();
      }
    } catch (CASException ce) {
      throw new AnalysisEngineProcessException(ce);
    }
  }
}
