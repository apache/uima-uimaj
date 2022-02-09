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

package org.apache.uima.collection.impl.cpm.utils;

/**
 * The Class ValuePair.
 */
public class ValuePair {

  /** The old V. */
  private String oldV;

  /** The new V. */
  private String newV;

  /**
   * Instantiates a new value pair.
   *
   * @param oldValue
   *          the old value
   * @param newValue
   *          the new value
   */
  public ValuePair(String oldValue, String newValue) {
    oldV = oldValue;
    newV = newValue;
  }

  /**
   * Returns the newV.
   * 
   * @return String
   */
  public String getNewV() {
    return newV;
  }

  /**
   * Returns the oldV.
   * 
   * @return String
   */
  public String getOldV() {
    return oldV;
  }

  /**
   * Sets the newV.
   * 
   * @param newV
   *          The newV to set
   */
  public void setNewV(String newV) {
    this.newV = newV;
  }

  /**
   * Sets the oldV.
   * 
   * @param oldV
   *          The oldV to set
   */
  public void setOldV(String oldV) {
    this.oldV = oldV;
  }

}
