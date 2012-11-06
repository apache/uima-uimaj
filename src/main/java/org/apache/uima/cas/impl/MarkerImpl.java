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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Marker;

/**
 * A MarkerImpl holds a high-water "mark" in the CAS,
 * for all views.
 * Typically, one is obtained via the createMarker call
 * on a CAS.
 * 
 * Currently only one marker is used per CAS.
 * The Marker enables testing on each CAS update if the
 * update is "below" or "above" the marker - this is
 * used for implementing delta serialization, in which
 * only the changed data is sent.
 */
public class MarkerImpl implements Marker {
	
  protected int nextFSId;    //next FS addr
  protected int nextStringHeapAddr; 
  protected int nextByteHeapAddr;
  protected int nextShortHeapAddr;
  protected int nextLongHeapAddr;
  protected boolean isValid;
  
  CASImpl cas;

  MarkerImpl(int nextFSAddr, int nextStringHeapAddr, 
		     int nextByteHeapAddr, int nextShortHeapAddr, int nextLongHeapAddr, 
		     CASImpl cas) {
    this.nextFSId = nextFSAddr;
    this.nextStringHeapAddr = nextStringHeapAddr;
    this.nextByteHeapAddr = nextByteHeapAddr;
    this.nextShortHeapAddr = nextShortHeapAddr;
    this.nextLongHeapAddr = nextLongHeapAddr;
    this.cas = cas;
    this.isValid = true;
  }

  public boolean isNew(FeatureStructure fs) {
  	//check if same CAS instance
  	//TODO: define a CASRuntimeException
  	if (!isValid || ((FeatureStructureImpl) fs).getCASImpl() != this.cas) {
  		CASRuntimeException e = new CASRuntimeException(
  		          CASRuntimeException.CAS_MISMATCH,
  		          new String[] { "FS and Marker are not from the same CAS." });
  		      throw e;
  	}
  	return isNew( ((FeatureStructureImpl) fs).getAddress());
  }

  public boolean isModified(FeatureStructure fs) {
	if (!isValid || ((FeatureStructureImpl) fs).getCASImpl() != this.cas) {
		CASRuntimeException e = new CASRuntimeException(
		          CASRuntimeException.CAS_MISMATCH,
		          new String[] { "FS and Marker are not from the same CAS." });
		      throw e;
	}
	int addr = ((FeatureStructureImpl) fs).getAddress();
	  return isModified(addr);
  }
  
  boolean isNew(int addr) {
	  return (addr >= nextFSId);
  }
  
  boolean isModified(int addr) {
  	if (isNew(addr)) {
  		return false;
      }
  	return this.cas.getModifiedFSList().contains(addr);
  }
  
  public boolean isValid() {
    return isValid;
  }

  public int getNextFSId() {
    return nextFSId;
  }

  public int getNextStringHeapAddr() {
    return nextStringHeapAddr;
  }

  public int getNextByteHeapAddr() {
    return nextByteHeapAddr;
  }

  public int getNextShortHeapAddr() {
    return nextShortHeapAddr;
  }

  public int getNextLongHeapAddr() {
    return nextLongHeapAddr;
  }
  
}
