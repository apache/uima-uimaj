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

import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.fit.internal.MetaDataUtil.scanDescriptors;
import static org.apache.uima.fit.util.CasUtil.UIMA_BUILTIN_JCAS_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.apache.uima.fit.internal.ClassLoaderUtils;
import org.apache.uima.fit.internal.MetaDataType;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.spi.TypePrioritiesProvider;
import org.apache.uima.spi.TypeSystemProvider;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypePrioritiesFactory {
  private static final Logger LOG = LoggerFactory.getLogger(TypePrioritiesFactory.class);

  private static final Object SCAN_LOCK = new Object();

  private static final Object CREATE_LOCK = new Object();

  private static final TypePriorities PLACEHOLDER = new TypePriorities_impl();

  private static WeakHashMap<String, TypePriorities> typePriorities;

  private static WeakHashMap<ClassLoader, String[]> typePrioritesLocationsByClassloader;

  private static WeakHashMap<ClassLoader, TypePriorities> typePrioritiesByClassloader;

  static {
    typePriorities = new WeakHashMap<>();
    typePrioritesLocationsByClassloader = new WeakHashMap<>();
    typePrioritiesByClassloader = new WeakHashMap<>();
  }

  private TypePrioritiesFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Create a TypePriorities given a sequence of ordered type classes
   *
   * @param prioritizedTypes
   *          a sequence of ordered type classes
   * @return type priorities created from the ordered JCas classes
   */
  @SafeVarargs
  public static TypePriorities createTypePriorities(Class<? extends TOP>... prioritizedTypes) {
    String[] typeNames = new String[prioritizedTypes.length];
    for (int i = 0; i < prioritizedTypes.length; i++) {
      if (!TOP.class.isAssignableFrom(prioritizedTypes[i])) {
        throw new IllegalArgumentException("[" + prioritizedTypes[i] + "] is not a JCas type");
      }

      String typeName = prioritizedTypes[i].getName();
      if (typeName.startsWith(UIMA_BUILTIN_JCAS_PREFIX)) {
        typeName = "uima." + typeName.substring(UIMA_BUILTIN_JCAS_PREFIX.length());
      }

      typeNames[i] = typeName;
    }
    return createTypePriorities(typeNames);
  }

  /**
   * Create a {@link TypePriorities} given a sequence of ordered type names
   *
   * @param prioritizedTypeNames
   *          a sequence of ordered type names
   * @return type priorities created from the ordered type names
   */
  public static TypePriorities createTypePriorities(String... prioritizedTypeNames) {
    TypePriorities priorities = new TypePriorities_impl();
    TypePriorityList typePriorityList = priorities.addPriorityList();
    for (String typeName : prioritizedTypeNames) {
      typePriorityList.addType(typeName);
    }
    return priorities;
  }

  /**
   * Creates a {@link TypePriorities} from all type priorities descriptions that can be found via
   * the pattern specified in the system property
   * {@code org.apache.uima.fit.typepriorities.import_pattern} or via the
   * {@code META-INF/org.apache.uima.fit/typepriorities.txt} files in the classpath.
   *
   * @return the auto-scanned type priorities.
   * @throws ResourceInitializationException
   *           if the collected type priorities cannot be merged.
   */
  public static TypePriorities createTypePriorities() throws ResourceInitializationException {
    ClassLoader cl = ClassLoaderUtils.findClassloader();
    TypePriorities aggTypePriorities = typePrioritiesByClassloader.get(cl);
    if (aggTypePriorities == null) {
      synchronized (CREATE_LOCK) {
        ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
        List<TypePriorities> typePrioritiesList = new ArrayList<>();

        loadTypePrioritiesFromScannedLocations(typePrioritiesList, resMgr);
        loadTypePrioritiesFromSPIs(typePrioritiesList);

        aggTypePriorities = CasCreationUtils.mergeTypePriorities(typePrioritiesList, resMgr);
        typePrioritiesByClassloader.put(cl, aggTypePriorities);
      }
    }

    return (TypePriorities) aggTypePriorities.clone();
  }

  static void loadTypePrioritiesFromScannedLocations(List<TypePriorities> typePrioritiesList,
          ResourceManager aResMgr) throws ResourceInitializationException {
    for (var location : scanTypePrioritiesDescriptors()) {
      try {
        var priorities = typePriorities.get(location);

        if (priorities == PLACEHOLDER) {
          // If the description has not yet been loaded, load it
          priorities = getXMLParser().parseTypePriorities(new XMLInputSource(location));
          priorities.resolveImports(aResMgr);
          typePriorities.put(location, priorities);
        }

        typePrioritiesList.add(priorities);
        LOG.debug("Detected type priorities at [{}]", location);
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      } catch (InvalidXMLException e) {
        LOG.warn("[{}] is not a type priorities descriptor file. Ignoring.", location, e);
      }
    }
  }

  static void loadTypePrioritiesFromSPIs(List<TypePriorities> typePrioritiesList) {
    var loaded = Collections.newSetFromMap(new IdentityHashMap<>());

    ServiceLoader.load(TypePrioritiesProvider.class).forEach(provider -> {
      for (var desc : provider.listTypePriorities()) {
        loaded.add(desc);
        typePrioritiesList.add(desc);
        LOG.debug("Loaded legacy SPI-provided type priorities at [{}]", desc.getSourceUrlString());
      }
    });

    ServiceLoader.load(TypeSystemProvider.class).forEach(provider -> {
      for (var desc : provider.listTypePriorities()) {
        if (loaded.contains(desc)) {
          continue;
        }
        typePrioritiesList.add(desc);
        LOG.debug("Loaded SPI-provided type priorities at [{}]", desc.getSourceUrlString());
      }
    });
  }

  /**
   * Get all currently accessible type priorities descriptor locations. A scan is actually only
   * performed on the first call and the locations are cached. To force a re-scan use
   * {@link #forceTypePrioritiesDescriptorsScan()}.
   *
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  public static String[] scanTypePrioritiesDescriptors() throws ResourceInitializationException {
    synchronized (SCAN_LOCK) {
      var cl = ClassLoaderUtils.findClassloader();
      var typePrioritesLocations = typePrioritesLocationsByClassloader.get(cl);
      if (typePrioritesLocations == null) {
        typePrioritesLocations = scanDescriptors(MetaDataType.TYPE_PRIORITIES);
        internTypePrioritiesLocations(typePrioritesLocations);
        typePrioritesLocationsByClassloader.put(cl, typePrioritesLocations);
      }
      return typePrioritesLocations;
    }
  }

  private static void internTypePrioritiesLocations(String[] typeDescriptorLocations) {
    // We "intern" the location strings because we will use them as keys in the WeakHashMap
    // caching the parsed type priorities. As part of this process, we put a PLACEHOLDER into the
    // map which is replaced when the type system is actually loaded
    Map<String, String> locationStrings = new HashMap<>();
    typePriorities.keySet().stream().forEach(loc -> locationStrings.put(loc, loc));
    for (int i = 0; i < typeDescriptorLocations.length; i++) {
      String existingLocString = locationStrings.get(typeDescriptorLocations[i]);
      if (existingLocString == null) {
        typePriorities.put(typeDescriptorLocations[i], PLACEHOLDER);
        locationStrings.put(typeDescriptorLocations[i], typeDescriptorLocations[i]);
      } else {
        typeDescriptorLocations[i] = existingLocString;
      }
    }
  }

  /**
   * Force rescan of type priorities descriptors. The next call to
   * {@link #scanTypePrioritiesDescriptors()} will rescan all auto-import locations.
   */
  public static void forceTypePrioritiesDescriptorsScan() {
    synchronized (SCAN_LOCK) {
      typePrioritesLocationsByClassloader.clear();
      typePrioritiesByClassloader.clear();
      typePriorities.clear();
    }
  }
}
