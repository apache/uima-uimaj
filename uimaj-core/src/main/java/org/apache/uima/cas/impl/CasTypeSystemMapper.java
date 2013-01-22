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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.Type;

/**
 * This class gets initialized with two type systems, and then provides 
 * resources to map type and feature codes between them.
 * 
 * It is used by some Binary serialization/ deserialization
 * code to allow non-exact matched type systems to send and
 * receive CASes in a binary-like format.
 * 
 * Use cases:
 * 
 * Serializing:  Source ts -> generate serialized form in Target ts 
 * Deserializing: Target ts -> generate deserialized form in Source ts
 *   - either from remote or
 *   - from disk-stored-form
 *   
 * LifeCycle:
 *   Instance of this are created for a CAS when needed, and then
 *   kept in the (source) TypeSystemImpl, in a map indexed by
 *   the target type system (identity map)
 */

public class CasTypeSystemMapper {
  private final static int[] INT0 = new int[0];
  
  public final TypeSystemImpl tsSrc;  // source type system
  public final TypeSystemImpl tsTgt;  // target type system
  
  /** 
   * Map from source type codes to target type codes.  
   * Source type code used as index, 
   * value is target type code 
   */
  final private int[] tSrc2Tgt;
  
  /**
   * First index is src type code, 2nd index is src feature offset, 0 is 1st feature.
   * Value is -1 if tgt doesn't have feature, else it is the feature offset in target.
   * Only for type codes that are not arrays.
   */
  final private int[][] fSrc2Tgt; 
  
  /** 
   * First index is src type code, 2nd index is tgt feature offset, 0 is 1st feature.
   * Value is -1 if src doesn't have feature, else it is the feature offset in source.
   * Only used for type codes that are not arrays.
   * Use: When serializing a source type that exists in the target, have to output
   *   the slots in the target feature order
   *   Also, when comparing the slots in the target with a given source
   */
  final private int[][] tgtFoffsets2Src;

  /** 
   * Same as tSrc2Tgt, but reversed 
   * used when deserializing a target back into a source 
   */
  final private int[] tTgt2Src;
  
  public CasTypeSystemMapper(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
    if (!tsSrc.isCommitted() || !tsTgt.isCommitted()) {
      throw new RuntimeException("Type Systems must be committed before calling this method");
    }
    this.tsSrc = tsSrc;
    this.tsTgt = tsTgt;
    
    this.tSrc2Tgt = addTypes(tsSrc, tsTgt);
    this.tTgt2Src = addTypes(tsTgt, tsSrc);
    this.fSrc2Tgt        = new int[tsSrc.getTypeArraySize()] [];
    this.tgtFoffsets2Src = new int[tsSrc.getTypeArraySize()] [];
    addFeatures(tsSrc, tsTgt);
//    this.fTgt2Src = addFeatures(tsTgt, tsSrc);
  }
  
  // returns 0 if type doesn't have corresponding code in other type system
  public int mapTypeCodeSrc2Tgt(int c) {
    return tSrc2Tgt[c];
  }

  // returns 0 if type doesn't have corresponding code in other type system
  public int mapTypeCodeTgt2Src(int c) {
    return tTgt2Src[c];
  }

  public int[] getTgtFeatOffsets2Src(int tCode) {
    return tgtFoffsets2Src[tCode];
  }
  
  // returns -1 if feature doesn't have corresponding code in other type system
  public int mapFeatureOffsetSrc2Tgt(int tCode, int offset) {
    return fSrc2Tgt[tCode][offset];
  }

  // returns 0 if feature doesn't have corresponding code in other type system
//  public int mapFeatureCodeTgt2Src(int c) {
//    return fTgt2Src[c];
//  }

  private int[] addTypes(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
    Map<TypeImpl, TypeImpl> mSrc2Tgt = new LinkedHashMap<TypeImpl, TypeImpl>();
    for (Iterator<Type> it = tsSrc.getTypeIterator(); it.hasNext();) {
      TypeImpl tSrc = (TypeImpl) it.next();
      TypeImpl tTgt = (TypeImpl) tsTgt.getType(tSrc.getName());
      if (tTgt != null) {
        mSrc2Tgt.put(tSrc, tTgt);
      }
    }
    int[] r = new int[tsSrc.getNumberOfTypes() + 1];  // type codes are numbered starting with 1
    for (Entry<TypeImpl, TypeImpl> e : mSrc2Tgt.entrySet()) {
      r[e.getKey().getCode()] = e.getValue().getCode();
    }
    return r;  
  }
  
  private void addFeatures(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
    for (int tCodeSrc = 0; tCodeSrc < tsSrc.getTypeArraySize(); tCodeSrc++) {
      final int tCodeTgt = mapTypeCodeSrc2Tgt(tCodeSrc);
      if (tCodeTgt == 0) {  // this type not in target
        fSrc2Tgt[tCodeSrc] = INT0;
        tgtFoffsets2Src[tCodeSrc] = null;  // should never be referenced
        continue;
      }
      
      // type is part of target ts
      final int[] fcSrc = tsSrc.ll_getAppropriateFeatures(tCodeSrc);
      final int[] fcTgt = tsTgt.ll_getAppropriateFeatures(tCodeTgt);
      
      if (fcSrc.length == 0) {
        // source has no features
        fSrc2Tgt[tCodeSrc] = INT0;
        tgtFoffsets2Src[tCodeSrc] = new int[fcTgt.length];
        Arrays.fill(tgtFoffsets2Src[tCodeSrc], -1);
        continue;  // source type has no features        
      }
      
      final int[] src2tgtOffsets = new int[fcSrc.length];
      fSrc2Tgt[tCodeSrc] = src2tgtOffsets;
      
      if (fcTgt.length == 0) {
        Arrays.fill(src2tgtOffsets, -1);
        tgtFoffsets2Src[tCodeSrc] = INT0;
        continue;  // target type has no features        
      }
      final int[] tgt2srcOffsets = new int[fcTgt.length];
      tgtFoffsets2Src[tCodeSrc] = tgt2srcOffsets;
      
//      // debug 
//      if (tCodeTgt == 228) {
//        String ss[] = new String[fcTgt.length];
//        for (int i = 0; i < fcTgt.length; i++) {
//          ss[i] = tsTgt.ll_getFeatureForCode(fcTgt[i]).getName();
//        }
//        System.out.print("");
//      }
//      // debug - verify features are in alpha order
//      String ss[] = new String[fcSrc.length];
//      String prev = " ";
//      boolean fault = false;
//      for (int i = 0; i < fcSrc.length; i++) {
//        String s = tsSrc.ll_getFeatureForCode(fcSrc[i]).getName();
//        ss[i] = s;
//        if (prev.compareTo(s) >= 0) {
//          fault = true;
//          System.out.format("Source feature names not sorted, prev = %s, this = %s%n", prev, s);
//        }
//        prev = s;
//      }
//      if (fault) {
//        System.out.print("");
//      }
//      prev = " ";
//      if (tCodeTgt == 228) {
//
//      for (int i = 0; i < fcTgt.length; i++) {
//        String s = tsTgt.ll_getFeatureForCode(fcTgt[i]).getName();
//        if (prev.compareTo(s) >= 0) {
//          fault = true;
//          System.out.format("Target feature names not sorted, prev = %s, this = %s%n", prev, s);
//        }
//        prev = s;
//      }
//      }      
      
      // get List of names of appropriate features in the target for this type
      List<String> namesTgt = new ArrayList<String>(fcTgt.length);
      for (int i = 0; i < fcTgt.length; i++) {
        namesTgt.add(tsTgt.ll_getFeatureForCode(fcTgt[i]).getName());
      }
      
      // get List of names of appropriate features in the source for this type
      List<String> namesSrc = new ArrayList<String>(fcSrc.length);
      for (int i = 0; i < fcSrc.length; i++) {
        namesSrc.add(tsSrc.ll_getFeatureForCode(fcSrc[i]).getName());
      }
      
            
      // for each feature in the source, find the corresponding target feature by name match (if any)
      for (int fciSrc = 0; fciSrc < fcSrc.length; fciSrc++) {
        final String nameSrc = namesSrc.get(fciSrc);
        // feature names are semi sorted, not completely sorted due to inheritence
        final int iTgt = namesTgt.indexOf(nameSrc);
        src2tgtOffsets[fciSrc] = iTgt;  // -1 if not there
      } // end of for loop over all source features of a type code
      
      // for each feature in the target, find the corresponding source feature by name match (if any)
      for (int fciTgt = 0; fciTgt < fcTgt.length; fciTgt++) {
        final String nameTgt = namesTgt.get(fciTgt);
        // feature names are semi sorted, not completely sorted due to inheritence
        final int iSrc = namesSrc.indexOf(nameTgt);
        tgt2srcOffsets[fciTgt] = iSrc;  // -1 if not there
      } // end of for loop over all target features of a type code
      
      
    }   // end of for loop over all typecodes
  }

  
}
