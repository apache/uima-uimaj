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

package org.apache.uima.analysis_engine;

import org.apache.uima.cas.CAS;
import org.apache.uima.util.ProcessTrace;

/**
 * Encapsulates all data that is modified by an <code>AnalysisEngine</code>'s
 * {@link AnalysisEngine#process(AnalysisProcessData,ResultSpecification)} method. This currently
 * includes:
 * <ul>
 * <li>The {@link CAS Common Analysis System(CAS)}, from which the AnalysisEngine obtains the
 * information to be processed, and to which the AnalysisEngine writes new annotation information.</li>
 * <li>The {@link ProcessTrace} object, which is used to record which AnalysisEngine components
 * have executed and information, such as timing, about that execution.</li>
 * </ul>
 * <p>
 * In a tightly-coupled system, a single <code>AnalysisProcessData</code> object is shared by
 * multiple AnalysisEngines.
 * <p>
 * In a loosely-coupled system, the <code>AnalysisProcessData</code> object is transmitted between
 * remote AnalysisEngine services.
 * 
 * @deprecated
 */
@Deprecated
public interface AnalysisProcessData {

  /**
   * Gets the Common Analysis System ({@link CAS}), from which the AnalysisEngine obtains the
   * information to be processed, and to which the AnalysisEngine writes new annotation information.
   * 
   * @return a reference to the CAS used by the AnalysisEngine
   */
  public CAS getCAS();

  /**
   * Gets the {@link ProcessTrace} object, which is used to record which AnalysisEngine components
   * have executed and information, such as timing, about that execution.
   * 
   * @return a reference to the <code>ProcessTrace</code> object used by the AnalysisEngine
   */
  public ProcessTrace getProcessTrace();
}
