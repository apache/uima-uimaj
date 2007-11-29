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

import org.apache.uima.analysis_engine.ResultSpecification;

/**
 * Special type of SimpleStep intended only for backwards compatibility with
 * the behavior of the Capability Language Flow in UIMA 1.x.  Allows the
 * Flow Controller to set a Result Specification for the AE that will be called next.
 * 
 * @deprecated  For backwards compatibility with Capability Language Flow only.
 *   User-developed Flow Controllers should not use this.
 */
public class SimpleStepWithResultSpec extends SimpleStep {

  /**
   * @param casProcessorKey
   * @param currentAnalysisResultSpec
   */
  public SimpleStepWithResultSpec(String aCasProcessorKey, ResultSpecification aResultSpec) {
    super(aCasProcessorKey);
    setResultSpecification(aResultSpec);
  }

  /**
   * Gets the key of the Analysis Engine to which the CAS should be routed.
   * 
   * @return an AnalysisEngine key
   */
  public ResultSpecification getResultSpecification() {
    return mResultSpec;
  }

  /**
   * Sets the key of the Analysis Engine to which the CAS should be routed. By using this method, a
   * user's Flow implementation can (but is not required to) reuse the same SimpleStep object
   * multiple times.
   * 
   * @return an Analysis Engine key. This must be one of the keys in the FlowController's
   *         {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}.
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    mResultSpec = aResultSpec;
  }
  
  private ResultSpecification mResultSpec;
}
