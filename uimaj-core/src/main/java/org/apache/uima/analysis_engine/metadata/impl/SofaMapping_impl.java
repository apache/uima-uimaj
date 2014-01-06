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

package org.apache.uima.analysis_engine.metadata.impl;

import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Implementation of {@link SofaMapping}.
 */
public class SofaMapping_impl extends MetaDataObject_impl implements SofaMapping {
  
  private static final long serialVersionUID = -6115544748030506703L;

  private String mComponentKey = CAS.NAME_DEFAULT_TEXT_SOFA;

  private String mComponentSofaName;

  private String mAggregateSofaName;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#getComponentKey()
   */
  public String getComponentKey() {
    return mComponentKey;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#setComponentKey(java.lang.String)
   */
  public void setComponentKey(String aComponentKey) {
    mComponentKey = aComponentKey;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#getComponentSofaName()
   */
  public String getComponentSofaName() {
    return mComponentSofaName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#setComponentSofaName(java.lang.String)
   */
  public void setComponentSofaName(String aComponentSofaName) {
    mComponentSofaName = aComponentSofaName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#getAggregateSofaName()
   */
  public String getAggregateSofaName() {
    return mAggregateSofaName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.metadata.SofaMapping#setAggregateSofaName(java.lang.String)
   */
  public void setAggregateSofaName(String aAggregateSofaName) {
    mAggregateSofaName = aAggregateSofaName;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("sofaMapping",
          new PropertyXmlInfo[] { new PropertyXmlInfo("componentKey"),
              new PropertyXmlInfo("componentSofaName"), new PropertyXmlInfo("aggregateSofaName") });
}
