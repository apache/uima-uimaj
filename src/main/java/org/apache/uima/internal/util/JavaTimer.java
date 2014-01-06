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

import org.apache.uima.util.UimaTimer;

/**
 * Simple implementation of {@link UimaTimer} using {@link System#currentTimeMillis()}.
 * 
 * 
 */
public class JavaTimer implements UimaTimer {
  
  private static final long serialVersionUID = 4644107369628471363L;

  private long start = 0;

  private long end = 0;

  // starts the timer
  public long startIt() {
    start = System.currentTimeMillis();
    return start;
  }

  // ends the timer
  public long stopIt() {
    end = System.currentTimeMillis();
    return end;
  }

  public int getResolution() {
    return 10;
  }

  // returns duration (in ms) between start() and end() calls
  public long getDuration() {
    return (end - start);
  }

  public long getTimeInSecs() {
    return (getTime() / 1000);
  }

  public long getTimeInMillis() {
    return getTime();
  }

  public long getTimeInMicros() {
    return System.currentTimeMillis() * 1000;
  }

  private long getTime() {
    return System.currentTimeMillis();
  }

}
