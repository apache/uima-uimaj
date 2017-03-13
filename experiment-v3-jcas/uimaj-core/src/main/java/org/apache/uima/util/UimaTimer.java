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

package org.apache.uima.util;

/**
 * Interface for a timer, used to collect performance statistics for UIMA components. A default
 * Timer implementation can be obtained by calling {@link org.apache.uima.UIMAFramework#newTimer()}.
 * A Timer implementation can also be specified in a
 * {@link org.apache.uima.collection.CollectionProcessingEngine} descriptor.
 * 
 * 
 */
public interface UimaTimer extends java.io.Serializable {
  /**
   * Starts the timer.
   * 
   * @return the current time in milliseconds
   */
  public long startIt();

  /**
   * Stops the timer.
   * 
   * @return the current time in milliseconds
   */
  public long stopIt();

  /**
   * Gets the time between the last call to stopIt() and the last call to startIt().
   * 
   * @return the duration in milliseconds
   */
  public long getDuration();

  /**
   * Gets the current time in seconds.
   * 
   * @return the current time in seconds
   */
  public long getTimeInSecs();

  /**
   * Gets the current time in milliseconds.
   * 
   * @return the current time in milliseconds
   */
  public long getTimeInMillis();

  /**
   * Gets the current time in microseconds.
   * 
   * @return the current time in microseconds
   */
  public long getTimeInMicros();

  /**
   * Gets the timer resolution in milliseconds.
   * 
   * @return the timer resolution in milliseconds
   */
  public int getResolution();
}
