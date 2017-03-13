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

package org.apache.uima.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;

/**
 * A pool of Analysis Engines, which supports reconfiguration. This is not part of the stable UIMA
 * API and may change in future releases.
 * 
 * 
 */
public class AnalysisEnginePool {

  /**
   * Creates a new AnalysisEnginePool.
   * 
   * @param aName
   *          the pool name
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public AnalysisEnginePool(String aName, int aNumInstances, ResourceSpecifier aResourceSpecifier)
          throws ResourceInitializationException {
    this(aName, aNumInstances, aResourceSpecifier, null);
  }

  /**
   * Creates a new AnalysisEnginePool.
   * 
   * @param aName
   *          the pool name
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * @param aResourceInitParams
   *          additional parameters to be passed to
   *          {@link Resource#initialize(ResourceSpecifier,Map)} methods. May be null if there are
   *          no parameters.
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public AnalysisEnginePool(String aName, int aNumInstances, ResourceSpecifier aResourceSpecifier,
          Map<String, Object> aResourceInitParams) throws ResourceInitializationException {
    if (aResourceInitParams == null) {
      aResourceInitParams = new HashMap<String, Object>();
    } else {
      aResourceInitParams = new HashMap<String, Object>(aResourceInitParams);
    }

    // initialize ResourcePool
    mPool = new ResourcePool(aNumInstances, aResourceSpecifier, getResourceClass(),
            aResourceInitParams);
  }

  /**
   * Checks out an AnalysisEngine from the pool.
   * 
   * @return an AnalysisEngine for use by the client. Returns <code>null</code> if none are
   *         available (in which case the client may wait on this object in order to be notified
   *         when an instance becomes available).
   */
  public AnalysisEngine getAnalysisEngine() {
    return (AnalysisEngine) mPool.getResource();
  }

  /**
   * Checks in an AnalysisEngine to the pool. Also notifies other Threads that may be waiting for a
   * connection.
   * 
   * @param aAE
   *          the resource to release
   */
  public void releaseAnalysisEngine(AnalysisEngine aAE) {
    mPool.releaseResource(aAE);
  }

  /**
   * Checks out an AnalysisEngine from the pool. If none is currently available, wait for the
   * specified amount of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return an AnalysisEngine for use by the client. Returns <code>null</code> if none are
   *         available (in which case the client may wait on this object in order to be notified
   *         when an instance becomes available).
   */
  public AnalysisEngine getAnalysisEngine(long aTimeout) {
    return (AnalysisEngine) mPool.getResource(aTimeout);
  }

  /**
   * Destroys all AnalysisEngines in this pool.
   */
  public synchronized void destroy() {
    mPool.destroy();
  }

  /**
   * Gets metadata for AnalysisEngines in this pool.
   * @return -
   */
  public AnalysisEngineMetaData getMetaData() {
    return (AnalysisEngineMetaData) mPool.getMetaData();
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(ResultSpecification)
   * This version only called for setResultSpecification called from an appl on the
   * MultiprocessingAnalysisEngine directly.  process(cas, result-spec) calls
   * setResultSpecification on the individual analysis engine from the pool.
   * @param aResultSpec -
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    
    // set Result Spec on each AnalysisEngine in the pool

    Vector<Resource> allInstances = mPool.getAllInstances();
    for (int i = 0; i < mPool.getSize(); i++) {
      AnalysisEngine ae = (AnalysisEngine)allInstances.get(i);
      
      mPool.checkoutSpecificResource(ae);
      
      try {
      //    set result spec
      ae.setResultSpecification(aResultSpec);
      } finally {
      mPool.releaseResource(ae);
      }
    }
 
  }

//  public void setResultSpecForAeIfPending(AnalysisEngine ae) {
//    Vector allInstances = mPool.getAllInstances();
//    int i = allInstances.indexOf(ae);
//    if (resultSpecChanged[i]) {
//      resultSpecChanged[i] = false;
//      ae.setResultSpecification(sharedResultSpec);
//    }  
//  }
  
  /**
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   * @throws ResourceConfigurationException -
   */
  public synchronized void reconfigure() throws ResourceConfigurationException {
    // reconfigure each AnalysisEngine in the pool
    List<AnalysisEngine> toRelease = new ArrayList<AnalysisEngine>();
    try {
      for (int i = 0; i < mPool.getSize(); i++) {
        // get an Analysis Engine from the pool
        AnalysisEngine ae = (AnalysisEngine) mPool.getResource(0); // wait forever

        // store AE instance on List to be released later
        toRelease.add(ae);

        // reconfigure
        ae.reconfigure();
      }
    } finally {
      // release all AnalysisEngines back to pool
      Iterator<AnalysisEngine> it = toRelease.iterator();
      while (it.hasNext()) {
        mPool.releaseResource(it.next());
      }
    }
  }

  /**
   * Calls batchProcessComplete on all AEs in pool.
   * @throws AnalysisEngineProcessException -
   */
  public synchronized void batchProcessComplete() throws AnalysisEngineProcessException {
    List<AnalysisEngine> toRelease = new ArrayList<AnalysisEngine>();
    try {
      for (int i = 0; i < mPool.getSize(); i++) {
        // get an Analysis Engine from the pool
        AnalysisEngine ae = (AnalysisEngine) mPool.getResource(0); // wait forever

        // store AE instance on List to be released later
        toRelease.add(ae);

        ae.batchProcessComplete();
      }
    } finally {
      // release all AnalysisEngines back to pool
      Iterator<AnalysisEngine> it = toRelease.iterator();
      while (it.hasNext()) {
        mPool.releaseResource(it.next());
      }
    }
  }

  /**
   * Calls collectionProcessComplete on all AEs in pool.
   * @throws AnalysisEngineProcessException -
   */
  public synchronized void collectionProcessComplete() throws AnalysisEngineProcessException {
    List<AnalysisEngine> toRelease = new ArrayList<AnalysisEngine>();
    try {
      for (int i = 0; i < mPool.getSize(); i++) {
        // get an Analysis Engine from the pool
        AnalysisEngine ae = (AnalysisEngine) mPool.getResource(0); // wait forever

        // store AE instance on List to be released later
        toRelease.add(ae);

        ae.collectionProcessComplete();
      }
    } finally {
      // release all AnalysisEngines back to pool
      Iterator<AnalysisEngine> it = toRelease.iterator();
      while (it.hasNext()) {
        mPool.releaseResource(it.next());
      }
    }
  }

  /**
   * Returns the size of this pool - the total number of AnalysisEngine instances it would contain
   * if no instances were checked out.
   * 
   * @return the pool size
   */
  public int getSize() {
    return mPool.getSize();
  }

  /**
   * Sets logger for all AnalysisEngines in pool.
   * @param aLogger -
   */
  public synchronized void setLogger(Logger aLogger) {
    List<AnalysisEngine> toRelease = new ArrayList<AnalysisEngine>();
    try {
      for (int i = 0; i < mPool.getSize(); i++) {
        // get an Analysis Engine from the pool
        AnalysisEngine ae = (AnalysisEngine) mPool.getResource(0); // wait forever

        // store AE instance on List to be released later
        toRelease.add(ae);

        // reconfigure
        ae.setLogger(aLogger);
      }
    } finally {
      // release all AnalysisEngines back to pool
      Iterator<AnalysisEngine> it = toRelease.iterator();
      while (it.hasNext()) {
        mPool.releaseResource(it.next());
      }
    }
  }

  /**
   * Gets the class of Resource contained in this pool - by default this is
   * <code>AnalysisEngine</code>, but subclasses may override.
   * 
   * @return class of Resource contained in this pool
   */
  protected Class<AnalysisEngine> getResourceClass() {
    return AnalysisEngine.class;
  }

  /** Pool of AnalysisEngine instances. */
  private ResourcePool mPool;
}
