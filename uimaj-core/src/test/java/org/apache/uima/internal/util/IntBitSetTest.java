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
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;


public class IntBitSetTest extends TestCase {
  
  IntBitSet ibs;
  IntBitSet ibs1k;
  
  public void setUp() {
    ibs = new IntBitSet();
    ibs1k = new IntBitSet(63, 1000);
  }

  public void testBasic() {
    
    ibs.add(15);
    ibs.add(188);
    
    IntListIterator it = ibs.iterator();    
    assertTrue(it.hasNext());
    assertEquals(15, it.next());
    assertTrue(it.hasNext());
    assertEquals(188, it.next());
    assertFalse(it.hasNext());
    assertEquals(3*64, ibs.getSpaceUsed_in_bits_no_overhead());
    assertEquals(6, ibs.getSpaceUsed_in_words_no_overhead());
    
    ibs = ibs1k;
    
    ibs.add(1015);
    ibs.add(1188);
    it = ibs.iterator();    
    assertTrue(it.hasNext());
    assertEquals(1015, it.next());
    assertTrue(it.hasNext());
    assertEquals(1188, it.next());
    assertFalse(it.hasNext());
    assertEquals(2,ibs.size());
    
    assertEquals(3*64, ibs.getSpaceUsed_in_bits_no_overhead());
    assertEquals(6, ibs.getSpaceUsed_in_words_no_overhead());
    
    ibs = new IntBitSet(64, 1000);
    ibs.add(1064);
    assertEquals(1,ibs.size());
    assertEquals(2*64, ibs.getSpaceUsed_in_bits_no_overhead());
    
    ibs = new IntBitSet(64, 1000);

    ibs.add(1063);
    assertEquals(1*64, ibs.getSpaceUsed_in_bits_no_overhead());
    assertEquals(1,ibs.size());

    ibs = new IntBitSet(6 * 64, 1000);

    ibs.add(1000 + 6 * 64 - 1);
    assertEquals(6*64, ibs.getSpaceUsed_in_bits_no_overhead());
    ibs.add(1000 + 6 * 64);
    assertEquals(2,ibs.size());
    assertEquals(12*64, ibs.getSpaceUsed_in_bits_no_overhead());
    
  }
  
  public void testoffset() {
    ibs = new IntBitSet(64,1000);
    ibs.add(1064);
    IntListIterator it = ibs.iterator();
    List<Integer> ints = new ArrayList<Integer>();
    while (it.hasNext()) {
      ints.add(it.next());
    }
    assertTrue(Arrays.equals(ints.toArray(), new Object[]{Integer.valueOf(1064)}));
    
    ibs.add(1063);
    it = ibs.iterator();
    ints.clear();
    while (it.hasNext()) {
      ints.add(it.next());
    }
    assertTrue(Arrays.equals(ints.toArray(), new Object[]{1063, 1064}));
    
    PositiveIntSet_impl posIntSet = new PositiveIntSet_impl();
    posIntSet.add(10);
    posIntSet.add(1);
    it = posIntSet.iterator();
    it.moveToStart();
    ints.clear();
    while (it.hasNext()) {
      ints.add(it.next());
    }
    assertTrue(Arrays.equals(ints.toArray(), new Object[]{1, 10}));
  }
  
  public void testRemove() {
    ibs.add(15);
    ibs.add(188);
    ibs.add(101);
    ibs.remove(188);
    assertEquals(101, ibs.getLargestMenber());
    assertEquals(2,ibs.size());
    IntListIterator it = ibs.iterator();    
    assertTrue(it.hasNext());
    assertEquals(15, it.next());
    assertTrue(it.hasNext());
    assertEquals(101, it.next());
    assertFalse(it.hasNext());
    assertEquals(3*64, ibs.getSpaceUsed_in_bits_no_overhead());
    
  }
  
  public void testContains() {
    ibs = new IntBitSet(63, 1000);
    
    ibs.add(1015);
    ibs.add(1188);
    assertTrue(ibs.contains(1015));
    assertFalse(ibs.contains(1187));
    assertFalse(ibs.contains(1189));
    assertTrue(ibs.contains(1188));
    assertEquals(3*64, ibs.getSpaceUsed_in_bits_no_overhead());
    assertEquals(6, ibs.getSpaceUsed_in_words_no_overhead());
    assertEquals(2,ibs.size());
    
  }
  
  public void testIterator() {
    ibs = new IntBitSet(63, 1000);
    for (int i = 0; i < 10; i = i + 4) {
      ibs.add(1000 + i);
    }
    
    IntListIterator it = ibs.iterator();
    for (int i = 0; i < 10; i += 4) {
      assertTrue(it.hasNext());
      assertEquals(1000 + i, it.next());        
    }
    assertFalse(it.hasNext());
  }
}
