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

import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.examples.type.Token;
import org.apache.uima.jcas.JCas;

/**
 * This "baseline" part-of-speech tagger isn't very sophisticated! Notice, however, that the tagger
 * operates on the default view. This will be mapped to the "system" view when we run our
 * experiment.
 */
public class BaselineTagger extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for (Token token : select(jCas, Token.class)) {
      String word = token.getCoveredText();
      if (word.equals("a") || word.equals("the")) {
        token.setPos("DT");
      } else {
        token.setPos("NN");
      }
    }
  }
}
