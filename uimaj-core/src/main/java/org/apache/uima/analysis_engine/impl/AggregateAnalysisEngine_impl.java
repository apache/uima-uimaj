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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.asb.ASB;
import org.apache.uima.analysis_engine.asb.impl.ASB_impl;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.impl.FlowControllerDeclaration_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.flow.impl.CapabilityLanguageFlowController;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.flow.impl.FlowControllerDescription_impl;
import org.apache.uima.internal.util.UUIDGenerator;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.impl.ProcessTraceEvent_impl;

/**
 * Reference implementation of {@link AnalysisEngine}.
 * 
 * 
 */
public class AggregateAnalysisEngine_impl extends AnalysisEngineImplBase implements AnalysisEngine {

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class CLASS_NAME = AggregateAnalysisEngine_impl.class;

  static public final String PARAM_RESULT_SPECIFICATION = "RESULT_SPECIFICATION";

  /**
   * The AnalysisEngineDescription for this AnlaysisEngine instance.
   */
  private AnalysisEngineDescription mDescription;

  /**
   * For an aggregate AnalysisEngine only, a Map from each component's key to a
   * ProcessingResourceMetaData object for that component. This includes component AEs as well as
   * the FlowController.
   */
  private Map mComponentMetaData;

  /**
   * For an aggregate AnalysisEngine only, the ASB used to communicate with the delegate
   * AnalysisEngines.
   */
  private ASB mASB;

  /**
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
                  throws ResourceInitializationException {
    try {
      // aSpecifier must be a AnalysisEngineDescription
      if (!(aSpecifier instanceof AnalysisEngineDescription)) {
        return false;
      }

      mDescription = (AnalysisEngineDescription) aSpecifier;

      // must be an aggregate AE descriptor
      if (mDescription.isPrimitive()) {
        return false;
      }

      // also framework implementation must start with:
      // org.apache.uima.java (new style) or JEDII (old style)
      final String fwImpl = mDescription.getFrameworkImplementation();
      if (!(fwImpl.startsWith("org.apache.uima.java") || fwImpl.startsWith("JEDII"))) {
        return false;
      }

      super.initialize(aSpecifier, aAdditionalParams);
      AnalysisEngineMetaData md = mDescription.getAnalysisEngineMetaData();

      getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
                      "UIMA_analysis_engine_init_begin__CONFIG", md.getName());

      // Normalize language codes. Need to do this since a wide variety of
      // spellings are acceptable according to ISO.
      normalizeIsoLangCodes(md);

      // clone this metadata and assign a UUID if not already present
      AnalysisEngineMetaData mdCopy = (AnalysisEngineMetaData) md.clone();

      if (mdCopy.getUUID() == null) {
        mdCopy.setUUID(UUIDGenerator.generate());
      }
      setMetaData(mdCopy);

      // resolve component AnalysisEngine and FlowController specifiers
      try {
        mDescription.getDelegateAnalysisEngineSpecifiers(getResourceManager());
        if (mDescription.getFlowControllerDeclaration() != null) {
          if (mDescription.getFlowControllerDeclaration().getImport() == null
                          && mDescription.getFlowControllerDeclaration().getSpecifier() == null) {
            throw new ResourceInitializationException(
                            ResourceInitializationException.EMPTY_FLOW_CONTROLLER_DECLARATION,
                            new Object[] { getMetaData().getName(), mDescription.getSourceUrl() });
          }

          mDescription.getFlowControllerDeclaration().resolveImports(getResourceManager());
        }
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }

      // validate the AnalysisEngineDescription and throw a
      // ResourceInitializationException if there is a problem
      mDescription.validate();

      // Read parameters from the aAdditionalParams map.
      // (First copy it so we can modify it and send the parameters on to
      // out delegate anlaysis engines.)
      if (aAdditionalParams == null) {
        aAdditionalParams = new HashMap();
      } else {
        aAdditionalParams = new HashMap(aAdditionalParams);
      }

      // put configuration parameter settings into the aAdditionalParams map to be
      // passed on to delegate AEs
      aAdditionalParams.put(Resource.PARAM_CONFIG_PARAM_SETTINGS,
                      getCurrentConfigParameterSettings());

      // add resource manager (initialized by superclass) to aAdditionalParams map
      // so that delegate AEs will share it
      aAdditionalParams.put(Resource.PARAM_RESOURCE_MANAGER, getResourceManager());

      initializeAggregateAnalysisEngine(mDescription, aAdditionalParams);

      // Initialize ResultSpec based on output capabilities
      // TODO: should only do this for outermost AE
      resetResultSpecificationToDefault();

      getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
                      "UIMA_analysis_engine_init_successful__CONFIG", md.getName());
      return true;
    } catch (ResourceConfigurationException e) {
      throw new ResourceInitializationException(
                      ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR,
                      new Object[] { getMetaData().getName(), aSpecifier.getSourceUrlString() });
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    if (aResultSpec == null) {
      resetResultSpecificationToDefault();
    } else {
      // To form the result spec that will be passed down to each component,
      // we take the union aResultSpec with the inputs of all components in this
      // aggregate. This forms the complete list of types that any component in
      // this aggregate might ever need to produce.
      ResultSpecification resultSpecForComponents = (ResultSpecification) aResultSpec.clone();
      resultSpecForComponents.addCapabilities(getAllComponentCapabilities(), false);

      // now iterate over components and call their setResultSpecification methods
      Iterator componentIter = _getASB().getComponentAnalysisEngines().values().iterator();
      while (componentIter.hasNext()) {
        AnalysisEngine ae = (AnalysisEngine) componentIter.next();
        ae.setResultSpecification(resultSpecForComponents);
      }
    }
  }

  /**
   * Gets an array containing all capabilities of all components of this aggregate
   * 
   * @return
   */
  private Capability[] getAllComponentCapabilities() {
    ArrayList capabilityList = new ArrayList();
    Iterator iter = _getComponentMetaData().values().iterator();
    while (iter.hasNext()) {
      AnalysisEngineMetaData md = (AnalysisEngineMetaData) iter.next();
      capabilityList.addAll(Arrays.asList(((ProcessingResourceMetaData) md).getCapabilities()));
    }
    Capability[] capabilityArray = new Capability[capabilityList.size()];
    capabilityList.toArray(capabilityArray);
    return capabilityArray;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (mASB != null)
      mASB.destroy();
    getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
                    "UIMA_analysis_engine_destroyed__CONFIG", getMetaData().getName());
    super.destroy();
  }

  /**
   * @see AnalysisEngine#processAndOutputNewCASes(CAS)
   */
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    // logging and instrumentation
    String resourceName = getMetaData().getName();
    getLogger().logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
                    "UIMA_analysis_engine_process_begin__FINE", resourceName);
    try {
      CasIterator iterator = _getASB().process(aCAS);

      // log end of event
      getLogger().logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
                      "UIMA_analysis_engine_process_end__FINE", resourceName);
      return iterator;
    } catch (Exception e) {
      // log and rethrow exception
      getLogger().log(Level.SEVERE, "", e);
      if (e instanceof AnalysisEngineProcessException)
        throw (AnalysisEngineProcessException) e;
      else
        throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * @see org.apache.uima.resource.AnalysisEngine#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    // do base resource reconfiguration
    super.reconfigure();

    // call this method recursively on each component
    Map components = this._getASB().getComponentAnalysisEngines();
    Iterator it = components.values().iterator();
    while (it.hasNext()) {
      ConfigurableResource component = (ConfigurableResource) it.next();
      component.reconfigure();
    }
  }

  public void batchProcessComplete() throws AnalysisEngineProcessException {
    enterBatchProcessComplete();
    try {
      // pass call down to components, which might be (or contain) CAS Consumers
      Iterator iter = this._getASB().getComponentAnalysisEngines().values().iterator();
      while (iter.hasNext()) {
        ((AnalysisEngine) iter.next()).batchProcessComplete();
      }
    } finally {
      exitBatchProcessComplete();
    }
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    enterCollectionProcessComplete();
    try {
      // pass call down to components, which might be (or contain) CAS Consumers
      Iterator iter = this._getASB().getComponentAnalysisEngines().values().iterator();
      while (iter.hasNext()) {
        ((AnalysisEngine) iter.next()).collectionProcessComplete();
      }
    } finally {
      exitCollectionProcessComplete();
    }
  }

  /**
   * A utility method that performs initialization logic for a aggregate AnalysisEngine.
   * 
   * @param aDescription
   *          the AnalysisEngine description for this AnalysisEngine
   * @param aAdditionalParams
   *          additional parameters that were passed to this AnalysisEngine's initialize method.
   * 
   * @throws ResourceInitializationException
   *           if an initialization failure occurs
   */
  protected void initializeAggregateAnalysisEngine(AnalysisEngineDescription aDescription,
                  Map aAdditionalParams) throws ResourceInitializationException {
    // Create and configure the ASB - the ASB will create and initialize
    // the component AnalysisEngines and the FlowController. This method also retrieves
    // the component AnalysisEngines' metadata from the ASB, so it can be access via the
    // _getComponentCasProcessorMetaData() method.
    initASB(aDescription, aAdditionalParams);

    // Do any processing we need to do now that we have this metadata.
    processDelegateAnalysisEngineMetaData();
  }

  /**
   * A utility method that creates and configures the ASB component. The ASB will create and
   * initialize the delegate AnalysisEngines. This method also retrieves delegate AnalysisEngine
   * metadata from the ASB and provides access to that method via the
   * {@link _getDelegateAnalysisEngineMetaData()} method.
   * 
   * @param aDescription
   *          the AnalysisEngine description for this AnalysisEngine
   * @param aAdditionalParams
   *          parameters that will be passed to the ASB's initialize method.
   * 
   * @throws ResourceInitializationException
   *           if the ASB or a delegate AnalysisEngine could not be created.
   */
  protected void initASB(AnalysisEngineDescription aAnalysisEngineDescription, Map aAdditionalParams)
                  throws ResourceInitializationException {
    // add this analysis engine's name to the parameters sent to the ASB
    Map asbParams = new HashMap(aAdditionalParams);
    asbParams.put(ASB.PARAM_AGGREGATE_ANALYSIS_ENGINE_NAME, this.getMetaData().getName());
    asbParams.put(Resource.PARAM_RESOURCE_MANAGER, getResourceManager());

    // Pass sofa mappings defined in this aggregate as additional ASB parameters
    // System.out.println("remapping sofa names");
    asbParams.put(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS, aAnalysisEngineDescription
                    .getSofaMappings());

    // Get FlowController specifier from the aggregate descriptor. If none, use
    // default FixedFlow specifier.
    FlowControllerDeclaration flowControllerDecl = aAnalysisEngineDescription
                    .getFlowControllerDeclaration();
    if (flowControllerDecl != null) {
      try {
        aAnalysisEngineDescription.getFlowControllerDeclaration().resolveImports(
                        getResourceManager());
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }
    } else {
      flowControllerDecl = getDefaultFlowControllerDeclaration();
    }

    // create and configure ASB
    mASB = new ASB_impl();
    ResourceCreationSpecifier_impl dummyAsbSpecifier = new ResourceCreationSpecifier_impl();
    dummyAsbSpecifier.setMetaData(new ResourceMetaData_impl());
    mASB.initialize(dummyAsbSpecifier, asbParams);
    mASB.setup(_getComponentCasProcessorSpecifierMap(), getUimaContextAdmin(), flowControllerDecl,
                    getAnalysisEngineMetaData());

    // Get delegate AnalysisEngine metadata from the ASB
    mComponentMetaData = _getASB().getAllComponentMetaData();
  }

  /**
   * Does processing using the delegate AnalysisEngine metadata once it becomes available.
   * <p>
   * Specifically, sets this aggregate AE's Type System, Type Priorities, and FS Index Descriptions
   * equal to the result of merging the information from its delegate AEs.
   * 
   * @throws ResourceInitializationException
   *           if an error occurs
   */
  protected void processDelegateAnalysisEngineMetaData() throws ResourceInitializationException {
    // set this aggregate AnalysisEngine's TypeSystem, TypePriorities, and FS
    // Index Descriptions to the result of merging the information from all
    // delegate AEs. (The aggregate AE may specify its own indexes or type
    // priorities but NOT its own types.)

    // first, create Collections of TypeSystems, TypePriorities, and Index Descriptions
    List typeSystems = new ArrayList();
    List typePriorities = new ArrayList();
    List fsIndexCollections = new ArrayList();

    TypePriorities thisAEsTypePriorities = getAnalysisEngineMetaData().getTypePriorities();
    if (thisAEsTypePriorities != null) {
      typePriorities.add(thisAEsTypePriorities);
    }
    FsIndexCollection thisAEsIndexes = getAnalysisEngineMetaData().getFsIndexCollection();
    if (thisAEsIndexes != null) {
      fsIndexCollections.add(thisAEsIndexes);
    }

    // iterate over metadata for all components
    Iterator metadataIterator = _getComponentMetaData().values().iterator();
    while (metadataIterator.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) metadataIterator.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
      if (md.getFsIndexCollection() != null)
        fsIndexCollections.add(md.getFsIndexCollection());
    }

    // now do merge
    TypeSystemDescription aggTypeDesc = CasCreationUtils.mergeTypeSystems(typeSystems,
                    getResourceManager());
    TypePriorities aggTypePriorities = CasCreationUtils.mergeTypePriorities(typePriorities,
                    getResourceManager());
    FsIndexCollection aggIndexColl = CasCreationUtils.mergeFsIndexes(fsIndexCollections,
                    getResourceManager());

    // assign results of merge to this aggregate AE's metadata
    AnalysisEngineMetaData aggregateMD = this.getAnalysisEngineMetaData();
    aggregateMD.setTypeSystem(aggTypeDesc);
    aggregateMD.setTypePriorities(aggTypePriorities);
    aggregateMD.setFsIndexCollection(aggIndexColl);

    // check for inconsistent operationalProperties between aggregate and delegates
    validateOperationalProperties();
  }

  /**
   * Checks operational properties in an aggregate to ensure they are not inconsistent with
   * operational properties of the components. For example, an aggregate cannot have
   * multipleDeploymentAlloiwed == true if it contains a component with multipleDeploymentAllowed ==
   * false.
   * 
   * @param aDesc
   *          the aggregate analysis engine description to validate
   * 
   * @throws ResourceInitializationException
   *           if there is an invalid parameter override declaration
   */
  protected void validateOperationalProperties() throws ResourceInitializationException {
    OperationalProperties aggProps = getAnalysisEngineMetaData().getOperationalProperties();
    if (aggProps != null) {
      boolean atLeastOneCasMultiplier = false;
      Iterator metadataIterator = _getComponentMetaData().values().iterator();
      while (metadataIterator.hasNext()) {
        ProcessingResourceMetaData md = (ProcessingResourceMetaData) metadataIterator.next();
        OperationalProperties componentProps = md.getOperationalProperties();
        if (componentProps != null) {
          if (aggProps.isMultipleDeploymentAllowed()
                          && !componentProps.isMultipleDeploymentAllowed()) {
            throw new ResourceInitializationException(
                            ResourceInitializationException.INVALID_MULTIPLE_DEPLOYMENT_ALLOWED,
                            new Object[] { getAnalysisEngineMetaData().getName(), md.getName(),
                                getAnalysisEngineMetaData().getSourceUrlString() });
          }
          if (!aggProps.getModifiesCas() && componentProps.getModifiesCas()) {
            throw new ResourceInitializationException(
                            ResourceInitializationException.INVALID_MODIFIES_CAS, new Object[] {
                                getAnalysisEngineMetaData().getName(), md.getName(),
                                getAnalysisEngineMetaData().getSourceUrlString() });
          }
          if (componentProps.getOutputsNewCASes()) {
            atLeastOneCasMultiplier = true;
          }
        }
      }
      if (aggProps.getOutputsNewCASes() && !atLeastOneCasMultiplier) {
        throw new ResourceInitializationException(
                        ResourceInitializationException.INVALID_OUTPUTS_NEW_CASES, new Object[] {
                            getAnalysisEngineMetaData().getName(),
                            getAnalysisEngineMetaData().getSourceUrlString() });
      }
    }
  }

  /**
   * Returns the default FlowController declaration to be used if the aggregate AE descriptor does
   * not specify one.
   */
  protected FlowControllerDeclaration getDefaultFlowControllerDeclaration() {
    FlowControllerDescription flowControllerSpecifier = new FlowControllerDescription_impl();
    flowControllerSpecifier.setFrameworkImplementation("org.apache.uima.java");
    if (getAnalysisEngineMetaData().getFlowConstraints() instanceof CapabilityLanguageFlow) {
      flowControllerSpecifier.setImplementationName(CapabilityLanguageFlowController.class
                      .getName());
    } else {
      flowControllerSpecifier.setImplementationName(FixedFlowController.class.getName());
    }
    flowControllerSpecifier.getMetaData().setName("Fixed Flow Controller");

    FlowControllerDeclaration decl = new FlowControllerDeclaration_impl();
    decl.setKey("_FlowController");
    decl.setSpecifier(flowControllerSpecifier);
    return decl;
  }

  /**
   * For an aggregate AnalysisEngine only, gets the ASB component.
   * 
   * @return the ASB
   */
  protected ASB _getASB() {
    return mASB;
  }

  /**
   * For an aggregate AnalysisEngine only, gets a Map from each component's key to
   * ProcessingResourceMetaData for that component. This includes component AEs as well as the
   * FlowController.
   * 
   * @return a Map from String keys to ProcessingResourceMetaData objects.
   */
  protected Map _getComponentMetaData() {
    return mComponentMetaData;
  }

  /**
   * For an aggregate AnalysisEngine only, gets a Map from each component's key to the specifier for
   * that component.
   * 
   * @return a Map with String keys and ResourceSpecifier values
   */
  protected Map _getComponentCasProcessorSpecifierMap() {
    try {
      return mDescription.getDelegateAnalysisEngineSpecifiers();
    } catch (InvalidXMLException e) {
      // this should not happen, because we resolve delegates during initialization
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Construct a ProcessTrace object that represnts the last excecution of this AnalysisEngine. This
   * is used so that we can return a ProcessTrace object from each process() call for backwards
   * compatibility with version 1.x.
   */
  protected void buildProcessTraceFromMBeanStats(ProcessTrace trace) {
    if (isProcessTraceEnabled()) {
      ProcessTraceEvent_impl procEvt = new ProcessTraceEvent_impl(getMetaData().getName(),
                      "Analysis", "");
      procEvt.setDuration((int) getMBean().getAnalysisTimeSinceMark());
      trace.addEvent(procEvt);

      // now add subevents for each component
      Iterator aeIter = _getASB().getComponentAnalysisEngines().values().iterator();
      while (aeIter.hasNext()) {
        AnalysisEngine ae = (AnalysisEngine) aeIter.next();
        if (ae instanceof AnalysisEngineImplBase) {
          ProcessTrace subPT = ((AnalysisEngineImplBase) ae).buildProcessTraceFromMBeanStats();
          if (subPT.getEvents().size() > 0) {
            procEvt.addSubEvent((ProcessTraceEvent) subPT.getEvents().get(0));
          }
        }
      }
      // and also FlowController
      FlowControllerContainer fcc = ((ASB_impl) _getASB()).getFlowControllerContainer();
      int flowControllerTime = (int) fcc.getMBean().getAnalysisTimeSinceMark();
      ProcessTraceEvent_impl flowControllerEvent = new ProcessTraceEvent_impl(fcc.getMetaData()
                      .getName(), "Analysis", "");
      flowControllerEvent.setDuration(flowControllerTime);
      procEvt.addSubEvent(flowControllerEvent);

      // set a mark at the current time, so that subsequent calls to
      // this method will pick up only times recorded after the mark.
      getMBean().mark();
    }
  }

}
