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

package org.apache.uima.collection.impl.cpm;

/**
 * Object containing checkpoint configuration.
 * 
 * 
 */
public class CheckpointConfig {
  private String checkpointFile = "";

  private long frequency = -1;

  private boolean timeBased = false;

  private boolean countBased = false;

  private boolean millis = false;

  private boolean seconds = false;

  private boolean minutes = false;

  /**
   * Initilizes instance with a file where the checkpoint will be stored and checkpoint frequency.
   * 
   * @param aChpFile -
   *          path to the checkpoint file
   * @param aFrequency -
   *          frequency of checkpoints
   */
  public CheckpointConfig(String aChpFile, String aFrequency) {
    checkpointFile = aChpFile;
    determineFrequency(aFrequency);
  }

  /**
   * Parses the frequency String
   * 
   * @param aFrequency
   */
  private void determineFrequency(String aFrequency) {
    try {
      frequency = Long.parseLong(aFrequency);
      countBased = true;
    } catch (NumberFormatException nfe) {
      String number = "";
      String unit = "";

      for (int i = 0; i < aFrequency.length(); i++) {
        if (Character.isDigit(aFrequency.charAt(i))) {
          number += aFrequency.charAt(i);
        } else {
          unit += aFrequency.charAt(i);
        }
      }
      if (unit.toLowerCase().equals("m")) {
        minutes = true;
      } else if (unit.toLowerCase().equals("ms")) {
        millis = true;
      } else if (unit.toLowerCase().equals("s")) {
        seconds = true;
      }
      frequency = Long.parseLong(number);
      timeBased = true;
    }
  }

  /**
   * Returns true if frequency is count based
   * 
   * @return - true if count based frequency
   */
  public boolean isCountBased() {
    return countBased;
  }

  /**
   * Returns checkpoint frequency
   * 
   * @return - frequency of checkpoints
   */
  public long getFrequency() {
    return frequency;
  }

  /**
   * Returns checkpoint frequency resolution in millis
   * 
   * @return - frequency in millis
   */
  public boolean isMillis() {
    return millis;
  }

  /**
   * Returns checkpoint frequency resolution in minutes
   * 
   * @return - frequency in minutes
   */
  public boolean isMinutes() {
    return minutes;
  }

  /**
   * Retusn checkpoint frequency in seconds
   * 
   * @return - frequency in seconds
   */
  public boolean isSeconds() {
    return seconds;
  }

  /**
   * Returns true if the checkpoint frequency is in terms of time
   * 
   * @return - true if time based frequency
   */
  public boolean isTimeBased() {
    return timeBased;
  }

  /**
   * Returns the path to a file containing checkpoint
   * 
   * @return - file path
   */
  public String getCheckpointFile() {
    return checkpointFile;
  }

}
