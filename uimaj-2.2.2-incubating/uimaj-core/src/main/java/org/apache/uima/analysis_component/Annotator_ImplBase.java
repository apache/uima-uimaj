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

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;

/**
 * Base class for all annotators. An Annotator is an {@link AnalysisComponent} that may modify its
 * input CAS, but never creates any new CASes as output. Typically, annotators should not extend
 * this class directly, but instead extend {@link CasAnnotator_ImplBase} or
 * {@link JCasAnnotator_ImplBase} depending on which CAS interface they wish to use.
 */
public abstract class Annotator_ImplBase extends AnalysisComponent_ImplBase {
  /**
   * Returns 0, since annotators are not allowed to create new CAS instances. Only CasMultipliers
   * are allowed to do this.
   */
  public final int getCasInstancesRequired() {
    return 0;
  }

  /**
   * Returns false, since annotators are not allowed to create new CAS instances. Only
   * CasMultipliers are allowed to do this.
   */
  public final boolean hasNext() throws AnalysisEngineProcessException {
    return false;
  }

  /**
   * Throws a UIMA_IllegalStateException, since annotators are not allowed to create new CAS
   * instances. Only CasMultipliers are allowed to do this.
   */
  public final AbstractCas next() throws AnalysisEngineProcessException {
    throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS, null);
  }
}
