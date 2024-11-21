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

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.jcas.JCas;

/**
 * Interface for JCas annotators in UIMA SDK v1.x. As of v2.0, JCAS annotators should extend
 * {@link org.apache.uima.analysis_component.JCasAnnotator_ImplBase}.
 * 
 * @deprecated As of release 2.3.0, use JCasAnnotator_ImplBase instead
 * @forRemoval 4.0.0
 */
@Deprecated(since = "2.3.0")
public interface JTextAnnotator extends BaseAnnotator {
  /**
   * Invokes this annotator's analysis logic. Prior to calling this method, the caller must ensure
   * that the {@link JCas} has been populated with the document to be analyzed as well as any
   * information that this annotator needs to do its processing. This annotator will access the data
   * in the JCas and add new data to the JCas.
   * <p>
   * The caller must also guarantee that the {@link ResultSpecification} falls within the scope of
   * the {@link org.apache.uima.resource.metadata.Capability Capabilities} of this annotator (as
   * published by its containing AnalysisEngine).
   * <p>
   * The annotator will only produce the output types and features that are declared in the
   * <code>aResultSpec</code> parameter.
   * 
   * @param aJCas
   *          contains the document to be analyzed and may contain other metadata about that
   *          document.
   * @param aResultSpec
   *          A list of output types and features that this annotator should produce.
   * 
   * @throws AnnotatorProcessException
   *           if a failure occurs during processing.
   */
  void process(JCas aJCas, ResultSpecification aResultSpec) throws AnnotatorProcessException;
}
