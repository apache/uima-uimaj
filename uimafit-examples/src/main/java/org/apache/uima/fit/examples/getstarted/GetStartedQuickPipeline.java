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
package org.apache.uima.fit.examples.getstarted;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

public class GetStartedQuickPipeline {

  public static void main(String[] args) throws UIMAException {
    // uimaFIT automatically uses all type systems listed in META-INF/org.apache.uima.fit/types.txt

    // uimaFIT doesn't provide any collection readers - so we will just instantiate a JCas and
    // run it through our AE
    JCas jCas = JCasFactory.createJCas();

    // Instantiate the analysis engine using the value "uimaFIT" for the parameter
    // PARAM_STRING ("stringParam").
    AnalysisEngine analysisEngine = AnalysisEngineFactory.createEngine(GetStartedQuickAE.class,
            GetStartedQuickAE.PARAM_STRING, "uimaFIT");

    // run the analysis engine and look for a special greeting in your console.
    analysisEngine.process(jCas);
  }
}
