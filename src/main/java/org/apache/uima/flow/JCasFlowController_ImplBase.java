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
import org.apache.uima.jcas.JCas;

/**
 * Base class to be extended by FlowControllers that use the {@link JCas} interface.
 */
public abstract class JCasFlowController_ImplBase extends FlowController_ImplBase {
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.FlowController#getRequiredCasInterface()
   */
  public Class<JCas> getRequiredCasInterface() {
    return JCas.class;
  }

  /**
   * Overriden to check that <code>aCAS</code> is an instanceof {@link JCas}. If it is, then
   * {@link #computeFlow(JCas)} is called. If not, an exception is thrown.
   */
  public final Flow computeFlow(AbstractCas aCAS) throws AnalysisEngineProcessException {
    if (aCAS instanceof JCas) {
      return computeFlow((JCas) aCAS);
    } else {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { JCas.class,
                  aCAS.getClass() });
    }
  }

  /**
   * This method must be overriden by subclasses. It takes a {@link JCas} and returns a {@link Flow}
   * object that is responsible for routing this particular JCas through the components of this
   * Aggregate. The <code>Flow</code> object should be given a handle to the JCas, so that it can
   * use information in the CAS to make routing decisions.
   * <p>
   * FlowController implementations will typically define their own class that implements
   * {@link Flow} by extending from the base class {@link JCasFlow_ImplBase}. This method would
   * then just instantiate the flow object, call its <code>setCas</code> method to provide a
   * handle to the JCas, and return the flow object.
   * 
   * @param aJCas
   *          the JCas to be routed
   * 
   * @return a Flow object that will be used to route <code>aJCas</code>
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   * @see FlowController#computeFlow(AbstractCas)
   */
  public abstract Flow computeFlow(JCas aJCas) throws AnalysisEngineProcessException;
}
