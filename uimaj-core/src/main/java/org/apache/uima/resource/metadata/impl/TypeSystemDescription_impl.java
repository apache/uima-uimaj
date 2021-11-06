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
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
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
  public String getName() {
    return mName;
  }

  /**
   * @see ResourceMetaData#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see ResourceMetaData#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see ResourceMetaData#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see ResourceMetaData#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see ResourceMetaData#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see ResourceMetaData#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see ResourceMetaData#setVendor(String)
   */
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /**
   * @see TypeSystemDescription#getImports()
   */
  public Import[] getImports() {
    return mImports;
  }

  /**
   * @see TypeSystemDescription#setImports(Import[])
   */
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
  public TypeDescription[] getTypes() {
    return mTypes;
  }

  /**
   * @see TypeSystemDescription#setTypes(TypeDescription[])
   */
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
  public TypeDescription getType(String aTypeName) {
    for (int i = 0; i < mTypes.length; i++) {
      if (aTypeName.equals(mTypes[i].getName()))
        return mTypes[i];
    }
    return null;
  }

  /**
   * @see TypeSystemDescription#resolveImports()
   */
  // allow these calls to be done multiple times on this same object, in different threads
  public synchronized void resolveImports() throws InvalidXMLException {
    resolveImports(UIMAFramework.newDefaultResourceManager());
  }

  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(new TreeSet<>(), aResourceManager);
  }

  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {

    List<TypeDescription> result = new ArrayList<>();

    if (getImports() == null || getImports().length == 0) {
      return;
    }

    Deque<String> typeSystemStack = new LinkedList<>();
    typeSystemStack.push(getSourceUrlString());
    aAlreadyImportedTypeSystemURLs.add(getSourceUrlString());

    collectTypeDescriptions(result, typeSystemStack, aAlreadyImportedTypeSystemURLs,
            aResourceManager);
    typeSystemStack.pop();

    setTypes(result.toArray(new TypeDescription_impl[0]));
    // clear imports
    this.setImports(Import.EMPTY_IMPORTS);
  }

  public boolean collectTypeDescriptions(List<TypeDescription> result, Deque<String> stack,
          Collection<String> visited, ResourceManager aResourceManager) throws InvalidXMLException {

    List<Import> unresolvedImports = new ArrayList<>();
    List<TypeDescription> resolvedTypeDescriptions = new ArrayList<>();
    resolvedTypeDescriptions.addAll(Arrays.asList(getTypes()));

    boolean completelyResolved = true;

    Import[] imports = getImports();
    for (Import tsImport : imports) {

      URL url = tsImport.findAbsoluteUrl(aResourceManager);
      String urlString = url.toString();

      if (visited.contains(urlString)) {
        // cycle detected? -> complete=false, not able to completely resolve imports right now
        completelyResolved &= !stack.contains(urlString);
        continue;
      }
      stack.push(urlString);
      visited.add(urlString);

      TypeSystemDescription_impl importedDescription = getTypeSystemDescription(url,
              aResourceManager);
      boolean importCompletelyCollected = importedDescription
              .collectTypeDescriptions(resolvedTypeDescriptions, stack, visited, aResourceManager);
      if (!importCompletelyCollected) {
        // TODO: update importUrlsCache? here or at all?
        unresolvedImports.add(tsImport);
      }
      completelyResolved &= importCompletelyCollected;
      stack.pop();
    }

    if (imports.length != unresolvedImports.size()) {
      // update own resolved status
      setTypes(resolvedTypeDescriptions.toArray(new TypeDescription_impl[0]));
      setImports(unresolvedImports.toArray(new Import[0]));
    }
    result.addAll(resolvedTypeDescriptions);

    return completelyResolved;
  }

  private TypeSystemDescription_impl getTypeSystemDescription(URL url,
          ResourceManager resourceManager) throws InvalidXMLException {

    String urlString = url.toString();
    // TODO where to synchronize the importCache?
    Map<String, XMLizable> importCache = ((ResourceManager_impl) resourceManager).getImportCache();
    XMLizable cachedObject = importCache.get(urlString);
    if (cachedObject instanceof TypeSystemDescription_impl) {
      return (TypeSystemDescription_impl) cachedObject;
    }
    try {
      XMLInputSource input = new XMLInputSource(url);
      TypeSystemDescription description = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(input);
      importCache.put(urlString, description);
      // TODO cast?
      return (TypeSystemDescription_impl) description;
    } catch (IOException e) {
      throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
              new Object[] { url, urlString }, e);
    }
  }

  private void resolveImport(URL aURL, Collection<String> aAlreadyImportedTypeSystemURLs,
          Collection<TypeDescription> aResults, ResourceManager aResourceManager)
          throws InvalidXMLException, IOException {
    // TODO remove method

    // check the import cache
    TypeSystemDescription desc;
    String urlString = aURL.toString();
    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResourceManager).getImportCache();
    Map<String, Set<String>> importUrlsCache = ((ResourceManager_impl) aResourceManager)
            .getImportUrlsCache();
    synchronized (importCache) {
      XMLizable cachedObject = importCache.get(urlString);
      if (cachedObject instanceof TypeSystemDescription) {
        desc = (TypeSystemDescription) cachedObject;
        // Add the URLs parsed for this cached object to the list already-parsed (UIMA-5058)
        aAlreadyImportedTypeSystemURLs.addAll(importUrlsCache.get(urlString));
      } else {
        XMLInputSource input;
        input = new XMLInputSource(aURL);
        desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(input);
        TreeSet<String> previouslyImported = new TreeSet<>(aAlreadyImportedTypeSystemURLs);
        desc.resolveImports(aAlreadyImportedTypeSystemURLs, aResourceManager);
        importCache.put(urlString, desc);
        // Save the URLS parsed by this import
        TreeSet<String> locallyImported = new TreeSet<>(aAlreadyImportedTypeSystemURLs);
        locallyImported.removeAll(previouslyImported);
        importUrlsCache.put(urlString, locallyImported);
      }

    }
    aResults.addAll(Arrays.asList(desc.getTypes()));
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("types", true) });
}
