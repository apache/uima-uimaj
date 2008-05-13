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

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;

/**
 * This component catches uncaught errors in the CPM. All critical threads in the CPM are part of
 * this ThreadGroup. If OutOfMemory Error is thrown this component is notified by the JVM and its
 * job is to notify registered listeners.
 * 
 */
public class CPMThreadGroup extends ThreadGroup {
  private ArrayList callbackListeners = null;

  private ProcessTrace procTr = null;

  /**
   * @param name
   */
  public CPMThreadGroup(String name) {
    super(name);
  }

  /**
   * @param parent -
   *          parent thread group
   * @param name -
   *          name of this thread group
   */
  public CPMThreadGroup(ThreadGroup parent, String name) {
    super(parent, name);
  }

  /**
   * Sets listeners to be used in notifications
   * 
   * @param aListenerList -
   *          list of registered listners
   */
  public void setListeners(ArrayList aListenerList) {
    callbackListeners = aListenerList;
  }

  public void setProcessTrace(ProcessTrace aProcessTrace) {
    procTr = aProcessTrace;
  }

  public void uncaughtException(Thread t, Throwable e) {
    System.out.println("ThreadGroup.uncaughtException()-Got Error");
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_unhandled_error__SEVERE",
              new Object[] { Thread.currentThread().getName(), e.getClass().getName() });

    }
    try {
      // Notify listeners
      for (int i = 0; callbackListeners != null && i < callbackListeners.size(); i++) {
        // System.out.println("ThreadGroup.uncaughtException()-Got Error - Notifying Listener");
        notifyListener((BaseStatusCallbackListener) callbackListeners.get(i), e);
      }

    } catch (Throwable tr) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), tr.getClass().getName() });
        tr.printStackTrace();
      }
    }
    // System.out.println("ThreadGroup.uncaughtException()-Done Handling Error");
  }

  private void notifyListener(BaseStatusCallbackListener aStatCL, Throwable e) {
    EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(procTr);
    enProcSt.addEventStatus("Process", "Failed", e);
    ((StatusCallbackListener) aStatCL).entityProcessComplete(null, enProcSt);
  }

  public void cleanup() {
    callbackListeners = null;
    procTr = null;
  }
}
