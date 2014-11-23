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
import org.apache.uima.jcas.JCas;

/**
 * Base class to be extended by CAS Multipliers that use the {@link CAS} interface. A CAS Multiplier
 * can produce multiplie output CASes while processing an input CAS. See {@link AnalysisComponent}
 * for a description of how the framework calls the methods on this interface.
 */
public abstract class JCasMultiplier_ImplBase extends AnalysisComponent_ImplBase {
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getRequiredCasInterface()
   */
  public final Class<JCas> getRequiredCasInterface() {
    return JCas.class;
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
    if (aCAS instanceof JCas) {
      process((JCas) aCAS);
    } else {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { JCas.class,
                  aCAS.getClass() });
    }
  }

  /**
   * This method should be overriden by subclasses. Inputs a JCAS to the AnalysisComponent. The
   * AnalysisComponent "owns" this JCAS until such time as {@link #hasNext()} is called and returns
   * false, or until the <code>process</code> method is called again (see
   * {@link AnalysisComponent} for details).
   * 
   * @param aJCas
   *          a JCAS that this AnalysisComponent should process.
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  public abstract void process(JCas aJCas) throws AnalysisEngineProcessException;

  /**
   * Gets an empty JCas that this CAS Multiplier can then populate.
   * 
   * @return an empty JCas
   */
  protected final JCas getEmptyJCas() {
    return getContext().getEmptyCas(JCas.class);
  }
}
