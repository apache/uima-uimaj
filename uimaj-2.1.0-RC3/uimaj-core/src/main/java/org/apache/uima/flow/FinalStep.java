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

/**
 * Indicates that a CAS has finished being processed by the aggregate. After returning a FinalStep,
 * the {@link Flow#next()} method will not be called again for this CAS.
 * <p>
 * If the CAS was passed in as an input to the aggregate's process method, then the aggregate's
 * processing is completed and ownership of this CAS is returned to the caller.
 * <p>
 * If the CAS was generated internally to this aggregate (by a CAS Multiplier that is part of this
 * aggregate), then it will either be output from the aggregate or it will be dropped. A CAS can
 * only be output if the aggregate's metadata includes declares the operational property
 * outputsNewCASes == true. (see
 * {@link org.apache.uima.resource.metadata.OperationalProperties#getOutputsNewCASes()}).
 * <p>
 * By default, if the aggregate's metadata declares outputsNewCASes == true, then <b>all</b> CASes
 * generated internal to the aggregate are output. However, by passing <code>true</code> to the
 * {@link #FinalStep(boolean)} constructor, the Flow Controller can force a particular CAS to be
 * dropped. This allows the Flow Controller to output some CASes but not others.
 * <p>
 * It is not permitted to drop a CAS that was passed as input to the AnalysisEngine, and using
 * <code>FinalStep(true)</code> for such a CAS is an error.
 */
public class FinalStep extends Step {
  private boolean forceDropCas;

  /**
   * Creates a new FinalStep.
   */
  public FinalStep() {
  }

  /**
   * Creates a new FinalStep, and may indicate that a CAS should be dropped. This can only be used
   * for CASes that are produced internally to the aggregate. It is an error to attempt to drop a
   * CAS that was passed as input to the aggregate.
   * 
   * @param aForceCasToBeDropped
   *          true forces this CAS to be dropped. false causes the default behavior, which is to
   *          output the CAS whenever appropriate.
   */

  public FinalStep(boolean aForceCasToBeDropped) {
    forceDropCas = aForceCasToBeDropped;
  }

  /**
   * Gets whether the CAS should be dropped.
   * 
   * @return true forces this CSA to be dropped. false causes the default behavior, which is to
   *         output the CAS whenever appropriate.
   */
  public boolean getForceCasToBeDropped() {
    return forceDropCas;
  }
}
