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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.CasDefinition;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.ProcessingResourceMetaData_impl;

/**
 * Utilities for creating and setting up CASes. Also includes utilities for merging CAS type
 * systems.
 * 
 * 
 */
public class CasCreationUtils {

  /**
   * Creates a new CAS instance. Note this method does not work for Aggregate Analysis Engine
   * descriptors -- use {@link #createCas(AnalysisEngineDescription)} instead.
   * 
   * @param aMetaData
   *                metadata for the analysis engine that will process this CAS. This is used to set
   *                up the CAS's type system and indexes.
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(AnalysisEngineMetaData aMetaData)
      throws ResourceInitializationException {
    List list = new ArrayList();
    list.add(aMetaData);
    return createCas(list);
  }

  /**
   * Creates a new CAS instance.
   * 
   * @param aMetaData
   *                metadata for the resource that will process this CAS. This is used to set up the
   *                CAS's type system and indexes.
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(ProcessingResourceMetaData aMetaData)
      throws ResourceInitializationException {
    List list = new ArrayList();
    list.add(aMetaData);
    return createCas(list);
  }

  /**
   * Creates a new CAS instance for an Analysis Engine. This works for both primitive and aggregate
   * analysis engines.
   * 
   * @param aDescription
   *                description of the anlaysis engine that will process this CAS. This is used to
   *                set up the CAS's type system and indexes.
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(AnalysisEngineDescription aDescription)
      throws ResourceInitializationException {
    return createCas(aDescription, UIMAFramework.getDefaultPerformanceTuningProperties());
  }

  /**
   * Creates a new CAS instance for an Analysis Engine. This works for both primitive and aggregate
   * analysis engines.
   * 
   * @param aDescription
   *                description of the anlaysis engine that will process this CAS. This is used to
   *                set up the CAS's type system and indexes.
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(AnalysisEngineDescription aDescription,
      Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    List list = new ArrayList();
    list.add(aDescription);
    return createCas(list, aPerformanceTuningSettings);
  }

  /**
   * Creates a new CAS instance for a collection of CAS Processors. This method correctly handles
   * aggregate as well as primitive analysis engines
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(Collection aComponentDescriptionsOrMetaData)
      throws ResourceInitializationException {
    return createCas(aComponentDescriptionsOrMetaData, UIMAFramework
        .getDefaultPerformanceTuningProperties());
  }

  /**
   * Creates a new CAS instance for a collection of CAS Processors. This method correctly handles
   * aggregate as well as primitive analysis engines
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(Collection aComponentDescriptionsOrMetaData,
      Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    return createCas(aComponentDescriptionsOrMetaData, aPerformanceTuningSettings, UIMAFramework
        .newDefaultResourceManager());
  }

  /**
   * Creates a new CAS instance for a collection of CAS Processors. This method correctly handles
   * aggregate as well as primitive analysis engines
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * @param aResourceManager
   *                the resource manager to use to resolve import declarations within the metadata
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(Collection aComponentDescriptionsOrMetaData,
      Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // build a list of metadata objects
    List mdList = getMetaDataList(aComponentDescriptionsOrMetaData, aResourceManager);

    // extract TypeSystems, TypePriorities, and FsIndexes from metadata
    List typeSystems = new ArrayList();
    List typePriorities = new ArrayList();
    List fsIndexes = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
      if (md.getFsIndexCollection() != null)
        fsIndexes.add(md.getFsIndexCollection());
    }

    // merge
    TypeSystemDescription aggTypeDesc = mergeTypeSystems(typeSystems, aResourceManager);
    TypePriorities aggTypePriorities = mergeTypePriorities(typePriorities, aResourceManager);
    FsIndexCollection aggIndexColl = mergeFsIndexes(fsIndexes, aResourceManager);

    return doCreateCas(null, aggTypeDesc, aggTypePriorities, aggIndexColl.getFsIndexes(),
        aPerformanceTuningSettings, aResourceManager);
  }

  /**
   * Creates a new CAS instance.
   * 
   * @param aTypeSystem
   *                type system to install in the CAS
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(TypeSystemDescription aTypeSystem, TypePriorities aTypePriorities,
      FsIndexDescription[] aFsIndexes) throws ResourceInitializationException {
    return createCas(aTypeSystem, aTypePriorities, aFsIndexes, null, null);
  }

  /**
   * Creates a new CAS instance.
   * 
   * @param aTypeSystem
   *                type system to install in the CAS
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(TypeSystemDescription aTypeSystem, TypePriorities aTypePriorities,
      FsIndexDescription[] aFsIndexes, Properties aPerformanceTuningSettings)
      throws ResourceInitializationException {
    return createCas(aTypeSystem, aTypePriorities, aFsIndexes, aPerformanceTuningSettings, null);
  }

  /**
   * Creates a new CAS instance.
   * 
   * @param aTypeSystem
   *                type system to install in the CAS
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(TypeSystemDescription aTypeSystem, TypePriorities aTypePriorities,
      FsIndexDescription[] aFsIndexes, Properties aPerformanceTuningSettings,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    return doCreateCas(null, aTypeSystem, aTypePriorities, aFsIndexes, aPerformanceTuningSettings,
        aResourceManager);
  }

  /**
   * Creates a new CAS instance for a collection of CAS Processors, which. reuses an existing type
   * system. Using this method allows several CASes to all share the exact same type system object.
   * This method correctly handles aggregate as well as primitive analysis engines.
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aTypeSystem
   *                type system to install in the CAS, null if none
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(Collection aComponentDescriptionsOrMetaData, TypeSystem aTypeSystem,
      Properties aPerformanceTuningSettings) throws ResourceInitializationException {
    return createCas(aComponentDescriptionsOrMetaData, aTypeSystem, aPerformanceTuningSettings,
        UIMAFramework.newDefaultResourceManager());
  }

  /**
   * Creates a new CAS instance for a collection of CAS Processors, which. reuses an existing type
   * system. Using this method allows several CASes to all share the exact same type system object.
   * This method correctly handles aggregate as well as primitive analysis engines.
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aTypeSystem
   *                type system to install in the CAS, null if none
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * @param aResourceManager
   *                the resource manager to use to resolve import declarations within the metadata,
   *                and also to set the JCas ClassLoader for the new CAS
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(Collection aComponentDescriptionsOrMetaData, TypeSystem aTypeSystem,
      Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // build a list of metadata objects
    List mdList = getMetaDataList(aComponentDescriptionsOrMetaData, aResourceManager);

    // extract TypeSystems, TypePriorities, and FsIndexes from metadata
    List typeSystems = new ArrayList();
    List typePriorities = new ArrayList();
    List fsIndexes = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
      if (md.getFsIndexCollection() != null)
        fsIndexes.add(md.getFsIndexCollection());
    }

    // merge TypePriorities and FsIndexes
    TypePriorities aggTypePriorities = mergeTypePriorities(typePriorities, aResourceManager);
    FsIndexCollection aggIndexColl = mergeFsIndexes(fsIndexes, aResourceManager);

    if (aTypeSystem != null) // existing type system object was specified; use that
    {
      return doCreateCas(aTypeSystem, null, aggTypePriorities, aggIndexColl.getFsIndexes(),
          aPerformanceTuningSettings, aResourceManager);
    } else {
      // no type system object specified; merge type system descriptions in metadata
      TypeSystemDescription aggTypeDesc = mergeTypeSystems(typeSystems);
      return doCreateCas(null, aggTypeDesc, aggTypePriorities, aggIndexColl.getFsIndexes(),
          aPerformanceTuningSettings, aResourceManager);
    }
  }

  /**
   * Creates a new CAS instance that reuses an existing type system. Using this method allows
   * several CASes to all share the exact same type system object.
   * 
   * @param aTypeSystem
   *                type system to install in the CAS
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(TypeSystem aTypeSystem, TypePriorities aTypePriorities,
      FsIndexDescription[] aFsIndexes, Properties aPerformanceTuningSettings)
      throws ResourceInitializationException {
    return createCas(aTypeSystem, aTypePriorities, aFsIndexes, aPerformanceTuningSettings, null);
  }

  /**
   * Creates a new CAS instance that reuses an existing type system. Using this method allows
   * several CASes to all share the exact same type system object.
   * 
   * @param aTypeSystem
   *                type system to install in the CAS
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * @param aResourceManager
   *                resource manager, which is used to set the JCas ClassLoader for the new CAS
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  public static CAS createCas(TypeSystem aTypeSystem, TypePriorities aTypePriorities,
      FsIndexDescription[] aFsIndexes, Properties aPerformanceTuningSettings,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    return doCreateCas(aTypeSystem, null, aTypePriorities, aFsIndexes, aPerformanceTuningSettings,
        aResourceManager);
  }

  /**
   * Method that does the work for creating a new CAS instance. Other createCas methods in this
   * class should all eventually call this method, so that the critical code is not duplicated in
   * more than one place.
   * 
   * @param aTypeSystem
   *                an existing type sytsem to reuse in this CAS, null if none.
   * @param aTypeSystemDescription
   *                description of type system to use for this CAS. This is only used if aTypeSystem
   *                is null.
   * @param aTypePriorities
   *                type priorities to install in the CAS
   * @param aFsIndexes
   *                indexes to install in the CAS
   * @param aPerformanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS instance
   * 
   * @throws ResourceInitializationException
   *                 if CAS creation fails
   */
  private static CAS doCreateCas(TypeSystem aTypeSystem, TypeSystemDescription aTypeSystemDesc,
      TypePriorities aTypePriorities, FsIndexDescription[] aFsIndexes,
      Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    if (aResourceManager == null) {
      aResourceManager = UIMAFramework.newDefaultResourceManager();
    }

    // resolve imports
    try {
      if (aTypeSystemDesc != null) {
        aTypeSystemDesc.resolveImports(aResourceManager);
        //even though there's only one Type System, we still need to do a merge, to handle the
        //case where this TypeSystem defines the same type more than once (or has imports that do)
        List tsList = new ArrayList();
        tsList.add(aTypeSystemDesc);
        aTypeSystemDesc = mergeTypeSystems(tsList, aResourceManager, null);        
      }
      if (aTypePriorities != null) {
        aTypePriorities.resolveImports(aResourceManager);
      }
    } catch (InvalidXMLException e) {
      throw new ResourceInitializationException(e);
    }

    // get initial heap size
    String initialHeapSizeStr = null;
    if (aPerformanceTuningSettings != null) {
      initialHeapSizeStr = aPerformanceTuningSettings
          .getProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE);
    }
    
    // Check Jcas cache performance setting.  Defaults to true.
    boolean useJcasCache = false;
    if (aPerformanceTuningSettings != null) {
      String useJcasCacheString = aPerformanceTuningSettings.getProperty(
          UIMAFramework.JCAS_CACHE_ENABLED, "true");
      if ("false".equalsIgnoreCase(useJcasCacheString)) {
        useJcasCache = false;
      }
    }

    // create CAS using either aTypeSystem or aTypeSystemDesc
    CASMgr casMgr;
    if (aTypeSystem != null) {
      if (initialHeapSizeStr != null) {
        casMgr = CASFactory.createCAS(Integer.parseInt(initialHeapSizeStr), aTypeSystem, useJcasCache);
      } else {
        casMgr = CASFactory.createCAS(aTypeSystem, useJcasCache);
      }
    } else // no TypeSystem to reuse - create a new one
    {
      if (initialHeapSizeStr != null) {
        casMgr = CASFactory.createCAS(Integer.parseInt(initialHeapSizeStr), useJcasCache);
      } else {
        casMgr = CASFactory.createCAS();
      }
      // install type system
      setupTypeSystem(casMgr, aTypeSystemDesc);
      // Commit the type system
      ((CASImpl) casMgr).commitTypeSystem();
    }

    try {
      // install TypePriorities into CAS
      setupTypePriorities(casMgr, aTypePriorities);

      // install Built-in indexes into CAS
      casMgr.initCASIndexes();
    } catch (CASException e) {
      throw new ResourceInitializationException(e);
    }

    // install AnalysisEngine's custom indexes into CAS
    setupIndexes(casMgr, aFsIndexes);

    // Commit the index repository
    casMgr.getIndexRepositoryMgr().commit();

    // Set JCas ClassLoader
    if (aResourceManager.getExtensionClassLoader() != null) {
      casMgr.setJCasClassLoader(aResourceManager.getExtensionClassLoader());
    }

    return casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);
  }

  /**
   * Create a CAS from a CAS Definition.
   * 
   * @param casDef
   *                completely describes the CAS to be created
   * @param performanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * 
   * @return a new CAS matching the given CasDefinition
   */
  public static CAS createCas(CasDefinition casDef, Properties performanceTuningSettings)
      throws ResourceInitializationException {
    return createCas(casDef.getTypeSystemDescription(), casDef.getTypePriorities(), casDef
        .getFsIndexDescriptions(), performanceTuningSettings, casDef.getResourceManager());
  }

  /**
   * Create a CAS from a CAS Definition, but reuse the provided TypeSystem object.
   * 
   * @param casDef
   *                completely describes the CAS to be created
   * @param performanceTuningSettings
   *                Properties object containing framework performance tuning settings using key
   *                names defined on {@link UIMAFramework} interface
   * @param typeSystem
   *                type system object to reuse
   * 
   * @return a new CAS matching the given CasDefinition
   */
  public static CAS createCas(CasDefinition casDef, Properties performanceTuningSettings,
      TypeSystem typeSystem) throws ResourceInitializationException {
    return createCas(typeSystem, casDef.getTypePriorities(), casDef.getFsIndexDescriptions(),
        performanceTuningSettings, casDef.getResourceManager());
  }

  /**
   * Installs a TypeSystem in a CAS.
   * 
   * @param aCASMgr
   *                the <code>CASMgr</code> object whose type system is to be modified.
   * @param aTypeSystem
   *                desription of type system to install
   * 
   * @throws ResourceInitializationException
   *                 if an error occurs during modification of the type system
   */
  public static void setupTypeSystem(CASMgr aCASMgr, TypeSystemDescription aTypeSystem)
      throws ResourceInitializationException {
    TypeSystemMgr typeSystemMgr = aCASMgr.getTypeSystemMgr();
    if (aTypeSystem != null) {
      TypeDescription[] types = aTypeSystem.getTypes();
      if (types != null) {
        // add all Types first (so that we can handle forward references) - note
        // that it isn't guarnanteed that a supertype will occur in the Types list
        // before its subtype.

        // Build a linked list of type descriptions. We will make multiple passes
        // over this, adding types to the CAS and removing them from the linked
        // list. We continue until the list is empty or we cannot make any
        // progress.
        LinkedList typeList = new LinkedList();
        typeList.addAll(Arrays.asList(types));
        int numTypes = typeList.size();
        int lastNumTypes;
        LinkedList typesInOrderOfCreation = new LinkedList();
        do {
          lastNumTypes = numTypes;
          Iterator it = typeList.iterator();
          while (it.hasNext()) {
            TypeDescription curTypeDesc = (TypeDescription) it.next();
            String typeName = curTypeDesc.getName();
            // type does not exist - add it under the appropriate supertype
            String superTypeName = curTypeDesc.getSupertypeName();
            if (superTypeName == null) {
              throw new ResourceInitializationException(
                  ResourceInitializationException.NO_SUPERTYPE, new Object[] { typeName,
                      curTypeDesc.getSourceUrlString() });
            }
            // Check if it's a built-in type: must not change supertype!
            Type builtIn = typeSystemMgr.getType(typeName);
            if (builtIn != null) {
              if (!superTypeName.equals(typeSystemMgr.getParent(builtIn).getName())) {
                throw new ResourceInitializationException(
                    ResourceInitializationException.REDEFINING_BUILTIN_TYPE, new Object[] {
                        typeSystemMgr.getParent(builtIn), typeName, superTypeName,
                        curTypeDesc.getSourceUrlString() });
              }
            }
            Type supertype = typeSystemMgr.getType(superTypeName);
            if (supertype != null) {
              // supertype is defined, so add to CAS type system
              // check for special "enumerated types" that extend String
              if (curTypeDesc.getSupertypeName().equals(CAS.TYPE_NAME_STRING)) {
                AllowedValue[] vals = curTypeDesc.getAllowedValues();
                if (vals == null) {
                  throw new ResourceInitializationException(
                      ResourceInitializationException.MISSING_ALLOWED_VALUES, new Object[] {
                          typeName, curTypeDesc.getSourceUrlString() });
                }
                String[] valStrs = new String[vals.length];
                for (int i = 0; i < valStrs.length; i++) {
                  valStrs[i] = vals[i].getString();
                }
                typeSystemMgr.addStringSubtype(typeName, valStrs);
              } else // a "normal" type
              {
                // make sure that allowed values are NOT specified for non-string subtypes
                if (curTypeDesc.getAllowedValues() != null
                    && curTypeDesc.getAllowedValues().length > 0) {
                  throw new ResourceInitializationException(
                      ResourceInitializationException.ALLOWED_VALUES_ON_NON_STRING_TYPE,
                      new Object[] { typeName, curTypeDesc.getSourceUrlString() });
                }
                typeSystemMgr.addType(typeName, supertype);
              }
              // remove from list of type descriptions and add it to the typesInOrderOfCreation list
              // for later processing
              it.remove();
              typesInOrderOfCreation.add(curTypeDesc);
            }
          }
          numTypes = typeList.size();
        } while (numTypes > 0 && numTypes != lastNumTypes);
        // we quit the above loop either when we've added all types or when
        // we went through the entire list without successfully finding any
        // supertypes. In the latter case, throw an exception. Since there
        // can be more than one such type, we look for one that does not have
        // ancestor in the list as it is the likely cause of the issue. The
        // implementation of this is not as efficient as it could be but avoids
        // issues with cyclic definitions.
        for (int i = 0; i < typeList.size(); i++) {
          TypeDescription td_i = (TypeDescription) typeList.get(i);
          boolean foundSuperType = false;
          for (int j = 0; j < typeList.size(); j++) {
            if (i == j) {
              continue;
            }
            TypeDescription td_j = (TypeDescription) typeList.get(j);
            if (td_j.getName().equals(td_i.getSupertypeName())) {
              foundSuperType = true;
              break;
            }
          }
          if (!foundSuperType) {
            throw new ResourceInitializationException(
                ResourceInitializationException.UNDEFINED_SUPERTYPE, new Object[] {
                    td_i.getSupertypeName(), td_i.getName(), td_i.getSourceUrlString() });
          }
        }

        if (numTypes > 0) {
          // We get here in either of two cases: there was only one problematic
          // type definition, or there was a cycle.
          TypeDescription firstFailed = (TypeDescription) typeList.getFirst();
          throw new ResourceInitializationException(
              ResourceInitializationException.UNDEFINED_SUPERTYPE, new Object[] {
                  firstFailed.getSupertypeName(), firstFailed.getName(),
                  firstFailed.getSourceUrlString() });
        }

        // now for each type, add its features. We add features to supertypes before subtypes. This
        // is done so that
        // if we have a duplicate feature name on both a supertype and a subtype, it is added to the
        // supertype and then
        // ignored when we get to the subtype. Although this is a dubious type system, we support it
        // for backwards
        // compatibility (but we might want to think about generating a warning).
        Iterator typeIter = typesInOrderOfCreation.iterator();
        while (typeIter.hasNext()) {
          TypeDescription typeDesc = (TypeDescription) typeIter.next();
          Type type = typeSystemMgr.getType(typeDesc.getName());
          // assert type != null;

          FeatureDescription[] features = typeDesc.getFeatures();
          if (features != null) {
            for (int j = 0; j < features.length; j++) {
              String featName = features[j].getName();
              String rangeTypeName = features[j].getRangeTypeName();
              Type rangeType = typeSystemMgr.getType(rangeTypeName);
              if (rangeType == null) {
                throw new ResourceInitializationException(
                    ResourceInitializationException.UNDEFINED_RANGE_TYPE, new Object[] {
                        rangeTypeName, featName, typeDesc.getName(),
                        features[j].getSourceUrlString() });
              }
              if (rangeType.isArray()) // TODO: also List?
              {
                // if an element type is specified, get the specific
                // array subtype for that element type
                String elementTypeName = features[j].getElementType();
                if (elementTypeName != null && elementTypeName.length() > 0) {
                  Type elementType = typeSystemMgr.getType(elementTypeName);
                  if (elementType == null) {
                    throw new ResourceInitializationException(
                        ResourceInitializationException.UNDEFINED_RANGE_TYPE, new Object[] {
                            elementTypeName, featName, typeDesc.getName(),
                            features[j].getSourceUrlString() });
                  }
                  rangeType = typeSystemMgr.getArrayType(elementType);
                }
              }
              Boolean multiRefAllowed = features[j].getMultipleReferencesAllowed();
              if (multiRefAllowed == null) {
                multiRefAllowed = Boolean.FALSE; // default to false if unspecified
              }
              typeSystemMgr.addFeature(featName, type, rangeType, multiRefAllowed.booleanValue());
            }
          }
        }
      }
    }
  }

  /**
   * Adds TypePriorities to a CAS.
   * 
   * @param aCASMgr
   *                the <code>CASMgr</code> object to be modified
   * @param aTypePriorities
   *                description of the type priorities to add
   * 
   * @throws CASException
   *                 if an error occurs during type priority setup
   */
  public static void setupTypePriorities(CASMgr aCASMgr, TypePriorities aTypePriorities)
      throws ResourceInitializationException {
    if (aTypePriorities != null) {
      LinearTypeOrderBuilder typeOrderBuilder = aCASMgr.getIndexRepositoryMgr()
          .getDefaultOrderBuilder();
      TypePriorityList[] priorityLists = aTypePriorities.getPriorityLists();
      for (int i = 0; i < priorityLists.length; i++) {
        // check that all types exist. This error would be caught in
        // typeOrderBuilder.getOrder(), but that's too late to indicate
        // the location of the faulty descriptor in the error message.
        String[] typeList = priorityLists[i].getTypes();
        for (int j = 0; j < typeList.length; j++) {
          if (aCASMgr.getTypeSystemMgr().getType(typeList[j]) == null) {
            throw new ResourceInitializationException(
                ResourceInitializationException.UNDEFINED_TYPE_FOR_PRIORITY_LIST, new Object[] {
                    typeList[j], priorityLists[i].getSourceUrlString() });
          }
        }
        try {
          typeOrderBuilder.add(priorityLists[i].getTypes());
        } catch (CASException e) {
          // typically caused by a cycle in the priorities - the caused-by message
          // will clarify.
          throw new ResourceInitializationException(
              ResourceInitializationException.INVALID_TYPE_PRIORITIES,
              new Object[] { priorityLists[i].getSourceUrlString() }, e);
        }
      }
    }
  }

  /**
   * Adds FeatureStructure indexes to a CAS.
   * 
   * @param aCASMgr
   *                the <code>CASMgr</code> object to be modified
   * @param aIndexes
   *                descriptions of the indexes to add
   * 
   * @throws ResourceInitializationException
   *                 if an error occurs during index creation
   */
  public static void setupIndexes(CASMgr aCASMgr, FsIndexDescription[] aIndexes)
      throws ResourceInitializationException {
    if (aIndexes != null) {
      TypeSystemMgr tsm = aCASMgr.getTypeSystemMgr();
      FSIndexRepositoryMgr irm = aCASMgr.getIndexRepositoryMgr();

      for (int i = 0; i < aIndexes.length; i++) {
        int kind = FSIndex.SORTED_INDEX;
        String kindStr = aIndexes[i].getKind();
        if (kindStr != null) {
          if (kindStr.equals(FsIndexDescription.KIND_BAG))
            kind = FSIndex.BAG_INDEX;
          else if (kindStr.equals(FsIndexDescription.KIND_SET))
            kind = FSIndex.SET_INDEX;
          else if (kindStr.equals(FsIndexDescription.KIND_SORTED))
            kind = FSIndex.SORTED_INDEX;
        }

        Type type = tsm.getType(aIndexes[i].getTypeName());
        if (type == null) {
          throw new ResourceInitializationException(
              ResourceInitializationException.UNDEFINED_TYPE_FOR_INDEX, new Object[] {
                  aIndexes[i].getTypeName(), aIndexes[i].getLabel(),
                  aIndexes[i].getSourceUrlString() });
        }
        FSIndexComparator comparator = irm.createComparator();
        comparator.setType(type);

        FsIndexKeyDescription[] keys = aIndexes[i].getKeys();
        if (keys != null) {
          for (int j = 0; j < keys.length; j++) {
            if (keys[j].isTypePriority()) {
              comparator.addKey(irm.getDefaultTypeOrder(), FSIndexComparator.STANDARD_COMPARE);
            } else {
              Feature feature = type.getFeatureByBaseName(keys[j].getFeatureName());
              if (feature == null) {
                throw new ResourceInitializationException(
                    ResourceInitializationException.INDEX_KEY_FEATURE_NOT_FOUND, new Object[] {
                        keys[j].getFeatureName(), aIndexes[i].getLabel(),
                        aIndexes[i].getSourceUrlString() });
              }
              comparator.addKey(feature, keys[j].getComparator());
            }
          }
        }

        irm.createIndex(comparator, aIndexes[i].getLabel(), kind);
      }
    }
  }

  /**
   * Extracts a TypeSystem definition from a CasData.
   * 
   * @param aCasData
   *                the CAS Data from which to extract the type system
   * 
   * @return a description of a TypeSystem to which the CAS Data conforms
   */
  public static TypeSystemDescription convertData2TypeSystem(CasData aCasData) {
    TypeSystemDescription result = UIMAFramework.getResourceSpecifierFactory()
        .createTypeSystemDescription();
    Iterator iter = aCasData.getFeatureStructures();
    ArrayList typesArr = new ArrayList();
    while (iter.hasNext()) {
      FeatureStructure casFS = (FeatureStructure) iter.next();
      TypeDescription newType = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
      newType.setName(casFS.getType());
      newType.setSupertypeName("uima.tcas.annotation");
      newType.setDescription("CasData Type");
      String features[] = casFS.getFeatureNames();
      if (features != null) {
        for (int i = 0; i < features.length; i++) {
          String featName = features[i];
          String rangeName = "";
          String description = "";
          PrimitiveValue pVal = (PrimitiveValue) casFS.getFeatureValue(featName);
          if (pVal.get().getClass().getName().equals("java.lang.String")) {
            System.out.println(" the feature is a String ");
            rangeName = "uima.cas.String";
            description = " featue of the casDataType";
          }
          newType.addFeature(featName, description, rangeName);
        }
      }
      typesArr.add(newType);
    }
    TypeDescription td[] = new TypeDescription[typesArr.size()];
    for (int j = 0; j < typesArr.size(); j++) {
      td[j] = (TypeDescription) typesArr.get(j);
    }
    result.setTypes(td);
    return result;
  }

  /**
   * Merges several TypeSystemDescriptions into one. Also resolves imports in the
   * TypeSystemDescription objects.
   * 
   * @param aTypeSystems
   *                a collection of TypeSystems to be merged
   * 
   * @return a new TypeSystemDescription that is the result of merging all of the type systems
   *         together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection aTypeSystems)
      throws ResourceInitializationException {
    return mergeTypeSystems(aTypeSystems, UIMAFramework.newDefaultResourceManager());
  }

  /**
   * Merges several TypeSystemDescriptions into one. Also resolves imports in the
   * TypeSystemDescription objects.
   * 
   * @param aTypeSystems
   *                a collection of TypeSystems to be merged
   * @param aResourceManager
   *                Resource Manager to use to locate type systems imported by name
   * 
   * @return a new TypeSystemDescription that is the result of merging all of the type systems
   *         together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection aTypeSystems,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    return mergeTypeSystems(aTypeSystems, aResourceManager, null);
  }

  /**
   * Merges several TypeSystemDescriptions into one. Also resolves imports in the
   * TypeSystemDescription objects.
   * <p>
   * This version of this method takes an argument <code>aOutputMergedTypes</code>, which this
   * method will populate with the names and descriptor locations of any types whose definitions
   * have been merged from multiple non-identical sources. That is, types that are declared more
   * than once, with different (but compatible) sets of features in each declaration, or with
   * different (but compatible) supertypes.
   * 
   * @param aTypeSystems
   *                a collection of TypeSystems to be merged
   * @param aResourceManager
   *                Resource Manager to use to locate type systems imported by name
   * @param aOutputMergedTypes
   *                A Map that this method will populate with information about the set of types
   *                whose definitions were merged from multiple non-identical sources. The keys in
   *                the Map will be the type names (Strings) and the values will be {link Set}s
   *                containing Descriptor URLs (Strings) where those types are declared. You may
   *                pass null if you are not interested in this information.
   * 
   * @return a new TypeSystemDescription that is the result of merging all of the type systems
   *         together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection aTypeSystems,
      ResourceManager aResourceManager, Map aOutputMergedTypes)
      throws ResourceInitializationException {
    // create the type system into which we are merging
    TypeSystemDescription result = UIMAFramework.getResourceSpecifierFactory()
        .createTypeSystemDescription();
    // also build a Map from Type names to Types
    Map typeNameMap = new HashMap();

    // Iterate through all type systems and add types to the merged TypeSystem.
    // If a type is defined more than once, we need to check if the superType
    // declarations are compatible (one inherits from another), and merge the
    // features.
    
    // In order to propertly handle the supertype merging, we need to make sure
    // that we process the supertype definitions before the subtypes. To do this,
    // we build a linked list of type descriptions, and make multiple passes
    // over this, adding types to the merged type system when their supertypes
    // become defined.  We continue until the list is empty or we cannot make any
    // progress.
    LinkedList typeList = new LinkedList();
    Iterator it = aTypeSystems.iterator();
    while (it.hasNext()) {
      TypeSystemDescription ts = (TypeSystemDescription) it.next();
      if (ts != null) {
        try {
          ts.resolveImports(aResourceManager);
        } catch (InvalidXMLException e) {
          throw new ResourceInitializationException(e);
        }
        TypeDescription[] types = ts.getTypes();
        typeList.addAll(Arrays.asList(types));
      }
    }
    int lastNumTypes;
    do {
      lastNumTypes = typeList.size();
      Iterator typeIter = typeList.iterator();
      while (typeIter.hasNext()) {
        TypeDescription type = (TypeDescription)typeIter.next();
        String supertypeName = type.getSupertypeName();
        if (supertypeName.startsWith("uima.cas") || supertypeName.startsWith("uima.tcas") || typeNameMap.containsKey(supertypeName)) {
          //supertype is defined, ok to proceed
          //check if type is already defined 
          addTypeToMergedTypeSystem(aOutputMergedTypes, result, typeNameMap, type);
          typeIter.remove();
        }
      }
    } while (typeList.size() > 0 && typeList.size() != lastNumTypes);
      
    //At this point, if the typeList is not empty, then we either have a type with an undefined supertype, or a cycle.
    //We go ahead and merge the type definitions anway - these problems will be caught at CAS creation time. Undefined supertypes 
    //may be OK at this stage - this type system will have to be further merged before it can be used.
    Iterator typeIter = typeList.iterator();
    while (typeIter.hasNext()) {
      TypeDescription type = (TypeDescription)typeIter.next();
      addTypeToMergedTypeSystem(aOutputMergedTypes, result, typeNameMap, type);
    }    
    return result;
  }

  private static void addTypeToMergedTypeSystem(Map aOutputMergedTypes, TypeSystemDescription result, Map typeNameMap, TypeDescription type) throws ResourceInitializationException {
    String typeName = type.getName();
    String supertypeName = type.getSupertypeName();
    TypeDescription existingType = (TypeDescription) typeNameMap.get(typeName);
    if (existingType == null) {
      // create new type
      existingType = result.addType(typeName, type.getDescription(), supertypeName);
      existingType.setAllowedValues(type.getAllowedValues());
      existingType.setSourceUrl(type.getSourceUrl());
      typeNameMap.put(type.getName(), existingType);
      FeatureDescription[] features = type.getFeatures();
      if (features != null) {
        mergeFeatures(existingType, type.getFeatures());
      }
    } else {
      // type already existed - check that supertypes are compatible
      String existingSupertypeName = existingType.getSupertypeName();
      if (!existingSupertypeName.equals(supertypeName)) {
        // supertypes are not identical - check if one subsumes the other
        if (subsumes(existingSupertypeName, supertypeName, typeNameMap)) {
          // existing supertype subsumes newly specified supertype -
          // reset supertype to the new, more specific type
          existingType.setSupertypeName(supertypeName);
          // report that a merge occurred
          reportMerge(aOutputMergedTypes, type, existingType);
        } else if (subsumes(supertypeName, existingSupertypeName, typeNameMap)) {
          // newly specified supertype subsumes old type, this is OK and we don't
          // need to do anything except report this
          reportMerge(aOutputMergedTypes, type, existingType);
        } else {
          // error
          throw new ResourceInitializationException(
              ResourceInitializationException.INCOMPATIBLE_SUPERTYPES, new Object[] {
                  typeName, supertypeName, existingSupertypeName,
                  type.getSourceUrlString() });
        }
      }
      // merge features
      int prevNumFeatures = existingType.getFeatures().length;
      FeatureDescription[] features = type.getFeatures();
      if (features != null) {
        mergeFeatures(existingType, type.getFeatures());
        // if feature-merged occurred, the number of features on the type will have
        // changed. Report this by adding to the aOutputMergedTypeNames collection.
        if (existingType.getFeatures().length != prevNumFeatures) {
          reportMerge(aOutputMergedTypes, type, existingType);
        }
      }
      
    }
  }

  /**
   * Utility method for populating the aOutputMergedTypes argument in the mergeTypeSystems method.
   * 
   * @param aOutputMergedTypes
   *                Map to populate
   * @param currentType
   *                TypeDescription currently being processed
   * @param existingType
   *                TypeDescription that already existed for the same name
   */
  private static void reportMerge(Map aOutputMergedTypes, TypeDescription currentType,
      TypeDescription existingType) {
    if (aOutputMergedTypes != null) {
      String typeName = currentType.getName();
      Set descriptorUrls = (Set) aOutputMergedTypes.get(typeName);
      if (descriptorUrls == null) {
        descriptorUrls = new TreeSet();
        descriptorUrls.add(existingType.getSourceUrlString());
        descriptorUrls.add(currentType.getSourceUrlString());
        aOutputMergedTypes.put(typeName, descriptorUrls);
      } else {
        descriptorUrls.add(currentType.getSourceUrlString());
      }
    }
  }

  /**
   * Merges the Type Systems of each component within an aggregate Analysis Engine, producing a
   * single combined Type System.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * 
   * @return a new TypeSystemDescription that is the result of merging all of the delegate AE type
   *         systems together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeDelegateAnalysisEngineTypeSystems(
      AnalysisEngineDescription aAggregateDescription) throws ResourceInitializationException {
    return mergeDelegateAnalysisEngineTypeSystems(aAggregateDescription, UIMAFramework
        .newDefaultResourceManager());
  }

  /**
   * Merges the Type Systems of each component within an aggregate Analysis Engine, producing a
   * single combined Type System.
   * <p>
   * This version of this method takes an argument <code>aOutputMergedTypeNames</code>, to which
   * this method will add the names of any types whose definitions have been merged from multiple
   * non-identical sources. That is, types that are declared more than once, with different (but
   * compatible) sets of features in each declaration, or with different (but compatible)
   * supertypes.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * @param aResourceManager
   *                ResourceManager instance used to resolve imports
   * 
   * @return a new TypeSystemDescription that is the result of merging all of the delegate AE type
   *         systems together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeDelegateAnalysisEngineTypeSystems(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    return mergeDelegateAnalysisEngineTypeSystems(aAggregateDescription, aResourceManager, null);
  }

  /**
   * Merges the Type Systems of each component within an aggregate Analysis Engine, producing a
   * single combined Type System.
   * <p>
   * This version of this method takes an argument <code>aOutputMergedTypes</code>, which this
   * method will populate with the names and descriptor locations of any types whose definitions
   * have been merged from multiple non-identical sources. That is, types that are declared more
   * than once, with different (but compatible) sets of features in each declaration, or with
   * different (but compatible) supertypes.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * @param aResourceManager
   *                ResourceManager instance used to resolve imports
   * @param aOutputMergedTypes
   *                A Map that this method will populate with information about the set of types
   *                whose definitions were merged from multiple non-identical sources. The keys in
   *                the Map will be the type names (Strings) and the values will be {link Set}s
   *                containing Descriptor URLs (Strings) where those types are declared. You may
   *                pass null if you are not interested in this information. *
   * @return a new TypeSystemDescription that is the result of merging all of the delegate AE type
   *         systems together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeDelegateAnalysisEngineTypeSystems(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager,
      Map aOutputMergedTypes) throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    ArrayList l = new ArrayList();
    l.add(aAggregateDescription);
    List mdList = getMetaDataList(l, aResourceManager);

    // extract type systems and merge
    List typeSystems = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
    }
    return mergeTypeSystems(typeSystems, aResourceManager, aOutputMergedTypes);
  }

  /**
   * Merges a List of FsIndexCollections into a single FsIndexCollection object.
   * 
   * @param aFsIndexCollections
   *                list of FsIndexCollection objects
   * @param aResourceManager
   *                ResourceManager instance to use to resolve imports
   * 
   * @return a merged FsIndexCollection object
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static FsIndexCollection mergeFsIndexes(List aFsIndexCollections,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    Map aggIndexes = new HashMap();
    Iterator it = aFsIndexCollections.iterator();
    while (it.hasNext()) {
      FsIndexCollection indexColl = (FsIndexCollection) it.next();

      if (indexColl != null) {
        try {
          indexColl.resolveImports(aResourceManager);
        } catch (InvalidXMLException e) {
          throw new ResourceInitializationException(e);
        }
        FsIndexDescription[] indexes = indexColl.getFsIndexes();
        for (int i = 0; i < indexes.length; i++) {
          // does an index with this label already exist?
          FsIndexDescription duplicateIndex = (FsIndexDescription) aggIndexes.get(indexes[i]
              .getLabel());
          if (duplicateIndex == null) {
            // no, so add it
            aggIndexes.put(indexes[i].getLabel(), indexes[i]);
          } else if (!duplicateIndex.equals(indexes[i])) {
            // index with same label exists, they better be equal!
            throw new ResourceInitializationException(
                ResourceInitializationException.DUPLICATE_INDEX_NAME, new Object[] {
                    duplicateIndex.getLabel(), duplicateIndex.getSourceUrlString(),
                    indexes[i].getSourceUrlString() });
          }
        }
      }
    }

    // convert index map to FsIndexCollection
    FsIndexCollection aggIndexColl = UIMAFramework.getResourceSpecifierFactory()
        .createFsIndexCollection();
    Collection indexes = aggIndexes.values();
    FsIndexDescription[] indexArray = new FsIndexDescription[indexes.size()];
    indexes.toArray(indexArray);
    aggIndexColl.setFsIndexes(indexArray);
    return aggIndexColl;
  }

  /**
   * Merges the FS Index Collections of each component within an aggregate Analysis Engine,
   * producing a single combined FS Index Collection.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * 
   * @return a new FsIndexCollection that is the result of merging all of the delegate AE
   *         FsIndexCollections together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static FsIndexCollection mergeDelegateAnalysisEngineFsIndexCollections(
      AnalysisEngineDescription aAggregateDescription) throws ResourceInitializationException {
    return mergeDelegateAnalysisEngineFsIndexCollections(aAggregateDescription, UIMAFramework
        .newDefaultResourceManager());
  }

  /**
   * Merges the FS Index Collections of each component within an aggregate Analysis Engine,
   * producing a single combined FS Index Collection.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * @param aResourceManager
   *                ResourceManager instance used to resolve imports
   * 
   * @return a new FsIndexCollection that is the result of merging all of the delegate AE
   *         FsIndexCollections together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static FsIndexCollection mergeDelegateAnalysisEngineFsIndexCollections(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    ArrayList l = new ArrayList();
    l.add(aAggregateDescription);
    List mdList = getMetaDataList(l, aResourceManager);

    // extract FsIndexCollections and merge
    List fsIndexes = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getFsIndexCollection() != null)
        fsIndexes.add(md.getFsIndexCollection());
    }
    return mergeFsIndexes(fsIndexes, aResourceManager);
  }

  /**
   * Merges a List of TypePriorities into a single TypePriorities object.
   * 
   * @param aTypePriorities
   *                list of TypePriorities objects
   * @param aResourceManager
   *                ResourceManager instance to use to resolve imports
   * 
   * @return a merged TypePriorities object
   * @throws ResourceInitializationException
   *                 if an import could not be resolved
   */
  public static TypePriorities mergeTypePriorities(List aTypePriorities,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    TypePriorities aggTypePriorities = UIMAFramework.getResourceSpecifierFactory()
        .createTypePriorities();
    Iterator it = aTypePriorities.iterator();
    while (it.hasNext()) {
      TypePriorities tp = (TypePriorities) it.next();
      try {
        tp.resolveImports(aResourceManager);
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }
      TypePriorityList[] pls = tp.getPriorityLists();
      if (pls != null) {
        for (int i = 0; i < pls.length; i++) {
          aggTypePriorities.addPriorityList(pls[i]);
        }
      }
    }
    return aggTypePriorities;
  }

  /**
   * Merges the Type Priorities of each component within an aggregate Analysis Engine, producing a
   * single combined TypePriorities object.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * 
   * @return a new TypePriorities object that is the result of merging all of the delegate AE
   *         TypePriorities together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibility exists
   */
  public static TypePriorities mergeDelegateAnalysisEngineTypePriorities(
      AnalysisEngineDescription aAggregateDescription) throws ResourceInitializationException {
    return mergeDelegateAnalysisEngineTypePriorities(aAggregateDescription, UIMAFramework
        .newDefaultResourceManager());
  }

  /**
   * Merges the Type Priorities of each component within an aggregate Analysis Engine, producing a
   * single combined TypePriorities object.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * @param aResourceManager
   *                ResourceManager instance used to resolve imports
   * 
   * @return a new TypePriorities object that is the result of merging all of the delegate AE
   *         TypePriorities together
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibility exists
   */
  public static TypePriorities mergeDelegateAnalysisEngineTypePriorities(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    ArrayList l = new ArrayList();
    l.add(aAggregateDescription);
    List mdList = getMetaDataList(l, aResourceManager);

    // extract TypePriorities and merge
    List typePriorities = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
    }
    return mergeTypePriorities(typePriorities, aResourceManager);
  }

  /**
   * Merges the Type Systems, Type Priorities, and FS Indexes of each component within an aggregate
   * Analysis Engine.
   * <p>
   * This version of this method takes an argument <code>aOutputMergedTypes</code>, which this
   * method will populate with the names and descriptor locations of any types whose definitions
   * have been merged from multiple non-identical sources. That is, types that are declared more
   * than once, with different (but compatible) sets of features in each declaration, or with
   * different (but compatible) supertypes.
   * 
   * @param aAggregateDescription
   *                an aggregate Analysis Engine description
   * @param aResourceManager
   *                ResourceManager instance used to resolve imports
   * @param aOutputMergedTypes
   *                A Map that this method will populate with information about the set of types
   *                whose definitions were merged from multiple non-identical sources. That is,
   *                types that are declared more than once, with different (but compatible) sets of
   *                features in each declaration, or with different (but compatible) supertypes. The
   *                keys in the Map will be the type names (Strings) and the values will be {link
   *                Set}s containing Descriptor URLs (Strings) where those types are declared. You
   *                may pass null if you are not interested in this information.
   * @param aOutputFailedRemotes
   *                If this paramter is non-null, and if a remote AE could not be contacted, then an
   *                entry will be added to this map. The key will be the context name (e.g.,
   *                /myDelegate1/nestedRemoteDelegate) of the failed remote, and the value will be
   *                the Exception that occurred. If this parameter is null, an exception will be
   *                thrown if a remote AE could not be contacted.
   * 
   * @return an object containing the merged TypeSystem, TypePriorities, and FS Index definitions.
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibiliy exists or if an import could not be resolved
   */
  public static ProcessingResourceMetaData mergeDelegateAnalysisEngineMetaData(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager,
      Map aOutputMergedTypes, Map aOutputFailedRemotes) throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    ArrayList l = new ArrayList();
    l.add(aAggregateDescription);
    List mdList = getMetaDataList(l, aResourceManager, aOutputFailedRemotes);

    ProcessingResourceMetaData result = UIMAFramework.getResourceSpecifierFactory()
        .createProcessingResourceMetaData();

    // extract type systems and merge
    List typeSystems = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
    }
    result.setTypeSystem(mergeTypeSystems(typeSystems, aResourceManager, aOutputMergedTypes));

    // extract TypePriorities and merge
    List typePriorities = new ArrayList();
    it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
    }
    result.setTypePriorities(mergeTypePriorities(typePriorities, aResourceManager));

    // extract FsIndexCollections and merge
    List fsIndexes = new ArrayList();
    it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getFsIndexCollection() != null)
        fsIndexes.add(md.getFsIndexCollection());
    }
    result.setFsIndexCollection(mergeFsIndexes(fsIndexes, aResourceManager));

    return result;
  }

  /**
   * Determines whether one type subsumes another.
   * 
   * @param aType1Name
   *                name of first type
   * @param aType2Name
   *                name of second type
   * @param aNameMap
   *                Map from type names to TypeDescriptions
   * 
   * @return true if and only if the type named <code>aType1Name</code> subsumes the type named
   *         <code>aType2Name</code> according to the information given in the
   *         <code>aNameMap</code>.
   */
  protected static boolean subsumes(String aType1Name, String aType2Name, Map aNameMap) {
    // Top type subsumes everything
    if (CAS.TYPE_NAME_TOP.equals(aType1Name)) {
      return true;
    }

    // "walk up the tree" from aType2Name until we reach aType1Name or null
    String current = aType2Name;
    while (current != null && !current.equals(aType1Name)) {
      TypeDescription curType = (TypeDescription) aNameMap.get(current);
      if (curType == null)
        current = null;
      else
        current = curType.getSupertypeName();
    }

    return (current != null);
  }

  /**
   * Merges features into a TypeDescription.
   * 
   * @param aType
   *                TypeDescription into which to merge the features
   * @param aFeatures
   *                array of features to merge
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibility exists
   */
  protected static void mergeFeatures(TypeDescription aType, FeatureDescription[] aFeatures)
      throws ResourceInitializationException {
    FeatureDescription[] existingFeatures = aType.getFeatures();
    if (existingFeatures == null) {
      existingFeatures = new FeatureDescription[0];
    }

    for (int i = 0; i < aFeatures.length; i++) {
      String featName = aFeatures[i].getName();
      String rangeTypeName = aFeatures[i].getRangeTypeName();
      String elementTypeName = aFeatures[i].getElementType();
      Boolean multiRefsAllowed = aFeatures[i].getMultipleReferencesAllowed();

      // see if a feature already exists with this name
      FeatureDescription feat = null;
      for (int j = 0; j < existingFeatures.length; j++) {
        if (existingFeatures[j].getName().equals(featName)) {
          feat = existingFeatures[j];
          break;
        }
      }

      if (feat == null) {
        // doesn't exist; add it
        FeatureDescription featDesc = aType.addFeature(featName, aFeatures[i].getDescription(),
            rangeTypeName, elementTypeName, multiRefsAllowed);
        featDesc.setSourceUrl(aFeatures[i].getSourceUrl());
      } else {// feature does exist
        // check that the range types match
        if (!feat.getRangeTypeName().equals(rangeTypeName)) {
          throw new ResourceInitializationException(
              ResourceInitializationException.INCOMPATIBLE_RANGE_TYPES, new Object[] {
                  aType.getName() + ":" + feat.getName(), rangeTypeName, feat.getRangeTypeName(),
                  aType.getSourceUrlString() });
        }
        Boolean mra1 = feat.getMultipleReferencesAllowed();
        Boolean mra2 = multiRefsAllowed;

        // the logic here:
        // OK if both null
        // OK if both not-null, and are equals()
        // OK if one is null, the other has boolean-value of false (false is the default)
        // not ok otherwise

        if (!(((mra1 == null) && (mra2 == null)) || ((mra1 != null) && mra1.equals(mra2))
            || ((mra1 == null) && (mra2.booleanValue() == false)) || ((mra2 == null) && (mra1
            .booleanValue() == false)))) {
          throw new ResourceInitializationException(
              ResourceInitializationException.INCOMPATIBLE_MULTI_REFS, new Object[] {
                  aType.getName() + ":" + feat.getName(), aType.getSourceUrlString() });
        }

        if (!elementTypesCompatible(feat.getElementType(), elementTypeName)) {
          throw new ResourceInitializationException(
              ResourceInitializationException.INCOMPATIBLE_ELEMENT_RANGE_TYPES, new Object[] {
                  aType.getName() + TypeSystem.FEATURE_SEPARATOR + feat.getName(), elementTypeName,
                  feat.getElementType(), aType.getSourceUrlString() });
        }
      }
    }
  }

  private static boolean elementTypesCompatible(Object o1, Object o2) {
    return ((null == o1) && (null == o2)) || ((null != o1) && o1.equals(o2)) ||
      // allow missing types to be equal to TOP
      (o1 != null && o1.equals(CAS.TYPE_NAME_TOP) && o2 == null) ||
      (o2 != null && o2.equals(CAS.TYPE_NAME_TOP) && o1 == null)
    ;
  }

  /**
   * Gets a list of ProcessingResourceMetadata objects from a list containing either
   * ResourceSpecifiers, ProcessingResourceMetadata objects, or subparts of
   * ProcessingResourceMetadata objects (type sypstems, indexes, or type priorities). Subparts will
   * be wrapped inside a ProcessingResourceMetadata object. All objects will be cloned, so that
   * further processing (such as import resolution) does not affect the caller.
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aResourceManager
   *                used to resolve delegate analysis engine imports
   * @param aOutputFailedRemotes
   *                If this paramter is non-null, and if a remote AE could not be contacted, then
   *                the context name (e.g. /myDelegate1/nestedRemoteDelegate) of the failed remote
   *                will be added to this collection. If this parameter is null, an exception will
   *                be thrown if a remote AE could not be contacted.
   * 
   * @return a List containing the ProcessingResourceMetaData objects containing all of the
   *         information in all of the objects in <code>aComponentDescriptionOrMetaData</code>
   *         (including all components of aggregate AnalysisEngines)
   * 
   * @throws ResourceInitialziationException
   *                 if a failure occurs because an import could not be resolved
   */
  public static List getMetaDataList(Collection aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager, Map aOutputFailedRemotes)
      throws ResourceInitializationException {
    return getMetaDataList(aComponentDescriptionOrMetaData, aResourceManager, aOutputFailedRemotes,
        "");
  }

  private static List getMetaDataList(Collection aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager, Map aOutputFailedRemotes, String aContextName)
      throws ResourceInitializationException {

    List mdList = new ArrayList();
    if (null == aComponentDescriptionOrMetaData) {
      return mdList;
    }
    Iterator iter = aComponentDescriptionOrMetaData.iterator();
    while (iter.hasNext()) {
      Object current = iter.next();
      if (current instanceof ProcessingResourceMetaData) {
        mdList.add(((ProcessingResourceMetaData) current).clone());
      } else if (current instanceof AnalysisEngineDescription) {
        AnalysisEngineDescription aeDesc = (AnalysisEngineDescription) current;
        mdList.add(aeDesc.getMetaData().clone());
        // expand aggregate
        if (!aeDesc.isPrimitive()) {
          Map delegateMap;
          try {
            delegateMap = aeDesc.getAllComponentSpecifiers(aResourceManager);
          } catch (InvalidXMLException e) {
            throw new ResourceInitializationException(e);
          }
          Iterator delIter = delegateMap.entrySet().iterator();
          while (delIter.hasNext()) {
            Map.Entry delEntry = (Map.Entry) delIter.next();
            List tempList = new ArrayList();
            tempList.add(delEntry.getValue());
            mdList.addAll(getMetaDataList(tempList, aResourceManager, aOutputFailedRemotes,
                aContextName + "/" + delEntry.getKey()));
          }
        }
      } else if (current instanceof CollectionReaderDescription) {
        mdList.add(((CollectionReaderDescription) current).getMetaData().clone());
      } else if (current instanceof CasInitializerDescription) {
        mdList.add(((CasInitializerDescription) current).getMetaData().clone());
      } else if (current instanceof CasConsumerDescription) {
        mdList.add(((CasConsumerDescription) current).getMetaData().clone());
      } else if (current instanceof FlowControllerDescription) {
        mdList.add(((FlowControllerDescription) current).getMetaData().clone());
      } else if (current instanceof TypeSystemDescription) {
        ProcessingResourceMetaData md = new ProcessingResourceMetaData_impl();
        md.setTypeSystem((TypeSystemDescription) current);
        mdList.add(md);
      } else if (current instanceof FsIndexCollection) {
        ProcessingResourceMetaData md = new ProcessingResourceMetaData_impl();
        md.setFsIndexCollection((FsIndexCollection) current);
        mdList.add(md);
      } else if (current instanceof TypePriorities) {
        ProcessingResourceMetaData md = new ProcessingResourceMetaData_impl();
        md.setTypePriorities((TypePriorities) current);
        mdList.add(md);
      } else if (current instanceof ResourceSpecifier) {
        // try to instantiate the resource
        Resource resource = null;
        Map resourceMgrInMap = new HashMap();
        resourceMgrInMap.put(Resource.PARAM_RESOURCE_MANAGER, aResourceManager);
        try {
          resource = UIMAFramework.produceResource((ResourceSpecifier) current,
              (null == aResourceManager) ? Collections.EMPTY_MAP : resourceMgrInMap);
        } catch (Exception e) {
          // failed. If aOutputFailedRemotes is non-null, add an entry to it to it, else throw the
          // exception.
          if (aOutputFailedRemotes != null) {
            aOutputFailedRemotes.put(aContextName, e);
          } else {
            if (e instanceof ResourceInitializationException)
              throw (ResourceInitializationException) e;
            else if (e instanceof RuntimeException)
              throw (RuntimeException) e;
            else
              throw new RuntimeException(e);
          }
        }
        if (resource != null) {
          ResourceMetaData metadata = resource.getMetaData();
          if (metadata instanceof ProcessingResourceMetaData) {
            mdList.add(metadata);
          }
          resource.destroy();
        }
      } else {
        throw new ResourceInitializationException(
            ResourceInitializationException.UNSUPPORTED_OBJECT_TYPE_IN_CREATE_CAS,
            new Object[] { current.getClass().getName() });
      }
    }

    return mdList;
  }

  /**
   * Gets a list of ProcessingResourceMetadata objects from a list containing either
   * ResourceSpecifiers, ProcessingResourceMetadata objects, or subparts of
   * ProcessingResourceMetadata objects (type sypstems, indexes, or type priorities). Subparts will
   * be wrapped inside a ProcessingResourceMetadata object. All objects will be cloned, so that
   * further processing (such as import resolution) does not affect the caller.
   * <p>
   * If you pass this method objects of type {@link AnalysisEngineDescription},
   * {@link CollectionReaderDescription}, {@link CasInitializerDescription}, or
   * {@link CasConsumerDescription}, it will not instantiate the components. It will just extract
   * the type system information from the descriptor. For any other kind of
   * {@link ResourceSpecifier}, it will call
   * {@link UIMAFramework#produceResource(org.apache.uima.resource.ResourceSpecifier, Map)}. For
   * example, if a {@link URISpecifier} is passed, a remote connection will be established and the
   * service will be queries for its metadata. An exception will be thrown if the connection can not
   * be opened.
   * 
   * @param aComponentDescriptionsOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aResourceManager
   *                used to resolve delegate analysis engine imports
   * 
   * @return a List containing the ProcessingResourceMetaData objects containing all of the
   *         information in all of the objects in <code>aComponentDescriptionOrMetaData</code>
   *         (including all components of aggregate AnalysisEngines)
   * 
   * @throws ResourceInitialziationException
   *                 if a failure occurs because an import could not be resolved
   */
  public static List getMetaDataList(Collection aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    return getMetaDataList(aComponentDescriptionOrMetaData, aResourceManager, null);
  }

}
