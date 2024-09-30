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
package org.apache.uima.fit.factory;

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.fit.internal.MetaDataUtil.scanDescriptors;
import static org.apache.uima.fit.internal.ReflectionUtil.getInheritableAnnotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.fit.internal.ClassLoaderUtils;
import org.apache.uima.fit.internal.MetaDataType;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.impl.FsIndexCollection_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.spi.FsIndexCollectionProvider;
import org.apache.uima.spi.TypeSystemProvider;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FsIndexFactory {
  private static Logger LOG = LoggerFactory.getLogger(FsIndexFactory.class);

  /**
   * Comparator that orders FeatureStructures according to the standard order of their key features.
   * For integer and float values, this is the standard linear order, and for strings it is
   * lexicographic order.
   */
  public static final int STANDARD_COMPARE = FsIndexKeyDescription.STANDARD_COMPARE;

  /**
   * Comparator that orders FeatureStructures according to the reverse order of their key features
   * (the opposite order as that defined by STANDARD_COMPARE).
   */
  public static final int REVERSE_STANDARD_COMPARE = FsIndexKeyDescription.REVERSE_STANDARD_COMPARE;

  private static final Object SCAN_LOCK = new Object();

  private static final Object CREATE_LOCK = new Object();

  private static final FsIndexCollection PLACEHOLDER = new FsIndexCollection_impl();

  private static WeakHashMap<String, FsIndexCollection> fsIndexCollections;

  private static WeakHashMap<ClassLoader, String[]> fsIndexLocationsByClassloader;

  private static WeakHashMap<ClassLoader, FsIndexCollection> fsIndexCollectionsByClassloader;

  static {
    fsIndexCollections = new WeakHashMap<>();
    fsIndexLocationsByClassloader = new WeakHashMap<>();
    fsIndexCollectionsByClassloader = new WeakHashMap<>();
  }

  private FsIndexFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Create index configuration data for a given class definition using reflection and the
   * configuration parameter annotation.
   *
   * @param componentClass
   *          the class to analyze
   * @return the index collection
   */
  public static FsIndexCollection createFsIndexCollection(Class<?> componentClass) {
    List<FsIndex> anFsIndexList = new ArrayList<>();

    // Check FsIndexCollection annotation
    org.apache.uima.fit.descriptor.FsIndexCollection anIndexCollection = getInheritableAnnotation(
            org.apache.uima.fit.descriptor.FsIndexCollection.class, componentClass);
    if (anIndexCollection != null) {
      anFsIndexList.addAll(asList(anIndexCollection.fsIndexes()));
    }

    // Check FsIndex annotation
    org.apache.uima.fit.descriptor.FsIndex anFsIndex = getInheritableAnnotation(FsIndex.class,
            componentClass);
    if (anFsIndex != null) {
      if (anIndexCollection != null) {
        throw new IllegalStateException(
                "Class [" + componentClass.getName() + "] must not " + "declare "
                        + org.apache.uima.fit.descriptor.FsIndexCollection.class.getSimpleName()
                        + " and " + FsIndex.class.getSimpleName() + " at the same time.");
      }

      anFsIndexList.add(anFsIndex);
    }

    FsIndexCollection_impl fsIndexCollection = new FsIndexCollection_impl();

    // Process collected FsIndex annotations
    for (FsIndex anIdx : anFsIndexList) {
      // Collect index keys
      List<FsIndexKeyDescription> keys = new ArrayList<>();
      for (FsIndexKey anIndexKey : anIdx.keys()) {
        keys.add(createFsIndexKeyDescription(anIndexKey.featureName(), anIndexKey.comparator()));
      }

      // type and typeName must not be set at the same time
      if (!anIdx.typeName().equals(FsIndex.NO_NAME_TYPE_SET)
              && anIdx.type() != FsIndex.NoClassSet.class) {
        throw new IllegalStateException("Class [" + componentClass.getName() + "] must not "
                + "declare an " + org.apache.uima.fit.descriptor.FsIndex.class.getSimpleName()
                + " with type and typeName both set at the same time.");
      }

      String typeName;
      if (!anIdx.typeName().equals(FsIndex.NO_NAME_TYPE_SET)) {
        typeName = anIdx.typeName();
      } else if (anIdx.type() != FsIndex.NoClassSet.class) {
        typeName = anIdx.type().getName();
      } else {
        throw new IllegalStateException("Class [" + componentClass.getName() + "] must not "
                + "declare an " + org.apache.uima.fit.descriptor.FsIndex.class.getSimpleName()
                + " with neither type nor typeName set.");
      }

      fsIndexCollection.addFsIndex(createFsIndexDescription(anIdx.label(), anIdx.kind(), typeName,
              anIdx.typePriorities(), keys.toArray(new FsIndexKeyDescription[keys.size()])));
    }

    return fsIndexCollection;
  }

  /**
   * @param label
   *          the index label
   * @param kind
   *          the type of index
   * @param typeName
   *          the indexed feature structure type
   * @param useTypePriorities
   *          whether to respect type priorities
   * @param keys
   *          the index keys
   * @return the index description
   */
  public static FsIndexDescription createFsIndexDescription(String label, String kind,
          String typeName, boolean useTypePriorities, FsIndexKeyDescription... keys) {
    FsIndexDescription_impl fsIndexDescription = new FsIndexDescription_impl();
    fsIndexDescription.setLabel(label);
    fsIndexDescription.setKind(kind);
    fsIndexDescription.setTypeName(typeName);
    fsIndexDescription.setKeys(keys);
    return fsIndexDescription;
  }

  /**
   * Create a index collection from a set of descriptions.
   *
   * @param descriptions
   *          the index descriptions
   * @return the index collection
   */
  public static FsIndexCollection createFsIndexCollection(FsIndexDescription... descriptions) {
    FsIndexCollection_impl fsIndexCollection = new FsIndexCollection_impl();
    fsIndexCollection.setFsIndexes(descriptions);
    return fsIndexCollection;
  }

  /**
   * Create a index collection from a set of descriptions.
   *
   * @param descriptions
   *          the index descriptions
   * @return the index collection
   */
  public static FsIndexCollection createFsIndexCollection(
          Collection<? extends FsIndexDescription> descriptions) {
    FsIndexCollection_impl fsIndexCollection = new FsIndexCollection_impl();
    fsIndexCollection
            .setFsIndexes(descriptions.toArray(new FsIndexDescription[descriptions.size()]));
    return fsIndexCollection;
  }

  /**
   * @param featureName
   *          the feature to index
   * @return the index key description
   */
  public static FsIndexKeyDescription createFsIndexKeyDescription(String featureName) {
    return createFsIndexKeyDescription(featureName, STANDARD_COMPARE);
  }

  /**
   * @param featureName
   *          the feature to index
   * @param comparator
   *          the index comparator
   * @return the index key description
   */
  public static FsIndexKeyDescription createFsIndexKeyDescription(String featureName,
          int comparator) {
    FsIndexKeyDescription_impl key = new FsIndexKeyDescription_impl();
    key.setFeatureName(featureName);
    key.setComparator(comparator);
    key.setTypePriority(false);
    return key;
  }

  /**
   * Creates a {@link FsIndexCollection} from descriptor names.
   *
   * @param descriptorNames
   *          The fully qualified, Java-style, dotted descriptor names.
   * @return a {@link FsIndexCollection} that includes the indexes from all of the specified files.
   */
  public static FsIndexCollection createFsIndexCollection(String... descriptorNames) {
    List<Import> imports = new ArrayList<>();
    for (String descriptorName : descriptorNames) {
      Import imp = new Import_impl();
      imp.setName(descriptorName);
      imports.add(imp);
    }
    Import[] importArray = new Import[imports.size()];

    FsIndexCollection fsIndexCollection = new FsIndexCollection_impl();
    fsIndexCollection.setImports(imports.toArray(importArray));
    return fsIndexCollection;
  }

  /**
   * Creates a {@link FsIndexCollection} from a descriptor file
   *
   * @param descriptorURIs
   *          The descriptor file paths.
   * @return A {@link FsIndexCollection} that includes the indexes from all of the specified files.
   */
  public static FsIndexCollection createTypeSystemDescriptionFromPath(String... descriptorURIs) {
    List<Import> imports = new ArrayList<>();
    for (String descriptorURI : descriptorURIs) {
      Import imp = new Import_impl();
      imp.setLocation(descriptorURI);
      imports.add(imp);
    }
    Import[] importArray = new Import[imports.size()];

    FsIndexCollection fsIndexCollection = new FsIndexCollection_impl();
    fsIndexCollection.setImports(imports.toArray(importArray));
    return fsIndexCollection;
  }

  /**
   * Creates a {@link FsIndexCollection} from all index descriptions that can be found via the
   * pattern specified in the system property {@code org.apache.uima.fit.fsindex.import_pattern} or
   * via the {@code META-INF/org.apache.uima.fit/fsindexes.txt} files in the classpath.
   *
   * @return the auto-scanned indexes.
   * @throws ResourceInitializationException
   *           if the index collection could not be assembled
   */
  public static FsIndexCollection createFsIndexCollection() throws ResourceInitializationException {
    ClassLoader cl = ClassLoaderUtils.findClassloader();
    FsIndexCollection aggFsIdxCol = fsIndexCollectionsByClassloader.get(cl);
    if (aggFsIdxCol == null) {
      synchronized (CREATE_LOCK) {
        ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
        List<FsIndexDescription> fsIndexList = new ArrayList<>();

        loadFsIndexCollectionsFromScannedLocations(fsIndexList, resMgr);
        loadFsIndexCollectionsfromSPIs(fsIndexList);

        aggFsIdxCol = createFsIndexCollection(
                fsIndexList.toArray(new FsIndexDescription[fsIndexList.size()]));
        fsIndexCollectionsByClassloader.put(cl, aggFsIdxCol);
      }
    }

    return (FsIndexCollection) aggFsIdxCol.clone();
  }

  static void loadFsIndexCollectionsFromScannedLocations(List<FsIndexDescription> fsIndexList,
          ResourceManager aResMgr) throws ResourceInitializationException {
    for (String location : scanIndexDescriptors()) {
      try {
        FsIndexCollection fsIdxCol = fsIndexCollections.get(location);

        if (fsIdxCol == PLACEHOLDER) {
          // If the description has not yet been loaded, load it
          fsIdxCol = getXMLParser().parseFsIndexCollection(new XMLInputSource(location));
          fsIdxCol.resolveImports(aResMgr);
          fsIndexCollections.put(location, fsIdxCol);
        }

        fsIndexList.addAll(asList(fsIdxCol.getFsIndexes()));
        LOG.debug("Detected index at [{}]", location);
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      } catch (InvalidXMLException e) {
        LOG.warn("[{}] is not a index descriptor file. Ignoring.", location, e);
      }
    }
  }

  static void loadFsIndexCollectionsfromSPIs(List<FsIndexDescription> fsIndexList) {
    var loaded = Collections.newSetFromMap(new IdentityHashMap<>());

    ServiceLoader.load(FsIndexCollectionProvider.class).forEach(provider -> {
      for (var fsIdxCol : provider.listFsIndexCollections()) {
        loaded.add(fsIdxCol);
        fsIndexList.addAll(asList(fsIdxCol.getFsIndexes()));
        LOG.debug("Loaded legacy SPI-provided index collection at [{}]",
                fsIdxCol.getSourceUrlString());
      }
    });

    ServiceLoader.load(TypeSystemProvider.class).forEach(provider -> {
      for (var fsIdxCol : provider.listFsIndexCollections()) {
        if (loaded.contains(fsIdxCol)) {
          continue;
        }
        fsIndexList.addAll(asList(fsIdxCol.getFsIndexes()));
        LOG.debug("Loaded SPI-provided index collection at [{}]", fsIdxCol.getSourceUrlString());
      }
    });
  }

  /**
   * Get all currently accessible index descriptor locations. A scan is actually only performed on
   * the first call and the locations are cached. To force a re-scan use
   * {@link #forceIndexDescriptorsScan()}.
   *
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  public static String[] scanIndexDescriptors() throws ResourceInitializationException {
    synchronized (SCAN_LOCK) {
      ClassLoader cl = ClassLoaderUtils.findClassloader();
      String[] indexLocations = fsIndexLocationsByClassloader.get(cl);
      if (indexLocations == null) {
        indexLocations = scanDescriptors(MetaDataType.FS_INDEX);
        internFsIndexCollectionLocations(indexLocations);
        fsIndexLocationsByClassloader.put(cl, indexLocations);
      }
      return indexLocations;
    }
  }

  private static void internFsIndexCollectionLocations(String[] indexDescriptorLocations) {
    // We "intern" the location strings because we will use them as keys in the WeakHashMap
    // caching the parsed index definitions. As part of this process, we put a PLACEHOLDER into the
    // map which is replaced when the type system is actually loaded
    Map<String, String> locationStrings = new HashMap<>();
    fsIndexCollections.keySet().stream().forEach(loc -> locationStrings.put(loc, loc));
    for (int i = 0; i < indexDescriptorLocations.length; i++) {
      String existingLocString = locationStrings.get(indexDescriptorLocations[i]);
      if (existingLocString == null) {
        fsIndexCollections.put(indexDescriptorLocations[i], PLACEHOLDER);
        locationStrings.put(indexDescriptorLocations[i], indexDescriptorLocations[i]);
      } else {
        indexDescriptorLocations[i] = existingLocString;
      }
    }
  }

  /**
   * Force rescan of index descriptors. The next call to {@link #scanIndexDescriptors()} will rescan
   * all auto-import locations.
   */
  public static void forceIndexDescriptorsScan() {
    synchronized (SCAN_LOCK) {
      fsIndexLocationsByClassloader.clear();
      fsIndexCollectionsByClassloader.clear();
      fsIndexCollections.clear();
    }
  }
}
