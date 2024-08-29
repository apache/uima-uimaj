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
import org.apache.uima.util.impl.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reference implementation of {@link ConfigurationParameter}.
 */
public class ConfigurationParameter_impl extends MetaDataObject_impl
        implements ConfigurationParameter {

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
  private String[] mOverrides = Constants.EMPTY_STRING_ARRAY;

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public void setName(String aName) {
    mName = aName;
  }

  @Override
  public String getExternalOverrideName() {
    return mExternalOverrideName;
  }

  @Override
  public void setExternalOverrideName(String aExternalOverrideName) {
    mExternalOverrideName = aExternalOverrideName;
  }

  @Override
  public String getDescription() {
    return mDescription;
  }

  @Override
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  @Override
  public String getType() {
    return mType;
  }

  @Override
  public void setType(String aType) throws UIMA_IllegalArgumentException {
    // check to make sure value is legal
    if (!isValidDataTypeName(aType)) {
      throw new UIMA_IllegalArgumentException(
              UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH,
              new Object[] { aType, "type" });
    }
    mType = aType;
  }

  @Override
  public boolean isMultiValued() {
    return mMultiValued;
  }

  @Override
  public void setMultiValued(boolean aMultiValued) {
    mMultiValued = aMultiValued;
  }

  @Override
  public boolean isMandatory() {
    return mMandatory;
  }

  @Override
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

  @Override
  public String[] getOverrides() {
    return mOverrides.clone();
  }

  @Override
  public void setOverrides(String... aOverrides) {
    if (aOverrides == null) {
      mOverrides = Constants.EMPTY_STRING_ARRAY;
    } else {
      mOverrides = aOverrides.clone();
    }
  }

  @Override
  public void addOverride(String aOverride) {
    String[] current = getOverrides();
    String[] newArr = new String[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aOverride;
    setOverrides(newArr);
  }

  @Override
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
   * @return true if and only if an object of class <code>aClass</code> can be legally assigned to a
   *         parameter described by <code>aTypeName</code> and <code>aMultiValued</code>.
   */
  public static boolean typeMatch(Class aClass, String aTypeName, boolean aMultiValued) {
    if (aTypeName == null) {
      throw new IllegalArgumentException("Parameter type cannot be null");
    }

    if (aMultiValued) {
      // aClass must be an array
      if (!aClass.isArray()) {
        return false;
      }

      // Component Type of the array must match
      return typeMatch(aClass.getComponentType(), aTypeName, false);
    }

    // not multi-valued
    switch (aTypeName) {
      case ConfigurationParameter.TYPE_STRING:
        return aClass == String.class;
      case ConfigurationParameter.TYPE_BOOLEAN:
        return aClass == Boolean.class;
      case ConfigurationParameter.TYPE_INTEGER:
        return aClass == Integer.class;
      case ConfigurationParameter.TYPE_LONG:
        return aClass == Long.class;
      case ConfigurationParameter.TYPE_FLOAT:
        return aClass == Float.class;
      case ConfigurationParameter.TYPE_DOUBLE:
        return aClass == Double.class;
      default:
        throw new IllegalArgumentException("Unsupported parameter type [" + aTypeName + "]");
    }
  }

  /**
   * Determines whether the given String is a valid name for a data type. Valid data type names are
   * legal arguments to the {@link #setType(String)} method, and are defined by the TYPE constants
   * on the {@link ConfigurationParameter} interface.
   * 
   * @param aTypeName
   *          an Object to test
   * 
   * @return true if and only if <code>aTypeName</code> is a <code>String</code> that is a valid
   *         data type name.
   */
  protected static boolean isValidDataTypeName(Object aTypeName) {
    if (!(aTypeName instanceof String)) {
      return false;
    }

    switch ((String) aTypeName) {
      case ConfigurationParameter.TYPE_STRING: // fall-through
      case ConfigurationParameter.TYPE_BOOLEAN: // fall-through
      case ConfigurationParameter.TYPE_INTEGER: // fall-through
      case ConfigurationParameter.TYPE_LONG: // fall-through
      case ConfigurationParameter.TYPE_FLOAT: // fall-through
      case ConfigurationParameter.TYPE_DOUBLE: // fall-through
        return true;
      default:
        return false;
    }
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
  @Override
  protected void readArrayPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo,
          Class aPropClass, Element aElement, XMLParser aParser, XMLParser.ParsingOptions aOptions)
          throws InvalidXMLException {
    if ("overrides".equals(aPropXmlInfo.propertyName)) {
      // get all child nodes (note not all may be elements)
      NodeList elems = aElement.getChildNodes();
      int numChildren = elems.getLength();

      // iterate through children, and for each element construct a value,
      // adding it to a list
      List<String> valueList = new ArrayList<>();
      for (int i = 0; i < numChildren; i++) {
        Node curNode = elems.item(i);
        if (curNode instanceof Element) {
          Element curElem = (Element) curNode;

          // does the PropertyXmlInfo specify the expected tag name?
          if ("parameter".equals(curElem.getTagName()) || "param".equals(curElem.getTagName())) {
            // get text of element
            String elemText = XMLUtils.getText(curElem, aOptions.expandEnvVarRefs);
            valueList.add(elemText);
          } else {
            // element type does not match
            throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TYPE,
                    new Object[] { aPropXmlInfo.arrayElementTagName, curElem.getTagName() });
          }
        }
      }
      // set property
      String[] overridesArray = new String[valueList.size()];
      valueList.toArray(overridesArray);
      setOverrides(overridesArray);
    } else {
      super.readArrayPropertyValueFromXMLElement(aPropXmlInfo, aPropClass, aElement, aParser,
              aOptions);
    }
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("configurationParameter",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"),
              new PropertyXmlInfo("externalOverrideName"), new PropertyXmlInfo("description"),
              new PropertyXmlInfo("type"), new PropertyXmlInfo("multiValued"),
              new PropertyXmlInfo("mandatory"),
              new PropertyXmlInfo("overrides", "overrides", true, "parameter"), });
}
