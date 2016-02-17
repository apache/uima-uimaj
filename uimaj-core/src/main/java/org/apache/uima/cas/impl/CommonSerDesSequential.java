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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import org.apache.uima.cas.function.Consumer_T_withIOException;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.jcas.cas.TOP;

/**
 * Common de/serialization for plain binary and compressed binary form 4
 * which both walk the cas using the sequential, incrementing id approach
 */
public class CommonSerDesSequential {

  /**
   * a map from a fs to its addr in the modeled heap
   * 
   * created during serialization and deserialization
   * used during serialization to create addr info for index info serialization
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the line.
   */
  final Obj2IntIdentityHashMap<TOP> fs2addr = new Obj2IntIdentityHashMap<>(TOP.class, TOP.singleton);

  /**
   * a map from a fs addr to the V3 FS
   * created when serializing (non-delta), deserializing (non-delta)
   *   augmented when deserializing(delta)
   * used when deserializing (delta and non-delta)
   * retained after deserializing (in case of subsequent delta (multiple) deserializations being combined) 
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the line.
   * 
   */
  final Int2ObjHashMap<TOP> addr2fs = new Int2ObjHashMap<>(TOP.class);  
  
  /**
   * This is populated from the main CAS's id to fs map, which is accessed once;
   *   Subsequent accessing of that could return different lists due to an intervening Garbage Collection.
   */
  final List<TOP> sortedFSs = new ArrayList<>();  // holds the FSs sorted by id
  final private CASImpl baseCas;
  int heapEnd;  // == the last addr + length of that

  public CommonSerDesSequential(CASImpl baseCas) {
    this.baseCas = baseCas;
  }

  /**
   * Cleanup - clear the list of sortedFSs
   */
  void clearSortedFSs() {
    sortedFSs.clear();
  }
  
  void addSortedFSs(TOP fs) {
    sortedFSs.add(fs);
  }
  
  /**
   * Must call in fs sorted order
   * @param fs
   */
  void addFS(TOP fs, int addr) {
    fs2addr.put(fs, addr);
    sortedFSs.add(fs);
    addr2fs.put(addr, fs);
  }
  
  void clear() {
    sortedFSs.clear();
    fs2addr.clear();
    addr2fs.clear();
  }
  
  void setup() {
    // local value as "final" to permit use in lambda below
    final int[] nextAddr = {1};
    clear();

    baseCas.walkReachablePlusFSsSorted(fs -> {
      fs2addr.put(fs, nextAddr[0]);
      sortedFSs.add(fs);
      addr2fs.put(nextAddr[0], fs);
      nextAddr[0] += BinaryCasSerDes.getFsSpaceReq(fs, fs._typeImpl);  
    });
    heapEnd = nextAddr[0];
  }
  

  
  void walkSeqFSs(Consumer_T_withIOException<TOP> action) throws IOException {
    for (TOP fs : sortedFSs) {
      action.accept(fs);
    }
  }
  
}
