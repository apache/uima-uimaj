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
  public final static int OFF_INT = Integer.MAX_VALUE;

  /** level value for level "SEVERE" */
  public final static int SEVERE_INT = 70000;

  /** level value for level "WARNING" */
  public final static int WARNING_INT = 60000;

  /** level value for level "INFO" */
  public final static int INFO_INT = 50000;

  /** level value for level "CONFIG" */
  public final static int CONFIG_INT = 40000;

  /** level value for level "FINE" */
  public final static int FINE_INT = 30000;

  /** level value for level "FINER" */
  public final static int FINER_INT = 20000;

  /** level value for level "FINEST" */
  public final static int FINEST_INT = 10000;

  /** level value for level "ALL" */
  public final static int ALL_INT = Integer.MIN_VALUE;

  /** message level "OFF" */
  final static public Level OFF = new Level(OFF_INT, "OFF");

  /** message level "SEVERE" */
  final static public Level SEVERE = new Level(SEVERE_INT, "SEVERE");

  /** message level "WARNING" */
  final static public Level WARNING = new Level(WARNING_INT, "WARNING");

  /** message level "INFO" */
  final static public Level INFO = new Level(INFO_INT, "INFO");

  /** message level "CONFIG" */
  final static public Level CONFIG = new Level(CONFIG_INT, "CONFIG");

  /** message level "FINE" */
  final static public Level FINE = new Level(FINE_INT, "FINE");

  /** message level "FINER" */
  final static public Level FINER = new Level(FINER_INT, "FINER");

  /** message level "FINEST" */
  final static public Level FINEST = new Level(FINEST_INT, "FINEST");

  /** message level "ALL" */
  final static public Level ALL = new Level(ALL_INT, "ALL");

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
  public boolean equals(Object o) {
    // check if current object o is an instance of Level
    if (o instanceof Level) {
      // cast Object to Level
      Level r = (Level) o;
      // return true if both levels are the same
      return (this.level == r.level);
    } else // if o is no instance of Level return false
    {
      return false;
    }
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return this.level;
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
