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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.service.impl.ResourceService_impl;
import org.apache.uima.util.CasPool;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Convenience base class for Analysis Engine Services. Analysis Engine services are not required to
 * extends this class, but it it useful for those services that communicate using binary data.
 * 
 * 
 */
public class AnalysisEngineService_impl extends ResourceService_impl {

  /**
   * Pool of CASes that will be used by this service.
   */
  private CasPool mCasPool;

  /**
   * Timeout period, in milliseocnds, to wait when attempting to get CAS from the pool.
   */
  private int mTimeout;

  /**
   * Initialize this service. This is where the CAS pool is created.
   * 
   * @see org.apache.uima.resource.service.impl.ResourceService_impl#initialize(ResourceSpecifier, Map)
   */
  public void initialize(ResourceSpecifier aResourceSpecifier, Map<String, Object> aResourceInitParams)
          throws ResourceInitializationException {
    super.initialize(aResourceSpecifier, aResourceInitParams);
    Integer numInstances = (Integer) aResourceInitParams
            .get(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS);
    if (numInstances == null) {
      numInstances = Integer.valueOf(1);
    }
    mCasPool = new CasPool(numInstances.intValue(), getAnalysisEngine());

    // also record timeout period to use for CAS pool
    Integer timeoutInteger = (Integer) aResourceInitParams.get(AnalysisEngine.PARAM_TIMEOUT_PERIOD);
    if (timeoutInteger != null) {
      mTimeout = timeoutInteger.intValue();
    } else {
      mTimeout = 0;
    }
  }

  /**
   * An alternative form of initialize that takes the number of simultaneous requests and timeout
   * period as explicit arguments.
   * @param aResourceSpecifier -
   * @param aNumSimultaneousRequests - 
   * @param aTimeout -
   * @throws ResourceInitializationException -
   */
  public void initialize(ResourceSpecifier aResourceSpecifier, int aNumSimultaneousRequests,
          int aTimeout) throws ResourceInitializationException {
    Map<String, Object> initParams = new HashMap<String, Object>();
    initParams.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, Integer.valueOf(
            aNumSimultaneousRequests));
    initParams.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, Integer.valueOf(aTimeout));
    this.initialize(aResourceSpecifier, initParams);
  }

  /**
   * Processes an entity.
   * 
   * @param aData
   *          data to be processed
   * @param aResultSpec
   *          specifies which results the Analysis Engine should produce
   * @throws ResourceServiceException -
   * @return the results of analysis
   */
  public ServiceDataCargo process(ServiceDataCargo aData, ResultSpecification aResultSpec)
          throws ResourceServiceException {
    ProcessTrace trace = aData.getProcessTrace();
    if (trace == null) {
      trace = new ProcessTrace_impl();
    }

    String resourceName = getMetaData().getName();
    trace.startEvent(resourceName, ProcessTraceEvent.SERVICE, "");

    CAS cas = null;
    String resultMessage = "success";
    try {
      // get CAS instance from pool
      cas = getCasFromPool(mTimeout);

      if (cas == null) {
        throw new ResourceServiceException(ResourceServiceException.RESOURCE_UNAVAILABLE,
                new Object[0]);
      }

      // deserialize CAS data into this CAS instance
      aData.unmarshalCas(cas, true);

      // run the AnalysisEngine's process method
      getAnalysisEngine().process(cas, aResultSpec, trace);

      // return results
      return new ServiceDataCargo(cas, trace);
    } catch (Exception e) {
      resultMessage = e.getLocalizedMessage();
      if (e instanceof ResourceServiceException) {
        throw (ResourceServiceException) e;
      } else {
        throw new ResourceServiceException(e);
      }
    } finally {
      // release CAS
      if (cas != null) {
        getCasPool().releaseCas(cas);
      }
      trace.endEvent(resourceName, ProcessTraceEvent.SERVICE, resultMessage);
    }
  }

  /**
   * Gets the AnalysisEngine that delivers the functionality for this service.
   * 
   * @return the AnalysisEngine
   */
  public AnalysisEngine getAnalysisEngine() {
    return (AnalysisEngine) getResource();
  }

  /**
   * @see org.apache.uima.resource.service.impl.ResourceService_impl#getResourceClass()
   */
  protected Class<? extends Resource> getResourceClass() {
    return AnalysisEngine.class;
  }

  /**
   * Gets the CAS pool used by this service.
   * 
   * @return the CAS pool
   */
  protected CasPool getCasPool() {
    return mCasPool;
  }

  /**
   * Gets a CAS from the CAS Pool. Throws an exception if the timeout period elapses.
   * 
   * @param aTimeout
   *          maximum time to wait in milliseconds
   * 
   * @return a CAS that has been checked-out of the pool
   * 
   * @throws ResourceServiceException
   *           if a CAS is not available within the timeout period.
   */
  protected CAS getCasFromPool(int aTimeout) throws ResourceServiceException {
    CAS cas = getCasPool().getCas(aTimeout);
    if (cas == null) {
      throw new ResourceServiceException(ResourceServiceException.RESOURCE_UNAVAILABLE, null);
    }
    return cas;
  }
}
