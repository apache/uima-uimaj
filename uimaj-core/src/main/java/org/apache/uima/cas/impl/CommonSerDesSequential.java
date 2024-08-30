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

import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.jcas.cas.TOP;

// @formatter:off
/**
 * Common de/serialization for plain binary and compressed binary form 4
 * which both used to walk the cas using the sequential, incrementing id approach
 * 
 * Lifecycle:  
 *   There is 0/1 instance per CAS, representing the FSs at some point in time in that CAS.
 * 
 *   Creation:  
 *     serialization (for delta serialization, 
 *       the csds made when deserialization was done is reused, if available
 *       Updates cannot add to the reachables).
 *     non-delta deserialization
 *     delta deserialization uses previous one
 * 
 *   Reset: 
 *     CAS Reset
 *     API call (for optimization - used after all delta deserializations into a particular CAS are complete.
 * 
 *   Logical constraints:
 *     - delta de/serialization must use an existing version of this,
 *        -- set during a previous non-delta de/serialization
 *        -- or created just in time via a scan of the cas
 */
// @formatter:on
public class CommonSerDesSequential {

  public static final boolean TRACE_SETUP = false;
  /**
   * a map from a fs to its addr in the modeled heap, == v2 style addr
   * 
   * created during serialization and deserialization used during serialization to create addr info
   * for index info serialization
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the
   * line.
   */
  final Obj2IntIdentityHashMap<TOP> fs2addr = new Obj2IntIdentityHashMap<>(TOP.class,
          TOP._singleton);

  /**
   * a map from the modelled (v2 style) FS addr to the V3 FS created when serializing (non-delta),
   * deserializing (non-delta) augmented when deserializing(delta) used when deserializing (delta
   * and non-delta) retained after deserializing (in case of subsequent delta (multiple)
   * deserializations being combined)
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the
   * line.
   * 
   */
  final Int2ObjHashMap<TOP, TOP> addr2fs = new Int2ObjHashMap<>(TOP.class);

  /**
   * The FSs in this list are not necessarily sequential, but is in ascending (simulated heap)
   * order, needed for V2 compatibility of serialized forms. This is populated either during
   * deserialization, or for serialization, from indexed + reachable.
   * 
   * Before accessing this, any pending items must be merged (sorting done lazily)
   */
  private final List<TOP> sortedFSs = new ArrayList<>(); // holds the FSs sorted by id

  private final List<TOP> pending = new ArrayList<>(); // batches up FSs that need to be inserted
                                                       // into sortedFSs

  /**
   * The associated CAS
   */
  private final CASImpl baseCas;

  /**
   * The first free (available) simulated heap addr, also the last addr + length of that
   */
  private int heapEnd; // == the last addr + length of that

  public CommonSerDesSequential(CASImpl cas) {
    baseCas = cas.getBaseCAS();
  }

  public boolean isEmpty() {
    return sortedFSs.isEmpty() && pending.isEmpty();
  }

  /**
   * Must call in fs sorted order
   * 
   * @param fs
   */
  void addFS(TOP fs, int addr) {
    addFS1(fs, addr);
    sortedFSs.add(fs);
  }

  void addFS1(TOP fs, int addr) {
    fs2addr.put(fs, addr);
    addr2fs.put(addr, fs);
  }

  /**
   * For out of order calls
   * 
   * @param fs
   */
  void addFSunordered(TOP fs, int addr) {
    addFS1(fs, addr);
    pending.add(fs);
  }

  void clear() {
    sortedFSs.clear();
    fs2addr.clear();
    addr2fs.clear();
    pending.clear();
    heapEnd = 0;
  }

  // @formatter:off
  /**
   * Scan all indexed + reachable FSs, sorted, and
   *   - create two maps from those to/from the int offsets in the simulated main heap
   *   - add all the (filtered - above the mark) FSs to the sortedFSs
   *   - set the heapEnd
   * @param mark null or the mark
   * @param fromAddr often 1 but sometimes the mark next fsid
   * @return all (not filtered) FSs sorted
   */
  // @formatter:on
  List<TOP> setup(MarkerImpl mark, int fromAddr) {
    if (mark == null) {
      clear();
    }
    // local value as "final" to permit use in lambda below
    int nextAddr = fromAddr;
    if (TRACE_SETUP)
      System.out.println("Cmn serDes sequential setup called by: " + Misc.getCaller());

    List<TOP> all = new AllFSs(baseCas).getAllFSsAllViews_sofas_reachable().getAllFSsSorted();
    List<TOP> filtered = CASImpl.filterAboveMark(all, mark);
    for (TOP fs : filtered) {
      addFS1(fs, nextAddr); // doesn't update sortedFSs, that will be done below in batch
      if (TRACE_SETUP) {
        System.out.format("Cmn serDes sequential setup: add FS id: %,4d addr: %,5d  type: %s%n",
                fs._id, nextAddr, fs._getTypeImpl().getShortName());
      }
      nextAddr += BinaryCasSerDes.getFsSpaceReq(fs, fs._getTypeImpl());
    }

    sortedFSs.addAll(filtered);
    heapEnd = nextAddr;
    return all;
    // if (heapEnd == 0) {
    // System.out.println("debug");
    // }
  }

  // /**
  // * called to augment an existing csds with information on FSs added after the mark was set
  // * @param mark -
  // */
  // void setup() { setup(1, 1); }

  // void walkSeqFSs(Consumer_T_withIOException<TOP> action) throws IOException {
  // for (TOP fs : sortedFSs) {
  // action.accept(fs);
  // }
  // }
  //
  /**
   * @return sorted FSs above mark if mark set, otherwise all, sorted
   */
  List<TOP> getSortedFSs() {
    if (!pending.isEmpty()) {
      merge();
    }
    return sortedFSs;
  }

  int getHeapEnd() {
    return heapEnd;
  }

  void setHeapEnd(int heapEnd) {
    this.heapEnd = heapEnd;
  }

  private void merge() {
    pending.sort(FeatureStructureImplC::compare);
    sortedFSs.addAll(pending);
    pending.clear();
    sortedFSs.sort(FeatureStructureImplC::compare);
  }

}
