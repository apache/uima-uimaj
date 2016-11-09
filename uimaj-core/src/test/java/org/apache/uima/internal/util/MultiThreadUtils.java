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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
  
  public final static boolean debug = false;
  
  public static interface Run2isb {
    public void call(int threadNumber, int repeatNumber, StringBuilder sb) throws Exception;
  }
  
  public static Runnable emptyReset = new Runnable() {public void run() {}};

  // also serves as a lock
  
  private static enum ThreadControl {
    WAIT,   // causes test thread to wait, is the initial state 
    RUN,    // causes test thread to run; when run is done, thread goes back to waiting and sets global entry in thread array to WAIT
    TERMINATE,  // causes test thread to finish 
  }
    
  private static final AtomicInteger numberRunning = new AtomicInteger(0);
  
  private static final AtomicInteger numberOfExceptions = new AtomicInteger(0);
  
  public void testMultiThreadTimers() {
       
    final int numberOfTimers = 50;
    final Timer[] timers = new Timer[numberOfTimers];

    final ThreadControl[][] threadState = new ThreadControl[50][1];
        
    final int[] repeatNumber = {0};
    
    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfTimers; i++) {
      final int finalI = i;
      threadState[i][0] = ThreadControl.WAIT;

      final Timer timer = new Timer();
      timers[i] = timer;
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
//          System.out.format("%nTimer %d Popped%n", finalI);
        }
      }, 1);
    }
    System.out.format("Time to create and start %d timers with separate Timer instances: %,d microsec%n", numberOfTimers, (System.nanoTime() - startTime) / 1000);
  }
  
  public static void testMultiThreadExecutors() {
       
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final int numberOfTimers = 50;
    final ScheduledFuture<?> [] timers = new ScheduledFuture<?> [numberOfTimers];

    
    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfTimers; i++) {
      final int finalI = i;

      final ScheduledFuture<?> timer = scheduler.schedule(new Runnable() {
        
        @Override
        public void run() {
//          System.out.format("%nScheduled Timer %d Popped%n", finalI);         
        }
      }, 1, TimeUnit.MILLISECONDS);
      timers[i] = timer;
    }
    System.out.format("Time to create and start %d timers using a single, reused thread and ScheduledExecutorService: %,d microsec%n", numberOfTimers, (System.nanoTime() - startTime) / 1000);
  } 
  

  
  /**
   * On a 2 GHz i7 running Windows, it seems to take about 1 millisecond to create and start up a thread.
   * 
   * To get maximum likelyhood of threads all starting together, the threads are all started, but then they wait
   * for a "go" signal.  Each thread has a "threadControl" value, which is accessed under a specific lock for the thread
   * to insure memory synchronization; the threads have the states above.
   * 
   * To reduce the overhead, the logic is:
   *   a) make the threads and start them.  They go to their wait point.
   *   b) for the repeat loop:
   *       b1) release all threads from wait point
   *       b2) wait for all threads to reach their wait point again (at the end of their processing) 
   *       
   *       b3) repeat b1 and b2 for the repeat count.
   *       
   *   c) signal all threads to terminate.
   *   
   *   d) do the join wait for everything to finish.
   *   
   * @param name root name for messages and thread ids
   * @param numberOfThreads number of threads
   * @param repeats number of times to repeat the whole test
   * @param run2isb the Callable to run in multiple threads, called with thread # and a string builder for messages 
   * @param beforeRepeatArg a Runnable or null, to run before each outer "repeat".
   * @throws Exception
   */
  public static void tstMultiThread(
      final String name,  // name root for messages and thread ids
      int numberOfThreads, 
      int repeats, 
      final Run2isb run2isb,    // the Callable that is run in a thread, passed in also are the thread # and a string builder for messages
      final Runnable beforeRepeatArg  // called before every repeat, use null or MultiThreadUtils.emptyReset if not wanted.
      ) throws Exception {
       
    final Runnable beforeRepeat = (null == beforeRepeatArg) ? emptyReset : beforeRepeatArg;
    final Thread[] threads = new Thread[numberOfThreads];

    final Throwable[] thrown = new Throwable[1];
    final ThreadControl[][] threadState = new ThreadControl[numberOfThreads][1];
    
    thrown[0] = null;
    
    final int[] repeatNumber = {0};
    
    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfThreads; i++) {
      final int finalI = i;
      threadState[i][0] = ThreadControl.WAIT;
      
      // We make the runnable inside this loop to capture the thread number
      Runnable runnable = new Runnable() {         
        public void run() {
          // sb is for debugging; it's passed into the runnable which can choose to print it or not
          StringBuilder sb = new StringBuilder(80);
        
          while (true) {
            synchronized (threadState[finalI]) {
              if (threadState[finalI][0] == ThreadControl.TERMINATE) {
                return;
              }
              while (threadState[finalI][0] == ThreadControl.WAIT) {
                try {
                  threadState[finalI].wait();
                } catch (InterruptedException e) {
                }
                if (threadState[finalI][0] == ThreadControl.TERMINATE) {
                  return;
                }
              }
              assertEquals(ThreadControl.RUN, threadState[finalI][0]);
            }
                        
            try {
              assertTrue(numberRunning.get() > 0);
              sb.append(name).append(", thread ").append(finalI).append(' ');
//              System.out.println(sb.toString());
              run2isb.call(finalI, repeatNumber[0], sb);
            } catch (Throwable e) {
              System.err.format("%s: Runnable threw exception %s%n", name, e.getMessage());
              e.printStackTrace(System.err);
              numberOfExceptions.incrementAndGet();
              synchronized (numberOfExceptions) {
                numberOfExceptions.notify();
              }
              thrown[0] = e;
//              synchronized (threadState[finalI]) {
//                threadState[finalI][0] = ThreadControl.EXCEPTION;
//              }
            }
            synchronized(threadState[finalI]) {
              threadState[finalI][0] = ThreadControl.WAIT;
            }
            numberRunning.decrementAndGet();
            synchronized (numberRunning) {
              numberRunning.notify();              
            }
          }
        }};
      threads[i] = new Thread(runnable);
      threads[i].setName(name + " Thread " + i);
      threads[i].setPriority(Thread.NORM_PRIORITY - 1);
      threads[i].start();
    }
    if (debug) {
      System.out.format("Time to create %d threads: %,d microsec%n", numberOfThreads, (System.nanoTime() - startTime) / 1000);
    }

    for (int r = 0; r < repeats; r++) {
      beforeRepeat.run();
      
      repeatNumber[0] = r;
      assertTrue(numberRunning.get() == 0);
      assertTrue(numberOfExceptions.get() == 0);
      
      startTime = System.nanoTime();       
      
      // release all threads from wait point
      for (int i = 0; i < numberOfThreads; i++) {
        synchronized (threadState[i]) {
          assertEquals(ThreadControl.WAIT, threadState[i][0]);
          threadState[i][0] = ThreadControl.RUN;
          numberRunning.incrementAndGet();
          threadState[i].notify();
        }
      }
      if (debug) {
        System.out.format("repeat %,d Time to release %d threads from wait: %,d microsec%n", r, numberOfThreads, (System.nanoTime() - startTime) / 1000);
      }
      
      // wait for all threads to return to wait state
      
      synchronized (numberRunning) {
        while (numberRunning.get() > 0) {
          numberRunning.wait();
        }          
      }
      for (int i = 0; i < numberOfThreads; i++) {
       synchronized (threadState[i]) {
          assertEquals(ThreadControl.WAIT, threadState[i][0]);          
        }
      }
    }  // end of repeat loop
    
    for (int i = 0; i < numberOfThreads; i++) {
      synchronized (threadState[i]) {
        threadState[i][0] = ThreadControl.TERMINATE;
        threadState[i].notify();
      }
    }
    
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

  /*
   * Thread management utilities
   */
  
  public final static int THREAD_ABOUT_TO_WAIT = 1;
  public final static int THREAD_RUNNING = 2;
  private final static int THREAD_TERMINATE = 3;
  private final static int THREAD_ABOUT_TO_RUN = 4;
  
  public static class ThreadM extends Thread {   
    public ThreadM(Runnable runnable) {
      super(runnable);
    }
    
    public ThreadM() {
      super();
    }

    public volatile int state = 0;
  }
  
  public static void waitForAllReady(ThreadM[] threads) {
    for (ThreadM thread : threads) {
      wait4wait(thread);
    }    
  }
  
//  public static void waitForAllTerminate(ThreadM[] threads) {
//    for (Thread t : threads) {
//      while (true) {
//        if (t.getState() == Thread.State.TERMINATED) {
//          break;
//        }
//        sleep(10000);
//      }
//    }
//  }

  public static void sleep(int nanoSeconds) {
    try {
      Thread.sleep(0, nanoSeconds); 
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void wait4wait(ThreadM t) {
    while (true) {
      if (t.state == THREAD_ABOUT_TO_WAIT && t.getState() == Thread.State.WAITING) {
        return;
      }
      sleep(1000);
    }
  }
  /**
   * 
   * @param t the thread
   * @return true if running, false if time to terminate
   */
  public static boolean wait4go(ThreadM t) {
    synchronized(t) {
      try {
        t.state = THREAD_ABOUT_TO_WAIT;
        t.wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    if (t.state == MultiThreadUtils.THREAD_TERMINATE) {
      return false;
    }
    t.state = MultiThreadUtils.THREAD_RUNNING;
    return true;
  }
  
  public static void kickOffThreads(ThreadM[] threads) {
    waitForAllReady(threads);
    // after all threads in wait state, rapidly release them
    for (ThreadM t : threads) {
      synchronized (t) {
        t.state = THREAD_ABOUT_TO_RUN;
        t.notify();
//        System.out.println("Debug notifying thread " + thread.getName());
      }
    } 
    // insure all threads started
    for (ThreadM t : threads) {
      wait4start(t);
    }  
  }
  
  private static void wait4start(ThreadM t) {
    while (true) {
      if (t.state != THREAD_ABOUT_TO_RUN || t.getState() == Thread.State.RUNNABLE) {
        return;
      }
      sleep(1000);
    }
  }
  
  public static void terminateThreads(ThreadM[] threads) {
    for (ThreadM t : threads) {
      wait4wait(t);
      t.state = THREAD_TERMINATE;
      synchronized (t) {
        t.notify();
      }
    }    
  }

}
