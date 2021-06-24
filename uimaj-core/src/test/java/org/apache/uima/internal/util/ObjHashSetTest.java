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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObjHashSetTest {

  ObjHashSet<Integer> ihs;

  Random random;

  @BeforeEach
  public void setUp() {
    ihs = new ObjHashSet<>(Integer.class, Integer.MIN_VALUE);
  }

  @Test
  public void testBasic() {

    ihs.add(15);
    ihs.add(188);
    Integer[] sv = ihs.toArray();
    assertEquals(2, sv.length);
    assertEquals(15 + 188, sv[0] + sv[1]);

    // test most positive / negative
    ihs.clear();
    ihs.add(189);
    ihs.add(1000);
    ihs.add(-1000);
    ihs.add(500);
    ihs.add(-500);
    ihs.remove(1000);
    ihs.add(1001);
  }

  @Test
  public void testContains() {
    ihs.add(1188);
    ihs.add(1040);
    assertTrue(ihs.contains(1188));
    assertTrue(ihs.contains(1040));
    assertFalse(ihs.contains(1));
    assertFalse(ihs.contains(99));
  }

  // public void testTableSpace() {
  // assertEquals(32, ObjHashSet.tableSpace(19, 0.6f)); // 19 / .6 = 31.xxx, round to 32
  // assertEquals(64, ObjHashSet.tableSpace(21, 0.6f));
  // assertEquals(32, ihs.tableSpace(21));
  // }

  @Test
  public void testExpandNpe() {
    ihs.add(15);
    ihs.add(150000); // makes 4 byte table entries

    for (int i = 1; i < 256; i++) { // 0 is invalid key
      ihs.add(i); // causes resize, check no NPE etc thrown.
    }
  }

  @Test
  public void testAddIntoRemovedSlot() {
    long seed = // -4571976104270514645L;
            new Random().nextLong();
    System.out.println(
            "Random seed for testAddIntoRemovedSlot in " + this.getClass().getName() + ": " + seed);
    random = new Random(seed);

    for (int i = 1; i < 100; i++) {
      ihs.add(i);
      assertEquals(i, ihs.size());
    }

    assertEquals(99, ihs.size());

    /** Test with 2 byte numbers */
    checkRemovedReuse(true);

    ihs = new ObjHashSet<>(Integer.class, Integer.MIN_VALUE);
    for (int i = 1; i < 99; i++) {
      ihs.add(i);
    }
    ihs.add(100000); // force 4 byte
    checkRemovedReuse(false);
  }

  private void checkRemovedReuse(boolean is2) {
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

    ihs.clear(); // doesn't set 2nd time because size + removed > 1/2 the capacity

    for (int i = 32768; i > 128; i = i / 2) {
      ihs.clear(); // sets 2nd time shrinkable
      assertEquals(i, ihs.getCapacity());
      ihs.clear(); // shrinks
      assertEquals(i / 2, ihs.getCapacity());
    }
    // ihs.clear();
    //

    // table size should be 128, adding 100 items should cause expansion (84 == .66 * 128)
    for (int i = 1; i < 100; i++) {
      ihs.add(i);
    }

    assertEquals(256, ihs.getCapacity());
    for (int i = 0; i < 1000; i++) {
      int v = 1 + random.nextInt(100);
      ihs.remove(v);
      assertTrue(!(ihs.contains(v)));
      ihs.add(v);
      assertTrue(ihs.contains(v));
    }

    assertEquals(256, ihs.getCapacity());

  }

  @Test
  public void testRandom() {
    int countAdd = 0;
    int dupsA = 0;
    int notPres = 0;
    int countRmv = 0;

    long seed = new Random().nextLong();
    System.out.println("Random seed for testRandom in " + this.getClass().getName() + ": " + seed);
    random = new Random(seed);

    for (int i = 1; i < 1024 * 1024; i++) {
      int k = i & (1024 * 256) - 1;
      if (k == 0)
        continue;
      if (random.nextInt(3) > 0) {
        int sz = ihs.size();
        if (ihs.add(k)) {
          countAdd++;
          assertEquals(sz + 1, ihs.size());
        } else {
          dupsA++;
        }

      } else {
        int sz = ihs.size();
        if (ihs.remove(k)) {
          countRmv++;
          assertEquals(sz - 1, ihs.size());
        } else {
          notPres++;
        }

      }
    }

    System.out.format("added: %,d dups: %,d rmvd: %,d notPres: %,d, size: %d%n", countAdd, dupsA,
            countRmv, notPres, ihs.size());
    assertEquals(countAdd - countRmv, ihs.size());
  }
}
