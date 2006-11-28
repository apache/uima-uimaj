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

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.cas.CAS;

/** Trivial CasIterator that returns no CASes. */
public class EmptyCasIterator implements CasIterator {
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.CasIterator#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.CasIterator#next()
   */
  public CAS next() throws AnalysisEngineProcessException {
    throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS, new Object[0]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.CasIterator#release()
   */
  public void release() {
    // nothing to do
  }
}
