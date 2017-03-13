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
package org.apache.uima.collection.impl;

import org.junit.Assert;

import org.apache.uima.analysis_component.CasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.TypeSystemImpl;

/**
 * Test CAS Multiplier that asserts that input and output CAS type systems
 * are identical.
 */
public class TestCasMultiplier extends CasMultiplier_ImplBase {
  private CAS mInputCAS;
  
  /* (non-Javadoc)
   * @see org.apache.uima.analysis_component.CasMultiplier_ImplBase#process(org.apache.uima.cas.CAS)
   */
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    mInputCAS = aCAS;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    CAS outputCas = getEmptyCAS();
    try {
      Assert.assertTrue(((TypeSystemImpl)mInputCAS.getTypeSystem()).getLargestTypeCode() ==
        ((TypeSystemImpl)outputCas.getTypeSystem()).getLargestTypeCode());
      Assert.assertTrue(mInputCAS.getTypeSystem() == outputCas.getTypeSystem());
    }
    finally {
      outputCas.release();
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    Assert.fail(); //should never get here
    return null;
  }

}
