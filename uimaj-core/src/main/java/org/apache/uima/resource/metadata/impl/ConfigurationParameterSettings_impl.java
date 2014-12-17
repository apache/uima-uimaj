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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.NameClassPair;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link ConfigurationParameterSettings}.
 * 
 * 
 */
public class ConfigurationParameterSettings_impl extends MetaDataObject_impl implements
        ConfigurationParameterSettings {

  static final long serialVersionUID = 3476535733588304983L;

  private static final Method methodGetSettingsForGroups;
  static {
    try {
      methodGetSettingsForGroups = ConfigurationParameterSettings.class.getDeclaredMethod("getSettingsForGroups");
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }
  /**
   * Settings for parameters that are not in any group.
   */
  private NameValuePair[] mParameterSettings = new NameValuePair[0];

  /**
   * Settings for parameters in groups. This HashMap has <code>String</code> keys (the group name)
   * and <code>NameValuePair[]</code> values (the parameter names and their values).
   */
  private Map<String, NameValuePair[]> mSettingsForGroups = new HashMap<String, NameValuePair[]>();

  /**
   * @see ConfigurationParameterSettings#getParameterSettings()
   */
  public NameValuePair[] getParameterSettings() {
    return mParameterSettings;
  }

  /**
   * @see ConfigurationParameterSettings#setParameterSettings(NameValuePair[])
   */
  public void setParameterSettings(NameValuePair[] aSettings) {
    if (aSettings == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aSettings", "setParameterSettings" });
    }
    mParameterSettings = aSettings;
  }

  /**
   * @see ConfigurationParameterSettings#getSettingsForGroups()
   */
  public Map<String, NameValuePair[]> getSettingsForGroups() {
    return mSettingsForGroups;
  }

  /**
   * @see ConfigurationParameterSettings#getParameterValue(String)
   */
  public Object getParameterValue(String aParamName) {
    NameValuePair[] nvps = getParameterSettings();
    if (nvps != null) {
      for (int i = 0; i < nvps.length; i++) {
        if (aParamName.equals(nvps[i].getName())) {
          return nvps[i].getValue();
        }
      }
    }
    return null;
  }

  /**
   * @see ConfigurationParameterSettings#getParameterValue(java.lang.String,
   *      java.lang.String)
   */
  public Object getParameterValue(String aGroupName, String aParamName) {
    if (aGroupName == null) {
      return getParameterValue(aParamName);
    } else {
      NameValuePair[] nvps = mSettingsForGroups.get(aGroupName);
      if (nvps != null) {
        for (int i = 0; i < nvps.length; i++) {
          if (aParamName.equals(nvps[i].getName())) {
            return nvps[i].getValue();
          }
        }
      }
      return null;
    }
  }

  /**
   * @see ConfigurationParameterSettings#setParameterValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setParameterValue(String aParamName, Object aValue) {
    if (aValue != null) // setting a value
    {
      NameValuePair[] nvps = getParameterSettings();
      if (nvps != null) {
        for (int i = 0; i < nvps.length; i++) {
          if (aParamName.equals(nvps[i].getName())) {
            nvps[i].setValue(aValue);
            return;
          }
        }

        // param not found - add new NameValuePair
        NameValuePair newNVP = new NameValuePair_impl(aParamName, aValue);
        NameValuePair[] newArr = new NameValuePair[nvps.length + 1];
        System.arraycopy(nvps, 0, newArr, 0, nvps.length);
        newArr[newArr.length - 1] = newNVP;
        setParameterSettings(newArr);
      } else {
        setParameterSettings(new NameValuePair[] { new NameValuePair_impl(aParamName, aValue) });
      }
    } else // clearing a value
    {
      NameValuePair[] nvps = getParameterSettings();
      if (nvps != null) {
        for (int i = 0; i < nvps.length; i++) {
          if (aParamName.equals(nvps[i].getName())) {
            NameValuePair[] newArr = new NameValuePair[nvps.length - 1];
            System.arraycopy(nvps, 0, newArr, 0, i);
            System.arraycopy(nvps, i + 1, newArr, i, nvps.length - i - 1);
            setParameterSettings(newArr);
            break;
          }
        }

      }
    }
  }

  /**
   * @see ConfigurationParameterSettings#setParameterValue(java.lang.String,
   *      java.lang.String, java.lang.Object)
   */
  public void setParameterValue(String aGroupName, String aParamName, Object aValue) {
    if (aGroupName == null) {
      setParameterValue(aParamName, aValue);
    } else {
      if (aValue != null) // setting a value
      {
        NameValuePair[] nvps = mSettingsForGroups.get(aGroupName);
        if (nvps == null) // create new group
        {
          NameValuePair newNVP = new NameValuePair_impl(aParamName, aValue);
          mSettingsForGroups.put(aGroupName, new NameValuePair[] { newNVP });
        } else {
          for (int i = 0; i < nvps.length; i++) {
            if (aParamName.equals(nvps[i].getName())) {
              nvps[i].setValue(aValue);
              return;
            }
          }
          // param not found - add new NameValuePair to group
          NameValuePair newNVP = new NameValuePair_impl(aParamName, aValue);
          NameValuePair[] newArr = new NameValuePair[nvps.length + 1];
          System.arraycopy(nvps, 0, newArr, 0, nvps.length);
          newArr[newArr.length - 1] = newNVP;
          mSettingsForGroups.put(aGroupName, newArr);
        }
      } else // clearing a value
      {
        NameValuePair[] nvps = mSettingsForGroups.get(aGroupName);
        if (nvps != null) {
          for (int i = 0; i < nvps.length; i++) {
            if (aParamName.equals(nvps[i].getName())) {
              NameValuePair[] newArr = new NameValuePair[nvps.length - 1];
              System.arraycopy(nvps, 0, newArr, 0, i);
              System.arraycopy(nvps, i + 1, newArr, i, nvps.length - i - 1);
              mSettingsForGroups.put(aGroupName, newArr);
              break;
            }
          }
        }
      }
    }
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  @Override
  public List<MetaDataAttr> getAdditionalAttributes() {
    return Collections.singletonList(
        new MetaDataAttr(
            "settingsForGroups",
            methodGetSettingsForGroups,
            null,  // no writer
            Map.class));
  }
  
  /**
   * Overridden to add the settingsForGroups property to the result list. Default introspection
   * implementation won't return it because it has no set method. We've also overridden the XML
   * import/export methods, though, so that set methods are not required.
   * 
   * @see MetaDataObject#listAttributes()
   * @deprecated - use getAdditionalAttributes instead
   */
  @Deprecated
  public List<NameClassPair> listAttributes() {
    List<NameClassPair> result = super.listAttributes();
    result.add(new NameClassPair("settingsForGroups", Map.class.getName()));
    return result;
  }

  /**
   * Overridden becuase of settingsForGroups property, which is a Map and isn't handled by default
   * XMLization routines.
   * 
   * @see XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    List<XMLizable> nvps = new ArrayList<XMLizable>();
    // get all child nodes
    NodeList childNodes = aElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node curNode = childNodes.item(i);
      if (curNode instanceof Element) {
        Element elem = (Element) curNode;
        // check element tag name
        if ("nameValuePair".equals(elem.getTagName())) {
          nvps.add(aParser.buildObject(elem, aOptions));
        } else if ("settingsForGroup".equals(elem.getTagName())) {
          String key = elem.getAttribute("name");

          List<XMLizable> vals = new ArrayList<XMLizable>();
          NodeList arrayNodes = elem.getChildNodes();
          for (int j = 0; j < arrayNodes.getLength(); j++) {
            Node curArrayNode = arrayNodes.item(j);
            if (curArrayNode instanceof Element) {
              Element valElem = (Element) curArrayNode;
              vals.add(aParser.buildObject(valElem));
            }
          }
          if (!vals.isEmpty()) {
            NameValuePair[] valArr = new NameValuePair[vals.size()];
            vals.toArray(valArr);
            mSettingsForGroups.put(key, valArr);
          }
        } else {
          throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT, new Object[] { elem
                  .getTagName() });
        }
      }
    }
    NameValuePair[] nvpArr = new NameValuePair[nvps.size()];
    nvps.toArray(nvpArr);
    setParameterSettings(nvpArr);
  }

  /**
   * Overridden to write the settingsForGroups property, whose value is a Map, which is not
   * supported by the default XMLization routines.
   * 
   * @see MetaDataObject_impl#writePropertyAsElement(PropertyXmlInfo, String)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    if ("settingsForGroups".equals(aPropInfo.propertyName)) {
      this.writeMapPropertyToXml("settingsForGroups", null, "name", "settingsForGroup", true,
              aNamespace);
    } else {
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "configurationParameterSettings", new PropertyXmlInfo[] {
              new PropertyXmlInfo("parameterSettings", null),
              new PropertyXmlInfo("settingsForGroups", null) // NOTE: custom XMLization
          });
}
