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

import org.apache.uima.collection.metadata.CasProcessorExecutable;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * The Class CasProcessorRunInSeperateProcessImpl.
 */
public class CasProcessorRunInSeperateProcessImpl extends MetaDataObject_impl
        implements CasProcessorRunInSeperateProcess {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1074137401279020375L;

  /** The exec. */
  private CasProcessorExecutable exec;

  /**
   * Instantiates a new cas processor run in seperate process impl.
   */
  public CasProcessorRunInSeperateProcessImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess#setExecutable(org.apache.
   * uima.collection.metadata.CasProcessorExecutable)
   */
  @Override
  public void setExecutable(CasProcessorExecutable aExec) {
    exec = aExec;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess#getExecutable()
   */
  @Override
  public CasProcessorExecutable getExecutable() {
    return exec;
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

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("runInSeparateProcess",
          new PropertyXmlInfo[] { new PropertyXmlInfo("exec", null), });

  /**
   * Gets the exec.
   *
   * @return the executable
   */
  public CasProcessorExecutable getExec() {
    return exec;
  }

  /**
   * Sets the exec.
   *
   * @param executable
   *          the new exec
   */
  public void setExec(CasProcessorExecutable executable) {
    exec = executable;
  }

}
