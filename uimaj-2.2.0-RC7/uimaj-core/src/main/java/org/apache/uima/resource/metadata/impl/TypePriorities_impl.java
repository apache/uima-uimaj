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
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link TypePriorities}.
 * 
 * 
 */
public class TypePriorities_impl extends MetaDataObject_impl implements TypePriorities {

  static final long serialVersionUID = -4773863151055424438L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = new Import[0];

  private ArrayList mPriorityLists = new ArrayList();

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setVendor(String)
   */
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#getImports()
   */
  public Import[] getImports() {
    return mImports;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#setImports(org.apache.uima.resource.metadata.Import[])
   */
  public void setImports(Import[] aImports) {
    if (aImports == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aImports", "setImports" });
    }
    mImports = aImports;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypePriorities#getPriorityLists()
   */
  public TypePriorityList[] getPriorityLists() {
    TypePriorityList[] result = new TypePriorityList[mPriorityLists.size()];
    mPriorityLists.toArray(result);
    return result;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypePriorities#setPriorityLists(org.apache.uima.analysis_engine.metadata.TypePriorityList[])
   */
  public void setPriorityLists(TypePriorityList[] aPriorityLists) {
    mPriorityLists.clear();
    for (int i = 0; i < aPriorityLists.length; i++) {
      mPriorityLists.add(aPriorityLists[i]);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypePriorities#addPriorityList(org.apache.uima.analysis_engine.metadata.TypePriorityList)
   */
  public void addPriorityList(TypePriorityList aPriorityList) {
    mPriorityLists.add(aPriorityList);
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypePriorities#addPriorityList()
   */
  public TypePriorityList addPriorityList() {
    TypePriorityList newPriorityList = new TypePriorityList_impl();
    mPriorityLists.add(newPriorityList);
    return newPriorityList;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypePriorities#removePriorityList(org.apache.uima.analysis_engine.metadata.TypePriorityList)
   */
  public void removePriorityList(TypePriorityList aPriorityList) {
    mPriorityLists.remove(aPriorityList);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#resolveImports()
   */
  public void resolveImports() throws InvalidXMLException {
    resolveImports(new TreeSet(), UIMAFramework.newDefaultResourceManager());
  }

  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports(new TreeSet(), aResourceManager);
  }

  public void resolveImports(Collection aAlreadyImportedTypePrioritiesURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    // add our own URL, if known, to the collection of already imported URLs
    if (getSourceUrl() != null) {
      aAlreadyImportedTypePrioritiesURLs.add(getSourceUrl().toString());
    }
    
    List importedPriorityLists = new ArrayList();
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
    // update this object
    TypePriorityList[] existingPriorityLists = this.getPriorityLists();
    if (existingPriorityLists == null) {
      existingPriorityLists = new TypePriorityList[0];
    }
    TypePriorityList[] newPriorityLists = new TypePriorityList[existingPriorityLists.length
            + importedPriorityLists.size()];
    System.arraycopy(existingPriorityLists, 0, newPriorityLists, 0, existingPriorityLists.length);
    for (int i = 0; i < importedPriorityLists.size(); i++) {
      newPriorityLists[existingPriorityLists.length + i] = (TypePriorityList) importedPriorityLists
              .get(i);
    }
    this.setPriorityLists(newPriorityLists);
    // clear imports
    this.setImports(new Import[0]);
  }

  private void resolveImport(URL aURL, Collection aAlreadyImportedTypePrioritiesURLs,
          Collection aResults, ResourceManager aResourceManager) throws InvalidXMLException,
          IOException {
    XMLInputSource input = new XMLInputSource(aURL);
    TypePriorities desc = UIMAFramework.getXMLParser().parseTypePriorities(input);
    desc.resolveImports(aAlreadyImportedTypePrioritiesURLs, aResourceManager);
    aResults.addAll(Arrays.asList(desc.getPriorityLists()));
  }

  /*
   * (non-Javadoc) Overridden to supress <priorityLists> tag for TAF compatibility
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#writePropertyAsElement(org.apache.uima.resource.metadata.impl.PropertyXmlInfo,
   *      java.lang.String, org.xml.sax.ContentHandler)
   */
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace,
          ContentHandler aContentHandler) throws SAXException {
    if ("priorityLists".equals(aPropInfo.propertyName)) {
      // call writeArrayPropertyAsElement directly, which will not generate the
      // <priorityLists> tag
      writeArrayPropertyAsElement(aPropInfo.propertyName, TypePriorityList[].class,
              getPriorityLists(), null, aNamespace, aContentHandler);
    } else // normal handling
    {
      super.writePropertyAsElement(aPropInfo, aNamespace, aContentHandler);
    }
  }

  /*
   * (non-Javadoc) Special purpose clone method to deal with ArrayList.
   */
  public Object clone() {
    TypePriorities_impl clone = (TypePriorities_impl) super.clone();
    clone.mPriorityLists = new ArrayList();
    Iterator priListIter = mPriorityLists.iterator();
    while (priListIter.hasNext()) {
      TypePriorityList priList = (TypePriorityList) priListIter.next();
      clone.addPriorityList((TypePriorityList) priList.clone());
    }

    return clone;
  }

  /**
   * @see org.apache.uima.resource.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("typePriorities", new PropertyXmlInfo[] {
        new PropertyXmlInfo("name", true), new PropertyXmlInfo("description", true),
        new PropertyXmlInfo("version", true), new PropertyXmlInfo("vendor", true),
        new PropertyXmlInfo("imports", true), new PropertyXmlInfo("priorityLists", true) });
  }
}
