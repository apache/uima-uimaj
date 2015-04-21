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

import junit.framework.TestCase;


public class CommonAuxHeapTest extends TestCase {

  private int capacity;
  private int multLimit;
  private int minSize;
  private final int[] shrinkableCount = new int[1];

  public void testcomputeShrunkArraySize() {
    CommonAuxHeap cah = new ByteHeap();
    
    multLimit = 1000;
    capacity = 1000;
    minSize = 10;
    
    tst(1000, 1000);        
    boolean ok = false;
    try {
      tst(1000, 10000);  // should throw
    } catch (IllegalArgumentException e) {
      ok = true;
    }
    assertTrue(ok);
        
    tst(1000, 999);    // size needs capacity - no shrink
    repeatedNoShrink(1000, 999);
    
    repeatedNoShrink(1000,  500);
    
    repeatedShrink(500,  499);
    multLimit = 999;
    
    repeatedNoShrink(1000, 999);
    repeatedNoShrink(1000, 500);
    repeatedShrink(500, 499);
    
    multLimit = 500;
    repeatedNoShrink(1000, 999);
    repeatedNoShrink(1000, 500);
    repeatedShrink(500, 499);
    
    multLimit = 300;
    repeatedNoShrink(1000, 999);
    repeatedNoShrink(1000, 700);
    repeatedShrink(700, 699);
    repeatedShrink(700, 400);
    repeatedShrink(700, 399);
    repeatedShrink2(700, 400, 399);
   
  }
  
  private void tst(int expected, int size) {
    assertEquals(expected,  CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
  }
  
  private void repeatedNoShrink(int expected, int size) {
    assertEquals(expected,  CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
    assertEquals(0, shrinkableCount[0]);
    assertEquals(expected,  CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
    assertEquals(expected,  CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
    assertEquals(0, shrinkableCount[0]);
  }
  
  private void repeatedShrink(int expected, int size) {
    // see if shrinkable gets reset to 0
    CommonAuxHeap.computeShrunkArraySize(capacity, capacity-1, 2, multLimit, minSize, shrinkableCount);
    assertEquals(0, shrinkableCount[0]);
   
    for (int i = 0; i < 20; i++) {
      assertEquals(capacity,  CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
    }
    assertEquals(20, shrinkableCount[0]);
    assertEquals(expected, CommonAuxHeap.computeShrunkArraySize(capacity, size, 2, multLimit, minSize, shrinkableCount));
  }
  
  private void repeatedShrink2(int expected1, int expected2, int size) {
    repeatedShrink(expected1, size);
    for (int i = 0; i < 4; i++) {
      CommonAuxHeap.computeShrunkArraySize(expected1,  size, 2, multLimit, minSize, shrinkableCount);
    }
    assertEquals(expected2, CommonAuxHeap.computeShrunkArraySize(expected1,  size, 2, multLimit, minSize, shrinkableCount));
  }
}
