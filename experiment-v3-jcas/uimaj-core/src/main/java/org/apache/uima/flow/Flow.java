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
 * A Flow object is responsible for routing a single CAS through an Aggregate Analysis Engine.
 * <p>
 * Typically, developers extend {@link org.apache.uima.flow.CasFlow_ImplBase} or
 * {@link org.apache.uima.flow.JCasFlow_ImplBase} depending on which CAS interface they wish to use.
 */
public interface Flow {
  /**
   * Gets the next destination for the CAS. This is defined by a {@link Step} object. There may be
   * different kinds of Step objects to indicate different kinds of routing actions.
   * 
   * @return the next destination for the CAS
   * @throws AnalysisEngineProcessException
   *           if a failure occurs while determining the next destination
   */
  Step next() throws AnalysisEngineProcessException;

  /**
   * Called by the framework if the CAS that is being routed by this Flow has been sent to a CAS
   * Multiplier which has then created a new CAS derived from that original CAS.
   * <p>
   * It is not required for a Flow implementation to support the production of new CASes in the
   * middle of the flow, in which case this method may throw an exception.
   * <p>
   * If implemented, this method should construct a new {@link Flow} object that will be used to
   * route the new CAS. The new Flow object then takes over all responsibility for that CAS.
   * 
   * @param newCas
   *          the new CAS
   * @param producedBy
   *          key of the AnalysisEngine (CAS Multiplier) that produced the new CAS
   * 
   * @return a new Flow object that has responsibility for routing <code>aCAS</code> through the
   *         Aggregate Analysis Engine.
   * 
   * @throws AnalysisEngineProcessException passthru
   */
  Flow newCasProduced(AbstractCas newCas, String producedBy) throws AnalysisEngineProcessException;

  /**
   * May be called by the framework to ask the FlowController if processing of the CAS can
   * continue after a failure occurred while executing the last Step that the Flow Controller
   * returned.  
   * <p>
   * If this method returns true, then the framework may continue to call the {@link #next()} method
   * to continue routing the CAS.  If this method returns false, the framework will not make any
   * more calls to the {@link #next()} method, and will call the {@link #aborted()} method.
   * <p>
   * In the case where the last Step was a {@link ParallelStep}, if at least one of the destinations 
   * resulted in a failure, this method will be called to report one of the failures.  If this method
   * returns true, but one of the other destinations in the ParallelStep resulted in a failure, this
   * method will be called again to report the next failure.  This continues until either this method
   * returns false or there are no more failures.
   * <p>
   * Note that it is possible for processing of a CAS to be aborted without this method being called.
   * This method is only called when an attempt is being made to continue processing of the CAS
   * following an error, which may be an application configuration decision. 
   *  
   * @param failedAeKey The key of the analysis engine that failed.
   * @param failure the Exception that occurred
   * 
   * @return true if the FlowController decides that processing of the CAS can continue; false if
   *   processing of the CAS should be aborted.
   */
  boolean continueOnFailure(String failedAeKey, Exception failure);

  /**
   * Called by the framework if processing has been aborted for the CAS that was being
   * routed by this Flow object.  No further processing will take place on the CAS after
   * this method is called, so the framework will not call the {@link #next()} method again.
   * <p>
   * This method provides the Flow object with an opportunity to clean up any resources.
   * Also, it could be used to allow the FlowController to reuse a Flow object if desired.
   */  
  void aborted();
  
}
