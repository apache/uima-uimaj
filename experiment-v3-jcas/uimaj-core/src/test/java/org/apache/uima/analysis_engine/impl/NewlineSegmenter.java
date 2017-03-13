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

package org.apache.uima.analysis_engine.impl;

import java.util.StringTokenizer;

import org.apache.uima.analysis_component.CasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;


public class NewlineSegmenter extends CasMultiplier_ImplBase {
  StringTokenizer mStringTok;
  private boolean casAvailable;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.CasSegmenter_ImplBase#process(org.apache.uima.cas.CAS)
   */
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    String doc = aCAS.getCurrentView().getDocumentText();
    mStringTok = new StringTokenizer(doc, "\n");
    casAvailable = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    // Check if have already returned true without an intervening next()  
    if (casAvailable) {
      throw new RuntimeException("CasMultiplier's hasNext() called twice");
    }
    return (casAvailable = mStringTok.hasMoreTokens());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    String nextSeg = mStringTok.nextToken();
    CAS cas = getContext().getEmptyCas(CAS.class);
    cas.getCurrentView().setDocumentText(nextSeg);
    casAvailable = false;
    return cas;
  }

}
