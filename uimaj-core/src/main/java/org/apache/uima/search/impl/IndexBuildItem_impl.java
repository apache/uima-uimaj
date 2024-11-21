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
import org.apache.uima.search.Filter;
import org.apache.uima.search.IndexBuildItem;
import org.apache.uima.search.IndexRule;

/**
 * 
 * 
 */
public class IndexBuildItem_impl extends MetaDataObject_impl implements IndexBuildItem {
  private static final long serialVersionUID = -2034703263819608423L;

  private Filter mFilter;

  private IndexRule mRule;

  private String mName;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#getName()
   */
  @Override
  public String getName() {
    return mName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#setName(java.lang.String)
   */
  @Override
  public void setName(String aName) {
    mName = aName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#getIndexRule()
   */
  @Override
  public IndexRule getIndexRule() {
    return mRule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#setIndexRule(org.apache.uima.search.IndexRule)
   */
  @Override
  public void setIndexRule(IndexRule aRule) {
    mRule = aRule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#getFilter()
   */
  @Override
  public Filter getFilter() {
    return mFilter;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexBuildItem#setFilter(org.apache.uima.search.Filter)
   */
  @Override
  public void setFilter(Filter aFilter) {
    mFilter = aFilter;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("indexBuildItem",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", "name"),
              new PropertyXmlInfo("indexRule", null), new PropertyXmlInfo("filter", null), });
}
