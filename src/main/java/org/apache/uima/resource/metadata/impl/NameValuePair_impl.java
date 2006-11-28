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

import org.apache.uima.resource.metadata.NameValuePair;

/**
 * Reference implementation of {@link NameValuePair}.
 * 
 * 
 */
public class NameValuePair_impl extends MetaDataObject_impl implements NameValuePair {

  static final long serialVersionUID = -1806648654924417387L;

  /** Name */
  private String mName;

  /** Value */
  private Object mValue;

  /**
   * Creates a new <code>NameValuePair_impl</code> with a null name and value.
   */
  public NameValuePair_impl() {
  }

  /**
   * Creates a new <code>NameValuePair_impl</code> with the specified name and value.
   * 
   * @param aName
   *          a name
   * @param aValue
   *          a value
   */
  public NameValuePair_impl(String aName, Object aValue) {
    setName(aName);
    setValue(aValue);
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName() {
    return mName;
  }

  /**
   * Sets the name.
   * 
   * @param aName
   *          a name
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * Gets the value.
   * 
   * @return the value
   */
  public Object getValue() {
    return mValue;
  }

  /**
   * Sets the value.
   * 
   * @param aValue
   *          a value
   */
  public void setValue(Object aValue) {
    mValue = aValue;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "nameValuePair",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"), new PropertyXmlInfo("value", false), });
}
