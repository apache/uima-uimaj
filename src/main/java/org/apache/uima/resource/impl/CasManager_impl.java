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

package org.apache.uima.resource.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.internal.util.JmxMBeanAgent;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.CasDefinition;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasPool;
import org.apache.uima.util.impl.CasPoolManagementImpl;

/**
 * Simple CAS Manager Implementation used in the AnalysisEngine framework. Maintains a pool of 1 CAS
 * for each requestor.
 */
public class CasManager_impl implements CasManager {
  private ResourceManager mResourceManager;

  private List<ProcessingResourceMetaData> mMetaDataList = new ArrayList<ProcessingResourceMetaData>();

  private Map<String, CasPool> mRequestorToCasPoolMap = new HashMap<String, CasPool>();

  private Map<CAS, CasPool> mCasToCasPoolMap = new HashMap<CAS, CasPool>();
  
  private Map<CAS, UimaContext> mCasToUimaContextMap = new HashMap<CAS, UimaContext>();

  private CasDefinition mCasDefinition = null;
  
  private TypeSystem mCurrentTypeSystem = null;

  private Object mMBeanServer;

  private String mMBeanNamePrefix;
  
  private List<CasPoolManagementImpl> casPoolMBeans = new ArrayList<CasPoolManagementImpl>();

  public CasManager_impl(ResourceManager aResourceManager) {
    mResourceManager = aResourceManager;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.CasManager#addMetaData(org.apache.uima.resource.metadata.ProcessingResourceMetaData)
   */
  public void addMetaData(ProcessingResourceMetaData aMetaData) {
    mMetaDataList.add(aMetaData);
    mCasDefinition = null; // mark this stale
    mCurrentTypeSystem = null; //this too
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.CasManager#getCasDefinition()
   */
  public CasDefinition getCasDefinition() throws ResourceInitializationException {
    if (mCasDefinition == null) {
      mCasDefinition = new CasDefinition(mMetaDataList, mResourceManager);
    }

    return mCasDefinition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.CasManager#getCAS(org.apache.uima.analysis_engine.AnalysisEngine)
   */
  public CAS getCas(String aRequestorContextName) {
    CasPool pool = (CasPool) mRequestorToCasPoolMap.get(aRequestorContextName);
    if (pool == null) {
      throw new UIMARuntimeException(UIMARuntimeException.REQUESTED_TOO_MANY_CAS_INSTANCES,
              new Object[] { aRequestorContextName, "1", "0" });
    }
    return pool.getCas(0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.CasManager#releaseCAS(org.apache.uima.cas.CAS)
   */
  public void releaseCas(AbstractCas aCAS) {
    if (!(aCAS instanceof CASImpl) &&
        !(aCAS instanceof JCas)) {
      throw new UIMARuntimeException(UIMARuntimeException.UNSUPPORTED_CAS_INTERFACE,
          new Object[] {aCAS.getClass()});
    }
    CASImpl baseCas = (aCAS instanceof JCas) ? ((JCas)aCAS).getCasImpl().getBaseCAS() 
                                             : ((CASImpl)aCAS).getBaseCAS();

    CasPool pool = (CasPool) mCasToCasPoolMap.get(baseCas);
    if (pool == null) {
      // CAS doesn't belong to this CasManager!
      throw new UIMARuntimeException(UIMARuntimeException.CAS_RELEASED_TO_WRONG_CAS_MANAGER,
              new Object[0]);
    } else {
      //see if we have a UimaContext that we can notify that CAS was release
      UimaContextAdmin uc = (UimaContextAdmin)mCasToUimaContextMap.get(baseCas);
      if (uc != null) {
        uc.returnedCAS(aCAS);
      }
      
      //release the CAS
      pool.releaseCas((CAS) aCAS);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.CasManager#setMinimumCasPoolSize(java.lang.String, int)
   */
  public void defineCasPool(String aRequestorContextName, int aMinimumSize,
          Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    defineCasPool(null, aRequestorContextName, aMinimumSize, aPerformanceTuningSettings);
  }

  
  
  /* (non-Javadoc)
   * @see org.apache.uima.resource.CasManager#defineCasPool(org.apache.uima.UimaContextAdmin, int, java.util.Properties)
   */
  public void defineCasPool(UimaContextAdmin aRequestorContext, int aMinimumSize, Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    defineCasPool(aRequestorContext, aRequestorContext.getUniqueName(), aMinimumSize, aPerformanceTuningSettings);
  }

  /*
   * Defines a CAS pool using either a UimaContext or the name of a UimaContext.
   */
  private void defineCasPool(UimaContext aRequestorContext, String aRequestorContextName, int aMinimumSize,
          Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    int poolSize = getCasPoolSize(aRequestorContextName, aMinimumSize);
    if (poolSize > 0) {
      CasPool pool = (CasPool) mRequestorToCasPoolMap.get(aRequestorContextName);
      if (pool == null) {
        // this requestor hasn't requested a CAS before
        pool = new CasPool(poolSize, this, aPerformanceTuningSettings);
        populateCasToCasPoolAndUimaContextMaps(pool, aRequestorContext);
        mRequestorToCasPoolMap.put(aRequestorContextName, pool);
        //register with JMX
        registerCasPoolMBean(aRequestorContextName, pool);

      } else {
        throw new UIMARuntimeException(UIMARuntimeException.DEFINE_CAS_POOL_CALLED_TWICE,
                new Object[] { aRequestorContextName });
      }
    }
  }  
  
  /* (non-Javadoc)
   * @see org.apache.uima.resource.CasManager#createNewCas(java.util.Properties)
   */
  public CAS createNewCas(Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    CAS cas;
    if (mCurrentTypeSystem != null) {
      cas = CasCreationUtils.createCas(getCasDefinition(), aPerformanceTuningSettings, mCurrentTypeSystem);      
    } else
    {
      cas = CasCreationUtils.createCas(getCasDefinition(), aPerformanceTuningSettings);
      mCurrentTypeSystem = cas.getTypeSystem();
    }    
    return cas;
  }

  /**
   * Gets a specified interface to a CAS.
   * 
   * @param cas
   *          The CAS
   * @param requiredInterface
   *          interface to get. Currently must be one of CAS or JCas.
   */
  public AbstractCas getCasInterface(CAS cas, Class requiredInterface) {
    if (requiredInterface == CAS.class) {
      return cas;
    } else if (requiredInterface == JCas.class) {
      try {
        return cas.getJCas();
      } catch (CASException e) {
        throw new UIMARuntimeException(e);
      }
    } else if (requiredInterface.isInstance(cas)) // covers AbstractCas
    {
      return cas;
    }
    {
      throw new UIMARuntimeException(UIMARuntimeException.UNSUPPORTED_CAS_INTERFACE,
              new Object[] { requiredInterface });
    }
  }
    
  /* (non-Javadoc)
   * @see org.apache.uima.resource.CasManager#setJmxInfo(java.lang.Object, java.lang.String)
   */
  public void setJmxInfo(Object aMBeanServer, String aRootMBeanName) {
    mMBeanServer = aMBeanServer;
    if (aRootMBeanName.endsWith("\"")) {
      mMBeanNamePrefix = aRootMBeanName.substring(0, aRootMBeanName.length() - 1) + " CAS Pools\",";
    }
    else {
      mMBeanNamePrefix = aRootMBeanName + " CAS Pools,";
    }
  }

  protected Map<CAS, CasPool> getCasToCasPoolMap() {
    return mCasToCasPoolMap;
  }
  
  protected void populateCasToCasPoolMap(CasPool aCasPool) {
    populateCasToCasPoolAndUimaContextMaps(aCasPool, null);
  }
  
  protected void populateCasToCasPoolAndUimaContextMaps(CasPool aCasPool, UimaContext aUimaContext) {
    CAS[] casArray = new CAS[aCasPool.getSize()];
    for (int i = 0; i < casArray.length; i++) {
      casArray[i] = ((CASImpl) aCasPool.getCas()).getBaseCAS();
      mCasToCasPoolMap.put(casArray[i], aCasPool);
      if (aUimaContext != null) {
        mCasToUimaContextMap.put(casArray[i], aUimaContext);
      }
    }
    for (int i = 0; i < casArray.length; i++) {
      aCasPool.releaseCas(casArray[i]);
    }
  }  
  
  /**
   * Registers an MBean for the given CasPool.
   * @param aRequestorContextName context name that identifies this CasPool
   * @param pool the CasPool
   */
  protected void registerCasPoolMBean(String aRequestorContextName, CasPool pool) {
    if (mMBeanNamePrefix != null) {
      String mbeanName = mMBeanNamePrefix + "casPoolContextName=" + aRequestorContextName;
      CasPoolManagementImpl mbean = new CasPoolManagementImpl(pool, mbeanName);
      JmxMBeanAgent.registerMBean(mbean, mMBeanServer);
      casPoolMBeans.add(mbean);
    }
  }  
  
  /**
   * Determines the size to use for a particular CAS Pool.  This can be overridden
   * by subclasses to specify custom pool sizes.
   * @param aRequestorContextName
   *          the context name of the AE that will request the CASes
   *          (AnalysisEngine.getUimaContextAdmin().getQualifiedContextName()).
   * @param aMinimumSize
   *          the minimum CAS pool size required
   * @return the size of the CAS pool to create for the specified AE
   */
  protected int getCasPoolSize(String aRequestorContextName, int aMinimumSize) {
    return aMinimumSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    // unregister MBeans from MBeanServer when GC occurs.
    for (CasPoolManagementImpl mbean : casPoolMBeans) {
      JmxMBeanAgent.unregisterMBean(mbean, mMBeanServer);
    }
    super.finalize();
  }
}
