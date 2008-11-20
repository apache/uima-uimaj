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
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;

/**
 * 
 * 
 */
public class FsIndexCollection_impl extends MetaDataObject_impl implements FsIndexCollection {

  private static final long serialVersionUID = -7687383527183197102L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports;

  private FsIndexDescription[] mFsIndexes = new FsIndexDescription[0];

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
    // don't all this to return null
    if (mImports == null)
      mImports = new Import[0];
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FsIndexCollection#getFsIndexes()
   */
  public FsIndexDescription[] getFsIndexes() {
    // don't allow this to return null
    if (mFsIndexes == null)
      mFsIndexes = new FsIndexDescription[0];
    return mFsIndexes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FsIndexCollection#setFsIndexes(org.apache.uima.resource.metadata.FsIndexDescription[])
   */
  public void setFsIndexes(FsIndexDescription[] aFSIndexes) {
    if (aFSIndexes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aFSIndexes", "setImports" });
    }
    mFsIndexes = aFSIndexes;
  }

  public void addFsIndex(FsIndexDescription aFsIndexDescription) {
    FsIndexDescription[] current = getFsIndexes();
    FsIndexDescription[] newArr = new FsIndexDescription[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aFsIndexDescription;
    setFsIndexes(newArr);
  }

  public void removeFsIndex(FsIndexDescription aFsIndexDescription) {
    FsIndexDescription[] current = getFsIndexes();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aFsIndexDescription) {
        FsIndexDescription[] newArr = new FsIndexDescription[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setFsIndexes(newArr);
        break;
      }
    }
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

  public void resolveImports(Collection aAlreadyImportedFsIndexURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    // add our own URL, if known, to the collection of already imported URLs
    if (getSourceUrl() != null) {
      aAlreadyImportedFsIndexURLs.add(getSourceUrl().toString());
    }
    
    List importedIndexes = new ArrayList();
    Import[] imports = getImports();
    for (int i = 0; i < imports.length; i++) {
      // make sure Import's relative path base is set, to allow for users who create
      // new import objects
      if (imports[i] instanceof Import_impl) {
        ((Import_impl) imports[i]).setSourceUrlIfNull(this.getSourceUrl());
      }

      URL url = imports[i].findAbsoluteUrl(aResourceManager);
      if (!aAlreadyImportedFsIndexURLs.contains(url.toString())) {
        aAlreadyImportedFsIndexURLs.add(url.toString());
        try {
          resolveImport(url, aAlreadyImportedFsIndexURLs, importedIndexes, aResourceManager);
        } catch (IOException e) {
          throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                  new Object[] { url, imports[i].getSourceUrlString() }, e);
        }
      }
    }
    // update this object
    FsIndexDescription[] existingIndexes = this.getFsIndexes();
    if (existingIndexes == null) {
      existingIndexes = new FsIndexDescription[0];
    }
    FsIndexDescription[] newIndexes = new FsIndexDescription[existingIndexes.length
            + importedIndexes.size()];
    System.arraycopy(existingIndexes, 0, newIndexes, 0, existingIndexes.length);
    for (int i = 0; i < importedIndexes.size(); i++) {
      newIndexes[existingIndexes.length + i] = (FsIndexDescription) importedIndexes.get(i);
    }
    this.setFsIndexes(newIndexes);
    // clear imports
    this.setImports(new Import[0]);
  }

  private void resolveImport(URL aURL, Collection aAlreadyImportedFsIndexCollectionURLs,
          Collection aResults, ResourceManager aResourceManager) throws InvalidXMLException,
          IOException {
    //check the import cache
    FsIndexCollection desc;    
    String urlString = aURL.toString();
    XMLizable cachedObject = aResourceManager.getImportCache().get(urlString);
    if (cachedObject instanceof FsIndexCollection) {
      desc = (FsIndexCollection)cachedObject;
    } else {   
      XMLInputSource input;
      input = new XMLInputSource(aURL);
      desc = UIMAFramework.getXMLParser().parseFsIndexCollection(input);
      desc.resolveImports(aAlreadyImportedFsIndexCollectionURLs, aResourceManager);
      aResourceManager.getImportCache().put(urlString, desc);
    }
    aResults.addAll(Arrays.asList(desc.getFsIndexes()));
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fsIndexCollection",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("fsIndexes", "fsIndexes") });

}
