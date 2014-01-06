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

package org.apache.uima.resource;

import java.util.Properties;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Manages creation and pooling of CAS instances within an AnalysisEngine.
 */
public interface CasManager extends CasOwner {
  /**
   * Called by components when they initialize.
   * 
   * @param aMetadata
   *          an object containing metadata for the component, including Type System, Type
   *          Priorities, and Index Definitions needed to create the CAS.
   */
  void addMetaData(ProcessingResourceMetaData aMetadata);

  /**
   * Gets the CasDefinition, as defined by merging all of the metadata supplied by calls
   * {@link #addMetaData(ProcessingResourceMetaData)}.
   * 
   * @return the merged CasDefinition
   * @throws ResourceInitializationException
   *           if metadata could not be merged
   */
  CasDefinition getCasDefinition() throws ResourceInitializationException;

  /**
   * Defines a CAS pool within this CAS Manager.  (Note that if this CAS pool is to be used
   * by a CAS Multiplier, then {@link #defineCasPool(UimaContextAdmin, int, Properties)}
   * should be used instead.
   * 
   * @param aRequestorContextName
   *          name to give to the pool.  Must be unique among all pool names in this
   *          CAS manager, and care should be taken to assure that this does not match the
   *          qualified name of UIMA Contexts passed to {@link #defineCasPool(UimaContextAdmin, int, Properties)}.
   *          This name is later passed to the {@link #getCas(String)} method, to check out
   *          CASes from the pool.
   * @param aMinimumSize
   *          the minimum CAS pool size required
   * @param aPerformanceTuningSettings
   *          settings, including initial CAS heap size, for the AE
   * @throws ResourceInitializationException
   *           if a CAS could not be created.
   */
  void defineCasPool(String aRequestorContextName, int aMinimumSize, Properties aPerformanceTuningSettings)
          throws ResourceInitializationException;
  
  /**
   * Defines the CAS pool required by a particular AnalysisEngine. (The AnalysisEngine must contain
   * a CAS Multiplier if it requires a CAS pool.)
   * 
   * @param aRequestorContext
   *          the UimaContext of the AE that will request the CASes
   *          (AnalysisEngine.getUimaContextAdmin()).
   * @param aMinimumSize
   *          the minimum CAS pool size required
   * @param aPerformanceTuningSettings
   *          settings, including initial CAS heap size, for the AE
   * @throws ResourceInitializationException
   *           if a CAS could not be created.
   */
  void defineCasPool(UimaContextAdmin aRequestorContext, int aMinimumSize, Properties aPerformanceTuningSettings)
          throws ResourceInitializationException;

  /**
   * Gets an empty CAS. An AnalysisEngine may only call this method after it has first called
   * {@link #defineCasPool(String, int, Properties)} and established a CAS Pool of size &gt; 0. The CAS
   * Manager maintains a separate pool for each AnalysisEngine. This method may block if the CAS
   * pool does not contain any free instances.
   * 
   * @param aRequestorContextName
   *          the context name of the AE requesting the CAS
   *          (AnalysisEngine.getUimaContextAdmin().getQualifiedContextName()).
   * 
   * @return an empty CAS
   */
  CAS getCas(String aRequestorContextName);

  /**
   * Create a new CAS using the CasDefinition stored in this CAS Manager.  Note
   * that this new CAS will not belong to any pool managed by this CAS Manager.
   * If this method is called multiple times while the CasDefinition is constant,
   * each new CAS will share an identical TypeSystem object.
   * 
   * @param aPerformanceTuningSettings
   *          settings, including initial CAS heap size, for the AE
   * @return a new CAS
   * @throws ResourceInitializationException if the CAS could not be created
   */
  CAS createNewCas(Properties aPerformanceTuningSettings) throws ResourceInitializationException;

  /**
   * Gets a specified interface to a CAS.
   * 
   * @param cas
   *          The CAS
   * @param requiredInterface
   *          interface to get. Currently must be either CAS or JCas.
   * @return the CAS Interface
   */
  AbstractCas getCasInterface(CAS cas, Class<? extends AbstractCas> requiredInterface);
  
  /**
   * Configures this CAS Manager so it can register an MBean for each of its CAS pools.
   * 
   * @param aMBeanServer the JMX MBean Server to register MBeans with.  If null, the
   *   platform MBeanServer (Java 1.5+ only) will be used.
   * @param aRootComponentMBeanName unique MBean name for the root component that owns
   *   this CAS Manager.  The names of the CAS Pool MBeans will be formed by appending
   *   <code>,casPoolContextName=[contextName]</code> to the root component name, where
   *   [contextName] is the requestorContextName for the CasPool.
   */
  public void setJmxInfo(Object aMBeanServer, String aRootComponentMBeanName);
}
