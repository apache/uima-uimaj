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

import java.util.Collection;
import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowController;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.impl.Util;
import org.apache.uima.internal.util.JmxMBeanAgent;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.UimaTimer;

/**
 * Container for a FlowController. Manages configuration parameters, resources, CAS interface
 * conversions, and performance timing.
 */
public class FlowControllerContainer extends ConfigurableResource_ImplBase {
  private FlowController mFlowController;

  private UimaTimer mTimer = UIMAFramework.newTimer();

  private boolean mSofaAware;

  private Object mMBeanServer;
  
  private boolean initialized = false;
  
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource_ImplBase#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   *      
   * UIMA-5043 Set & restore the UimaContextHolder around calls to user code so it can be used to access the External Settings
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());   // Get this early so the restore in correct
    try {
      // specifier must be a FlowControllerDescription. (Eventually, we
      // might support remote specifiers, but not yet)
      if (!(aSpecifier instanceof FlowControllerDescription)) {
        throw new ResourceInitializationException(
                ResourceInitializationException.NOT_A_FLOW_CONTROLLER_DESCRIPTOR, new Object[] {
                    aSpecifier.getSourceUrlString(), aSpecifier.getClass().getName() });
      }
      ResourceCreationSpecifier desc = (ResourceCreationSpecifier) aSpecifier;

      // also framework implementation must start with org.apache.uima.java
      final String fwImpl = desc.getFrameworkImplementation();
      if (fwImpl == null
              || !fwImpl.equalsIgnoreCase(Constants.JAVA_FRAMEWORK_NAME)) {
        throw new ResourceInitializationException(
                ResourceInitializationException.UNSUPPORTED_FRAMEWORK_IMPLEMENTATION, new Object[] {
                    fwImpl, aSpecifier.getSourceUrlString() });
      }

      super.initialize(aSpecifier, aAdditionalParams);

      // validate the descriptor
      desc.validate(getResourceManager());

      // instantiate FlowController
      mFlowController = instantiateFlowController(desc);

      // record metadata
      setMetaData(desc.getMetaData());

      // add our metadata to the CasManager, so that it will pick up our
      // type system, priorities, and indexes
      getCasManager().addMetaData(getProcessingResourceMetaData());

      // determine if this component is Sofa-aware (based on whether it
      // declares any input or output sofas in its capabilities)
      mSofaAware = getProcessingResourceMetaData().isSofaAware();
      
      // Set Logger, to enable component-specific logging configuration
      UimaContextAdmin uimaContext = getUimaContextAdmin();
      Logger logger = UIMAFramework.getLogger(mFlowController.getClass());
      logger.setResourceManager(this.getResourceManager());
      uimaContext.setLogger(logger);
      
      Logger classLogger = getLogger();
      classLogger.logrb(Level.CONFIG, this.getClass().getName(), "initialize", LOG_RESOURCE_BUNDLE,
              "UIMA_flow_controller_init_begin__CONFIG", getMetaData().getName());
      
      // initialize FlowController
      mFlowController.initialize(getFlowControllerContext());

      mMBeanServer = null;
      String mbeanNamePrefix = null;
      if (aAdditionalParams != null) {
        mMBeanServer = aAdditionalParams.get(AnalysisEngine.PARAM_MBEAN_SERVER);
        mbeanNamePrefix = (String)aAdditionalParams.get(AnalysisEngine.PARAM_MBEAN_NAME_PREFIX);
      }
      // update MBean with the name taken from metadata
      getMBean().setName(getMetaData().getName(), getUimaContextAdmin(), mbeanNamePrefix);
      // register MBean with MBeanServer. If no MBeanServer specified in the
      // additionalParams map, this will use the platform MBean Server
      // (Java 1.5 only)
      JmxMBeanAgent.registerMBean(getMBean(), mMBeanServer);

      classLogger.logrb(Level.CONFIG, this.getClass().getName(), "initialize", LOG_RESOURCE_BUNDLE,
              "UIMA_flow_controller_init_successful__CONFIG", getMetaData().getName());
      
      initialized = true;
      return true;
    } catch (ResourceConfigurationException e) {
      throw new ResourceInitializationException(e);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    // unregister MBean from MBeanServer when GC occurs
    // NOTE: we don't want to do this in destroy() because all AEs in a CPE are
    // destroyed when the CPE processing completes. If we unregistered the MBean then,
    // the user could not see the stats of a completed CPE.
    JmxMBeanAgent.unregisterMBean(getMBean(), mMBeanServer);
    super.finalize();
  }


  private FlowControllerContext getFlowControllerContext() {
    return (FlowControllerContext) getUimaContext();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource_ImplBase#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
    try {
      mFlowController.reconfigure();
    } catch (ResourceInitializationException e) {
      throw new ResourceConfigurationException(e);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource_ImplBase#destroy()
   */
  public void destroy() {
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
    try {
      mFlowController.destroy();
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
    super.destroy();
  }

  /**
   * Invokes the FlowController's computeFlow method, returning a Flow object that routes the given
   * CAS through this aggregate. This method makes sure to provide the FlowController with its
   * required CAS interface (e.g. JCas).
   * 
   * @param aCAS
   *          the CAS to pass to the FlowController
   * @return a Flow object that routes this CAS
   * 
   * @throws AnalysisEngineProcessException
   *           if the FlowController failed
   */
  public FlowContainer computeFlow(final CAS aCAS) throws AnalysisEngineProcessException {
    mTimer.startIt();
    CAS view = null;
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
    try {
      // throws if _InitialView is mapped to non-existent sofa https://issues.apache.org/jira/browse/UIMA-5097
      view = Util.getStartingView(aCAS, mSofaAware, getUimaContextAdmin().getComponentInfo());     
      // now get the right interface(e.g. CAS or JCAS)
      Class<? extends AbstractCas> requiredInterface = mFlowController.getRequiredCasInterface();
      AbstractCas casToPass = getCasManager().getCasInterface(view, requiredInterface);    
      ((CASImpl)view).switchClassLoaderLockCasCL(this.getResourceManager().getExtensionClassLoader());
      Flow flow = mFlowController.computeFlow(casToPass);
      if (flow instanceof CasFlow_ImplBase) {
        ((CasFlow_ImplBase)flow).setCas(view);
      }
      if (flow instanceof JCasFlow_ImplBase) {
        ((JCasFlow_ImplBase)flow).setJCas(view.getJCas());
      }
      return new FlowContainer(flow, this, aCAS);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    } finally {
      aCAS.setCurrentComponentInfo(null); // https://issues.apache.org/jira/browse/UIMA-5097
      if (view != null) {
        ((CASImpl)view).restoreClassLoaderUnlockCas();      
      }
      mTimer.stopIt();
      getMBean().reportAnalysisTime(mTimer.getDuration());
      getMBean().incrementCASesProcessed();
      UimaContextHolder.setContext(prevContext);
    }
  }

  /** 
   * @return  the required CAS interface of the FlowController
   */
  public Class<? extends AbstractCas> getRequiredCasInterface() {
    return mFlowController.getRequiredCasInterface();
  }

  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /**
   * @return the MBean to use to report performance statistics.
   */
  public AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) getUimaContextAdmin().getManagementInterface();
  }
  
  /**
   * Notifies this FlowController that new Analysis Engines
   * @see FlowController#addAnalysisEngines(Collection)
   * @param aKeys the keys for the delegates
   */
  public void addAnalysisEngines(Collection<String> aKeys) {
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
    try {
      mFlowController.addAnalysisEngines(aKeys);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  /**
   * Notifies this FlowController that some Analysis Engines are no longer available to route CASes to.
   * @see FlowController#removeAnalysisEngines(Collection)
   * @param aKeys the keys of the delegates to be removed
   * @throws AnalysisEngineProcessException - 
   */
  public void removeAnalysisEngines(Collection<String> aKeys) throws AnalysisEngineProcessException {
    UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
    try {
      mFlowController.removeAnalysisEngines(aKeys);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }


  /**
   * Instantiates the FlowController class specified in the descriptor.
   */
  private FlowController instantiateFlowController(ResourceCreationSpecifier aDescriptor)
          throws ResourceInitializationException {
    String flowControllerClassName;
    flowControllerClassName = aDescriptor.getImplementationName();

    if (flowControllerClassName == null || flowControllerClassName.length() == 0) {
      throw new ResourceInitializationException(
              ResourceInitializationException.MISSING_IMPLEMENTATION_CLASS_NAME,
              new Object[] { aDescriptor.getSourceUrlString() });
    }
    // load FlowController class
    Class<?> flowControllerClass = null;
    try {
      // get UIMA extension ClassLoader if available
      ClassLoader cl = getUimaContextAdmin().getResourceManager().getExtensionClassLoader();

      if (cl != null) {
        // use UIMA extension ClassLoader to load the class
        flowControllerClass = cl.loadClass(flowControllerClassName);
      } else {
        // use application ClassLoader to load the class
        flowControllerClass = Class.forName(flowControllerClassName);
      }
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
              new Object[] { flowControllerClassName, aDescriptor.getSourceUrlString() }, e);
    }

    Object userObject;
    try {
      userObject = flowControllerClass.newInstance();
    } catch (Exception e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] {
                  flowControllerClassName, aDescriptor.getSourceUrlString() }, e);
    }
    if (!(userObject instanceof FlowController)) {
      throw new ResourceInitializationException(
              ResourceInitializationException.RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE, new Object[] {
                  flowControllerClassName, FlowController.class, aDescriptor.getSourceUrlString() });
    }
    return (FlowController) userObject;
  }

  boolean isInitialized() {
    return initialized;
  }
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    if ( mFlowController != null ) {
      UimaContext prevContext = UimaContextHolder.setContext(getFlowControllerContext());  // for use by POJOs
      try {
        mFlowController.collectionProcessComplete();
      } finally {
        UimaContextHolder.setContext(prevContext);
      }
    }
  }
}
