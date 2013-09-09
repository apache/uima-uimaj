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

import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeTimer;
import org.apache.uima.collection.metadata.OutputQueue;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

public class CpeConfigurationImpl extends MetaDataObject_impl implements CpeConfiguration {
  private static final long serialVersionUID = 1153815602567127240L;

  private String startAt;

  private long num2Process;

  private CpeCheckpoint checkpoint;

  private String timerImpl;

  private String deployAs;

  private OutputQueue outputQueue;

  public CpeConfigurationImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setDeployment(java.lang.String)
   */
  public void setDeployment(String aDeploy) throws CpeDescriptorException {
    deployAs = aDeploy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getDeployment()
   */
  public String getDeployment() {
    return deployAs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setNumToProcess(int)
   */
  public void setNumToProcess(int aNumToProcess) throws CpeDescriptorException {
    num2Process = aNumToProcess;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getNumToProcess()
   */
  public int getNumToProcess() {
    return (int) num2Process;
  }

  public void setStartingEntityId(String aStartAt) {
    startAt = aStartAt;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getStartingEntityId()
   */
  public String getStartingEntityId() {
    return startAt;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setCheckpoint(org.apache.uima.collection.metadata.CpeCheckpoint)
   */
  public void setCheckpoint(CpeCheckpoint aCheckpoint) throws CpeDescriptorException {
    checkpoint = aCheckpoint;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getCheckpoint()
   */
  public CpeCheckpoint getCheckpoint() {
    return checkpoint;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#removeCheckpoint()
   */
  public void removeCheckpoint() {
    checkpoint = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setCpeTimer(org.apache.uima.collection.metadata.CpeTimer)
   */
  public void setCpeTimer(CpeTimer aTimer) {
    timerImpl = aTimer.get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getCpeTimer()
   */
  public CpeTimer getCpeTimer() {
    return new CpeTimerImpl(timerImpl);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#removeCpeTimer()
   */
  public void removeCpeTimer() {
    timerImpl = "";
  }

  public String getDeployAs() {
    return deployAs;
  }

  public long getNum2Process() {
    return num2Process;
  }

  public String getStartAt() {
    return startAt;
  }

  public String getTimerImpl() {
    return timerImpl;
  }

  /**
   * @param string
   */
  public void setDeployAs(String string) {
    deployAs = string;
  }

  /**
   * @param l
   */
  public void setNumToProcess(long l) {
    num2Process = l;
  }

  /**
   * @param aStartAt
   */
  public void setStartAt(String aStartAt) {

    startAt = aStartAt;
  }

  /**
   * @param string
   */
  public void setTimerImpl(String string) {
    timerImpl = string;
  }

  public OutputQueue getOutputQueue() {
    return outputQueue;
  }

  public int getMaxTimeToWait() {
    if (getOutputQueue() == null) {
      return 0;
    }
    return getOutputQueue().getDequeueTimeout();
  }

  /**
   * @param queue
   */
  public void setOutputQueue(OutputQueue queue) {
    outputQueue = queue;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("cpeConfig",
          new PropertyXmlInfo[] { new PropertyXmlInfo("numToProcess"),
              new PropertyXmlInfo("deployAs"), new PropertyXmlInfo("checkpoint", null),
              new PropertyXmlInfo("timerImpl"), new PropertyXmlInfo("outputQueue", null),
              new PropertyXmlInfo("startAt"), });

}
