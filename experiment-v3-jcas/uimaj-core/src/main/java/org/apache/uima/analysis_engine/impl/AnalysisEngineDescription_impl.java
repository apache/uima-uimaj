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
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.NameClassPair;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link AnalysisEngineDescription}. Note that this class contains two
 * attributes of class Map, which are not supported by the default XML input/output routines.
 * Therefore we override the {@link MetaDataObject_impl#writePropertyAsElement(PropertyXmlInfo, String)} and
 * {@link MetaDataObject_impl#readPropertyValueFromXMLElement(PropertyXmlInfo, Element, XMLParser, XMLParser.ParsingOptions)} methods.
 * 
 * 
 */
public class AnalysisEngineDescription_impl extends ResourceCreationSpecifier_impl implements
        AnalysisEngineDescription {

  static final private Method getterForAEwImports;
  static { 
    try {
      getterForAEwImports = AnalysisEngineDescription_impl.class.getDeclaredMethod("getDelegateAnalysisEngineSpecifiersWithImports");
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }
  /**
   * Name of the "delegateAnalysisEngineSpecifiers" property. Change this if interface changes.
   */
  final protected String PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS = "delegateAnalysisEngineSpecifiers";
  
  /**
   * Name of the "delegateAnalysisEngineSpecifiersWithImports" property. Change this if interface
   * changes.
   */
  final protected String PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS = "delegateAnalysisEngineSpecifiersWithImports";

  /**
   * Name of the "delegateAnalysisEngineSpecifiers" XML Element. Change this if schema changes.
   */
  final protected String ELEM_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS = "delegateAnalysisEngineSpecifiers";

  private String mFrameworkImplementation;

  private boolean mPrimitive;

  private FlowControllerDeclaration mFlowControllerDeclaration;

  // This holds delegates after imports have been resolved (merged from xmlComments 1187355)
  private Map<String, ResourceSpecifier> mDelegateAnalysisEngineSpecifiers = new LinkedHashMap<String, ResourceSpecifier>();

  // This holds delegates as they come from reading the descriptor, may have import elements (unresolved) (merged from xmlComments 1187355)
  private Map<String, MetaDataObject> mDelegateAnalysisEngineSpecifiersWithImports = new LinkedHashMap<String, MetaDataObject>();

  private Map<String, Import> mProcessedImports = new HashMap<String, Import>();

  private SofaMapping[] mSofaMappings;

  static final long serialVersionUID = -8103625125291855592L;

  /**
   * Creates a new AnalysisEngineDescription_impl. Initializes the MetaData,
   * DelegateAnalysisEngineSpecifiers, and ExternalResourcesRequired attributes.
   */
  public AnalysisEngineDescription_impl() {
    setMetaData(new AnalysisEngineMetaData_impl());
    setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    // Set default operational properties. These are used if the
    // descriptor is constructed programatically, rather than parsed.
    OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
            .createOperationalProperties();
    opProps.setModifiesCas(true);
    opProps.setMultipleDeploymentAllowed(true);
    opProps.setOutputsNewCASes(false);
    getAnalysisEngineMetaData().setOperationalProperties(opProps);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getFrameworkImplementation()
   */
  public String getFrameworkImplementation() {
    return mFrameworkImplementation;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setFrameworkImplementation(java.lang.String)
   */
  public void setFrameworkImplementation(String aFrameworkImplementation) {
    mFrameworkImplementation = aFrameworkImplementation;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#isPrimitive()
   */
  public boolean isPrimitive() {
    return mPrimitive;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setPrimitive(boolean)
   */
  public void setPrimitive(boolean aPrimitive) {
    mPrimitive = aPrimitive;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getAnnotatorImplementationName()
   */
  public String getAnnotatorImplementationName() {
    return getImplementationName();
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setAnnotatorImplementationName(String)
   */
  public void setAnnotatorImplementationName(String aImplementationName) {
    setImplementationName(aImplementationName);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()
   */
  public Map<String, ResourceSpecifier> getDelegateAnalysisEngineSpecifiers() throws InvalidXMLException {
    resolveDelegateAnalysisEngineImports(UIMAFramework.newDefaultResourceManager(), false);
    return Collections.unmodifiableMap(mDelegateAnalysisEngineSpecifiers);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()
   */
  public Map<String, ResourceSpecifier> getDelegateAnalysisEngineSpecifiers(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveDelegateAnalysisEngineImports(aResourceManager, false);
    return Collections.unmodifiableMap(mDelegateAnalysisEngineSpecifiers);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiersWithImports()
   */
  public Map<String, MetaDataObject> getDelegateAnalysisEngineSpecifiersWithImports() {
    return mDelegateAnalysisEngineSpecifiersWithImports;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getFlowControllerDeclaration()
   */
  public FlowControllerDeclaration getFlowControllerDeclaration() {
    return mFlowControllerDeclaration;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setFlowController(org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration)
   */
  public void setFlowControllerDeclaration(FlowControllerDeclaration aFlowControllerDeclaration) {
    mFlowControllerDeclaration = aFlowControllerDeclaration;
  }

  public Map<String, ResourceSpecifier> getAllComponentSpecifiers(ResourceManager aResourceManager) throws InvalidXMLException {
    if (aResourceManager == null) {
      aResourceManager = UIMAFramework.newDefaultResourceManager();
    }
    resolveImports(aResourceManager);
    Map<String, ResourceSpecifier> map = new LinkedHashMap<String, ResourceSpecifier>(mDelegateAnalysisEngineSpecifiers);
    if (getFlowControllerDeclaration() != null) {
      map.put(getFlowControllerDeclaration().getKey(), getFlowControllerDeclaration()
              .getSpecifier());
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getAnalysisEngineMetaData()
   */
  public AnalysisEngineMetaData getAnalysisEngineMetaData() {
    return (AnalysisEngineMetaData) getMetaData();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getSofaMappings()
   */
  public SofaMapping[] getSofaMappings() {
    return mSofaMappings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setSofaMappings(org.apache.uima.analysis_engine.metadata.SofaMapping[])
   */
  public void setSofaMappings(SofaMapping[] aSofaMappings) {
    mSofaMappings = aSofaMappings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#doFullValidation()
   */
  public void doFullValidation() throws ResourceInitializationException {
    // attempt to instantiate AE in "verification mode"
    Map<String, Object> m = new HashMap<String, Object>();
    m.put(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE, Boolean.TRUE);
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(this, m);
    validateSofaMappings();
    ae.newCAS();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#doFullValidation(org.apache.uima.resource.ResourceManager)
   */
  public void doFullValidation(ResourceManager aResourceManager)
          throws ResourceInitializationException {
    // attempt to instantiate AE in "verification mode"
    Map<String, Object> m = new HashMap<String, Object>();
    m.put(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE, Boolean.TRUE);
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(this, aResourceManager, m);
    validateSofaMappings();
    ae.newCAS();
  }

  /**
   * Determines if the AnalysisEngineDescription is valid. An exception is thrown if it is not
   * valid. This should be called from this Analysis Engine's initialize method. Note this does not
   * check configuration parameter settings - that must be done by an explicit call to
   * validateConfigurationParameterSettings.
   * 
   * @param aResourceManager
   *          a ResourceManager instance to use to resolve imports by name.
   *  
   * @throws ResourceInitializationException
   *           if <code>aDesc</code> is invalid
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings in <code>aDesc</code> are invalid
   */
  public void validate(ResourceManager aResourceManager) throws ResourceInitializationException, ResourceConfigurationException {
    // do validation common to all descriptor types (e.g. config parameters)
    super.validate(aResourceManager);

    // TypeSystem may not be specified for an Aggregate Analysis Engine
    if (!isPrimitive() && getAnalysisEngineMetaData().getTypeSystem() != null) {
      throw new ResourceInitializationException(
              ResourceInitializationException.AGGREGATE_AE_TYPE_SYSTEM,
              new Object[] { getSourceUrlString() });
    }
    
    //Keys in FixedFlow or LanguageCapabilityFlow must be defined
    FlowConstraints fc = getAnalysisEngineMetaData().getFlowConstraints();
    String[] keys = null;
    if (fc instanceof FixedFlow) {
      keys = ((FixedFlow)fc).getFixedFlow();
    }
    else if (fc instanceof CapabilityLanguageFlow) {
      keys = ((CapabilityLanguageFlow)fc).getCapabilityLanguageFlow();
    }
    if (keys != null) {
      for (int i = 0; i < keys.length; i++) {
        if (!getDelegateAnalysisEngineSpecifiersWithImports().containsKey(keys[i])) {
          throw new ResourceInitializationException(ResourceInitializationException.UNDEFINED_KEY_IN_FLOW,
                  new Object[]{getAnalysisEngineMetaData().getName(), keys[i], getSourceUrlString()});
        }
      }
    }
  }

  /**
   * Overrides{@link ResourceCreationSpecifier_impl#checkForInvalidParameterOverrides(ConfigurationParameter[], String, ResourceManager)}
   * to validate parameter overrides in an aggregate AE. Also logs a warning for aggregate
   * parameters with no declared overrides.
   * 
   * @param aParams
   *          an array of ConfigurationParameters
   * @param aGroupName
   *          name of groups in which these parameters are contained. Null if no group
   * @param aResourceManager
   *          a ResourceManager instance to use to resolve imports by name.
   *           
   * @throws ResourceInitializationException
   *           if there is an invalid parameter override declaration
   */
  protected void checkForInvalidParameterOverrides(ConfigurationParameter[] aParams,
          String aGroupName, ResourceManager aResourceManager) throws ResourceInitializationException {
    //make sure delegate analysis engine specifiers are resolved using the correct resource manager
    try {
      resolveImports(aResourceManager);
    } catch (InvalidXMLException e) {
      throw new ResourceInitializationException(e);
    }
    
    for (int i = 0; i < aParams.length; i++) {
      String[] overrides = aParams[i].getOverrides();
      if (overrides.length > 0 && isPrimitive()) {
        throw new ResourceInitializationException(
                ResourceInitializationException.PARAM_OVERRIDE_IN_PRIMITIVE, new Object[] {
                    aParams[i].getName(), getMetaData().getName(), getSourceUrlString() });
      } else if (overrides.length == 0 && !isPrimitive()) {
        // Were deprecated for many years ... now no longer supported.
        throw new ResourceInitializationException(
                ResourceInitializationException.INVALID_PARAM_OVERRIDE_NO_OVERRIDES, new Object[] {
                    aParams[i].getName(), getMetaData().getName(), getSourceUrlString() });
      }
      for (int j = 0; j < overrides.length; j++) {
        // overrides should be of form delegateKey/paramName
        int slashPos = overrides[j].indexOf('/');
        if (slashPos <= 0 || slashPos >= overrides[j].length()) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.INVALID_PARAM_OVERRIDE_SYNTAX, new Object[] {
                      overrides[j], aParams[i].getName(), getMetaData().getName(),
                      getSourceUrlString() });
        }
        String delegateKey = overrides[j].substring(0, slashPos);
        String paramName = overrides[j].substring(slashPos + 1);
        // get component descriptor (could be an AE or could be the FlowController)
        ResourceSpecifier componentSpecifier = getComponentSpecifier(delegateKey);
        if (componentSpecifier == null) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.INVALID_PARAM_OVERRIDE_NONEXISTENT_DELEGATE,
                  new Object[] { overrides[j], aParams[i].getName(), getMetaData().getName(),
                      delegateKey, getSourceUrlString() });
        }
        if (componentSpecifier instanceof ResourceCreationSpecifier) {
          ConfigurationParameter overriddenParam = null;
          ConfigurationParameterDeclarations delegateParamDecls = ((ResourceCreationSpecifier) componentSpecifier)
                  .getMetaData().getConfigurationParameterDeclarations();
          if (aGroupName == null) // param not in group
          {
            overriddenParam = delegateParamDecls.getConfigurationParameter(null, paramName);
            if (overriddenParam == null) {
              throw new ResourceInitializationException(
                      ResourceInitializationException.INVALID_PARAM_OVERRIDE_NONEXISTENT_PARAMETER,
                      new Object[] { overrides[j], aParams[i].getName(), getMetaData().getName(),
                          delegateKey, paramName, getSourceUrlString() });

            }
          } else {
            // make sure parameter exists in group
            overriddenParam = delegateParamDecls.getConfigurationParameter(aGroupName, paramName);
            if (overriddenParam == null) {
              throw new ResourceInitializationException(
                      ResourceInitializationException.INVALID_PARAM_OVERRIDE_NONEXISTENT_PARAMETER_IN_GROUP,
                      new Object[] { overrides[j], aParams[i].getName(), getMetaData().getName(),
                          delegateKey, paramName, aGroupName, getSourceUrlString() });
            }
          }
        }
      }
    }
  }

  /**
   * Gets the ResourceSpecifier of one a component of this aggregate, based on its key. This may be
   * the specifier of a component (i.e. delegate) AnalysisEngine, or it may be the specifier of the
   * FlowController.
   * 
   * @param key
   *          the key of the component specifier to get
   * @return the specifier for the component, null if there is no component with the given key
   * @throws ResourceInitializationException
   *           if there's a problem resolving imports
   */
  public ResourceSpecifier getComponentSpecifier(String key) throws ResourceInitializationException {
    ResourceSpecifier componentSpecifier;
    if (getFlowControllerDeclaration() != null
            && key.equals(getFlowControllerDeclaration().getKey())) {
      try {
        getFlowControllerDeclaration().resolveImports();
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }
      componentSpecifier = getFlowControllerDeclaration().getSpecifier();
    } else {
      try {
        componentSpecifier = getDelegateAnalysisEngineSpecifiers().get(key);
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }
    }
    return componentSpecifier;
  }

  /**
   * Validate SofA mappings and inputs/outputs for an aggregate AE.
   * @throws ResourceInitializationException -
   */
  protected void validateSofaMappings() throws ResourceInitializationException {
    if (this.isPrimitive())
      return;
    String aggName = this.getAnalysisEngineMetaData().getName();
    // build an actual Map (key: componentKey@/@componentSofa) from the sofa mappings
    // along the way check that all component keys and component sofa names exist
    Map<String, String> sofamap = new TreeMap<String, String>();
    SofaMapping[] sofaMappings = this.getSofaMappings();
    if (sofaMappings != null) {
      for (int s = 0; s < sofaMappings.length; s++) {
        String componentKey = sofaMappings[s].getComponentKey();
        ResourceSpecifier componentSpec = getComponentSpecifier(componentKey);
        if (componentSpec == null) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.SOFA_MAPPING_HAS_UNDEFINED_COMPONENT_KEY,
                  new Object[] { componentKey, sofaMappings[s].getAggregateSofaName(), aggName,
                      getSourceUrlString() });
        }
        String componentSofaName = sofaMappings[s].getComponentSofaName();
        if (componentSofaName == null) {
          componentSofaName = CAS.NAME_DEFAULT_SOFA;
        } else if (componentSpec instanceof AnalysisEngineDescription
                && !CAS.NAME_DEFAULT_SOFA.equals(componentSofaName)
                && !declaresSofa((AnalysisEngineDescription) componentSpec, componentSofaName)) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.SOFA_MAPPING_HAS_UNDEFINED_COMPONENT_SOFA,
                  new Object[] { componentKey, componentSofaName,
                      sofaMappings[s].getAggregateSofaName(), aggName, getSourceUrlString() });
        }

        String compoundKey = sofaMappings[s].getComponentKey() + "@/@"
                + sofaMappings[s].getComponentSofaName();
        String aggSofaName = sofaMappings[s].getAggregateSofaName();
        // check for double-mapping
        String existingMapping = sofamap.get(compoundKey);
        if (existingMapping != null && !existingMapping.equals(aggSofaName)) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.SOFA_MAPPING_CONFLICT, new Object[] {
                      sofaMappings[s].getComponentSofaName(), sofaMappings[s].getComponentKey(),
                      aggName, existingMapping, aggSofaName, getSourceUrlString() });
        } else {
          sofamap.put(compoundKey, aggSofaName);
        }
      }
    }

    // Rules for SofAs:
    // (1) Each component output sofa must be mapped to an aggregate output sofa
    // (2) Each aggregate output sofa must be mapped to a component output sofa
    // (3) Each component input sofa must be mapped to an aggregate input sofa OR a component output
    // sofa
    // (4) Each aggregate input sofa must be mapped to a component input sofa

    // From (1) and (3) we derive that:
    // Each component input sofa must be mapped to an aggregate input sofa OR an aggregate output
    // sofa
    // (which is easier to check)

    // Exception: if aggregate contains a remote component, we cannot check that remote
    // component's input or output sofas, so rules (2) and (4) cannot be checked.

    boolean containsRemote = false;
    Set<String> correctlyMappedAggregateOutputs = new HashSet<String>();
    Set<String> correctlyMappedAggregateInputs = new HashSet<String>();

    Iterator<Map.Entry<String, ResourceSpecifier>> iter;
    try {
      iter = getDelegateAnalysisEngineSpecifiers().entrySet().iterator();
    } catch (InvalidXMLException e) {
      throw new ResourceInitializationException(e);
    }
    while (iter.hasNext()) {
      Map.Entry<String, ResourceSpecifier> entry = iter.next();
      String componentKey = entry.getKey();
      ResourceSpecifier delegateSpec = entry.getValue();

      if (delegateSpec instanceof AnalysisEngineDescription) {
        Capability[] caps = ((AnalysisEngineDescription) delegateSpec).getAnalysisEngineMetaData()
                .getCapabilities();
        for (int i = 0; i < caps.length; i++) {
          // all component output sofas must be mapped to aggregate output sofas
          String[] outputSofas = caps[i].getOutputSofas();
          for (int j = 0; j < outputSofas.length; j++) {
            String aggSofa = sofamap.get(componentKey + "@/@" + outputSofas[j]);
            if (aggSofa == null) // no declared mapping, name remains unchanged
            {
              aggSofa = outputSofas[j];
            }
            if (!capabilitiesContainSofa(aggSofa, true)) {
              throw new ResourceInitializationException(
                      ResourceInitializationException.OUTPUT_SOFA_NOT_DECLARED_IN_AGGREGATE,
                      new Object[] { outputSofas[j], componentKey, aggName, getSourceUrlString() });
            }
            correctlyMappedAggregateOutputs.add(aggSofa);
          }

          // all component input sofas must be mapped to aggregate input OR output sofas
          String[] inputSofas = caps[i].getInputSofas();
          for (int j = 0; j < inputSofas.length; j++) {
            String aggSofa = sofamap.get(componentKey + "@/@" + inputSofas[j]);
            if (aggSofa == null) // no declared mapping, name remains unchanged
            {
              aggSofa = inputSofas[j];
            }
            if (!capabilitiesContainSofa(aggSofa, false) && !capabilitiesContainSofa(aggSofa, true)) {
              throw new ResourceInitializationException(
                      ResourceInitializationException.INPUT_SOFA_HAS_NO_SOURCE, new Object[] {
                          inputSofas[j], componentKey, aggName, getSourceUrlString() });
            }
            correctlyMappedAggregateInputs.add(aggSofa);
          }

          // also check default text sofa
          String aggDefSofa = sofamap.get(componentKey + "@/@" + CAS.NAME_DEFAULT_SOFA);
          if (aggDefSofa != null) {
            if (capabilitiesContainSofa(aggDefSofa, true)) {
              correctlyMappedAggregateOutputs.add(aggDefSofa);
            } else {
              correctlyMappedAggregateInputs.add(aggDefSofa);
            }
          }
        }
      } // delegateSpec is not an AnalysisEngineDescription
      else {
        containsRemote = true;
      }
    }

    if (!containsRemote) {
      // check that all aggregate outputs and inputs were mapped correclty to
      // component inputs/outputs
      Capability[] caps = this.getAnalysisEngineMetaData().getCapabilities();
      for (int i = 0; i < caps.length; i++) {
        String[] sofas = caps[i].getOutputSofas();
        for (int j = 0; j < sofas.length; j++) {
          if (!correctlyMappedAggregateOutputs.contains(sofas[j])) {
            throw new ResourceInitializationException(
                    ResourceInitializationException.AGGREGATE_SOFA_NOT_MAPPED, new Object[] {
                        sofas[j], aggName, getSourceUrlString() });
          }
        }
        sofas = caps[i].getInputSofas();
        for (int j = 0; j < sofas.length; j++) {
          if (!correctlyMappedAggregateInputs.contains(sofas[j])) {
            throw new ResourceInitializationException(
                    ResourceInitializationException.AGGREGATE_SOFA_NOT_MAPPED, new Object[] {
                        sofas[j], aggName, getSourceUrlString() });
          }
        }
      }
    }
  }

  private boolean declaresSofa(AnalysisEngineDescription aDesc, String aSofaName) {
    Capability[] caps = aDesc.getAnalysisEngineMetaData().getCapabilities();
    for (int i = 0; i < caps.length; i++) {
      String[] sofas = caps[i].getOutputSofas();
      for (int j = 0; j < sofas.length; j++) {
        if (aSofaName.equals(sofas[j])) {
          return true;
        }
      }
      sofas = caps[i].getInputSofas();
      for (int j = 0; j < sofas.length; j++) {
        if (aSofaName.equals(sofas[j])) {
          return true;
        }
      }
    }
    return false;
  }

  protected boolean capabilitiesContainSofa(String aSofaName, boolean aOutput) {
    Capability[] caps = this.getAnalysisEngineMetaData().getCapabilities();
    for (int i = 0; i < caps.length; i++) {
      String[] sofas = aOutput ? caps[i].getOutputSofas() : caps[i].getInputSofas();
      for (int j = 0; j < sofas.length; j++) {
        if (aSofaName.equals(sofas[j])) {
          return true;
        }
      }
    }
    return false;
  }
  
  @Override
  public List<MetaDataAttr> getAdditionalAttributes() {
    return Collections.singletonList(
        new MetaDataAttr(
            PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS,
            getterForAEwImports,
            null,  // no writer
            Map.class));
  }

  /**
   * Overridden to add Delegate AE Specifiers to the result list. Default introspection
   * implementation won't return it because it has no set method. We've also overridden the XML
   * import/export methods, though, so that set methods are not required.
   * 
   * @see MetaDataObject#listAttributes()
   * @deprecated never called anymore - getAdditionalAttributes called instead
   */
  @Deprecated
  public List<NameClassPair> listAttributes() {
    List<NameClassPair> result = super.listAttributes();
    result.add(new NameClassPair(PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS, Map.class
            .getName()));
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#toXML(java.io.OutputStream,
   *      boolean)
   */
  public void toXML(OutputStream aOutputStream, boolean aPreserveDelegateAnalysisEngineImports)
          throws SAXException, IOException {
    if (aPreserveDelegateAnalysisEngineImports) {
      // trick the writePropertyAsElement method into thinking that
      // imports haven't been resolved yet
      Map<String, ResourceSpecifier> tempMap = mDelegateAnalysisEngineSpecifiers;
      mDelegateAnalysisEngineSpecifiers = Collections.emptyMap();
      toXML(aOutputStream);
      mDelegateAnalysisEngineSpecifiers = tempMap;
    } else {
      toXML(aOutputStream);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#toXML(java.io.Writer, boolean)
   */
  public void toXML(Writer aWriter, boolean aPreserveDelegateAnalysisEngineImports)
          throws SAXException, IOException {
    if (aPreserveDelegateAnalysisEngineImports) {
      // trick the writePropertyAsElement method into thinking that
      // imports haven't been resolved yet
      Map<String, ResourceSpecifier> tempMap = mDelegateAnalysisEngineSpecifiers;
      mDelegateAnalysisEngineSpecifiers = Collections.emptyMap();
      toXML(aWriter);
      mDelegateAnalysisEngineSpecifiers = tempMap;
    } else {
      toXML(aWriter);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#toXML(org.xml.sax.ContentHandler,
   *      boolean, boolean)
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute,
          boolean aPreserveDelegateAnalysisEngineImports) throws SAXException {
    if (aPreserveDelegateAnalysisEngineImports) {
      // trick the writePropertyAsElement method into thinking that
      // imports haven't been resolved yet
      Map<String, ResourceSpecifier> tempMap = mDelegateAnalysisEngineSpecifiers;
      mDelegateAnalysisEngineSpecifiers = Collections.emptyMap();
      toXML(aContentHandler, aWriteDefaultNamespaceAttribute);
      mDelegateAnalysisEngineSpecifiers = tempMap;
    } else {
      toXML(aContentHandler, aWriteDefaultNamespaceAttribute);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#resolveImports(org.apache.uima.resource.ResourceManager)
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports(new HashSet<String>(), aResourceManager);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#resolveImports(java.util.Collection,
   *      org.apache.uima.resource.ResourceManager)
   */
  public void resolveImports(Collection<String> aAlreadyImportedDelegateAeUrls,
          ResourceManager aResourceManager) throws InvalidXMLException {
    // add our own URL, if known, to the collection of already imported URLs
    if (getSourceUrl() != null) {
      aAlreadyImportedDelegateAeUrls.add(getSourceUrl().toString());
    }
    // resolve delegate AE imports (and recursively resolve imports therein)
    resolveDelegateAnalysisEngineImports(aAlreadyImportedDelegateAeUrls, aResourceManager, true);
    // resolve flow controller import
    if (getFlowControllerDeclaration() != null) {
      getFlowControllerDeclaration().resolveImports(aResourceManager);
    }

    // resolve imports in metadata (type systems, indexes, type priorities)
    if (getAnalysisEngineMetaData() != null) {
      getAnalysisEngineMetaData().resolveImports(aResourceManager);
    }
    // resolve imports in resource manager configuration
    if (getResourceManagerConfiguration() != null) {
      getResourceManagerConfiguration().resolveImports(aResourceManager);
    }
  }
  

  /**
   * Resolves imports of delegate Analysis Engines. This reads from the
   * delegateAnalysisEngineSpecifiersWithImports map and populates the
   * delegateAnalysisEngineSpecifiers map. 
   * 
   * @param aResourceManager -
   * @param aRecursive If true, this method will call {@link #resolveImports(Collection, ResourceManager)} 
   *   on each delegate. If a cirular import is found, an exception will be thrown.
   * @throws InvalidXMLException -
   */
  protected void resolveDelegateAnalysisEngineImports(ResourceManager aResourceManager, boolean aRecursive) 
          throws InvalidXMLException {
    // add our own URL, if known, to the collection of enclosing aggregate URLs
    Set<String> urls = new HashSet<String>();
    if (getSourceUrl() != null) {
      urls.add(getSourceUrl().toString());
    }   
    resolveDelegateAnalysisEngineImports(urls, aResourceManager, aRecursive);
  }  

  /**
   * Resolves imports of delegate Analysis Engines. This reads from the
   * delegateAnalysisEngineSpecifiersWithImports map and populates the
   * delegateAnalysisEngineSpecifiers map. 
   * 
   * @param aEnclosingAggregateAeUrls URLs of enclosing aggregate AEs.  Used to detect circular imports.
   * @param aResourceManager - 
   * @param aRecursive If true, this method will call {@link #resolveImports(Collection, ResourceManager)} 
   *   on each delegate. If a circular import is found, an exception will be thrown.
   * @throws InvalidXMLException -
   */
  protected synchronized void resolveDelegateAnalysisEngineImports(Collection<String> aEnclosingAggregateAeUrls,
          ResourceManager aResourceManager, boolean aRecursive) throws InvalidXMLException {
    Set<String> keys = null;
    if (getDelegateAnalysisEngineSpecifiersWithImports().size() > 0) {
      keys = new HashSet<String>(); // keep track of keys we've encountered
      // so we can remove stale entries
      for (Map.Entry<String, MetaDataObject> entry : 
      	getDelegateAnalysisEngineSpecifiersWithImports().entrySet()) {
        String key = entry.getKey();
        keys.add(key);
        if (entry.getValue() instanceof Import) {
          Import aeImport = ((Import) entry.getValue());
          // see if we processed this already
          if (entry.getValue().equals(mProcessedImports.get(key))) {
            continue;
          }
          // make sure Import's relative path base is set, to allow for
          // users who create
          // new import objects
          if (aeImport instanceof Import_impl) {
            ((Import_impl) aeImport).setSourceUrlIfNull(this.getSourceUrl());
          }
          // locate import target
          URL url = aeImport.findAbsoluteUrl(aResourceManager);
  
          // check for recursive import
          if (aEnclosingAggregateAeUrls.contains(url.toString())) {
            String name = getMetaData() == null ? "<null>" : getMetaData().getName();
            throw new InvalidXMLException(InvalidXMLException.CIRCULAR_AE_IMPORT, new Object[] {
                name, url });
          }
  
          // parse import target
          XMLInputSource input;
          try {
            input = new XMLInputSource(url);
          } catch (IOException e) {
            throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                    new Object[] { url, aeImport.getSourceUrlString() }, e);
          }
          ResourceSpecifier spec = UIMAFramework.getXMLParser().parseResourceSpecifier(input);
  
          // update entry in derived mDelegateAnalysisEngineSpecifiers map.
          mDelegateAnalysisEngineSpecifiers.put(key, spec);
  
          // add to processed imports map so we don't redo
          mProcessedImports.put(key, aeImport);
  
          // now resolve imports in ths delegate
          if (spec instanceof AnalysisEngineDescription) {
            Set<String> alreadyImportedUrls = new HashSet<String>(aEnclosingAggregateAeUrls);
            alreadyImportedUrls.add(url.toString());
            ((AnalysisEngineDescription) spec).resolveImports(alreadyImportedUrls, aResourceManager);
          }
        } else {
          // not an import -- copy directly to derived mDelegateAnalysisEngineSpecifiers map.
          mDelegateAnalysisEngineSpecifiers.put(entry.getKey(), (ResourceSpecifier) entry.getValue());
          // resolve imports recursively on the delegate
          if (entry.getValue() instanceof AnalysisEngineDescription) {
            ((AnalysisEngineDescription) entry.getValue()).resolveImports(
                    aEnclosingAggregateAeUrls, aResourceManager);
          }
        }
      }
    }
    // remove stale entries
    
    if (mDelegateAnalysisEngineSpecifiers.size() > 0) {
      final Set<Map.Entry<String, ResourceSpecifier>> staleEntries = mDelegateAnalysisEngineSpecifiers.entrySet();
      List<String> staleKeys = new ArrayList<String>();
      for (Map.Entry<String, ResourceSpecifier> entry : staleEntries) {
        String key = entry.getKey();
        if (null == keys || !keys.contains(key)) {
          staleKeys.add(key);
        }
      }
      for (String key : staleKeys) {
        mDelegateAnalysisEngineSpecifiers.remove(key);
        mProcessedImports.remove(key);
      }
    }
  }

  /**
   * Overridden to handle XML export of the DelegateAnalysisEngineSpecifiers attribute. This
   * attribute has a value of type <code>Map</code>, which is not handled by the default XML
   * export logic.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#writePropertyAsElement(PropertyXmlInfo, String)
   * @param aPropInfo -
   * @param aNamespace - 
   * @throws SAXException -
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {

    if (PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS.equals(aPropInfo.propertyName)) {
      // special logic here -- if imports have been resolved, then we want
      // to write
      // out the XML with those resolved imports. If imports have not been
      // resolved,
      // (mDelegateAnalysisEngineSpecifiers is empty), then we want to
      // write out
      // the original XML, which has the import declarations.
      String propName = PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS;
      if (mDelegateAnalysisEngineSpecifiers.isEmpty()) {
        propName = PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS;
      }
      writeMapPropertyToXml(propName, ELEM_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS, "key",
              "delegateAnalysisEngine", aPropInfo.omitIfNull, aNamespace);
    } else {
      // for all other attributes, use the default superclass behavior
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  /**
   * Overridden to handle XML import of the DelegateAnalysisEngineSpecifiers attribute. This
   * attribute has a value of type <code>Map</code>, which is not handled by the default XML
   * import logic.
   * 
   * @see MetaDataObject_impl#readPropertyValueFromXMLElement(PropertyXmlInfo, Element, XMLParser, XMLParser.ParsingOptions)
   */
  @Override
  protected void readPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo, Element aElement,
          XMLParser aParser, XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    String propName = aPropXmlInfo.propertyName;
    if (PROP_DELEGATE_ANALYSIS_ENGINE_SPECIFIERS_WITH_IMPORTS.equals(propName)) {
      readMapPropertyFromXml(propName, aElement, "key", "delegateAnalysisEngine", aParser,
              aOptions, false);
    } else {
      // for all other attributes, use the default superclass behavior
      super.readPropertyValueFromXMLElement(aPropXmlInfo, aElement, aParser, aOptions);
    }
  }

  /**
   * Overridden to set default operational properties if they are not specified in descriptor.
   * @param aElement -
   * @param aParser - 
   * @param aOptions -
   * @throws InvalidXMLException - 
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    if (getAnalysisEngineMetaData().getOperationalProperties() == null) {
      OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
              .createOperationalProperties();
      opProps.setModifiesCas(true);
      opProps.setMultipleDeploymentAllowed(true);
      getAnalysisEngineMetaData().setOperationalProperties(opProps);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /**
   * Static method to get XmlizationInfo, used by subclasses to set up their own XmlizationInfo.
   * @return XmlizationInfo, used by subclasses to set up their own XmlizationInfo
   */
  protected static XmlizationInfo getXmlizationInfoForClass() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "analysisEngineDescription", new PropertyXmlInfo[] {
              new PropertyXmlInfo("frameworkImplementation"),
              new PropertyXmlInfo("primitive"),
              new PropertyXmlInfo("annotatorImplementationName"),
              new PropertyXmlInfo("delegateAnalysisEngineSpecifiersWithImports",
                      "delegateAnalysisEngineSpecifiers"), // NOTE: custom
              // XMLization
              new PropertyXmlInfo("flowControllerDeclaration", null),
              new PropertyXmlInfo("metaData", null),
              new PropertyXmlInfo("externalResourceDependencies"),
              new PropertyXmlInfo("resourceManagerConfiguration", null),
              new PropertyXmlInfo("sofaMappings") });
}