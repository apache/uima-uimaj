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

import java.util.Collection;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.InvalidXMLException;

public class FsIndexCollection_impl extends MetaDataObject_impl implements FsIndexCollection {

  private static final long serialVersionUID = -7687383527183197102L;

  private static final FsIndexDescription[] EMPTY_FS_INDEX_DESCRIPTION_ARRAY = new FsIndexDescription[0];

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = Import.EMPTY_IMPORTS;

  private FsIndexDescription[] mFsIndexes = EMPTY_FS_INDEX_DESCRIPTION_ARRAY;

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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#getImports()
   */
  @Override
  public Import[] getImports() {
    // don't allow this to return null
    return (mImports == null) ? Import.EMPTY_IMPORTS : mImports;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.resource.metadata.TypeSystemDescription#setImports(org.apache.uima.resource.
   * metadata.Import[])
   */
  @Override
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
  @Override
  public FsIndexDescription[] getFsIndexes() {
    // don't allow this to return null
    if (mFsIndexes == null) {
      mFsIndexes = EMPTY_FS_INDEX_DESCRIPTION_ARRAY;
    }
    return mFsIndexes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FsIndexCollection#setFsIndexes(org.apache.uima.resource.
   * metadata.FsIndexDescription[])
   */
  @Override
  public void setFsIndexes(FsIndexDescription[] aFSIndexes) {
    if (aFSIndexes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aFSIndexes", "setImports" });
    }
    mFsIndexes = aFSIndexes;
  }

  @Override
  public void addFsIndex(FsIndexDescription aFsIndexDescription) {
    FsIndexDescription[] current = getFsIndexes();
    FsIndexDescription[] newArr = new FsIndexDescription[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aFsIndexDescription;
    setFsIndexes(newArr);
  }

  @Override
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
  // support multi-threading, avoid object creation if no imports
  @Override
  public synchronized void resolveImports() throws InvalidXMLException {
    resolveImports(null, null);
  }

  @Override
  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(null, aResourceManager);
  }

  @Deprecated
  @Override
  public synchronized void resolveImports(Collection<String> aAlreadyImportedFsIndexURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    ImportResolver<FsIndexCollection, FsIndexDescription> resolver = new ImportResolver<>(
            FsIndexCollectionImportResolverAdapter::new);
    resolver.resolveImports(this, aAlreadyImportedFsIndexURLs, aResourceManager);
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fsIndexCollection",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("fsIndexes", "fsIndexes") });

}
