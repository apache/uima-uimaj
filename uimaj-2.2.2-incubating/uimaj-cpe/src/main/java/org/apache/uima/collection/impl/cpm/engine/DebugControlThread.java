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

package org.apache.uima.collection.impl.cpm.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Level;

public class DebugControlThread implements Runnable {
  private final static String NOTFOUND = "NOT-FOUND";

  private String fileName = null;

  private volatile boolean stop = false;

  private int checkpointFrequency = 3000;

  // This variable guarded by lockForPause
  private boolean pause = false;

  private final Object lockForPause = new Object();

  // private boolean isRunning = false;
  private CPMEngine cpm = null;

  public DebugControlThread(CPMEngine aCpm, String aFilename, int aCheckpointFrequency) {
    cpm = aCpm;
    fileName = aFilename;
    checkpointFrequency = aCheckpointFrequency;
  }

  public void start() throws RuntimeException {
    if (fileName == null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_checkpoint_target_not_defined__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      throw new RuntimeException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_target_checkpoint_not_defined__WARNING", new Object[] { Thread
                      .currentThread().getName() }));
    }
    if (cpm == null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_cpm_instance__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      throw new RuntimeException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_cpm__WARNING",
              new Object[] { Thread.currentThread().getName() }));
    } else {
      new Thread(this).start();
    }
  }

  public void stop() {
    stop = true;
    // isRunning = false;
    doCheckpoint();
  }

  public void run() {
    // isRunning = true;
    while (!stop && cpm.isRunning()) {
      synchronized (lockForPause) {
        if (pause) {
          try {
            lockForPause.wait();
          } catch (InterruptedException e) {
          }
        }
      }
      String command = doCheckpoint();
      if (NOTFOUND.equals(command) == false) {
        interpretAndExecuteCommand(command);
      }
      try {
        Thread.sleep(checkpointFrequency);
      } catch (Exception e) {
      }
    }
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_thread_terminating__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * 
   * 
   * @param aCommand
   */
  private void interpretAndExecuteCommand(String aCommand) {
    if (aCommand == null) {
      return; // nothing to do
    }
    try {
      StringTokenizer st = new StringTokenizer(aCommand, " ");
      while (st.hasMoreTokens()) {
        String cmd = aCommand.trim().toLowerCase();
        if ("die".equalsIgnoreCase(cmd)) {
          cpm.stopIt();
          deleteCheckpoint();
          break;
        } else if ("halt".equalsIgnoreCase(cmd)) {
          cpm.stopIt();
          break;
        } else if (aCommand != null && aCommand.trim().startsWith("-D")) {

          int pos = 0;
          String value;
          String key;
          if ((pos = aCommand.indexOf("=")) > -1) {
            key = aCommand.substring(2, pos);
            value = aCommand.substring(pos + 1);
            if (value.trim().equalsIgnoreCase("off") && System.getProperties().containsKey(key)) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                        this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_disabling_key__FINEST",
                        new Object[] { Thread.currentThread().getName(), key, value });

              }
              System.getProperties().remove(key);
            } else if (value.trim().equalsIgnoreCase("on")
                    && !System.getProperties().containsKey(key)) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                        this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_enabling_key__FINEST",
                        new Object[] { Thread.currentThread().getName(), key, value });
              }
              System.getProperties().put(key, value);
            }
          }
          break;
        }
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void deleteCheckpoint() {
    try {
      File inF = null;
      inF = new File(fileName);
      if (inF.exists()) {
        inF.delete();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void pause() {
    pause = true;
  }

  public void resume() {
    synchronized (lockForPause) {
      if (pause) {
        try {
          lockForPause.notify();
        } catch (Exception e) {
        }
        pause = false;
      }
    }

  }

  public String doCheckpoint() {
    File inF = null;
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_access_control_file__FINEST",
                new Object[] { Thread.currentThread().getName(), fileName });
      }
      inF = new File(fileName);

      return FileUtils.file2String(inF);

    } catch (FileNotFoundException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_access_control_file_not_found__FINEST",
                new Object[] { Thread.currentThread().getName(), fileName });
      }
      return NOTFOUND;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        inF.delete();
      } catch (Exception e) {
      }
    }
    return null;
  }

  public boolean exists() {
    try {
      new File(fileName);
      return true;
    } catch (Exception e) {
    }
    return false;
  }

}
