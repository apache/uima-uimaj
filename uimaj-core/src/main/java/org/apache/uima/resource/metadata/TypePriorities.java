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
 * A declaration of priorities between CAS Types. Type priority can be a basis for ordering feature
 * structures within an index - see {@link FsIndexDescription}.
 * <p>
 * This object implements <code>MetaDataObject</code>, which implements
 * {@link org.apache.uima.util.XMLizable}, so it can be serialized to and deserialized from an XML
 * element.
 * <p>
 * TypePriorities can declare that they {@link #getImports() import} other TypePriorities. At
 * runtime, these imports will be resolved to create a single logical TypePriorities object.
 * <p>
 * Note that type priorities imports are not automatically resolved when a TypePriorities object is
 * deserialized from XML. To resolve the imports, call the {@link #resolveImports()} method. Import
 * resolution is done automatically when a CAS is created using a TypePriorities object.
 * <p>
 * TypePriorities declarations can optionally be assigned a {@link #getName() name},
 * {@link #getDescription() description}, {@link #getVendor() vendor}, and
 * {@link #getVersion() version}. It is recommended that these properties be set on any
 * TypePriorities declaration that is meant to be shared by (imported by) multiple components.
 * 
 * 
 */
public interface TypePriorities extends MetaDataObject {

  /**
   * Gets the name of this TypePriorities declaration.
   * 
   * @return the name of this TypePriorities declaration, null if none has been specified.
   */
  public String getName();

  /**
   * Sets the name of this TypePriorities declaration.
   * 
   * @param aName
   *          the name of this TypePriorities declaration
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Gets the version number of this TypePriorities declaration.
   * 
   * @return the version number of this TypePriorities declaration, as a String, null if none has
   *         been specified.
   */
  public String getVersion();

  /**
   * Sets the version number of this TypePriorities declaration.
   * 
   * @param aVersion
   *          the version number of this TypePriorities declaration, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVersion(String aVersion);

  /**
   * Gets the description of this TypePriorities declaration.
   * 
   * @return the description of this TypePriorities declaration, null if none has been specified.
   */
  public String getDescription();

  /**
   * Sets the description of this TypePriorities declaration.
   * 
   * @param aDescription
   *          the description of this TypePriorities declaration
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Gets the vendor of this TypePriorities declaration.
   * 
   * @return the vendor of this TypePriorities declaration, as a String
   */
  public String getVendor();

  /**
   * Sets the vendor of this TypePriorities declaration.
   * 
   * @param aVendor
   *          the vendor of this TypePriorities declaration, as a String, null if none has been
   *          specified.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVendor(String aVendor);

  /**
   * Gets the imports declared by this TypePriorities declaration.
   * 
   * @return an array of imports declared by this TypePriorities declaration.
   */
  public Import[] getImports();

  /**
   * Sets the imports declared by this TypePriorities declaration.
   * 
   * @param aImports
   *          an array of imports declared by this TypePriorities declaration.
   */
  public void setImports(Import[] aImports);

  /**
   * Gets the <code>TypePriorityList</code>s that define the priorities. Each
   * <code>TypePriorityList</code> declares the relative priority of two or more types.
   * 
   * @return the <code>TypePriorityList</code>s that define the priorities
   */
  public TypePriorityList[] getPriorityLists();

  /**
   * Sets the <code>TypePriorityList</code>s that define the priorities. Each
   * <code>TypePriorityList</code> declares the relative priority of two or more types.
   * 
   * @param aPriorityLists
   *          the <code>TypePriorityList</code>s that define the priorities
   */
  public void setPriorityLists(TypePriorityList[] aPriorityLists);

  /**
   * Adds a <code>TypePriorityList</code>.
   * 
   * @param aPriorityList
   *          the <code>TypePriorityList</code> to add
   */
  public void addPriorityList(TypePriorityList aPriorityList);

  /**
   * Creates a new, empty <code>TypePriorityList</code> and adds it to this object.
   * 
   * @return the new <code>TypePriorityList</code>, which can be modified by the caller
   */
  public TypePriorityList addPriorityList();

  /**
   * Removes a <code>TypePriorityList</code>.
   * 
   * @param aPriorityList
   *          the <code>TypePriorityList</code> to remove
   */
  public void removePriorityList(TypePriorityList aPriorityList);

  /**
   * Resolves any import declarations in this Type Priorities declaration, adding the imported
   * {@link TypePriorityList} objects directly onto this TypePriorities object's
   * {@link #getPriorityLists() priorityLists}. The import elements are then deleted, so this
   * results in a structure that is equivalent to the imported elements having been defined locally.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations in this Type Priorities declaration, adding the imported
   * {@link TypePriorityList} objects directly onto this TypePriorities object's
   * {@link #getPriorityLists() priorityLists}. The import elements are then deleted, so this
   * results in a structure that is equivalent to the imported elements having been defined locally.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate type priorities XML files imported by name. For
   *          example, the path in which to locate these type priorities XML files can be set via
   *          the {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Resolves any import declarations in this Type Priorities declaration, adding the imported
   * {@link TypePriorityList} objects directly onto this TypePriorities object's
   * {@link #getPriorityLists() priorityLists}. The import elements are then deleted, so this
   * results in a structure that is equivalent to the imported elements having been defined locally.
   * <p>
   * This version is used internally to resolve nested imports.
   * 
   * @param aAlreadyImportedPriorityListURLs
   *          URLs of already imported type priorities, so we don't import them again.
   * @param aResourceManager
   *          the Resource Manager used to locate type priorities XML files imported by name. For
   *          example, the path in which to locate these type priorities XML files can be set via
   *          the {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(Collection<String> aAlreadyImportedPriorityListURLs,
          ResourceManager aResourceManager) throws InvalidXMLException;
}
