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

package org.apache.uima.flow;

import java.util.Collection;
import java.util.Collections;

/**
 * Indicates that a CAS should be routed to a multiple AnalysisEngines and that the relative order
 * in which these execute does not matter. Logically, they can run in parallel. The runtime is not
 * obligated to actually execute them in parallel, however.
 * <p>
 * After all the specified Analysis Engines have completed their processing, the {@link Flow#next()}
 * method will be called again to determine the next destination for the CAS.
 */
public class ParallelStep extends Step {
  
  private Collection<String> mKeys;
  
  /**
   * Creates a new ParallelStep
   * 
   * @param aAnalysisEngineKeys
   *          A Collection of Strings, where each String is the key of an Analysis Engine to which the CAS
   *          should be routed. Each String must bee one of the keys in the FlowController's
   *          {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}.
   */
  public ParallelStep(Collection<String> aAnalysisEngineKeys) {
    setAnalysisEngineKeys(aAnalysisEngineKeys);
  }

  /**
   * Gets the keys of the Analysis Engines to which the CAS should be routed.
   * 
   * @return an unmodifiable Collection of Strings, where each String is the key of an Analysis Engine to 
   *         which the CAS should be routed.
   */
  public Collection<String> getAnalysisEngineKeys() {
    return mKeys;
  }

  /**
   * Sets the keys of the Analysis Engines to which the CAS should be routed. By using this method,
   * a user's Flow implementation can (but is not required to) reuse the same ParallelStep object
   * multiple times.
   * 
   * @param aKeys A Collection of Strings, where each String is the key of an Analysis Engine to which the CAS
   *         should be routed. Each String must bee one of the keys in the FlowController's
   *         {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}.
   */
  public void setAnalysisEngineKeys(Collection<String> aKeys) {
    mKeys = Collections.unmodifiableCollection(aKeys);
  }
}
