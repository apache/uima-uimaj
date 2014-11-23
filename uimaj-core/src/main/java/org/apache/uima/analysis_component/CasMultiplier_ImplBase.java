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

package org.apache.uima.analysis_component;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/**
 * Base class to be extended by CAS Multipliers that use the {@link CAS} interface. A CAS Multiplier
 * can produce multiple output CASes while processing an input CAS. See {@link AnalysisComponent}
 * for a description of how the framework calls the methods on this interface.
 */
public abstract class CasMultiplier_ImplBase extends AnalysisComponent_ImplBase {
  /**
   * Stores the last type system that this component operated on, so we can tell when typeSystemInit
   * needs to be called.
   */
  private TypeSystem mLastTypeSystem = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getRequiredCasInterface()
   */
  public final Class<CAS> getRequiredCasInterface() {
    return CAS.class;
  }

  /**
   * Returns the maximum number of CAS instances that this CAS Multiplier expects to use at the same
   * time. Returns a default value of 1, which will be sufficient for most CAS Multipliers. Only if
   * there is a clear need should this be overridden to return something greater than 1.
   */
  public int getCasInstancesRequired() {
    return 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#process(org.apache.uima.core.AbstractCas)
   */
  public final void process(AbstractCas aCAS) throws AnalysisEngineProcessException {
    if (aCAS instanceof CAS) {
      checkTypeSystemChange((CAS) aCAS);
      process((CAS) aCAS);
    } else {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { CAS.class,
                  aCAS.getClass() });
    }
  }

  /**
   * This method should be overriden by subclasses. Inputs a CAS to the AnalysisComponent. The
   * AnalysisComponent "owns" this CAS until such time as {@link #hasNext()} is called and returns
   * false, or until the <code>process</code> method is called again (see
   * {@link AnalysisComponent} for details).
   * 
   * @param aCAS
   *          A CAS that this AnalysisComponent should process.
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  public abstract void process(CAS aCAS) throws AnalysisEngineProcessException;

  /**
   * Informs this annotator that the CAS TypeSystem has changed. The Analysis Engine calls this
   * method immediately following the call to {@link #initialize(org.apache.uima.UimaContext)}, and will call
   * it again whenever the CAS TypeSystem changes.
   * <p>
   * In this method, the Annotator should use the {@link TypeSystem} to resolve the names of Type
   * and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used during processing.
   * 
   * @param aTypeSystem the new type system to use as input to your initialization
   * @throws AnalysisEngineProcessException
   *           if the provided type system is missing types or features required by this annotator
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
    // no default behavior
  }

  /**
   * Gets an empty CAS that this CAS Multiplier can then populate.
   * 
   * @return an empty CAS
   */
  protected final CAS getEmptyCAS() {
    return getContext().getEmptyCas(CAS.class);
  }

  /**
   * Checks it the type system of the given CAS is different from the last type system this
   * component was operating on. If it is different, calls the typeSystemInit method on the
   * component.
   */
  private void checkTypeSystemChange(CAS aCAS) throws AnalysisEngineProcessException {
    TypeSystem typeSystem = aCAS.getTypeSystem();
    if (typeSystem != mLastTypeSystem) {
      typeSystemInit(typeSystem);
      mLastTypeSystem = typeSystem;
    }
  }

}
