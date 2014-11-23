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

import java.util.Random;

import junit.framework.TestCase;

import org.apache.uima.internal.util.rb_trees.IntArrayRBT;

public class IntHashSetPerfTest extends TestCase {
  /**
   * Set to false to run the performance test
   * 
   * Tests both IntHashSet and IntBitSet
   */
  final boolean SKIP = true;
  
  
  Random r = new Random();
//  Set<Integer> keys = new HashSet<Integer>(1000);
  
  int dmv = 0;
  
  IntHashSet m2;
  IntArrayRBT m1;
  IntBitSet m3;
  
  final int[] keys10000 = new int[111111];
  int k10ki = 0;
  
  
  public void testPerf() {
    if (SKIP) return;
    m1 = new IntArrayRBT(16);
    m2 = new IntHashSet(16);
    m3 = new IntBitSet(16);
     
    for (int i = 0; i < 111111; i++) {
      int k = r.nextInt(100000);
      while (k == 0) { k = r.nextInt(100000);}
      keys10000[i] = k;
    }

    System.out.format("%n%n W A R M U P %n%n");
    warmup(m1);
    warmup(m2);
    
    System.out.format("%n%n Time 100 %n%n");
    timelp(100);
    System.out.format("%n%n Time 1000 %n%n");
    timelp(1000);
    System.out.format("%n%n Time 10000 %n%n");
    timelp(10000);
    System.out.format("%n%n Time 100000 %n%n");
    timelp(100000);
    
    System.out.println(dmv);
  }
  private void time2(int n) {
    float f1 = time(m1, n);
    float f2 = time(m2, n);
    float f3 = time(m3, n);
    System.out.format(" ratio RBT/hash = %.3f   RTB/bitset = %.3f  hash/bitset = %.3f%n", 
        f1/f2,  f1/f3, f2/f3);   
  }
  
  private void timelp(int n) {
    time2(n);
    time2(n);
    time2(n);
  }

  private void warmup(Object m) {
    for (int i = 0; i < 500; i++) {
      inner(m,true, 1000) ; // warm up
    }
  }
  
  private float time(Object m, int ss) {
    long start = System.nanoTime();
    for (int i = 0; i < 500; i++) {
      inner(m,false, ss);
    }
    float t = (System.nanoTime() - start) / 1000000.0F;
    System.out.format("time for %,d:  %s is %.3f milliseconds %n", ss,
        m.getClass().getSimpleName(),
        t);
    return t;
  }
  
  private int nextKey() {
    int r = keys10000[k10ki++];
    if (k10ki >= keys10000.length) {
      k10ki = 0;
    }
    return r;
  }
  
  private void inner(Object m, boolean check, int ss) {
    CS cs = new CS(m);     

    for (int i = 0; i < ss; i++) {
      
      int k = keys10000[i];
//      System.out.print(" " + k);
//      if (i %100 == 0) System.out.println("");
//      keys.add(k);
      cs.add(k);
      if (check) {
        assertTrue(cs.contains(k));
      }
    }
    for (int i = 0; i < ss; i++) {
      boolean v = cs.contains(keys10000[i]); 
      if (!v) {
        throw new RuntimeException("never happen");
      }
      dmv += 1;
    }
    cs.clear();
    

//    for (int k : keys) {     
//      assertEquals(10000 + k, (m instanceof IntHashSet) ? 
//          ((IntHashSet)m).get(k) :
//          ((IntArrayRBT)m).getMostlyClose(k));
//    }

  }
  
  static class CS {
    final Object set;
    
    CS(Object set) {
      this.set = set;
    }
    
    boolean contains(int i) {
      return (set instanceof IntArrayRBT) ? ((IntArrayRBT)set).contains(i) :
             (set instanceof IntHashSet)  ? ((IntHashSet) set).contains(i) :
                                            ((IntBitSet)  set).contains(i);
    }
    
    void add(int i) {
      if (set instanceof IntArrayRBT) {
        ((IntArrayRBT)set).add(i);
      } else if (set instanceof IntHashSet) {
        ((IntHashSet)set).add(i);
      } else {
        ((IntBitSet)set).add(i);
      }    
    }
    
    void clear() {
      if (set instanceof IntArrayRBT) {
        ((IntArrayRBT)set).clear();
      } else if (set instanceof IntHashSet) {
        ((IntHashSet)set).clear();
      } else {
        ((IntBitSet)set).clear();
      }    
    }

  }
  
  
}
