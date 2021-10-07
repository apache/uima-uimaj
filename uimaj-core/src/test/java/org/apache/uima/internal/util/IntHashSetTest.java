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
import java.util.Random;

import junit.framework.TestCase;


public class IntHashSetTest extends TestCase {
  
  IntHashSet ihs;
  
  Random random;
  
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
    sv = getSortedValues(ihs);
    assertTrue(Arrays.equals(sv, new int[]{-1000, -500, 189, 500, 1001}));
  }
  
  public void testSwitching224() {
    final int OS = 100000;
    ihs = new IntHashSet(16, OS);
    ihs.add(OS - 1);
    ihs.add(OS);
    ihs.add(OS + 1);
    int[] sv = getSortedValues(ihs);
    assertTrue(Arrays.equals(sv, new int[]{99999, 100000, 100001 }));
    ihs.add(OS - 32767);
    sv = getSortedValues(ihs);
    assertTrue(Arrays.equals(sv, new int[]{OS - 32767, 99999, 100000, 100001}));
    ihs.add(OS - 32768);
    sv = getSortedValues(ihs);
    assertTrue(Arrays.equals(sv, new int[]{OS - 32768, OS - 32767, 99999, 100000, 100001}));
    
  }
  
  private int[] getSortedValues(IntHashSet s) {
    IntListIterator it = s.iterator();
    int[] r = new int[s.size()];
    int i = 0;
    while (it.hasNext()) {
      r[i++] = it.nextNvc();
    }
    Arrays.sort(r);
    return r;
  }
  
  public void testContains() {
    ihs.add(1188);
    ihs.add(1040);
    assertTrue(ihs.contains(1188));
    assertTrue(ihs.contains(1040));
    assertFalse(ihs.contains(1));
    assertFalse(ihs.contains(99));  
  }
  
//  public void testTableSpace() {
//    assertEquals(32, IntHashSet.tableSpace(19, 0.6f));    // 19 / .6 = 31.xxx, round to 32
//    assertEquals(64, IntHashSet.tableSpace(21, 0.6f));
//    assertEquals(32, ihs.tableSpace(21));
//  }
  
  public void testWontExpand() {
    ihs = new IntHashSet(21);
    assertEquals(16, ihs.getSpaceUsedInWords());
    assertTrue(ihs.wontExpand(20));
    assertFalse(ihs.wontExpand(21));
  }
  
  public void testExpandNpe() {
    ihs.add(15);
    ihs.add(150000);  // makes 4 byte table entries
    
    for (int i = 1; i < 256; i++) {  // 0 is invalid key
      ihs.add(i);  // causes resize, check no NPE etc thrown.
    }
  }
  
  public void testAddIntoRemovedSlot() {
    long seed = // 6738591171221169418L;
        new Random().nextLong();
    System.out.println("Random seed for testAddIntoRemovedSlot in " + this.getClass().getName() + ": "  + seed);
    random = new Random(seed);

    for (int i = 1; i < 100; i++) {
      ihs.add(i);
      assertEquals(i, ihs.size());
    }
    
    assertEquals(99, ihs.size());
    
    /** Test with 2 byte numbers */
    checkRemovedReuse(true);
    
    ihs = new IntHashSet();
    for (int i = 1; i < 99; i++) {
      ihs.add(i);
    }
    ihs.add(100000);  // force 4 byte
    checkRemovedReuse(false);
  }
  
  private void checkRemovedReuse(boolean is2) {
    assertTrue(ihs.getSpaceUsedInWords() == ((is2) ? 128 : 256));
    for (int i = 0; i < 100000; i++) {
      int v = 1 + random.nextInt(100 + (i % 30000)); // random between 1 and 30,101
      int sz = ihs.size();
      boolean wasRemoved = ihs.remove(v);
      assertEquals(sz - (wasRemoved ? 1 : 0), ihs.size());
      assertTrue(!(ihs.contains(v)));
      v = 1 + random.nextInt(100 + (i % 30000));
      sz = ihs.size();
      boolean wasAdded = ihs.add(v);
      assertEquals(sz + (wasAdded ? 1 : 0), ihs.size());
      assertTrue(ihs.contains(v));
    }
    assertTrue(ihs.getSpaceUsedInWords() == ((is2) ? 16384 : 32768) );
    
    //  32,768, 16,384, 8,192, 4096, 2048, 1024, 512, 256
    // for 2 byte storage, is2 = true, and expected is: i / 2
    // for 4 byte storage, is2 = false, and expected is i
    
    ihs.clear(); // doesn't set 2nd time because size + removed > 1/2 the capacity
    
    for (int i = 32768; i > 128; i = i / 2) {
      ihs.clear(); // sets 2nd time shrinkable
      assertTrue(ihs.getSpaceUsedInWords() == (is2 ? i/2 : i));
      ihs.clear();  // shrinks
      assertTrue(ihs.getSpaceUsedInWords() == (is2 ? i/4 : i/2));
    }
//    ihs.clear();
//    
    assertTrue(ihs.getSpaceUsedInWords() == (is2 ? 64: 128));

    // table size should be 128, adding 100 items should cause expansion (84 == .66 * 128)
    for (int i = 1; i < ((is2) ? 100 : 99); i++) {
      ihs.add(i);
    }
    if (!is2) {
      ihs.add(100000);
    }
    
    assertTrue(ihs.getSpaceUsedInWords() == ((is2) ? 128 : 256));
    for (int i = 0; i < 1000; i++) {
      int v = 1 + random.nextInt(100);
      ihs.remove(v);
      assertTrue(!(ihs.contains(v)));
      ihs.add(v);
      assertTrue(ihs.contains(v));
    }
    
    assertTrue(ihs.getSpaceUsedInWords() == ((is2) ? 128 : 256));
    
  }
 
  public void testRandom() {
    int countAdd = 0;
    int dupsA = 0;
    int notPres = 0;
    int countRmv = 0;
    
    long seed = 
        new Random().nextLong();
    System.out.println("Random seed for testRandom in " + this.getClass().getName() + ": "  + seed);
    random = new Random(seed);
    
    for (int i = 1; i < 1024 * 1024; i++) {
      int k = i & (1024 * 256) - 1;
      if (k == 0) continue;
      if (random.nextInt(3) > 0) {
        int sz = ihs.size();
        if (ihs.add(k)) {
          countAdd ++;
          assertEquals(sz + 1, ihs.size());
        } else {
          dupsA ++;
        }
        
      } else {
        int sz = ihs.size();
        if (ihs.remove(k)) {
          countRmv ++;
          assertEquals(sz - 1, ihs.size());
        } else {
          notPres ++;
        }
        
      }
    }
    
    System.out.format("added: %,d dups: %,d rmvd: %,d notPres: %,d, size: %d%n", countAdd, dupsA, countRmv, notPres, ihs.size());
    assertEquals(countAdd - countRmv, ihs.size() );
  }
}
