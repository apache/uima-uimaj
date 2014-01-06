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

package org.apache.uima.collection.impl.metadata.cpe;

import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.xml.sax.helpers.AttributesImpl;

public class CasProcessorDeploymentParamImpl extends MetaDataObject_impl implements
        CasProcessorDeploymentParam {
  private static final long serialVersionUID = 8950620301535742630L;

  private String name;

  private String value;

  private String type;

  public CasProcessorDeploymentParamImpl() {
  }

  public CasProcessorDeploymentParamImpl(String aName, String aValue, String aType) {
    name = aName;
    value = aValue;
    type = aType;
  }

  public void setParameterName(String aParamName) throws CpeDescriptorException {
    name = aParamName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParam#getParameterName()
   */
  public String getParameterName() throws CpeDescriptorException {
    return name;
  }

  
  public void setParameterValue(String aParamValue) throws CpeDescriptorException {
    value = aParamValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParam#getParameterValue()
   */
  public String getParameterValue() throws CpeDescriptorException {
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParam#setParameterType(java.lang.String)
   */
  public void setParameterType(String aParamType) throws CpeDescriptorException {
    type = aParamType;
  }

  
  public String getParameterType() throws CpeDescriptorException {
    return type;
  }

  /**
   * Overridden to handle "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    try {
      attrs.addAttribute("", "name", "name", "CDATA", getParameterName());
      if (getParameterType() != null && getParameterType().trim().length() > 0) {
        attrs.addAttribute("", "type", "type", "CDATA", getParameterType());
      }
      attrs.addAttribute("", "value", "value", "CDATA", getParameterValue());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return attrs;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("parameter",
          new PropertyXmlInfo[0]);

}
