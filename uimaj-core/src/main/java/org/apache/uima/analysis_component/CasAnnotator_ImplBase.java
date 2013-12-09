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
 * Base class to be extended by Annotators that use the {@link CAS} interface. An Annotator is an
 * {@link AnalysisComponent} that may modify its input CAS, but never creates any new CASes as
 * output.
 */
public abstract class CasAnnotator_ImplBase extends Annotator_ImplBase {
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
  public Class<CAS> getRequiredCasInterface() {
    return CAS.class;
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
   * Inputs a CAS to the AnalysisComponent. This method should be overriden by subclasses to perform
   * analysis of the CAS.
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
