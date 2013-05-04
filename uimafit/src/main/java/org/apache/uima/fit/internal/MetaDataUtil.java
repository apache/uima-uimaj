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
package org.apache.uima.fit.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.resource.ResourceInitializationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * INTERNAL API - Utility methods to locate and access uimaFIT meta data.
 */
public final class MetaDataUtil {

  private MetaDataUtil() {
    // No instances
  }

  /**
   * Scan patterns from manifest files and from the specified system property.
   * 
   * @param manifestPatterns
   *          pattern matching the manifest files.
   * @param importProperty
   *          system property containing additional patterns.
   * @return array or all patterns found.
   */
  public static String[] scanImportsAndManifests(MetaDataType aType)
          throws ResourceInitializationException {
    ArrayList<String> patterns = new ArrayList<String>();

    // Scan auto-import locations
    for (String property : getImportProperties(aType)) {
      patterns.addAll(Arrays.asList(System.getProperty(property, "").split(";")));
    }

    // Scan manifest
    for (String mfUrl : resolve(getManifestLocations(aType))) {
      InputStream is = null;
      try {
        is = new URL(mfUrl).openStream();
        @SuppressWarnings("unchecked")
        List<? extends String> lines = IOUtils.readLines(is);
        patterns.addAll(lines);
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }

    return patterns.toArray(new String[patterns.size()]);
  }

  /**
   * Resolve a list of patterns to a set of URLs.
   * 
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  public static String[] resolve(String... patterns) throws ResourceInitializationException {
    Set<String> locations = new HashSet<String>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      // Scan auto-import locations. Using a set to avoid scanning a pattern twice.
      for (String pattern : new TreeSet<String>(Arrays.asList(patterns))) {
        String p = pattern.trim();
        if (p.length() == 0) {
          continue;
        }
        for (Resource r : resolver.getResources(pattern)) {
          locations.add(r.getURL().toString());
        }
      }
      return locations.toArray(new String[locations.size()]);
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Get manifest locations for the specified type.
   */
  public static String[] getManifestLocations(MetaDataType aType) {
    List<String> locations = new ArrayList<String>();
    switch (aType) {
      case FS_INDEX:
        locations.add("classpath*:META-INF/org.apache.uima.fit/fsindexes.txt");
        break;
      case TYPE_SYSTEM:
        locations.add("classpath*:META-INF/org.apache.uima.fit/types.txt");
        break;
    }

    return locations.toArray(new String[locations.size()]);
  }

  /**
   * Get system properties indicating which locations to scan for descriptions of the given type. A
   * list of locations may be given separated by ";".
   */
  public static String[] getImportProperties(MetaDataType aType) {
    List<String> locations = new ArrayList<String>();
    switch (aType) {
      case FS_INDEX:
        locations.add("org.apache.uima.fit.fsindex.import_pattern");
        break;
      case TYPE_SYSTEM:
        locations.add("org.apache.uima.fit.type.import_pattern");
        break;
    }

    return locations.toArray(new String[locations.size()]);
  }
  
  /**
   * Get all currently accessible descriptor locations for the given type.
   * 
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  public static String[] scanDescriptors(MetaDataType aType)
          throws ResourceInitializationException {
    String[] locations1 = resolve(scanImportsAndManifests(aType));
    String[] locations2 = LegacySupport.getInstance().scanTypeDescriptors(aType);
    return (String[]) ArrayUtils.addAll(locations1, locations2);
  }
}
