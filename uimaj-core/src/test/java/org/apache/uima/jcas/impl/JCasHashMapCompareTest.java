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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.Utilities;
import org.apache.uima.jcas.cas.TOP;

import junit.framework.TestCase;

/**
 * Run this as a single test with yourkit, and look at the retained storage for both maps.
 * 
 *   Java 8 test: Concurrent Hash Map impl showed ~2.25 MB * 8 (concurrency level)
 *                JCasHashmap              showed ~0.835 MB * 8 
 *
 */
public class JCasHashMapCompareTest extends TestCase {
    
  private static final long rm =  0x5deece66dL;

  private static int sizeOfTest = 1024 * 8;  
//  private static final int SIZEm1 = SIZE - 1;
//  private JCasHashMap jhm;
  private ConcurrentMap<Integer, TOP> concurrentMap;
  
  private long custAcc = 0;
  private long custNbr = 0;

  
  public void testComp() throws Exception {
    Thread.sleep(0000);  // set non-zero to delay so you can get yourkit tooling hooked up, if using yourkit
    int numberOfThreads =  Utilities.numberOfCores; 
    numberOfThreads = Math.min(8, Utilities.nextHigherPowerOf2(numberOfThreads));  // avoid too big slowdown on giant machines.
    System.out.format("test JCasHashMapComp with %d threads%n", numberOfThreads);
    for (int i = 0; i < 3; i++) {
//      for (int j = 0; j < 10000; j++) {
//      for (int j = 0; j < 10000; j++) {
    runCustom(numberOfThreads); 
//      }
//      for (int j = 0; j < 10000; j++) {
    runConCur(numberOfThreads);
//      }
//      }
    runCustom(numberOfThreads*2);
    runConCur(numberOfThreads*2);
    runCustom(numberOfThreads*4);
    runConCur(numberOfThreads*4);
    
//    stats("custom", runCustom(numberOfThreads));  // not accurate, use yourkit retained size instead
//    stats("concur", runConCur(numberOfThreads));
    Set<Integer> ints = new HashSet<>();
    for (Entry<Integer, TOP> e : concurrentMap.entrySet()) {
      assertFalse(ints.contains(Integer.valueOf(e.getKey())));
      assertEquals(e.getValue()._id(), (int)(e.getKey()));
      ints.add(e.getKey());
    }
    }
//    System.out.println("Found " + i);
    
    // launch yourkit profiler and look at retained sizes for both
//    Thread.sleep(1000000);
  }
  
  private int runConCur(int numberOfThreads) throws Exception {
    final ConcurrentMap<Integer, TOP> m =
        new ConcurrentHashMap<>(200, 0.75F, numberOfThreads);
    concurrentMap = m;
    
    final int numberOfWaiters = numberOfThreads*2;
    final Object[] waiters = new Object[numberOfWaiters];
    for (int i = 0; i < numberOfWaiters; i++) {
      waiters[i] = new Object();
    }
    MultiThreadUtils.Run2isb run2isb= new MultiThreadUtils.Run2isb() {
      
      public void call(int threadNumber, int repeatNumber, StringBuilder sb) {
//        int founds = 0, puts = 0;
        for (int i = 0; i < sizeOfTest*threadNumber; i++) {
          final int key = hash(i, threadNumber) / 2;
          final Object waiter = waiters[key & (numberOfWaiters - 1)];
          TOP newFs = TOP._createSearchKey(key);
          TOP fs = m.putIfAbsent(key, newFs);
//          while (fs != null && fs._isJCasHashMapReserve()) {
//            // someone else reserved this
//
//            // wait for notify
//            synchronized (waiter) {
//              fs = m.get(key);
//              if (fs._isJCasHashMapReserve()) {
//                try {
//                  waiter.wait();
//                } catch (InterruptedException e) {
//                }
//              }
//            }
//          }
//            
////          TOP fs = m.get(key);
//          if (null == fs) {
////            puts ++;
//            TOP prev = m.put(key,  TOP._createSearchKey(key));
//            if (prev._isJCasHashMapReserve()) {
//              synchronized (waiter) {
//                waiter.notifyAll();
//              }
//            }
////              puts --;  // someone beat us 
////              founds ++;
//          }
//          
        } // end of for loop
////        System.out.println("concur Puts = " + puts + ", founds = " + founds);
      }  
    };  
    long start = System.currentTimeMillis();
    MultiThreadUtils.tstMultiThread("JCasHashMapTestCompConcur",  numberOfThreads, 10, run2isb,
        new Runnable() {
          public void run() {
            m.clear();
        }});
    System.out.format("JCasCompTest - using ConcurrentHashMap, threads = %d, time = %,f seconds%n", numberOfThreads, ((double)(System.currentTimeMillis() - start)) / 1000.d);
    return m.size();
  }
  
  private int runCustom(int numberOfThreads) throws Exception {
    final JCasHashMap m = new JCasHashMap(256); // true = do use cache

    MultiThreadUtils.Run2isb run2isb= new MultiThreadUtils.Run2isb() {
      
      public void call(int threadNumber, int repeatNumber, StringBuilder sb) {
//        int founds = 0, puts = 0;
        for (int i = 0; i < sizeOfTest*threadNumber; i++) {
          final int key = hash(i, threadNumber) / 2;
          m.putIfAbsent(key, TOP::_createSearchKey);
//          if (key == 456551)
//            System.out.println("debug");
//          TOP fs = m.getReserve(key);

//          if (null == fs) {
//            puts++
//            m.put(TOP._createSearchKey(key));
//          } else {
//            founds ++;
//          }
        }
//        System.out.println("custom Puts = " + puts + ", founds = " + founds);
      }
    };  
    long start = System.currentTimeMillis();
    MultiThreadUtils.tstMultiThread("JCasHashMapTestComp0",  numberOfThreads,  10, run2isb,
        new Runnable() {
          public void run() {
            m.clear();
        }});
    long el = System.currentTimeMillis() - start;
    if (custNbr == 100) {
      custNbr = 1;
      custAcc = el;
    } else {
      custAcc += el;
      custNbr ++;
    }
    
    
    System.out.format("JCasCompTest - using JCasHashMap, threads = %d, time = %,f seconds avg = %,f%n", numberOfThreads, ((double)el) / 1000.d, (((double)custAcc)/custNbr) / 1000.d);
    m.showHistogram();
    return m.getApproximateSize();
  }
  
  // not accurate, use yourkit retained size instead
//  private void stats(String m, int size) {
//    for (int i = 0; i < 2; i++) {
//      System.gc();
//    }
//    Runtime r = Runtime.getRuntime();
//    long free =r.freeMemory();
//    long total = r.totalMemory();
//    System.out.format("JCasHashMapComp %s used = %,d  size = %,d%n",
//        m, total - free, size);
//  }

  private int hash(int i, int threadNumber) {    
    return (int)(((
                  (i + (threadNumber << 4)) * rm + 11 + 
                  (threadNumber << 1))
                 >>> 16) & (sizeOfTest*threadNumber - 1));
  }
}
