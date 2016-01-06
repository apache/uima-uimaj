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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

public class Int2IntHashMapTest extends TestCase {
  
  public void testIterator() {
    Int2IntHashMap ia = new Int2IntHashMap();
    Integer[] vs = new Integer[] {2, 2, 5, 1, 6, 7, 3, 4};
    for (Integer i : vs) {
      ia.put(i, i * 2);
    }
    Integer[] r = new Integer[vs.length];
    int i = 0;
    IntListIterator itl = ia.keyIterator();

    while(itl.hasNext()){
      r[i++] = itl.next();  
    }
    assertEquals(i, vs.length - 1);
    assertTrue(Arrays.equals(r, new Integer[] {3, 2, 1, 4, 7, 6, 5, null}));

    i = 0;
    for (IntKeyValueIterator it = ia.keyValueIterator(); it.isValid(); it.inc()) {
      r[i++] = it.getValue();  
//      System.out.format("key: %d   value: %d%n", it.get(), it.getValue());
    }
    assertTrue(Arrays.equals(r, new Integer[] {6, 4, 2, 8, 14, 12, 10, null} ));
    
    i = 0;
    
    IntKeyValueIterator it = ia.keyValueIterator();
    assertTrue(it.isValid());
    it.dec();
    assertFalse(it.isValid());
    it.inc();
    assertFalse(it.isValid());
    it.moveToLast();
    assertTrue(it.isValid());
    it.inc();
    assertFalse(it.isValid());
//    it.dec();  // causes infinite loop
//    assertFalse(it.isValid());
    
  }
  
  public void testFastLookup() {
    Int2IntHashMap ia = new Int2IntHashMap();
    Random r = new Random();
    Set<Integer> keys = new HashSet<Integer>(1000);
    
    for (int i = 0; i < 1000; i++) {
      int k = r.nextInt(1000);
      while (k == 0) {k = r.nextInt(1000);}
      keys.add(k);
      ia.put(k, 10000+k);
    }
    
    for (int k : keys) {     
      assertEquals(10000 + k, ia.get(k));
    }
  }
  
  public void testContainsKey() {
    Int2IntHashMap map1 = new Int2IntHashMap();
    
    for (int i = 1; i < 100; i++) {
        map1.put(i, 100-1);
    }

    for (int i = 1; i < 100; i++) {
      assertTrue("Map should contain key " + i + " but it is missing", map1.containsKey(i));
    }
    
  }
  
}
