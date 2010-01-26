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
import org.apache.uima.search.IndexBuildItem;
import org.apache.uima.search.IndexBuildSpecification;

/**
 * 
 * 
 */
public class IndexBuildSpecification_impl extends MetaDataObject_impl implements
        IndexBuildSpecification {
  private static final long serialVersionUID = -5922996488248689708L;

  private IndexBuildItem[] mItems = new IndexBuildItem[0];

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildSpecification#getIndexBuildItems()
   */
  public IndexBuildItem[] getIndexBuildItems() {
    return mItems;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildSpecification#setIndexBuildItems(org.apache.uima.search.IndexBuildItem[])
   */
  public void setIndexBuildItems(IndexBuildItem[] aItems) {
    mItems = (aItems == null) ? new IndexBuildItem[0] : aItems;
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
          "indexBuildSpecification", new PropertyXmlInfo[] { new PropertyXmlInfo("indexBuildItems",
                  null), });
}
