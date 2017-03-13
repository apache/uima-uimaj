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
import org.apache.uima.search.IndexRule;
import org.apache.uima.search.Style;

/**
 * 
 * 
 */
public class IndexRule_impl extends MetaDataObject_impl implements IndexRule {
  private static final long serialVersionUID = 8072560372466068952L;

  private Style[] mStyles = new Style[0];

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexRule#getStyles()
   */
  public Style[] getStyles() {
    return mStyles;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.IndexRule#setStyles(org.apache.uima.search.Style[])
   */
  public void setStyles(Style[] aStyles) {
    mStyles = (aStyles == null) ? new Style[0] : aStyles;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("indexRule",
          new PropertyXmlInfo[] { new PropertyXmlInfo("styles", null), });
}
