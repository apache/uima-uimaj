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
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reference implementation of {@link ConfigurationParameter}.
 * 
 * 
 */

public class ConfigurationParameter_impl extends MetaDataObject_impl implements
        ConfigurationParameter {

  static final long serialVersionUID = 4234432343384779535L;

  /** Name of this Configuration Parameter. */
  private String mName;

  /** External name of this Configuration Parameter. */
  private String mExternalOverrideName;

  /** Description of this Configuration Parameter. */
  private String mDescription;

  /** Data Type of this Configuration Parameter. */
  private String mType;

  /** Whether this Configuration Parameter is multi-valued. */
  private boolean mMultiValued;

  /** Whether this Configuration Parameter is mandatory. */
  private boolean mMandatory;

  /**
   * The parameters that this Configuration Parameter overrides. Elements of this array are of the
   * form componentName/parameterName.
   */
  private String[] mOverrides = new String[0];

  /**
   * @see ConfigurationParameter#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see ConfigurationParameter#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see ConfigurationParameter#getExternalOverrideName()
   */
  public String getExternalOverrideName() {
    return mExternalOverrideName;
  }

  /**
   * @see ConfigurationParameter#setExternalOverrideName(String)
   */
  public void setExternalOverrideName(String aExternalOverrideName) {
    mExternalOverrideName = aExternalOverrideName;
  }

  /**
   * @see ConfigurationParameter#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see ConfigurationParameter#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see ConfigurationParameter#getType()
   */
  public String getType() {
    return mType;
  }

  /**
   * @see ConfigurationParameter#setType(String)
   */
  public void setType(String aType) throws UIMA_IllegalArgumentException {
    // check to make sure value is legal
    if (!isValidDataTypeName(aType)) {
      throw new UIMA_IllegalArgumentException(
              UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH, new Object[] { aType,
                  "type" });
    }
    mType = aType;
  }

  /**
   * @see ConfigurationParameter#isMultiValued()
   */
  public boolean isMultiValued() {
    return mMultiValued;
  }

  /**
   * @see ConfigurationParameter#setMultiValued(boolean)
   */
  public void setMultiValued(boolean aMultiValued) {
    mMultiValued = aMultiValued;
  }

  /**
   * @see ConfigurationParameter#isMandatory()
   */
  public boolean isMandatory() {
    return mMandatory;
  }

  /**
   * @see ConfigurationParameter#setMandatory(boolean)
   */
  public void setMandatory(boolean aMandatory) {
    mMandatory = aMandatory;
  }

  /*
   * @see ConfigurationParameter#isPublished()
   */
  /*
   * public boolean isPublished() { return mPublished; }
   */
  /*
   * @see ConfigurationParameter#setPublished(boolean)
   */
  /*
   * public void setPublished(boolean aPublished) { mPublished = aPublished; }
   */

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameter#getOverrides()
   */
  public String[] getOverrides() {
    return mOverrides.clone();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameter#setOverrides(java.lang.String[])
   */
  public void setOverrides(String[] aOverrides) {
    if (aOverrides == null)
      mOverrides = new String[0];
    else
      mOverrides = aOverrides.clone();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameter#addOverride(java.lang.String)
   */
  public void addOverride(String aOverride) {
    String[] current = getOverrides();
    String[] newArr = new String[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aOverride;
    setOverrides(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ConfigurationParameter#removeOverride(java.lang.String)
   */
  public void removeOverride(String aOverride) {
    String[] current = getOverrides();
    for (int i = 0; i < current.length; i++) {
      if (current[i].equals(aOverride)) {
        String[] newArr = new String[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setOverrides(newArr);
        break;
      }
    }
  }

  /**
   * Determines whether the given Java class is an appropriate value for a parameter with the
   * specified type. For example, if the parameter's type is "Integer", then java.lang.Integer is a
   * match but java.lang.String is not.
   * 
   * @param aClass
   *          class to check
   * @param aTypeName
   *          configuration parameter type, as defined by one of the TYPE constants on the
   *          {@link ConfigurationParameter} interface.
   * @param aMultiValued
   *          true if and only if the configuration parameter is multi-valued. If true,
   *          <code>aClass</code> is expected to be an array.
   * 
   * @return true if and only if an object of class <code>aClass</code> can be legally assigned to
   *         a parameter described by <code>aTypeName</code> and <code>aMultiValued</code>.
   */
  public static boolean typeMatch(Class aClass, String aTypeName, boolean aMultiValued) {
    if (aMultiValued) {
      // aClass must be an array
      if (!aClass.isArray())
        return false;

      // Component Type of the array must match
      return typeMatch(aClass.getComponentType(), aTypeName, false);
    } else // not multi-valued
    {
      if (aTypeName.equals(TYPE_STRING))
        return aClass == String.class;
      else if (aTypeName.equals(TYPE_BOOLEAN))
        return aClass == Boolean.class;
      else if (aTypeName.equals(TYPE_INTEGER))
        return aClass == Integer.class;
      else if (aTypeName.equals(TYPE_FLOAT))
        return aClass == Float.class;
      else
        return false;
    }
  }

  /**
   * Determines whether the given String is a valid name for a data type. Valid data type names are
   * legal arguments to the {@link #setType(String)} method, and are defined by the TYPE constants on
   * the {@link ConfigurationParameter} interface.
   * 
   * @param aTypeName
   *          an Object to test
   * 
   * @return true if and only if <code>aTypeName</code> is a <code>String</code> that is a valid
   *         data type name.
   */
  protected static boolean isValidDataTypeName(Object aTypeName) {
    return TYPE_STRING.equals(aTypeName) || TYPE_BOOLEAN.equals(aTypeName)
            || TYPE_INTEGER.equals(aTypeName) || TYPE_FLOAT.equals(aTypeName);
  }

  /**
   * Overriden to allow both "param" and "parameter" as the array element tags. (For historical
   * reasons.)
   * 
   * @param aPropXmlInfo
   *          information about the property to read
   * @param aPropClass
   *          class of the property's value
   * @param aElement
   *          DOM element representing the entire array
   * @param aParser
   *          parser to use to construct complex values
   * @param aOptions
   *          option settings
   */
  protected void readArrayPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo,
          Class aPropClass, Element aElement, XMLParser aParser, XMLParser.ParsingOptions aOptions)
          throws InvalidXMLException {
    if ("overrides".equals(aPropXmlInfo.propertyName)) {
      // get all child nodes (note not all may be elements)
      NodeList elems = aElement.getChildNodes();
      int numChildren = elems.getLength();

      // iterate through children, and for each element construct a value,
      // adding it to a list
      List<String> valueList = new ArrayList<String>();
      for (int i = 0; i < numChildren; i++) {
        Node curNode = elems.item(i);
        if (curNode instanceof Element) {
          Element curElem = (Element) curNode;

          // does the PropertyXmlInfo specify the expected tag name?
          if ("parameter".equals(curElem.getTagName()) || "param".equals(curElem.getTagName())) {
            // get text of element
            String elemText = XMLUtils.getText(curElem, aOptions.expandEnvVarRefs);
            valueList.add(elemText);
          } else
            // element type does not match
            throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TYPE, new Object[] {
                aPropXmlInfo.arrayElementTagName, curElem.getTagName() });
        }
      }
      // set property
      String[] overridesArray = new String[valueList.size()];
      valueList.toArray(overridesArray);
      this.setOverrides(overridesArray);
    } else {
      super.readArrayPropertyValueFromXMLElement(aPropXmlInfo, aPropClass, aElement, aParser,
              aOptions);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "configurationParameter", new PropertyXmlInfo[] { new PropertyXmlInfo("name"),
              new PropertyXmlInfo("externalOverrideName"),
              new PropertyXmlInfo("description"), new PropertyXmlInfo("type"),
              new PropertyXmlInfo("multiValued"), new PropertyXmlInfo("mandatory"),
              new PropertyXmlInfo("overrides", "overrides", true, "parameter"), });
}
