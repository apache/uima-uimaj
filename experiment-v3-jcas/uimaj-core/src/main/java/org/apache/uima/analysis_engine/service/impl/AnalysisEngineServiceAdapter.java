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

import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.analysis_engine.impl.EmptyCasIterator;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.UimaTimer;

/**
 * Base class for analysis engine service adapters. Implements the {@link AnalysisEngine} interface
 * by communicating with an Analysis Engine service. This insulates the application from having to
 * know whether it is calling a local AnalysisEngine or a remote service.
 * <p>
 * Subclasses must provide an implementation of the {@link #initialize(ResourceSpecifier,Map)}
 * method, which must create an {@link AnalysisEngineServiceStub} object that can communicate with the
 * remote service. The stub must be passed to the {@link #setStub(AnalysisEngineServiceStub)} method of
 * this class.
 * 
 * 
 */
public abstract class AnalysisEngineServiceAdapter extends AnalysisEngineImplBase implements
        TextAnalysisEngine, CasConsumer {

  /**
   * current class
   */
  private static final Class<AnalysisEngineServiceAdapter> CLASS_NAME = AnalysisEngineServiceAdapter.class;

  /**
   * The stub that communicates with the remote service.
   */
  private AnalysisEngineServiceStub mStub;

  /**
   * The resource metadata, cached so that service does not have to be called each time metadata is
   * needed.
   */
  private ResourceMetaData mCachedMetaData;

  /**
   * Timer for collecting performance statistics.
   */
  private UimaTimer mTimer = UIMAFramework.newTimer();

  /**
   * Sets the stub to be used to communicate with the remote service. Subclasses must call this from
   * their <code>initialize</code> method.
   * 
   * @param aStub
   *          the stub for the remote service
   */
  protected void setStub(AnalysisEngineServiceStub aStub) {
    mStub = aStub;
  }

  /**
   * Gets the stub to be used to communicate with the remote service.
   * 
   * @return the stub for the remote service
   */
  protected AnalysisEngineServiceStub getStub() {
    return mStub;
  }

  /**
   * @see org.apache.uima.resource.Resource#getMetaData()
   */
  public ResourceMetaData getMetaData() {
    try {
      if (mCachedMetaData == null && getStub() != null) {
        mCachedMetaData = getStub().callGetMetaData();
      }
      return mCachedMetaData;
    } catch (ResourceServiceException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (getStub() != null)
      getStub().destroy();
    super.destroy();
  }

  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    // logging and instrumentation
    mTimer.startIt();
    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "process",
            LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_begin__FINE", getResourceName());
    try {
      // invoke service
      getStub().callProcess(aCAS);

      // log end of event
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "process",
              LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_end__FINE", getResourceName());

      // we don't support CasMultiplier services yet, so this always returns
      // an empty iterator
      return new EmptyCasIterator();
    } catch (Exception e) {
      // log exception
      UIMAFramework.getLogger(CLASS_NAME).log(Level.SEVERE, "", e);
      // rethrow as AnalysisEngineProcessException
      throw new AnalysisEngineProcessException(e);
    } finally {
      mTimer.stopIt();
      getMBean().reportServiceCallTime((int) mTimer.getDuration());
      getMBean().incrementCASesProcessed();
    }
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String,
   *      java.lang.String)
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return getMetaData().getConfigurationParameterSettings().getParameterValue(aGroupName,
            aParamName);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String)
   */
  public Object getConfigParameterValue(String aParamName) {
    return getMetaData().getConfigurationParameterSettings().getParameterValue(aParamName);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setConfigParameterValue(String aParamName, Object aValue) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   *      java.lang.String, java.lang.Object)
   */
  public void setConfigParameterValue(String aGroupName, String aParamName, Object aValue) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  public void batchProcessComplete() throws AnalysisEngineProcessException {
    try {
      getStub().callBatchProcessComplete();
    } catch (ResourceServiceException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    try {
      getStub().callCollectionProcessComplete();
    } catch (ResourceServiceException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * Gets the name of this resource, for use in logging and trace messages.
   * 
   * @return the name of this resource
   */
  protected String getResourceName() {
    return getMetaData().getName();
  }
}
