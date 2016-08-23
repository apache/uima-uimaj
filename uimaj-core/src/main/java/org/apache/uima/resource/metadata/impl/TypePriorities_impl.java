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
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link TypePriorities}.
 */
public class TypePriorities_impl extends MetaDataObject_impl implements TypePriorities {

  static final long serialVersionUID = -4773863151055424438L;
  
  private volatile String mName;

  private volatile String mVersion;

  private volatile String mDescription;

  private volatile String mVendor;

  private volatile Import[] mImports = Import.EMPTY_IMPORTS;

  // not final or volatile because clone() copies a ref to shared value, and we need that value to be a new instance
  // Threading: all access synchronized except initial creation during cloning
  private List<TypePriorityList> mPriorityLists = new ArrayList<TypePriorityList>();
  
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
   * @see TypePriorities#getPriorityLists()
   * synchronized to prevent concurrent mod exceptions
   */
  public TypePriorityList[] getPriorityLists() {
    synchronized (mPriorityLists) { // saw concurrent mod exception 3/2014
      TypePriorityList[] result = new TypePriorityList[mPriorityLists.size()];
      mPriorityLists.toArray(result);
      return result;
    }
  }

  /**
   * @see TypePriorities#setPriorityLists(TypePriorityList[])
   * could be called by thread doing resolve imports,
   * while another thread was iterating over them
   */
  public void setPriorityLists(TypePriorityList[] aPriorityLists) {
    synchronized (mPriorityLists) { // saw concurrent mod exceptions 3/2014      
      mPriorityLists.clear();
      for (int i = 0; i < aPriorityLists.length; i++) {
        mPriorityLists.add(aPriorityLists[i]);
      }
    }
  }

  /**
   * @see TypePriorities#addPriorityList(TypePriorityList)
   */
  public void addPriorityList(TypePriorityList aPriorityList) {
    synchronized (mPriorityLists) { // saw concurrent mod exceptions 3/2014 
      mPriorityLists.add(aPriorityList);
    }
  }

  /**
   * @see TypePriorities#addPriorityList()
   */
  public TypePriorityList addPriorityList() {
    TypePriorityList newPriorityList = new TypePriorityList_impl();
    synchronized (mPriorityLists) { // saw concurrent mod exceptions while iterating on this 3/2014
      mPriorityLists.add(newPriorityList);
    }
    return newPriorityList;
  }

  /**
   * @see TypePriorities#removePriorityList(TypePriorityList)
   */
  public void removePriorityList(TypePriorityList aPriorityList) {
    synchronized (mPriorityLists) { // saw concurrent mod exceptions while iterating on this 3/2014
      mPriorityLists.remove(aPriorityList);
    }
  }

  /**
   * @see TypeSystemDescription#resolveImports()
   */
  // support multithreading,
  // avoid object creation if already resolved
  public synchronized void resolveImports() throws InvalidXMLException {
    if (getImports().length == 0) {
      resolveImports(null, null);
    } else {
      resolveImports(new TreeSet<String>(), UIMAFramework.newDefaultResourceManager());
    }
  }

  public synchronized void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports((getImports().length == 0) ? null : new TreeSet<String>(), aResourceManager);
  }

  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypePrioritiesURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    List<TypePriorityList> importedPriorityLists = null;
    if (getImports().length != 0) {
  
      // add our own URL, if known, to the collection of already imported URLs
      if (getSourceUrl() != null) {
        aAlreadyImportedTypePrioritiesURLs.add(getSourceUrl().toString());
      }
      
      importedPriorityLists = new ArrayList<TypePriorityList>();
      Import[] imports = getImports();
      for (int i = 0; i < imports.length; i++) {
        // make sure Import's relative path base is set, to allow for users who create
        // new import objects
        if (imports[i] instanceof Import_impl) {
          ((Import_impl) imports[i]).setSourceUrlIfNull(this.getSourceUrl());
        }
  
        URL url = imports[i].findAbsoluteUrl(aResourceManager);
        if (!aAlreadyImportedTypePrioritiesURLs.contains(url.toString())) {
          aAlreadyImportedTypePrioritiesURLs.add(url.toString());
          try {
            resolveImport(url, aAlreadyImportedTypePrioritiesURLs, importedPriorityLists,
                    aResourceManager);
          } catch (IOException e) {
            throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                    new Object[] { url, imports[i].getSourceUrlString() }, e);
          }
        }
      }
    }
    //update this object
    TypePriorityList[] existingPriorityLists = this.getPriorityLists();
    if (existingPriorityLists == null) {
      this.setPriorityLists(existingPriorityLists = TypePriorityList.EMPTY_TYPE_PRIORITY_LISTS);
    }
    if (importedPriorityLists != null ) {
      TypePriorityList[] newPriorityLists = new TypePriorityList[existingPriorityLists.length
              + importedPriorityLists.size()];
      System.arraycopy(existingPriorityLists, 0, newPriorityLists, 0, existingPriorityLists.length);
      for (int i = 0; i < importedPriorityLists.size(); i++) {
        newPriorityLists[existingPriorityLists.length + i] = importedPriorityLists
                .get(i);
      }
      this.setPriorityLists(newPriorityLists);
    }
    // clear imports
    this.setImports(Import.EMPTY_IMPORTS);
  }

  private void resolveImport(URL aURL, Collection<String> aAlreadyImportedTypePrioritiesURLs,
          Collection<TypePriorityList> aResults, ResourceManager aResourceManager) throws InvalidXMLException,
          IOException {
    //check the import cache
    TypePriorities desc;    
    String urlString = aURL.toString();
    Map<String, XMLizable> importCache = ((ResourceManager_impl)aResourceManager).getImportCache();
    Map<String, Set<String>> importUrlsCache = ((ResourceManager_impl)aResourceManager).getImportUrlsCache();
    synchronized(importCache) {
      XMLizable cachedObject = importCache.get(urlString);
      if (cachedObject instanceof TypePriorities) {
        desc = (TypePriorities)cachedObject;
        // Add the URLs parsed for this cached object to the list already-parsed (UIMA-5058)
        aAlreadyImportedTypePrioritiesURLs.addAll(importUrlsCache.get(urlString));
      } else {   
        XMLInputSource input;
        input = new XMLInputSource(aURL);
        desc = UIMAFramework.getXMLParser().parseTypePriorities(input);
        TreeSet<String> previouslyImported = new TreeSet<String>(aAlreadyImportedTypePrioritiesURLs);
        desc.resolveImports(aAlreadyImportedTypePrioritiesURLs, aResourceManager);
        importCache.put(urlString, desc);
        // Save the URLS parsed by this import 
        TreeSet<String> locallyImported = new TreeSet<String>(aAlreadyImportedTypePrioritiesURLs);
        locallyImported.removeAll(previouslyImported);
        importUrlsCache.put(urlString, locallyImported);
      }
    }
    aResults.addAll(Arrays.asList(desc.getPriorityLists()));
  }

  /**
   * Overridden to supress &lt;priorityLists&gt; tag for TAF compatibility
   * 
   * @see MetaDataObject_impl#writePropertyAsElement(org.apache.uima.resource.metadata.impl.PropertyXmlInfo,
   *      java.lang.String)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    if ("priorityLists".equals(aPropInfo.propertyName)) {
      // call writeArrayPropertyAsElement directly, which will not generate the
      // <priorityLists> tag
      writeArrayPropertyAsElement(aPropInfo.propertyName, TypePriorityList[].class,
              getPriorityLists(), null, aNamespace);
    } else // normal handling
    {
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  /*
   * (non-Javadoc) Special purpose clone method to deal with ArrayList.
   */
  public Object clone() {
    TypePriorities_impl clone = (TypePriorities_impl) super.clone();
    clone.mPriorityLists = new ArrayList<TypePriorityList>();
    final List<TypePriorityList> origPriorityLists = mPriorityLists;
    synchronized (origPriorityLists) { // saw concurrent mod exceptions while iterating on this 3/2014
      for (TypePriorityList priList : origPriorityLists) {
        clone.addPriorityList((TypePriorityList) priList.clone());
      }
    }
    return clone;
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("typePriorities", new PropertyXmlInfo[] {
        new PropertyXmlInfo("name", true), new PropertyXmlInfo("description", true),
        new PropertyXmlInfo("version", true), new PropertyXmlInfo("vendor", true),
        new PropertyXmlInfo("imports", true), new PropertyXmlInfo("priorityLists", true) });
  }
}
