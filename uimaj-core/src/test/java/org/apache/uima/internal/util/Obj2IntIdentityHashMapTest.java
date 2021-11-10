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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Obj2IntIdentityHashMapTest {

  // a number of extras needed to cause rebalance
  // is > 1 because it might turn out randomly that
  // the puts clear all the REMOVED_KEY values
  // needs to be > than 1/2 the capacity/2
  private static final int REBAL = 32;

  Obj2IntIdentityHashMap<Integer> ihm;

  private static Integer[] I = new Integer[300];
  static {
    for (int i = 0; i < I.length; i++)
      I[i] = i;
  }

  Map<Integer, Integer> im = new HashMap<>();

  @BeforeEach
  public void setUp() {
    ihm = new Obj2IntIdentityHashMap<>(Integer.class, Integer.MIN_VALUE);
  }

  @Test
  public void testBasic() {

    Integer I15 = 15;
    Integer I188 = 188;

    ihm.put(I15, 150);
    ihm.put(I188, 1880);
    assertEquals(2, ihm.size());
    assertEquals(150, (int) ihm.get(I15));
    assertEquals(1880, (int) ihm.get(I188));
    assertEquals(0, (int) ihm.get(188)); // not identity equal

    assertEquals(0, ihm.remove(18));
    assertEquals(150, (int) ihm.remove(I15));
    assertEquals(1, ihm.size());

    // for (IntEntry<Integer> ie : ihm) {
    // assertEquals(ie.getKey(), 188);
    // }

    assertEquals(1880, (int) ihm.remove(I188));
    assertEquals(0, ihm.size());
    // for (IntEntry<Integer> ie : ihm) {
    // fail(); // should be empty
    // }

  }

  @Test
  public void testRebalance() {
    // 100 elements, require 256 table (128 * .66 = 85)
    for (int i = 1; i < 101; i++) {
      ihm.put(I[i], i * 10);
    }

    // have 100 elements, remove 100 elements
    for (int i = 1; i < 101; i++) {
      assertEquals(i * 10, (int) ihm.remove(I[i]));
    }

    assertEquals(0, ihm.size());

    assertEquals(64, ihm.getCapacity());

    for (int i = 1; i < 101 + REBAL; i++) {
      ihm.put(I[i + 100], i * 10); // add different keys, so may use some new slots
    }

    assertEquals(256, ihm.getCapacity()); // because above are different adds, likely into
                                          // non-removed positions

    // have 100 elements, remove 100 elements
    for (int i = 1; i < 101 + REBAL; i++) {
      assertEquals(i * 10, (int) ihm.remove(I[i + 100]));
    }

    assertEquals(64, ihm.getCapacity());

  }

  private Integer J(int v) {
    im.putIfAbsent(v, v);
    return im.get(v);
  }

  @Test
  public void testRandom() {
    int countAdd = 0;
    int dupsA = 0;
    int notPres = 0;
    int countRmv = 0;

    long seed = // -6616473831883690L;
            new Random().nextLong();
    System.out.println("Random seed for Obj2IntIdentityHashMapTest: " + seed);
    Random r = new Random(seed);

    for (int i = 1; i < 1024 * 512; i++) {
      int k = i & 1024 - 1;
      if (k == 0)
        continue;
      if (r.nextInt(3) > 0) {
        int sz = ihm.size();
        if (ihm.put(J(k), -k) == 0) {
          countAdd++;
          assertEquals(sz + 1, ihm.size());
        } else {
          dupsA++;
        }
      } else {
        int sz = ihm.size();
        if (ihm.remove(J(k)) != 0) {
          countRmv++;
          assertEquals(sz - 1, ihm.size());
        } else {
          notPres++;
        }

      }
    }
    System.out.format("%s testRandom added: %,d dups: %,d rmvd: %,d notPres: %,d, size: %d%n",
            this.getClass().getName(), countAdd, dupsA, countRmv, notPres, ihm.size());
    assertEquals(countAdd - countRmv, ihm.size());
  }
}
