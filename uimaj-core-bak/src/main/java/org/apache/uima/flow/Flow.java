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

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;

/**
 * A Flow object is responsible for routing a single CAS through an Aggregate Analysis
 * Engine.
 * <p>
 * Typically, developers extend {@link org.apache.uima.flow.CasFlow_ImplBase} or
 * {@link org.apache.uima.flow.JCasFlow_ImplBase} depending on which CAS interface they
 * wish to use.
 */
public interface Flow
{
  /**
   * Gets the next destination for the CAS.  This is defined by a {@link Step} object.
   * There may be different kinds of Step objects to indicate different kinds of
   * routing actions.
   * @return the next destination for the CAS
   * @throws AnalysisEngineProcessException if a failure occurs while determining the next destination
   */
  Step next() throws AnalysisEngineProcessException;
  
  /**
   * Called by the framework if the CAS that is being routed by this Flow has been
   * sent to a CAS Multiplier which has then created a new CAS derived from that original CAS.
   * <p>
   * It is not required for a Flow implementation to support the production of new CASes in
   * the middle of the flow, in which case this method may throw an exception.
   * <p>
   * If implemented, this method should construct a new {@link Flow} object that
   * will be used to route the new CAS.  The new Flow object then takes over
   * all responsibility for that CAS.   
   * 
   * @param newCas the new CAS
   * @param producedBy key of the AnalysisEngine (CAS Multiplier) that produced the new CAS
   * 
   * @return a new Flow object that has responsibility for routing <code>aCAS</code> 
   *   through the Aggregate Analysis Engine.
   *   
   * @throws AnalysisEngineProcessException
   */
  Flow newCasProduced(AbstractCas newCas, String producedBy) throws AnalysisEngineProcessException;
}
