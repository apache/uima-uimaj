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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Return a java Properties object containing environment variables.
 * 
 */
public class SystemEnvReader {
  /**
   * Returns system environment settings. It uses OS specific system command to obtain variables.
   * 
   * @return Java Properties object containing environment variables.
   * @throws Throwable
   *           If any error occurred.
   */
  public static Properties getEnvVars() throws Throwable {
    Process p = null;
    Properties envVars = new Properties();
    Runtime r = Runtime.getRuntime();
    String OS = System.getProperty("os.name").toLowerCase();
    if (OS.indexOf("windows 9") > -1) {
      p = r.exec("command.com /c set");
    } else if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows 2000") > -1)
            || (OS.indexOf("windows xp") > -1)) {
      p = r.exec("cmd.exe /c set");
    } else {
      // our last hope, we assume Unix
      p = r.exec("env");
    }
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        int idx = line.indexOf('=');
        if (idx < 0)
          continue;
        String key = line.substring(0, idx);
        String value = (idx < line.length() - 1) ? line.substring(idx + 1) : "";
        envVars.setProperty(key, value);
      }
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (Exception e) {
          // do nothing
        }
      }
    }
    return envVars;
  }

}
