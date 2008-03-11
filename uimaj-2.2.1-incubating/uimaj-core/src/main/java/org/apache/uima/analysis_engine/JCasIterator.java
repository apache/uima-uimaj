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

package org.apache.uima.analysis_engine;

import org.apache.uima.jcas.JCas;

/**
 * An iterator over a collection of JCAS objects. One use for this interface is to allow an
 * application to iterate over all of the CASes produced by as output of the
 * {@link AnalysisEngine#processAndOutputNewCASes(JCas)} method.
 */
public interface JCasIterator {
  /**
   * Checks if there are more JCASes to be returned by the iterator.
   * 
   * @return true if there are more JCASes to be returned, false if not
   * 
   * @throws AnalysisEngineProcessException
   *           if a failure has occurred during processing. If an exception is thrown, this
   *           indicates that processing has aborted, so no further calls to the JCasIterator
   *           should be made.
   */
  boolean hasNext() throws AnalysisEngineProcessException;

  /**
   * Gets the next JCAS from the iterator.
   * 
   * @return the next JCAS.
   * 
   * @throws AnalysisEngineProcessException
   *           if a failure has occurred during processing. If an exception is thrown, this
   *           indicates that processing has aborted, so no further calls to the JCasIterator
   *           should be made.
   */
  JCas next() throws AnalysisEngineProcessException;

  /**
   * Releases any CASes owned by this JCasIterator. You only need to Call this method if you stop
   * using a CasIterator before you have iterated all the way through.
   */
  public void release();
}
