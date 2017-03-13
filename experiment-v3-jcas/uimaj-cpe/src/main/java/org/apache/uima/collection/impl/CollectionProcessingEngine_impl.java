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

package org.apache.uima.collection.impl;

import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.cpm.BaseCPMImpl;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.ProcessControllerAdapter;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;


public class CollectionProcessingEngine_impl implements CollectionProcessingEngine {
  /**
   * CPM instance that handles the processing
   */
  private BaseCPMImpl mCPM = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#initialize(org.apache.uima.collection.metadata.cpeDescription,
   *      java.util.Map)
   */
  public void initialize(CpeDescription aCpeDescription, Map aAdditionalParams)
          throws ResourceInitializationException {
    if (mCPM != null) // repeat initialization - not allowed
    {
      throw new UIMA_IllegalStateException(UIMA_IllegalStateException.RESOURCE_ALREADY_INITIALIZED,
              new Object[] { getClass().getName() });
    }

    // get the ResourceManager (if any) supplied in the aAdditionalParams map
    ResourceManager resMgr = aAdditionalParams == null ? null : (ResourceManager) aAdditionalParams
            .get(Resource.PARAM_RESOURCE_MANAGER);

    // get performance tuning settings (if any) supplied in the aAdditionalParams map
    Properties perfSettings = aAdditionalParams == null ? null : (Properties) aAdditionalParams
            .get(Resource.PARAM_PERFORMANCE_TUNING_SETTINGS);

    // get ProcessControllerAdapter responsible for launching fenced services
    ProcessControllerAdapter pca = aAdditionalParams == null ? null
            : (ProcessControllerAdapter) aAdditionalParams.get("ProcessControllerAdapter");
    // instantiate CPM from Descriptor
    try {
      mCPM = new BaseCPMImpl(aCpeDescription, resMgr, false, perfSettings);
      if (perfSettings != null) {
        mCPM.setPerformanceTuningSettings(perfSettings);
      }
      if (pca != null) {
        mCPM.setProcessControllerAdapter(pca);
      }
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#addStatusCallbackListener(org.apache.uima.collection.StatusCallbackListener)
   */
  public void addStatusCallbackListener(StatusCallbackListener aListener) {
    mCPM.addStatusCallbackListener(aListener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#removeStatusCallbackListener(org.apache.uima.collection.StatusCallbackListener)
   */
  public void removeStatusCallbackListener(StatusCallbackListener aListener) {
    mCPM.removeStatusCallbackListener(aListener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#process()
   */
  public void process() throws ResourceInitializationException {
    mCPM.process();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#isProcessing()
   */
  public boolean isProcessing() {
    return mCPM.isProcessing();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#pause()
   */
  public void pause() {
    mCPM.pause();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#isPaused()
   */
  public boolean isPaused() {
    return mCPM.isPaused();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#resume()
   */
  public void resume() {
    mCPM.resume();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#stop()
   */
  public void stop() {
    mCPM.stop();
  }

  public void kill() {
    mCPM.kill();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#stop()
   */
  public void asynchStop() {
    mCPM.asynchStop();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#getPerformanceReport()
   */
  public ProcessTrace getPerformanceReport() {
    return mCPM.getPerformanceReport();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#getProgress()
   */
  public Progress[] getProgress() {
    return mCPM.getProgress();
  }

  protected BaseCPMImpl getCPM() {
    return mCPM;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#getCasProcessors()
   */
  public CasProcessor[] getCasProcessors() {
    return mCPM.getCasProcessors();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionProcessingEngine#getCollectionReader()
   */
  public BaseCollectionReader getCollectionReader() {
    return mCPM.getCollectionReader();
  }

}
