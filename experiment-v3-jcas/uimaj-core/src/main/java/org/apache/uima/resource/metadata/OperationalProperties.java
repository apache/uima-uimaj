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

package org.apache.uima.resource.metadata;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;

/**
 * Operational Properties for a UIMA component.
 */
public interface OperationalProperties {
  /**
   * Gets whether this component will modify the CAS.
   * 
   * @return true if this component modifies the CAS, false if it does not.
   */
  public boolean getModifiesCas();

  /**
   * Sets whether this component will modify the CAS.
   * 
   * @param aModifiesCas
   *          true if this component modifies the CAS, false if it does not.
   */
  public void setModifiesCas(boolean aModifiesCas);

  /**
   * Gets whether multiple instances of this component can be run in parallel, each receiving a
   * subset of the documents from a collection.
   * 
   * @return true if multiple instances can be run in parallel, false if not
   */
  public boolean isMultipleDeploymentAllowed();

  /**
   * Sets whether multiple instances of this component can be run in parallel, each receiving a
   * subset of the documents from a collection.
   * 
   * @param aMultipleDeploymentAllowed
   *          true if multiple instances can be run in parallel, false if not
   */
  public void setMultipleDeploymentAllowed(boolean aMultipleDeploymentAllowed);

  /**
   * Gets whether this AnalysisEngine may output new CASes. If this property is set to true, an
   * application can use the {@link AnalysisEngine#processAndOutputNewCASes(CAS)} to pass a CAS to
   * this this AnalysisEngine and then step through all of the output CASes that it produces. For
   * example, such an AnalysisEngine could segment a CAS into smaller pieces, emitting each as a
   * separate CAS.
   * 
   * @return true if this component may output new CASes, false if it does not
   */
  public boolean getOutputsNewCASes();

  /**
   * Sets whether this AnalysisEngine may output new CASes. If this property is set to true, an
   * application can use the {@link AnalysisEngine#processAndOutputNewCASes(CAS)} to pass a CAS to
   * this this AnalysisEngine and then step through all of the output CASes that it produces. For
   * example, such an AnalysisEngine could segment a CAS into smaller pieces, emitting each as a
   * separate CAS.
   * 
   * @param aOutputsNewCASes
   *          true if this component may output new CASes, false if it does not
   */
  public void setOutputsNewCASes(boolean aOutputsNewCASes);
  
}
