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

package org.apache.uima.collection.impl.cpm.engine;

import org.apache.uima.cas.CAS;



/**
 * The Class WorkUnit.
 */
public class WorkUnit {
  
  /** The payload. */
  private Object payload = null;

  /** The cas. */
  private CAS[] cas = null;

  /** The timedout. */
  private boolean timedout = false;

  
  /**
   * Instantiates a new work unit.
   *
   * @param aPayload the a payload
   */
  public WorkUnit(Object aPayload) {
    payload = aPayload;
  }

  /**
   * Gets the.
   *
   * @return the object
   */
  public Object get() {
    return payload;
  }

  /**
   * Sets the cas.
   *
   * @param aCas the new cas
   */
  public void setCas(CAS[] aCas) {
    cas = aCas;
  }

  /**
   * Gets the cas.
   *
   * @return the cas
   */
  public CAS[] getCas() {
    return cas;
  }

  /**
   * Sets the timed out.
   */
  public void setTimedOut() {
    timedout = true;
  }

  /**
   * Checks if is timed out.
   *
   * @return true, if is timed out
   */
  public boolean isTimedOut() {
    return timedout;
  }
}
