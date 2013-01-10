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
package org.apache.uima.fit.factory.testAes;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.jcas.JCas;

/**
 */
@SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA, outputSofas = ViewNames.REVERSE_VIEW)
public class Annotator3 extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    try {
      jCas = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      String text = jCas.getDocumentText();
      String reverseText = reverse(text);
      JCas reverseView = ViewCreatorAnnotator.createViewSafely(jCas, ViewNames.REVERSE_VIEW);
      reverseView.setDocumentText(reverseText);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  private String reverse(String string) {
    int stringLength = string.length();
    StringBuffer returnValue = new StringBuffer();

    for (int i = stringLength - 1; i >= 0; i--) {
      returnValue.append(string.charAt(i));
    }
    return returnValue.toString();
  }

}
