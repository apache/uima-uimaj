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

package org.apache.uima.collection.impl.cpm.container.deployer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;

/**
 * Sends kill -9 to a process
 */
public class FencedProcessReaper {

  
  public FencedProcessReaper() {
    super();

  }

  /**
   * When running on linux this method kill a process identified by a given PID.
   * 
   * @param aPid -
   *          process id to kill
   */
  public void killProcess(String aPid) {
    try {
      String cmd[] = new String[] { "kill", "-9", aPid };

      if (System.getProperty("os.name").equalsIgnoreCase("linux")) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_killing_process__FINEST",
                  new Object[] { Thread.currentThread().getName(), aPid });
        }
        Runtime.getRuntime().exec(cmd);
      }
    } catch (Exception e) {
      // non-fatal exception
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_killing_process_failed__WARNING",
              new Object[] { Thread.currentThread().getName(), aPid, e });
    }

  }
}
