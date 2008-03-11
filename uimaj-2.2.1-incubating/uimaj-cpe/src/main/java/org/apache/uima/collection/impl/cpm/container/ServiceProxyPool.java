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

package org.apache.uima.collection.impl.cpm.container;

import java.util.Date;
import java.util.LinkedList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;

/**
 * Pool containing and managing instances of CasProcessors. Managed by the container the pool
 * facilitates check out and check in of Cas Processors.
 * 
 * 
 */
public class ServiceProxyPool {
  private LinkedList mAllInstances = new LinkedList();

  private LinkedList mFreeInstances = new LinkedList();

//  private int mNumInstances;

  /**
   * Checks out a Resource from the pool.
   * 
   * @return a Resource for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public synchronized CasProcessor checkOut() {
    if (!mFreeInstances.isEmpty()) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_checking_out_cp_from_pool__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      CasProcessor r = (CasProcessor) mFreeInstances.remove(0);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_cp_pool_size__FINEST",
                new Object[] { Thread.currentThread().getName(),
                    String.valueOf(mAllInstances.size()), String.valueOf(mFreeInstances.size()) });
      }
      return r;
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.WARNING,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cp_pool_empty__WARNING",
                new Object[] { Thread.currentThread().getName(),
                    String.valueOf(mAllInstances.size()), String.valueOf(mFreeInstances.size()) });
      }
      return null;
    }
  }

  /**
   * Checks in a Resource to the pool. Also notifies other Threads that may be waiting for available
   * instance.
   * 
   * @param aResource -
   *          instance of the CasProcessor to check in
   */
  public synchronized void checkIn(CasProcessor aResource) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_checking_in_cp_to_pool__FINEST",
              new Object[] { Thread.currentThread().getName(),
                  String.valueOf(mAllInstances.size()), String.valueOf(mFreeInstances.size()) });
    }
    // make sure this Resource was actually belongs to this pool and is checked out
    if (!mAllInstances.contains(aResource) || mFreeInstances.contains(aResource)) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_checking_in_invalid_cp_to_pool__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      if (!mAllInstances.contains(aResource)) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cp_not_in_pool__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      } else {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_cp_already_checked_in__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
    } else {
      // Add the Resource to the end of the free instances List
      mFreeInstances.add(aResource);
    }
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_cp_pool_size__FINEST",
              new Object[] { Thread.currentThread().getName(),
                  String.valueOf(mAllInstances.size()), String.valueOf(mFreeInstances.size()) });
    }
    // Notify any threads waiting on this object
    notifyAll();
  }

  /**
   * Checks out a Resource from the pool. If none is currently available, wait for the specified
   * amount of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a Resource for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public synchronized CasProcessor checkOut(long aTimeout) {
    long startTime = new Date().getTime();
    CasProcessor resource;
    while ((resource = checkOut()) == null) {
      try {
        wait(aTimeout);
      } catch (InterruptedException e) {
      }
      if (aTimeout > 0 && (new Date().getTime() - startTime) >= aTimeout) {
        // Timeout has expired
        return null;
      }
    }
    return resource;
  }

  /**
   * Destroys all Resources in this pool.
   */
  public synchronized void destroy() {
    mAllInstances.clear();
    mFreeInstances.clear();
  }

  /**
   * Gets the available size of this pool (the number of free, available instances at this moment).
   * 
   * @return the available size of this pool
   */
  public synchronized int getSize() {  // synch for JVM memory model to get current value
    return mFreeInstances.size();
  }

  public synchronized void addCasProcessor(CasProcessor aCasProcessor) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_add_cp_to_pool__FINEST",
              new Object[] { Thread.currentThread().getName(),
                  aCasProcessor.getProcessingResourceMetaData().getName() });
    }
    mAllInstances.add(aCasProcessor);
    mFreeInstances.add(aCasProcessor);
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_cp_pool_size__FINEST",
              new Object[] { Thread.currentThread().getName(),
                  String.valueOf(mAllInstances.size()), String.valueOf(mFreeInstances.size()) });
    }
  }

//  /**
//   * Utility method used in the constructor to fill the pool with Resource instances.
//   * 
//   * @param aResourceSpecifier
//   *          specifier that describes how to create the Resource instances for the pool
//   * @param aResourceClass
//   *          class of resource to instantiate
//   * @param aResourceInitParams
//   *          initialization parameters to be passed to the
//   *          {@link Resource#initialize(ResourceSpecifier,Map)} method.
//   * 
//   * 
//   * @throws ResourceInitializationException
//   *           if the Resource instances could not be created
//   */
//  protected void fillPool(BoundedWorkQueue portQueue, Map initParams)
//          throws ResourceInitializationException {
//    boolean isServiceLocal = false;
//    if (initParams != null && initParams.containsKey("SERVICE_NAME")) {
//      isServiceLocal = true;
//    }
//    // fill the pool
//    for (int i = 0; i < mNumInstances; i++) {
//      VinciTAP tap = new VinciTAP();
//      if (isServiceLocal) {
//        String portAsString = (String) portQueue.dequeue();
//        int port = -1;
//        try {
//          port = Integer.parseInt(portAsString);
//        } catch (NumberFormatException e) {
//        }
//        String vnsHost = (String) initParams.get("VNS_HOST");
//        String vnsPort = (String) initParams.get("VNS_PORT");
//        tap.setVNSHost(vnsHost);
//        tap.setVNSPort(vnsPort);
//        try {
//          tap.connect("127.0.0.1", port);
//        } catch (ConnectException e) {
//          throw new ResourceInitializationException(e.getMessage(), null);
//        }
//      }
//      mAllInstances.add(tap);
//      mFreeInstances.add(tap);
//    }
//  }

  // never used
//  /**
//   * Returns all instances in the pool
//   * 
//   * @return - list of CasProcessor instances
//   */
//  protected synchronized LinkedList getAllInstances() {
//    return mAllInstances;
//  }
//
//  /**
//   * Returns a list of CasProcessor instances not currently in use
//   * 
//   * @return -list of free proxies
//   */
//  protected synchronized LinkedList getFreeInstances() {
//    return mFreeInstances;
//  }

  public synchronized int getAllInstanceCount() {
    return mAllInstances.size();
  }
}
