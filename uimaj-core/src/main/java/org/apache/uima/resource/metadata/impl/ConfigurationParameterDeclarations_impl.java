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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class ConfigurationParameterDeclarations_impl extends MetaDataObject_impl implements
        ConfigurationParameterDeclarations {

  static final long serialVersionUID = -2248322904617280983L;

  /** Configuration Parameters that are not in any group */
  private ConfigurationParameter[] mConfigurationParameters = new ConfigurationParameter[0];

  /** Configuration Groups */
  private ConfigurationGroup[] mConfigurationGroups = new ConfigurationGroup[0];

  /** Parameters common to all groups */
  private ConfigurationParameter[] mCommonParameters = new ConfigurationParameter[0];

  /** Name of the default group */
  private String mDefaultGroupName;

  /** Configuration parameter search strategy */
  private String mSearchStrategy;

  /**
   * @see ConfigurationParameterDeclarations#getConfigurationParameters()
   */
  public ConfigurationParameter[] getConfigurationParameters() {
    return mConfigurationParameters;
  }

  /**
   * @see ConfigurationParameterDeclarations#setConfigurationParameters(ConfigurationParameter[])
   */
  public void setConfigurationParameters(ConfigurationParameter[] aParams) {
    if (aParams == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aParams", "setConfigurationParameters" });
    }
    mConfigurationParameters = aParams;
  }

  /**
   * @see ConfigurationParameterDeclarations#getCommonParameters()
   */
  public ConfigurationParameter[] getCommonParameters() {
    return mCommonParameters;
  }

  /**
   * @see ConfigurationParameterDeclarations#getConfigurationGroups()
   */
  public ConfigurationGroup[] getConfigurationGroups() {
    return mConfigurationGroups;
  }

  /**
   * @see ConfigurationParameterDeclarations#getDefaultGroupName()
   */
  public String getDefaultGroupName() {
    return mDefaultGroupName;
  }

  /**
   * @see ConfigurationParameterDeclarations#getSearchStrategy()
   */
  public String getSearchStrategy() {
    return mSearchStrategy;
  }

  /**
   * @see ConfigurationParameterDeclarations#setCommonParameters(ConfigurationParameter[])
   */
  public void setCommonParameters(ConfigurationParameter[] aParams) {
    if (aParams == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aParams", "setCommonParameters" });
    }
    mCommonParameters = aParams;
  }

  /**
   * @see ConfigurationParameterDeclarations#setConfigurationGroups(ConfigurationGroup[])
   */
  public void setConfigurationGroups(ConfigurationGroup[] aGroups) {
    if (aGroups == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aGroups", "setConfigurationGroups" });
    }
    mConfigurationGroups = aGroups;
  }

  /**
   * @see ConfigurationParameterDeclarations#setDefaultGroupName(String)
   */
  public void setDefaultGroupName(String aGroupName) {
    mDefaultGroupName = aGroupName;
  }

  /**
   * @see ConfigurationParameterDeclarations#setSearchStrategy(java.lang.String)
   */
  public void setSearchStrategy(String aStrategy) {
    mSearchStrategy = aStrategy;
  }

  /**
   * @see ConfigurationParameterDeclarations#getConfigurationParameter(String, String)
   */
  public ConfigurationParameter getConfigurationParameter(String aGroupName, String aParamName) {
    if (aGroupName == null) {
      // look in list of params that are in no group
      ConfigurationParameter[] params = getConfigurationParameters();
      return _getConfigurationParameter(params, aParamName);
    } else {
      // look in common parameters
      ConfigurationParameter[] commonParams = getCommonParameters();
      ConfigurationParameter p = _getConfigurationParameter(commonParams, aParamName);
      if (p == null) {
        // find group
        ConfigurationGroup[] groups = getConfigurationGroupDeclarations(aGroupName);
        for (int i = 0; i < groups.length; i++) {
          ConfigurationParameter[] paramsInGroup = groups[i].getConfigurationParameters();
          p = _getConfigurationParameter(paramsInGroup, aParamName);
          if (p != null)
            break;
        }
      }
      return p;
    }
  }

  /**
   * @see ConfigurationParameterDeclarations#getConfigurationGroupDeclarations(String)
   */
  public ConfigurationGroup[] getConfigurationGroupDeclarations(String aGroupName) {
    List<ConfigurationGroup> results = new ArrayList<ConfigurationGroup>();
    ConfigurationGroup[] grps = getConfigurationGroups();
    if (grps != null) {
      for (int i = 0; i < grps.length; i++) {
        String[] names = grps[i].getNames();
        for (int j = 0; j < names.length; j++) {
          if (aGroupName.equals(names[j])) {
            results.add(grps[i]);
            break;
          }
        }
      }
    }
    ConfigurationGroup[] resultArr = new ConfigurationGroup[results.size()];
    results.toArray(resultArr);
    return resultArr;
  }

  /**
   * 
   * @see ConfigurationParameterDeclarations#addConfigurationParameter(ConfigurationParameter)
   */
  public void addConfigurationParameter(ConfigurationParameter aConfigurationParameter) {
    ConfigurationParameter[] current = getConfigurationParameters();
    ConfigurationParameter[] newArr = new ConfigurationParameter[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aConfigurationParameter;
    setConfigurationParameters(newArr);
  }

  /**
   * 
   * @see ConfigurationParameterDeclarations#removeConfigurationParameter(ConfigurationParameter)
   */
  public void removeConfigurationParameter(ConfigurationParameter aConfigurationParameter) {
    ConfigurationParameter[] current = getConfigurationParameters();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aConfigurationParameter) {
        ConfigurationParameter[] newArr = new ConfigurationParameter[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setConfigurationParameters(newArr);
        break;
      }
    }
  }

  /**
   * @see ConfigurationParameterDeclarations#addConfigurationParameter(ConfigurationParameter)
   */
  public void addCommonParameter(ConfigurationParameter aConfigurationParameter) {
    ConfigurationParameter[] current = getCommonParameters();
    ConfigurationParameter[] newArr = new ConfigurationParameter[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aConfigurationParameter;
    setCommonParameters(newArr);
  }

  /**
   * @see ConfigurationParameterDeclarations#removeConfigurationParameter(ConfigurationParameter)
   */
  public void removeCommonParameter(ConfigurationParameter aConfigurationParameter) {
    ConfigurationParameter[] current = getCommonParameters();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aConfigurationParameter) {
        ConfigurationParameter[] newArr = new ConfigurationParameter[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setCommonParameters(newArr);
        break;
      }
    }
  }

  /**
   * @see ConfigurationParameterDeclarations#addConfigurationGroup(ConfigurationGroup)
   */
  public void addConfigurationGroup(ConfigurationGroup aConfigurationGroup) {
    ConfigurationGroup[] current = getConfigurationGroups();
    ConfigurationGroup[] newArr = new ConfigurationGroup[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aConfigurationGroup;
    setConfigurationGroups(newArr);
  }

  /**
   * @see ConfigurationParameterDeclarations#removeConfigurationGroup(ConfigurationGroup)
   */
  public void removeConfigurationGroup(ConfigurationGroup aConfigurationGroup) {
    ConfigurationGroup[] current = getConfigurationGroups();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aConfigurationGroup) {
        ConfigurationGroup[] newArr = new ConfigurationGroup[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setConfigurationGroups(newArr);
        break;
      }
    }
  }

  /* Utility method */
  protected ConfigurationParameter _getConfigurationParameter(ConfigurationParameter[] aParams,
          String aName) {
    if (aParams != null) {
      for (int i = 0; i < aParams.length; i++) {
        if (aName.equals(aParams[i].getName()))
          return aParams[i];
      }
    }
    return null;
  }

  /**
   * Overridden to provide custom XMLization.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    // read defaultGroup and searchStrategy from attributes
    String defaultGroup = aElement.getAttribute("defaultGroup");
    if (defaultGroup.length() > 0) {
      setDefaultGroupName(defaultGroup);
    } else {
      setDefaultGroupName(null);
    }

    String searchStrategy = aElement.getAttribute("searchStrategy");
    if (searchStrategy.length() > 0) {
      setSearchStrategy(searchStrategy);
    } else {
      setSearchStrategy(null);
    }

    // read parameters, commonParameters, and configurationGroups
    List<XMLizable> params = new ArrayList<XMLizable>();
    List<XMLizable> groups = new ArrayList<XMLizable>();
    NodeList childNodes = aElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node curNode = childNodes.item(i);
      if (curNode instanceof Element) {
        Element elem = (Element) curNode;
        if ("configurationParameter".equals(elem.getTagName())) {
          params.add(aParser.buildObject(elem, aOptions));
        } else if ("commonParameters".equals(elem.getTagName())) {
          final PropertyXmlInfo commonParametersPropInfo = new PropertyXmlInfo("commonParameters");
          readPropertyValueFromXMLElement(commonParametersPropInfo, elem, aParser, aOptions);
        } else if ("configurationGroup".equals(elem.getTagName())) {
          groups.add(aParser.buildObject(elem, aOptions));
        } else {
          throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT, new Object[] { elem
                  .getTagName() });
        }
      }
    }
    ConfigurationParameter[] paramArr = new ConfigurationParameter[params.size()];
    params.toArray(paramArr);
    setConfigurationParameters(paramArr);
    ConfigurationGroup[] groupArr = new ConfigurationGroup[groups.size()];
    groups.toArray(groupArr);
    setConfigurationGroups(groupArr);
  }

  /**
   * Overridden to return defaultGroup and searchStrategy as XML attributes.
   * 
   * 9/2013: superclasses don't have this method, so can't be overridden.
   * 9/2013: method never called
   * @return - 
   */
  protected String getXMLAttributeString() {
    StringBuffer buf = new StringBuffer();
    if (getDefaultGroupName() != null) {
      buf.append("defaultGroup = \"");
      buf.append(getDefaultGroupName());
      buf.append('"');
    }
    if (getSearchStrategy() != null) {
      if (buf.length() > 0) {
        buf.append(' ');
      }
      buf.append("searchStrategy = \"");
      buf.append(getSearchStrategy());
      buf.append('"');
    }
    return buf.toString();
  }

  /**
   * @see MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = new AttributesImpl();
    if ((getDefaultGroupName() != null) && (!getDefaultGroupName().equals(""))) {
      attrs.addAttribute("", "defaultGroup", "defaultGroup", "string", getDefaultGroupName());
    }
    if ((getSearchStrategy() != null) && (!getSearchStrategy().equals(""))) {
      attrs.addAttribute("", "searchStrategy", "searchStrategy", "string", getSearchStrategy());
    }
    return attrs;
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    // NOTE: custom XMLization is used for reading. This information
    // is only used for writing.
    return new XmlizationInfo("configurationParameters", new PropertyXmlInfo[] {
        new PropertyXmlInfo("configurationParameters", null),
        new PropertyXmlInfo("commonParameters", "commonParameters"),
        new PropertyXmlInfo("configurationGroups", null) });
  }
}
