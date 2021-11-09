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
package org.apache.uima.resource.metadata.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;

public class ImportResolver<D extends MetaDataObject, COLLECTIBLE extends MetaDataObject> {

  private Function<D, DescriptionAdapter<D, COLLECTIBLE>> adapterFactory;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  ImportResolver(Function<D, DescriptionAdapter> aAdapterFactory) {
    adapterFactory = (Function) aAdapterFactory;
  }

  /**
   * Resolves the imports in the given descriptor.
   * 
   * @param aDesc
   *          the descriptor in which to resolve the imports.
   * @param aAlreadyImportedURLs
   *          URLs of already imported descriptors, so we don't import them again. This argument may
   *          be {@code null}.
   * @param aResourceManager
   *          the resource manager used to load the imported descriptions. If this argument is
   *          {@code null} then a new default resource manager is created.
   * @throws InvalidXMLException
   *           if an import could not be processed.
   */
  public void resolveImports(D aDesc,
          Collection<String> aAlreadyImportedURLs, ResourceManager aResourceManager)
          throws InvalidXMLException {
    DescriptionAdapter<D, COLLECTIBLE> wrapper = adapterFactory.apply(aDesc);

    if (wrapper.getImports() == null || wrapper.getImports().length == 0) {
      return;
    }

    ResourceManager resourceManager = aResourceManager;
    if (aResourceManager == null) {
      resourceManager = UIMAFramework.newDefaultResourceManager();
    }

    Set<String> alreadyImportedURLs = new HashSet<>();
    Deque<String> stack = new LinkedList<>();

    if (aAlreadyImportedURLs != null) {
      alreadyImportedURLs.addAll(aAlreadyImportedURLs);
      aAlreadyImportedURLs.forEach(stack::push);
    }

    Map<Key, COLLECTIBLE> collectedObjects = new LinkedHashMap<>();

    stack.push(wrapper.unwrap().getSourceUrlString());
    resolveImports(wrapper, new HashSet<>(), collectedObjects, stack, resourceManager);
    stack.pop();

    wrapper.setCollectibles(collectedObjects.values());
    wrapper.clearImports();
  }

  /**
   * Recursively traverses the import graph and collects all imported collectible objects.
   * 
   * @param aAllCollectedObjects
   *          all the collectible objects that are found in the (transitively) imported descriptors
   *          are collected in this list.
   * @param aStack
   *          the path through the descriptor import graph that was walked to reach the current
   *          descriptor.
   * @param aResourceManager
   *          the resource manager used to load the imported descriptors.
   * @throws InvalidXMLException
   *           if an import could not be processed.
   */
  private void resolveImports(DescriptionAdapter<D, COLLECTIBLE> aWrapper, Set<String> aAlreadyVisited,
          Map<Key, COLLECTIBLE> aAllCollectedObjects, Deque<String> aStack,
          ResourceManager aResourceManager) throws InvalidXMLException {

    if (aAlreadyVisited.contains(aWrapper.unwrap().getSourceUrlString())) {
      collectAll(aWrapper, aAllCollectedObjects);
      return;
    }

    Import[] imports = aWrapper.getImports();

    if (imports.length == 0) {
      collectAll(aWrapper, aAllCollectedObjects);
      return;
    }

    List<Import> unresolvedImports = new ArrayList<>();
    collectAll(aWrapper, aAllCollectedObjects);

    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResourceManager).getImportCache();
    for (Import imp : imports) {
      // make sure Import's relative path base is set, to allow for users who create new import
      // objects
      if (imp instanceof Import_impl) {
        ((Import_impl) imp).setSourceUrlIfNull(aWrapper.unwrap().getSourceUrl());
      }

      // Skip self-imports
      if (aWrapper.unwrap().getSourceUrlString().equals(imp.getLocation())) {
        continue;
      }

      URL absUrl = imp.findAbsoluteUrl(aResourceManager);
      String absUrlString = absUrl.toString();

      // Loop cancellation - skip imports of descriptors that lie on the path from the
      // entry point to the current descriptor
      if (aStack.contains(absUrlString)) {
        unresolvedImports.add(imp);
        continue;
      }

      aStack.push(absUrlString);

      synchronized (importCache) {
        DescriptionAdapter<D, COLLECTIBLE> importedTSAdapter = adapterFactory
                .apply(getOrLoadDescription(imp, absUrl, importCache, aWrapper));

        resolveImports(importedTSAdapter, aAlreadyVisited, aAllCollectedObjects, aStack,
                aResourceManager);

        if (importedTSAdapter.getImports().length > 0) {
          // TODO: update importUrlsCache? here or at all?
          unresolvedImports.add(imp);
        }
      }

      aStack.pop();
    }

    aAlreadyVisited.add(aWrapper.unwrap().getSourceUrlString());
  }

  private void collectAll(DescriptionAdapter<D, COLLECTIBLE> aWrapper, Map<Key, COLLECTIBLE> aAllCollectibleObjects) {
    COLLECTIBLE[] collectibles = aWrapper.getCollectibles();
    for (int i = 0; i < collectibles.length; i++) {
      aAllCollectibleObjects.put(new Key(collectibles[i]), collectibles[i]);
    }
  }

  private D getOrLoadDescription(Import aImport, URL aAbsUrl, Map<String, XMLizable> aImportCache,
          DescriptionAdapter<D, COLLECTIBLE> aWrapper)
          throws InvalidXMLException {
    Class<D> descClass = aWrapper.getDescriptorClass();

    XMLizable cachedDescriptor = aImportCache.get(aAbsUrl.toString());
    if (descClass.isInstance(cachedDescriptor)) {
      return descClass.cast(cachedDescriptor);
    }

    return loadDescriptor(aImport, aAbsUrl, aImportCache, aWrapper.getParser());
  }

  private D loadDescriptor(Import aImport, URL aAbsUrl, Map<String, XMLizable> aImportCache,
          ParserFunction<D> aParserFunction)
          throws InvalidXMLException {
    String urlString = aAbsUrl.toString();
    try {
      XMLInputSource input = new XMLInputSource(aAbsUrl);
      D descriptor = aParserFunction.apply(input);
      aImportCache.put(urlString, descriptor);
      return descriptor;
    } catch (IOException e) {
      throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
              new Object[] { aAbsUrl, aImport.getLocation() }, e);
    }
  }

  interface DescriptionAdapter<DESCRIPTOR, COLLECTIBLE> {
    Import[] getImports();

    void clearImports();

    void setCollectibles(Collection<COLLECTIBLE> aCollectedObjects);

    COLLECTIBLE[] getCollectibles();

    MetaDataObject unwrap();

    Class<DESCRIPTOR> getDescriptorClass();

    Class<COLLECTIBLE> getCollectedClass();

    ParserFunction<DESCRIPTOR> getParser();
  }

  @FunctionalInterface
  interface ParserFunction<T> {
    T apply(XMLInputSource aArg0) throws InvalidXMLException;
  }

  private class Key {
    private final COLLECTIBLE wrapped;

    public Key(COLLECTIBLE aObj) {
      wrapped = aObj;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(wrapped);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Key other = (Key) obj;
      return wrapped == other.wrapped;
    }
  }
}
