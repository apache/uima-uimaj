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

import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link org.apache.uima.resource.PearSpecifier}.
 * 
 * 
 */
public class PearSpecifier_impl extends MetaDataObject_impl implements PearSpecifier {

  static final long serialVersionUID = -7910540167197537337L;

  /** PEAR path setting */
  private String mPearPath;

  private Parameter[] mParameters;

  /**
   * Creates a new <code>PearSpecifier_impl</code>.
   */
  public PearSpecifier_impl() {
  }

  /**
   * @return Returns the Parameters.
   */
  public Parameter[] getParameters() {
    return this.mParameters;
  }

  /**
   * @param parameters
   *          The Parameters to set.
   */
  public void setParameters(Parameter[] parameters) {
    this.mParameters = parameters;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.PearSpecifier#getPearPath()
   */
  public String getPearPath() {
    return this.mPearPath;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.PearSpecifier#setPearPath(java.lang.String)
   */
  public void setPearPath(String aPearPath) {
    this.mPearPath = aPearPath;    
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("pearSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("pearPath"), new PropertyXmlInfo("parameters") });
}
