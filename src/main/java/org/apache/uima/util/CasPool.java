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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.CasDefinition;
import org.apache.uima.resource.CasManager;
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
 * <p>
 * Design considerations:
 *   The pool favors reuse of CASes is some arbitrary preferred priority order.  For example if there is a pool
 *   of 10 CASes, but only 2 are being check-out at any given time, the same 2 CASes will be used (as opposed
 *   to a FIFO approach where all the CASes would be cycled through).
 *   
 *   If more threads request CASes from the pool than are available, the pool (optionally) puts requesting
 *   threads into a wait state.  When CASes become available, the longest-waiting thread gets the CAS; this
 *   approach prevents starvation behavior (where some threads get all the CASes and others get none).
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
  private static final Class<CasPool> CLASS_NAME = CasPool.class;

  // no sync needed because this list is filled during initialization of this instance, and
  // from then on is read-only, which can occur in parallel
  final private Set<CAS> mAllInstances;

  
  // We use this rather than a form of BlockingQueue, to achieve an (arbitrary) LIFO-like reuse of CASes
  // this is a set rather than an array, to speed up "contains()" check used when releasing
  //   (user code could call release multiple times on same cas...)
  final private Set<CAS> mFreeInstances;
 
  final private int mNumInstances;
  
  // a fair lock to prevent starvation of a thread
  final private Semaphore permits;
  
  private CasPool(int aNumInstances, Set<CAS> allInstances) {
    mNumInstances = aNumInstances;
    permits = new Semaphore(mNumInstances, true);
    mAllInstances = allInstances;
    Set<CAS> free = Collections.newSetFromMap(new ConcurrentHashMap<CAS, Boolean>());
    free.addAll(mAllInstances);
    mFreeInstances = free;  // concurrent safe publishing idiom 
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aCollectionOfProcessingResourceMetaData
   *          a collection of 
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
  public CasPool(int aNumInstances, Collection<? extends ProcessingResourceMetaData> aCollectionOfProcessingResourceMetaData,
          Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
          throws ResourceInitializationException {
    this(aNumInstances, fillPool(aNumInstances, aCollectionOfProcessingResourceMetaData, aPerformanceTuningSettings, aResourceManager));
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
    this(aNumInstances, 
         fillPool(aNumInstances, 
                  Collections.singletonList((ProcessingResourceMetaData) aAnalysisEngine.getMetaData()),
                  aAnalysisEngine.getPerformanceTuningSettings(),
                  aAnalysisEngine.getResourceManager()));
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
    this(aNumInstances, fillPool(aNumInstances, Collections.singletonList(aMetaData), null, null));
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aMetaData
   *          metadata that includes the type system for the CAS
   * @param aResourceManager Resource Manager
   * @throws ResourceInitializationException
   *           if the CAS instances could not be created
   */
  public CasPool(int aNumInstances, ProcessingResourceMetaData aMetaData,
          ResourceManager aResourceManager) throws ResourceInitializationException {
    this(aNumInstances, fillPool(aNumInstances, Collections.singletonList(aMetaData), null, aResourceManager));
  }

  /**
   * Creates a new CasPool.
   * TODO: do we need this method AND the one that takes a CasManager?
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aCasDefinition
   *          the Cas definition, which includes the type system, type priorities, and indexes for
   *          the CASes in the pool.
   * @param aPerformanceTuningSettings
   *          Properties object containing framework performance tuning settings using key names
   *          defined on {@link UIMAFramework} interface
   * @throws ResourceInitializationException -
   */
  public CasPool(int aNumInstances, CasDefinition aCasDefinition,
          Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    this(aNumInstances, fillPool(aNumInstances, aCasDefinition, aPerformanceTuningSettings));
  }

  /**
   * Creates a new CasPool
   * 
   * @param aNumInstances
   *          the number of CAS instances in the pool
   * @param aCasManager
   *          CAS Manager that will be used to create the CAS.  The CAS Manager
   *          holds the CAS Definition.  Also all CASes created from the same
   *          CAS Manager will share identical TypeSystem objects.
   * @param aPerformanceTuningSettings
   *          Properties object containing framework performance tuning settings using key names
   *          defined on {@link UIMAFramework} interface
   * @throws ResourceInitializationException -
   */
  public CasPool(int aNumInstances, CasManager aCasManager,
          Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    this(aNumInstances, fillPool(aNumInstances, aCasManager, aPerformanceTuningSettings));
  }
  
  /**
   * Checks out a CAS from the pool.
   * 
   * @return a CAS instance. Returns <code>null</code> if none are available (in which case the
   *         client may {@link Object#wait()} on this object in order to be notified when an
   *         instance becomes available).
   */
  public CAS getCas() {
    boolean gotPermit;
    gotPermit = permits.tryAcquire();
    if (!gotPermit) {
      return null;
    }
    
    return getCasAfterPermitAcquired();      
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
  public CAS getCas(long aTimeout) {
    if (aTimeout == 0) {
      permits.acquireUninterruptibly();
      return getCasAfterPermitAcquired();
    }
    boolean gotIt;
    try {
      gotIt = permits.tryAcquire(aTimeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return null;
    }
    
    if (!gotIt) {
      return null;
    }    
    return getCasAfterPermitAcquired();
  }

  
  private CAS getCasAfterPermitAcquired() {
    // synchronize so only one iterator is running
    synchronized (mFreeInstances) {
      final Iterator<CAS> freeIterator = mFreeInstances.iterator();
      if (!freeIterator.hasNext()) {
        throw new RuntimeException("internal error");
      }
      final CAS cas = freeIterator.next();
      freeIterator.remove();
//      mFreeInstances.remove(cas);
//      int debugFree = mFreeInstances.size();
//      int debugAvail = permits.availablePermits();
//      if (debugFree != debugAvail) {
//        System.out.println("  on acquire permits != free: " + debugAvail + " " + debugFree);
//      }
      return cas;
    }
  }

  /**
   * Checks in a CAS to the pool. This automatically calls the {@link CAS#reset()} method, to ensure
   * that when the CAS is later retrieved from the pool it will be ready to use. Also notifies other
   * Threads that may be waiting for an instance to become available.
   * 
   * Synchronized on the CAS to avoid the unnatural case where 
   * multiple threads attempt to return the same CAS to the pool
   * at the same time. 
   * 
   * @param aCas
   *          the Cas to release
   */
  public void releaseCas(CAS aCas) {
    // note the pool stores references to the InitialView of each CAS
    aCas.setCurrentComponentInfo(null);  // https://issues.apache.org/jira/browse/UIMA-3655
    CAS cas = aCas.getView(CAS.NAME_DEFAULT_SOFA);

    // make sure this CAS actually belongs to this pool and is checked out
    // synchronize to avoid the same CAS being released on 2 threads
    synchronized (cas) {
      if (!mAllInstances.contains(cas) || mFreeInstances.contains(cas)) {
        UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING, CLASS_NAME.getName(), "releaseCas",
                LOG_RESOURCE_BUNDLE, "UIMA_return_cas_to_pool__WARNING");
      } else {
        // restore the ClassLoader and unlock the CAS, since release() can be called 
        // from within a CAS Multiplier.
        ((CASImpl)cas).restoreClassLoaderUnlockCas(); 
        
        // reset CAS
        cas.reset();
        
        // Add the CAS to the end of the free instances List
        mFreeInstances.add(cas);
        permits.release();  // should follow adding cas back to mFreeInstances
      }
    }

    // Notify any threads waiting on this object
    // not needed by UIMA Core - other users may need.
    synchronized (this) {
      notifyAll();
    }
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
   * Gets the number of CASes currently available in this pool.
   * @return the numberof available CASes 
   */
  public int getNumAvailable() {
    return mFreeInstances.size();
  }  

  /**
   * @param componentDescriptionsOrMetaData
   * @param performanceTuningSettings
   * @param resourceManager
   */
  private static Set<CAS> fillPool(int aNumInstances, Collection<? extends ProcessingResourceMetaData> mdList, Properties performanceTuningSettings,
          ResourceManager resourceManager) throws ResourceInitializationException {
    CasDefinition casDef = new CasDefinition(mdList, resourceManager);
    return fillPool(aNumInstances, casDef, performanceTuningSettings);
  }

  private static Set<CAS> fillPool(int aNumInstances, CasDefinition casDef, Properties performanceTuningSettings)
          throws ResourceInitializationException {
    // create first CAS from metadata
    CAS c0 = CasCreationUtils.createCas(casDef, performanceTuningSettings);
    Set<CAS> all = new HashSet<CAS>(aNumInstances);
    // set owner so cas.release() can return it to the pool
    ((CASImpl) c0).setOwner(casDef.getCasManager());
    all.add(c0);
    // create additional CASes that share same type system
    for (int i = 1; i < aNumInstances; i++) {
      CAS c = CasCreationUtils.createCas(casDef, performanceTuningSettings, c0.getTypeSystem());
      ((CASImpl) c).setOwner(casDef.getCasManager());
      all.add(c);
    }
    return all;
  }

  private static Set<CAS> fillPool(int aNumInstances, CasManager casManager, Properties performanceTuningSettings)
          throws ResourceInitializationException {
    Set<CAS> all = new HashSet<CAS>(aNumInstances);
    // create additional CASes that share same type system
    for (int i = 0; i < aNumInstances; i++) {
      CAS c = casManager.createNewCas(performanceTuningSettings);
      ((CASImpl) c).setOwner(casManager);
      all.add(c);
    }
    return all;
  }  
  
  // no callers as of March 2014
  // left as Vector
  protected Vector<CAS> getAllInstances() {
    return new Vector<CAS>(mAllInstances);
  }

  // no callers as of March 2014
  // left as Vector
  protected Vector<CAS> getFreeInstances() {
    return new Vector<CAS>(mFreeInstances);
  }
}
