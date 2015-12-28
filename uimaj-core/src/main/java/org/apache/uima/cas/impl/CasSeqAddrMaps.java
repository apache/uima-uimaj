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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.rb_trees.Int2IntRBT;
import org.apache.uima.jcas.cas.TOP;

/**
 * Used by Binary serialization form 6
 * 
 * Manage the conversion of FSs to relative sequential index number, and back 
 * Manage the difference in two type systems
 *   both size of the FSs and
 *   handling excluded types
 * 
 * During serialization, these maps are constructed before serialization.
 * During deserialization, these maps are constructed while things are being deserialized, and
 *   then used in a "fixup" call at the end.
 *   This allows for forward references.
 *   
 *   For delta deserialization, the base part of these maps (for below-the-line) is
 *   constructed by scanning up to the mark.  
 */
public class CasSeqAddrMaps {
  
  /**
   * map from a target FS sequence nbr to a source id.
   *   value is 0 if the target instance doesn't exist in the source
   *     (this doesn't occur for receiving remote CASes back
   *      (because src ts is always a superset of tgt ts),
   *      but can occur while deserializing from Disk.
   *      
   * First seq number is 0.
   */
  final private IntVector tgtId2SrcId;
  
//  /**
//   * map from a target FS sequence nbr to a source FS.
//   *   value is null if the target instance doesn't exist in the source
//   */
//  final private List<TOP> tgtId2SrcFs;
  
//  /**
//   * (Not Used, currently)
//   * map from a source seq number to a target seq number.
//   * value is -1 if the source FS is not in the target
//   */
//  final private IntVector srcSeq2TgtSeq = new IntVector();
  
//  /**
//   * (Not Used, currently)
//   * map from a target seq number to a target address.
//   */
//  final private IntVector tgtSeq2TgtAddr = new IntVector();  // used for comparing
  
  /**
   * map from source id to target id.
   * if source is not in target, value = -1;
   */
  final private Int2IntRBT srcId2TgtId;
   
  private int nextTgt = 0;

  public CasSeqAddrMaps() {
    // this call makes the first real seq number == 1.
    // seq 0 refers to the NULL fs value.
    this.tgtId2SrcId = new IntVector();
//    this.tgtId2SrcFs = new ArrayList<>();
    this.srcId2TgtId = new Int2IntRBT();
    addItemId(0, 0, true);
  }
  
  public CasSeqAddrMaps(IntVector tgtSeq2SrcAddr, Int2IntRBT srcAddr2TgtSeq) {
    this.tgtId2SrcId = tgtSeq2SrcAddr;
    this.srcId2TgtId = srcAddr2TgtSeq;
  }
        
  /**
   * Add a new FS id - done during prescan of source
   * Must call in heap scan order
   * @param srcId -
   * @param tgtId -
   * @param inTarget true if this type is in the target
   */
  public void addItemId(int srcId, int tgtId, boolean inTarget) {
    if (inTarget) {
      tgtId2SrcId.add(srcId);
    }
    srcId2TgtId.put(srcId, inTarget ? nextTgt++ : -1);
  }
  
  /**
   * Called during deserialize to incrementally add 
   * @param srcAddr -
   * @param inSrc -
   */
  public void addSrcAddrForTgt(int srcAddr, boolean inSrc) {
    if (inSrc) {
      srcId2TgtId.put(srcAddr, nextTgt);
      tgtId2SrcId.add(srcAddr);
    } else {
      tgtId2SrcId.add(0);
    }
    nextTgt++;
  }
  
//  public void addSrcFsForTgtId(TOP srcFS, boolean isInSrc) {
//    if (isInSrc) {
//      tgtId2SrcFs.add(srcFS);
//    } else {
//      tgtId2SrcFs.add(null);
//    }
//  }
             
  /**
   * 
   * @param seq -
   * @return 0 means target seq doesn't exist in source CAS
   */
  public int getSrcAddrFromTgtSeq(int seq) {
    if (seq >= tgtId2SrcId.size()) {
      return 0;
    }
    return tgtId2SrcId.get(seq);
  }

  /**
   * @param itemAddr -
   * @return -1 if src addr not in target seq
   */
  public int getTgtSeqFromSrcAddr(int itemAddr) {
    return srcId2TgtId.getMostlyClose(itemAddr);      
  }
  
  public int getNumberSrcFss() {
    return srcId2TgtId.size();
  }

  CasSeqAddrMaps copy() {
    CasSeqAddrMaps c = new CasSeqAddrMaps(
        tgtId2SrcId.copy(),
        srcId2TgtId.copy());
    c.nextTgt = nextTgt;
    return c;    
  }
  
}
