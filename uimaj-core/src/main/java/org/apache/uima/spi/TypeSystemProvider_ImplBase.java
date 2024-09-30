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
package org.apache.uima.spi;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.uima.util.TypeSystemUtil.loadTypeSystemDescriptionsFromClasspath;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.TypeSystemUtil;

public abstract class TypeSystemProvider_ImplBase implements TypeSystemProvider {

  private Set<String> typeSystemPaths = emptySet();
  private Set<String> fsIndexCollectionPaths = emptySet();
  private Set<String> typePrioritiesPaths = emptySet();

  private List<TypeSystemDescription> typeSystemDescriptions;
  private List<FsIndexCollection> fsIndexCollections;
  private List<TypePriorities> typePriorities;
  private List<Class<? extends TOP>> jCasClasses;

  protected void setTypeSystemLocations(String... aLocations) {
    typeSystemPaths = unmodifiableSet(resolveLocations(aLocations));
  }

  @Override
  public synchronized List<TypeSystemDescription> listTypeSystemDescriptions() {

    if (typeSystemDescriptions == null) {
      typeSystemDescriptions = loadTypeSystemDescriptionsFromClasspath(getClass(), //
              typeSystemPaths.toArray(String[]::new));
    }

    return typeSystemDescriptions;
  }

  protected void setFsIndexCollectionLocations(String... aLocations) {
    fsIndexCollectionPaths = unmodifiableSet(resolveLocations(aLocations));
  }

  @Override
  public synchronized List<FsIndexCollection> listFsIndexCollections() {

    if (fsIndexCollections == null) {
      fsIndexCollections = TypeSystemUtil.loadFsIndexCollectionsFromClasspath(getClass(), //
              fsIndexCollectionPaths.toArray(String[]::new));
    }

    return fsIndexCollections;
  }

  protected void setTypePrioritiesLocations(String... aLocations) {
    typePrioritiesPaths = unmodifiableSet(resolveLocations(aLocations));
  }

  @Override
  public synchronized List<TypePriorities> listTypePriorities() {
    if (typePriorities == null) {
      typePriorities = TypeSystemUtil.loadTypePrioritiesFromClasspath(getClass(), //
              typePrioritiesPaths.toArray(String[]::new));
    }

    return typePriorities;
  }

  private Set<String> resolveLocations(String... aLocations) {
    var paths = new HashSet<String>();
    var packagePath = "/" + getClass().getPackage().getName().replace('.', '/') + "/";

    for (var location : aLocations) {
      var path = location;

      // Resolve relative locations to the current package
      if (!path.startsWith("/")) {
        path = packagePath + path;
      }

      path += ".xml";

      paths.add(path);
    }

    return paths;
  }

  @Override
  public Optional<URL> findResourceUrl(String aName) {

    var fullName = "/" + aName.replace('.', '/') + ".xml";
    if (typeSystemPaths.contains(fullName)) {
      return ofNullable(getClass().getResource(fullName));
    }

    return empty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized List<Class<? extends TOP>> listJCasClasses() {

    if (jCasClasses == null) {
      var classes = new ArrayList<Class<? extends TOP>>();
      var cl = getClass().getClassLoader();

      for (var tsd : listTypeSystemDescriptions()) {
        for (var td : tsd.getTypes()) {
          try {
            classes.add((Class<? extends TOP>) cl.loadClass(td.getName()));
          } catch (ClassNotFoundException e) {
            // This is acceptable - there may not be a JCas class
          }
        }
      }
      jCasClasses = classes;
    }

    return jCasClasses;
  }
}
