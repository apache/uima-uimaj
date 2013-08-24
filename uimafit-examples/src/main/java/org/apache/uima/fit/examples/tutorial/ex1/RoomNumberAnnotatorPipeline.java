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
package org.apache.uima.fit.examples.tutorial.ex1;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.examples.tutorial.type.RoomNumber;
import org.apache.uima.jcas.JCas;

public class RoomNumberAnnotatorPipeline {

  public static void main(String[] args) throws UIMAException {
    String text = "The meeting was moved from Yorktown 01-144 to Hawthorne 1S-W33.";
    AnalysisEngine analysisEngine = createEngine(RoomNumberAnnotator.class);
    JCas jCas = analysisEngine.newJCas();
    jCas.setDocumentText(text);
    analysisEngine.process(jCas);

    for (RoomNumber roomNumber : select(jCas, RoomNumber.class)) {
      System.out.println(roomNumber.getCoveredText() + "\tbuilding = " + roomNumber.getBuilding());
    }
  }
}
