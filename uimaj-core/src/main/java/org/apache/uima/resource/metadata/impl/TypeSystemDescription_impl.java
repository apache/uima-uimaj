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
import static org.apache.uima.UIMAFramework.getXMLParser;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;

/**
 * Reference implementation of {@link TypeSystemDescription}.
 * 
 * 
 */
public class TypeSystemDescription_impl extends MetaDataObject_impl
        implements TypeSystemDescription {

  static final long serialVersionUID = -3372766232454730201L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = Import.EMPTY_IMPORTS;

  /** Descriptions of all Types in this type system. */
  private TypeDescription[] mTypes = TypeDescription.EMPTY_TYPE_DESCRIPTIONS;

  /**
   * Creates a new TypeSystemDescription_impl.
   */
  public TypeSystemDescription_impl() {
  }

  /**
   * @see ResourceMetaData#getName()
   */
  @Override
  public String getName() {
    return mName;
  }

  /**
   * @see ResourceMetaData#setName(String)
   */
  @Override
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see ResourceMetaData#getVersion()
   */
  @Override
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see ResourceMetaData#setVersion(String)
   */
  @Override
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see ResourceMetaData#getDescription()
   */
  @Override
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see ResourceMetaData#setDescription(String)
   */
  @Override
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see ResourceMetaData#getVendor()
   */
  @Override
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see ResourceMetaData#setVendor(String)
   */
  @Override
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /**
   * @see TypeSystemDescription#getImports()
   */
  @Override
  public Import[] getImports() {
    return mImports;
  }

  /**
   * @see TypeSystemDescription#setImports(Import[])
   */
  @Override
  public void setImports(Import[] aImports) {
    if (aImports == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aImports", "setImports" });
    }
    mImports = aImports;
  }

  /**
   * @see TypeSystemDescription#getTypes()
   */
  @Override
  public TypeDescription[] getTypes() {
    return mTypes;
  }

  /**
   * @see TypeSystemDescription#setTypes(TypeDescription[])
   */
  @Override
  public void setTypes(TypeDescription[] aTypes) {
    if (aTypes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aTypes", "setTypes" });
    }
    mTypes = aTypes;
  }

  /**
   * @see TypeSystemDescription#addType(String, String, String)
   */
  @Override
  public TypeDescription addType(String aTypeName, String aDescription, String aSupertypeName) {
    // create new type description
    TypeDescription newType = new TypeDescription_impl(aTypeName, aDescription, aSupertypeName);

    // add to array
    TypeDescription[] types = getTypes();
    if (types == null) {
      setTypes(new TypeDescription[] { newType });
    } else {
      TypeDescription[] newArray = new TypeDescription[types.length + 1];
      System.arraycopy(types, 0, newArray, 0, types.length);
      newArray[types.length] = newType;
      setTypes(newArray);
    }

    return newType;
  }

  /**
   * @see TypeSystemDescription#getType(java.lang.String)
   */
  @Override
  public TypeDescription getType(String aTypeName) {
    for (int i = 0; i < mTypes.length; i++) {
      if (aTypeName.equals(mTypes[i].getName())) {
        return mTypes[i];
      }
    }
    return null;
  }

  /**
   * @see TypeSystemDescription#resolveImports()
   */
  // allow these calls to be done multiple times on this same object, in different threads
  @Override
  public synchronized void resolveImports() throws InvalidXMLException {
    resolveImports(UIMAFramework.newDefaultResourceManager());
  }

  @Override
  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(new TreeSet<>(), aResourceManager);
  }

  @Deprecated
  @Override
  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {

    // TODO what to do with aAlreadyImportedTypeSystemURLs???
    List<TypeDescription> result = new ArrayList<>();

    if (getImports() == null || getImports().length == 0) {
      return;
    }

    Deque<String> typeSystemStack = new LinkedList<>();
    typeSystemStack.push(getSourceUrlString());
    resolveImports(this, new HashSet<>(), result, typeSystemStack, aResourceManager);
    typeSystemStack.pop();

    setTypes(result.toArray(new TypeDescription_impl[0]));
    setImports(Import.EMPTY_IMPORTS);
  }

  /**
   * Resolves the imports in the current type system description. If there are circular dependencies
   * between the type systems, the those imports involved in the circular dependency will remain
   * unresolved.
   * 
   * @param aAllImportedTypes
   *          all the types that are found in the (transitively) imported type systems are collected
   *          in this list.
   * @param aStack
   *          the path through the type system import graph that was walked to reach the current
   *          type system.
   * @param aResourceManager
   *          the resource manager used to load the imported type systems.
   * @throws InvalidXMLException
   *           if an import could not be processed.
   */
  private static void resolveImports(TypeSystemDescription aDesc, Set<String> aAlreadyVisited,
          Collection<TypeDescription> aAllImportedTypes, Deque<String> aStack,
          ResourceManager aResourceManager) throws InvalidXMLException {

    if (aAlreadyVisited.contains(aDesc.getSourceUrlString())) {
      return;
    }

    Import[] imports = aDesc.getImports();

    if (imports.length == 0) {
      aAllImportedTypes.addAll(asList(aDesc.getTypes()));
      return;
    }

    List<Import> unresolvedImports = new ArrayList<>();
    Set<TypeDescription> resolvedTypes = new HashSet<>(asList(aDesc.getTypes()));

    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResourceManager).getImportCache();
    for (Import tsImport : imports) {
      if (aDesc.getSourceUrlString().equals(tsImport.getLocation())) {
        continue;
      }

      URL absUrl = tsImport.findAbsoluteUrl(aResourceManager);
      String absUrlString = absUrl.toString();

      if (aStack.contains(absUrlString)) {
        unresolvedImports.add(tsImport);
        continue;
      }

      aStack.push(absUrlString);

      // The imported type system is obtained from the resource manager cache or loaded from disk
      // and then immediately added to the resource manager cache. Afterwards, the type system is
      // resolved. This can cause a change to the (cached) type system (i.e. imported types being
      // inlined and imports being removed). To avoid concurrency problems, we need to synchronize
      // this entire process (loading and resolving) so that other threads only see them once they
      // are in a state that does not change anymore.
      synchronized (importCache) {
        TypeSystemDescription importedTS = getOrLoadTypeSystemDescription(tsImport, absUrl,
                importCache);

        resolveImports(importedTS, aAlreadyVisited, resolvedTypes, aStack, aResourceManager);

        if (importedTS.getImports().length > 0) {
          // TODO: update importUrlsCache? here or at all?
          unresolvedImports.add(tsImport);
        }
      }

      aStack.pop();
    }

    // update own resolved status
    aDesc.setTypes(resolvedTypes.toArray(new TypeDescription_impl[resolvedTypes.size()]));
    aDesc.setImports(unresolvedImports.toArray(new Import[unresolvedImports.size()]));

    aAllImportedTypes.addAll(resolvedTypes);

    aAlreadyVisited.add(aDesc.getSourceUrlString());
  }

  private static TypeSystemDescription getOrLoadTypeSystemDescription(Import aTsImport, URL aAbsUrl,
          Map<String, XMLizable> aImportCache) throws InvalidXMLException {
    XMLizable cachedObject = aImportCache.get(aAbsUrl.toString());
    if (cachedObject instanceof TypeSystemDescription) {
      return (TypeSystemDescription) cachedObject;
    }

    return loadTypeSystemDescription(aTsImport, aAbsUrl, aImportCache);
  }

  private static TypeSystemDescription loadTypeSystemDescription(Import aTsImport, URL aAbsUrl,
          Map<String, XMLizable> aImportCache) throws InvalidXMLException {
    String urlString = aAbsUrl.toString();
    try {
      XMLInputSource input = new XMLInputSource(aAbsUrl);
      TypeSystemDescription description = getXMLParser().parseTypeSystemDescription(input);
      aImportCache.put(urlString, description);
      return description;
    } catch (IOException e) {
      throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
              new Object[] { aAbsUrl, aTsImport.getLocation() }, e);
    }
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("types", true) });
}
