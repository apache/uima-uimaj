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

import org.apache.uima.util.IntEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class Int2ObjHashMapTest {

  // a number of extras needed to cause rebalance
  // is > 1 because it might turn out randomly that
  // the puts clear all the REMOVED_KEY values
  // needs to be > than 1/2 the capacity/2
  private static final int REBAL = 32;

  Int2ObjHashMap<Integer, Integer> ihm;

  @BeforeEach
  void setUp() {
    ihm = new Int2ObjHashMap<>(Integer.class);
  }

  @Test
  void testBasic() {

    ihm.put(15, 150);
    ihm.put(188, 1880);
    assertThat(ihm.size()).isEqualTo(2);
    assertThat((int) ihm.get(15)).isEqualTo(150);
    assertThat((int) ihm.get(188)).isEqualTo(1880);

    assertThat(ihm.remove(18)).isNull();
    assertThat((int) ihm.remove(15)).isEqualTo(150);
    assertThat(ihm.size()).isEqualTo(1);

    for (IntEntry<Integer> ie : ihm) {
      assertThat(188).isEqualTo(ie.getKey());
    }

    assertThat((int) ihm.remove(188)).isEqualTo(1880);
    assertThat(ihm).isEmpty();
    assertThat(ihm.iterator().hasNext()).isFalse();
  }

  @Test
  void testRebalance() {
    // 100 elements, require 256 table (128 * .66 = 85)
    for (int i = 1; i < 101; i++) {
      ihm.put(i, i * 10);
    }

    // have 100 elements, remove 100 elements
    for (int i = 1; i < 101; i++) {
      assertThat((int) ihm.remove(i)).isEqualTo(i * 10);
    }

    assertThat(ihm.size()).isEqualTo(0);

    assertThat(ihm.getCapacity()).isEqualTo(64);

    for (int i = 1; i < 101 + REBAL; i++) {
      ihm.put(i + 100, i * 10); // add different keys, so may use some new slots
    }

    assertThat(ihm.getCapacity()).isEqualTo(256); // because above are different adds, likely into
                                          // non-removed positions

    // have 100 elements, remove 100 elements
    for (int i = 1; i < 101 + REBAL; i++) {
      assertThat((int) ihm.remove(i + 100)).isEqualTo(i * 10);
    }

    assertThat(ihm.getCapacity()).isEqualTo(64);

  }

  @Test
  void testRandom() {
    int countAdd = 0;
    int dupsA = 0;
    int notPres = 0;
    int countRmv = 0;

    long seed = // -6616473831883690L;
            new Random().nextLong();
    System.out.println("Random seed for Int2ObjHashMapTest: " + seed);
    Random r = new Random(seed);

    for (int i = 1; i < 1024 * 512; i++) {
      int k = i & 1024 - 1;
      if (k == 0)
        continue;
      if (r.nextInt(3) > 0) {
        int sz = ihm.size();
        if (ihm.put(k, -k) == null) {
          countAdd++;
          assertThat(ihm.size()).isEqualTo(sz + 1);
        } else {
          dupsA++;
        }
      } else {
        int sz = ihm.size();
        if (ihm.remove(k) != null) {
          countRmv++;
          assertThat(ihm.size()).isEqualTo(sz - 1);
        } else {
          notPres++;
        }

      }
    }
    System.out.format("%s testRandom added: %,d dups: %,d rmvd: %,d notPres: %,d, size: %d%n",
            this.getClass().getName(), countAdd, dupsA, countRmv, notPres, ihm.size());
    assertThat(ihm.size()).isEqualTo(countAdd - countRmv);
  }
}
