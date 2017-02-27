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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.uima.internal.util.IntListIterator;

public class IntArrayRBTtest extends TestCase {
  private final static int NIL = 0;
  static final Random rand = new Random();    
  static {  
//     long seed = -585739628489294672L; 
         /* rand.nextLong() */ ;
     long seed = rand.nextLong();
     rand.setSeed(seed);
     System.out.println("IntArrayRBTtest seed is " + seed);
  }

    
  //debug occasional looping in table
  
  private void checkStructure(IntArrayRBT ia) {
    if (ia.root == NIL) {
      if (ia.greatestNode != NIL) {
        assertTrue(ia.greatestNode == NIL);
      }
      return;
    }
    if (ia.greatestNode == NIL) {
      assertTrue(ia.root == NIL);
      return;
    }
    checkParents(ia, ia.root);
  }
  
  private void checkParents(IntArrayRBT ia, int node) {
    if (node == NIL) {
      return;
    }
    int v0 = Integer.MIN_VALUE;
    int v2 = Integer.MAX_VALUE;
    int v1 = ia.getKeyForNode(node);
    int left = ia.getLeft(node);
    int right = ia.getRight(node);
    if (left != NIL) {
      assertEquals(ia.getParent(left), node);
      assertFalse(left == right);
      assertTrue(left != ia.root);
      assertTrue(left != ia.greatestNode);
      v0 = ia.getKeyForNode(left);
    }
    if (right != NIL) {
      assertEquals(ia.getParent(right), node);
      assertFalse(left == right);
      assertTrue(right != ia.root);
      v2 = ia.getKeyForNode(right);
    }
    assertTrue(v0 < v1);
    assertTrue(v1 < v2);
    checkParents(ia, left);
    checkParents(ia, right);
  }
  
  public void testStructure() {
    
    for (int ol = 0; ol < 100; ol ++) {
      IntArrayRBT ia = new IntArrayRBT(4);
      int i = 0;
      int [] vs = new int[128];
      for (; i < 120; i++) {
        vs[i] = rand.nextInt(100);  // some collisions
        ia.add(vs[i]);
        if ((i % 2) == 1) { // do removes at 50% rate
          ia.deleteKey(rand.nextInt(i)); // remove a random one
          checkStructure(ia);
          assertTrue(ia.satisfiesRedBlackProperties());
        }
      }
    }
  }
  
  
  public void testFindInsertionPoint() {
    IntArrayRBT ia = new IntArrayRBT();
    Integer[] vs = new Integer[] {2, 2, 5, 1, 6, 7, 3, 4};
    for (Integer i : vs) {
      ia.insertKey(i);
    }
    
    assertTrue(ia.findInsertionPoint(7) != 0);
    assertEquals(0, ia.findInsertionPoint(8));
    
  }
  
  public void testIterator() {
    IntArrayRBT ia = new IntArrayRBT();
    
    ia.insertKey(4);
    ia.insertKey(8);
    ia.insertKey(2);
    
    IntListIterator it = ia.iterator();
    
    assertEquals(2, it.next());
    assertEquals(4, it.next());
    assertEquals(8, it.next());
    assertFalse(it.hasNext());
  }
  
//  public void testIterator() throws ResourceInitializationException {
//    CAS cas = CasCreationUtils.createCas((TypeSystem)null,  null, null, null);
//    FSRBTSetIndex<Annotation> setIndex = new FSRBTSetIndex<Annotation>((CASImpl)cas, cas.getAnnotationType(), FSIndex.SET_INDEX);
//    IntArrayRBT ia = new IntArrayRBT();
//    Integer[] vs = new Integer[] {2, 2, 5, 1, 6, 7, 3, 4};
//    for (Integer i : vs) {
//      ia.insertKey(i);
//    }
//    Integer[] rand = new Integer[vs.length];
//    int i = 0;
//    IntListIterator itl = ia.iterator();
//
//    while(itl.hasNext()){
//      rand[i++] = itl.next();  
//    }
//    assertEquals(i, vs.length - 1);
//    assertTrue(Arrays.equals(rand, new Integer[] {1, 2, 3, 4, 5, 6, 7, null}));

//  moved to IteratorTest to take advantage of that test's setup
//    i = 0;
//
//    IntPointerIterator it = ia.pointerIterator(setIndex, null, null);
//    while (it.isValid()) {
//      rand[i++] = it.get();
//      it.inc();
//    }
//    assertEquals(i, vs.length - 1);
//    assertTrue(Arrays.equals(rand, new Integer[] {1, 2, 3, 4, 5, 6, 7, null}));
//    
//    it = ia.pointerIterator(setIndex, null, null);
//    assertTrue(it.isValid());
//    it.dec();
//    assertFalse(it.isValid());
//    it.inc();
//    assertFalse(it.isValid());
//    it.moveToLast();
//    assertTrue(it.isValid());
//    it.inc();
//    assertFalse(it.isValid());
//    it.dec();  // causes infinite loop
//    assertFalse(it.isValid());
    
//  }
  
  public void testLargeInsertsDeletes() {
    IntArrayRBT ia = new IntArrayRBT();
    System.gc();
    long fm1 = Runtime.getRuntime().freeMemory();
    int[] ks = new int[10000];
    System.out.print("freemem after intArrayRBT keys deleted to 0 (should be about the same): " + fm1 + " ");
    
    for (int j = 0; j < 10; j++) {
      
      // insert values
      int shadowSize = 0;
      for (int i = 0; i < 1000; i++) {
        int k = rand.nextInt(1000);
        ks[i] = k;
        boolean wasInserted = ia.insertKeyShowNegative(k) >= 0;
        if (wasInserted) {
          shadowSize++;
        }
      }
      assertEquals(shadowSize, ia.size);
      
      //debug
//      int[] iv = new int[1000];
//      int iiv = 0;
//      IntListIterator it = ia.iterator();
//      while (it.hasNext()) {
//        iv[iiv++] = it.next();
//      }

      assertTrue(ia.size <= 1000 && ia.size > 600);
      // check all values are present
//      Set<Integer> presentValues = new HashSet<>();
      for (int i = 0; i < 1000; i++) {
        int v = ks[i];
        assertTrue(NIL != ia.findKey(v));
//        presentValues.add(v);
      }
      
//      System.out.println("debug IntArrayRBT before delete, number of present values is " + presentValues.size());
      
      // delete all values 
      Set<Integer> deletedValues = new HashSet<>();
      int localSize = ia.size();
      for (int i = 0; i < 1000; i++) {
        boolean wasDeleted = ia.deleteKey(ks[i]);
        if (wasDeleted) {
          assertTrue(deletedValues.add(ks[i]));
          localSize --;
          assertEquals(ia.size(), localSize);
          checkStructure(ia);
        } else {
          assertTrue(deletedValues.contains(ks[i]));
//          if (!deletedValues.contains(ks[i])) {  // debugging
//            System.out.println("debug");
//            boolean found = ia.debugScanFor(ks[i]);
//            wasDeleted = ia.deleteKey(ks[i]);  // try again, see why not deleted
//          }
          assertTrue(deletedValues.contains(ks[i]));
        }
      }
      
//      it = ia.iterator();  // for debugging
//      Arrays.fill(iv,  -1);
//      iiv = 0;
//      while (it.hasNext()) {
//        iv[iiv++] = it.next();
//      }

      assertEquals(0, ia.size);
      System.gc();
      System.out.print(Runtime.getRuntime().freeMemory() + " ");
     
    }
    System.out.println("");
  }
}
