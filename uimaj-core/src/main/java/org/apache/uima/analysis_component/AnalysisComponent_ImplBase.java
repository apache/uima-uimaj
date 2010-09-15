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

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Implementation base class for AnalysisComponents. Normally developers do not extend this class
 * directly. Instead extend one of the Annotator or CasMultiplier base classes.
 * <p>
 * This class implements the {@link #initialize(UimaContext)} method and stores the
 * <code>UimaContext</code> in a private field where it can be accessed via the
 * {@link #getContext()} method.
 * <p>
 * This class also provides a "dumb" implementation of the {@link #reconfigure()} method, which
 * simply calls {@link #destroy()} followed by {@link #initialize(UimaContext)}. Developers of
 * AnalysisComponents with expensive initialization logic should override this method and provide a
 * more intelligent implementation.
 */
public abstract class AnalysisComponent_ImplBase implements AnalysisComponent {
  private UimaContext mContext;

  private ResultSpecification mResultSpecification;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.AnalysisComponent.AnalysisComponent#initialize(org.apache.uima.AnalysisComponent.AnalysisComponentContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    mContext = aContext;
  }

  /**
   * Notifies this AnalysisComponent that its configuration parameters have changed. This
   * implementation just calls {@link #destroy()} followed by {@link #initialize(UimaContext)}. Subclasses can
   * override to provide more efficient reconfiguration logic if necessary.
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException, ResourceInitializationException {
    destroy();
    initialize(getContext());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.AnalysisComponent.AnalysisComponent#batchProcessComplete()
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    // no default behavior
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.AnalysisComponent.AnalysisComponent#collectionProcessComplete()
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // no default behavior
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.AnalysisComponent.AnalysisComponent#destroy()
   */
  public void destroy() {
    // no default behavior
  }

  /**
   * Sets the Result Specification for this Analysis Component. This implementation just saves the
   * Result Specification to a field, where it can later be accessed by calling
   * {@link #getResultSpecification()}. An AnalysisComponent implementation may override this
   * method if it would like to do specific processing when its ResultSpecificatin is changed.
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#setResultSpecification(ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    mResultSpecification = aResultSpec;
  }

  /**
   * Gets the UimaContext for this AnalysisComponent. This provides access to configuration
   * parameters and external resources.
   * 
   * @return the UimaContext for this AnalysisComponent
   */
  protected final UimaContext getContext() {
    if (null == mContext) {
      // wrapped in RuntimeException because we don't want to change the API of this method
      throw new UIMARuntimeException(UIMARuntimeException.UIMA_CONTEXT_NULL, new Object[] {} );
    }    
    return mContext;
  }

  /**
   * Gets the ResultSpecification for this AnalysisComponent. The ResultSpecification is a set of
   * types and features that this AnalysisComponent is asked to produce. An Analysis Component may
   * (but is not required to) optimize its processing by omitting the generation of any types or
   * features that are not part of the ResultSpecification.
   * 
   * @return the ResultSpecification for this Analysis Component to use.
   */
  protected ResultSpecification getResultSpecification() {
    if (null == mResultSpecification) {
      // wrapped in RuntimeException because we don't want to change the API of this method
      throw new UIMARuntimeException(UIMARuntimeException.RESULT_SPEC_NULL, new Object[] {} );
    }
    return mResultSpecification;
  }
}
