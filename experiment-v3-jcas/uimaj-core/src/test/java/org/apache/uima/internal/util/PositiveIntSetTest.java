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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

public class PositiveIntSetTest extends TestCase {
   
  Random rand = new Random();
  
  static class R {
    boolean useInitParms = false;
    int initSize = 2;
    int estMin = 15;
    int estMax = 100;
    int firstValue = estMin;
    float add_pc = .9f;
    float clear_pc = .001f;
    float remove_not_present_pc = .01f;
  }
  
  private R r = new R();
  private Set<Integer> cs = new HashSet<Integer>();
  private PositiveIntSet s;
  private int gi;
  
  public void testSwitchFromHashToBitWithOffset() {
    PositiveIntSet_impl s = new PositiveIntSet_impl();
    int [] x = new int[100];
    int ix = 0;
    
    s.add(x[ix++] = 100000);  // start as bit set with offset 100000
    
    s.add(x[ix++] = 101328);  // switches to intSet
    for (int i = 0; i < 14; i++) {
      s.add(x[ix++] = 100001 + i);  
    }
    s.add(x[ix++] = 100001 + 14);  // switches to hashSet, size = 64 entries in short table = 32 words + 11
    for (int i = 0; i < 24; i++) {
      s.add(x[ix++] = 100100 + i);
    }
    // next causes an exception on 2.7.0 rc2 
    s.add(x[ix++] = 99999);  // switch to bit set with key being the lowest value      
    s.add(x[ix++] = 100501);
    
    
    // validate s
    assertEquals(19 + 24, s.size());
    for (int i = 0; i < (19 + 24); i++) {
      assertTrue(s.contains(x[i]));
    }
    assertFalse(s.contains(1));
  }
  

  public void testBasic() {
    PositiveIntSet_impl s = new PositiveIntSet_impl();
    s.add(128);
    assertTrue(s.isBitSet);
    s.add(128128);
    assertFalse(s.isBitSet);
    
    IntListIterator it = s.getOrderedIterator();
    assertTrue(it.hasNext());
    assertEquals(128, it.next());
    assertTrue(it.hasNext());
    assertEquals(128128, it.next());
    assertFalse(it.hasNext());

    // test offset
    int bb = 300000;
    s = new PositiveIntSet_impl();
    assertTrue(s.useOffset);
    s.add(bb);

    s.add(bb);
    s.add(bb+1);
    assertTrue(s.isBitSet);
    s.add(bb+2);
    
    assertTrue(s.isBitSet);
    assertEquals(3, s.size());
    assertTrue(s.useOffset);
    
    // test offset converting to hashset
    s.add(bb - 6000);
    assertEquals(4, s.size());
    assertFalse(s.isBitSet);
    it = s.getOrderedIterator();
    assertEquals(bb-6000, it.next());
    assertEquals(bb, it.next());
    assertEquals(bb+1, it.next());
    assertEquals(bb+2, it.next());

    
    bb = 67;
    s = new PositiveIntSet_impl();
    assertTrue(s.useOffset);
    s.add(bb);
    s.add(bb);
    s.add(bb+1);
    s.add(bb+2);
    
    assertEquals(3, s.size());
    assertTrue(s.isBitSet);
    assertTrue(s.useOffset);
    // test offset converting to bitset with no offset    
    s.add(bb - 66);
    assertEquals(4, s.size());
    assertTrue(s.isBitSet);
    assertFalse(s.useOffset);
    it = s.getOrderedIterator();
    assertEquals(bb-66, it.next());
    assertEquals(bb, it.next());
    assertEquals(bb+1, it.next());
    assertEquals(bb+2, it.next());
    
    // test switch from IntSet to bit set
    s.clear();  // keeps useOffset false
    s.add(1216);  // makes the space used by bit set = 41 words
    
    for (int i = 1; i < 23; i++) {
      s.add(i);
//      System.out.println("i is " + i + ", isBitSet = " + s.isBitSet);
      assertTrue("i is " + i, (i < 16) ? (s.isIntSet) : s.isBitSet);
    }
    
    it = s.getOrderedIterator();
    for (int i = 1; i < 23; i++) {
      assertEquals(i, it.next());
    }
    assertEquals(1216,it.next());
    
    boolean reached = false;
    for (int i = 10; i < 20000/*5122*/; i = i <<1) {
      s.add(i);  // switches to hash set when i = 2560. == 1010 0000 0000  (>>5 = 101 0000 = 80 (decimal)
                 // hash set size for 19 entries = 19 * 3 = 57
                 // bit set size for 2560 = 80 words
                 // bit set size for prev i (1280 dec) = 101 0000 0000 (>>5 = 10 1000) = 40 words
//      if (!s.isBitSet) {
//        System.out.println("is Bit set? " + s.isBitSet + ", i = " + i + ", # of entries is " + s.size());
//      }
      reached = i >= 2560;
      assertTrue((!reached) ? s.isBitSet : !s.isBitSet);
    }
    assertTrue(reached);
  }
  
  public void testiterators() {
    PositiveIntSet_impl s = new PositiveIntSet_impl();
    int [] e = new int [] {123, 987, 789, 155, 
                           156, 177, 444, 333,
                           242, 252, 262, 243,
                           221, 219, 217, 300,
                           399};
    int [] eOrdered = Arrays.copyOf(e, e.length);
    Arrays.sort(eOrdered);
    for (int i : e) {s.add(i);}
    int[] r = s.toUnorderedIntArray();
    assertTrue(Arrays.equals(r, eOrdered));    
   
    s.clear(); 
    e[0] = 125;
    e[2] = 1500000;
    eOrdered = Arrays.copyOf(e, e.length);
    Arrays.sort(eOrdered);
    
    for (int i : e) {s.add(i);}
    r = s.toUnorderedIntArray();
    assertFalse(Arrays.equals(r, e));
    assertFalse(s.isBitSet);
    r = s.toOrderedIntArray();
    assertTrue(Arrays.equals(r, eOrdered));
    
  }
  
  /**
   * TODO  extra cases to verify (paths)
   * 
   * 1) Switching from bit set due to lower bound dropping below offset
   *   - where result would (?) fit in "short" hash set
   *   - where result would be size < 16 
   *   
   * 2) Switching from bit set (tiny) to intSet  
   */
  
  public void testBit2ShortHash() {
    PositiveIntSet_impl s = new PositiveIntSet_impl();
    for (int i = 0; i < 16; i++) {
      s.add(1000 + i);
    }
    assertTrue(s.isBitSet && s.isOffsetBitSet());
    s.add(874);  // bit sets with offset have some space below; this is below that
    assertTrue(s.isBitSet && (!s.isOffsetBitSet()));
    s.add(1);
    assertTrue(s.isBitSet);  // without offset, fits
    s.add(10000);
    assertTrue(s.isHashSet && s.isShortHashSet() && !s.isBitSet);
    s.add(100000);
    assertTrue(!s.isShortHashSet());
    int[] sv = s.toIntArray();
    Arrays.sort(sv);
    assertTrue(Arrays.equals(sv,  new int[] {1, 874, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
      1010, 1011, 1012, 1013, 1014, 1015, 10000, 100000}));
    
    // try going from bit set with offset directly to hash set
    s = new PositiveIntSet_impl();
    for (int i = 0; i < 16; i++) {
      s.add(1000 + i);
    }
    s.add(10000);
    assertTrue(s.isHashSet && s.isShortHashSet() && !s.isBitSet);
    s.add(100000);
    assertTrue(!s.isShortHashSet());
    sv = s.toIntArray();
    Arrays.sort(sv);
    assertTrue(Arrays.equals(sv,  new int[] { 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
      1010, 1011, 1012, 1013, 1014, 1015, 10000, 100000}));
    
    // going from bit set to intset
    s = new PositiveIntSet_impl();
    s.add(1000);
    assertTrue(s.isBitSet && s.isOffsetBitSet());
    s.add(1259);   // 1258 doesn't switch, because 1258 "fits" in existing bit set allocation
    assertTrue(!s.isBitSet && s.isIntSet);
    
    // going from int set to bit set
    for (int i = 0; i < 14; i++) {
      s.add(1001 + i);   // add ints that will fit in bit set
      assertTrue(s.isIntSet);
    }
    s.add(1015);
    assertTrue(s.isBitSet && s.isOffsetBitSet());
    assertTrue(Arrays.equals(s.toIntArray(),  new int[] { 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
        1010, 1011, 1012, 1013, 1014, 1015, 1259}));
    s.remove(1015);  // removing doesn't cause re-sizing
    assertTrue(s.isBitSet && s.isOffsetBitSet());
    assertTrue(Arrays.equals(s.toIntArray(),  new int[] { 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
       1010, 1011, 1012, 1013, 1014, 1259}));
  }
  
  public void testRandom() {
    long seed = rand.nextLong();
    System.out.println("PositiveIntSet test random seed = " + seed);
//    seed = 3324098689001789475L;
    rand.setSeed(seed);
    for (gi = 0; gi < 100 /*10000*/; gi++) {
      generateRandomParameters();
      runRandomTests();
    }
  }
  
  public void generateRandomParameters() {
    r.initSize = rand.nextInt(100000) + 2;
    r.add_pc = rand.nextFloat() + .5f;
    r.clear_pc = rand.nextFloat() * .01f;
    r.remove_not_present_pc = rand.nextFloat() * .1f;
    r.useInitParms = choose(.2f);
    r.estMin = rand.nextInt(100000) + 1;
    r.estMax = r.estMin + ((choose(.5f)) ? rand.nextInt(1000) : rand.nextInt(100000)); 
  }
  
  /**
   * Random testing
   * 
   *   Do updates to both positiveintset and plain java set<Integer>
   * 
   *   adds and removes (in x % ratio)
   *   adds are unique x % of time
   *   removes remove existing x % of time
   *   
   *   clear() done x % of each run.
   *   
   *   clustering:  
   *     values > offset x % of runs.
   *     values in range offset - 32K to offset + 32K  x % of runs
   *     values < (offset + n * 32) x % of runs   
   *   
   *   checks: 
   *     at end:
   *       content as expected via compare to plain java set<integer>
   *       style of intSet as expected
   *     iterator: produces values, as expected via compare  
   *     
   *       
   */
  private void runRandomTests() { 
    s = r.useInitParms ? new PositiveIntSet_impl(r.initSize, r.estMin, r.estMax) : new PositiveIntSet_impl();
    cs = new HashSet<Integer>();
    dadd(r.firstValue);
    
    for (int i = 0; i < 10000; i++) {
      add_remove_clear(i);
    }
    compare();
  }
  
  /**
   * @param perCent the percent to use
   * @return true x perCent of the time, randomly
   */
  private boolean choose(float perCent) {
    return rand.nextFloat() <= perCent;
  }
  
  private void add_remove_clear(int i) {
    if (choose(r.clear_pc)) {
      s.clear();
      cs.clear();
    } else if (choose(r.add_pc)) {
      dadd(r.firstValue + i);      
    } else {
      dremove(i);
    }
  }
  
  private void compare() {
    int[] v1 = s.toIntArray();
    int[] v2 = new int[cs.size()];
    Iterator<Integer> it = cs.iterator();
    int i = 0;
    while (it.hasNext()) {
      v2[i++] = it.next();
    }
    Arrays.sort(v1);
    Arrays.sort(v2);
    assertTrue(Arrays.equals(v1, v2));
    
    IntListIterator it2 = s.iterator();
    i = 0;
    while (it2.hasNext()) {
      v1[i++] = it2.next();
    }
    Arrays.sort(v1);
    assertTrue(Arrays.equals(v1, v2));    
  }
  
  
  private boolean dadd(int v) {
    boolean wasAdded = s.add(v);
    boolean wasAddedc = cs.add(v);
    assertEquals(wasAdded, wasAddedc);
    return wasAdded;
  }
  
  private boolean dremove(int i) {
    int key = getKeyToBeRemoved(i);
    boolean wasRemoved = s.remove(key);
    boolean wasRemovedc = cs.remove(key);
    assertEquals(wasRemoved, wasRemovedc);
    return wasRemoved;
  }
  
  private int getKeyToBeRemoved(int w) {
    int sz = cs.size();

    if (sz == 0 || choose(r.remove_not_present_pc)) {
      return nonPresentValue();
    }
    Iterator<Integer> it = cs.iterator();
    int which = Math.min(sz - 1, w % 8);
    for (int i = 0; i < which; i++) {
      it.next();
    }
    return it.next();
  }
  
  private int nonPresentValue() {
    for (int i = 1; ; i++) {
      if (!cs.contains(i)) {
        return i;
      }
      if (i > 0) {
        i = -i;
      } else {
        i = (-i) + 1;
      }
    }
  }
}
