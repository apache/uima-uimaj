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

package org.apache.uima.analysis_engine.service.impl;

import java.io.Serializable;

import org.apache.uima.analysis_engine.AnalysisProcessData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.util.ProcessTrace;

/**
 * A serializable class containing the information passed to and returned from Analysis Engine
 * Services.
 * <p>
 * It is not required that Analysis Engine Services use this class. It is provided as a convenience
 * for those services that communicate using binary data.
 * <p>
 * This object contains state information extracted from an {@link AnalysisProcessData}. The
 * <code>AnalysisProcessData</code> object itself is not serializable, because it contains the
 * <code>CAS</code> object. CASes are heavyweight objects and should not be created and destroyed
 * with each network call.
 * <p>
 * Instead, to pass Analysis Process Data to a remote service, one should create a
 * <code>ServiceDataCargo</code> and send that to the remote service.
 * <p>
 * A <code>ServiceDataCargo</code> can be unmarshalled into an existing
 * <code>AnalysisProcessData</code> by calling the {@link #unmarshalInto(AnalysisProcessData, boolean)}
 * method. Alternatively, the CAS state can be unmarshalled separately by calling the
 * {@link #unmarshalCas(CAS, boolean)} method.
 * 
 * 
 */
public class ServiceDataCargo implements Serializable {
  
  private static final long serialVersionUID = 2433836175315405277L;

  private CASCompleteSerializer mCasSer;

  private ProcessTrace mProcessTrace;

  /**
   * Creates a new <code>SerializableAnalysisProcessData</code> that contains information
   * extracted from the specified <code>AnalysisProcessData</code>.
   * 
   * @param aData
   *          the AnalysisProcessData to extract from
   */
  public ServiceDataCargo(AnalysisProcessData aData) {
    mCasSer = Serialization.serializeCASComplete((CASMgr) aData.getCAS());
    mProcessTrace = aData.getProcessTrace();
  }

  /**
   * Creates a new <code>SerializableAnalysisProcessData</code> that contains the given
   * <code>CAS</code> and <code>ProcessTrace</code>.
   * 
   * @param aCAS
   *          the CAS whose state will be extracted into this object
   * @param aProcessTrace
   *          the process trace object. This may be null, if no process trace is available. (For
   *          example, ProcessTrace data may often be returned from a service but not passed to the
   *          service.)
   */
  public ServiceDataCargo(CAS aCAS, ProcessTrace aProcessTrace) {
    mCasSer = Serialization.serializeCASComplete((CASMgr) aCAS);
    mProcessTrace = aProcessTrace;
  }

  /**
   * Unmarshalls this <code>SerializableAnalysisProcessData</code> into an existing
   * <code>AnalysisProcessData</code> object. The existing CAS data in the
   * <code>aDataContainer</code> object will be replaced by the CAS data in this object. The
   * <code>ProcessTrace</code> events in this object will be appended to the
   * <code>ProcessTrace</code> of the <code>aDataContainer</code> object.
   * 
   * @param aDataContainer
   *          the AnalysisProcessData to unmarshal into
   * @param aReplaceCasTypeSystem -
   * @throws CASException -
   */
  public void unmarshalInto(AnalysisProcessData aDataContainer, boolean aReplaceCasTypeSystem)
          throws CASException {
    unmarshalCas(aDataContainer.getCAS(), aReplaceCasTypeSystem);
    aDataContainer.getProcessTrace().addAll(mProcessTrace.getEvents());
  }

  /**
   * Unmarshalls the CAS data in this <code>ServiceDataCargo</code> into an existing
   * <code>CAS</code> instance. The data in the exsiting CAS will be replaced by the CAS data in
   * this object.
   * 
   * @param aCas the CAS to unmarshal into
   * @param aReplaceCasTypeSystem if true, assumes serialized data contains the type system
   * @throws CASException passthru
   */
   public void unmarshalCas(CAS aCas, boolean aReplaceCasTypeSystem) throws CASException {
    CASMgr casMgr = (CASMgr) aCas;
    if (aReplaceCasTypeSystem) {
      Serialization.deserializeCASComplete(mCasSer, casMgr);
    } else {
      Serialization.createCAS(casMgr, mCasSer.getCASSerializer());
    }
  }

  /**
   * Gets the ProcessTrace object from this <code>ServiceDataCargo</code>. This may return null,
   * if no process trace is available. (For example, ProcessTrace data may often be returned from a
   * service but not passed to the service.)
   * 
   * @return the process trace
   */
  public ProcessTrace getProcessTrace() {
    return mProcessTrace;
  }

  /**
   * Sets the ProcessTrace object from this <code>ServiceDataCargo</code>.
   * 
   * @param aProcessTrace
   *          the process trace
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mProcessTrace = aProcessTrace;
  }
}
