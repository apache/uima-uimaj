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

package org.apache.uima.internal.util;

import java.util.HashMap;

/**
 * Straightforward, many-to-one map from Strings to ints, based on a Java
 * {@link java.util.HashMap HashMap}.
 * 
 * 
 */
public class StringToIntMap {

  private static final int DEFAULT_VALUE = 0;

  private HashMap<String, Integer> map;

  /**
   * Constructor.
   * 
   * @see java.lang.Object#Object()
   */
  public StringToIntMap() {
    super();
    this.map = new HashMap<String, Integer>();
  }

  /**
   * Check if the the argument string is defined as a key in this map.
   * 
   * @see java.util.Map#containsKey(java.lang.Object)
   * @param key
   *          The string to be looked up.
   * @return <code>true</code> if a value is defined for this string; <code>false</code> else.
   */
  public boolean containsKey(String key) {
    return this.map.containsKey(key);
  }

  /**
   * Get the value for the key.
   * 
   * @param key
   *          The string to be looked up.
   * @return The int value for <code>key</code>, or <code>0</code> if <code>key</code> is not
   *         a key in the map. Use {@link #containsKey(String) containsKey()} to find out if
   *         <code>key</code> is actually defined in the map.
   */
  public int get(String key) {
    Integer i = this.map.get(key);
    if (i == null) {
      return DEFAULT_VALUE;
    }
    return i.intValue();
  }

  /**
   * Add a key-value pair to the map.
   * 
   * @param key
   *          The string key.
   * @param value
   *          The int value.
   * @return The previous value of <code>key</code>, if it was set. <code>0</code> else.
   */
  public int put(String key, int value) {
    Integer i = this.map.get(key);
    int rc;
    if (i == null) {
      rc = 0;
    } else {
      rc = i.intValue();
    }
    i = Integer.valueOf(value);
    this.map.put(key, i);
    return rc;
  }

}
