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

import org.apache.uima.internal.util.MultiThreadUtils.ThreadControl;
import org.junit.jupiter.api.Test;

/**
 * Helper class for running multi-core tests.
 * 
 * Runs a passed-in "runnable" inside loops for repetition and for multi-threads The threads run at
 * the same time, and there's a "join" style wait at the end. Any exceptions thrown by the threads
 * are reflected back.
 *
 */
public class MultiThreadUtilsTest {

  @Test
  public void testMultiThreadTimers() {

    final int numberOfTimers = 50;
    final Timer[] timers = new Timer[numberOfTimers];

    final ThreadControl[][] threadState = new ThreadControl[50][1];

    final int[] repeatNumber = { 0 };

    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfTimers; i++) {
      final int finalI = i;
      threadState[i][0] = ThreadControl.WAIT;

      final Timer timer = new Timer();
      timers[i] = timer;
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          // System.out.format("%nTimer %d Popped%n", finalI);
        }
      }, 1);
    }
    System.out.format(
            "Time to create and start %d timers with separate Timer instances: %,d microsec%n",
            numberOfTimers, (System.nanoTime() - startTime) / 1000);
  }

  @Test
  public void testMultiThreadExecutors() {

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final int numberOfTimers = 50;
    final ScheduledFuture<?>[] timers = new ScheduledFuture<?>[numberOfTimers];

    long startTime = System.nanoTime();
    for (int i = 0; i < numberOfTimers; i++) {
      final int finalI = i;

      final ScheduledFuture<?> timer = scheduler.schedule(new Runnable() {

        @Override
        public void run() {
          // System.out.format("%nScheduled Timer %d Popped%n", finalI);
        }
      }, 1, TimeUnit.MILLISECONDS);
      timers[i] = timer;
    }
    System.out.format(
            "Time to create and start %d timers using a single, reused thread and ScheduledExecutorService: %,d microsec%n",
            numberOfTimers, (System.nanoTime() - startTime) / 1000);
  }
}
