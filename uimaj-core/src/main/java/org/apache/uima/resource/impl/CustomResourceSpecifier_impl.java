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

import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Implementation of {@link CustomResourceSpecifier}.
 */
public class CustomResourceSpecifier_impl extends MetaDataObject_impl implements
        CustomResourceSpecifier {
  private static final long serialVersionUID = 8922306013278525153L;
  
  private static final Parameter[] EMPTY_PARAMETERS = new Parameter[0];
  private Parameter[] mParameters = EMPTY_PARAMETERS;
  private String mResourceClassName;
  
  /* (non-Javadoc)
   * @see org.apache.uima.resource.CustomResourceSpecifier#getParameters()
   */
  public Parameter[] getParameters() {
    return mParameters;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.CustomResourceSpecifier#getResourceClassName()
   */
  public String getResourceClassName() {
    return mResourceClassName;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.CustomResourceSpecifier#setParameters(org.apache.uima.resource.Parameter[])
   */
  public void setParameters(Parameter[] aParameters) {
    if (aParameters != null) {
      mParameters = aParameters;
    } else {
      mParameters = EMPTY_PARAMETERS;
    }
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.CustomResourceSpecifier#setResourceClassName(java.lang.String)
   */
  public void setResourceClassName(String aResourceClassName) {
    mResourceClassName = aResourceClassName;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("customResourceSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("resourceClassName"),
              new PropertyXmlInfo("parameters")});
}
