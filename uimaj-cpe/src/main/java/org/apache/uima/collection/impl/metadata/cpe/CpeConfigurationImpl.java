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


/**
 * The Class CpeConfigurationImpl.
 */
public class CpeConfigurationImpl extends MetaDataObject_impl implements CpeConfiguration {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1153815602567127240L;

  /** The start at. */
  private String startAt;

  /** The num 2 process. */
  private long num2Process;

  /** The checkpoint. */
  private CpeCheckpoint checkpoint;

  /** The timer impl. */
  private String timerImpl;

  /** The deploy as. */
  private String deployAs;

  /** The output queue. */
  private OutputQueue outputQueue;

  /**
   * Instantiates a new cpe configuration impl.
   */
  public CpeConfigurationImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setDeployment(java.lang.String)
   */
  @Override
  public void setDeployment(String aDeploy) throws CpeDescriptorException {
    deployAs = aDeploy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getDeployment()
   */
  @Override
  public String getDeployment() {
    return deployAs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setNumToProcess(int)
   */
  @Override
  public void setNumToProcess(int aNumToProcess) throws CpeDescriptorException {
    num2Process = aNumToProcess;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getNumToProcess()
   */
  @Override
  public int getNumToProcess() {
    return (int) num2Process;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setStartingEntityId(java.lang.String)
   */
  @Override
  public void setStartingEntityId(String aStartAt) {
    startAt = aStartAt;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getStartingEntityId()
   */
  @Override
  public String getStartingEntityId() {
    return startAt;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setCheckpoint(org.apache.uima.collection.metadata.CpeCheckpoint)
   */
  @Override
  public void setCheckpoint(CpeCheckpoint aCheckpoint) throws CpeDescriptorException {
    checkpoint = aCheckpoint;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getCheckpoint()
   */
  @Override
  public CpeCheckpoint getCheckpoint() {
    return checkpoint;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#removeCheckpoint()
   */
  @Override
  public void removeCheckpoint() {
    checkpoint = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#setCpeTimer(org.apache.uima.collection.metadata.CpeTimer)
   */
  @Override
  public void setCpeTimer(CpeTimer aTimer) {
    timerImpl = aTimer.get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getCpeTimer()
   */
  @Override
  public CpeTimer getCpeTimer() {
    return new CpeTimerImpl(timerImpl);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeConfiguration#removeCpeTimer()
   */
  @Override
  public void removeCpeTimer() {
    timerImpl = "";
  }

  /**
   * Gets the deploy as.
   *
   * @return the deploy as
   */
  public String getDeployAs() {
    return deployAs;
  }

  /**
   * Gets the num 2 process.
   *
   * @return the num 2 process
   */
  public long getNum2Process() {
    return num2Process;
  }

  /**
   * Gets the start at.
   *
   * @return the start at
   */
  public String getStartAt() {
    return startAt;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getTimerImpl()
   */
  @Override
  public String getTimerImpl() {
    return timerImpl;
  }

  /**
   * Sets the deploy as.
   *
   * @param string the new deploy as
   */
  public void setDeployAs(String string) {
    deployAs = string;
  }

  /**
   * Sets the num to process.
   *
   * @param l the new num to process
   */
  public void setNumToProcess(long l) {
    num2Process = l;
  }

  /**
   * Sets the start at.
   *
   * @param aStartAt the new start at
   */
  public void setStartAt(String aStartAt) {

    startAt = aStartAt;
  }

  /**
   * Sets the timer impl.
   *
   * @param string the new timer impl
   */
  public void setTimerImpl(String string) {
    timerImpl = string;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getOutputQueue()
   */
  @Override
  public OutputQueue getOutputQueue() {
    return outputQueue;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeConfiguration#getMaxTimeToWait()
   */
  @Override
  public int getMaxTimeToWait() {
    if (getOutputQueue() == null) {
      return 0;
    }
    return getOutputQueue().getDequeueTimeout();
  }

  /**
   * Sets the output queue.
   *
   * @param queue the new output queue
   */
  public void setOutputQueue(OutputQueue queue) {
    outputQueue = queue;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("cpeConfig",
          new PropertyXmlInfo[] { new PropertyXmlInfo("numToProcess"),
              new PropertyXmlInfo("deployAs"), new PropertyXmlInfo("checkpoint", null),
              new PropertyXmlInfo("timerImpl"), new PropertyXmlInfo("outputQueue", null),
              new PropertyXmlInfo("startAt"), });

}
