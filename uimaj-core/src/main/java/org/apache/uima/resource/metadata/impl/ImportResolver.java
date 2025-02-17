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

import static java.util.Arrays.asList;
import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

class ImportResolver<DESCRIPTOR extends MetaDataObject, COLLECTIBLE extends MetaDataObject> {

  private Function<DESCRIPTOR, DescriptorAdapter<DESCRIPTOR, COLLECTIBLE>> adapterFactory;
  private ThreadLocal<Map<String, URL>> absUrlCacheHolder = ThreadLocal
          .withInitial(() -> new HashMap<>());

  @SuppressWarnings({ "rawtypes", "unchecked" })
  ImportResolver(Function<DESCRIPTOR, DescriptorAdapter> aAdapterFactory) {
    adapterFactory = (Function) aAdapterFactory;
  }

  /**
   * Resolves the imports in the given descriptor.
   * 
   * @param aDesc
   *          the descriptor in which to resolve the imports.
   * @param aResourceManager
   *          the resource manager used to load the imported descriptions. If this argument is
   *          {@code null} then a new default resource manager is created.
   * @throws InvalidXMLException
   *           if an import could not be processed.
   */
  void resolveImports(DESCRIPTOR aDesc, ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(aDesc, null, aResourceManager);
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
  void resolveImports(DESCRIPTOR aDesc, Collection<String> aAlreadyImportedURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    DescriptorAdapter<DESCRIPTOR, COLLECTIBLE> wrapper = adapterFactory.apply(aDesc);

    if (wrapper.getImports() == null || wrapper.getImports().length == 0) {
      return;
    }

    try {
      Set<COLLECTIBLE> originalTypes = newSetFromMap(new IdentityHashMap<>());
      originalTypes.addAll(asList(wrapper.getCollectibles()));

      var resourceManager = aResourceManager;
      if (aResourceManager == null) {
        resourceManager = UIMAFramework.newDefaultResourceManager();
      }

      var alreadyImportedURLs = new HashSet<String>();
      var stack = new LinkedList<String>();

      if (aAlreadyImportedURLs != null) {
        alreadyImportedURLs.addAll(aAlreadyImportedURLs);
        aAlreadyImportedURLs.forEach(stack::push);
      }

      var collectedObjects = new LinkedHashMap<Key, COLLECTIBLE>();

      stack.push(wrapper.unwrap().getSourceUrlString());
      resolveImports(wrapper, new HashSet<>(), collectedObjects, stack, resourceManager);
      stack.pop();

      // Defensive copy to prevent cache pollution in case the caller makes changes to the by
      // casting collectibles to their mutable implementation and making changes to them.
      wrapper.setCollectibles(collectedObjects.values().stream() //
              .map(c -> originalTypes.contains(c) ? c : (COLLECTIBLE) c.clone()) //
              .collect(toList()));
      wrapper.clearImports();
    } finally {
      absUrlCacheHolder.remove();
    }
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
  private void resolveImports(DescriptorAdapter<DESCRIPTOR, COLLECTIBLE> aWrapper,
          Set<String> aAlreadyVisited, Map<Key, COLLECTIBLE> aAllCollectedObjects,
          Deque<String> aStack, ResourceManager aResourceManager) throws InvalidXMLException {

    collectAll(aWrapper, aAllCollectedObjects);

    if (aAlreadyVisited.contains(aWrapper.unwrap().getSourceUrlString())) {
      return;
    }

    var imports = aWrapper.getImports();
    if (imports.length == 0) {
      return;
    }

    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResourceManager).getImportCache();
    for (var imp : imports) {
      // make sure Import's relative path base is set, to allow for users who create new import
      // objects
      if (imp instanceof Import_impl importImpl) {
        importImpl.setSourceUrlIfNull(aWrapper.unwrap().getSourceUrl());
      }

      // Skip self-imports
      if (aWrapper.unwrap().getSourceUrlString().equals(imp.getLocation())) {
        continue;
      }

      var absUrl = findAbsoluteUrl(aResourceManager, imp);
      var absUrlString = absUrl.toString();

      // Loop cancellation - skip imports of descriptors that lie on the path from the
      // entry point to the current descriptor
      if (aStack.contains(absUrlString)) {
        continue;
      }

      aStack.push(absUrlString);

      synchronized (importCache) {
        var importedDescriptionAdapter = adapterFactory
                .apply(getOrLoadDescription(imp, absUrl, importCache, aWrapper));

        resolveImports(importedDescriptionAdapter, aAlreadyVisited, aAllCollectedObjects, aStack,
                aResourceManager);
      }

      aStack.pop();
    }

    aAlreadyVisited.add(aWrapper.unwrap().getSourceUrlString());
  }

  private URL findAbsoluteUrl(ResourceManager aResourceManager, Import imp)
          throws InvalidXMLException {
    String absUrlCacheKey;
    if (imp.getLocation() != null) {
      absUrlCacheKey = "location:" + imp.getLocation();
    } else {
      absUrlCacheKey = "name:" + imp.getName();
    }

    var absUrlCache = absUrlCacheHolder.get();
    var absUrl = absUrlCache.get(absUrlCacheKey);
    if (absUrl == null) {
      absUrl = imp.findAbsoluteUrl(aResourceManager);
      absUrlCache.put(absUrlCacheKey, absUrl);
    }

    return absUrl;
  }

  private void collectAll(DescriptorAdapter<DESCRIPTOR, COLLECTIBLE> aWrapper,
          Map<Key, COLLECTIBLE> aAllCollectibleObjects) {
    COLLECTIBLE[] collectibles = aWrapper.getCollectibles();
    for (int i = 0; i < collectibles.length; i++) {
      aAllCollectibleObjects.put(new Key(collectibles[i]), collectibles[i]);
    }
  }

  private DESCRIPTOR getOrLoadDescription(Import aImport, URL aAbsUrl,
          Map<String, XMLizable> aImportCache, DescriptorAdapter<DESCRIPTOR, COLLECTIBLE> aWrapper)
          throws InvalidXMLException {
    Class<DESCRIPTOR> descClass = aWrapper.getDescriptorClass();

    var cachedDescriptor = aImportCache.get(aAbsUrl.toString());
    if (descClass.isInstance(cachedDescriptor)) {
      return descClass.cast(cachedDescriptor);
    }

    return loadDescriptor(aImport, aAbsUrl, aImportCache, aWrapper.getParser());
  }

  private DESCRIPTOR loadDescriptor(Import aImport, URL aAbsUrl,
          Map<String, XMLizable> aImportCache, ParserFunction<DESCRIPTOR> aParserFunction)
          throws InvalidXMLException {
    var urlString = aAbsUrl.toString();
    try {
      var input = new XMLInputSource(aAbsUrl);
      DESCRIPTOR descriptor = aParserFunction.apply(input);
      aImportCache.put(urlString, descriptor);
      return descriptor;
    } catch (IOException e) {
      throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
              new Object[] { aAbsUrl, aImport.getLocation() }, e);
    }
  }

  interface DescriptorAdapter<DESCRIPTOR, COLLECTIBLE> {
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
  interface ParserFunction<DESCRIPTOR> {
    DESCRIPTOR apply(XMLInputSource aArg0) throws InvalidXMLException;
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
      if ((obj == null) || (getClass() != obj.getClass())) {
        return false;
      }
      Key other = (Key) obj;
      return wrapped == other.wrapped;
    }
  }
}
