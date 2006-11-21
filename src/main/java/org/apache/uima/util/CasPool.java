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
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.CasDefinition;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * This class represents a simple pool of {@link CAS} instances. This is useful for multithreaded
 * applications, where there is a need for multiple CASes to be processed simultaneously. Because
 * CAS creation is expensive, it is a good idea to create a pool of reusable CAS instances at
 * initialization time, rather than creating a new CAS each time one is needed.
 * <p>
 * Clients check-out CAS instances from the pool using the {@link #getCas()} method and check-in CAS
 * instances using the {@link #releaseCas(CAS)} method.
 * 
 * 
 * 
 */
public class CasPool {

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class CLASS_NAME = CasPool.class;

  private Vector mAllInstances = new Vector();

  private Vector mFreeInstances = new Vector();

  private int mNumInstances;

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aComponentDescriptionsOrMetaData
   *          a collection of {@link AnalysisEngineDescription},
   *          {@link CollectionReaderDescription}, {@link CasConsumerDescription}, or
   *          {@link ProcessingResourceMetaData} objects.
   * @param aPerformanceTuningSettings
   *          Properties object containing framework performance tuning settings using key names
   *          defined on {@link UIMAFramework} interface
   * @param aResourceManager
   *          the resource manager to use to resolve import declarations within the metadata
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public CasPool(int aNumInstances, Collection aComponentDescriptionsOrMetaData,
                  Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
                  throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aComponentDescriptionsOrMetaData, aPerformanceTuningSettings, aResourceManager);
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aAnalysisEngine
   *          the analysis engine that will create the CAS instances and which will later be used to
   *          process them
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public CasPool(int aNumInstances, AnalysisEngine aAnalysisEngine)
                  throws ResourceInitializationException {
    mNumInstances = aNumInstances;
    ArrayList mdList = new ArrayList();
    mdList.add(aAnalysisEngine.getMetaData());
    fillPool(mdList, aAnalysisEngine.getPerformanceTuningSettings(), aAnalysisEngine
                    .getResourceManager());
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aMetaData
   *          metadata that includes the type system for the CAS
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public CasPool(int aNumInstances, ProcessingResourceMetaData aMetaData)
                  throws ResourceInitializationException {
    mNumInstances = aNumInstances;
    ArrayList mdList = new ArrayList();
    mdList.add(aMetaData);
    fillPool(mdList, null, null);
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aMetaData
   *          metadata that includes the type system for the CAS
   * 
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public CasPool(int aNumInstances, ProcessingResourceMetaData aMetaData,
                  ResourceManager aResourceManager) throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    ArrayList mdList = new ArrayList();
    mdList.add(aMetaData);
    fillPool(mdList, null, aResourceManager);
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aCasDefinition
   *          the Cas definition, which includes the type system, type priorities, and indexes for
   *          the CASes in the pool.
   * @param aPerformanceTuningSettings
   *          Properties object containing framework performance tuning settings using key names
   *          defined on {@link UIMAFramework} interface
   */
  public CasPool(int aNumInstances, CasDefinition aCasDefinition,
                  Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    mNumInstances = aNumInstances;
    fillPool(aCasDefinition, aPerformanceTuningSettings);
  }

  /**
   * Checks out a CAS from the pool.
   * 
   * @return a CAS instance. Returns <code>null</code> if none are available (in which case the
   *         client may {@link Object#wait()} on this object in order to be notified when an
   *         instance becomes available).
   */
  public synchronized CAS getCas() {
    if (!mFreeInstances.isEmpty()) {
      return (CAS) mFreeInstances.remove(0);
    } else {
      // no instances available
      return null;
    }
  }

  /**
   * Checks in a CAS to the pool. This automatically calls the {@link CAS#reset()} method, to ensure
   * that when the CAS is later retrieved from the pool it will be ready to use. Also notifies other
   * Threads that may be waiting for an instance to become available.
   * 
   * @param aCas
   *          the Cas to release
   */
  public synchronized void releaseCas(CAS aCas) {
    // note the pool stores references to the InitialView of each CAS
    CAS cas = aCas.getView(CAS.NAME_DEFAULT_SOFA);

    // make sure this CAS actually belongs to this pool and is checked out
    if (!mAllInstances.contains(cas) || mFreeInstances.contains(cas)) {
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING, CLASS_NAME.getName(), "releaseCas",
                      LOG_RESOURCE_BUNDLE, "UIMA_return_cas_to_pool__WARNING");
    } else {
      // reset CAS
      cas.reset();
      // Add the CAS to the end of the free instances List
      mFreeInstances.add(cas);
    }

    // Notify any threads waiting on this object
    notifyAll();
  }

  /**
   * Checks out a CAS from the pool. If none is currently available, wait for the specified amount
   * of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a CAS instance. Returns <code>null</code> if none are available within the specified
   *         timeout period.
   */
  public synchronized CAS getCas(long aTimeout) {
    long startTime = new Date().getTime();
    CAS cas;
    while ((cas = getCas()) == null) {
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
   * Gets the size of this pool (the total number of CAS instances that it can hold).
   * 
   * @return the size of this pool
   */
  public int getSize() {
    return mNumInstances;
  }

  /**
   * @param componentDescriptionsOrMetaData
   * @param performanceTuningSettings
   * @param resourceManager
   */
  private void fillPool(Collection mdList, Properties performanceTuningSettings,
                  ResourceManager resourceManager) throws ResourceInitializationException {
    CasDefinition casDef = new CasDefinition(mdList, resourceManager);
    fillPool(casDef, performanceTuningSettings);
  }

  private void fillPool(CasDefinition casDef, Properties performanceTuningSettings)
                  throws ResourceInitializationException {
    // create first CAS from metadata
    CAS c0 = CasCreationUtils.createCas(casDef, performanceTuningSettings);
    // set owner so cas.release() can return it to the pool
    ((CASImpl) c0).setOwner(casDef.getCasManager());
    mAllInstances.add(c0);
    mFreeInstances.add(c0);
    // create additional CASes that share same type system
    for (int i = 1; i < mNumInstances; i++) {
      CAS c = CasCreationUtils.createCas(casDef, performanceTuningSettings, c0.getTypeSystem());
      ((CASImpl) c).setOwner(casDef.getCasManager());
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
