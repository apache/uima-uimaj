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

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

/**
 * Implementation base class for FlowControllers. Normally developers do not extend this class
 * directly. Instead use {@link JCasFlowController_ImplBase} or {@link CasFlowController_ImplBase},
 * depending on which CAS interface you would like to use.
 * <p>
 * This class implements the {@link #initialize(FlowControllerContext)} method and stores the
 * <code>FlowControllerContext</code> in a private field where it can be accessed via the
 * {@link #getContext()} method.
 * <p>
 * This class also provides a "dumb" implementation of the {@link #reconfigure()} method, which
 * simply calls {@link #destroy()} followed by {@link #initialize(FlowControllerContext)}.
 * Developers of FlowControllers with expensive initialization logic should override this method and
 * provide a more intelligent implementation.
 */
public abstract class FlowController_ImplBase implements FlowController {
  private FlowControllerContext mContext = null;

  @Override
  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    mContext = aContext;
  }

  @Override
  public void reconfigure() throws ResourceInitializationException, ResourceConfigurationException {
    destroy();
    initialize(mContext);
  }

  @Override
  public void batchProcessComplete() throws AnalysisEngineProcessException {
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
  }

  @Override
  public void destroy() {
  }

  /**
   * Does nothing by default. Subclasses may override this to support adding new AnalysisEngines to
   * the flow.
   * 
   * @see org.apache.uima.flow.FlowController#addAnalysisEngines(java.util.Collection)
   */
  @Override
  public void addAnalysisEngines(Collection<String> aKeys) {
    // does nothing by default
  }

  /**
   * Throws an AnalysisEngineProcessException by default. Subclasses may override this to support
   * removing AnalysisEngines from the flow.
   * 
   * @see org.apache.uima.flow.FlowController#removeAnalysisEngines(java.util.Collection)
   */
  @Override
  public void removeAnalysisEngines(Collection<String> aKeys)
          throws AnalysisEngineProcessException {
    throw new AnalysisEngineProcessException(
            AnalysisEngineProcessException.REMOVE_AE_FROM_FLOW_NOT_SUPPORTED,
            new Object[] { getClass().getName() });
  }

  /**
   * Gets the FlowControllerContext for this FlowController. This provides access to configuration
   * parameters, resources, and also to metadata for each AnalysisEngine that is available for this
   * FlowController to route CASes to.
   * 
   * @return the FlowControllerContext for this FlowController
   */
  protected FlowControllerContext getContext() {
    return mContext;
  }

  /**
   * Gets the logger for this FlowController
   * 
   * @return the logger for this FlowController
   */
  protected Logger getLogger() { // https://issues.apache.org/jira/projects/UIMA/issues/UIMA-5565
    return getContext().getLogger();
  }
}
