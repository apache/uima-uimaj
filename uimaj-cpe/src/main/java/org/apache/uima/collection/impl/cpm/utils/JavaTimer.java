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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.Serializable;

import org.apache.uima.util.UimaTimer;


/**
 * The Class JavaTimer.
 *
 * @deprecated replaced by {@link UimaTimer}
 */

@Deprecated
public class JavaTimer implements Timer, Serializable {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 5399135137398839124L;

  /** The start. */
  private long start = 0;

  /** The end. */
  private long end = 0;

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#start()
   */
  // starts the time
  @Override
  public void start() {
    start = System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#end()
   */
  // ends the timer
  @Override
  public void end() {
    end = System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#getResolution()
   */
  @Override
  public long getResolution() {
    return 10;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#getDuration()
   */
  // returns duration (in ms) between start() and end() calls
  @Override
  public long getDuration() {
    return (end - start);
  }

  /**
   * Gets the time.
   *
   * @return the time
   */
  private synchronized long getTime() {
    return System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#getTimeInSecs()
   */
  @Override
  public synchronized long getTimeInSecs() {
    return (getTime() / 1000);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#getTimeInMillis()
   */
  @Override
  public synchronized long getTimeInMillis() {
    return getTime();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.impl.cpm.utils.Timer#getTimeInMicros()
   */
  @Override
  public synchronized long getTimeInMicros() {
    return System.currentTimeMillis() * 1000;
  }

}
