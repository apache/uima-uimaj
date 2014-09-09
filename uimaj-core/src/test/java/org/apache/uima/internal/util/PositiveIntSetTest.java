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

import org.apache.uima.internal.util.IntBitSet;
import org.apache.uima.internal.util.IntHashSet;
import org.apache.uima.internal.util.rb_trees.IntArrayRBT;

import junit.framework.TestCase;

public class PositiveIntSetTest extends TestCase {
   
  Random r = new Random();
  


  public void testBasic() {
    PositiveIntSet s = new PositiveIntSet();
    s.add(128);
    assertFalse(s.isBitSet);
    
    for (int i = 1; i < 10; i++) {
      s.add(i);
      assertTrue("i is " + i, (i < 4) ? (!s.isBitSet) : s.isBitSet);
    }
    
    for (int i = 10; i < 10000; i = i <<1) {
      s.add(i);  // switches to hash set when i = 2560. == 1010 0000 0000  (>>5 = 101 0000 = 80 (decimal)
                 // hash set size for 19 entries = 19 * 3 = 57
                 // bit set size for 2560 = 80 words
                 // bit set size for prev i (1280 dec) = 101 0000 0000 (>>5 = 10 1000) = 40 words
//      if (!s.isBitSet) {
//        System.out.println("# of entries is " + s.size());
//      }
      assertTrue((i < 2560) ? s.isBitSet : !s.isBitSet);
    }
  }
  
  public void testiterators() {
    PositiveIntSet s = new PositiveIntSet();
    int [] e = new int [] {123, 987, 789};
    int [] eOrdered = Arrays.copyOf(e, e.length);
    Arrays.sort(eOrdered);
    s.add(e);
    int[] r = s.toUnorderedIntArray();
    assertTrue(Arrays.equals(r, eOrdered));    
   
    s.clear(); 
    e[0] = 125;
    e[2] = 1500000;
    s.add(e);
    r = s.toUnorderedIntArray();
    assertFalse(Arrays.equals(r, e));
    assertFalse(s.isBitSet);
    r = s.toOrderedIntArray();
    assertTrue(Arrays.equals(r, e));
    
  }
  
}
