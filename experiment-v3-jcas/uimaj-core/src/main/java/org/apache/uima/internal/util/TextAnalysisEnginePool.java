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

package org.apache.uima.internal.util;

import java.util.Map;

import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * A pool of Text Analysis Engines, which supports reconfiguration. This is not part of the stable
 * UIMA API and may change in future releases.
 * 
 * @deprecated As of v2.0, use {@link org.apache.uima.internal.util.AnalysisEnginePool} instead.
 */
@Deprecated
public class TextAnalysisEnginePool extends AnalysisEnginePool {

  /**
   * Creates a new TextAnalysisEnginePool.
   * 
   * @param aName
   *          the pool name
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public TextAnalysisEnginePool(String aName, int aNumInstances,
          ResourceSpecifier aResourceSpecifier) throws ResourceInitializationException {
    this(aName, aNumInstances, aResourceSpecifier, null);
  }

  /**
   * Creates a new TextAnalysisEnginePool.
   * 
   * @param aName
   *          the pool name
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * @param aResourceInitParams
   *          additional parameters to be passed to
   *          {@link Resource#initialize(ResourceSpecifier, Map)} methods. May be null if there are
   *          no parameters.
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public TextAnalysisEnginePool(String aName, int aNumInstances,
          ResourceSpecifier aResourceSpecifier, Map<String, Object> aResourceInitParams)
          throws ResourceInitializationException {
    super(aName, aNumInstances, aResourceSpecifier, aResourceInitParams);
  }

  /**
   * Checks out a TextAnalysisEngine from the pool.
   * 
   * @return a TAE for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public TextAnalysisEngine getTAE() {
    return (TextAnalysisEngine) getAnalysisEngine();
  }

  /**
   * Checks in a TAE to the pool. Also notifies other Threads that may be waiting for a connection.
   * 
   * @param aTAE
   *          the resource to release
   */
  public void releaseTAE(TextAnalysisEngine aTAE) {
    releaseAnalysisEngine(aTAE);
  }

  /**
   * Checks out a TextAnalysisEngine from the pool. If none is currently available, wait for the
   * specified amount of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a TAE for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public TextAnalysisEngine getTAE(long aTimeout) {
    return (TextAnalysisEngine) getAnalysisEngine(aTimeout);
  }

  /**
   * @see AnalysisEnginePool#getResourceClass()
   */
  protected Class getResourceClass() {
    return TextAnalysisEngine.class;
  }

}
