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
 * Like string heap, but keeps strings in a hashmap (for quick testing) and an array list. This is
 * used to emulate how v2 keeps strings, to support backwards compatibility for low-level access
 * 
 */
final class StringSet {

  private int lastStringCode = 0;
  final private ArrayList<String> strings = new ArrayList<>();
  {
    strings.add(null);
  }
  final private HashMap<String, Integer> string2int = new HashMap<>();

  // Reset the string heap (called on CAS reset).
  void reset() {
    strings.clear();
    strings.add(null);
    string2int.clear();
    lastStringCode = 0;
  }

  // Get a string value
  String getStringForCode(int stringCode) {
    if (stringCode == LowLevelCAS.NULL_FS_REF) {
      return null;
    }
    return strings.get(stringCode);
  }

  /**
   * get the code for a string, adding it to the string table if not already there.
   * 
   * @param s
   *          The string.
   * @return The code corresponding to the string, which can be used in the getStringForCode call
   *         above
   */
  int getCodeForString(String s) {
    if (s == null) {
      return LowLevelCAS.NULL_FS_REF;
    }

    Integer prev = string2int.putIfAbsent(s, lastStringCode + 1);
    if (prev == null) {
      strings.add(s);
      return ++lastStringCode;
    }

    return prev;
  }

  int getSize() {
    return strings.size();
  }

}
