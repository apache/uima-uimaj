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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.SynchPoint;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;

/**
 * Runing in a seperate thread creates a checkpoint file at predefined intervals.
 * 
 */
public class Checkpoint implements Runnable {
  private String fileName = null;

  private volatile boolean stop = false;  // volatile may be buggy in some JVMs apparently
                                          // consider changing to use synch

  private long checkpointFrequency = 3000;

  /**
   * @GuardedBy(lockForPause)
   */
  private boolean pause = false;

  private final Object lockForPause = new Object();

  // private boolean isRunning = false;
  private BaseCPMImpl cpm = null;

  private String synchPointFileName = null;

  /**
   * Initialize the checkpoint with a reference to controlling cpe, the file where the checkpoint is
   * to be stored, and the frequency of checkpoints.
   * 
   * @param aCpm
   * @param aFilename
   * @param aCheckpointFrequency
   */
  public Checkpoint(BaseCPMImpl aCpm, String aFilename, long aCheckpointFrequency) {
    fileName = aFilename;
    int fExtPos = fileName.indexOf('.');
    if (fExtPos > -1) {
      synchPointFileName = fileName.substring(0, fExtPos) + "_synchPoint.xml";
    }
    cpm = aCpm;
    checkpointFrequency = aCheckpointFrequency;
  }

  /**
   * Start the thread
   * 
   */
  public void start() {
    new Thread(this).start();
  }

  /**
   * Stops the checkpoint thread
   * 
   */
  public void stop() {
    stop = true;
    // isRunning = false;
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_checkpoint_thread__INFO",
              new Object[] { Thread.currentThread().getName() });

    }
  }

  /**
   * Starts the checkpoint thread and runs until the cpe tells it to stop
   */
  public void run() {
    Thread.currentThread().setName("CPM Checkpoint");

    // isRunning = true;
    while (!stop) {
      synchronized (lockForPause) {
        if (pause) {
          try {
            lockForPause.wait();
          } catch (InterruptedException e) {
          }
        }
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_checkpoint__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      doCheckpoint();

      try {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_sleep__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        Thread.sleep(checkpointFrequency);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_wakeup__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      } catch (Exception e) {
      }
    }
  }

  /**
   * Deletes checkpoint file from the filesystem
   * 
   */
  public void delete() {
    try {
      File checkpointFile = new File(fileName);
      checkpointFile.delete();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Pauses checkpoint thread
   * 
   */
  public void pause() {
    synchronized (lockForPause) {
      pause = true;
    }
  }

  /**
   * Resumes checkpoint thread
   * 
   */
  public void resume() {
    synchronized (lockForPause) {
      if (pause) {
        lockForPause.notifyAll();
        pause = false;
      }
    }
  }

  /**
   * Serializes checkpoint information to disk. It retrieves data to checkpoint from the CPEEngine.
   * 
   */
  public void doCheckpoint() {
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_checkpoint__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      // 02/08/05 Checkpoint has been broken up into two files. One containing the
      // ProcessTrace saved as binary object, and second containing the SynchPoint
      // saved as xml.
      rename(fileName);
      rename(synchPointFileName);
      // This stream is for the ProcessTrace part of the Checkppoint
      FileOutputStream out = null;
      // This stream is for the SynchPoint part of the Checkpoint
      FileOutputStream synchPointOut = null;
      ObjectOutputStream s = null;

      try {
        out = new FileOutputStream(fileName);
        synchPointOut = new FileOutputStream(synchPointFileName);
        s = new ObjectOutputStream(out);
        SynchPoint synchPoint = cpm.getSynchPoint();

        ProcessTrace pTrace = cpm.getPerformanceReport();
        CheckpointData targetToSave = null;
        if (pTrace != null) {
          if (synchPoint != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_checkpoint_with_synchpoint__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            targetToSave = new CheckpointData(pTrace, synchPoint);
          } else {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_checkpoint_with_pt__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            targetToSave = new CheckpointData(pTrace);
          }
          synchronized (targetToSave) {
            s.writeObject(targetToSave);
            s.flush();
            if (synchPoint != null) {
              String xmlSynchPoint = synchPoint.serializeToXML();
              synchPointOut.write(xmlSynchPoint.getBytes());
              synchPointOut.flush();
            }
          }
        }
      } catch (Exception e) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_exception_when_checkpointing__FINEST",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      } finally {
        if (out != null) {
          try {
            out.close();
            s.close();

          } catch (Exception e) {
          }
        }
        if (synchPointOut != null) {
          try {
            synchPointOut.close();
          } catch (Exception e) {
          }
        }
      }

    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_exception_when_checkpointing__FINEST",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });
    }
  }

  /**
   * Renames previous checkpoint file.
   * 
   * @param aFilename -
   *          checkpoint file to rename
   */
  public void rename(String aFilename) {
    File currentFile = new File(aFilename);
    File backupFile = new File(aFilename + ".prev");
    currentFile.renameTo(backupFile);
  }

  public static void printStats(ProcessTrace prT) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST,
              "\n\t\t\t----------------------------------------");
      UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST, "\t\t\t\t PERFORMANCE REPORT ");
      UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST,
              "\t\t\t----------------------------------------\n");
    }
    // get the list of events from the processTrace
    List eveList = prT.getEvents();
    printEveList(eveList, 0);
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST,
              "_________________________________________________________________\n");
    }
  }

  /**
   * Prints the list of Process Events in the order that they were produced.
   * 
   * @param lst
   *          List of ProcessEvent
   * @param tCnt
   *          depth of this List in the Process Trace hierarchy
   */
  public static void printEveList(List lst, int tCnt) {
    String compNameS;
    String typeS;
    int dur;
    int totDur;
    List subEveList;
    String tabS = "";
    int tabCnt = tCnt;
    for (int j = 0; j < tabCnt; j++) {
      tabS = tabS + "\t";
    }
    for (int i = 0; i < lst.size(); i++) {
      ProcessTraceEvent prEvent = (ProcessTraceEvent) lst.get(i);
      compNameS = prEvent.getComponentName();
      typeS = prEvent.getType();
      dur = prEvent.getDurationExcludingSubEvents();
      totDur = prEvent.getDuration();
      subEveList = prEvent.getSubEvents();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(Checkpoint.class).log(
                Level.FINEST,
                tabS + "COMPONENT : " + compNameS + "\tTYPE : " + typeS + "\tDescription : "
                        + prEvent.getDescription());
        UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST,
                tabS + "TOTAL_TIME : " + totDur + "\tTIME_EXCLUDING_SUBEVENTS : " + dur);
      }
      if (subEveList != null) {
        printEveList(subEveList, (tabCnt + 1));
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(Checkpoint.class).log(Level.FINEST, " ");
      }
    }
  }

  /**
   * Returns true if configured checkpoinjt file exists on disk
   * 
   * @return - true if file exists, false otherwise
   */
  public boolean exists() {
    try {
      return new File(fileName).exists();
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Retrieves the checkpoint from the filesystem.
   * 
   * @return - desirialized object containing recovery information.
   * @throws IOException -
   */
  public synchronized Object restoreFromCheckpoint() throws IOException {
    ObjectInputStream stream = null;
    FileInputStream synchPointStream = null;
    try {
      File file = new File(fileName);
      Object anObject = null;
      // The checkpoint consists of two seperate files. One that holds a binary representation of
      // checkpoint and the other as xml representation. The xml representation consists of only
      // part of the checkpoint, namely the SynchPoint. This data needed to be exposed as text to
      // a human administrator to manually change it. Requirement from the WF project.
      if (file.exists()) {

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_restoring_from_checkpoint__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        FileInputStream in = new FileInputStream(file);
        stream = new ObjectInputStream(in);
        if (stream != null) {
          anObject = stream.readObject();
          if (anObject != null && anObject instanceof CheckpointData) {
            ProcessTrace processTrace = ((CheckpointData) anObject).getProcessTrace();
            printStats(processTrace);
          }
        }
      }
      file = new File(synchPointFileName);
      // Read the synchpoint from the filesystem.
      SynchPoint synchPoint = null;
      if (file.exists()) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_synchpoint_from_file__FINEST",
                  new Object[] { Thread.currentThread().getName(), synchPointFileName });
        }
        synchPointStream = new FileInputStream(file);
        if (synchPointStream != null) {
          // Use the SynchPoint object retrieved above. Its internal data should be
          // overwritten during deserialization done below. Its just a convenience to
          // reuse the same object that was saved as part of a checkpoint.
          if (anObject != null && anObject instanceof CheckpointData) {
            synchPoint = ((CheckpointData) anObject).getSynchPoint();
            if (synchPoint != null) {
              synchPoint.deserialize(synchPointStream);
            }
          }
        }
      }

      return anObject;
    } catch (EOFException e) {
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (stream != null) {
        stream.close();
      }
      if (synchPointStream != null) {
        synchPointStream.close();
      }
    }
    return null;
  }
}
