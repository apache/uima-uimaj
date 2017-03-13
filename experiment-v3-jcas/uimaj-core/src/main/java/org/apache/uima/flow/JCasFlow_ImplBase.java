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


import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.jcas.JCas;

/**
 * Convenience base class for Flow objects that use the JCas interface. Stores the JCas in a field
 * made accessible through the protected {@link #getJCas()} method.
 */
public abstract class JCasFlow_ImplBase implements Flow {
  private JCas mJCas;

  /**
   * Sets the JCas to be routed by this Flow object. This should be called from the
   * {@link FlowController#computeFlow(AbstractCas)} method after this Flow object is instantiated.
   * 
   * @param aJCas
   *          the JCas to be routed by this Flow object
   * @deprecated this is done automatically by the framework with the Flow object is created
   */
  @Deprecated
  public void setJCas(JCas aJCas) {
    mJCas = aJCas;
  }

  /**
   * Overriden to check that <code>newCas</code> is an instanceof {@link JCas}. If it is, then
   * {@link #newCasProduced(JCas,String)} is called. If not, an exception is thrown.
   * 
   * @see Flow#newCasProduced(AbstractCas, String)
   */
  public final Flow newCasProduced(AbstractCas newCas, String producedBy)
          throws AnalysisEngineProcessException {
    if (newCas instanceof JCas) {
      return newCasProduced((JCas) newCas, producedBy);
    } else {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { JCas.class,
                  newCas.getClass() });
    }
  }
  
  /**
   * By default, returns false, indicating that processing cannot continue after a failure.
   * May be overridden by subclasses to allow processing to continue.
   * @see org.apache.uima.flow.Flow#continueOnFailure(String, java.lang.Exception)
   */
  public boolean continueOnFailure(String failedAeKey, Exception failure) {
    return false;
  }
  
  /** 
   * By default, does nothing.  May be overriden by subclasses to release resources 
   * when a flow is aborted.
   * @see Flow#aborted()
   */
  public void aborted() {
    // does nothing by default
  }    

  /**
   * By default, throws an exception to indicate this this Flow object does not support new CASes
   * being produced in the middle of the flow. Subclasses can override to implement handling for
   * this.
   * 
   * @param newCas
   *          the new JCas
   * @param producedBy
   *          the key of the CAS Multiplier that produced this JCas
   * 
   * @return a Flow object that will be used to route the new JCas
   * @throws AnalysisEngineProcessException -
   * @see Flow#newCasProduced(AbstractCas, String)
   * 
   */
  protected Flow newCasProduced(JCas newCas, String producedBy)
          throws AnalysisEngineProcessException {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.CAS_MULTIPLIER_NOT_SUPPORTED, new Object[] { this
                    .getClass().getName() });
  }

  /**
   * Gets the JCas being routed by this Flow object.
   * 
   * @return the JCas being routed by this Flow object
   */
  protected JCas getJCas() {
    return mJCas;
  }
}
