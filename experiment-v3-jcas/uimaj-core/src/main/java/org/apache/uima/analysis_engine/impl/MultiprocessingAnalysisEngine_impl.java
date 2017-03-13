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

package org.apache.uima.analysis_engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.analysis_engine.ResultNotSupportedException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.internal.util.AnalysisEnginePool;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;

/**
 * An {@link AnalysisEngine} implementation that can process multiple {@link CAS} objects
 * simultaneously. This is accomplished by maintaining a pool of {@link AnalysisEngine}
 * instances. When initialized, this class checks for the parameter
 * {@link #PARAM_NUM_SIMULTANEOUS_REQUESTS} to determine how many <code>AnalysisEngine</code>
 * instances to put in the pool.
 */
public class MultiprocessingAnalysisEngine_impl extends AnalysisEngineImplBase implements
        TextAnalysisEngine {

  /**
   * AnalysisEngine pool used to serve process requests.
   */
  private AnalysisEnginePool mPool;

  private static int DEFAULT_NUM_SIMULTANEOUS_REQUESTS = 3;

  private static int DEFAULT_TIMEOUT_PERIOD = 0;

  private int mTimeout;

  /**
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    
    // Read parameters from the aAdditionalParams map.
    // (First copy it so we can modify it and send the parameters on to
    // each Analysis Engine in the pool.)
    if (aAdditionalParams == null) {
      aAdditionalParams = new HashMap<String, Object>();
    } else {
      aAdditionalParams = new HashMap<String, Object>(aAdditionalParams);
    }

    // get or create ResourceManager
    // This ResourceManager is shared among all the AEs in the pool, and also used by 
    // this MultiprocessingAE instance
    // https://issues.apache.org/jira/browse/UIMA-2078
    ResourceManager resMgr = (ResourceManager) aAdditionalParams.get(Resource.PARAM_RESOURCE_MANAGER);
    if (resMgr == null) {
      resMgr = UIMAFramework.newDefaultResourceManager(); 
      aAdditionalParams.put(Resource.PARAM_RESOURCE_MANAGER, resMgr);
    }
    
    // Share the configMgr so that (re)configure actions affect all instances
    
    ConfigurationManager configMgr = (ConfigurationManager) aAdditionalParams.get(Resource.PARAM_CONFIG_MANAGER);
    if (configMgr == null) {
      configMgr = UIMAFramework.newConfigurationManager();
      aAdditionalParams.put(Resource.PARAM_CONFIG_MANAGER, configMgr);
    }
    
    super.initialize(aSpecifier, aAdditionalParams);

    // determine size of Analysis Engine pool and timeout period
    Integer poolSizeInteger = (Integer) aAdditionalParams.get(PARAM_NUM_SIMULTANEOUS_REQUESTS);
    int poolSize = (poolSizeInteger != null) ? poolSizeInteger.intValue()
            : DEFAULT_NUM_SIMULTANEOUS_REQUESTS;

    Integer timeoutInteger = (Integer) aAdditionalParams.get(PARAM_TIMEOUT_PERIOD);
    mTimeout = (timeoutInteger != null) ? timeoutInteger.intValue() : DEFAULT_TIMEOUT_PERIOD;

    // Share resource manager, but don't share uima-context
//    // add UimaContext to params map so that all AEs in pool will share it
//    aAdditionalParams.put(PARAM_UIMA_CONTEXT, getUimaContextAdmin());

    
    // create pool (REMOVE pool size parameter from map so we don't try to
    // fill pool with other MultiprocessingAnalysisEngines!)
    aAdditionalParams.remove(PARAM_NUM_SIMULTANEOUS_REQUESTS);
    mPool = new AnalysisEnginePool("", poolSize, aSpecifier, aAdditionalParams);

    // update metadata from pool (this gets the merged type system for aggregates)
    this.setMetaData(mPool.getMetaData());
    return true;
  }


  private AnalysisEngine getAeFromPool() throws AnalysisEngineProcessException {
    AnalysisEngine ae = mPool.getAnalysisEngine(mTimeout);
    if (ae == null) { // timeout elapsed
      throw new AnalysisEngineProcessException(AnalysisEngineProcessException.TIMEOUT_ELAPSED,
         new Object[] { Integer.valueOf(getTimeout()) });
    }       
    return ae;
  }
  
  
  /***************************************************************
   * The next set of methods override the normal Annotator APIs 
   * with code that checks out an instance, runs that instance
   * for the particular method being called, and then 
   * returns the instance to the pool.
   ***************************************************************/
  
  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.cas.CAS,
   *      org.apache.uima.analysis_engine.ResultSpecification)
   */
  public ProcessTrace process(CAS aCAS, ResultSpecification aResultSpec)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();
      return ae.process(aCAS, aResultSpec);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#process(org.apache.uima.cas.CAS,
   *      org.apache.uima.analysis_engine.ResultSpecification, org.apache.uima.util.ProcessTrace)
   */
  public void process(CAS aCAS, ResultSpecification aResultSpec, ProcessTrace aTrace)
          throws ResultNotSupportedException, AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();       
      ae.process(aCAS, aResultSpec, aTrace);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }
  
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    // https://issues.apache.org/jira/browse/UIMA-5191
    final long startTime = System.currentTimeMillis();    
    final AnalysisEngine ae = getAeFromPool();
    final CasIterator it_inner = ae.processAndOutputNewCASes(aCAS);
    final AnalysisEnginePool localMPool = mPool;
    CasIterator it_outer = new CasIterator() {

      @Override
      public boolean hasNext() throws AnalysisEngineProcessException {
        boolean r = it_inner.hasNext();
        if (!r) {
          localMPool.releaseAnalysisEngine(ae);
          getMBean().reportAnalysisTime(System.currentTimeMillis() - startTime);
        }
        return r;      }

      @Override
      public CAS next() throws AnalysisEngineProcessException { return it_inner.next(); }
      @Override
      public void release() { it_inner.release(); }
    };
    
    return it_outer;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#process(org.apache.uima.cas.CAS)
   */
  @Override
  public ProcessTrace process(CAS aCAS) throws AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();       
      return ae.process(aCAS);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }


  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#process(org.apache.uima.jcas.JCas)
   */
  @Override
  public ProcessTrace process(JCas aJCas) throws AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();       
      return ae.process(aJCas);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }


  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#process(org.apache.uima.jcas.JCas, org.apache.uima.analysis_engine.ResultSpecification)
   */
  @Override
  public ProcessTrace process(JCas aJCas, ResultSpecification aResultSpec)
      throws ResultNotSupportedException, AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();       
      return ae.process(aJCas, aResultSpec);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }


  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#process(org.apache.uima.jcas.JCas, org.apache.uima.analysis_engine.ResultSpecification, org.apache.uima.util.ProcessTrace)
   */
  @Override
  public void process(JCas aJCas, ResultSpecification aResultSpec, ProcessTrace aTrace)
      throws ResultNotSupportedException, AnalysisEngineProcessException {
    AnalysisEngine ae = null;
    try {
      ae = getAeFromPool();       
      ae.process(aJCas, aResultSpec, aTrace);
    } finally {
      if (ae != null) {
        mPool.releaseAnalysisEngine(ae);
      }
    }
  }


  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#processAndOutputNewCASes(org.apache.uima.jcas.JCas)
   */
  @Override
  public JCasIterator processAndOutputNewCASes(JCas aJCas) throws AnalysisEngineProcessException {
    // https://issues.apache.org/jira/browse/UIMA-5191
    final long startTime = System.currentTimeMillis();    
    final AnalysisEngine ae = getAeFromPool();
    final JCasIterator it_inner = ae.processAndOutputNewCASes(aJCas);
    final AnalysisEnginePool localMPool = mPool;
    JCasIterator it_outer = new JCasIterator() {
      
      @Override
      public boolean hasNext() throws AnalysisEngineProcessException {
        boolean r = it_inner.hasNext();
        if (!r) {
          localMPool.releaseAnalysisEngine(ae);
          getMBean().reportAnalysisTime(System.currentTimeMillis() - startTime);
        }
        return r;
      }

      @Override
      public JCas next() throws AnalysisEngineProcessException { return it_inner.next(); }
      @Override
      public void release() { it_inner.release(); }
    };
    return it_outer;
  }


  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
   mPool.setResultSpecification(aResultSpec);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    mPool.reconfigure();
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    mPool.destroy();
    super.destroy();
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setLogger(org.apache.uima.util.Logger)
   */
  public void setLogger(Logger aLogger) {
    super.setLogger(aLogger);
    mPool.setLogger(aLogger);
  }

  public void batchProcessComplete() throws AnalysisEngineProcessException {
    mPool.batchProcessComplete();
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    mPool.collectionProcessComplete();
  }

  /**
   * Gets the AnalysisEngine pool used to serve process requests.
   * 
   * @return the AnalysisEngine pool
   */
  protected AnalysisEnginePool getPool() {
    return mPool;
  }

  /**
   * Gets the timeout period, after which an exception will be thrown if no AnalysisEngine is
   * available in the pool.
   * 
   * @return the timeout period in milliseconds
   */
  protected int getTimeout() {
    return mTimeout;
  }



}
