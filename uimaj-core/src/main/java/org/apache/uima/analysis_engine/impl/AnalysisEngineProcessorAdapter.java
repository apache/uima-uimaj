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

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.UimaTimer;

/**
 * Base class for analysis engine processor adapters. Used for embedded (functional) processors.
 */
public abstract class AnalysisEngineProcessorAdapter extends AnalysisEngineImplBase
        implements TextAnalysisEngine, CasConsumer {

  /**
   * current class
   */
  private static final Class<AnalysisEngineProcessorAdapter> CLASS_NAME = AnalysisEngineProcessorAdapter.class;

  /**
   * The stub that talks to the actual implementation.
   */
  private AnalysisEngineProcessorStub mStub;

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
   * Sets the stub to be used to actual implementation. Subclasses must call this from their
   * <code>initialize</code> method.
   * 
   * @param aStub
   *          the stub for the remote service
   */
  protected void setStub(AnalysisEngineProcessorStub aStub) {
    mStub = aStub;
  }

  /**
   * @return the stub to be used to communicate with the remote service.
   */
  protected AnalysisEngineProcessorStub getStub() {
    return mStub;
  }

  @Override
  public ResourceMetaData getMetaData() {

    return getStub() != null ? getStub().getMetaData() : null;
  }

  @Override
  public void destroy() {
    if (getStub() != null) {
      getStub().destroy();
    }
    super.destroy();
  }

  @Override
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    // logging and instrumentation
    mTimer.startIt();
    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "process",
            LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_begin__FINE", getResourceName());
    try {
      // invoke service
      getStub().process(aCAS);

      // log end of event
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "process",
              LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_end__FINE", getResourceName());

      // we don't support CasMultiplier services yet, so this always returns
      // an empty iterator
      return new EmptyCasIterator();
    } catch (AnalysisEngineProcessException e) {
      throw e;
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    } finally {
      mTimer.stopIt();
      getMBean().reportServiceCallTime((int) mTimer.getDuration());
      getMBean().incrementCASesProcessed();
    }
  }

  @Override
  public void reconfigure() throws ResourceConfigurationException {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  @Override
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return getMetaData().getConfigurationParameterSettings().getParameterValue(aGroupName,
            aParamName);
  }

  @Override
  public Object getConfigParameterValue(String aParamName) {
    return getMetaData().getConfigurationParameterSettings().getParameterValue(aParamName);
  }

  @Override
  public void setConfigParameterValue(String aParamName, Object aValue) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  @Override
  public void setConfigParameterValue(String aGroupName, String aParamName, Object aValue) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.SHARED_RESOURCE_NOT_RECONFIGURABLE, new Object[] {});
  }

  @Override
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    getStub().batchProcessComplete();
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    getStub().collectionProcessComplete();
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
