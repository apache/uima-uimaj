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

import org.apache.uima.cas.CAS;

/**
 * Feature structure implementation.
 * 
 * 
 */
public class FeatureStructureImplC extends FeatureStructureImpl {

  final protected CASImpl casImpl;

  final protected int addr;

  protected FeatureStructureImplC() {
    this.casImpl = null;
    this.addr = 0;
  }

  FeatureStructureImplC(CASImpl casImpl, int addr) {
    // assert(addr > 0);
    this.addr = addr;
    this.casImpl = casImpl;
  }

//  public void setUp(CASImpl casImpl, int addr) {
//    this.addr = addr;
//    this.casImpl = casImpl;
//  }

  public int getAddress() {
    return this.addr;
  }

  public CAS getCAS() {
    return this.casImpl;
  }

  public CASImpl getCASImpl() { // was package private 9-03
    return this.casImpl;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!(o instanceof FeatureStructureImplC)) {
      return false;
    }
    FeatureStructureImplC fs = (FeatureStructureImplC) o;
    if ((this.addr == fs.addr) && (this.casImpl.getBaseCAS() == fs.casImpl.getBaseCAS())) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return this.addr;
  }

}
