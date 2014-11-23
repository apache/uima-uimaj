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

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.internal.util.InstrumentationFacility_impl;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.Session;
import org.apache.uima.resource.impl.Session_impl;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Settings;

/**
 * Implementation of the root {@link UimaContext}. UIMA Contexts are arranged in a tree structure
 * corresponding to the nested structure of the components in a CPE or Aggregate AE. The root
 * UimaContext has direct references to shared components such as the ResourceManager and
 * ConfigurationManager. The children UimaContexts reference these through the root.
 */
public class RootUimaContext_impl extends UimaContext_ImplBase {

  /**
   * Logger
   * 
   * Volatile because mLogger can be updated on one thread and accessed on another
   * 
   */
  private volatile Logger mLogger;

  /**
   * ResourceManager used to locate and access external resources
   */
  private final ResourceManager mResourceManager;

  /**
   * ConfigurationManager used to access configuration parameter settings
   */
  private final ConfigurationManager mConfigurationManager;

  /**
   * Instrumentation Facility (wraps ProcessTrace)
   */
  final private InstrumentationFacility_impl mInstrumentationFacility = new InstrumentationFacility_impl(
          null);

  /**
   * Current Session
   * 
   * Has general setter and getter;
   * marked volatile to allow effect of setting to be seen on another thread
   */
  private volatile Session mSession;
  
  /**
   * External parameter override specifications - held at the root context level
   */
  protected volatile Settings mExternalOverrides;
  
  public Settings getExternalOverrides() {
    return mExternalOverrides;
  }
  
  public void setExternalOverrides(Settings externalOverrides) {
    mExternalOverrides = externalOverrides;
  }

  public RootUimaContext_impl() {
    // ugly trick - passing parameters in thread local of one known caller,
    // to allow these to be final,
    // which causes a store memory barrier to be inserted for them
    // which makes other accesses to them "safe" from other threads
    //   without further synchronization
    mResourceManager = UIMAFramework.newContextResourceManager.get();
    mConfigurationManager = UIMAFramework.newContextConfigManager.get();
  }
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#initialize(org.apache.uima.resource.ResourceCreationSpecifier,
   *      org.apache.uima.util.Logger, org.apache.uima.resource.ResourceManager,
   *      ConfigurationManager)
   */
  public void initializeRoot(Logger aLogger, ResourceManager aResourceManager,
          ConfigurationManager aConfigurationManager) {
    mLogger = aLogger;
//    mResourceManager = aResourceManager;
//    mConfigurationManager = aConfigurationManager;
    mSession = new Session_impl();
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getLogger()
   */
  public Logger getLogger() {
    return mLogger;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#setLogger(org.apache.uima.util.Logger)
   */
  public void setLogger(Logger aLogger) {
    mLogger = aLogger;
  }

  /**
   * Gets the ResourceManager used by this UimaContext to locate and access external resources
   * 
   * @return the ResourceManager
   */
  public ResourceManager getResourceManager() {
    return mResourceManager;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#getConfigurationManager()
   */
  public ConfigurationManager getConfigurationManager() {
    return mConfigurationManager;
  }

  /**
   * Gets the InstrumentationFacility to be used within this AnalysisEngine.
   * 
   * @return the InstrumentationFacility to be used within this AnalysisEngine
   */
  public InstrumentationFacility getInstrumentationFacility() {
    return mInstrumentationFacility;
  }

  /**
   * Sets the current ProcessTrace object, which will receive trace events generated by the
   * InstrumentationFacility.
   * <p>
   * This method is to be called from the Analysis Engine, not the Annotator, so it is not part of
   * the AnnotatorContext interface.
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mInstrumentationFacility.setProcessTrace(aProcessTrace);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#setSession(org.apache.uima.resource.Session)
   */
  public void setSession(Session aSession) {
    mSession = aSession;
    mConfigurationManager.setSession(mSession);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getSession()
   */
  public Session getSession() {
    return mSession;
  }

  /**
   * Get the Root Context
   * 
   * @return root context
   */
  public UimaContextAdmin getRootContext() {
    return this;
  }
}
