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

import org.apache.uima.resource.metadata.OperationalProperties;

public class OperationalProperties_impl extends MetaDataObject_impl implements
        OperationalProperties {

  private static final long serialVersionUID = 8649608701550531026L;

  private boolean mModifiesCas;

  private boolean mMultipleDeploymentAllowed;

  private boolean mOutputsNewCASes;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.OperationalProperties#getModifiesCas()
   */
  public boolean getModifiesCas() {
    return mModifiesCas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.OperationalProperties#isMultipleDeploymentAllowed()
   */
  public boolean isMultipleDeploymentAllowed() {
    return mMultipleDeploymentAllowed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.OperationalProperties#setModifiesCas(boolean)
   */
  public void setModifiesCas(boolean aModifiesCas) {
    mModifiesCas = aModifiesCas;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.OperationalProperties#setMultipleDeploymentAllowed(boolean)
   */
  public void setMultipleDeploymentAllowed(boolean aMultipleDeploymentAllowed) {
    mMultipleDeploymentAllowed = aMultipleDeploymentAllowed;
  }

  public boolean getOutputsNewCASes() {
    return mOutputsNewCASes;
  }

  public void setOutputsNewCASes(boolean aOutputsNewCASes) {
    mOutputsNewCASes = aOutputsNewCASes;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("operationalProperties",
          new PropertyXmlInfo[] { new PropertyXmlInfo("modifiesCas"),
              new PropertyXmlInfo("multipleDeploymentAllowed"),
              new PropertyXmlInfo("outputsNewCASes") });
}
