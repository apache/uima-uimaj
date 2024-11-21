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

import java.util.Arrays;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.jcas.JCas;

/**
 */

@SofaCapability(inputSofas = { CAS.NAME_DEFAULT_SOFA, ViewNames.PARENTHESES_VIEW }, outputSofas = {
    ViewNames.SORTED_VIEW, ViewNames.SORTED_PARENTHESES_VIEW })
public class Annotator2 extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    try {
      JCas sortedView = ViewCreatorAnnotator.createViewSafely(jCas, ViewNames.SORTED_VIEW);
      jCas = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      String initialText = jCas.getDocumentText();
      char[] chars = initialText.toCharArray();
      Arrays.sort(chars);
      String sortedText = new String(chars).trim();
      sortedView.setDocumentText(sortedText);

      sortedView = ViewCreatorAnnotator.createViewSafely(jCas, ViewNames.SORTED_PARENTHESES_VIEW);
      JCas parenthesesView = jCas.getView(ViewNames.PARENTHESES_VIEW);
      String parenthesesText = parenthesesView.getDocumentText();
      chars = parenthesesText.toCharArray();
      Arrays.sort(chars);
      sortedText = new String(chars).trim();
      sortedView.setDocumentText(sortedText);

    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }

  }

}
