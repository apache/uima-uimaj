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
import java.util.StringTokenizer;

import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Reference implementation of {@link ConfigurationGroup}.
 * 
 * 
 */
public class ConfigurationGroup_impl extends MetaDataObject_impl implements ConfigurationGroup {

  static final long serialVersionUID = 4220504881786100821L;

  /**
   * Group names.
   */
  private String[] mNames;

  /**
   * Parameters contained within the group(s).
   */
  private ConfigurationParameter[] mConfigurationParameters = new ConfigurationParameter[0];

  /**
   * @see ConfigurationGroup#getNames()
   */
  public String[] getNames() {
    return mNames;
  }

  /**
   * @see ConfigurationGroup#setNames(java.lang.String[])
   */
  public void setNames(String[] aNames) {
    mNames = aNames;
  }

  /**
   * @see ConfigurationGroup#getConfigurationParameters()
   */
  public ConfigurationParameter[] getConfigurationParameters() {
    return mConfigurationParameters;
  }

  /**
   * @see ConfigurationGroup#setConfigurationParameters(ConfigurationParameter[])
   */
  public void setConfigurationParameters(ConfigurationParameter[] aParams) {
    mConfigurationParameters = aParams;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameterDeclarations#addConfigurationParameter(org.apache.uima.resource.metadata.ConfigurationParameter)
   */
  public void addConfigurationParameter(ConfigurationParameter aConfigurationParameter) {
    ConfigurationParameter[] current = getConfigurationParameters();
    ConfigurationParameter[] newArr = new ConfigurationParameter[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aConfigurationParameter;
    setConfigurationParameters(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameterDeclarations#removeConfigurationParameter(org.apache.uima.resource.metadata.ConfigurationParameter)
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
   * Overridden to write the <code>names</code> property as an XML attribute.
   * 
   * @see MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    StringBuffer buf = new StringBuffer();
    String[] names = getNames();
    buf.append(names[0]);
    for (int i = 1; i < names.length; i++) {
      buf.append(' ').append(names[i]);
    }
    attrs.addAttribute("", "names", "names", "", buf.toString());
    return attrs;
  }

  /**
   * Overridden to read <code>names</code> property from XML attribute.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    String names = aElement.getAttribute("names");
    if (names.length() == 0) {
      throw new InvalidXMLException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING, new Object[] {
          "names", "configurationGroup" });
    }
    // treat names as a space-separated list
    StringTokenizer tokenizer = new StringTokenizer(names, " \t");
    List<String> nameList = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      nameList.add(tokenizer.nextToken());
    }
    String[] nameArr = new String[nameList.size()];
    nameList.toArray(nameArr);
    setNames(nameArr);

    // call superclass method to read the configurationParameters property
    super.buildFromXMLElement(aElement, aParser, aOptions);
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("configurationGroup",
          new PropertyXmlInfo[] {
          // NOTE: names property is XMLized as an attribute
          new PropertyXmlInfo("configurationParameters", null), });
}
