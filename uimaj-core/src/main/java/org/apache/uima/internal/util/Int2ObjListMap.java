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

import java.util.ArrayList;

/**
 * A map&lt;int, T&gt;
 * 
 * based on ArrayList
 * 
 * This impl is for use in a single thread case only
 * 
 * Implements Map - like interface: keys are ints &ge; 0
 * 
 * values can be anything, but null is the value returned by get if not found so values probably
 * should not be null
 * 
 * remove not currently supported
 * 
 */
public class Int2ObjListMap<T> {

  private final ArrayList<T> values;

  public Int2ObjListMap() {
    values = new ArrayList<>();
  }

  public Int2ObjListMap(int initialSize) {
    values = new ArrayList<>(initialSize);
  }

  public void clear() {
    values.clear();
  }

  public T get(int key) {
    return (key < 0 || key >= values.size()) ? null : values.get(key);
  }

  public T put(int key, T value) {
    T prev = get(key);
    Misc.setWithExpand(values, key, value);
    return prev;
  }

}
