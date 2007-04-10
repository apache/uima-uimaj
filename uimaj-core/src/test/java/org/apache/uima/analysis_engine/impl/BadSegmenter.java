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

import org.apache.uima.analysis_component.CasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;

/**
 * Segmenter that requests three CASes in every next() method but only asks for four in its
 * getCasInstancesRequired() method. It will run out on the second call. For testing that we can
 * catch segmenters that request too many CASes.
 */
public class BadSegmenter extends CasMultiplier_ImplBase {
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.CasSegmenter_ImplBase#process(org.apache.uima.cas.CAS)
   */
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    //do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    // request too many CASes
    getContext().getEmptyCas(CAS.class);
    getContext().getEmptyCas(CAS.class);
    return getContext().getEmptyCas(CAS.class);
  }

  public int getCasInstancesRequired() {
    return 4;
  }

}
