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

/**
 * Base interface for annotators in UIMA SDK v1.x. As of v2.0, annotators should extend
 * {@link org.apache.uima.analysis_component.CasAnnotator_ImplBase} or
 * {@link org.apache.uima.analysis_component.JCasAnnotator_ImplBase}.
 * 
 * @deprecated As of release 2.3.0, use CasAnnotator_ImplBase or JCasAnnotator_ImplBase instead
 * @forRemoval 4.0.0
 */
@Deprecated(since = "2.3.0")
public abstract class GenericAnnotator_ImplBase extends Annotator_ImplBase
        implements GenericAnnotator {
  // overrides no methods
}
