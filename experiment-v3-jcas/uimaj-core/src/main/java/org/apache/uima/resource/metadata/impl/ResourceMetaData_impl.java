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

package org.apache.uima.resource.metadata.impl;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;

/**
 * Reference implementation of {@link ResourceMetaData}.
 * 
 * 
 */

public class ResourceMetaData_impl extends MetaDataObject_impl implements ResourceMetaData {

  static final long serialVersionUID = 3408359518094534817L;

  /** UUID of the Resource */
  private String mUUID;

  /** Name of the Resource */
  private String mName;

  /** Description of the Resource */
  private String mDescription;

  /** Version number of the Resource */
  private String mVersion;

  /** Vendor of the Resource */
  private String mVendor;

  /** Copyright notice for the Resource */
  private String mCopyright;

  /** Configuration Parameter Declarations for the Resource */
  private ConfigurationParameterDeclarations mConfigurationParameterDeclarations = new ConfigurationParameterDeclarations_impl();

  /** Configuration Parameter Settings for the Resource */
  private ConfigurationParameterSettings mConfigurationParameterSettings = new ConfigurationParameterSettings_impl();

  /**
   * @see ResourceMetaData#resolveImports()
   */
  public void resolveImports() throws InvalidXMLException {
    // does nothing by default; may be overriden in subclasses

  }

  /**
   * @see ResourceMetaData#resolveImports(ResourceManager)
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    // does nothing by default; may be overriden in subclasses
  }

  /**
   * Creates a new <code>ResourceMetaData_impl</code>.
   */
  public ResourceMetaData_impl() {
  }

  /**
   * @see ResourceMetaData#getUUID()
   */
  public String getUUID() {
    return mUUID;
  }

  /**
   * @see ResourceMetaData#setUUID(String)
   */
  public void setUUID(String aUUID) {
    mUUID = aUUID;
  }

  /**
   * @see ResourceMetaData#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see ResourceMetaData#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see ResourceMetaData#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see ResourceMetaData#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see ResourceMetaData#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see ResourceMetaData#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see ResourceMetaData#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see ResourceMetaData#setVendor(String)
   */
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /**
   * @see ResourceMetaData#getCopyright()
   */
  public String getCopyright() {
    return mCopyright;
  }

  /**
   * @see ResourceMetaData#setCopyright(String)
   */
  public void setCopyright(String aCopyright) {
    mCopyright = aCopyright;
  }

  /**
   * @see ResourceMetaData#getConfigurationParameterSettings()
   */
  public ConfigurationParameterSettings getConfigurationParameterSettings() {
    return mConfigurationParameterSettings;
  }

  /**
   * @see ResourceMetaData#setConfigurationParameterSettings(ConfigurationParameterSettings)
   */
  public void setConfigurationParameterSettings(ConfigurationParameterSettings aSettings) {
    mConfigurationParameterSettings = aSettings;
  }

  /**
   * @see ResourceMetaData#getConfigurationParameterDeclarations()
   */
  public ConfigurationParameterDeclarations getConfigurationParameterDeclarations() {
    return mConfigurationParameterDeclarations;
  }

  /**
   * @see ResourceMetaData#setConfigurationParameterDeclarations(ConfigurationParameterDeclarations)
   */
  public void setConfigurationParameterDeclarations(ConfigurationParameterDeclarations aDeclarations) {
    mConfigurationParameterDeclarations = aDeclarations;
  }

  /**
   * Validates configuration parameter settings within this Resource MetaData, and throws an
   * exception if they are not valid.
   * <p>
   * This method checks to make sure that each configuration parameter setting corresponds to an
   * declared configuration parameter, and that the data types are compatible. It does NOT check
   * that all mandatory parameters have been assigned values - this should be done at resource
   * initialization time and not before.
   * <P>
   * NOTE: this method can cause a change to the ConfigurationParameterSettings object in the case
   * where the value of a parameter is an empty Object[] and the parameter type is an array of a
   * different type. In this case the empty object array will be replaced by an empty array of the
   * appropriate type.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   * 
   * @see ResourceMetaData#validateConfigurationParameterSettings()
   */
  public void validateConfigurationParameterSettings() throws ResourceConfigurationException {
    ConfigurationParameterDeclarations cfgParamDecls = getConfigurationParameterDeclarations();
    ConfigurationParameterSettings cfgParamSettings = getConfigurationParameterSettings();

    // check that all settings refer to declared parameters and are of the correct data type
    // Must check both the group-less ones AND any group ones'
    // For backwards compatibility (see Jira 3123) if have some group-less settings and 
    // if special environment variable is set then ignore any errors in group parameter settings.
    // NOTE - for 2.4.1 act as if backwards compatibility is enabled.
    boolean support240bug = false;
    NameValuePair[] nvps = cfgParamSettings.getParameterSettings();
    if (nvps.length > 0) {
      validateConfigurationParameterSettings(nvps, null, cfgParamDecls);
      support240bug = true; // System.getenv("UIMA_Jira3123") != null;  // restore this post 2.4.1
    }
    
    try {
      Map<String, NameValuePair[]> settingsForGroups = cfgParamSettings.getSettingsForGroups();
      Set<Map.Entry<String, NameValuePair[]>> entrySet = settingsForGroups.entrySet();
      Iterator<Entry<String, NameValuePair[]>> it = entrySet.iterator();
      while (it.hasNext()) {
        Map.Entry<String, NameValuePair[]> entry = it.next();
        String groupName = entry.getKey();
        NameValuePair[] gnvps = entry.getValue();
        if (gnvps != null) {
          validateConfigurationParameterSettings(gnvps, groupName, cfgParamDecls);
        }
      }
    } catch (ResourceConfigurationException e) {
      // Propagate the exception unless in back-level compatibility mode
      // If the error was ignored in old releases describe the work-around
      if (!support240bug) {
        if (nvps.length > 0) {
          UIMAFramework.getLogger().log(Level.SEVERE, "To restore back-level support for this error set environment variable UIMA_Jira3123");
        }
        throw e;
      }
      UIMAFramework.getLogger().log(Level.WARNING, "Ignoring error in parameter setting: " + e.getMessage());
    }
  }

  /**
   * Validates configuration parameter settings within a group.
   * 
   * @param aNVPs
   *          the parameter settings
   * @param aGroupName
   *          the group
   * @param aParamDecls
   *          Configuration Parameter Declarations
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   */
  protected void validateConfigurationParameterSettings(NameValuePair[] aNVPs, String aGroupName,
          ConfigurationParameterDeclarations aParamDecls) throws ResourceConfigurationException {
    for (int i = 0; i < aNVPs.length; i++) {
      // look up the parameter info
      String name = aNVPs[i].getName();
      if (name == null) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "name", "nameValuePair" });
      }
      ConfigurationParameter param = aParamDecls.getConfigurationParameter(aGroupName, name);
      if (param == null) {
        if (aGroupName == null) {
          throw new ResourceConfigurationException(
                  ResourceConfigurationException.NONEXISTENT_PARAMETER, new Object[] { name,
                      getName() });
        } else {
          throw new ResourceConfigurationException(
                  ResourceConfigurationException.NONEXISTENT_PARAMETER_IN_GROUP, new Object[] {
                      name, aGroupName, getName() });
        }
      } else {
        // check datatype
        validateConfigurationParameterDataTypeMatch(param, aNVPs[i]);
      }
    }
  }

  /**
   * Validate that a value is of an appropriate data type for assignment to the given parameter.
   * <P>
   * NOTE: this method can cause a change to the NameValuePair object in the case where the value of
   * a parameter is an empty Object[] and the parameter type is an array of a different type. In
   * this case the empty object array will be replaced by an empty array of the appropriate type.
   * 
   * @param aParam
   *          configuration parameter
   * @param aNVP
   *          name value pair containing candidate value
   * 
   * @throws ResourceConfigurationException
   *           if the data types do not match
   */
  protected void validateConfigurationParameterDataTypeMatch(ConfigurationParameter aParam,
          NameValuePair aNVP) throws ResourceConfigurationException {
    String paramName = aParam.getName();
    String paramType = aParam.getType();
    if (aNVP.getValue() == null) {
      throw new ResourceConfigurationException(ResourceConfigurationException.CONFIG_SETTING_ABSENT,
              new Object[] { paramName });
    }
    Class<?> valClass = aNVP.getValue().getClass();

    if (aParam.isMultiValued()) // value must be an array
    {
      if (!valClass.isArray()) {
        throw new ResourceConfigurationException(ResourceConfigurationException.ARRAY_REQUIRED,
                new Object[] { paramName, getName() });
      }
      valClass = valClass.getComponentType();
      // check for zero-length array special case
      if (Array.getLength(aNVP.getValue()) == 0 && valClass.equals(Object.class)) {
        aNVP.setValue(Array.newInstance(getClassForParameterType(paramType), 0));
        return;
      }
    }

    if (valClass != getClassForParameterType(paramType)) {
      throw new ResourceConfigurationException(
              ResourceConfigurationException.PARAMETER_TYPE_MISMATCH, new Object[] { getName(),
                  valClass.getName(), paramName, paramType });
      /*  Parameter type mismatch in component "{0}".  A value of class {1} cannot be 
          assigned to the configuration parameter {2}, which has type {3}.
       */
    }
  }

  /**
   * Gets the expected Java class for the given parameter type name.
   * 
   * @param paramType
   *          parameter type name from ConfigurationParameterDeclarations
   * 
   * @return expected Java class for parameter values of this type
   */
  protected Class<?> getClassForParameterType(String paramType) {
    if (ConfigurationParameter.TYPE_STRING.equals(paramType)) {
      return String.class;
    } else if (ConfigurationParameter.TYPE_BOOLEAN.equals(paramType)) {
      return Boolean.class;
    } else if (ConfigurationParameter.TYPE_INTEGER.equals(paramType)) {
      return Integer.class;
    } else if (ConfigurationParameter.TYPE_FLOAT.equals(paramType)) {
      return Float.class;
    } else
      return null;
  }

  /**
   * Overridden to validate configuration parameter data types immediately after parsing is
   * complete.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    try {
      validateConfigurationParameterSettings();
    } catch (ResourceConfigurationException e) {
      throw new InvalidXMLException(e);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /**
   * Static method to get XmlizationInfo, used by subclasses to set up their own XmlizationInfo.
   * @return -
   */
  protected static XmlizationInfo getXmlizationInfoForClass() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("resourceMetaData",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", false),
              new PropertyXmlInfo("description"), new PropertyXmlInfo("version"),
              new PropertyXmlInfo("vendor"), new PropertyXmlInfo("copyright"),
              new PropertyXmlInfo("configurationParameterDeclarations", null),
              new PropertyXmlInfo("configurationParameterSettings", null) });

}
