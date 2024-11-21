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

package org.apache.uima.tools.cpm;

/**
 * The Class ElapsedTimeFormatter.
 */
public final class ElapsedTimeFormatter {

  /**
   * Format.
   *
   * @param totalSecs
   *          the total secs
   * @return the string
   */
  public static String format(int totalSecs) {
    int hours = 0;
    int mins = totalSecs / 60;
    int secs = totalSecs % 60;
    if (mins > 60) {
      hours = mins / 60;
      mins = mins % 60;
    }

    String hoursString;
    String minsString;
    String secsString;
    if (hours < 10)
      hoursString = "0" + String.valueOf(hours);
    else
      hoursString = String.valueOf(hours);

    if (mins < 10)
      minsString = "0" + String.valueOf(mins);
    else
      minsString = String.valueOf(mins);

    if (secs < 10)
      secsString = "0" + String.valueOf(secs);
    else
      secsString = String.valueOf(secs);

    return hoursString + ":" + minsString + ":" + secsString;
  }
}
