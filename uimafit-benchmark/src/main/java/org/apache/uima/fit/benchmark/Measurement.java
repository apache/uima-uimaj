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
package org.apache.uima.fit.benchmark;

public class Measurement {
  private final int run;
  private final long duration;
  private final Exception exception;

  public Measurement(int aRun, long aDuration) {
    run = aRun;
    duration = aDuration;
    exception = null;
  }

  public Measurement(int aRun, long aDuration, Exception aException) {
    exception = aException;
    run = aRun;
    duration = aDuration;
  }

  public int getRun() {
    return run;
  }

  public long getDuration() {
    return duration;
  }

  public Exception getException() {
    return exception;
  }

  public boolean failed() {
    return exception != null;
  }

  @Override
  public String toString() {
    if (failed()) {
      return "[" + run + ": FAIL]";
    } else {
      return "[" + run + ": " + duration + "]";
    }
  }
}