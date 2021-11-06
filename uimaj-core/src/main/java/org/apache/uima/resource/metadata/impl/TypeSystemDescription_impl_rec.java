///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * 
// *   http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.uima.resource.metadata.impl;
//
//import static java.util.Arrays.asList;
//import static org.apache.uima.UIMAFramework.getXMLParser;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeSet;
//
//import org.apache.uima.UIMAFramework;
//import org.apache.uima.UIMA_IllegalArgumentException;
//import org.apache.uima.resource.ResourceManager;
//import org.apache.uima.resource.impl.ResourceManager_impl;
//import org.apache.uima.resource.metadata.Import;
//import org.apache.uima.resource.metadata.ResourceMetaData;
//import org.apache.uima.resource.metadata.TypeDescription;
//import org.apache.uima.resource.metadata.TypeSystemDescription;
//import org.apache.uima.util.InvalidXMLException;
//import org.apache.uima.util.XMLInputSource;
//import org.apache.uima.util.XMLizable;
//
///**
// * Reference implementation of {@link TypeSystemDescription}.
// * 
// * 
// */
//public class TypeSystemDescription_impl extends MetaDataObject_impl
//        implements TypeSystemDescription {
//
//  static final long serialVersionUID = -3372766232454730201L;
//
//  private String mName;
//
//  private String mVersion;
//
//  private String mDescription;
//
//  private String mVendor;
//
//  private Import[] mImports = Import.EMPTY_IMPORTS;
//
//  /** Descriptions of all Types in this type system. */
//  private TypeDescription[] mTypes = TypeDescription.EMPTY_TYPE_DESCRIPTIONS;
//
//  /**
//   * Creates a new TypeSystemDescription_impl.
//   */
//  public TypeSystemDescription_impl() {
//  }
//
//  /**
//   * @see ResourceMetaData#getName()
//   */
//  @Override
//  public String getName() {
//    return mName;
//  }
//
//  /**
//   * @see ResourceMetaData#setName(String)
//   */
//  @Override
//  public void setName(String aName) {
//    mName = aName;
//  }
//
//  /**
//   * @see ResourceMetaData#getVersion()
//   */
//  @Override
//  public String getVersion() {
//    return mVersion;
//  }
//
//  /**
//   * @see ResourceMetaData#setVersion(String)
//   */
//  @Override
//  public void setVersion(String aVersion) {
//    mVersion = aVersion;
//  }
//
//  /**
//   * @see ResourceMetaData#getDescription()
//   */
//  @Override
//  public String getDescription() {
//    return mDescription;
//  }
//
//  /**
//   * @see ResourceMetaData#setDescription(String)
//   */
//  @Override
//  public void setDescription(String aDescription) {
//    mDescription = aDescription;
//  }
//
//  /**
//   * @see ResourceMetaData#getVendor()
//   */
//  @Override
//  public String getVendor() {
//    return mVendor;
//  }
//
//  /**
//   * @see ResourceMetaData#setVendor(String)
//   */
//  @Override
//  public void setVendor(String aVendor) {
//    mVendor = aVendor;
//  }
//
//  /**
//   * @see TypeSystemDescription#getImports()
//   */
//  @Override
//  public Import[] getImports() {
//    return mImports;
//  }
//
//  /**
//   * @see TypeSystemDescription#setImports(Import[])
//   */
//  @Override
//  public void setImports(Import[] aImports) {
//    if (aImports == null) {
//      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
//              new Object[] { "null", "aImports", "setImports" });
//    }
//    mImports = aImports;
//  }
//
//  /**
//   * @see TypeSystemDescription#getTypes()
//   */
//  @Override
//  public TypeDescription[] getTypes() {
//    return mTypes;
//  }
//
//  /**
//   * @see TypeSystemDescription#setTypes(TypeDescription[])
//   */
//  @Override
//  public void setTypes(TypeDescription[] aTypes) {
//    if (aTypes == null) {
//      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
//              new Object[] { "null", "aTypes", "setTypes" });
//    }
//    mTypes = aTypes;
//  }
//
//  /**
//   * @see TypeSystemDescription#addType(String, String, String)
//   */
//  @Override
//  public TypeDescription addType(String aTypeName, String aDescription, String aSupertypeName) {
//    // create new type description
//    TypeDescription newType = new TypeDescription_impl(aTypeName, aDescription, aSupertypeName);
//
//    // add to array
//    TypeDescription[] types = getTypes();
//    if (types == null) {
//      setTypes(new TypeDescription[] { newType });
//    } else {
//      TypeDescription[] newArray = new TypeDescription[types.length + 1];
//      System.arraycopy(types, 0, newArray, 0, types.length);
//      newArray[types.length] = newType;
//      setTypes(newArray);
//    }
//
//    return newType;
//  }
//
//  /**
//   * @see TypeSystemDescription#getType(java.lang.String)
//   */
//  @Override
//  public TypeDescription getType(String aTypeName) {
//    for (int i = 0; i < mTypes.length; i++) {
//      if (aTypeName.equals(mTypes[i].getName())) {
//        return mTypes[i];
//      }
//    }
//    return null;
//  }
//
//  /**
//   * @see TypeSystemDescription#resolveImports()
//   */
//  // allow these calls to be done multiple times on this same object, in different threads
//  @Override
//  public synchronized void resolveImports() throws InvalidXMLException {
//    resolveImports(null, UIMAFramework.newDefaultResourceManager());
//  }
//
//  @Override
//  public synchronized void resolveImports(ResourceManager aResourceManager)
//          throws InvalidXMLException {
//    resolveImports(null, aResourceManager);
//  }
//
//  @Override
//  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
//          ResourceManager aResMgr) throws InvalidXMLException {
//
//    if (getImports().length == 0) {
//      this.setImports(Import.EMPTY_IMPORTS);
//    }
//
//    List<String> stack = new ArrayList<>();
//    Set<String> seen = new TreeSet<>();
//    if (aAlreadyImportedTypeSystemURLs != null) {
//      seen.addAll(aAlreadyImportedTypeSystemURLs);
//    }
//
//    // add our own URL, if known, to the collection of already imported URLs
//    String url = getSourceUrl() != null ? getSourceUrl().toString() : "<TOP>";
//    seen.add(url);
//    stack.add(url);
//
//    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResMgr).getImportCache();
//    synchronized (importCache) {
//      _resolveImports(url, this, stack, seen, aResMgr);
//    }
//  }
//
//  /**
//   * @param aUrl
//   *          URL of the currently processed description
//   * @param aStack
//   *          current traversal stack. This is used to discover <b>at which point</b> we entered
//   *          into a loop.
//   * @param aSeen
//   *          all URLs that were already processed. This is used to see <b>if</b> we enter into any
//   *          kind of loop and to discover the transitive imports of a given import.
//   * @param aResourceManager
//   *          the resource manager used to resolve imports.
//   * @return the stack depth of the item in the stack which triggered the current loop or
//   *         {@code -1}.
//   * @throws InvalidXMLException
//   *           if an import could not be resolved.
//   */
//  private static Loop _resolveImports(String aUrl, TypeSystemDescription_impl aDesc,
//          List<String> aStack, Collection<String> aSeen,
//          ResourceManager aResourceManager) throws InvalidXMLException {
//
//    Map<String, XMLizable> importCache = ((ResourceManager_impl) aResourceManager).getImportCache();
//    Map<String, Set<String>> transitiveImportCache = ((ResourceManager_impl) aResourceManager)
//            .getImportUrlsCache();
//
//    List<TypeDescription> importedTypes = new ArrayList<>();
//    Loop loop = null;
//    for (Import imp : aDesc.getImports()) {
//      // ensure Import's relative path base is set, to allow for users who create new import objects
//      if (imp instanceof Import_impl) {
//        ((Import_impl) imp).setSourceUrlIfNull(aDesc.getSourceUrl());
//      }
//
//      URL impUrl = imp.findAbsoluteUrl(aResourceManager);
//      String sImpUrl = impUrl.toString();
//      if (aStack.contains(sImpUrl)) {
//        loop = loop == null ? new Loop(sImpUrl, aStack) : loop.extend(sImpUrl, aStack, loop);
//        continue;
//      }
//
//      aSeen.add(sImpUrl);
//      aStack.add(sImpUrl);
//
//      // check the import cache
//      String urlString = sImpUrl;
//      XMLizable cachedObject = importCache.get(urlString);
//      if (cachedObject instanceof TypeSystemDescription) {
//        TypeSystemDescription desc = (TypeSystemDescription) cachedObject;
//        // Add the URLs parsed for this cached object to the list already-parsed (UIMA-5058)
//        Set<String> transitiveImports = transitiveImportCache.get(urlString);
//        assert transitiveImports != null : "TSD found in cache but no cached transitive imports info";
//        importedTypes.addAll(asList(desc.getTypes()));
//
//        for (String tImp : transitiveImports) {
//          if (aSeen.contains(tImp)) {
//            loop = loop == null ? new Loop(tImp, aStack) : loop.extend(tImp, aStack, loop);
//          } else {
//            aSeen.add(tImp);
//          }
//        }
//      } else {
//        // Make a snapshot of the URLs we have "seen" until now
//        TreeSet<String> seenBeforeResolvingImport = new TreeSet<>(aSeen);
//
//        // Resolve the current import - this adds new URLs to the "seen" list
//        TypeSystemDescription_impl desc = loadImportedTypeSystem(imp, impUrl);
//        Loop nestedLoop = _resolveImports(urlString, desc, aStack, aSeen, aResourceManager);
//        importCache.put(urlString, desc);
//
//        // Calculate which URLs were newly added by the above resolving and add them to the
//        // transitive import cache
//        TreeSet<String> transitiveImports = new TreeSet<>(aSeen);
//        transitiveImports.removeAll(seenBeforeResolvingImport);
//        transitiveImportCache.put(urlString, transitiveImports);
//        importedTypes.addAll(asList(desc.getTypes()));
//
//        if (nestedLoop != null) {
//          for (String tImp : nestedLoop.members) {
//            if (aStack.contains(tImp)) {
//              loop = loop == null ? new Loop(tImp, aStack) : loop.extend(tImp, aStack, loop);
//            } else {
//              aSeen.add(tImp);
//            }
//          }
//        }
//      }
//
//      aStack.remove(aStack.size() - 1);
//    }
//
//    aDesc.addTypes(importedTypes);
//
//    // On the way back up here, we need to account for circular imports. So if we skipped
//    // resolving imports because the URL of the descriptor was in the "already imported" list,
//    // then we need to check if the descriptor imports any of the already imported descriptors
//    // and if so update its type list with the types from that descriptor...
//    // See: https://issues.apache.org/jira/browse/UIMA-6393
//    if (loop != null) {
//      int depth = aStack.indexOf(aUrl);
//      assert depth != -1 : "Current type system description not found on stack during import traversal";
//      if (depth == loop.entryPoint) {
//        importCache.put(aUrl, aDesc);
//        Set<String> transitiveImports = new TreeSet<>();
//        for (String member : loop.members) {
//          TypeSystemDescription tsd = (TypeSystemDescription) importCache.get(member);
//          tsd.setTypes(aDesc.getTypes());
//          Set<String> memberTransitiveImports = transitiveImportCache.get(member);
//          if (memberTransitiveImports != null) {
//            transitiveImports.addAll(memberTransitiveImports);
//          }
//        }
//        transitiveImportCache.put(aUrl, transitiveImports);
//        loop = null;
//      }
//    }
//
//    // clear imports
//    aDesc.setImports(Import.EMPTY_IMPORTS);
//
//    return loop;
//  }
//
//  private static TypeSystemDescription_impl loadImportedTypeSystem(Import imp, URL impUrl)
//          throws InvalidXMLException {
//    try {
//      return (TypeSystemDescription_impl) getXMLParser()
//              .parseTypeSystemDescription(new XMLInputSource(impUrl));
//    } catch (IOException e) {
//      throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
//              new Object[] { impUrl, imp.getSourceUrlString() }, e);
//    }
//
//  }
//
//  private static class Loop {
//    final int entryPoint;
//    final String[] members;
//
//    /**
//     * Create new loop.
//     */
//    Loop(String aUrl, List<String> aStack) {
//      entryPoint = aStack.indexOf(aUrl);
//      members = aStack.subList(entryPoint, aStack.size()).stream().toArray(String[]::new);
//    }
//
//    private Loop(int aEntryPoint, String aUrl, Loop aOtherLoop) {
//      entryPoint = Math.min(aEntryPoint, aOtherLoop.entryPoint);
//      members = new String[aOtherLoop.members.length + 1];
//      members[0] = aUrl;
//      System.arraycopy(aOtherLoop.members, 0, members, 1, aOtherLoop.members.length);
//    }
//
//    Loop extend(String aUrl, List<String> aStack, Loop aOtherLoop) {
//      if (asList(aOtherLoop.members).indexOf(aUrl) != -1) {
//        return aOtherLoop;
//      }
//
//      return new Loop(aStack.indexOf(aUrl), aUrl, aOtherLoop);
//    }
//  }
//
//  private void addTypes(Collection<TypeDescription> aTypes) {
//    if (aTypes == null || aTypes.isEmpty()) {
//      return;
//    }
//
//    TypeDescription[] existingTypes = this.getTypes();
//    if (existingTypes == null) {
//      this.setTypes(existingTypes = TypeDescription.EMPTY_TYPE_DESCRIPTIONS);
//    }
//
//    TypeDescription[] newTypes = new TypeDescription[existingTypes.length + aTypes.size()];
//    System.arraycopy(existingTypes, 0, newTypes, 0, existingTypes.length);
//    int i = 0;
//    for (TypeDescription d : aTypes) {
//      newTypes[existingTypes.length + i] = d;
//      i++;
//    }
//    this.setTypes(newTypes);
//  }
//
//  @Override
//  protected XmlizationInfo getXmlizationInfo() {
//    return XMLIZATION_INFO;
//  }
//
//  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
//          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
//              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
//              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
//              new PropertyXmlInfo("types", true) });
//}
