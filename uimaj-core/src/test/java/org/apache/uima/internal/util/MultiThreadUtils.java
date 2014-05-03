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

import junit.framework.TestCase;

/**
 * Helper class for running multi-core tests.
 * 
 * Runs a passed-in "runnable" inside loops for repetition and for multi-threads
 *   The threads run at the same time, and there's a "join" style wait at the end.
 *   Any exceptions thrown by the threads are reflected back.
 *
 */
public class MultiThreadUtils extends TestCase {

  public final static int PROCESSORS = Runtime.getRuntime().availableProcessors();
  
  public static interface Run2isb {
    public void call(int threadNumber, int repeatNumber, StringBuilder sb) throws Exception;
  }
  
  public static Runnable emptyReset = new Runnable() {public void run() {}};
  
  // needed because
  //   this class extends TestCase (in order to have access to assertTrue, etc
  //   this causes the junit runner to warn if there are no "test"s in this class
  public void testDummy() {}
  
  public static void tstMultiThread(
      final String name, 
      int numberOfThreads, 
      int repeats, 
      final Run2isb run2isb, 
      final Runnable beforeRepeat) throws Exception {
    Thread[] threads = new Thread[numberOfThreads];

    final Throwable[] thrown = new Throwable[1];
    thrown[0] = null;
    
    for (int r = 0; r < repeats; r++) {
      beforeRepeat.run();
      final int finalR = r;
      try {
        for (int i = 0; i < numberOfThreads; i++) {
          final int finalI = i;
          threads[i] = new Thread(new Runnable() {
            
            public void run() {
              // sb is for debugging; it's passed into the runnable which can choose to print it or not
              StringBuilder sb = new StringBuilder(80);
              try {
                sb.append(name).append(", thread ").append(finalI).append(' ');
                run2isb.call(finalI, finalR, sb);
              } catch (Throwable e) {
                System.err.format("%s: Runnable threw exception %s%n", name, e.getMessage());
                e.printStackTrace(System.err);
                thrown[0] = e;
                throw new RuntimeException(e); // silly, just causes thread to end
              }
            }} );
          threads[i].setName(name + " Thread " + i);
          threads[i].setPriority(Thread.NORM_PRIORITY - 1);
          threads[i].start();
        }
      
        for (int i = 0; i < numberOfThreads; i++) {
          try {
            if (thrown[0] != null) {
              assertTrue(false);
            }
            threads[i].join();
            if (thrown[0] != null) {
              thrown[0].printStackTrace();
              assertTrue(false);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
            assertTrue(false);
          }
        }
      } finally {
        // cleanup
        // interrupt live threads
        // wait for all threads to terminate
        for (Thread thread : threads) {
          if (thread.isAlive()) {
            thread.interrupt();
          }
        }
        for (Thread thread : threads) {
          if (thread.isAlive()) {
            thread.join();
          }
        }
      }
    }   
  }

}
