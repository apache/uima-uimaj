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

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * FlowControllers are components that decide how to route CASes within Aggregate Analysis Engines.
 * There is always exactly one FlowController per Aggregate Analysis Engine.
 * <p>
 * FlowController's {@link #initialize(FlowControllerContext)} method receives a
 * {@link FlowControllerContext}, which is a subtype of {@link UimaContext} that has the following
 * additional information useful for routing:
 * <ul>
 * <li>A map from String keys to Analysis Engine Metadata for all Analysis Engines that the
 * FlowController can route CASes to</li>
 * <li>Declared Capabilities of the Aggregate AnalysisEngine containing this FlowController.</li>
 * </ul>
 * <p>
 * For each new CAS that is passed to the Aggregate Analysis Engine containing the FlowController,
 * the FlowController's {@link #computeFlow(AbstractCas)} method will be called. This method must
 * construct and return a {@link Flow} object that is responsible for routing that CAS through the
 * components of the Aggregate Analysis Engine.
 * <p>
 * A FlowController, like other components, can have custom configuration parameters that it
 * accesses through its Context. These parameters can define the flow using whatever flow language
 * the particular FlowController implementation requires. The <code>Flow</code> object can be
 * given a handle to the CAS, so that it can use any information in the CAS to make its routing
 * decisions.
 * <p>
 * For convenience, FlowController implementations can extend from the base classes
 * {@link org.apache.uima.flow.CasFlowController_ImplBase} or
 * {@link org.apache.uima.flow.JCasFlowController_ImplBase}, depending on which CAS interface they
 * wish to use.
 */
public interface FlowController {
  /**
   * Performs any startup tasks required by this component. The framework calls this method only
   * once, just after the FlowController has been instantiated.
   * <p>
   * The framework supplies this FlowController with a reference to the
   * {@link FlowControllerContext} that it will use, for example to access configuration settings or
   * resources. This FlowController should store a reference to this Context for later use.
   * 
   * @param aContext
   *          Provides access to services and resources managed by the framework. This includes
   *          configuration parameters, logging, and access to external resources. Also provides the
   *          FlowController with the metadata of all of the AnalysisEngines that are possible
   *          targets for routing CASes.
   * 
   * @throws ResourceInitializationException
   *           if the FlowController cannot initialize successfully.
   */
  void initialize(FlowControllerContext aContext) throws ResourceInitializationException;

  /**
   * Alerts this FlowController that the values of its configuration parameters or external
   * resources have changed. This FlowController should re-read its configuration from the
   * {@link UimaContext} and take appropriate action to reconfigure itself.
   * <p>
   * In the abstract base classes provided by the framework, this is generally implemented by
   * calling <code>destroy</code> followed by <code>initialize</code>. If a more efficient
   * implementation is needed, you can override that implementation.
   * 
   * @throws ResourceConfigurationException
   *           if the new configuration is invalid
   * @throws ResourceInitializationException
   *           if this component encounters a problem in reinitializing itself from the new
   *           configuration
   */
  void reconfigure() throws ResourceConfigurationException, ResourceInitializationException;

  /**
   * Completes the processing of a batch of CASes. The size of a batch is determined based on
   * configuration provided by the application that is using this component. The purpose of
   * <code>batchProcessComplete</code> is to give this component the change to flush information
   * from memory to persistent storage. In the event of an error, this allows the processing to be
   * restarted from the end of the last completed batch.
   * <p>
   * If this component's descriptor declares that it is <code>recoverable</code>, then this
   * component is <i>required</i> to be restartable from the end of the last completed batch.
   * 
   * @throws AnalysisEngineProcessException
   *           if this component encounters a problem in flushing its state to persistent storage
   */
  void batchProcessComplete() throws AnalysisEngineProcessException;

  /**
   * Notifies this component that processing of an entire collection has been completed. In this
   * method, this component should finish writing any output relating to the current collection.
   * 
   * @throws AnalysisEngineProcessException
   *           if this component encounters a problem in its end-of-collection processing
   */
  void collectionProcessComplete() throws AnalysisEngineProcessException;

  /**
   * Frees all resources held by this FlowController. The framework calls this method only once,
   * when it is finished using this component.
   */
  void destroy();

  /**
   * Invokes this FlowController on a CAS. The FlowController returns a {@link Flow} object that is
   * responsible for routing this particular CAS through the components of this Aggregate. The
   * <code>Flow</code> object should be given a handle to the CAS, so that it can use information
   * in the CAS to make routing decisions.
   * <p>
   * FlowController implementations will typically define their own class that implements
   * {@link Flow} by extending from the base class {@link CasFlow_ImplBase} or
   * {@link JCasFlow_ImplBase}. This method would then just instantiate the flow object, call its
   * <code>setCas</code> method to provide a handle to the CAS, and return the flow object.
   * 
   * @param aCAS
   *          A CAS that this FlowController should process. The framework will ensure that aCAS
   *          implements the specific CAS interface declared in the &lt;casInterface&gt; element of
   *          this FlowController's descriptor.
   * 
   * @return a Flow object that has responsibility for routing <code>aCAS</code> through the
   *         Aggregate Analysis Engine.
   * 
   * @throws AnalysisEngineProcessException
   *           if this FlowController encounters a problem computing the flow for the CAS
   */
  Flow computeFlow(AbstractCas aCAS) throws AnalysisEngineProcessException;

  /**
   * Returns the specific CAS interface that this FlowController requires the framework to pass to
   * its {@link #computeFlow(AbstractCas)} method.
   * 
   * @return the required CAS interface. This must specify a subtype of {@link AbstractCas}.
   */
  Class<? extends AbstractCas> getRequiredCasInterface();
  
  /**
   * Notifies this FlowController that new Analysis Engines are now available to route CASes to.
   * Prior to calling this method the framework will update
   * {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}
   * with the metadata for these new Analysis Engines.
   * <p>
   * This FlowController is not obligated to do anything in response to this method if it does
   * not want to consider routing CASes to the new AnalysisEngines.
   * <p>
   * The contract for this method is that the framework will not concurrently call any
   * {@link Flow#next()} methods on any Flow objects produced by this FlowController, during the
   * time between when the Analysis Engine MetaData map is updated and the time when this method
   * completes.
   * 
   * @param aKeys a Collection of Strings, each of which is the key of an Analysis Engine to which
   *   CASes can be routed.  These are the same keys as used in
   *   {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}.
   */
  void addAnalysisEngines(Collection<String> aKeys);

  /**
   * Notifies this FlowController that some Analysis Engines are no longer available to route CASes to.
   * Prior to calling this method the framework will update
   * {@link FlowControllerContext#getAnalysisEngineMetaDataMap()}
   * and will remove the metadata for these new Analysis Engines.
   * <p>
   * It is not required for a FlowController implementation to support this method.  It may throw
   * an exception if this operation is not supported
   * (see {@link AnalysisEngineProcessException#REMOVE_AE_FROM_FLOW_NOT_SUPPORTED}.
   * Also the FlowController may throw an Exception if it determines that it does not make sense for
   * the flow to continue in the absence of the removed Analysis Engines
   * (see {@link AnalysisEngineProcessException#FLOW_CANNOT_CONTINUE_AFTER_REMOVE}.
   * <p>
   * The contract for this method is that the framework will not concurrently call any
   * {@link Flow#next()} methods on any Flow objects produced by this FlowController, during the
   * time between when the Analysis Engine MetaData map is updated and the time when this method
   * completes.
   * 
   * @param aKeys a Collection of Strings, each of which is the key of an Analysis Engine to which CASes
   *   may no longer be routed.
   * 
   * @throws AnalysisEngineProcessException if the FlowController cannot continue with these
   *   Analysis Engines removed, or doesn't support removing Analysis Engines at all.
   */
  void removeAnalysisEngines(Collection<String> aKeys) throws AnalysisEngineProcessException;
}
