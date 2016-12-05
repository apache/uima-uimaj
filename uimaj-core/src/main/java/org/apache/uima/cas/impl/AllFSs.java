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
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;

/**
 * support for collecting all FSs in a CAS
 *   -  over all views
 *   -  both indexed, and reachable
 */
class AllFSs {
  
  final CASImpl cas;
  final private MarkerImpl mark;
  final private PositiveIntSet foundFSs = new PositiveIntSet_impl(4096, 1, 4096);
  final private PositiveIntSet foundFSsBelowMark;
  final private ArrayList<TOP> toBeScanned = new ArrayList<TOP>();
  final private Predicate<TOP> includeFilter;
  final private CasTypeSystemMapper typeMapper;
  
  AllFSs(CASImpl cas, MarkerImpl mark, Predicate<TOP> includeFilter, CasTypeSystemMapper typeMapper) {
    this.cas = cas;
    this.mark = mark;
    foundFSsBelowMark = (mark != null) ? new PositiveIntSet_impl(1024, 1, 1024) : null;
    this.includeFilter = includeFilter;
    this.typeMapper = typeMapper;
    
    getAllIndexedFSsAllViews();
  }
  
  PositiveIntSet getAllBelowMark() {
    return foundFSsBelowMark;
  }
  
  PositiveIntSet getAllNew() {
    return foundFSs;
  }
  
  ArrayList<TOP> getAllFSs() {
    return toBeScanned;
  }
    
  ArrayList<TOP> getAllFSsSorted() {
    Collections.sort(toBeScanned, FeatureStructureImplC::compare);
    return toBeScanned;
  }
  
  /**
   * simpler version, no mark info, no filter or type mapper
   * @param cas -
   */
  AllFSs(CASImpl cas) {
    this.cas = cas;
    this.mark = null;
    foundFSsBelowMark = null;
    this.includeFilter = null;
    this.typeMapper = null;
    getAllIndexedFSsAllViews();    
  }
  
  void getAllIndexedFSsAllViews() {
    cas.forAllSofas(sofa -> enqueueFS(sofa));
    cas.forAllViews(view -> 
       getFSsForView(view.indexRepository.<TOP>getAllIndexedFS(cas.getTypeSystemImpl().topType).asStream()));
    for (int i = 0; i < toBeScanned.size(); i++) {
      enqueueFeatures(toBeScanned.get(i));
    }
    
//    // add FSs that are in the CAS and held-on-to explicitly
     // maybe needed for a test case - SerDesTest4 for delta CAS with previous v2 data ?
//    Int2ObjHashMap<TOP> allKeptFSs = cas.getId2FSs();
//    if (null != allKeptFSs && allKeptFSs.size() > 0) {
//      Iterator<TOP> it = allKeptFSs.values();
//      while (it.hasNext()) {
//        enqueueFS(it.next());
//      }
//    }

  }
  
  private void getFSsForView(Stream<TOP> fss) {
    fss.forEach(fs -> enqueueFS(fs));
  }
  
  private void enqueueFS(TOP fs) {
    if (null == fs || (includeFilter != null && !includeFilter.test(fs))) {
      return;
    }

    final int id = fs._id;
    
    if (mark == null || mark.isNew(fs)) { // separately track items below the line
      if (!foundFSs.contains(id)) {
        foundFSs.add(id);
        toBeScanned.add(fs);
      }
    } else {
      if (!foundFSsBelowMark.contains(id)) {
        foundFSsBelowMark.add(id);
        toBeScanned.add(fs);
      }
    }
  }
    
  private void enqueueFeatures(TOP fs) {
    if (fs instanceof FSArray) {
      for (TOP item : ((FSArray)fs)._getTheArray()) {
        enqueueFS(item);
      }
      return;
    }
    
    // not an FS Array
    if (fs instanceof CommonArray) {
      return;  // no refs
    }
  
    final TypeImpl srcType = fs._getTypeImpl();
    if (srcType.getStaticMergedNonSofaFsRefs().length > 0) {
      if (fs instanceof UimaSerializableFSs) {
        ((UimaSerializableFSs)fs)._save_fsRefs_to_cas_data();
      }
      for (FeatureImpl srcFeat : srcType.getStaticMergedNonSofaFsRefs()) {
        if (typeMapper != null) {
          FeatureImpl tgtFeat = typeMapper.getTgtFeature(srcType, srcFeat);
          if (tgtFeat == null) {
            continue;  // skip enqueue if not in target
          }
        }
        enqueueFS(fs._getFeatureValueNc(srcFeat)); 
      }
    }
    
    
//    
//    if (srcType.hasRefFeature) {
//      if (fs instanceof UimaSerializableFSs) {
//        ((UimaSerializableFSs)fs)._save_fsRefs_to_cas_data();
//      }
//      for (FeatureImpl srcFeat : srcType.getStaticMergedRefFeatures()) {
//        if (typeMapper != null) {
//          FeatureImpl tgtFeat = typeMapper.getTgtFeature(srcType, srcFeat);
//          if (tgtFeat == null) {
//            continue;  // skip enqueue if not in target
//          }
//        } 
//        if (srcFeat.getSlotKind() == SlotKind.Slot_HeapRef) {
////        if (srcFeat.getRangeImpl().isRefType) {
//          enqueueFS(fs._getFeatureValueNc(srcFeat));
//        }
//      }
//    }
  }
}
