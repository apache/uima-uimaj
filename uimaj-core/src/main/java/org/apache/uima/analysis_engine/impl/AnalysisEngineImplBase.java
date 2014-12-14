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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineManagement;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.analysis_engine.ResultNotSupportedException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.Language;
import org.apache.uima.internal.util.JmxMBeanAgent;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.CasDefinition;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTraceEvent_impl;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Provides functionality common to Analysis Engine implementations.
 * 
 * 
 */
public abstract class AnalysisEngineImplBase extends ConfigurableResource_ImplBase implements
        TextAnalysisEngine {
  
  /* (non-Javadoc)
   * @see org.apache.uima.resource.Resource_ImplBase#setMetaData(org.apache.uima.resource.metadata.ResourceMetaData)
   */
  @Override
  //*****************************************************************************************
  // NOTICE: This method is logically not needed it would seem, because the superclass of the superclass of this 
  // implements it.
  // However, there is an obscure issue here involving the precise definition of "protected". 
  // If it is not included in this class and this class is not in the same package as the class needing
  // this, then a class (e.g. PearAnalysisEngineWrapper) which does 
  //        ((AnalysisEngineImplBase)ae).setMetaData(...) fails with a compile error saying that
  // the method setMetaData in the Resource_ImplBase is not visible (even though it is "protected")
  // This makes that problem go away.
  // Details of this issue are explained in section 6.6.2.1, Access to a protected member,
  //   in The Java Language Specification (pg 139).
  //   
  //*****************************************************************************************
  
  protected void setMetaData(ResourceMetaData aMetaData) {
    super.setMetaData(aMetaData);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.AnalysisEngine#batchProcessComplete()
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.AnalysisEngine#collectionProcessComplete()
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.AnalysisEngine#processAndOutputNewCASes(org.apache.uima.cas.CAS)
   */
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * Key that must be inserted into the aAdditionalParams map to turn on verification mode. Also
   * passed down to delegates.
   */
  public static final String PARAM_VERIFICATION_MODE = "VERIFICATION_MODE";


  /**
   * Performance Tuning setting in effect for this Analysis Engine.
   */
  private Properties mPerformanceTuningSettings = UIMAFramework
          .getDefaultPerformanceTuningProperties();

  private UimaTimer mProcessTimer = UIMAFramework.newTimer();

  private boolean mProcessTraceEnabled = true;

  /**
   * The JMX MBeanServer that this AnalysisEngine registers with to publish its statistics.
   */
  private Object mMBeanServer;

  /**
   * An optional name prefix for this AnalysisEngine's MBean.
   */
  private String mMBeanNamePrefix;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource_ImplBase#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    boolean result = super.initialize(aSpecifier, aAdditionalParams);
    if (result) {
      // add our metadata to the CasManager, so that it will pick up our
      // type system, priorities, and indexes (clone it first, to ensure
      // CasManager's version doesn't change).
      AnalysisEngineMetaData md = getAnalysisEngineMetaData();
      getCasManager().addMetaData((AnalysisEngineMetaData)md.clone());

      // read parameters from additionalParams map
      Properties perfSettings = null;
      mMBeanServer = null;
      mMBeanNamePrefix = null;
      if (aAdditionalParams != null) {
        perfSettings = (Properties) aAdditionalParams.get(PARAM_PERFORMANCE_TUNING_SETTINGS);
        mMBeanServer = aAdditionalParams.get(PARAM_MBEAN_SERVER);
        mMBeanNamePrefix = (String)aAdditionalParams.get(PARAM_MBEAN_NAME_PREFIX);
      }
      // set performance tuning settings
      if (perfSettings != null) {
        setPerformanceTuningSettings(perfSettings);
      }
      // register MBean with MBeanServer. If no MBeanServer specified in the
      // additionalParams map, this will use the platform MBean Server
      // (Java 1.5 only)
      getMBean().setName(getMetaData().getName(), getUimaContextAdmin(), mMBeanNamePrefix);
      JmxMBeanAgent.registerMBean(getManagementInterface(), mMBeanServer);
      
      //if this is the root component, also configure the CAS Manager's JMX info at this point
      //TODO: not really necessary to do this every time, only for the first AE we initialize that
      //uses this CasManager.
      getCasManager().setJmxInfo(mMBeanServer, 
              getUimaContextAdmin().getRootContext().getManagementInterface().getUniqueMBeanName());
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() throws Throwable {
    // unregister MBean from MBeanServer when GC occurs.
    // NOTE: we don't want to do this in destroy() because all AEs in a CPE are
    // destroyed when the CPE processing completes. If we unregistered the MBean then,
    // the user could not see the stats of a completed CPE.
    AnalysisEngineManagement mbean = getManagementInterface();
    if (mbean != null) {
      JmxMBeanAgent.unregisterMBean(mbean, mMBeanServer);
    }
    super.finalize();

  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#getAnalysisEngineMetaData()
   */
  public AnalysisEngineMetaData getAnalysisEngineMetaData() {
    return (AnalysisEngineMetaData) getMetaData();
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#newCAS()
   */
  public synchronized CAS newCAS() throws ResourceInitializationException {
    CasDefinition casDef = getCasManager().getCasDefinition();
    return CasCreationUtils.createCas(casDef, getPerformanceTuningSettings());
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#newJCas()
   */
  public JCas newJCas() throws ResourceInitializationException {
    try {
      return newCAS().getJCas();
    } catch (CASException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.cas.CAS,
   *      org.apache.uima.analysis_engine.ResultSpecification)
   */
  public ProcessTrace process(CAS aCAS, ResultSpecification aResultSpec)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    setResultSpecification(aResultSpec);
    return process(aCAS);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.cas.CAS,
   *      org.apache.uima.analysis_engine.ResultSpecification, org.apache.uima.util.ProcessTrace)
   */
  public void process(CAS aCAS, ResultSpecification aResultSpec, ProcessTrace aTrace)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    setResultSpecification(aResultSpec);
    process(aCAS);
    if (isProcessTraceEnabled()) {  // a slight performance speedup https://issues.apache.org/jira/browse/UIMA-4151
      buildProcessTraceFromMBeanStats(aTrace); 
    }
  }

  public ProcessTrace process(CAS aCAS) throws AnalysisEngineProcessException {
    CasIterator iter = processAndOutputNewCASes(aCAS);
    // step through all output CASes which lets the AE finish all processing
    while (iter.hasNext()) {
      CAS cas = iter.next();
      cas.release();
    }
    // https://issues.apache.org/jira/browse/UIMA-4151
    return isProcessTraceEnabled() ? buildProcessTraceFromMBeanStats() : ProcessTrace_impl.disabledProcessTrace;
//    return buildProcessTraceFromMBeanStats();
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.jcas.JCas)
   */
  public ProcessTrace process(JCas aJCas) throws AnalysisEngineProcessException {
    return process(aJCas.getCas());
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.jcas.JCas,
   *      org.apache.uima.analysis_engine.ResultSpecification)
   */
  public ProcessTrace process(JCas aJCas, ResultSpecification aResultSpec)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    return process(aJCas.getCas(), aResultSpec);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.jcas.JCas,
   *      org.apache.uima.analysis_engine.ResultSpecification, org.apache.uima.util.ProcessTrace)
   */
  public void process(JCas aJCas, ResultSpecification aResultSpec, ProcessTrace aTrace)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    process(aJCas.getCas(), aResultSpec, aTrace);
  }

  /**
   * @deprecated
   */
  @Deprecated
  public void process(org.apache.uima.analysis_engine.AnalysisProcessData aProcessData,
          ResultSpecification aResultSpec) throws ResultNotSupportedException,
          AnalysisEngineProcessException {
    process(aProcessData.getCAS(), aResultSpec, aProcessData.getProcessTrace());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#process(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    try {
      process(aCAS);
    } catch (AnalysisEngineProcessException e) {
      throw new ResourceProcessException(e);
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS[])
   */
  public void processCas(CAS[] aCASes) throws ResourceProcessException {
    for (int i = 0; i < aCASes.length; i++) {
      if (aCASes[i] != null) {
        processCas(aCASes[i]);
      }
    }
  }

  /**
   * Default implementation of processAndOutputNewCASes(JCas) method. Calls the version of this
   * method that takes a CAS, then wraps the resulting CasIterator in a JCasIterator.
   */
  public JCasIterator processAndOutputNewCASes(JCas aJCas) throws AnalysisEngineProcessException {
    return new JCasIteratorWrapper(processAndOutputNewCASes(aJCas.getCas()));
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#createResultSpecification()
   */
  public ResultSpecification createResultSpecification() {
    return new ResultSpecification_impl();
  }

  /**
   * @see AnalysisEngine#createResultSpecification(TypeSystem)
   */
  public ResultSpecification createResultSpecification(TypeSystem aTypeSystem) {
    return new ResultSpecification_impl(aTypeSystem);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#getFeatureNamesForType(java.lang.String)
   */
  public synchronized String[] getFeatureNamesForType(String aTypeName) {
    // build CAS and populate mFirstTypeSystem
    CAS cas = getCasManager().getCas(this.getUimaContextAdmin().getQualifiedContextName());
    TypeSystem ts = cas.getTypeSystem();
    getCasManager().releaseCas(cas);

    Type t = ts.getType(aTypeName);
    if (t != null) {
      List<Feature> features = t.getFeatures();
      String[] featNames = new String[features.size()];
      for (int i = 0; i < features.size(); i++) {
        Feature f = features.get(i);
        featNames[i] = f.getShortName();
      }
      return featNames;
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isStateless()
   */
  public boolean isStateless() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public boolean isReadOnly() {
    OperationalProperties opProps = getAnalysisEngineMetaData().getOperationalProperties();
    return opProps == null ? false : !opProps.getModifiesCas();
  }

  /**
   * From the CAS Processor interface. Called by the CPM if the CAS type system changes - this does
   * not need to do anything since the annotators' typeSystemInit methods are automatically called
   * prior to processing whenever it is necessary.
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException {
    // does not need to do anything since the annotators' typeSystemInit methods are automatically
    // called prior to processing whenever it is necessary.
  }

  /**
   * Gets the performance tuning settings in effect for this Analysis Engine.
   * 
   * @return performance tuning settings
   */
  public Properties getPerformanceTuningSettings() {
    return mPerformanceTuningSettings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    // does nothing by default (for service adapters)
    // overridden in both primitive and aggregate AE implementations
  }

  public void resetResultSpecificationToDefault() {
    ResultSpecification resultSpec = new ResultSpecification_impl();
    resultSpec.addCapabilities(this.getAnalysisEngineMetaData().getCapabilities());
    setResultSpecification(resultSpec);
  }

  public AnalysisEngineManagement getManagementInterface() {
    UimaContextAdmin uc = getUimaContextAdmin();
    return uc == null ? null : uc.getManagementInterface();
  }

  public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    batchProcessComplete();

  }

  public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    collectionProcessComplete();
  }

  /**
   * Sets the performance tuning settings in effect for this Analysis Engine. This should be set
   * from the initialize() method if the defaults are to be overriden.
   * 
   * @param aSettings
   *          performance tuning settings
   */
  protected void setPerformanceTuningSettings(Properties aSettings) {
    mPerformanceTuningSettings = (Properties) aSettings.clone();
    String procTrEnabled = mPerformanceTuningSettings
            .getProperty(UIMAFramework.PROCESS_TRACE_ENABLED);
    mProcessTraceEnabled = procTrEnabled == null || procTrEnabled.equalsIgnoreCase("true");
  }

  protected void normalizeIsoLangCodes(ProcessingResourceMetaData md) {
    if (md == null) {
      return;
    }
    Capability[] capabilities = md.getCapabilities();
    if (capabilities == null) {
      return;
    }
    for (int i = 0; i < capabilities.length; i++) {
      Capability c = capabilities[i];
      String[] languages = c.getLanguagesSupported();
      for (int j = 0; j < languages.length; j++) {
        languages[j] = Language.normalize(languages[j]);
      }
    }
  }

  /**
   * Kludge - make this public (but not part of AnalysisEngine interface) so that TAFAnnotator can
   * access it.
   * 
   * @see org.apache.uima.resource.ConfigurationManager#getCurrentConfigParameterSettings(String)
   * @return -
   */
  protected ConfigurationParameterSettings getCurrentConfigParameterSettings() {
    return getUimaContextAdmin().getConfigurationManager().getCurrentConfigParameterSettings(
            getUimaContextAdmin().getQualifiedContextName());
  }

  /**
   * @return the MBean that provides the management interface to this AE. Returns the same object as
   * getManagementInterface() but casted to the AnalysisEngineManagement type.
   */
  protected AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) getUimaContextAdmin().getManagementInterface();
  }

  protected void enterProcess() {
    mProcessTimer.startIt();
  }

  protected void exitProcess() {
    mProcessTimer.stopIt();
    getMBean().reportAnalysisTime(mProcessTimer.getDuration());
  }

  protected void enterBatchProcessComplete() {
    mProcessTimer.startIt();
  }

  protected void exitBatchProcessComplete() {
    mProcessTimer.stopIt();
    getMBean().reportBatchProcessCompleteTime(mProcessTimer.getDuration());
  }

  protected void enterCollectionProcessComplete() {
    mProcessTimer.startIt();
  }

  protected void exitCollectionProcessComplete() {
    mProcessTimer.stopIt();
    getMBean().reportCollectionProcessCompleteTime(mProcessTimer.getDuration());
  }

  /**
   * @return a constructed ProcessTrace object that represents the last execution of this AnalysisEngine. This
   * is used so that we can return a ProcessTrace object from each process() call for backwards
   * compatibility with version 1.x.
   */
  protected ProcessTrace buildProcessTraceFromMBeanStats() {
    ProcessTrace trace = new ProcessTrace_impl(getPerformanceTuningSettings());
    buildProcessTraceFromMBeanStats(trace);
    return trace;
  }

  /**
   * Modify an existing ProcessTrace object by adding events that represent the last excecution of
   * this AnalysisEngine. This is used so that we can return a ProcessTrace object from each
   * process() call for backwards compatibility with version 1.x.
   * @param trace -
   */
  protected void buildProcessTraceFromMBeanStats(ProcessTrace trace) {
    // this is the implementation for primitives only. Aggregate AE overrides.
    if (isProcessTraceEnabled()) {
      // to accommodate service adapters, check if a Service Call time is registered
      int serviceCallTime = (int) getMBean().getServiceCallTimeSinceMark();
      ProcessTraceEvent_impl serviceCallEvent = null;
      if (serviceCallTime > 0) {
        serviceCallEvent = new ProcessTraceEvent_impl(getMetaData().getName(), "Service Call", "");
        serviceCallEvent.setDuration(serviceCallTime);
        trace.addEvent(serviceCallEvent);
      }

      // now check Analysis time
      int analysisTime = (int) getMBean().getAnalysisTimeSinceMark();
      if (analysisTime > 0 || 
          // Jira http://issues.apache.org/jira/browse/uima-941
          // intent is to skip recording analysis times of 0
          //   if these are coming from a remote which supports
          //   serviceCallTime but not analysisTime
          //   If both are 0, the presumption is that the time really
          //   was 0.  If only analysisTime is 0, the presumption is
          //   that the remote didn't implement it, in favor of
          //   returning serviceCallTime.
          (analysisTime == 0 && serviceCallTime == 0)) {
        ProcessTraceEvent_impl analysisEvent = new ProcessTraceEvent_impl(getMetaData().getName(),
                "Analysis", "");
        analysisEvent.setDuration(analysisTime);
        if (serviceCallEvent != null) {
          serviceCallEvent.addSubEvent(analysisEvent);
        } else {
          trace.addEvent(analysisEvent);
        }
      }

      // set a mark at the current time, so that subsequent calls to
      // this method will pick up only times recorded after the mark.
      getMBean().mark();
    }
  }

  /**
   * Gets whether the Process Trace (which collects performance stats for this AnalysisEngine) is
   * enabled. This is controlled through the PerformanceTuningSettings passed to the initialize()
   * method.
   * 
   * @return true if the ProcessTrace is enabled, false if not.
   */
  protected boolean isProcessTraceEnabled() {
    return mProcessTraceEnabled;
  }
  
  protected Object getMBeanServer() {
    return mMBeanServer;
  }
  
  protected String getMBeanNamePrefix() {
    return mMBeanNamePrefix;
  }
}
