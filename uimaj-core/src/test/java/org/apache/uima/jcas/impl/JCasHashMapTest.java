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

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.TestCase;

import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.Utilities;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

public class JCasHashMapTest extends TestCase {
  static private class FakeTopType extends TOP_Type {
    public FakeTopType() {
      super();
    }    
  }
  
  static final TOP_Type FAKE_TOP_TYPE_INSTANCE = new FakeTopType(); 
  static final int SIZE = 20000;  // set > 2 million for cache avoidance timing tests
  static final long SEED = 12345;
  static Random r = new Random(SEED);
  static private int[] addrs = new int[SIZE];
  static int prev = 0;
  
  static {  
    // unique numbers
    for (int i = 0; i < SIZE; i++) { 
      addrs[i] = prev = prev + r.nextInt(14) + 1;
    }
    // shuffled
    for (int i = SIZE - 1; i >= 1; i--) {
      int ir = r.nextInt(i+1);
      int temp = addrs[i];
      addrs[i] = addrs[ir];
      addrs[ir] = temp;
    }
  }
   
  public void testBasic() {
    JCasHashMap m;

    for (int i = 1; i <= 128; i *= 2) {
      JCasHashMap.setDEFAULT_CONCURRENCY_LEVEL(i);
      // test default concurrency level adjusted down 
      m = new JCasHashMap(32 * i, true);
      assertEquals( i, m.getConcurrencyLevel());
      m = new JCasHashMap(16 * i, true);
      assertEquals(Math.max(1, i / 2), m.getConcurrencyLevel());
      
      //test capacity adjusted up
      m = new JCasHashMap(32 * i, true, i);
      assertEquals( 32 * i, m.getCapacity());
      m = new JCasHashMap(31 * i, true, i);
      assertEquals( 32 * i, m.getCapacity());
      m = new JCasHashMap(16 * i, true, i);
      assertEquals( 32 * i, m.getCapacity());
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
  
  public void testMultiThread() throws Exception {
    final Random random = new Random();
    int numberOfThreads = Utilities.numberOfCores;    
    System.out.format("test JCasHashMap with up to %d threads%n", numberOfThreads);

    
    for (int th = 2; th <= numberOfThreads; th *=2) {
      JCasHashMap.setDEFAULT_CONCURRENCY_LEVEL(th);
      final JCasHashMap m = new JCasHashMap(200, true); // true = do use cache   
      MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {
        
        public void call(int threadNumber, int repeatNumber, StringBuilder sb) {
          for (int k = 0; k < 4; k++) {
            for (int i = 0; i < SIZE / 4; i++) {
              final int key = addrs[random.nextInt(SIZE / 16)];
              FeatureStructureImpl fs = m.getReserve(key);
              if (null == fs) {
                m.put(new TOP(key, FAKE_TOP_TYPE_INSTANCE));
              }
            }
            try {
              Thread.sleep(0, random.nextInt(1000));
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
  //        System.out.println(sb.toString());
        }
      };  
      MultiThreadUtils.tstMultiThread("JCasHashMapTest",  numberOfThreads,  10, run2isb,
          new Runnable() {
            public void run() {
              m.clear();
            }});
    }
  }

  public void testMultiThreadCompare() throws Exception {
    final Random random = new Random();
    int numberOfThreads = Utilities.numberOfCores;    
    System.out.format("test JCasHashMap with compare with up to %d threads%n", numberOfThreads);

    final ConcurrentMap<Integer, FeatureStructureImpl> check = 
        new ConcurrentHashMap<Integer, FeatureStructureImpl>(SIZE, .5F, numberOfThreads * 2);
    
    for (int th = 2; th <= numberOfThreads; th *= 2) {
      JCasHashMap.setDEFAULT_CONCURRENCY_LEVEL(th);
      final JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 
  
      MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {
        
        public void call(int threadNumber, int repeatNumber, StringBuilder sb) {
          for (int k = 0; k < 4; k++) {
            for (int i = 0; i < SIZE / 4; i++) {
              final int key = addrs[random.nextInt(SIZE / 16)];
              FeatureStructureImpl fs = m.getReserve(key);
              if (null == fs) {
                fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
                check.put(key, fs);  
                m.put(fs);
              } else {
                FeatureStructureImpl fscheck = check.get(key);
                if (fscheck == null || fscheck != fs) {
                  String msg = String.format("JCasHashMapTest miscompare, repeat=%,d, count=%,d key=%,d"
                      + ", checkKey=%s JCasHashMapKey=%,d",
                      k, i, key, (null == fscheck) ? "null" : Integer.toString(fscheck.getAddress()), fs.getAddress());
                  System.err.println(msg);
                  throw new RuntimeException(msg);
                }
              }
            }
            try {
              Thread.sleep(0, random.nextInt(1000));
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
  //        System.out.println(sb.toString());
        }
      };  
      MultiThreadUtils.tstMultiThread("JCasHashMapTest",  numberOfThreads,  10, run2isb, 
          new Runnable() {
            public void run() {
              check.clear();
              m.clear();
            }
      });
    }
  }
  /**
   * Create situation
   *   make a set of indexed fs instances, no JCas
   *   on multiple threads, simultaneously, attempt to get the jcas cover object for this
   *     one getReserve should succeed, but reserve, and the others should "wait".
   *     then put
   *     then the others should "wakeup" and return the same instance 
   *   
   * @throws Exception
   */
  public void testMultiThreadCollide() throws Exception {
    int numberOfThreads = Utilities.numberOfCores;
    if (numberOfThreads < 2) {
      return;
    }
    System.out.format("test JCasHashMap collide with up to %d threads%n", numberOfThreads);

    Thread thisThread = Thread.currentThread();
    final int subThreadPriority = thisThread.getPriority();
    thisThread.setPriority(subThreadPriority - 1);
    final MultiThreadUtils.ThreadM[] threads = new MultiThreadUtils.ThreadM[numberOfThreads];
    final JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 
    final Random r = new Random();  // used to sleep from 0 to 4 milliseconds
    final int hashKey = 15;
    final TOP fs = new TOP(hashKey, FAKE_TOP_TYPE_INSTANCE);
    final FeatureStructureImpl[] found = new FeatureStructureImpl[numberOfThreads];
    
    for (int i = 0; i < numberOfThreads; i++) {
      final int finalI = i;
      threads[i] = new MultiThreadUtils.ThreadM() {
            public void run() {
              while (true) {
                if (!MultiThreadUtils.wait4go(this)) {
                  break;
                }
                MultiThreadUtils.sleep(r.nextInt(500000)); // 0-500 microseconds 
                found[finalI] = m.getReserve(hashKey);
              }
            }
          };
      threads[i].setPriority(subThreadPriority);
      threads[i].start();
    }    

    for (int loopCount = 0; loopCount < 10; loopCount ++) {
      System.out.println("  JCasHashMap collide loop count is " + loopCount);
  
      // create threads and start them
      for (int th = 2; th <= numberOfThreads; th *= 2) {
        JCasHashMap.setDEFAULT_CONCURRENCY_LEVEL(th);
        Arrays.fill(found,  null);
        m.clear();
        
        MultiThreadUtils.kickOffThreads(threads);  

        Thread.sleep(20); 
        // verify that one thread finished, others are waiting, because of the reserve.
        // this assumes that all the threads got to run.
        int numberWaiting = 0;
        int threadFinished = -1;
        for (int i = 0; i < numberOfThreads; i++) {
          if (threads[i].state == MultiThreadUtils.THREAD_RUNNING) {
            numberWaiting ++;
          } else {
            threadFinished = i;
          }
        }
        
        assertEquals(numberOfThreads - 1, numberWaiting);  // expected 7 but was 8
        m.put(fs);
        found[threadFinished] = fs;
        
        MultiThreadUtils.waitForAllReady(threads);
   
//        // loop a few times to give enough time for the other threads to finish.
//        long startOfWait = System.currentTimeMillis();
//        while (System.currentTimeMillis() - startOfWait < 30000) { // wait up to 30 seconds in case of machine stall
//                  
//          // Attempt to insure we let the threads under test run in preference to this one       
//          Thread.sleep(20);   // imprecise.  Intent is to allow other thread that was waiting, to run
//                              // before this thread resumes.  Depends on thread priorities, but
//                              // multiple threads could be running at the same time.
//          
//          numberWaiting = 0;
//          for (int i = 0; i < numberOfThreads; i++) {
//            if (threads[i].state == MultiThreadUtils.THREAD_RUNNING) {
//              numberWaiting ++;
//            }
//          }
//          if (numberWaiting == 0) {
//            break;
//          }
//        }
        
//        assertEquals(0, numberWaiting);  // if not 0 by now, something is likely wrong, or machine stalled more than 30 seconds
  //      System.out.format("JCasHashMapTest collide,  found = %s%n", intList(found));
        for (FeatureStructureImpl f : found) {
          if (f != fs) {
            System.err.format("JCasHashMapTest miscompare fs = %s,  f = %s%n", fs, (f == null) ? "null" : f);
          }
          assertTrue(f == fs);
        }
      }
    }
    
    MultiThreadUtils.terminateThreads(threads);
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
//      TOP fs = new TOP(7 * i, NULL_TOP_TYPE_INSTANCE);
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
    assertTrue(m.getApproximateSize() == 0);
       
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
        m.put(fs);
//      }
    }
    
    assertEquals(m.getApproximateSize(), n);
    
    System.out.format("time for v1 %,d is %,d ms%n",
        n, System.currentTimeMillis() - start);
    m.showHistogram();

  }
  
  private void arunCk(int n) {
    JCasHashMap m = new JCasHashMap(200, true); // true = do use cache
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
//        m.findEmptySlot(key);
        m.put(fs);
//      }
    }
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = (TOP) m.getReserve(key);
      if (fs == null) {  // for debugging
        System.out.println("stop");
      }
      assertTrue(null != fs);
    }

  }
  
  public void testGrowth() {
    System.out.println("JCasHashMapTest growth");
    for (int th = 2; th <= 128; th *= 2) {
      JCasHashMap.setDEFAULT_CONCURRENCY_LEVEL(th);
      double loadfactor = .6;  // from JCasHashMap impl
      int sub_capacity = 32;   // from JCasHashMap impl
      int subs = th;
      int agg_capacity = subs * sub_capacity;
      JCasHashMap m = new JCasHashMap(agg_capacity, true); // true = do use cache 
      assertEquals(0, m.getApproximateSize());
      assertEquals(agg_capacity, m.getCapacity());
       
      int switchpoint = (int)Math.floor(agg_capacity * loadfactor);
      fill(switchpoint, m);
      System.out.print("JCasHashMapTest: after fill to switch point: ");
      assertTrue(checkSubsCapacity(m, sub_capacity));
      System.out.print("JCasHashMapTest: after 1 past switch point:  ");
      m.put(new TOP(addrs[switchpoint + 1], null));
      assertTrue(checkSubsCapacity(m, sub_capacity));
      
      m.clear();
      System.out.print("JCasHashMapTest: after clear:                ");
      assertTrue(checkSubsCapacity(m, sub_capacity));
  
  
      fill(switchpoint, m);
      System.out.print("JCasHashMapTest: after fill to switch point: ");
      assertTrue(checkSubsCapacity(m, sub_capacity));
      m.put(new TOP(addrs[switchpoint + 1], null));
      System.out.print("JCasHashMapTest: after 1 past switch point:  ");
      assertTrue(checkSubsCapacity(m, sub_capacity));
  
      m.clear();  // size is above switchpoint, so no shrinkage
      System.out.print("JCasHashMapTest: after clear (size above sp: ");
      assertTrue(checkSubsCapacity(m, sub_capacity));
      m.clear();  // size is 0, so first time shrinkage a possibility
      System.out.print("JCasHashMapTest: clear (size below sp:       ");
      assertTrue(checkSubsCapacity(m, sub_capacity)); // but we don't shrink on first time
      m.clear(); 
      System.out.print("JCasHashMapTest: clear (size below 2nd time: ");
      assertTrue(checkSubsCapacity(m, sub_capacity, sub_capacity));  // but we do on second time
//      m.clear(); 
//      System.out.print("JCasHashMapTest: clear (size below 3rd time: ");
//      assertTrue(checkSubsCapacity(m, sub_capacity, sub_capacity));
//      m.clear(); 
//      System.out.print("JCasHashMapTest: clear (size below 4th time: ");
//      assertTrue(checkSubsCapacity(m, sub_capacity, sub_capacity));  // don't shrink below minimum
    }
  }

  private boolean checkSubsCapacity(JCasHashMap m, int v) {
    return checkSubsCapacity(m, v, v * 2);
  }
  
  // check: the subMaps should be mostly of size v, but some might be of size v*2.
  private boolean checkSubsCapacity(JCasHashMap m, int v, int v2) {
    int[] caps = m.getCapacities();
    for (int i : caps) {
      if (i == v || i == v2 ) {
        continue;
      }
      System.err.format("expected %d or %d, but got %s%n", v, v2, intList(caps));
      return false;
    }
    System.out.format("%s%n", intListPm(caps, v));
    return true;
  }
  
  private String intList(int[] a) {
    StringBuilder sb = new StringBuilder();
    for (int i : a) {
      sb.append(i).append(", ");
    }
    return sb.toString();
  }
  
  private String intListPm(int[] a, int smaller) {
    StringBuilder sb = new StringBuilder(a.length);
    for (int i : a) {
      sb.append(i == smaller ? '.' : '+');
    }
    return sb.toString();
  }
  
  private String intList(FeatureStructureImpl[] a) {
    StringBuilder sb = new StringBuilder();
    for (FeatureStructureImpl i : a) {
      sb.append(i == null ? "null" : i.getAddress()).append(", ");
    }
    return sb.toString();
  }
  
  private void fill (int n, JCasHashMap m) {
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
      m.put(fs);
//      System.out.format("JCasHashMapTest fill %s%n",  intList(m.getCapacities()));
    }
  }
}
