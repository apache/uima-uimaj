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
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.apache.uima.fit.internal.ClassLoaderUtils;
import org.apache.uima.fit.internal.MetaDataType;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.spi.TypeSystemDescriptionProvider;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypeSystemDescriptionFactory {

  private static final Logger LOG = LoggerFactory.getLogger(TypeSystemDescriptionFactory.class);

  private static final Object SCAN_LOCK = new Object();

  private static final Object CREATE_LOCK = new Object();

  private static final TypeSystemDescription PLACEHOLDER = new TypeSystemDescription_impl();

  private static WeakHashMap<String, TypeSystemDescription> typeDescriptors;

  private static WeakHashMap<ClassLoader, String[]> typeDescriptorLocationsByClassloader;

  private static WeakHashMap<ClassLoader, TypeSystemDescription> typeDescriptorByClassloader;

  static {
    typeDescriptors = new WeakHashMap<>();
    typeDescriptorLocationsByClassloader = new WeakHashMap<>();
    typeDescriptorByClassloader = new WeakHashMap<>();
  }

  private TypeSystemDescriptionFactory() {

    // This class is not meant to be instantiated
  }

  /**
   * Creates a TypeSystemDescription from descriptor names.
   *
   * @param descriptorNames
   *          The fully qualified, Java-style, dotted descriptor names.
   * @return A TypeSystemDescription that includes the types from all of the specified files.
   */
  public static TypeSystemDescription createTypeSystemDescription(String... descriptorNames) {

    var typeSystem = new TypeSystemDescription_impl();
    var imports = new ArrayList<Import>();
    for (var descriptorName : descriptorNames) {
      var imp = new Import_impl();
      imp.setName(descriptorName);
      imports.add(imp);
    }
    var importArray = new Import[imports.size()];
    typeSystem.setImports(imports.toArray(importArray));
    return typeSystem;
  }

  /**
   * Creates a {@link TypeSystemDescription} from a descriptor file
   *
   * @param descriptorURIs
   *          The descriptor file paths.
   * @return A TypeSystemDescription that includes the types from all of the specified files.
   */
  public static TypeSystemDescription createTypeSystemDescriptionFromPath(
          String... descriptorURIs) {

    var typeSystem = new TypeSystemDescription_impl();
    var imports = new ArrayList<Import>();
    for (var descriptorURI : descriptorURIs) {
      var imp = new Import_impl();
      imp.setLocation(descriptorURI);
      imports.add(imp);
    }
    var importArray = new Import[imports.size()];
    typeSystem.setImports(imports.toArray(importArray));
    return typeSystem;
  }

  /**
   * Creates a {@link TypeSystemDescription} from all type descriptions that can be found via the
   * default import pattern or via the {@code META-INF/org.apache.uima.fit/types.txt} files in the
   * classpath.
   *
   * @return the auto-scanned type system.
   * @throws ResourceInitializationException
   *           if the collected type system descriptions cannot be merged.
   */
  public static TypeSystemDescription createTypeSystemDescription()
          throws ResourceInitializationException {

    var cl = ClassLoaderUtils.findClassloader();
    var tsd = typeDescriptorByClassloader.get(cl);
    if (tsd == null) {
      synchronized (CREATE_LOCK) {
        var resMgr = ResourceManagerFactory.newResourceManager();
        var tsdList = new ArrayList<TypeSystemDescription>();

        loadTypeSystemDescriptionsFromScannedLocations(tsdList, resMgr);
        loadTypeSystemDescriptionsFromSPIs(tsdList);

        LOG.trace("Merging type systems and resolving imports...");
        tsd = mergeTypeSystems(tsdList, resMgr);
        typeDescriptorByClassloader.put(cl, tsd);
      }
    }
    return (TypeSystemDescription) tsd.clone();
  }

  static void loadTypeSystemDescriptionsFromScannedLocations(List<TypeSystemDescription> tsdList,
          ResourceManager aResMgr) throws ResourceInitializationException {
    for (var location : scanTypeDescriptors()) {
      try {
        var description = typeDescriptors.get(location);

        if (description == PLACEHOLDER) {
          // If the description has not yet been loaded, load it
          description = getXMLParser().parseTypeSystemDescription(new XMLInputSource(location));
          description.resolveImports(aResMgr);
          typeDescriptors.put(location, description);
        }

        tsdList.add(description);
        LOG.debug("Detected type system at [{}]", location);
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      } catch (InvalidXMLException e) {
        LOG.warn("[{}] is not a type file. Ignoring.", location, e);
      }
    }
  }

  static void loadTypeSystemDescriptionsFromSPIs(List<TypeSystemDescription> tsdList) {
    var loader = ServiceLoader.load(TypeSystemDescriptionProvider.class);
    loader.forEach(provider -> {
      for (var desc : provider.listTypeSystemDescriptions()) {
        tsdList.add(desc);
        LOG.debug("Loaded SPI-provided type system at [{}]", desc.getSourceUrlString());
      }
    });
  }

  /**
   * Get all currently accessible type system descriptor locations. A scan is actually only
   * performed on the first call and the locations are cached. To force a re-scan use
   * {@link #forceTypeDescriptorsScan()}.
   *
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  public static String[] scanTypeDescriptors() throws ResourceInitializationException {

    synchronized (SCAN_LOCK) {
      var cl = ClassLoaderUtils.findClassloader();
      var typeDescriptorLocations = typeDescriptorLocationsByClassloader.get(cl);

      if (typeDescriptorLocations == null) {
        typeDescriptorLocations = scanDescriptors(MetaDataType.TYPE_SYSTEM);

        internTypeDescriptorLocations(typeDescriptorLocations);

        typeDescriptorLocationsByClassloader.put(cl, typeDescriptorLocations);
      }

      return typeDescriptorLocations;
    }
  }

  private static void internTypeDescriptorLocations(String[] typeDescriptorLocations) {
    // We "intern" the location strings because we will use them as keys in the WeakHashMap
    // caching the parsed type systems. As part of this process, we put a PLACEHOLDER into the
    // map which is replaced when the type system is actually loaded
    var locationStrings = new HashMap<String, String>();
    typeDescriptors.keySet().stream().forEach(loc -> locationStrings.put(loc, loc));
    for (int i = 0; i < typeDescriptorLocations.length; i++) {
      var existingLocString = locationStrings.get(typeDescriptorLocations[i]);
      if (existingLocString == null) {
        typeDescriptors.put(typeDescriptorLocations[i], PLACEHOLDER);
        locationStrings.put(typeDescriptorLocations[i], typeDescriptorLocations[i]);
      } else {
        typeDescriptorLocations[i] = existingLocString;
      }
    }
  }

  /**
   * Force rescan of type descriptors. The next call to {@link #scanTypeDescriptors()} will rescan
   * all auto-import locations.
   */
  public static void forceTypeDescriptorsScan() {

    synchronized (SCAN_LOCK) {
      typeDescriptorLocationsByClassloader.clear();
      typeDescriptorByClassloader.clear();
      typeDescriptors.clear();
    }
  }
}
