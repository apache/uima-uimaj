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
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
/**
 * Note: This class is not used by the framework itself.
 */

/**
 * This class represents a simple pool of {@link JCas} instances. This is useful for multithreaded
 * applications, where there is a need for multiple CASes to be processed simultaneously. Because
 * JCas creation is expensive, it is a good idea to create a pool of reusable JCas instances at
 * initialization time, rather than creating a new JCas each time one is needed.
 * <p>
 * Clients check-out JCas instances from the pool using the {@link #getJCas()} method and check-in
 * JCas instances using the {@link #releaseJCas(JCas)} method.
 * 
 * 
 * 
 */
public class JCasPool {

  private Vector<JCas> mAllInstances = new Vector<JCas>();

  private Vector<JCas> mFreeInstances = new Vector<JCas>();

  private int mNumInstances;

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class<JCasPool> CLASS_NAME = JCasPool.class;

  /**
   * Creates a new JCasPool
   * 
   * @param aNumInstances
   *          the number of JCas instances in the pool
   * @param aTextAnalysisEngine
   *          the TAE that will create the JCas instances and which will later be used to process
   *          them
   * 
   * @throws ResourceInitializationException
   *           if the JCas instances could not be created
   * 
   * @deprecated As of v2.0, TextAnalysisEngine has been deprecated. Use
   *             {@link #JCasPool(int, AnalysisEngine)} instead.
   */
  @Deprecated
  public JCasPool(int aNumInstances, TextAnalysisEngine aTextAnalysisEngine)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aTextAnalysisEngine.getAnalysisEngineMetaData());
  }

  /**
   * Creates a new JCasPool
   * 
   * @param aNumInstances
   *          the number of JCas instances in the pool
   * @param aAnalysisEngine
   *          the AE that will create the JCas instances and which will later be used to process
   *          them
   * 
   * @throws ResourceInitializationException
   *           if the JCas instances could not be created
   */
  public JCasPool(int aNumInstances, AnalysisEngine aAnalysisEngine)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aAnalysisEngine.getAnalysisEngineMetaData());
  }

  /**
   * Creates a new JCasPool
   * 
   * @param aNumInstances
   *          the number of JCas instances in the pool
   * @param aMetaData
   *          metadata that includes the type system for the CAS
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public JCasPool(int aNumInstances, ProcessingResourceMetaData aMetaData)
          throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aMetaData);
  }

  /**
   * Checks out a JCas from the pool.
   * 
   * @return a JCas instance. Returns <code>null</code> if none are available (in which case the
   *         client may {@link Object#wait()} on this object in order to be notified when an
   *         instance becomes available).
   */
  public synchronized JCas getJCas() {
    if (!mFreeInstances.isEmpty()) {
      return mFreeInstances.remove(0);
    } else {
      // no instances available
      return null;
    }
  }

  /**
   * Checks in a JCas to the pool. This automatically calls the {@link JCas#reset()} method, to
   * ensure that when the JCas is later retrieved from the pool it will be ready to use. Also
   * notifies other Threads that may be waiting for an instance to become available.
   * 
   * @param aJCas
   *          the JCas to release
   */
  public synchronized void releaseJCas(JCas aJCas) {
    // make sure this CAS actually belongs to this pool and is checked out
    if (!mAllInstances.contains(aJCas) || mFreeInstances.contains(aJCas)) {
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING, CLASS_NAME.getName(), "releaseJCas",
              LOG_RESOURCE_BUNDLE, "UIMA_return_jcas_to_pool__WARNING");
    } else {
      // reset CAS
      aJCas.reset();
      // Add the CAS to the end of the free instances List
      mFreeInstances.add(aJCas);
    }

    // Notify any threads waiting on this object
    notifyAll();
  }

  /**
   * Checks out a JCas from the pool. If none is currently available, wait for the specified amount
   * of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a JCas instance. Returns <code>null</code> if none are available within the specified
   *         timeout period.
   */
  public synchronized JCas getJCas(long aTimeout) {
    long startTime = new Date().getTime();
    JCas cas;
    while ((cas = getJCas()) == null) {
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
   * Gets the size of this pool (the total number of JCas instances that it can hold).
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
   *          metadata including the type system for the CASes
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  protected void fillPool(ProcessingResourceMetaData aMetaData)
          throws ResourceInitializationException {
    // fill the pool
    ArrayList<ProcessingResourceMetaData> mdList = new ArrayList<ProcessingResourceMetaData>();
    mdList.add(aMetaData);
    for (int i = 0; i < mNumInstances; i++) {
      JCas c;
      try {
        c = CasCreationUtils.createCas(mdList).getJCas();
      } catch (CASException e) {
        throw new ResourceInitializationException(e);
      }
      mAllInstances.add(c);
      mFreeInstances.add(c);
    }
  }

  protected Vector<JCas> getAllInstances() {
    return mAllInstances;
  }

  protected Vector<JCas> getFreeInstances() {
    return mFreeInstances;
  }
}
