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

import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link ExternalResourceDependency}.
 * 
 * 
 */
public class ExternalResourceDependency_impl extends MetaDataObject_impl implements
        ExternalResourceDependency {

  static final long serialVersionUID = 8416783152191685356L;

  private String mKey;

  private String mDescription;

  private String mInterfaceName;

  private boolean mOptional;

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#getKey()
   */
  public String getKey() {
    return mKey;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#getInterfaceName()
   */
  public String getInterfaceName() {
    return mInterfaceName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#setKey(String)
   */
  public void setKey(String aKey) {
    mKey = aKey;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#setInterfaceName(String)
   */
  public void setInterfaceName(String aName) {
    mInterfaceName = aName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#isOptional()
   */
  public boolean isOptional() {
    return mOptional;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#setDescription(java.lang.String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;

  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDependency#setOptional(boolean)
   */
  public void setOptional(boolean aOptional) {
    mOptional = aOptional;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "externalResourceDependency", new PropertyXmlInfo[] { new PropertyXmlInfo("key"),
              new PropertyXmlInfo("description", false), new PropertyXmlInfo("interfaceName"),
              new PropertyXmlInfo("optional"), });
}
