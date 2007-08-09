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

package org.apache.uima.collection.impl.cpm;

import java.io.Serializable;

import org.apache.uima.collection.base_cpm.SynchPoint;
import org.apache.uima.util.ProcessTrace;

/**
 * Serializable containing the checkpoint. The checkpoint contains both {@link ProcessTrace} and
 * {@link SynchPoint} objects. It is serialized to file system by {@link Checkpoint} thread at
 * predefined intervals.
 * 
 * 
 */
public class CheckpointData implements Serializable {
  private static final long serialVersionUID = -3261502844386898304L;

  private ProcessTrace processTrace;

  private SynchPoint synchPoint;

  public CheckpointData() {
  }

  public CheckpointData(ProcessTrace aProcessTrace) {
    processTrace = aProcessTrace;
  }

  /**
   * Initialize instance with ProcessTrace and SynchPoint
   * 
   * @param aProcessTrace -
   *          events and timers accumulated so far
   * @param aSynchPoint -
   */
  public CheckpointData(ProcessTrace aProcessTrace, SynchPoint aSynchPoint) {
    processTrace = aProcessTrace;
    synchPoint = aSynchPoint;
  }

  /**
   * Returns current ProcessTrace object
   * 
   * @return - ProcessTrace object
   */
  public ProcessTrace getProcessTrace() {
    return processTrace;
  }

  /**
   * Returns current SynchPoint object
   * 
   * @return - SynchPoint object
   */
  public SynchPoint getSynchPoint() {
    return synchPoint;
  }

  /**
   * Adds ProcessTrace to save in a checkpoint
   * 
   * @param trace -
   *          ProcessTrace to save
   */
  public void setProcessTrace(ProcessTrace trace) {
    processTrace = trace;
  }

  /**
   * Adds SynchPoint to save in a checkpoint
   * 
   * @param point -
   *          SynchPoint to save
   */
  public void setSynchPoint(SynchPoint point) {
    synchPoint = point;
  }

}
