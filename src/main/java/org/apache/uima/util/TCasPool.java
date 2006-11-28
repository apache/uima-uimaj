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

package org.apache.uima.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * This class represents a simple pool of {@link TCAS} instances. This is useful for multithreaded
 * applications, where there is a need for multiple CASes to be processed simultaneously. Because
 * TCAS creation is expensive, it is a good idea to create a pool of reusable TCAS instances at
 * initialization time, rather than creating a new TCAS each time one is needed.
 * <p>
 * Clients check-out TCAS instances from the pool using the {@link #getTCas()} method and check-in
 * TCAS instances using the {@link #releaseTCas(TCAS)} method.
 * 
 * 
 * 
 */
public class TCasPool {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class CLASS_NAME = TCasPool.class;

  private Vector mAllInstances = new Vector();

  private Vector mFreeInstances = new Vector();

  private int mNumInstances;

  /**
   * Creates a new TCasPool
   * 
   * @param aNumInstances
   *          the number of TCAS instances in the pool
   * @param aTextAnalysisEngine
   *          the TAE that will create the TCAS instances and which will later be used to process
   *          them
   * 
   * @throws ResourceInitializationException
   *           if the TCAS instances could not be created
   * 
   * @deprecated As of v2.0, TextAnalysisEngine has been deprecated. Use
   *             {@link #TCasPool(int, AnalysisEngine)} instead.
   */
  public TCasPool(int aNumInstances, TextAnalysisEngine aTextAnalysisEngine)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aTextAnalysisEngine.getAnalysisEngineMetaData());
  }

  /**
   * Creates a new TCasPool
   * 
   * @param aNumInstances
   *          the number of TCAS instances in the pool
   * @param aAnalysisEngine
   *          the AE that will create the TCAS instances and which will later be used to process
   *          them
   * 
   * @throws ResourceInitializationException
   *           if the TCAS instances could not be created
   */
  public TCasPool(int aNumInstances, AnalysisEngine aAnalysisEngine)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aAnalysisEngine.getAnalysisEngineMetaData());
  }

  /**
   * Creates a new TCasPool
   * 
   * @param aNumInstances
   *          the number of TCAS instances in the pool
   * @param aMetaData
   *          metadata that includes the type system for the CAS
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public TCasPool(int aNumInstances, ProcessingResourceMetaData aMetaData)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aMetaData);
  }

  /**
   * Creates a new TCasPool
   * 
   * @param aNumInstances
   *          the number of TCAS instances in the pool
   * @param aMetaDataList
   *          list of ResourceMetaData objects including the type sytsem for the CASes
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public TCasPool(int aNumInstances, List aMetaDataList) throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aMetaDataList);
  }

  /**
   * Checks out a TCAS from the pool.
   * 
   * @return a TCAS instance. Returns <code>null</code> if none are available (in which case the
   *         client may {@link Object#wait()} on this object in order to be notified when an
   *         instance becomes available).
   */
  public synchronized TCAS getTCas() {
    if (!mFreeInstances.isEmpty()) {
      return (TCAS) mFreeInstances.remove(0);
    } else {
      // no instances available
      return null;
    }
  }

  /**
   * Checks in a TCAS to the pool. This automatically calls the {@link TCAS#reset()} method, to
   * ensure that when the TCAS is later retrieved from the pool it will be ready to use. Also
   * notifies other Threads that may be waiting for an instance to become available.
   * 
   * @param aTCas
   *          the TCAS to release
   */
  public synchronized void releaseTCas(TCAS aTCas) {
    // make sure this CAS actually belongs to this pool and is checked out
    if (!mAllInstances.contains(aTCas) || mFreeInstances.contains(aTCas)) {
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING, CLASS_NAME.getName(), "releaseTCas",
              LOG_RESOURCE_BUNDLE, "UIMA_return_tcas_to_pool__WARNING");
    } else {
      // reset CAS
      aTCas.reset();
      // Add the CAS to the end of the free instances List
      mFreeInstances.add(aTCas);
    }

    // Notify any threads waiting on this object
    notifyAll();
  }

  /**
   * Checks out a TCAS from the pool. If none is currently available, wait for the specified amount
   * of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a TCAS instance. Returns <code>null</code> if none are available within the specified
   *         timeout period.
   */
  public synchronized TCAS getTCas(long aTimeout) {
    long startTime = new Date().getTime();
    TCAS cas;
    while ((cas = getTCas()) == null) {
      try {
        wait(aTimeout);
      } catch (InterruptedException e) {
      }
      if (aTimeout > 0 && (new Date().getTime() - startTime) >= aTimeout) {
        // Timeout has expired
        return null;
      }
    }
    return cas;
  }

  /**
   * Gets the size of this pool (the total number of TCAS instances that it can hold).
   * 
   * @return the size of this pool
   */
  public int getSize() {
    return mNumInstances;
  }

  /**
   * Utility method used in the constructor to fill the pool with CAS instances.
   * 
   * @param aMetaData
   *          metadata including the type sytsem for the CASes
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  protected void fillPool(ProcessingResourceMetaData aMetaData)
          throws ResourceInitializationException {
    // create first CAS from metadata
    ArrayList mdList = new ArrayList();
    mdList.add(aMetaData);
    TCAS c0 = CasCreationUtils.createTCas(mdList);
    mAllInstances.add(c0);
    mFreeInstances.add(c0);
    // create additional CASes that share same type system
    for (int i = 1; i < mNumInstances; i++) {
      TCAS c = CasCreationUtils.createTCas(c0.getTypeSystem(), aMetaData.getTypePriorities(),
              aMetaData.getFsIndexes());
      mAllInstances.add(c);
      mFreeInstances.add(c);
    }
  }

  /**
   * Utility method used in the constructor to fill the pool with CAS instances.
   * 
   * @param aMetaDataList
   *          list of ResourceMetaData objects including the type sytsem for the CASes
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  protected void fillPool(List aMetaDataList) throws ResourceInitializationException {
    // create first CAS from metadata
    TCAS c0 = CasCreationUtils.createTCas(aMetaDataList);
    mAllInstances.add(c0);
    mFreeInstances.add(c0);
    // create additional CASes that share same type system
    for (int i = 1; i < mNumInstances; i++) {
      TCAS c = CasCreationUtils.createTCas(aMetaDataList, c0.getTypeSystem());
      mAllInstances.add(c);
      mFreeInstances.add(c);
    }
  }

  protected Vector getAllInstances() {
    return mAllInstances;
  }

  protected Vector getFreeInstances() {
    return mFreeInstances;
  }
}
