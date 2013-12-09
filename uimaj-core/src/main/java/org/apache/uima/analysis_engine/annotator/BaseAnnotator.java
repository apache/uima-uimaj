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
 * Base interface for annotators in UIMA SDK v1.x. As of v2.0, annotators should extend
 * {@link org.apache.uima.analysis_component.CasAnnotator_ImplBase} or
 * {@link org.apache.uima.analysis_component.JCasAnnotator_ImplBase}.
 */
public interface BaseAnnotator {
  /**
   * Performs any startup tasks required by this annotator. The Analysis Engine calls this method
   * only once, just after an Annotator has been instantiated.
   * <p>
   * The Analysis Engine supplies this annotator with a reference to the {@link AnnotatorContext}
   * that it will use. This annotator should store a reference to its this object for later use.
   * 
   * @param aContext
   *          Provides access to external resources that may be used by this annotator. This
   *          includes configuration parameters, logging and instrumentation services, and access to
   *          external analysis resources.
   * 
   * @throws AnnotatorInitializationException
   *           if the annotator cannot initialize itself.
   * @throws AnnotatorConfigurationException
   *           if the configuration specified for this annotator is invalid.
   */
  public void initialize(AnnotatorContext aContext) throws AnnotatorInitializationException,
          AnnotatorConfigurationException;

  /**
   * Informs this annotator that the CAS TypeSystem has changed. The Analysis Engine calls this
   * method immediately following the call to {@link #initialize(AnnotatorContext)}, and will call
   * it again whenever the CAS TypeSystem changes.
   * <p>
   * In this method, the Annotator should use the {@link TypeSystem} to resolve the names of Type
   * and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used during processing.
   * 
   * @param aTypeSystem the new type system
   * @throws AnnotatorInitializationException
   *           if the annotator cannot initialize itself.
   * @throws AnnotatorConfigurationException
   *           if the configuration specified for this annotator is invalid.
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnnotatorInitializationException,
          AnnotatorConfigurationException;

  /**
   * Alerts this annotator that the values of its configuration parameters or external resources
   * have changed. This annotator should take appropriate action to reconfigure itself.
   * <p>
   * It is suggested that annotators implement this efficiently, but it is not required. Annotators
   * may implement a "dumb" reconfigure by calling <code>destroy</code> followed by
   * <code>initialize</code> and <code>typeSystemInit</code>.
   * 
   * @throws AnnotatorConfigurationException
   *           if the configuration specified for this annotator is invalid.
   * @throws AnnotatorInitializationException
   *           if the annotator fails to reinitialize itself based on the new configuration.
   */
  public void reconfigure() throws AnnotatorConfigurationException,
          AnnotatorInitializationException;

  /**
   * Frees all resources held by this annotator. The Analysis Engine calls this method only once,
   * when it is finished using this annotator.
   */
  public void destroy();
}
