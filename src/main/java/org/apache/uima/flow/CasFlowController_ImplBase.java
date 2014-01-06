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
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/**
 * Base class to be extended by FlowControllers that use the {@link CAS} interface.
 */
public abstract class CasFlowController_ImplBase extends FlowController_ImplBase {
  private TypeSystem mLastTypeSystem;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.FlowController#getRequiredCasInterface()
   */
  public Class<CAS> getRequiredCasInterface() {
    return CAS.class;
  }

  /**
   * Overriden to check that <code>aCAS</code> is an instanceof {@link CAS}. If it is, then
   * {@link #computeFlow(CAS)} is called. If not, an exception is thrown.
   * 
   * @see FlowController#computeFlow(AbstractCas)
   */
  public final Flow computeFlow(AbstractCas aCAS) throws AnalysisEngineProcessException {
    if (aCAS instanceof CAS) {
      checkTypeSystemChange((CAS) aCAS);
      return computeFlow((CAS) aCAS);
    } else {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { CAS.class,
                  aCAS.getClass() });
    }
  }

  /**
   * This method must be overriden by subclasses. It takes a {@link CAS} and returns a {@link Flow}
   * object that is responsible for routing this particular CAS through the components of this
   * Aggregate. The <code>Flow</code> object should be given a handle to the CAS, so that it can
   * use information in the CAS to make routing decisions.
   * <p>
   * FlowController implementations will typically define their own class that implements
   * {@link Flow} by extending from the base class {@link CasFlow_ImplBase}. This method would then
   * just instantiate the flow object, call its <code>setCas</code> method to provide a handle to
   * the CAS, and return the flow object.
   * 
   * @param aCAS
   *          the CAS to be routed
   * 
   * @return a Flow object that will be used to route <code>aCAS</code>
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   * @see FlowController#computeFlow(AbstractCas)
   */
  public abstract Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException;

  /**
   * This method may be overriden by subclasses. It is called whenever th TypeSystem of the CAS
   * changes. This method will be called immediately prior to the first call to
   * {@link #computeFlow(CAS)}, and will be called again whenever the CAS TypeSystem changes.
   * <p>
   * In this method, the FlowController can use the {@link TypeSystem} to resolve the names of Type
   * and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used to access information from
   * the CAS during routing.
   * 
   * @param aTypeSystem
   *          the typesystem
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
    // no default behavior
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#typeSystemChanged(org.apache.uima.core.AbstractCas)
   */
  private void checkTypeSystemChange(CAS aCAS) throws AnalysisEngineProcessException {
    TypeSystem typeSystem = aCAS.getTypeSystem();
    if (typeSystem != mLastTypeSystem) {
      typeSystemInit(typeSystem);
      mLastTypeSystem = typeSystem;
    }
  }
}
