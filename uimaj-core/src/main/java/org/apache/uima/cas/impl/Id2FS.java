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
import java.util.stream.Collectors;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.util.Misc;

/**
 * A map from ints representing FS id's (or "addresses") to those FSs
 * There is one map instance per CAS (all views).
 * 
 * The values are weak references, to allow gc
 * 
 * New additions always have increasing int keys.
 * 
 * Removes are done using the weak queue mechanism.
 * 
 * Multiple removes adjacent are compacted to save space in the table.
 * 
 * Searching is by a modified binary search
 *   - a quick start by assuming no removes, or no compacted removes.
 *   - a full binary search otherwise, taking into account markings for 
 *     compacted removes.
 */
public class Id2FS {
  
  private static final String DISABLE_FEATURE_STRUCTURE_GARBAGE_COLLECTION = "uima.disable_feature_structure_garbage_collection";  
 
  static int cleanupThreshold = 10;  // visible and changable for testing

  boolean is_gc = !Misc.getNoValueSystemProperty(DISABLE_FEATURE_STRUCTURE_GARBAGE_COLLECTION);
  private int monitorForCleanup; 

  private final ArrayList<FeatureStructureImplC> id2fsp;
  private       ArrayList<WeakReference<FeatureStructureImplC>> id2fsw;
  
  public Id2FS(boolean is_gc) {  // for testing
    this.is_gc = is_gc;
    if (is_gc) {
      id2fsp = null;
      id2fsw = new ArrayList<>();
    } else {
      id2fsp = new ArrayList<>();
      id2fsp.add(null);
      id2fsw = null;
    }
  }

  public Id2FS() {  
    if (is_gc) {
      id2fsp = null;
      id2fsw = new ArrayList<>();
    } else {
      id2fsp = new ArrayList<>();
      id2fsp.add(null);
      id2fsw = null;
    }
  }

  
  /**
   * @param fs -
   */
  public void add(FeatureStructureImplC fs) {
    if (is_gc) {
      id2fsw.add(new WeakReference<FeatureStructureImplC>(fs));
    } else {
      id2fsp.add(fs);
    }
  }
 
  public <T extends FeatureStructure> T get(int id) {
    if (is_gc) {
      monitorForCleanup = 0;
      T v = (T) binarySearch(id);
//      System.out.println("debug id: " + id + ", monitor4cu: " + monitorForCleanup);
      if (v == null) {
        throw new CASRuntimeException(CASRuntimeException.CAS_MISSING_FS, id);
      }
      
      if (monitorForCleanup > 4) { // tuning parameter
        id2fsw = cleanup(id2fsw);
      }
      
      return v;
    } else {
      
      // non gc version
      if (id < 1 || id >= id2fsp.size()) {
        throw new CASRuntimeException(CASRuntimeException.INVALID_FS_ID, id);
      }
      return (T) id2fsp.get(id);
    }
  }
  
  // binary search, between weak refs <FeatureStructureImplC> and FeatureStructureImplC
  // gc'd values are skipped
  private final FeatureStructureImplC binarySearch(int id) {
    int start = 0;
    int end = id2fsw.size() - 1;
     
      
    int i; // Current position 
    int comp; // Compare value 
    int skipnull = 0;
  outer:
    while (start <= end) { 
      i = (start + end) >>> 1;  // this form works when start + end overflows
      
      WeakReference<FeatureStructureImplC> wr = id2fsw.get(i);
      FeatureStructureImplC v = wr.get();
      
      if (v == null) {
        monitorForCleanup ++;
        
        /** find valid position for i where the weakref has a value, 
         *    or if none, break
         *  search in both directions, alternating for best locality of ref
         */
        boolean hitUpperBndry = false;
        boolean hitLowerBndry = false;
        int delta = 1;
        while (true) {
          
          // check bounds
          if (delta > 0 && (i + delta > end)) { // going forward - check if past end
            hitUpperBndry = true;
            if (hitLowerBndry) {
              break outer;  // not found in both directions, break out of outer
            }
            delta = - delta;  // going backwards next
            continue;
          } else if (delta < 0 && (i + delta < start)) { // going backwards, check if before start
            hitLowerBndry = true;
            if (hitUpperBndry) {
              break outer;  // not found in both directions, break out of outer
            }
            delta = (- delta) + 1; // increment when switching from backwards to forwards
            continue;
          }
          
          // bounds OK, check value
          v = id2fsw.get(i + delta).get();
          if (v != null) {
            // return to outer loop with valid i and v
            i = i + delta;
            break;
          }
          
          // this new position also is empty, continue looking
          monitorForCleanup ++;
          delta = (delta > 0) 
                  ? ( (!hitLowerBndry) ?  ( - delta)      : delta + 1)
                  : ( (!hitUpperBndry) ? (( - delta) + 1) : delta - 1);
        }
      }
      
      if (v._id == id) {
        return v;  // found it
      }
     
      if (v._id > id) {
        end = i - 1;
      } else {
        start = i + 1;
      }
    } //This means that the input span is empty.
     
    return null; 
  }
  
  private ArrayList<WeakReference<FeatureStructureImplC>> cleanup(ArrayList<WeakReference<FeatureStructureImplC>> wrl) {
    return wrl.stream()
              .filter(wr -> wr.get() != null)
              .collect(Collectors.toCollection(ArrayList::new));  
  }
  
  int size() {
    return is_gc ? -1 : id2fsp.size(); 
  }
}
