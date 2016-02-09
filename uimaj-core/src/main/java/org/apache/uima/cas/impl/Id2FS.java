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
import java.util.function.Consumer;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * A map from ints representing FS id's (or "addresses") to those FSs
 * There is one map instance per CAS (all views).
 * 
 * The values are weak references, to allow gc
 * 
 * New additions always have increasing int keys.
 * 
 * Removes not supported; the weak refs allow garbage collection to reclaim the feature structure space.
 * 
 * Searching is by simple index lookup in an ArrayList
 * 
 * Alternative: a map based on sorted arrays, searched by binary search
 */
public class Id2FS {
   
  final private ArrayList<WeakReference<TOP>> id2fsw;
  
  public Id2FS() {  
    id2fsw = new ArrayList<>();
    id2fsw.add(null);  // because id's start with 1
  }
  
  /**
   * @param fs -
   */
  public void add(TOP fs) {
    id2fsw.add(new WeakReference<TOP>(fs));
  }
 
  public <T extends TOP> T get(int id) {
    if (id < 1 || id >= id2fsw.size()) {
      /** The Feature Structure ID {0} is invalid.*/
      throw new CASRuntimeException(CASRuntimeException.INVALID_FS_ID, id);
    }    
    return (T) id2fsw.get(id).get();  // could return null if fs is gc'd
  }
  
  public <T extends TOP> T getWithMissingIsNull(int id) {
    if (id < 1 || id >= id2fsw.size()) {
      return null;
    }    
    return (T) id2fsw.get(id).get();  // could return null if fs is gc'd
  }
    
  int size() {
    return id2fsw.size(); 
  }
  
  void clear() {
    id2fsw.clear();
    id2fsw.add(null); // so that ids start at 1
    id2fsw.trimToSize();
  }
  
  /**
   * plus means all reachable, plus maybe others not reachable but not yet gc'd
   * @param action
   */
  void walkReachablePlusFSsSorted(Consumer<TOP> action) {
    for (WeakReference<TOP> wr : id2fsw.subList(1, id2fsw.size())) {
      TOP fs = wr.get();
      if (null != fs) {
        action.accept(fs);
      }
    }
  }
}
