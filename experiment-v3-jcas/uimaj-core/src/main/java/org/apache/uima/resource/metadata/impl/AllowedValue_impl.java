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

import org.apache.uima.resource.metadata.AllowedValue;

/**
 * Reference implementation of {@link AllowedValue}.
 * 
 * 
 */
public class AllowedValue_impl extends MetaDataObject_impl implements AllowedValue {

  static final long serialVersionUID = -3463916068572525348L;

  /** The allowed value string. */
  private String mString;

  /** Verbose description of this value. */
  private String mDescription;

  /**
   * Default constructor.
   */
  public AllowedValue_impl() {
  }

  /**
   * Constructor.
   * 
   * @param aString
   *          the allowed value string
   * @param aDescription
   *          verbose description of this allowed value
   */
  public AllowedValue_impl(String aString, String aDescription) {
    setString(aString);
    setDescription(aDescription);
  }

  /**
   * @see org.apache.uima.resource.metadata.AllowedValue#getString()
   */
  public String getString() {
    return mString;
  }

  /**
   * @see org.apache.uima.resource.metadata.AllowedValue#setString(java.lang.String)
   */
  public void setString(String aString) {
    mString = aString;
  }

  /**
   * @see org.apache.uima.resource.metadata.AllowedValue#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.metadata.AllowedValue#setDescription(java.lang.String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("value",
          new PropertyXmlInfo[] { new PropertyXmlInfo("string"),
              new PropertyXmlInfo("description", false), });
}
