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

package org.apache.uima.search.impl;

import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.search.Mapping;


public class Mapping_impl extends MetaDataObject_impl implements Mapping {

  private static final long serialVersionUID = -2371976614485187381L;

  private String mFeature;

  private String mIndexName;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Mapping#getFeature()
   */
  public String getFeature() {
    return mFeature;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Mapping#setFeature(java.lang.String)
   */
  public void setFeature(String aFeature) {
    mFeature = aFeature;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Mapping#getIndexName()
   */
  public String getIndexName() {
    return mIndexName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Mapping#setIndexName(java.lang.String)
   */
  public void setIndexName(String aIndexName) {
    mIndexName = aIndexName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "mapping",
          new PropertyXmlInfo[] { new PropertyXmlInfo("feature"), new PropertyXmlInfo("indexName") });
}
