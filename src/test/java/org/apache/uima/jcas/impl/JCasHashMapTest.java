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

package org.apache.uima.jcas.impl;

import java.util.Random;

import junit.framework.TestCase;

import org.apache.uima.jcas.cas.TOP;

/**
 * 
 *
 */
public class JCasHashMapTest extends TestCase {
  
  static final int SIZE = 20000;  // set > 2 million for cache avoidance timing tests
  static final long SEED = 12345;
  static Random r = new Random(SEED);
  static private int[] addrs = new int[SIZE];
  static int prev = 0;
  
  static {   
    for (int i = 0; i < SIZE; i++) { 
      addrs[i] = prev = prev + r.nextInt(14) + 1;
    }
    for (int i = SIZE - 1; i >= 1; i--) {
      int ir = r.nextInt(i+1);
      int temp = addrs[i];
      addrs[i] = addrs[ir];
      addrs[ir] = temp;
    }
  }

  public void testWithPerf()  {
    
    for (int i = 0; i <  5; i++ ) {
      arun(SIZE);
    }
    
    arunCk(SIZE);

//    for (int i = 0; i < 50; i++ ) {
//      arun2(2000000);
//    }

  }
  
//  private void arun2(int n) {
//    JCasHashMap2 m = new JCasHashMap2(200, true); 
//    assertTrue(m.size() == 0);
//    assertTrue(m.getbitsMask() == 0x000000ff);
//    
//    JCas jcas = null;
//    
//    long start = System.currentTimeMillis();
//    for (int i = 0; i < n; i++) {
//      TOP fs = new TOP(7 * i, null);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.putAtLastProbeAddr(fs);
//      }
//    }
//    System.out.format("time for v2 %,d is %,d ms%n",
//        n, System.currentTimeMillis() - start);
//    m.showHistogram();
//
//  }
   
  private void arun(int n) {
    JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 
    assertTrue(m.size() == 0);
       
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, null);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
        m.put(fs);
//      }
    }
    System.out.format("time for v1 %,d is %,d ms%n",
        n, System.currentTimeMillis() - start);
    m.showHistogram();

  }
  
  private void arunCk(int n) {
    JCasHashMap m = new JCasHashMap(200, true); // true = do use cache
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, null);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
//        m.findEmptySlot(key);
        m.put(fs);
//      }
    }
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = (TOP) m.get(key);
      if (fs == null) {
        System.out.println("stop");
      }
      assertTrue(null != fs);
    }

  }
  
  public void testGrowth() {
    JCasHashMap m = new JCasHashMap(64, true); // true = do use cache 
    assertTrue(m.size() == 0);
     
    fill32(m);
    assertTrue(m.getbitsMask() == 63);
    m.put(new TOP(addrs[32], null));
    assertTrue(m.getbitsMask() == 127);
    
    m.clear();
    assertTrue(m.getbitsMask() == 127);

    fill32(m);
    assertTrue(m.getbitsMask() == 127);
    m.put(new TOP(addrs[32], null));
    assertTrue(m.getbitsMask() == 127);

    m.clear();  // size is 33, so no shrinkage
    assertTrue(m.getbitsMask() == 127);
    m.clear();  // size is 0, so first time shrinkage a possibility
    assertTrue(m.getbitsMask() == 127);  // but we don't shrink on first time
    m.clear(); 
    assertTrue(m.getbitsMask() == 63);  // but we do on second time
    m.clear(); 
    assertTrue(m.getbitsMask() == 63);  
    m.clear(); 
    assertTrue(m.getbitsMask() == 63);  // don't shrink below minimum

    
  }

  private void fill32 (JCasHashMap m) {
    for (int i = 0; i < 32; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, null);
      m.put(fs);
    }
  }
}
