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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
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
import org.apache.uima.resource.metadata.MetaDataObject;
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
    List<AnalysisEngineMetaData> list = new ArrayList<AnalysisEngineMetaData>();
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
    List<ProcessingResourceMetaData> list = new ArrayList<ProcessingResourceMetaData>();
    list.add(aMetaData);
    return createCas(list);
  }

  /**
   * Creates a new CAS instance for an Analysis Engine. This works for both primitive and aggregate
   * analysis engines.
   * 
   * @param aDescription
   *                description of the analysis engine that will process this CAS. This is used to
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
   *                description of the analysis engine that will process this CAS. This is used to
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
    List<AnalysisEngineDescription> list = new ArrayList<AnalysisEngineDescription>();
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
  public static CAS createCas(Collection<? extends MetaDataObject> aComponentDescriptionsOrMetaData)
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
  public static CAS createCas(Collection<? extends MetaDataObject> aComponentDescriptionsOrMetaData,
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
  public static CAS createCas(Collection<? extends MetaDataObject> aComponentDescriptionsOrMetaData,
      Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // build a list of metadata objects
    List<ProcessingResourceMetaData> mdList = getMetaDataList(aComponentDescriptionsOrMetaData, aResourceManager);

    // extract TypeSystems, TypePriorities, and FsIndexes from metadata
    List<TypeSystemDescription> typeSystems = new ArrayList<TypeSystemDescription>();
    List<TypePriorities> typePriorities = new ArrayList<TypePriorities>();
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
   * @param aResourceManager 
   *                the resource manager
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
  public static CAS createCas(Collection<? extends MetaDataObject> aComponentDescriptionsOrMetaData, TypeSystem aTypeSystem,
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
  public static CAS createCas(Collection<? extends MetaDataObject> aComponentDescriptionsOrMetaData, TypeSystem aTypeSystem,
      Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // build a list of metadata objects
    List<ProcessingResourceMetaData> mdList = getMetaDataList(aComponentDescriptionsOrMetaData, aResourceManager);

    // extract TypeSystems, TypePriorities, and FsIndexes from metadata
    List<TypeSystemDescription> typeSystems = new ArrayList<TypeSystemDescription>();
    List<TypePriorities> typePriorities = new ArrayList<TypePriorities>();
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
   * @param aPerformanceTuningSettings
   *                the settings for performance tuning
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
   * @param aPerformanceTuningSettings
   *                the settings for performance tuning
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
   *                an existing type system to reuse in this CAS, null if none.
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
        List<TypeSystemDescription> tsList = new ArrayList<TypeSystemDescription>();
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
    boolean useJcasCache = true;
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
        casMgr = CASFactory.createCAS(CASImpl.DEFAULT_INITIAL_HEAP_SIZE, useJcasCache);
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
   * @throws ResourceInitializationException
   *                 if CAS creation fails

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
   * @throws ResourceInitializationException
   *                 if CAS creation fails
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
   *                description of type system to install
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
        // that it isn't guaranteed that a supertype will occur in the Types list
        // before its subtype.

        // Build a linked list of type descriptions. We will make multiple passes
        // over this, adding types to the CAS and removing them from the linked
        // list. We continue until the list is empty or we cannot make any
        // progress.
        LinkedList<TypeDescription> typeList = new LinkedList<TypeDescription>();
        typeList.addAll(Arrays.asList(types));
        int numTypes = typeList.size();
        int lastNumTypes;
        List<TypeDescription> typesInOrderOfCreation = new LinkedList<TypeDescription>();
        do {
          lastNumTypes = numTypes;
          Iterator<TypeDescription> it = typeList.iterator();
          while (it.hasNext()) {
            TypeDescription curTypeDesc = it.next();
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
          TypeDescription td_i = typeList.get(i);
          boolean foundSuperType = false;
          for (int j = 0; j < typeList.size(); j++) {
            if (i == j) {
              continue;
            }
            TypeDescription td_j = typeList.get(j);
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
          TypeDescription firstFailed = typeList.getFirst();
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
        Iterator<TypeDescription> typeIter = typesInOrderOfCreation.iterator();
        while (typeIter.hasNext()) {
          TypeDescription typeDesc = typeIter.next();
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
   * @throws ResourceInitializationException
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
    Iterator<FeatureStructure> iter = aCasData.getFeatureStructures();
    List<TypeDescription> typesArr = new ArrayList<TypeDescription>();
    while (iter.hasNext()) {
      FeatureStructure casFS = iter.next();
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
      td[j] = typesArr.get(j);
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection<? extends TypeSystemDescription> aTypeSystems)
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection<? extends TypeSystemDescription> aTypeSystems,
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeTypeSystems(Collection<? extends TypeSystemDescription> aTypeSystems,
      ResourceManager aResourceManager, Map<String, Set<String>> aOutputMergedTypes)
      throws ResourceInitializationException {
    // also build a Map from Type names to Types.  Use a TreeMap so we get a consistent ordering of types.
    Map<String, TypeDescription> typeNameMap = new TreeMap<String,TypeDescription>();

    // Iterate through all type systems and add types to the merged TypeSystem.
    // If a type is defined more than once, we need to check if the superType
    // declarations are compatible (one inherits from another), and merge the
    // features.
    
    // In order to properly handle the supertype merging, we need to make sure
    // that we process the supertype definitions before the subtypes. To do this,
    // we build a linked list of type descriptions, and make multiple passes
    // over this, adding types to the merged type system when their supertypes
    // become defined.  We continue until the list is empty or we cannot make any
    // progress.
    LinkedList<TypeDescription> typeList = new LinkedList<TypeDescription>();
    Iterator<? extends TypeSystemDescription> it = aTypeSystems.iterator();
    while (it.hasNext()) {
      TypeSystemDescription ts = it.next();
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
      Iterator<TypeDescription> typeIter = typeList.iterator();
      while (typeIter.hasNext()) {
        TypeDescription type = typeIter.next();
        String supertypeName = type.getSupertypeName();
        if (supertypeName.startsWith("uima.cas") || supertypeName.startsWith("uima.tcas") || typeNameMap.containsKey(supertypeName)) {
          //supertype is defined, ok to proceed
          //check if type is already defined 
          addTypeToMergedTypeSystem(aOutputMergedTypes, typeNameMap, type);
          typeIter.remove();
        }
      }
    } while (typeList.size() > 0 && typeList.size() != lastNumTypes);
      
    //At this point, if the typeList is not empty, then we either have a type with an undefined supertype, or a cycle.
    //We go ahead and merge the type definitions anyway - these problems will be caught at CAS creation time. Undefined supertypes 
    //may be OK at this stage - this type system will have to be further merged before it can be used.
    Iterator<TypeDescription> typeIter = typeList.iterator();
    while (typeIter.hasNext()) {
      TypeDescription type = typeIter.next();
      addTypeToMergedTypeSystem(aOutputMergedTypes, typeNameMap, type);
    }    

    // create the type system and populate from the typeNamesMap
    TypeSystemDescription result = UIMAFramework.getResourceSpecifierFactory()
        .createTypeSystemDescription();
    TypeDescription[] types = new TypeDescription[typeNameMap.values().size()];
    typeNameMap.values().toArray(types);
    result.setTypes(types);
    return result;
  }

  private static void addTypeToMergedTypeSystem(Map<String, Set<String>> aOutputMergedTypes, Map<String,TypeDescription> typeNameMap, TypeDescription type) throws ResourceInitializationException {
    String typeName = type.getName();
    String supertypeName = type.getSupertypeName();
    TypeDescription existingType = typeNameMap.get(typeName);
    if (existingType == null) {
      // create new type
      existingType = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
      existingType.setName(typeName);
      existingType.setDescription(type.getDescription());
      existingType.setSupertypeName(supertypeName);
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
      // merge features or check string allowed values are the same
      if (supertypeName.equals("uima.cas.String")) {
        AllowedValue[] av1 = getAllowedValues(type);
        AllowedValue[] av2 = getAllowedValues(existingType);
        if (!isAllowedValuesMatch(av1, av2)) {
          throw new ResourceInitializationException(
              ResourceInitializationException.ALLOWED_VALUES_NOT_IDENTICAL, new Object[] {
                  typeName, avAsString(av1), avAsString(av2), 
                  type.getSourceUrlString() });
        }
      } else {
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
  }

  private static boolean isAllowedValuesMatch(AllowedValue[] av1, AllowedValue[] av2) {
    if (av1.length != av2.length) {
      return false;
    }
    
    Set<String> s1 = new HashSet<String>(av1.length);
    Set<String> s2 = new HashSet<String>(av1.length);
    
    for (AllowedValue av : av1) {
      s1.add(av.getString());
    }
    
    for (AllowedValue av : av2) {
      s2.add(av.getString());
    }
    
    return s1.equals(s2);
  }
  
  
  private static String avAsString(AllowedValue[] av) {
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < av.length; i++) {
      sb.append(av[i].getString());
      if (i < av.length - 1) {
        sb.append(", ");
      }
    }
    sb.append('}');
    return sb.toString();
  }


  private static AllowedValue[] getAllowedValues(TypeDescription type) {
    AllowedValue[] r = type.getAllowedValues();
    if (r == null) {
      return new AllowedValue[0];
    }
    return r;
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
  private static void reportMerge(Map<String, Set<String>> aOutputMergedTypes, TypeDescription currentType,
      TypeDescription existingType) {
    if (aOutputMergedTypes != null) {
      String typeName = currentType.getName();
      Set<String> descriptorUrls = aOutputMergedTypes.get(typeName);
      if (descriptorUrls == null) {
        descriptorUrls = new TreeSet<String>();
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
   *                 if an incompatibility exists or if an import could not be resolved
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
   *                 if an incompatibility exists or if an import could not be resolved
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static TypeSystemDescription mergeDelegateAnalysisEngineTypeSystems(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager,
      Map<String, Set<String>> aOutputMergedTypes) throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    List<AnalysisEngineDescription> l = new ArrayList<AnalysisEngineDescription>();
    l.add(aAggregateDescription);
    List<ProcessingResourceMetaData> mdList = getMetaDataList(l, aResourceManager);

    // extract type systems and merge
    List<TypeSystemDescription> typeSystems = new ArrayList<TypeSystemDescription>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static FsIndexCollection mergeFsIndexes(List<? extends FsIndexCollection> aFsIndexCollections,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    Map<String, FsIndexDescription> aggIndexes = new HashMap<String, FsIndexDescription>();
    Iterator<? extends FsIndexCollection> it = aFsIndexCollections.iterator();
    while (it.hasNext()) {
      FsIndexCollection indexColl = it.next();

      if (indexColl != null) {
        try {
          indexColl.resolveImports(aResourceManager);
        } catch (InvalidXMLException e) {
          throw new ResourceInitializationException(e);
        }
        FsIndexDescription[] indexes = indexColl.getFsIndexes();
        for (int i = 0; i < indexes.length; i++) {
          // does an index with this label already exist?
          FsIndexDescription duplicateIndex = aggIndexes.get(indexes[i]
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
    Collection<FsIndexDescription> indexes = aggIndexes.values();
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
   *                 if an incompatibility exists or if an import could not be resolved
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
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static FsIndexCollection mergeDelegateAnalysisEngineFsIndexCollections(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager)
      throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    List<AnalysisEngineDescription> l = new ArrayList<AnalysisEngineDescription>();
    l.add(aAggregateDescription);
    List<ProcessingResourceMetaData> mdList = getMetaDataList(l, aResourceManager);

    // extract FsIndexCollections and merge
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
  public static TypePriorities mergeTypePriorities(List<? extends TypePriorities> aTypePriorities,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    TypePriorities aggTypePriorities = UIMAFramework.getResourceSpecifierFactory()
        .createTypePriorities();
    Iterator<? extends TypePriorities> it = aTypePriorities.iterator();
    while (it.hasNext()) {
      TypePriorities tp = it.next();
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
    ArrayList<AnalysisEngineDescription> l = new ArrayList<AnalysisEngineDescription>();
    l.add(aAggregateDescription);
    List<ProcessingResourceMetaData> mdList = getMetaDataList(l, aResourceManager);

    // extract TypePriorities and merge
    List<TypePriorities> typePriorities = new ArrayList<TypePriorities>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
   *                If this parameter is non-null, and if a remote AE could not be contacted, then an
   *                entry will be added to this map. The key will be the context name (e.g.,
   *                /myDelegate1/nestedRemoteDelegate) of the failed remote, and the value will be
   *                the Exception that occurred. If this parameter is null, an exception will be
   *                thrown if a remote AE could not be contacted.
   * 
   * @return an object containing the merged TypeSystem, TypePriorities, and FS Index definitions.
   * 
   * @throws ResourceInitializationException
   *                 if an incompatibility exists or if an import could not be resolved
   */
  public static ProcessingResourceMetaData mergeDelegateAnalysisEngineMetaData(
      AnalysisEngineDescription aAggregateDescription, ResourceManager aResourceManager,
      Map<String, Set<String>> aOutputMergedTypes, Map<String, ? super Exception> aOutputFailedRemotes) throws ResourceInitializationException {
    // expand the aggregate AE description into the individual delegates
    ArrayList<AnalysisEngineDescription> l = new ArrayList<AnalysisEngineDescription>();
    l.add(aAggregateDescription);
    List<ProcessingResourceMetaData> mdList = getMetaDataList(l, aResourceManager, aOutputFailedRemotes);

    ProcessingResourceMetaData result = UIMAFramework.getResourceSpecifierFactory()
        .createProcessingResourceMetaData();

    // extract type systems and merge
    List<TypeSystemDescription> typeSystems = new ArrayList<TypeSystemDescription>();
    Iterator<ProcessingResourceMetaData> it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
    }
    result.setTypeSystem(mergeTypeSystems(typeSystems, aResourceManager, aOutputMergedTypes));

    // extract TypePriorities and merge
    List<TypePriorities> typePriorities = new ArrayList<TypePriorities>();
    it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
      if (md.getTypePriorities() != null)
        typePriorities.add(md.getTypePriorities());
    }
    result.setTypePriorities(mergeTypePriorities(typePriorities, aResourceManager));

    // extract FsIndexCollections and merge
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
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
  protected static boolean subsumes(String aType1Name, String aType2Name, Map<String, ? extends TypeDescription> aNameMap) {
    // Top type subsumes everything
    if (CAS.TYPE_NAME_TOP.equals(aType1Name)) {
      return true;
    }

    // "walk up the tree" from aType2Name until we reach aType1Name or null
    String current = aType2Name;
    while (current != null && !current.equals(aType1Name)) {
      TypeDescription curType = aNameMap.get(current);
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
            || ((mra1 == null) && !mra2.booleanValue()) || ((mra2 == null) && !mra1.booleanValue()))) {
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

  /**
   * Compare element type names for array-like features
   * @param o1 name of first element type
   * @param o2 name of second element type
   * @return true if elements are compatible for merging features
   */
  private static boolean elementTypesCompatible(String o1, String o2) {
    return ((null == o1) && (null == o2)) || ((null != o1) && o1.equals(o2)) ||
      // allow missing types to be equal to TOP
      (o1 != null && o1.equals(CAS.TYPE_NAME_TOP) && o2 == null) ||
      (o2 != null && o2.equals(CAS.TYPE_NAME_TOP) && o1 == null)
    ;
  }

  /*************************************************************************************************
   * Caching of getMeta info that requires producing the resource                                  *
   *   - done because producing the resource can be very expensive                                 *                        
   *     including accessing remote things on the network                                          *
   * Cache is cleared approximately every 30 seconds because remote resource's statuses may change *
   *                                                                                               *
   * Cache key is the ResourceSpecifier's class loaders and the ResourceManager                    *
   *   Both the DataPath and the uima extension class loader are used as part of the key           *
   *   because differences in these could cause different metadata to be loaded                    *
   *************************************************************************************************/
  
  private static class MetaDataCacheKey {
    final ResourceSpecifier resourceSpecifier;
    final ClassLoader rmClassLoader;
    final String rmDataPath;
    
    MetaDataCacheKey(ResourceSpecifier resourceSpecifier, ResourceManager resourceManager) {
      this.resourceSpecifier = resourceSpecifier;
      this.rmClassLoader = (null == resourceManager) ? null : resourceManager.getExtensionClassLoader(); // can be null
      this.rmDataPath = (null == resourceManager) ? null : resourceManager.getDataPath();
    }

    @Override
    public int hashCode() {
      return ((rmClassLoader == null) ? 0 : rmClassLoader.hashCode()) 
             + ((rmDataPath == null)  ? 0 : rmDataPath.hashCode()) 
             + resourceSpecifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (null == obj) {
        return false;
      }
      MetaDataCacheKey k = (MetaDataCacheKey) obj;
      if (rmDataPath == null) {
        if (k.rmDataPath != null) {
          return false;
        }
        return resourceSpecifier.equals(k.resourceSpecifier) && 
               rmClassLoader == k.rmClassLoader;
      }
      return resourceSpecifier.equals(k.resourceSpecifier) && 
             rmClassLoader == k.rmClassLoader &&
             rmDataPath.equals(k.rmDataPath);
    }

    @Override
    public String toString() {
      return "MetaDataCacheKey [resourceSpecifier=" + resourceSpecifier + ", rmClassLoader="
          + rmClassLoader + ", rmDataPath=" + rmDataPath + "]";
    }
  }
  
  private static final boolean cacheDebug = false; // set true for debugging info
  private static final int HOLD_TIME = 30000;  // keep cache for 30 seconds, approx., in case a remote resource changes state
  
  /**
   * This is the cache.
   * All references to it are synchronized, using it as the object.
   */
  private static final transient Map<MetaDataCacheKey, MetaDataCacheEntry> metaDataCache = new HashMap<MetaDataCacheKey, MetaDataCacheEntry>();

  /** This holds an instance of a Timer object
   * This object is nulled out and gets gc'd when it's timertask finishes, when the
   * cache is empty.  
   * 
   * All references to it are synchronized under the metaDataCache.
   */
  private static Timer cleanupTimer = null;

  /**
   * This class holds the processing Resource Metadata, or null if there is none, and
   * a timestamp when the metadata was obtained.
   */
  private static class MetaDataCacheEntry {
    ProcessingResourceMetaData processingResourceMetaData;
    long creationTime;
    
    MetaDataCacheEntry(ResourceMetaData resourceMetaData) {
      processingResourceMetaData = (resourceMetaData instanceof ProcessingResourceMetaData) ? (ProcessingResourceMetaData) resourceMetaData : null;
      creationTime = System.currentTimeMillis(); 
      if (null == cleanupTimer) {
        if (cacheDebug) {
          System.err.format("GetMetaDataCache: Scheduling new cleanup task%n");
        }

        cleanupTimer = new Timer("metaDataCacheCleanup", true);  // run as daemon
        // create a new instance of the timer task, because a previous one may 
        // still be running
        TimerTask metaDataCacheCleanupTask = new TimerTask() {   
          @Override
          public void run() {
            synchronized (metaDataCache) {
              long now = System.currentTimeMillis();
              if (cacheDebug) {
                System.err.format("GetMetaDataCache: cleanup task running%n");
              }
              for (Iterator<Entry<MetaDataCacheKey, MetaDataCacheEntry>> it = metaDataCache.entrySet().iterator(); it.hasNext();) {
                Entry<MetaDataCacheKey, MetaDataCacheEntry> e = it.next();
                if (e.getValue().creationTime + HOLD_TIME < now) {
                  if (cacheDebug) {
                    System.err.format("GetMetaDataCache: cleanup task removing entry %s%n", e.getKey().toString() );
                  }
                  it.remove();
                }
              }
              if (metaDataCache.size() == 0) {
                if (cacheDebug) {
                  System.err.format("GetMetaDataCache: cleanup task terminating, cache empty%n");
                }
                cancel();
                cleanupTimer.cancel();  // probably not needed, but for safety ...
                cleanupTimer = null;
              }
              if (cacheDebug) {
                System.err.format("GetMetaDataCache: cleanup task finished a cycle%n");
              }
            }
          }
        };
        cleanupTimer.schedule(metaDataCacheCleanupTask, HOLD_TIME, HOLD_TIME);
      }
    }
  }
   
  
  /**
   * Gets a list of ProcessingResourceMetadata objects from a list containing either
   * ResourceSpecifiers, ProcessingResourceMetadata objects, or subparts of
   * ProcessingResourceMetadata objects (type systems, indexes, or type priorities). Subparts will
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
   * Note that this last kind of lookup may be expensive (calling produceResource, which in turn may
   * query remote connections etc.).  Because of this, a cache is maintained for these, 
   * (because some scenarios end up requesting the same metadata multiple times, in rapid succession).
   * 
   * Because remote resource may become available, the cache entries are removed 30 seconds
   * after they are created.  This also reclaims space from the cache.
   *  
   * @param aComponentDescriptionOrMetaData
   *                a collection of {@link ResourceSpecifier}, {@link ProcessingResourceMetaData},
   *                {@link TypeSystemDescription}, {@link FsIndexCollection}, or
   *                {@link TypePriorities} objects.
   * @param aResourceManager
   *                used to resolve delegate analysis engine imports
   * @param aOutputFailedRemotes
   *                If this parameter is non-null, and if a remote AE could not be contacted, then
   *                the context name (e.g. /myDelegate1/nestedRemoteDelegate) of the failed remote
   *                will be added to this collection. If this parameter is null, an exception will
   *                be thrown if a remote AE could not be contacted.
   * 
   * @return a List containing the ProcessingResourceMetaData objects containing all of the
   *         information in all of the objects in <code>aComponentDescriptionOrMetaData</code>
   *         (including all components of aggregate AnalysisEngines)
   * 
   * @throws ResourceInitializationException
   *                 if a failure occurs because an import could not be resolved
   */
  public static List<ProcessingResourceMetaData> getMetaDataList(Collection<? extends MetaDataObject> aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager, Map<String, ? super Exception> aOutputFailedRemotes)
      throws ResourceInitializationException {
    return getMetaDataList(aComponentDescriptionOrMetaData, aResourceManager, aOutputFailedRemotes,
        "");
  }

  private static List<ProcessingResourceMetaData> getMetaDataList(Collection<? extends MetaDataObject> aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager, Map<String, ? super Exception> aOutputFailedRemotes, String aContextName)
      throws ResourceInitializationException {

    List<ProcessingResourceMetaData> mdList = new ArrayList<ProcessingResourceMetaData>();
    if (null == aComponentDescriptionOrMetaData) {
      return mdList;
    }
    Iterator<? extends MetaDataObject> iter = aComponentDescriptionOrMetaData.iterator();
    while (iter.hasNext()) {
      Object current = iter.next();
      if (current instanceof ProcessingResourceMetaData) {
        mdList.add((ProcessingResourceMetaData) ((ProcessingResourceMetaData) current).clone());
      } else if (current instanceof AnalysisEngineDescription) {
        AnalysisEngineDescription aeDesc = (AnalysisEngineDescription) current;
        mdList.add((ProcessingResourceMetaData) aeDesc.getAnalysisEngineMetaData().clone());
        // expand aggregate
        if (!aeDesc.isPrimitive()) {
          Map<String, ResourceSpecifier> delegateMap;
          try {
            delegateMap = aeDesc.getAllComponentSpecifiers(aResourceManager);
          } catch (InvalidXMLException e) {
            throw new ResourceInitializationException(e);
          }
          Iterator<Map.Entry<String, ResourceSpecifier>> delIter = delegateMap.entrySet().iterator();
          while (delIter.hasNext()) {
            Map.Entry<String, ResourceSpecifier> delEntry = delIter.next();
            List<ResourceSpecifier> tempList = new ArrayList<ResourceSpecifier>();
            tempList.add(delEntry.getValue());
            mdList.addAll(getMetaDataList(tempList, aResourceManager, aOutputFailedRemotes,
                aContextName + "/" + delEntry.getKey()));
          }
        }
      } else if (current instanceof CollectionReaderDescription) {
        mdList.add((ProcessingResourceMetaData) ((CollectionReaderDescription) current).getCollectionReaderMetaData().clone());
      } else if (current instanceof CasInitializerDescription) {
        mdList.add((ProcessingResourceMetaData) ((CasInitializerDescription) current).getCasInitializerMetaData().clone());
      } else if (current instanceof CasConsumerDescription) {
        mdList.add((ProcessingResourceMetaData) ((CasConsumerDescription) current).getCasConsumerMetaData().clone());
      } else if (current instanceof FlowControllerDescription) {
        mdList.add((ProcessingResourceMetaData) ((FlowControllerDescription) current).getFlowControllerMetaData().clone());
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
                
        // first try the cache
        MetaDataCacheKey metaDataCacheKey = new MetaDataCacheKey((ResourceSpecifier)current, aResourceManager);
        synchronized(metaDataCache) {
          MetaDataCacheEntry metaData = metaDataCache.get(metaDataCacheKey);
          if (null != metaData) {
            if (cacheDebug) {
              System.err.format("GetMetaDataCache: using cached entry%n");
            }
            if (null != metaData.processingResourceMetaData) {
              mdList.add(metaData.processingResourceMetaData);
            }
            continue;
          } 
        }
        
        // try to instantiate the resource
        
        Resource resource = null;
        Map<String, Object> prParams = new HashMap<String, Object>();
        if (aResourceManager != null) {
          prParams.put(Resource.PARAM_RESOURCE_MANAGER, aResourceManager);
        }
        prParams.put(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE, Boolean.TRUE);
        try {
          resource = UIMAFramework.produceResource((ResourceSpecifier) current, prParams);
//              (null == aResourceManager) ? Collections.<String, Object>emptyMap() : resourceMgrInMap);
        } catch (Exception e) {
          // record failure, so we don't ask for this again, for a while
          synchronized (metaDataCache) {
            if (cacheDebug) {
              System.err.format("GetMetaDataCache: saving entry in cache%n");
            }
            metaDataCache.put(metaDataCacheKey, new MetaDataCacheEntry(null));
          }
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
        ResourceMetaData metadata = (resource == null) ? null : resource.getMetaData();

        synchronized (metaDataCache) {
          if (cacheDebug) {
            System.err.format("GetMetaDataCache: saving entry in cache%n");
          }
          metaDataCache.put(metaDataCacheKey, new MetaDataCacheEntry(metadata));
        }

        if (resource != null) {
          if (metadata instanceof ProcessingResourceMetaData) {
            mdList.add((ProcessingResourceMetaData) metadata);
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
   * ProcessingResourceMetadata objects (type systems, indexes, or type priorities). Subparts will
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
   * @param aComponentDescriptionOrMetaData
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
   * @throws ResourceInitializationException
   *                 if a failure occurs because an import could not be resolved
   */
  public static List<ProcessingResourceMetaData> getMetaDataList(Collection<? extends MetaDataObject> aComponentDescriptionOrMetaData,
      ResourceManager aResourceManager) throws ResourceInitializationException {
    return getMetaDataList(aComponentDescriptionOrMetaData, aResourceManager, null);
  }

}
