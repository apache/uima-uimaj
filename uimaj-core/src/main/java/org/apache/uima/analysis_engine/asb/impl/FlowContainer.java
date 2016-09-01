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

package org.apache.uima.analysis_engine.asb.impl;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.Step;
import org.apache.uima.impl.Util;
import org.apache.uima.resource.CasManager;
import org.apache.uima.util.UimaTimer;

/**
 * Container for Flow objects, to handle CAS conversions and performance timing.
 */
public class FlowContainer {
  private Flow mFlow;

  private FlowControllerContainer mFlowControllerContainer;

  private boolean mSofaAware;
  
  private CASImpl mCAS;

  private UimaTimer mTimer = UIMAFramework.newTimer();

  public FlowContainer(Flow aFlow, FlowControllerContainer aFlowControllerContainer, CAS aCAS) {
    mFlow = aFlow;
    mFlowControllerContainer = aFlowControllerContainer;
    mSofaAware = mFlowControllerContainer.getProcessingResourceMetaData().isSofaAware();
    mCAS = (CASImpl)aCAS;
  }

  public FlowContainer newCasProduced(final CAS newCAS, String producedBy)
          throws AnalysisEngineProcessException {
    mTimer.startIt();
    CAS view = null;
    try {
      view = Util.getStartingView(   
          newCAS, 
          mSofaAware, 
          mFlowControllerContainer.getUimaContextAdmin().getComponentInfo());
      // now get the right interface(e.g. CAS or JCAS)
      // must be done before call to switchClassLoader
      Class<? extends AbstractCas> requiredInterface = mFlowControllerContainer.getRequiredCasInterface();
      AbstractCas casToPass = getCasManager().getCasInterface(view, requiredInterface);

      ((CASImpl)view).switchClassLoaderLockCasCL(getFlowClassLoader());
      Flow flow = mFlow.newCasProduced(casToPass, producedBy);
      if (flow instanceof CasFlow_ImplBase) {
        ((CasFlow_ImplBase)flow).setCas(view);
      }
      if (flow instanceof JCasFlow_ImplBase) {
        ((JCasFlow_ImplBase)flow).setJCas(view.getJCas());
      }
      return new FlowContainer(flow, mFlowControllerContainer, newCAS);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    } finally {
      newCAS.setCurrentComponentInfo(null);
      if (null != view) {
        ((CASImpl)view).restoreClassLoaderUnlockCas();
      }
      mTimer.stopIt();
      getMBean().reportAnalysisTime(mTimer.getDuration());
      getMBean().incrementCASesProcessed();
    }
  }

  public Step next() throws AnalysisEngineProcessException {
    mTimer.startIt();
    try {
      mCAS.switchClassLoaderLockCasCL(getFlowClassLoader());
      return mFlow.next();
    } finally {
      mCAS.restoreClassLoaderUnlockCas();
      mTimer.stopIt();
      getMBean().reportAnalysisTime(mTimer.getDuration());
    }
  }
  
  public void aborted() {
    try {
      mCAS.switchClassLoaderLockCasCL(getFlowClassLoader());
      mFlow.aborted();
    } finally {
      mCAS.restoreClassLoaderUnlockCas();
    }
  }
  
  public boolean continueOnFailure(String failedAeKey, Exception failure) {
    try {
      mCAS.switchClassLoaderLockCasCL(getFlowClassLoader());
      return mFlow.continueOnFailure(failedAeKey, failure);
    } finally {
      mCAS.restoreClassLoaderUnlockCas();
    }
  }


  private CasManager getCasManager() {
    return mFlowControllerContainer.getResourceManager().getCasManager();
  }

  /**
   * Gets the MBean to use to report performance statistics.
   * @return the MBean to use to report performance statistics
   */
  public AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) mFlowControllerContainer.getUimaContextAdmin()
            .getManagementInterface();
  }
  
  private ClassLoader getFlowClassLoader() {
    return mFlowControllerContainer.getResourceManager().getExtensionClassLoader();
  }
}
