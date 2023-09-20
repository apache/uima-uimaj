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
import java.util.HashMap;

/**
 * Sets of long values, used to support ll_set/getIntValue that manipulate v2 style long data
 * 
 */
final class LongSet {

  private int lastLongCode = 0;
  final private ArrayList<Long> longs = new ArrayList<>();
  {
    longs.add(null);
  }
  final private HashMap<Long, Integer> long2int = new HashMap<>();

  // Reset the long heap (called on CAS reset).
  void reset() {
    longs.clear();
    longs.add(null);
    long2int.clear();
    lastLongCode = 0;
  }

  // Get a long value
  Long getLongForCode(int longCode) {
    if (longCode == LowLevelCAS.NULL_FS_REF) {
      return null;
    }
    return longs.get(longCode);
  }

  /**
   * get the code for a long, adding it to the long table if not already there.
   * 
   * @param s
   *          The long.
   * @return The code corresponding to the long, which can be used in the getLongForCode call above
   */
  int getCodeForLong(Long s) {
    if (s == null) {
      return LowLevelCAS.NULL_FS_REF;
    }

    Integer prev = long2int.putIfAbsent(s, lastLongCode + 1);
    if (prev == null) {
      longs.add(s);
      return ++lastLongCode;
    }

    return prev;
  }

  int getSize() {
    return longs.size();
  }

}
