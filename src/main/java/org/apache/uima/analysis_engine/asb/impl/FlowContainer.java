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
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.Step;
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

  public FlowContainer newCasProduced(CAS newCAS, String producedBy)
          throws AnalysisEngineProcessException {
    mTimer.startIt();
    try {
      // set the current component info of the CAS, so that it knows the sofa
      // mappings for the component that's about to process it (the FlowController)
      newCAS.setCurrentComponentInfo(mFlowControllerContainer.getUimaContextAdmin()
              .getComponentInfo());

      // Get the right view of the CAS. Sofa-aware components get the base CAS.
      // Sofa-unaware components get whatever is mapped to the default text sofa.
      CAS view = ((CASImpl) newCAS).getBaseCAS();
      if (!mSofaAware) {
        view = newCAS.getView(CAS.NAME_DEFAULT_SOFA);
      }
      // now get the right interface(e.g. CAS or JCAS)
      Class requiredInterface = mFlowControllerContainer.getRequiredCasInterface();
      AbstractCas casToPass = getCasManager().getCasInterface(view, requiredInterface);

      ((CASImpl)newCAS).switchClassLoaderLockCas(mFlow);
      Flow flow = mFlow.newCasProduced(casToPass, producedBy);
      return new FlowContainer(flow, mFlowControllerContainer, newCAS);
    } finally {
      ((CASImpl)newCAS).restoreClassLoaderUnlockCas();
      newCAS.setCurrentComponentInfo(null);
      mTimer.stopIt();
      getMBean().reportAnalysisTime(mTimer.getDuration());
      getMBean().incrementCASesProcessed();
    }
  }

  public Step next() throws AnalysisEngineProcessException {
    mTimer.startIt();
    try {
      mCAS.switchClassLoaderLockCas(mFlow);
      return mFlow.next();
    } finally {
      mCAS.restoreClassLoaderUnlockCas();
      mTimer.stopIt();
      getMBean().reportAnalysisTime(mTimer.getDuration());
    }
  }
  
  public void aborted() {
    try {
      mCAS.switchClassLoaderLockCas(mFlow);
    mFlow.aborted();
    } finally {
      mCAS.restoreClassLoaderUnlockCas();
    }
  }
  
  public boolean continueOnFailure(String failedAeKey, Exception failure) {
    try {
      mCAS.switchClassLoaderLockCas(mFlow);
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
   */
  public AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) mFlowControllerContainer.getUimaContextAdmin()
            .getManagementInterface();
  }
}
