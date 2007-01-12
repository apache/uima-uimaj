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

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;

/**
 * Callback Listener.
 * 
 * 
 */
class StatusCallbackListenerImpl implements StatusCallbackListener {
  int entityCount = 0;

  long size = 0;

  /**
   * Called when the initialization is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
   */
  public void initializationComplete() {
    if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cpm_init_complete__CONFIG",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the batchProcessing is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
   * 
   */
  public synchronized void batchProcessComplete() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_method_ping__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the collection processing is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
   */
  public synchronized void collectionProcessComplete() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_method_ping__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the CPM is paused.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
   */
  public synchronized void paused() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_paused__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the CPM is resumed after a pause.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
   */
  public synchronized void resumed() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_resumed__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the CPM is stopped abruptly due to errors.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
   */
  public void aborted() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stopped__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Called when the processing of a Document is completed. <br>
   * The process status can be looked at and corresponding actions taken.
   * 
   * @param aCas
   *          CAS corresponding to the completed processing
   * @param aStatus
   *          EntityProcessStatus that holds the status of all the events for aEntity
   */

  public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
    // if there is an error above the individual document level,
    // an entityProcessStatus is created with a null value for entity
    if (aCas == null) {
      for (int i = 0; i < aStatus.getFailedComponentNames().size(); i++) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_failed_component__FINEST",
                  new Object[] { Thread.currentThread().getName(),
                      ((String) aStatus.getFailedComponentNames().get(i)) });
        }
      }
      for (int i = 0; i < aStatus.getExceptions().size(); i++) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_component_exception__FINEST",
                  new Object[] { Thread.currentThread().getName(),
                      (aStatus.getExceptions().get(i)).toString() });
        }
      }
      return;
    }
    try {
      entityCount++;
      int dataSize = 0;
      // get size here
      Type t = aCas.getTypeSystem().getType("uima.cpm.FileLocation");
      Feature f = t.getFeatureByBaseName("DocumentSize");
      FSIterator fsI = aCas.getAnnotationIndex(t).iterator();
      if (fsI.isValid()) {
        dataSize = fsI.get().getIntValue(f);
      }

      size += dataSize;
      // to handle exceptions occured in any of the components for the entity
      if (aStatus.isException()) {
        for (int q = 0; q < aStatus.getExceptions().size(); q++) {
          Exception e = (Exception) aStatus.getExceptions().get(q);
          e.printStackTrace();
        }
      }
    } catch (Exception io) {
      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, "", io);
    }
  }

}
