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
 * Provides the message level constants for logging and tracing
 */
public class Level {
  /** level value */
  private int level;

  /** level name */
  private String levelText;

  /** level value for level "OFF" */
  public static final int OFF_INT = Integer.MAX_VALUE;

  /** level value for level "SEVERE" */
  public static final int SEVERE_INT = 70000;

  /** level value for level "WARNING" */
  public static final int WARNING_INT = 60000;

  /** level value for level "INFO" */
  public static final int INFO_INT = 50000;

  /** level value for level "CONFIG" */
  public static final int CONFIG_INT = 40000;

  /** level value for level "FINE" */
  public static final int FINE_INT = 30000;

  /** level value for level "FINER" */
  public static final int FINER_INT = 20000;

  /** level value for level "FINEST" */
  public static final int FINEST_INT = 10000;

  /** level value for level "ALL" */
  public static final int ALL_INT = Integer.MIN_VALUE;

  /** level value for level "ERROR" */
  public static final int ERROR_INT = SEVERE_INT;
  /** level value for level "WARN" */
  public static final int WARN_INT = WARNING_INT;
  /** level value for level "DEBUG" */
  public static final int DEBUG_INT = FINE_INT;
  /** level value for level "TRACE" */
  public static final int TRACE_INT = FINER_INT;

  /** message level "OFF" */
  public static final Level OFF = new Level(OFF_INT, "OFF");

  /** message level "SEVERE" */
  public static final Level SEVERE = new Level(SEVERE_INT, "SEVERE");
  public static final Level ERROR = SEVERE;

  /** message level "WARNING" */
  public static final Level WARNING = new Level(WARNING_INT, "WARNING");
  public static final Level WARN = WARNING;

  /** message level "INFO" */
  public static final Level INFO = new Level(INFO_INT, "INFO");

  /** message level "CONFIG" */
  public static final Level CONFIG = new Level(CONFIG_INT, "CONFIG");

  /** message level "FINE" */
  public static final Level FINE = new Level(FINE_INT, "FINE");
  public static final Level DEBUG = FINE;

  /** message level "FINER" */
  public static final Level FINER = new Level(FINER_INT, "FINER");
  public static final Level TRACE = FINER;

  /** message level "FINEST" */
  public static final Level FINEST = new Level(FINEST_INT, "FINEST");

  /** message level "ALL" */
  public static final Level ALL = new Level(ALL_INT, "ALL");

  /**
   * Instantiate a new level object.
   * 
   * @param level
   *          level value
   * @param levelText
   *          level name
   */
  protected Level(int level, String levelText) {
    // set level value
    this.level = level;
    // set level name
    this.levelText = levelText;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    // check if current object o is an instance of Level
    if (o instanceof Level r) {
      // return true if both levels are the same
      return (level == r.level);
    } else // if o is no instance of Level return false
    {
      return false;
    }
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return level;
  }

  /**
   * method isOn() returns true if the message level is not OFF
   * 
   * @return boolean true if the message level is less than OFF
   */
  public boolean isOn() {
    return level < OFF_INT;
  }

  /**
   * method isGreaterOrEqual() returns true if the argument level is greater or equal to the
   * specified level.
   * 
   * @param r
   *          argument level passed to the method
   * 
   * @return boolean - true if argument level is greater of equal to the specified level
   */
  public boolean isGreaterOrEqual(Level r) {
    if (r == null)
      return false;
    else
      return level <= r.level;
  }

  /**
   * Returns the string representation of this priority.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public final String toString() {
    return levelText;
  }

  /**
   * Returns the integer representation of this level.
   * 
   * @return int - level value
   */
  public final int toInteger() {
    return level;
  }

}
