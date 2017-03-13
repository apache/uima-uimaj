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

package org.apache.uima.resource.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link ResourceCreationSpecifier}.
 * 
 */
public class ResourceCreationSpecifier_impl extends MetaDataObject_impl implements
        ResourceCreationSpecifier {

  private String mImplementationName;

  private String mFrameworkImplementation;

  private ResourceMetaData mMetaData;

  private List<ExternalResourceDependency> mExternalResourceDependencies = new ArrayList<ExternalResourceDependency>();

  private ResourceManagerConfiguration mResourceManagerConfiguration;

  static final long serialVersionUID = 7946890459654653436L;

  /**
   * @see ResourceCreationSpecifier#getFrameworkImplementation()
   */
  public String getFrameworkImplementation() {
    return mFrameworkImplementation;
  }

  /**
   * @see ResourceCreationSpecifier#setFrameworkImplementation(java.lang.String)
   */
  public void setFrameworkImplementation(String aFrameworkImplementation) {
    mFrameworkImplementation = aFrameworkImplementation;
  }

  /**
   * @see ResourceCreationSpecifier#getImplementationName()
   */
  public String getImplementationName() {
    return mImplementationName;
  }

  /**
   * @see ResourceCreationSpecifier#setImplementationName(java.lang.String)
   */
  public void setImplementationName(String aImplementationName) {
    mImplementationName = aImplementationName;

  }

  /**
   * @see ResourceCreationSpecifier#getMetaData()
   */
  public ResourceMetaData getMetaData() {
    return mMetaData;
  }

  /**
   * Sets the MetaData for this <code>ResourceCreationSpecifier_impl</code>. Users should not do
   * this, so this method is not published through the <code>ResourceCreationSpecifier</code>
   * interface.
   * 
   * @param aMetaData
   *          metadata to assign
   */
  public void setMetaData(ResourceMetaData aMetaData) {
    mMetaData = aMetaData;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getExternalResourceDependencies()
   */
  public ExternalResourceDependency[] getExternalResourceDependencies() {
    ExternalResourceDependency[] result = new ExternalResourceDependency[mExternalResourceDependencies
            .size()];
    mExternalResourceDependencies.toArray(result);
    return result;
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#setExternalResourceDependencies(ExternalResourceDependency[])
   */
  public void setExternalResourceDependencies(ExternalResourceDependency[] aDependencies) {
    // can't just clear the ArrayList since that breaks clone(). Create a new list.
    mExternalResourceDependencies = new ArrayList<ExternalResourceDependency>();
    if (aDependencies != null) {
      for (int i = 0; i < aDependencies.length; i++) {
        mExternalResourceDependencies.add(aDependencies[i]);
      }
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngineDescription#getExternalResourceDependency(java.lang.String)
   */
  public ExternalResourceDependency getExternalResourceDependency(String aKey) {
    for (ExternalResourceDependency dep : mExternalResourceDependencies) {
      if (aKey.equals(dep.getKey()))
        return dep;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceCreationSpecifier#getResourceManagerConfiguration()
   */
  public ResourceManagerConfiguration getResourceManagerConfiguration() {
    return mResourceManagerConfiguration;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceCreationSpecifier#setResourceManagerConfiguration(org.apache.uima.resource.metadata.ResourceManagerConfiguration)
   */
  public void setResourceManagerConfiguration(
          ResourceManagerConfiguration aResourceManagerConfiguration) {
    mResourceManagerConfiguration = aResourceManagerConfiguration;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceCreationSpecifier#doFullValidation()
   */
  public void doFullValidation() throws ResourceInitializationException {
    doFullValidation(UIMAFramework.newDefaultResourceManager());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceCreationSpecifier#doFullValidation(org.apache.uima.resource.ResourceManager)
   */
  public void doFullValidation(ResourceManager aResourceManager)
          throws ResourceInitializationException {
    // try to instantiate dummy resource - this checks config params
    // and resources
    DummyResource dummy = new DummyResource();
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Resource.PARAM_RESOURCE_MANAGER, aResourceManager);
    dummy.initialize(this, params);

    // subclasses should override to check CAS creation and
    // loading of the user-supplied CAS as appropriate
  }

  /**
   * Determines if the AnalysisEngineDescription is valid. An exception is thrown if it is not
   * valid. This should be called from this Analysis Engine's initialize method. Note this does not
   * check configuration parameter settings - that must be done by an explicit call to
   * validateConfigurationParameterSettings.
   * 
   * @throws ResourceInitializationException
   *           if <code>aDesc</code> is invalid
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings in <code>aDesc</code> are invalid
   */
  public final void validate() throws ResourceInitializationException, ResourceConfigurationException {
    validate(UIMAFramework.newDefaultResourceManager());
  }
  
  

  /* (non-Javadoc)
   * @see org.apache.uima.resource.ResourceCreationSpecifier#validate(org.apache.uima.resource.ResourceManager)
   */
  public void validate(ResourceManager aResourceManager) throws ResourceInitializationException, ResourceConfigurationException {
    // Validate configuration parameter declarations (but not settings)
    validateConfigurationParameters(aResourceManager); 
  }

  /**
   * Validates configuration parameters within this Resource, and throws an exception if they are
   * not valid.
   * <p>
   * This method checks to make sure that there are no duplicate configuration group names or
   * duplicate parameter names within groups. For aggregates, it also checks that parameter
   * overrides are valid, and logs a warning for parameters with no overrides. (For primitives,
   * there should be no overrides.)
   * 
   * @param aResourceManager used to resolve import by name.  This is necessary to validate
   *         configuration parameter overrides.
   * 
   * @throws ResourceInitializationException
   *           if the configuration parameters are invalid
   */
  protected void validateConfigurationParameters(ResourceManager aResourceManager) throws ResourceInitializationException {
    ConfigurationParameterDeclarations cfgParamDecls = getMetaData()
            .getConfigurationParameterDeclarations();
    ConfigurationParameter[] params = cfgParamDecls.getConfigurationParameters();
    if (params.length > 0) {
      // check for duplicate names
      checkForDuplicateParameterNames(params);
      checkForInvalidParameterOverrides(params, null, aResourceManager);
    } else {
      ConfigurationParameter[] commonParams = cfgParamDecls.getCommonParameters();
      // check for duplicates in common params
      Set<String> commonParamNames = new HashSet<String>();
      if (commonParams != null) {
        for (int i = 0; i < commonParams.length; i++) {
          if (!commonParamNames.add(commonParams[i].getName())) {
            throw new ResourceInitializationException(
                    ResourceInitializationException.DUPLICATE_CONFIGURATION_PARAMETER_NAME,
                    new Object[] { commonParams[i].getName(), getMetaData().getName(),
                        commonParams[i].getSourceUrlString() });
          }
        }
      }
      // check for duplicates in groups
      ConfigurationGroup[] groups = cfgParamDecls.getConfigurationGroups();
      if (groups != null) {
        Map<String, Set<String>> groupToParamSetMap = new HashMap<String, Set<String>>(); // map from group name to HashSet of param names
        // in that group
        for (int i = 0; i < groups.length; i++) {
          String[] names = groups[i].getNames();
          for (int j = 0; j < names.length; j++) {
            Set<String> paramNamesInGroup = groupToParamSetMap.get(names[j]);
            if (paramNamesInGroup == null) {
              // first time we've seen this group. create an entry and add common params
              paramNamesInGroup = new HashSet<String>(commonParamNames);
            }

            // check for duplicate parameter names
            ConfigurationParameter[] paramsInGroup = groups[i].getConfigurationParameters();
            if (paramsInGroup != null) {
              for (int k = 0; k < paramsInGroup.length; k++) {
                if (!paramNamesInGroup.add(paramsInGroup[k].getName())) {
                  throw new ResourceInitializationException(
                          ResourceInitializationException.DUPLICATE_CONFIGURATION_PARAMETER_NAME,
                          new Object[] { paramsInGroup[k].getName(), getMetaData().getName(),
                              paramsInGroup[k].getSourceUrlString() });
                }
              }
            }
            checkForInvalidParameterOverrides(paramsInGroup, names[j], aResourceManager);
            if (commonParams != null) {
              checkForInvalidParameterOverrides(commonParams, names[j], aResourceManager);
            }
          }
        }
      }
    }
  }

  /**
   * Checks for duplicate parameter names and throws an exception if any are found.
   * 
   * @param aParams
   *          an array of ConfigurationParameters
   * 
   * @throws ResourceInitializationException
   *           if there is a duplicate parameter name in the arrays
   */
  protected void checkForDuplicateParameterNames(ConfigurationParameter[] aParams)
          throws ResourceInitializationException {
    Set<String> paramNames = new HashSet<String>();
    for (int i = 0; i < aParams.length; i++) {
      if (!paramNames.add(aParams[i].getName())) {
        throw new ResourceInitializationException(
                ResourceInitializationException.DUPLICATE_CONFIGURATION_PARAMETER_NAME,
                new Object[] { aParams[i].getName(), getMetaData().getName(),
                    aParams[i].getSourceUrlString() });
      }
    }
  }

  /**
   * Checks parameter overrides and throws a ResourceInitializationException if they are invalid.
   * Note that since overrides are invalid in primitive components, this default implementation
   * throws an exception for ANY parameter override it finds. AnalysisEngineDescription_impl
   * overrides this method to correctly handle parameter overrides in aggregates.
   * 
   * @param aParams
   *          an array of ConfigurationParameters
   * @param aGroupName
   *          name of groups in which these parameters are contained. Null if no group
   * @param aResourceManager used to resolve imports by name.
   *          
   * @throws ResourceInitializationException
   *           if there is an invalid parameter override declaration
   */
  protected void checkForInvalidParameterOverrides(ConfigurationParameter[] aParams,
          String aGroupName, ResourceManager aResourceManager) throws ResourceInitializationException {
    for (int i = 0; i < aParams.length; i++) {
      String[] overrides = aParams[i].getOverrides();
      if (overrides.length > 0) {
        throw new ResourceInitializationException(
                ResourceInitializationException.PARAM_OVERRIDE_IN_PRIMITIVE,
                new Object[] { aParams[i].getName(), getMetaData().getName(),
                    aParams[i].getSourceUrlString() });
      }
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "resourceCreationSpecifier", new PropertyXmlInfo[] {
              new PropertyXmlInfo("frameworkImplementation"),
              new PropertyXmlInfo("implementationName"), 
              new PropertyXmlInfo("metaData", null),
              new PropertyXmlInfo("externalResourceDependencies"),
              new PropertyXmlInfo("externalResources"),
              new PropertyXmlInfo("resourceManagerConfiguration", null), });

  // used by doFullValidation
  private static class DummyResource extends Resource_ImplBase {
  }
}
