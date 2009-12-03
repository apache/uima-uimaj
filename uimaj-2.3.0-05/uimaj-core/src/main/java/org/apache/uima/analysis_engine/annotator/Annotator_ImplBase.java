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

package org.apache.uima.analysis_engine.annotator;

import org.apache.uima.cas.TypeSystem;

/**
 * Base class for annotators in UIMA SDK v1.x. As of v2.0, annotators should extend
 * {@link org.apache.uima.analysis_component.CasAnnotator_ImplBase} or
 * {@link org.apache.uima.analysis_component.JCasAnnotator_ImplBase}.
 * @deprecated As of release 2.3.0, use CasAnnotator_ImplBase or JCasAnnotator_ImplBase instead
 */
@Deprecated
public abstract class Annotator_ImplBase implements BaseAnnotator {

  /**
   * Stores the <code>AnnotatorContext</code> used by this Annotator.
   */
  private AnnotatorContext mContext;

  /**
   * Stores the last TypeSystem passed to {@link #typeSystemInit(TypeSystem)}.
   */
  private TypeSystem mTypeSystem;

  /**
   * The only thing this implementation does is store the AnnotatorContext so that it can be
   * accessed later via the {@link #getContext()} method.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
   */
  public void initialize(AnnotatorContext aContext) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    mContext = aContext;
  }

  /**
   * The only thing this implementation does is store the TypeSystem so that it can be accessed by
   * the {@link #getTypeSystem()} method, and also so that it can be passed back to the
   * {@link #typeSystemInit(TypeSystem)} method by the default implementation of
   * {@link #reconfigure()}.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    mTypeSystem = aTypeSystem;
  }

  /**
   * This default implementation does nothing.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#destroy()
   */
  public void destroy() {
    // no default behavior
  }

  /**
   * This default implementation calls {@link #destroy()} followed by
   * {@link #initialize(AnnotatorContext)} and {@link #typeSystemInit(TypeSystem)}. The
   * <code>typeSystemInit</code> method will be passed the last known TypeSystem.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#reconfigure()
   */
  public void reconfigure() throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    destroy();
    initialize(getContext());
    if (mTypeSystem != null) {
      typeSystemInit(mTypeSystem);
    }
  }

  /**
   * Gets the <code>AnnotatorContext</code> to be used by this Annotator. The
   * <code>AnnotatorContext</code> provides access to external resources that may be used by this
   * annotator. This includes configuration parameters, logging and instrumentation services, and
   * access to text analysis resources.
   * 
   * @return the Annotator Context
   */
  protected AnnotatorContext getContext() {
    return mContext;
  }

  /**
   * Gets the <code>TypeSystem</code> that was passed to the most recent call to
   * {@link #typeSystemInit(TypeSystem)}.
   * 
   * @return the Annotator Context
   */
  protected TypeSystem getTypeSystem() {
    return mTypeSystem;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    destroy();
  }
}
