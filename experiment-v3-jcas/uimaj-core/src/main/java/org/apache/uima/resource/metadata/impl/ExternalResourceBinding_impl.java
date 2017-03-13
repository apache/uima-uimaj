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

import org.apache.uima.resource.metadata.ExternalResourceBinding;

/**
 * 
 * 
 */
public class ExternalResourceBinding_impl extends MetaDataObject_impl implements
        ExternalResourceBinding {

  
  private static final long serialVersionUID = 8736222753308388218L;

  private String mResourceName;

  private String mKey;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ExternalResourceBinding#getKey()
   */
  public String getKey() {
    return mKey;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ExternalResourceBinding#setKey(java.lang.String)
   */
  public void setKey(String aKey) {
    mKey = aKey;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ExternalResourceBinding#getResourceName()
   */
  public String getResourceName() {
    return mResourceName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ExternalResourceBinding#setResourceName(java.lang.String)
   */
  public void setResourceName(String aName) {
    mResourceName = aName;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "externalResourceBinding", new PropertyXmlInfo[] { new PropertyXmlInfo("key"),
              new PropertyXmlInfo("resourceName"), });
}
