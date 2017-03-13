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
 * Description of a CAS TypeSystem. This implements <code>MetaDataObject</code>, which implements
 * {@link org.apache.uima.util.XMLizable}, so it can be serialized to and deserialized from an XML
 * element.
 * <p>
 * Type systems can declare that they {@link #getImports() import} other type systems. At runtime,
 * these imports will be resolved to create a single logical type system.
 * <p>
 * Note that type system imports are not automatically resolved when a TypeSytemDescription is
 * deserialized from XML. To resolve the imports, call the {@link #resolveImports()} method. Import
 * resolution is done automatically when a CAS is created using a TypeSystemDescription.
 * <p>
 * Type systems can optionally be assigned a {@link #getName() name},
 * {@link #getDescription() description}, {@link #getVendor() vendor}, and
 * {@link #getVersion() version}. It is recommended that these properties be set on any type system
 * that is meant to be shared by (imported by) multiple components.
 * 
 * 
 */
public interface TypeSystemDescription extends MetaDataObject {

  public final static TypeSystemDescription[] EMPTY_TYPE_SYSTEM_DESCRIPTIONS = new TypeSystemDescription[0];
  /**
   * Gets the name of this Type System.
   * 
   * @return the name of this Type System, null if none has been specified.
   */
  public String getName();

  /**
   * Sets the name of this Type System.
   * 
   * @param aName
   *          the name of this Type System
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Gets the version number of this Type System.
   * 
   * @return the version number of this Type System, as a String, null if none has been specified.
   */
  public String getVersion();

  /**
   * Sets the version number of this Type System.
   * 
   * @param aVersion
   *          the version number of this Type System, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVersion(String aVersion);

  /**
   * Gets the description of this Type System.
   * 
   * @return the description of this Type System, null if none has been specified.
   */
  public String getDescription();

  /**
   * Sets the description of this Type System.
   * 
   * @param aDescription
   *          the description of this Type System
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Gets the vendor of this Type System.
   * 
   * @return the vendor of this Type System, as a String
   */
  public String getVendor();

  /**
   * Sets the vendor of this Type System.
   * 
   * @param aVendor
   *          the vendor of this Type System, as a String, null if none has been specified.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVendor(String aVendor);

  /**
   * Gets the imports declared by this Type System.
   * 
   * @return an array of imports declared by this type system.
   */
  public Import[] getImports();

  /**
   * Sets the imports declared by this Type System.
   * 
   * @param aImports
   *          an array of imports declared by this type system.
   */
  public void setImports(Import[] aImports);

  /**
   * Gets descriptions of all Types in this TypeSystem fragment.
   * 
   * @return descriptions of all Types in this TypeSystem fragment
   */
  public TypeDescription[] getTypes();

  /**
   * Sets the descriptions of all Types in this TypeSystem fragment.
   * 
   * @param aTypes
   *          descriptions of all Types in this TypeSystem fragment
   */
  public void setTypes(TypeDescription[] aTypes);

  /**
   * Adds a Type to this TypeSystem fragment.
   * 
   * @param aTypeName
   *          name of Type to add
   * @param aDescription
   *          verbose description of this Type
   * @param aSupertypeName
   *          name of supertype for the new Type
   * 
   * @return description of the new Type
   */
  public TypeDescription addType(String aTypeName, String aDescription, String aSupertypeName);

  /**
   * Retrieves a Type from this TypeSystem fragment.
   * 
   * @param aTypeName
   *          name of Type to retrieve
   * 
   * @return the type with the specified name, <code>null</code> if no such type exists
   */
  public TypeDescription getType(String aTypeName);

  /**
   * Resolves any import declarations in this type system, adding the imported types directly onto
   * this TypeSystemDescription's {@link #getTypes() types} list. The import elements are then
   * deleted, so this results in a structure that is equivalent to the imported elements having been
   * defined locally.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations in this type system, adding the imported types directly onto
   * this TypeSystemDescription's {@link #getTypes() types} list. The import elements are then
   * deleted, so this results in a structure that is equivalent to the imported elements having been
   * defined locally.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate type systems imported by name. For example, the
   *          path in which to locate these type systems can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Resolves any import declarations in this type system, adding the imported types directly onto
   * this TypeSystemDescription's {@link #getTypes() types} list. The import elements are then
   * deleted, so this results in a structure that is equivalent to the imported elements having been
   * defined locally.
   * <p>
   * This version is used internally to resolve nested imports.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate type systems imported by name. For example, the
   *          path in which to locate these type systems can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * @param aAlreadyImportedTypeSystemURLs
   *          URLs of already imported type systems, so we don't import them again.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException;
}
