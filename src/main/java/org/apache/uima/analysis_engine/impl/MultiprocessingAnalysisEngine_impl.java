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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.internal.util.AnalysisEnginePool;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;

/**
 * An {@link AnalysisEngine} implementation that can process multiple
 * {@link CAS} objects simultaneously.  This is accomplished by maintaining
 * a pool of {@link AnalysisEngine_impl} instances.  When initialized, this 
 * class checks for the parameter {@link #PARAM_NUM_SIMULTANEOUS_REQUESTS} to 
 * determine how many <code>AnalysisEngine_impl</code> instances to put in 
 * the pool. 
 */
public class MultiprocessingAnalysisEngine_impl extends AnalysisEngineImplBase
  implements TextAnalysisEngine
{ 
  /**
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier, java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier,
    Map aAdditionalParams)
    throws ResourceInitializationException
  {
    super.initialize(aSpecifier, aAdditionalParams);
    
    //Read parameters from the aAdditionalParams map.
    //(First copy it so we can modify it and send the parameters on to
    // each Analysis Engine in the pool.)  
    if (aAdditionalParams == null)
    {
      aAdditionalParams = new HashMap();
    }
    else
    {
      aAdditionalParams = new HashMap(aAdditionalParams);
    }

    //determine size of Analysis Engine pool and timeout period
    Integer poolSizeInteger = (Integer)aAdditionalParams.get(
        PARAM_NUM_SIMULTANEOUS_REQUESTS);
    int poolSize = (poolSizeInteger != null) ? poolSizeInteger.intValue() :
        DEFAULT_NUM_SIMULTANEOUS_REQUESTS;   


    Integer timeoutInteger = (Integer)
        aAdditionalParams.get(PARAM_TIMEOUT_PERIOD);
    mTimeout = (timeoutInteger != null) ? timeoutInteger.intValue() :
        DEFAULT_TIMEOUT_PERIOD;
        
    //add UimaContext to params map so that all AEs in pool will share it
    aAdditionalParams.put(PARAM_UIMA_CONTEXT, getUimaContextAdmin());    
        
    //create pool (REMOVE pool size parameter from map so we don't try to 
    //fill pool with other MultiprocessingAnalysisEngines!)
    aAdditionalParams.remove(PARAM_NUM_SIMULTANEOUS_REQUESTS);
    mPool = new AnalysisEnginePool("", poolSize, aSpecifier, aAdditionalParams);
        
    //update metadata from pool (this gets the merged type system for aggregates)
    this.setMetaData(mPool.getMetaData());       
    return true;
  }

  
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException
  {
    enterProcess(); //start timer for collecting performance stats
    AnalysisEngine ae = null;
    try
    {
      ae = mPool.getAnalysisEngine(mTimeout);
      if (ae == null) //timeout elapsed
      {
        throw new AnalysisEngineProcessException(
          AnalysisEngineProcessException.TIMEOUT_ELAPSED,
          new Object[]{new Integer(getTimeout())});
      }
      
      return ae.processAndOutputNewCASes(aCAS);
    }
    finally
    {
      if (ae != null)
      {
        mPool.releaseAnalysisEngine(ae);
      }  
      exitProcess(); //stop timer for collecting performance stats
    }
  }

  
  /* (non-Javadoc)
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec)
  {
    mPool.setResultSpecification(aResultSpec);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException
  {
    mPool.reconfigure();
  }


  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy()
  {
    mPool.destroy();
    super.destroy();
  }
  

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setLogger(org.apache.uima.util.Logger)
   */
  public void setLogger(Logger aLogger)
  {
    super.setLogger(aLogger);
    mPool.setLogger(aLogger);
  }
  
  

  public void batchProcessComplete() throws AnalysisEngineProcessException
  {
    mPool.batchProcessComplete();  
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException
  {
    mPool.collectionProcessComplete();
  }

  /**
   * Gets the AnalysisEngine pool used to serve process requests.
   * 
   * @return the AnalysisEngine pool
   */
  protected AnalysisEnginePool getPool()
  {
    return mPool;
  }
  
  /**
   * Gets the timeout period, after which an exception will be thrown if 
   * no AnalysisEngine is available in the pool.
   * 
   * @return the timeout period in milliseconds
   */
  protected int getTimeout()
  {
    return mTimeout;
  }

  /**
   * AnalysisEngine pool used to serve process requests.
   */
  private AnalysisEnginePool mPool;

  private static int DEFAULT_NUM_SIMULTANEOUS_REQUESTS = 3;

  private static int DEFAULT_TIMEOUT_PERIOD = 0;

  private int mTimeout;
}
