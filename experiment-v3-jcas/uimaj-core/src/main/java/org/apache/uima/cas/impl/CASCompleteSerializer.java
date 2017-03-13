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

package org.apache.uima.cas.impl;

import java.io.Serializable;

/**
 * This is a small object which contains 
 *   - CASMgrSerializer instance - a Java serializable form of the type system + index definitions
 *   - CASSerializer instance - a Java serializable form of the CAS including lists of which FSs are indexed  
 */
public class CASCompleteSerializer implements Serializable {

  static final long serialVersionUID = 6841574968081866308L;

  private CASMgrSerializer casMgrSer;

  private CASSerializer casSer;

  /**
   * Constructor for CASCompleteSerializer.
   */
  public CASCompleteSerializer() {
    super();
  }

  public CASCompleteSerializer(CASImpl cas) {
    this();
    this.casMgrSer = Serialization.serializeCASMgr(cas);
    this.casSer = Serialization.serializeCAS(cas);
  }

  public CASMgrSerializer getCASMgrSerializer() {
    return this.casMgrSer;
  }

  public CASSerializer getCASSerializer() {
    return this.casSer;
  }

  /**
   * Sets the casMgrSer.
   * 
   * @param casMgrSer
   *          The casMgrSer to set
   */
  public void setCasMgrSerializer(CASMgrSerializer casMgrSer) {
    this.casMgrSer = casMgrSer;
  }

  /**
   * Sets the casSer.
   * 
   * @param casSer
   *          The casSer to set
   */
  public void setCasSerializer(CASSerializer casSer) {
    this.casSer = casSer;
  }

}
