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

/**
 * Simple timer class.
 * 
 * 
 */
public final class Timer {

  private long time;

  private long start;

  private boolean isRunning;

  /**
   * Create a new timer. The new timer will NOT be started automatically. You need to invoke
   * {@link #start() start()} explicitly.
   */
  public Timer() {
    super();
    this.time = 0;
    this.start = 0;
    this.isRunning = false;
  }

  /**
   * (Re)start the timer. If the timer has already been started, does nothing.
   * 
   * @return <code>false</code> iff the timer is already running.
   */
  public boolean start() {
    if (this.isRunning) {
      return false;
    }
    this.isRunning = true;
    this.start = System.currentTimeMillis();
    return true;
  }

  /**
   * Stop the timer.
   * 
   * @return <code>false</code> iff the timer is not running.
   */
  public boolean stop() {
    final long end = System.currentTimeMillis();
    if (!this.isRunning) {
      return false;
    }
    this.isRunning = false;
    this.time += end - this.start;
    return true;
  }

  /**
   * Reset the timer to 0. The timer must be stopped before it can be reset.
   * 
   * @return <code>false</code> iff the timer is currently running.
   */
  public boolean reset() {
    if (this.isRunning) {
      return false;
    }
    this.time = 0;
    return true;
  }

  /**
   * Get the currently accumulated time. This method may be called while the timer is running, or
   * after it has been stopped.
   * 
   * @return The duration in milliseconds.
   */
  public long getTime() {
    if (this.isRunning) {
      stop();
      final long rt = this.time;
      start();
      return rt;
    }
    return this.time;
  }

  /**
   * Get the accumulated time as a time span object.
   * 
   * @see #getTime()
   * @return A time span object for the current value of the timer.
   */
  public TimeSpan getTimeSpan() {
    return new TimeSpan(getTime());
  }

}
