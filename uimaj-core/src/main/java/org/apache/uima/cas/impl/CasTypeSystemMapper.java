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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.Type;
import org.apache.uima.resource.ResourceInitializationException;

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
 * Serializing:  Source ts -%gt; generate serialized form in Target ts 
 * Deserializing: Target ts -%gt; generate deserialized form in Source ts
 *   - either from remote or
 *   - from disk-stored-form
 * 
 * Mapping details:
 *   Types are mapped by name. 
 *     Same-named types do not need to have the same number of features.
 *     Same-named features must have same Range - otherwise, not mapped.
 *     Types with 0 features mapped allowed.
 * LifeCycle:
 *   Instance of this are created for a CAS when needed, and then
 *   kept in the (source) TypeSystemImpl, in a map indexed by
 *   the target type system (identity map)
 *   
 * 
 */

public class CasTypeSystemMapper {
  private final static int[] INT0 = new int[0];
  private final static boolean[] BOOLEAN0 = new boolean[0];
  
  public final TypeSystemImpl tsSrc;  // source type system
  // weak ref to target type system, to allow that object to be gc'd
  //   which in turn allows a weak map using these as keys to reclaim space
  public final WeakReference<TypeSystemImpl> tsTgt;
  
  /** 
   * Map from source type codes to target type codes.  
   * Source type code used as index, 
   * value is target type code 
   */
  final private int[] tSrc2Tgt;
  
  /**
   * First index is src type code, 2nd index is src feature offset, 0 is 1st feature.
   * Value is true if target has source feature
   */
  final private boolean[][] fSrcInTgt; 
  
  /** 
   * First index is src type code, 2nd index is tgt feature offset, 0 is 1st feature.
   * Value is -1 if src doesn't have feature, else it is the feature offset in source.
   * Only used for type codes that are not arrays.
   * Use: When serializing a source type that exists in the target, have to output
   *   the slots in the target feature order
   *   Also, when comparing the slots in the target with a given source
   */
  final private int[][] fTgt2Src;

  /** 
   * Same as tSrc2Tgt, but reversed 
   * used when deserializing a target back into a source 
   */
  final private int[] tTgt2Src;
  
  final private boolean typeSystemsSame;
  
  public boolean isEqual() {
    return this.typeSystemsSame;
  }
  
  public CasTypeSystemMapper(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) throws ResourceInitializationException {
    if (!tsSrc.isCommitted() || !tsTgt.isCommitted()) {
      throw new RuntimeException("Type Systems must be committed before calling this method");
    }
    this.tsSrc = tsSrc;
    this.tsTgt = new WeakReference<TypeSystemImpl>(tsTgt);
    
    int[] temptSrc2Tgt = null;
    int[] temptTgt2Src = null;
    boolean[][] localFSrcInTgt = null;
    int[][] localFTgt2Src = null;
    if (tsSrc == tsTgt) {
      typeSystemsSame = true;
    } else {
      temptSrc2Tgt = addTypes(tsSrc, tsTgt);
      temptTgt2Src = addTypes(tsTgt, tsSrc);
    
      localFSrcInTgt = new boolean[tsSrc.getTypeArraySize()] [];
      localFTgt2Src = new int[tsSrc.getTypeArraySize()] [];
      typeSystemsSame = addFeatures(tsSrc, tsTgt, localFSrcInTgt, localFTgt2Src, temptSrc2Tgt);
    }
    
    this.tSrc2Tgt = temptSrc2Tgt;
    this.tTgt2Src = temptTgt2Src;
    this.fSrcInTgt =localFSrcInTgt;
    this.fTgt2Src = localFTgt2Src;
    
//    this.fTgt2Src = addFeatures(tsTgt, tsSrc);
  }
  
  /**
   * @param c -
   * @return 0 if type doesn't have corresponding code in other type system
   */
  public int mapTypeCodeSrc2Tgt(int c) {
    return tSrc2Tgt[c];
  }

  /**
   * @param c -
   * @return 0 if type doesn't have corresponding code in other type system
   */
  public int mapTypeCodeTgt2Src(int c) {
    return tTgt2Src[c];
  }
  /**
   * 
   * @param c -
   * @param src2tgt -
   * @return 0 if type doesn't have corresponding code in other type system
   */
  public int mapTypeCode2Other(int c, boolean src2tgt) {
    if (src2tgt) {
      return mapTypeCodeSrc2Tgt(c);
    } 
    return mapTypeCodeTgt2Src(c);
  }

  /**
   * 
   * @param tCode - source type code
   * @return int vec of 0-based feat offsets in src of feats in tgt, in the order of the target
   */
  public int[] getTgtFeatOffsets2Src(int tCode) {
    return fTgt2Src[tCode];
  }
  
  public boolean[] getFSrcInTgt(int tCode) {
    return fSrcInTgt[tCode];
  }
  
//  /**
//   * 
//   * @param tCode source type code
//   * @param offset feature slot offset, 0 = first feature after type code
//   * @return offset to slot in target, 0 = first feature slot after type code, -1 if feature doesn't have corresponding code in other type system
//   */
//  public int mapFeatureOffsetSrc2Tgt(int tCode, int offset) {
//    return fSrc2Tgt[tCode][offset];
//  }

  // returns 0 if feature doesn't have corresponding code in other type system
//  public int mapFeatureCodeTgt2Src(int c) {
//    return fTgt2Src[c];
//  }

  private static int[] addTypes(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
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
  
  private boolean addFeatures(final TypeSystemImpl tsSrc, final TypeSystemImpl tsTgt, final boolean[][] localFSrcInTgt, final int[][] localFTgt2Src, final int[] temptSrc2Tgt) throws ResourceInitializationException {
    boolean isEqual = tsSrc.getTypeArraySize() == tsTgt.getTypeArraySize();
    for (int tCodeSrc = 0; tCodeSrc < tsSrc.getTypeArraySize(); tCodeSrc++) {
      final int tCodeTgt = temptSrc2Tgt[tCodeSrc];
      if (tCodeTgt != tCodeSrc) {
        isEqual = false;
      }
      if (tCodeTgt == 0) {  // this type not in target
        localFSrcInTgt[tCodeSrc] = BOOLEAN0;
        localFTgt2Src[tCodeSrc] = null;  // should never be referenced
        continue;
      }
      
      // type is part of target ts
      final int[] fcSrc = tsSrc.ll_getAppropriateFeatures(tCodeSrc);
      final int[] fcTgt = tsTgt.ll_getAppropriateFeatures(tCodeTgt);
      
      if (fcSrc.length != fcTgt.length) {
        isEqual = false;
      }
      
      if (fcSrc.length == 0) {
        // source has no features
        localFSrcInTgt[tCodeSrc] = BOOLEAN0;
        localFTgt2Src[tCodeSrc] = new int[fcTgt.length];
        Arrays.fill(localFTgt2Src[tCodeSrc], -1);
        continue;  // source type has no features        
      }
      
      final boolean[] srcInTgt = new boolean[fcSrc.length];
      localFSrcInTgt[tCodeSrc] = srcInTgt;
      
      if (fcTgt.length == 0) {
        Arrays.fill(srcInTgt, false);
        localFTgt2Src[tCodeSrc] = INT0;
        continue;  // target type has no features        
      }
      
      final int[] tgt2srcOffsets = new int[fcTgt.length];
      localFTgt2Src[tCodeSrc] = tgt2srcOffsets;
      
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
        if (iTgt == -1) {
          isEqual = false;          
          srcInTgt[fciSrc] = false;
        } else {
          if (! tsSrc.ll_getFeatureForCode(fcSrc[fciSrc]).getRange().getName().equals(
                tsTgt.ll_getFeatureForCode(fcTgt[iTgt]).getRange().getName())) {
            throw new ResourceInitializationException(
                ResourceInitializationException.INCOMPATIBLE_RANGE_TYPES, new Object[] {
                    tsSrc.ll_getTypeForCode(tCodeSrc).getName() + ":" + nameSrc, 
                    tsSrc.ll_getFeatureForCode(fcSrc[fciSrc]).getRange().getName(), 
                    tsTgt.ll_getFeatureForCode(fcTgt[iTgt]).getRange().getName(),
                    ""});
          }
          srcInTgt[fciSrc] = true;
          
          
        } 
      } // end of for loop over all source features of a type code
      
      // for each feature in the target, find the corresponding source feature by name match (if any)
      for (int fciTgt = 0; fciTgt < fcTgt.length; fciTgt++) {
        final String nameTgt = namesTgt.get(fciTgt);
        // feature names are semi sorted, not completely sorted due to inheritence
        final int iSrc = namesSrc.indexOf(nameTgt);
        tgt2srcOffsets[fciTgt] = iSrc;  // -1 if not there
        if (fciTgt != iSrc) {
          isEqual = false;
        }
      } // end of for loop over all target features of a type code      
    }   // end of for loop over all typecodes
    return isEqual;
  }

  
}
