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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasHashMap;
import org.apache.uima.util.IteratorNvc;

// @formatter:off
/**
 * A map from ints representing FS id's (or "addresses") to those FSs There is one map instance per
 * CAS (all views).
 * 
 * The map is not populated, normally.  It is only populated when there is a need to
 *   be able to map from the id to the FS, or to prevent the FS from being GC'd
 *   -- The low level CAS apis for creating FSs have this need, because they return the id, and this
 *      won't prevent the FS from being GC'd.  
 * 
 * Removes not supported; they happen when the map is reset / cleared This corresponds to the v2
 * property of "once created, a FS cannot be reclaimed (until reset)"
 * 
 * Threading: to support read-only views, concurrent with updates, needs to be thread safe
 */
// @formatter:on
public class Id2FS implements Iterable<TOP> {

  static final boolean MEASURE = false;
  private static final int MEASURE_STACK_SIZE = 10;
  private static Map<MeasureCaller, MeasureCaller> callers = MEASURE ? new HashMap<>() : null;
  private static Map<MeasureCaller, MeasureCaller> walkableCallers = MEASURE ? new HashMap<>()
          : null;

  private static final String REPORT_FS_PINNING = "uima.report.fs.pinning";
  private static final boolean IS_REPORT_PINNING;
  private static int pinning_count; // ignoring multithreading issues, not critical
  static {
    String s = // ""; // debug
            System.getProperty(REPORT_FS_PINNING);
    IS_REPORT_PINNING = (s != null);
    if (IS_REPORT_PINNING) {
      pinning_count = (s.length() == 0) ? 10 : Integer.parseInt(s);
    }
  }

  // /**
  // * Set this JVM property to true for backwards compatibility, where an application retains
  // * some references to Feature Structures held only using the low-level references (which are
  // ints)..
  // */
  // public static final String DISABLE_FS_GC = "uima.disable_feature_structure_garbage_collection";
  //
  // public static final boolean IS_DISABLE_FS_GC = // true || // disabled due to performance
  // Misc.getNoValueSystemProperty(DISABLE_FS_GC);

  final private JCasHashMap id2fs;
  final private int initialSize;

  public Id2FS(int initialHeapSize) {
    this.initialSize = Math.max(32, initialHeapSize >> 4); // won't shrink below this
    id2fs = new JCasHashMap(initialSize);
  }

  private void maybeReport() {
    if (!IS_REPORT_PINNING) {
      return;
    }
    pinning_count--;
    if (pinning_count < 0) {
      return;
    }
    System.out.println("UIMA Report: FS pinning " + pinning_count + " occuring here:");
    new Throwable().printStackTrace(System.out);
  }

  /** put but assert wasn't there before */
  void put(int id, TOP fs) {
    TOP prev = id2fs.put(id, fs);
    assert prev == null;
    maybeReport();
  }

  /**
   * Skips the assert that the item wasn't already present
   * 
   * @param fs
   *          the fs to add
   */
  void putUnconditionally(TOP fs) {
    id2fs.put(fs._id, fs);
    maybeReport();
  }

  /**
   * make an id map to an fs, asserting there was a previous mapping for this id
   * 
   * @param id
   *          -
   * @param fs
   *          -
   */
  void putChange(int id, TOP fs) {
    TOP prev = id2fs.put(id, fs);
    assert prev != null; // changing a preexisting value
    maybeReport();
  }

  void put(TOP fs) {
    put(fs._id, fs);
  }

  TOP get(int id) {
    return id2fs.get(id);
  }

  // /**
  // * @param fs -
  // */
  // public void add(TOP fs) {
  // id2fsw.add(
  // null // experiment - hangs
  //// IS_DISABLE_FS_GC
  //// ? fs
  //// : new WeakReference<TOP>(fs)
  // );
  // maxSize ++; // tracked for computing shrinking upon clear() call
  // }

  // public void setStrongRef(TOP fs, int i) {
  // id2fsw.set(i, fs);
  // }

  // public void replaceWithStrongRef(TOP fs) {
  // if (IS_DISABLE_FS_GC) {
  // return;
  // }
  // id2fsw.set(fs._id, fs);
  // }

  int size() {
    return id2fs.getApproximateSize();
  }

  /**
   * adjusts the underlying array down in size if grew beyond the reset heap size value
   */
  void clear() {
    id2fs.clear();
    // disabled for now
    // use common routine in Misc if re-enabling

    // secondTimeShrinkable = Misc.maybeShrink(
    // secondTimeShrinkable, id2fsw.size(), Misc.nextHigherPowerOf2(maxSize), 2, initialSize,
    // newCapacity -> {
    // id2fsw = new ArrayList<>(newCapacity);
    // },
    // () -> {
    // id2fsw.clear();
    // });
    // id2fsw.add(null); // so that ids start at 1

    // if (id2fsw.size() > (CASImpl.DEFAULT_RESET_HEAP_SIZE >> 4)) {
    // id2fsw.clear();
    // id2fsw.add(null); // so that ids start at 1
    // id2fsw.trimToSize();
    // id2fsw.ensureCapacity(CASImpl.DEFAULT_INITIAL_HEAP_SIZE >> 4);
    // } else {
    // id2fsw.clear();
    // id2fsw.add(null); // so that ids start at 1
    // }
  }

  // /**
  // * plus means all reachable, plus maybe others not reachable but not yet gc'd
  // * @param action
  // */
  // void walkReachablePlusFSsSorted(Consumer<TOP> action) {
  // walkReachablePlueFSsSorted(action, 1);
  // }

  // /**
  // * walk a part of the id2fsw list; for delta, just the part above the line
  // * @param action
  // * @param items the part of the id2fsw list to walk
  // */
  // void walkReachablePlueFSsSorted(Consumer<TOP> action, int fromId) {
  //
  //// int i;
  //// if (fromId == 1) {
  //// i = fromId;
  //// } else {
  //// TOP holdkey = TOP.createSearchKey(fromId); // hold to kep from getting GC'd
  //// WeakReference<TOP> key = new WeakReference<TOP>(holdkey);
  //// i = Collections.binarySearch(id2fsw, key, new Comparator<WeakReference<TOP>>() {
  //// @Override
  //// public int compare(WeakReference<TOP> o1, WeakReference<TOP> o2) {
  //// TOP k1 = o1.get();
  //// if (k1 == null) return -1;
  //// return k1.compareTo(holdkey);
  //// }
  //// });
  ////
  //// if (i < 0) {
  //// i = -(i + 1); // i is (-(insertion point) - 1)
  //// }
  //// }
  // // in this impl, the id is the index.
  // if (MEASURE) {
  // trace(walkableCallers);
  // }
  //
  // final int sz = id2fs.size();
  // for (int i = fromId; i < sz; i++) {
  // Object o = id2fs.get(i);
  // if (o == null) {
  // continue;
  // }
  //
  // if (o instanceof TOP) {
  // action.accept((TOP)o);
  // } else {
  // TOP fs = ((WeakReference<TOP>)o).get();
  // if (fs == null) {
  //// id2fs.set(i, null);
  // continue;
  // }
  // action.accept(fs);
  // }
  // }
  // }

  void traceWeakGets() {
    trace(callers);
  }

  void trace(Map<MeasureCaller, MeasureCaller> map) {
    synchronized (map) {
      StackTraceElement[] e = Thread.currentThread().getStackTrace();
      MeasureCaller k = new MeasureCaller();
      for (int i = 3, j = 0; i < e.length; i++, j++) {
        if (j >= MEASURE_STACK_SIZE) {
          break;
        }
        k.className[j] = e[i].getClassName();
        k.methodName[j] = e[i].getMethodName();
        k.lineNumber[j] = e[i].getLineNumber();
      }
      MeasureCaller prev = map.putIfAbsent(k, k);
      if (null != prev) {
        prev.count++;
      }
    }
  }

  private static class MeasureCaller {
    int count = 1;
    String[] className = new String[MEASURE_STACK_SIZE];
    String[] methodName = new String[MEASURE_STACK_SIZE];
    int[] lineNumber = new int[MEASURE_STACK_SIZE];

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(className);
      result = prime * result + Arrays.hashCode(lineNumber);
      result = prime * result + Arrays.hashCode(methodName);
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof MeasureCaller)) {
        return false;
      }
      MeasureCaller other = (MeasureCaller) obj;
      if (!Arrays.equals(className, other.className)) {
        return false;
      }
      if (!Arrays.equals(lineNumber, other.lineNumber)) {
        return false;
      }
      if (!Arrays.equals(methodName, other.methodName)) {
        return false;
      }
      return true;
    }
  }

  private static void dumpCallers(String title, Map<MeasureCaller, MeasureCaller> map) {
    System.out.println(title + ": size:" + map.size());
    MeasureCaller[] a = map.keySet().toArray(new MeasureCaller[map.size()]);
    Arrays.sort(a, (c1, c2) -> -Integer.compare(c1.count, c2.count));

    for (MeasureCaller c : a) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < MEASURE_STACK_SIZE; i++) {
        if (c.className[i] == null) {
          break;
        }
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(Misc.formatcaller(c.className[i], c.methodName[i], c.lineNumber[i]));
      }

      System.out.format("count: %,d, %s%n", c.count, sb);
    }
  }

  static {
    if (MEASURE) {
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        dumpCallers("Callers of getId with weak ref", callers);
        dumpCallers("Callers of walkReachablePlueFSsSorted", walkableCallers);
      }, "Dump id2fs weak"));
    }
  }

  @Override
  public IteratorNvc<TOP> iterator() {
    return id2fs.iterator();
  }

}
