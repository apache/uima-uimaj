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

import static java.io.ObjectInputFilter.allowFilter;
import static java.io.ObjectInputFilter.rejectUndecidedClass;
import static java.io.ObjectInputFilter.Status.UNDECIDED;
import static org.apache.uima.util.Level.FINEST;
import static org.apache.uima.util.Level.INFO;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.SynchPoint;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Running in a separate thread creates a checkpoint file at predefined intervals.
 */
public class Checkpoint implements Runnable {

  static final String PROP_CPE_CHECKPOINT_SERIAL_FILTER = "uima.cpe.checkpoint.serial_filter";

  /** The file name. */
  private final String fileName;

  /** The stop. */
  // volatile may be buggy in some JVMs apparently
  // consider changing to use synch
  private volatile boolean stop = false;

  /** The checkpoint frequency. */
  private final long checkpointFrequency;

  /**
   * The pause.
   *
   * @GuardedBy(lockForPause)
   */
  private boolean pause = false;

  /** The lock for pause. */
  private final Object lockForPause = new Object();

  /** The cpm. */
  // private boolean isRunning = false;
  private final BaseCPMImpl cpm;

  /** The synch point file name. */
  private final String synchPointFileName;

  /**
   * Initialize the checkpoint with a reference to controlling cpe, the file where the checkpoint is
   * to be stored, and the frequency of checkpoints.
   *
   * @param aCpm
   *          the a cpm
   * @param aFilename
   *          the a filename
   * @param aCheckpointFrequency
   *          the a checkpoint frequency
   */
  public Checkpoint(BaseCPMImpl aCpm, String aFilename, long aCheckpointFrequency) {
    fileName = aFilename;
    int fExtPos = fileName.indexOf('.');
    if (fExtPos > -1) {
      synchPointFileName = fileName.substring(0, fExtPos) + "_synchPoint.xml";
    } else {
      synchPointFileName = null;
    }
    cpm = aCpm;
    checkpointFrequency = aCheckpointFrequency;
  }

  /**
   * Stops the checkpoint thread.
   */
  public void stop() {
    stop = true;
    // isRunning = false;
    if (UIMAFramework.getLogger().isInfoEnabled()) {
      UIMAFramework.getLogger(this.getClass()).logrb(INFO, this.getClass().getName(), "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_checkpoint_thread__INFO",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Starts the checkpoint thread and runs until the cpe tells it to stop.
   */
  @Override
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
      if (UIMAFramework.getLogger().isLoggable(FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(), "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_checkpoint__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      doCheckpoint();

      try {
        if (UIMAFramework.getLogger().isLoggable(FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_sleep__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        Thread.sleep(checkpointFrequency);
        if (UIMAFramework.getLogger().isLoggable(FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_wakeup__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      } catch (Exception e) {
      }
    }
  }

  /**
   * Deletes checkpoint file from the filesystem.
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
   * Pauses checkpoint thread.
   */
  public void pause() {
    synchronized (lockForPause) {
      pause = true;
    }
  }

  /**
   * Resumes checkpoint thread.
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
      if (UIMAFramework.getLogger().isLoggable(FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(), "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_checkpoint__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      // 02/08/05 Checkpoint has been broken up into two files. One containing the
      // ProcessTrace saved as binary object, and second containing the SynchPoint
      // saved as xml.
      rename(fileName);
      rename(synchPointFileName);
      // This stream is for the ProcessTrace part of the Checkppoint
      // This stream is for the SynchPoint part of the Checkpoint
      ObjectOutputStream s = null;

      try (FileOutputStream out = new FileOutputStream(fileName);
              FileOutputStream synchPointOut = new FileOutputStream(synchPointFileName)) {
        s = new ObjectOutputStream(out);
        SynchPoint synchPoint = cpm.getSynchPoint();

        ProcessTrace pTrace = cpm.getPerformanceReport();
        CheckpointData targetToSave = null;
        if (pTrace != null) {
          if (synchPoint != null) {
            if (UIMAFramework.getLogger().isLoggable(FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(),
                      "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_checkpoint_with_synchpoint__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            targetToSave = new CheckpointData(pTrace, synchPoint);
          } else {
            if (UIMAFramework.getLogger().isLoggable(FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(),
                      "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
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
        UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(), "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception_when_checkpointing__FINEST",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }

    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(), "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception_when_checkpointing__FINEST",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });
    }
  }

  /**
   * Renames previous checkpoint file.
   * 
   * @param aFilename
   *          - checkpoint file to rename
   */
  public void rename(String aFilename) {
    File currentFile = new File(aFilename);
    File backupFile = new File(aFilename + ".prev");
    currentFile.renameTo(backupFile);
  }

  /**
   * Prints the stats.
   *
   * @param prT
   *          the pr T
   */
  public static void printStats(ProcessTrace prT) {
    if (UIMAFramework.getLogger().isLoggable(FINEST)) {
      UIMAFramework.getLogger(Checkpoint.class).log(FINEST,
              "\n\t\t\t----------------------------------------");
      UIMAFramework.getLogger(Checkpoint.class).log(FINEST, "\t\t\t\t PERFORMANCE REPORT ");
      UIMAFramework.getLogger(Checkpoint.class).log(FINEST,
              "\t\t\t----------------------------------------\n");
    }
    // get the list of events from the processTrace
    List<ProcessTraceEvent> eveList = prT.getEvents();
    printEveList(eveList, 0);
    if (UIMAFramework.getLogger().isLoggable(FINEST)) {
      UIMAFramework.getLogger(Checkpoint.class).log(FINEST,
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
  public static void printEveList(List<ProcessTraceEvent> lst, int tCnt) {
    String compNameS;
    String typeS;
    int dur;
    int totDur;
    List<ProcessTraceEvent> subEveList;
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
      if (UIMAFramework.getLogger().isLoggable(FINEST)) {
        UIMAFramework.getLogger(Checkpoint.class).log(FINEST, tabS + "COMPONENT : " + compNameS
                + "\tTYPE : " + typeS + "\tDescription : " + prEvent.getDescription());
        UIMAFramework.getLogger(Checkpoint.class).log(FINEST,
                tabS + "TOTAL_TIME : " + totDur + "\tTIME_EXCLUDING_SUBEVENTS : " + dur);
      }
      if (subEveList != null) {
        printEveList(subEveList, (tabCnt + 1));
      }
      if (UIMAFramework.getLogger().isLoggable(FINEST)) {
        UIMAFramework.getLogger(Checkpoint.class).log(FINEST, " ");
      }
    }
  }

  /**
   * Returns true if configured checkpoinjt file exists on disk.
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
   * @return deserialized object containing recovery information.
   * @throws IOException
   *           -
   */
  public synchronized Object restoreFromCheckpoint() throws IOException {
    try {
      File file = new File(fileName);
      Object anObject = null;
      // The checkpoint consists of two seperate files. One that holds a binary representation of
      // checkpoint and the other as xml representation. The xml representation consists of only
      // part of the checkpoint, namely the SynchPoint. This data needed to be exposed as text to
      // a human administrator to manually change it. Requirement from the WF project.
      if (file.exists()) {
        if (UIMAFramework.getLogger().isLoggable(FINEST)) {
          UIMAFramework.getLogger(getClass()).logrb(FINEST, this.getClass().getName(), "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_restoring_from_checkpoint__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        try (var stream = new FileInputStream(file)) {
          anObject = deserializeCheckpoint(stream);
          if (anObject instanceof CheckpointData) {
            ProcessTrace processTrace = ((CheckpointData) anObject).getProcessTrace();
            printStats(processTrace);
          }
        }
      }
      file = new File(synchPointFileName);
      // Read the synchpoint from the filesystem.
      SynchPoint synchPoint = null;
      if (file.exists()) {
        if (UIMAFramework.getLogger().isLoggable(FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_synchpoint_from_file__FINEST",
                  new Object[] { Thread.currentThread().getName(), synchPointFileName });
        }

        try (var synchPointStream = new FileInputStream(file)) {
          // Use the SynchPoint object retrieved above. Its internal data should be
          // overwritten during deserialization done below. Its just a convenience to
          // reuse the same object that was saved as part of a checkpoint.
          if (anObject instanceof CheckpointData) {
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
    }
    return null;
  }

  static CheckpointData deserializeCheckpoint(InputStream stream)
          throws IOException, ClassNotFoundException {
    var safeList = Set.of(CheckpointData.class, ProcessTrace.class, ProcessTrace_impl.class,
            SynchPoint.class);

    var filter = allowFilter(safeList::contains, UNDECIDED);
    var ois = new ObjectInputStream(stream);
    var serialFilterPropertyValue = System.getProperty(PROP_CPE_CHECKPOINT_SERIAL_FILTER);
    if (serialFilterPropertyValue != null) {
      var serialFilter = ObjectInputFilter.Config.createFilter(serialFilterPropertyValue);
      filter = ObjectInputFilter.merge(filter, serialFilter);
    }
    filter = rejectUndecidedClass(filter);
    ois.setObjectInputFilter(filter);
    return (CheckpointData) ois.readObject();
  }
}
