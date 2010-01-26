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

package org.apache.uima.resource.metadata;

import java.util.Collection;

import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.InvalidXMLException;

/**
 * Description of a collection of CAS feature structure indexes. This implements
 * <code>MetaDataObject</code>, which implements {@link org.apache.uima.util.XMLizable}, so it
 * can be serialized to and deserialized from an XML element.
 * <p>
 * FS Index Collections can declare that they {@link #getImports() import} other FS Index
 * Collections. At runtime, these imports will be resolved to create a single logical FS Index
 * Collection.
 * <p>
 * Note that imports are not automatically resolved when an FsIndexCollection is deserialized from
 * XML. To resolve the imports, call the {@link #resolveImports()} method. Import resolution is done
 * automatically when a CAS is created using a FsIndexCollection.
 * <p>
 * FS Index Collections can optionally be assigned a {@link #getName() name},
 * {@link #getDescription() description}, {@link #getVendor() vendor}, and
 * {@link #getVersion() version}. It is recommended that these properties be set on any FS Index
 * Collection that is meant to be shared by (imported by) multiple components.
 * 
 * 
 */
public interface FsIndexCollection extends MetaDataObject {

  /**
   * Gets the name of this FS Index Collection.
   * 
   * @return the name of this FS Index Collection, null if none has been specified.
   */
  public String getName();

  /**
   * Sets the name of this FS Index Collection.
   * 
   * @param aName
   *          the name of this FS Index Collection
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Gets the version number of this FS Index Collection.
   * 
   * @return the version number of this FS Index Collection, as a String, null if none has been
   *         specified.
   */
  public String getVersion();

  /**
   * Sets the version number of this FS Index Collection.
   * 
   * @param aVersion
   *          the version number of this FS Index Collection, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVersion(String aVersion);

  /**
   * Gets the description of this FS Index Collection.
   * 
   * @return the description of this FS Index Collection, null if none has been specified.
   */
  public String getDescription();

  /**
   * Sets the description of this FS Index Collection.
   * 
   * @param aDescription
   *          the description of this FS Index Collection
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Gets the vendor of this FS Index Collection.
   * 
   * @return the vendor of this FS Index Collection, as a String
   */
  public String getVendor();

  /**
   * Sets the vendor of this FS Index Collection.
   * 
   * @param aVendor
   *          the vendor of this FS Index Collection, as a String, null if none has been specified.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVendor(String aVendor);

  /**
   * Gets the imports declared by this FS Index Collection.
   * 
   * @return an array of imports declared by this FS Index Collection.
   */
  public Import[] getImports();

  /**
   * Sets the imports declared by this FS Index Collection.
   * 
   * @param aImports
   *          an array of imports declared by this FS Index Collection.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setImports(Import[] aImports);

  /**
   * Retrieves the FS Index descriptions that are part of this collection. These define the indexes
   * that are used to iterate over annotations in the {@link org.apache.uima.cas.CAS}.
   * 
   * @return a description of the FS Indexes that comprise this FS Index Collection
   */
  public FsIndexDescription[] getFsIndexes();

  /**
   * Retrieves the FS Index descriptions that are part of this collection. These define the indexes
   * that are used to iterate over annotations in the {@link org.apache.uima.cas.CAS}.
   * 
   * @param aFSIndexes
   *          a description of the FS Indexes that comprise this FS Index Collection
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setFsIndexes(FsIndexDescription[] aFSIndexes);

  /**
   * Adds an FS Index description to this collection.
   * 
   * @param aFsIndexDescription
   *          the FS Index description to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addFsIndex(FsIndexDescription aFsIndexDescription);

  /**
   * Removes an FS Index description from this collection.
   * 
   * @param aFsIndexDescription
   *          the FS Index description to remove (must be == with an FsIndexDescription in this
   *          collection, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeFsIndex(FsIndexDescription aFsIndexDescription);

  /**
   * Resolves any import declarations in this FS Index Collection, adding the imported
   * FsIndexDescriptions directly onto this FsIndexCollection's {@link #getFsIndexes() fsIndexes}
   * list. The import elements are then deleted, so this results in a structure that is equivalent
   * to the imported elements having been defined locally.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations in this FS Index Collection, adding the imported
   * FsIndexDescriptions directly onto this FsIndexCollection's {@link #getFsIndexes() fsIndexes}
   * list. The import elements are then deleted, so this results in a structure that is equivalent
   * to the imported elements having been defined locally.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate FS Index Collections imported by name. For
   *          example, the path in which to locate these FS Index Collections can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Resolves any import declarations in this FS Index Collection, adding the imported
   * FsIndexDescriptions directly onto this FsIndexCollection's {@link #getFsIndexes() fsIndexes}
   * list. The import elements are then deleted, so this results in a structure that is equivalent
   * to the imported elements having been defined locally.
   * <p>
   * This version is used internally to resolve nested imports.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate FS Index Collections imported by name. For
   *          example, the path in which to locate these FS Index Collections can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * @param aAlreadyImportedFsIndexCollectionURLs
   *          URLs of already imported FS Index Collections, so we don't import them again.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(Collection<String> aAlreadyImportedFsIndexCollectionURLs,
          ResourceManager aResourceManager) throws InvalidXMLException;
}
