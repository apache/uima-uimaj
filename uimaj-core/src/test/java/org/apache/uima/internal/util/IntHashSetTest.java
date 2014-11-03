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

import junit.framework.TestCase;


public class IntHashSetTest extends TestCase {
  
  IntHashSet ihs;
  
  public void setUp() {
    ihs = new IntHashSet();
  }

  public void testBasic() {
    
    ihs.add(15);
    ihs.add(188);
    int[] sv = getSortedValues(ihs);
    assertEquals(2, sv.length);
    assertEquals(15, sv[0]);
    assertEquals(188, sv[1]);
    assertEquals(15, ihs.getMostNegative());
    assertEquals(188, ihs.getMostPositive());
    
    // test most positive / negative
    ihs.clear();
    ihs.add(189);
    assertEquals(189, ihs.getMostNegative());
    assertEquals(189, ihs.getMostPositive());
    ihs.add(1000);
    ihs.add(-1000);
    assertEquals(1000, ihs.getMostPositive());
    assertEquals(-1000, ihs.getMostNegative());
    ihs.add(500);
    ihs.add(-500);
    assertEquals(1000, ihs.getMostPositive());
    assertEquals(-1000, ihs.getMostNegative());
    ihs.remove(1000);
    assertEquals(999, ihs.getMostPositive());
    assertEquals(-1000, ihs.getMostNegative());
    ihs.add(1001);    
    assertEquals(1001, ihs.getMostPositive());
  }
  
  private int[] getSortedValues(IntHashSet s) {
    IntListIterator it = s.iterator();
    int[] r = new int[s.size()];
    int i = 0;
    while (it.hasNext()) {
      r[i++] = it.next();
    }
    Arrays.sort(r);
    return r;
  }
  
  public void testContains() {
    ihs.add(1188);
    ihs.add(1040);
    assertTrue(ihs.contains(1188));
    assertTrue(ihs.contains(1040));
    assertFalse(ihs.contains(0));
    assertFalse(ihs.contains(99));  
  }
  
  public void testTableSpace() {
    assertEquals(32, IntHashSet.tableSpace(19, 0.6f));    // 19 / .6 = 31.xxx, round to 32
    assertEquals(64, IntHashSet.tableSpace(21, 0.6f));
    assertEquals(32, ihs.tableSpace(21));
  }
  
  public void testWontExpand() {
    ihs = new IntHashSet(21);
    assertEquals(16, ihs.getSpaceUsedInWords());
    assertTrue(ihs.wontExpand(20));
    assertFalse(ihs.wontExpand(21));
  }
  
  
  
 
  
}
