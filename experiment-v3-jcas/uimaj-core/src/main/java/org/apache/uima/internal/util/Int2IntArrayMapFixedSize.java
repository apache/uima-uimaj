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

import java.util.Arrays;

/**
 * A map&lt;int, int&gt;
 * 
 * based on having a key and value int array, where the keys are sorted 
 * 
 * Supports sharing a single key array with multiple value arrays
 * 
 * Implements Map - like interface:
 *   keys and values are ints
 *   
 * values can be anything except 0; 0 is the value returned by get if not found
 * 
 * All adds must occur before any gets; then a sort must be called unless the adds are in sort order
 *
 * Threading: instances of this class may be accessed on multiple threads 
 *   (different iterators may be on different threads)
 */
public class Int2IntArrayMapFixedSize {
  
  private final int [] values;
  private final boolean isSmall;
      
  public Int2IntArrayMapFixedSize(int length) {
    values = new int[length];
    isSmall = length <= 4;
  }
    
  public int get(int key, final int[] sortedKeys) {    
    int f = isSmall ? quickFind(key, sortedKeys) : Arrays.binarySearch(sortedKeys, key);
    if (f < 0) {
      throw new RuntimeException(); // return 0; // not found
    }
    return values[f];
  }
  
  private int quickFind(int key, final int[] sortedKeys) {
    for (int i = 0; i < sortedKeys.length; i++) {
      if (key == sortedKeys[i]) {
        return i;
      }
    }
    throw new RuntimeException();  // not found
  }
  
  public int getAtIndex(int index) {
    return values[index];
  }
   
  public void putAtIndex(int index, int value) {
    values[index] = value;
  }    
}
