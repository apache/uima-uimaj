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

package org.apache.uima.internal.util.rb_trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntPointerIterator;

import junit.framework.TestCase;

public class IntArrayRBTtest extends TestCase {
  public void testIterator() {
    IntArrayRBT ia = new IntArrayRBT();
    Integer[] vs = new Integer[] {2, 2, 5, 1, 6, 7, 3, 4};
    for (Integer i : vs) {
      ia.insertKey(i);
    }
    Integer[] r = new Integer[vs.length];
    int i = 0;
    IntListIterator itl = ia.iterator();

    while(itl.hasNext()){
      r[i++] = itl.next();  
    }
    assertEquals(i, vs.length - 1);
    assertTrue(Arrays.equals(r, new Integer[] {1, 2, 3, 4, 5, 6, 7, null}));

    i = 0;
    IntPointerIterator it = ia.pointerIterator();
    while (it.isValid()) {
      r[i++] = it.get();
      it.inc();
    }
    assertEquals(i, vs.length - 1);
    assertTrue(Arrays.equals(r, new Integer[] {1, 2, 3, 4, 5, 6, 7, null}));
    
    it = ia.pointerIterator();
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
}
