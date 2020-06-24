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

package org.apache.uima.impl;

import java.util.Map;

import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.Session;
import org.apache.uima.resource.impl.SessionNamespaceView_impl;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;

/**
 * Reference implementation of {@link AnnotatorContext}.
 * 
 * 
 */
public class ChildUimaContext_impl extends UimaContext_ImplBase implements UimaContextAdmin {
  /**
   * Logger
   */
  private volatile Logger mLogger;

  /**
   * Root Context (if root, points to self)
   */
  private final UimaContextAdmin mRootContext;

  /**
   * This Context's view of the Session object
   */
  private final SessionNamespaceView_impl mSessionNamespaceView;

  /**
   * ResourceManager used to locate and access external resources
   * Set non-null only for Pear resources contained in an aggregate
   */
  
  private volatile ResourceManager mPearResourceManager = null;

  /**
   * ref to the parent.  
   * This is only used to find containing resource managers
   * that may exist due to Pear Wrappers
   *
   */
  private final UimaContextAdmin parentContext;

  /*
   * (non-Javadoc) Creates a child context.
   */
  public ChildUimaContext_impl(UimaContextAdmin aParentContext, String aContextName,
//IC see: https://issues.apache.org/jira/browse/UIMA-1452
          Map<String, String> aSofaMappings) {
//IC see: https://issues.apache.org/jira/browse/UIMA-3693
//IC see: https://issues.apache.org/jira/browse/UIMA-3694
    super(aParentContext.getQualifiedContextName() + aContextName + '/', aSofaMappings);
//IC see: https://issues.apache.org/jira/browse/UIMA-48
    mRootContext = aParentContext.getRootContext();
    mLogger = aParentContext.getRootContext().getLogger();
    mSessionNamespaceView = new SessionNamespaceView_impl(mRootContext.getSession(),
            mQualifiedContextName);
//IC see: https://issues.apache.org/jira/browse/UIMA-1107
    parentContext = aParentContext;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#initialize(org.apache.uima.resource.ResourceCreationSpecifier,
   *      org.apache.uima.util.Logger, org.apache.uima.resource.ResourceManager,
   *      ConfigurationManager)
   */
  @Override
  public void initializeRoot(Logger aLogger, ResourceManager aResourceManager,
//IC see: https://issues.apache.org/jira/browse/UIMA-48
          ConfigurationManager aConfigurationManager) {
    throw new UIMA_UnsupportedOperationException();
  }

  /**
   * Gets the InstrumentationFacility to be used within this AnalysisEngine.
   * 
   * @return the InstrumentationFacility to be used within this AnalysisEngine
   */
  @Override
  public InstrumentationFacility getInstrumentationFacility() {
    return getRootContext().getInstrumentationFacility();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#setLogger(org.apache.uima.util.Logger)
   */
  @Override
  public void setLogger(Logger aLogger) {
//IC see: https://issues.apache.org/jira/browse/UIMA-5324
    mLogger = maybeThrottleLogger(aLogger);
  }

  /**
   * Gets the ResourceManager used by this UimaContext to locate and access external resources
   * 
   * @return the ResourceManager
   */
  @Override
  public ResourceManager getResourceManager() {
//IC see: https://issues.apache.org/jira/browse/UIMA-1107
    if (null == mPearResourceManager) {
      return parentContext.getResourceManager();
    }
    return mPearResourceManager;
  }

  /**
   * Set the Pear resource manager, to be used instead of any
   * containing Resource Manager.
   * @param resourceManager -
   */
  public void setPearResourceManager(ResourceManager resourceManager) {
    mPearResourceManager = resourceManager;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#getConfigurationManager()
   */
  @Override
  public ConfigurationManager getConfigurationManager() {
    return getRootContext().getConfigurationManager();
  }

  /**
   * Sets the current ProcessTrace object, which will receive trace events generated by the
   * InstrumentationFacility.
   * <p>
   * This method is to be called from the Analysis Engine, not the Annotator, so it is not part of
   * the AnnotatorContext interface.
   */
  @Override
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    getRootContext().setProcessTrace(aProcessTrace);
  }

  /**
   * Get the Root Context
   * 
   * @return root context
   */
  @Override
  public UimaContextAdmin getRootContext() {
    return mRootContext;
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getLogger()
   */
  @Override
  public Logger getLogger() {
    return mLogger;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getSession()
   */
  @Override
  public Session getSession() {
    // must update root session first, in case it has been changed, for example by the deployment
    // wrapper
    mSessionNamespaceView.setRootSession(getRootContext().getSession());
    return mSessionNamespaceView;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#setSession(org.apache.uima.resource.Session)
   */
  @Override
  public void setSession(Session aSession) {
    throw new UIMA_UnsupportedOperationException();

  }
}
