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

package org.apache.uima.cas.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.OrderedFsSet_array;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

import junit.framework.TestCase;

public class OrderedFsSet_array_Test extends TestCase {

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  static int SZ = 23;
  
  static final Random r = new Random();
  static long seed =  r.nextLong();
 //     -4709156741850323232L;
                     // 1783099358091571349L;
  static {
    r.setSeed(seed);
    System.out.println("OrderedFsSet_array_test random seed: " + seed);
  }
    
  CASImpl cas;
  JCas jcas;
  FSIndexRepositoryImpl ir;
  private Comparator<TOP> comparatorWithID;
  private Comparator<TOP> comparatorWithoutID;
  private Annotation[] as = new Annotation[Integer.highestOneBit(SZ) << 1];
  
  private OrderedFsSet_array<TOP> a;   
  
  protected void setUp() throws Exception {
    
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    jcas = cas.getJCas();
    ir = (FSIndexRepositoryImpl) cas.getIndexRepository();
    comparatorWithID = ir.getAnnotationFsComparatorWithId(); 
    comparatorWithoutID = ir.getAnnotationFsComparatorWithoutId();
    a = new OrderedFsSet_array(comparatorWithID, comparatorWithoutID);
    
  }
  
  /**
   * work with 16-32 elements
   * 
   * Do a lot of inserts / removes in patterns:
   *   remove n, insert n in random order
   * 
   */
  public void testInsert() {
    int i = 0;
//    for (; i < 100; i++) {  //enable for lots of iterationss, disable for normal test case
      seed = r.nextLong();
      r.setSeed(seed);
      System.out.println("OrderedFsSet_array_test i: " + i + ", seed: " + seed);
      insert1(i);
//    }
  }
  
  private void insert1(int iter) {    
    
    //prefill
    for (int i = 0; i < SZ; i++) {
      as[i] = new Annotation(jcas, i, i + 200);
    }
    a = new OrderedFsSet_array(comparatorWithID, comparatorWithoutID);
    
    add(3, SZ);    
    a.size();
    
    add(0, 3);
    
    a.size();  // force batch add
        
    rr(0, 3);
    rr(1, 3);
        
    for (int i = 0; i < 100_000; i++) {
      removeAndReinsertRandom(i);
//      if ((i % 100000) == 0) {
//        System.out.println("random testing OrderedFsSet_array, count: " + i);
//      }
    }

  }
  
  private void add(int ... is) {
    for (int i : is) {
      a.add(as[i], comparatorWithID);
    }
  }
  
  private void add(int start, int end) {
    for (int i = start; i < end; i++) {
      a.add(as[i], comparatorWithID);
    }
  }
  
  private void rmv(int ... is) {
    for (int i : is) {
      a.remove(as[i]);
    }
  }
  
  private void rmv(int start, int end) {
    for (int i = start; i < end; i++) {
      a.remove(as[i]);
    }
  }

  //debug 87  
  private void removeAndReinsertRandom(int iteration) {
    int n_remove = r.nextInt(SZ);
    if (n_remove == 0) return; 
//    System.out.println(n_remove);
    int[] rmvd_i = r.ints(0, SZ - 1).distinct().limit(n_remove).toArray();
    int[] adds_i = shuffle(rmvd_i);
//    if (iteration == 12685) {
//      System.out.println("debug 12685");
//    }
    rr(rmvd_i, adds_i);
  }
  
  private void vall() {
    int i = 0;
    for (TOP fs : a) {
      assertTrue(as[i++] == fs);
//      if (fs != as[i++]) {
//        System.out.println("debug mismatch");
//      }
    }
//    TOP[] cc = a.getInternalArrayDebug();
//    for (int i = 0; i < SZ; i++) {
//      if (cc[i] == null) {
//        System.out.println("debug found null");
//      }
//    }    
  }
  
  private void rr(int start, int end) {
    rmv(start, end);
    add(start, end);
    a.size();
    vall();
  }
  
  private void rr(int[] rmv, int[] add) {
    for (int i : rmv) {
      a.remove(as[i]);
    }
    int splt = r.nextInt(add.length) + 1;
    
    for (int i = 0; i < splt; i++) {
      a.add(as[add[i]], comparatorWithID);
    }
    a.size(); 
    
    for (int i = splt; i < add.length; i++) {
      a.add(as[add[i]], comparatorWithID);
    }
    a.size();
    
    vall();
  }
  
  private int[] shuffle(int[] a) {
    int[] b = a.clone();
    for (int i = 0; i < b.length; i++) {
      int j = r.nextInt(b.length);  
      int tmp = b[i];
      b[i] = b[j];
      b[j] = tmp;
    }
    return b;
  }
}
