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

/**
 * Common part of array impl for those arrays of primitives which exist in the main heap. Is a super
 * class to those.
 */
public abstract class CommonArrayFSImpl extends FeatureStructureImplC {

  protected CommonArrayFSImpl() {
    super();
  }

  protected CommonArrayFSImpl(CASImpl cas, int addr) {
    super(cas, addr);
  }

  public int size() {
    return this.casImpl.ll_getArraySize(this.addr);
  }

  public abstract void copyToArray(int srcOffset, String[] dest, int destOffset, int length);

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
}
