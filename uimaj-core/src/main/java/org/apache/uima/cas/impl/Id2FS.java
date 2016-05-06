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
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

/**
 * A map from ints representing FS id's (or "addresses") to those FSs
 * There is one map instance per CAS (all views).
 * 
 * The values are weak references, to allow gc
 * 
 * New additions always have increasing int keys.
 * 
 * IN THIS IMPL, the id is the index into the array.
 * IN THIS IMPL, Searching is by simple index lookup in an ArrayList
 * 
 * Removes not supported; the weak refs allow garbage collection to reclaim the feature structure space.
 * 
 * Alternative: a map based on sorted arrays, searched by binary search
 */
public class Id2FS {
  
  /**
   * Set this JVM property to true for backwards compatibility, where an application retains
   * some references to Feature Structures held only using the low-level references (which are ints)..
   */
  public static final String DISABLE_FS_GC = "uima.disable_feature_structure_garbage_collection";
  
  public static final boolean IS_DISABLE_FS_GC =   // true || // debug
      Misc.getNoValueSystemProperty(DISABLE_FS_GC);
  
  final private ArrayList<Object> id2fsw;
  
  public Id2FS(int initialHeapSize) {  
    id2fsw = new ArrayList<>(initialHeapSize >> 4);  
    id2fsw.add(null);  // because id's start with 1
  }
  
  /**
   * @param fs -
   */
  public void add(TOP fs) {
    id2fsw.add(IS_DISABLE_FS_GC ? fs : new WeakReference<TOP>(fs));
  }
  
  public void setStrongRef(TOP fs, int i) {
    id2fsw.set(i, fs);
  }
  
  public void replaceWithStrongRef(TOP fs) {
    if (IS_DISABLE_FS_GC) {
      return;
    } 
    id2fsw.set(fs._id, fs);  
  }
 
  public <T extends TOP> T get(int id) {
    if (id < 1 || id >= id2fsw.size()) {
      /** The Feature Structure ID {0} is invalid.*/
      throw new CASRuntimeException(CASRuntimeException.INVALID_FS_ID, id);
    }  
    return getNoCheck(id);
  }
  
  public <T extends TOP> T getWithMissingIsNull(int id) {
    if (id < 1 || id >= id2fsw.size()) {
      return null;
    }    
    return getNoCheck(id);  // could return null if fs is gc'd
  }
  
  private <T extends TOP> T getNoCheck(int id) {
    Object o = id2fsw.get(id);
    if (o == null) { 
      return null;
    }
    if (o instanceof TOP) {
      return (T) o; 
    }
    return (T) ((WeakReference)o).get();  // could return null if fs is gc'd    
  }
    
  int size() {
    return id2fsw.size(); 
  }
  
  /**
   * adjusts the underlying array down in size if grew beyond the reset heap size value
   */
  void clear() {
    if (id2fsw.size() > (CASImpl.DEFAULT_RESET_HEAP_SIZE >> 4)) {
      id2fsw.clear();
      id2fsw.add(null); // so that ids start at 1
      id2fsw.trimToSize();  
      id2fsw.ensureCapacity(CASImpl.DEFAULT_INITIAL_HEAP_SIZE >> 4);     
    } else {
      id2fsw.clear();
      id2fsw.add(null); // so that ids start at 1      
    }
  }
  
  /**
   * plus means all reachable, plus maybe others not reachable but not yet gc'd
   * @param action
   */
  void walkReachablePlusFSsSorted(Consumer<TOP> action) {
    walkReachablePlueFSsSorted(action, 1);
  }
  
  /**
   * walk a part of the id2fsw list; for delta, just the part above the line
   * @param action
   * @param items the part of the id2fsw list to walk
   */
  void walkReachablePlueFSsSorted(Consumer<TOP> action, int fromId) {
//    int i;
//    if (fromId == 1) {
//      i = fromId;
//    } else {
//      TOP holdkey = TOP.createSearchKey(fromId); // hold to kep from getting GC'd
//      WeakReference<TOP> key = new WeakReference<TOP>(holdkey);
//      i = Collections.binarySearch(id2fsw, key, new Comparator<WeakReference<TOP>>() {
//        @Override
//        public int compare(WeakReference<TOP> o1, WeakReference<TOP> o2) {
//          TOP k1 = o1.get();
//          if (k1 == null) return -1;
//          return k1.compareTo(holdkey);
//        }
//      });
//      
//      if (i < 0) {
//        i = -(i + 1); // i is (-(insertion point) - 1) 
//      }
//    }
    // in this impl, the id is the index.
    final int sz = id2fsw.size();
    for (int i = fromId; i < sz; i++) {
      Object o = id2fsw.get(i);
      if (o == null) { 
        continue;
      }
      
      if (o instanceof TOP) {
        action.accept((TOP)o);
      } else {
        TOP fs = ((WeakReference<TOP>)o).get();
        if (fs == null) {
          id2fsw.set(i, null);
          continue;
        }
        action.accept(fs);
      }
    }   
  }
}
