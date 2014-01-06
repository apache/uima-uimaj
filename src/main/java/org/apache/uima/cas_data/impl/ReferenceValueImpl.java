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

package org.apache.uima.cas_data.impl;

import org.apache.uima.cas_data.ReferenceValue;

/**
 * 
 * 
 */
public class ReferenceValueImpl implements ReferenceValue {
  
  private static final long serialVersionUID = -2890705944833477494L;

  private String mTargetId;

  public ReferenceValueImpl(String aTargetId) {
    mTargetId = aTargetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.ReferenceValue#getTargetId()
   */
  public String getTargetId() {
    return mTargetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.ReferenceValue#setTargetId(java.lang.String)
   */
  public void setTargetId(String aId) {
    mTargetId = aId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.FeatureValue#get()
   */
  public Object get() {
    return mTargetId;
  }

  public String toString() {
    return mTargetId;
  }
}
