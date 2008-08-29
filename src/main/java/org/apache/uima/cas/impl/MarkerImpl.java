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

public class MarkerImpl implements Marker {
	
  private int nextFSId;
  CASImpl cas;

  MarkerImpl(int nextPos, CASImpl cas) {
    this.nextFSId = nextPos;
    this.cas = cas;
  }

  public boolean isNew(FeatureStructure fs) {
	//check if same CAS instance
	//TODO: define a CASRuntimeException
	if (((FeatureStructureImpl) fs).getCASImpl() != this.cas) {
		CASRuntimeException e = new CASRuntimeException(
		          CASRuntimeException.CAS_MISMATCH,
		          new String[] { "FS and Marker are not from the same CAS." });
		      throw e;
	}
	return isNew( ((FeatureStructureImpl) fs).getAddress());
  }

  public boolean isModified(FeatureStructure fs) {
	if (((FeatureStructureImpl) fs).getCASImpl() != this.cas) {
		CASRuntimeException e = new CASRuntimeException(
		          CASRuntimeException.CAS_MISMATCH,
		          new String[] { "FS and Marker are not from the same CAS." });
		      throw e;
	}
	int addr = ((FeatureStructureImpl) fs).getAddress();
	return isModified(addr);
  }
  
  boolean isNew(int addr) {
	return (addr == nextFSId || addr > nextFSId);
  }
  
  boolean isModified(int addr) {
	if (isNew(addr)) {
		return false;
    }
	return this.cas.getModifiedFSList().contains(addr);
  }
}
