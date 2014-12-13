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

import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.rb_trees.Int2IntRBT;

/**
 * Manage the conversion of Items (FSrefs) to relative sequential index number, and back 
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
   * map from a target FS sequence nbr to a source address.
   *   value is 0 if the target instance doesn't exist in the source
   *     (this doesn't occur for receiving remote CASes back
   *      (because src ts is always a superset of tgt ts),
   *      but can occur while deserializing from Disk.
   *      
   * First seq number is 0.
   */
  final private IntVector tgtSeq2SrcAddr;
  
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
   * map from source address to target sequence number.
   * if source is not in target, value = -1;
   */
  final private Int2IntRBT srcAddr2TgtSeq;
  
  /**
   * info needed to do a map from target aux heap to source aux heap
   * Used when applying delta modifications "below the line" to these elements
   *   Assumes any target ts element exists in source ts, so target is a subset
   *   (due to type merging, when delta cas is used to return updates from service)
   */
  

  /**
   * Indexed by AuxHeap kind: 
   */

//  final private List<List<AuxSkip>> skips = new ArrayList<List<AuxSkip>>(AuxHeap.values().length);
//  
//  { // initialize instance block
//    for (int i = 0; i < skips.size(); i++) {
//      skips.add(new ArrayList<AuxSkip>());
//    }
//  }
 
  private int nextTgt = 0;

  public CasSeqAddrMaps() {
    // this call makes the first real seq number == 1.
    // seq 0 refers to the NULL fs value at heap location 0.
    this.tgtSeq2SrcAddr = new IntVector();
    this.srcAddr2TgtSeq = new Int2IntRBT();
    addItemAddr(0, 0, true);
  }
  
  public CasSeqAddrMaps(IntVector tgtSeq2SrcAddr, Int2IntRBT srcAddr2TgtSeq) {
    this.tgtSeq2SrcAddr = tgtSeq2SrcAddr;
    this.srcAddr2TgtSeq = srcAddr2TgtSeq;
  }
        
  /**
   * Add a new FS address - done during prescan of source
   * Must call in heap scan order
   * @param srcAddr -
   * @param tgtAddr -
   * @param inTarget true if this type is in the target
   */
  public void addItemAddr(int srcAddr, int tgtAddr, boolean inTarget) {
    if (inTarget) {
      tgtSeq2SrcAddr.add(srcAddr);
//      tgtSeq2TgtAddr.add(tgtAddr);
    }
    srcAddr2TgtSeq.put(srcAddr, inTarget ? nextTgt++ : -1);
//    // debug
//    if (srcAddr < 525) {
//      System.out.format("Adding to srcAddr2TgtSeq: addr: %d tgtSeq: %d, type=%s%n", srcAddr, inTarget ? i : 0, 
//         );
//    }
//    srcSeq2TgtSeq.add(inTarget ? nextTgt++ : 0);
  }
  
//  /**
//   * record skipped entries in an Aux heap
//   * @param auxHeap which heap this is for
//   * @param srcSkipIndex the index of the first skipped slot in the src heap
//   * @param srcSkipSize the number of entries skipped
//   */
//  public void recordSkippedAuxHeap(AuxHeap auxHeap, int srcSkipIndex, int srcSkipSize) {
//    skips.get(auxHeap.ordinal()).add(new AuxSkip(srcSkipIndex, srcSkipSize));
//  }
  
  /**
   * Called during deserialize to incrementally add 
   * @param srcAddr -
   * @param inSrc -
   */
  public void addSrcAddrForTgt(int srcAddr, boolean inSrc) {
    if (inSrc) {
      srcAddr2TgtSeq.put(srcAddr, nextTgt);
//      srcSeq2TgtSeq.add(nextTgt);
      tgtSeq2SrcAddr.add(srcAddr);
    } else {
      tgtSeq2SrcAddr.add(0);
    }
//    tgtSeq2TgtAddr.add(-1);  // not used I hope - need to check TODO
    nextTgt++;
  }
             
  /**
   * 
   * @param seq -
   * @return 0 means target seq doesn't exist in source CAS
   */
  public int getSrcAddrFromTgtSeq(int seq) {
    return tgtSeq2SrcAddr.get(seq);
  }

//  public int getTgtAddrFromTgtSeq(int seq) {
//    return tgtSeq2TgtAddr.get(seq);
//  }

//  public int getMappedItemAddr(int index) {
//    if (null == typeMapper) {
//      return tgtIndexToSeq.get(index);
//    } else {
//      return tgtItemIndexToAddr.get(index);
//    }
//  }
  
  /**
   * @param itemAddr -
   * @return -1 if src addr not in target seq
   */
  public int getTgtSeqFromSrcAddr(int itemAddr) {
//    System.out.println(" " + itemAddr);
    return srcAddr2TgtSeq.getMostlyClose(itemAddr);      
  }
  
  public int getNumberSrcFss() {
    return srcAddr2TgtSeq.size();
  }

  CasSeqAddrMaps copy() {
    CasSeqAddrMaps c = new CasSeqAddrMaps(tgtSeq2SrcAddr.copy(), srcAddr2TgtSeq.copy());
    c.nextTgt = nextTgt;
    return c;    
  }
  
}
