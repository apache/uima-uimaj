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

package org.apache.uima.analysis_engine.impl;

import java.util.Properties;

import org.apache.uima.analysis_engine.AnalysisProcessData;
import org.apache.uima.cas.CAS;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Reference implementation of {@link AnalysisProcessData_impl}.
 * 
 * @deprecated since v2.0
 */
@Deprecated
public class AnalysisProcessData_impl implements AnalysisProcessData {

  /**
   * The CAS owned by this AnalysisProcessData. This reference does not change during the life of
   * this AnalysisProcessData.
   */
  protected CAS mCAS;

  /**
   * The ProcessTrace owned by this AnalysisProcessData. This reference may change during the life
   * of this AnalysisProcessData. once and does not change.
   */
  protected ProcessTrace mProcessTrace;

  /**
   * Creates a new AnalysisProcessData_impl from exsiting {@link CAS}. and {@link ProcessTrace}
   * objects.
   * @param aCAS -
   * @param aTrace -
   */
  public AnalysisProcessData_impl(CAS aCAS, ProcessTrace aTrace) {
    mCAS = aCAS;
    mProcessTrace = aTrace;
  }

  /**
   * Creates a new AnalysisProcessData_impl from an exsiting {@link CAS}. A new
   * {@link ProcessTrace} will be created.
   * 
   * @param aCAS -
   * @param aPerformanceTuningSettings
   *          performance tuning settings used to configure ProcessTrace.
   */
  public AnalysisProcessData_impl(CAS aCAS, Properties aPerformanceTuningSettings) {
    mCAS = aCAS;
    mProcessTrace = new ProcessTrace_impl(aPerformanceTuningSettings);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisProcessData#getCAS()
   */
  public CAS getCAS() {
    return mCAS;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisProcessData#getProcessTrace()
   * @return -
   */
  public ProcessTrace getProcessTrace() {
    return mProcessTrace;
  }

  /**
   * Sets the ProcessTrace object. This is not available through the AnalysisProcessData interface.
   * @param aProcessTrace -
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mProcessTrace = aProcessTrace;
  }
}
