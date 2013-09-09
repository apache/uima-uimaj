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

import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link org.apache.uima.resource.FileResourceSpecifier}.
 * 
 * 
 */
public class ConfigurableDataResourceSpecifier_impl extends MetaDataObject_impl implements
        ConfigurableDataResourceSpecifier {
  private static final long serialVersionUID = -5414343447386950507L;

  /** URL of the data. */
  private String mUrl;

  /** Resource metadata. */
  private ResourceMetaData mMetaData;

  /**
   * Creates a new <code>ConfigurableDataResourceSpecifier_impl</code>.
   */
  public ConfigurableDataResourceSpecifier_impl() {
  }

  /**
   * @see org.apache.uima.resource.ConfigurableDataResourceSpecifier#getUrl()
   */
  public String getUrl() {
    return mUrl;
  }

  /**
   * @see org.apache.uima.resource.ConfigurableDataResourceSpecifier#setUrl(String)
   */
  public void setUrl(String aUrl) {
    mUrl = aUrl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableDataResourceSpecifier#getMetaData()
   */
  public ResourceMetaData getMetaData() {
    return mMetaData;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableDataResourceSpecifier#setMetaData(org.apache.uima.resource.metadata.ResourceMetaData)
   */
  public void setMetaData(ResourceMetaData aMetaData) {
    mMetaData = aMetaData;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "configurableDataResourceSpecifier", new PropertyXmlInfo[] { new PropertyXmlInfo("url"),
              new PropertyXmlInfo("metaData", null) });
}
