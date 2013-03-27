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
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.InvalidXMLException;

/**
 * Defines a set of external resources and their bindings to
 * {@link org.apache.uima.resource.ExternalResourceDependency ExternalResourceDependencies}.
 * <p>
 * Resource Manager Configurations can declare that they {@link #getImports() import} other Resource
 * Manager Configurations. At runtime, these imports will be resolved to create a single logical
 * Resource Manager Configuration..
 * <p>
 * Note that type system imports are not automatically resolved when a ResourceMangerConfiguration
 * is deserialized from XML. To resolve the imports, call the {@link #resolveImports()} method.
 * Import resolution is done automatically when the {@link org.apache.uima.resource.ResourceManager}
 * is initialized (e.g. during AnalysisEngine initialization).
 * <p>
 * Resource Manager Configurations can optionally be assigned a {@link #getName() name},
 * {@link #getDescription() description}, {@link #getVendor() vendor}, and
 * {@link #getVersion() version}. It is recommended that these properties be set on any Resource
 * Manager Configuration that is meant to be shared by (imported by) multiple components.
 */
public interface ResourceManagerConfiguration extends MetaDataObject {
  /**
   * Gets the name of this Resource Manager Configuration.
   * 
   * @return the name of this Resource Manager Configuration, null if none has been specified.
   */
  public String getName();

  /**
   * Sets the name of this Resource Manager Configuration.
   * 
   * @param aName
   *          the name of this Resource Manager Configuration
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Gets the version number of this Resource Manager Configuration.
   * 
   * @return the version number of this Resource Manager Configuration, as a String, null if none
   *         has been specified.
   */
  public String getVersion();

  /**
   * Sets the version number of this Resource Manager Configuration.
   * 
   * @param aVersion
   *          the version number of this Resource Manager Configuration, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVersion(String aVersion);

  /**
   * Gets the description of this Resource Manager Configuration.
   * 
   * @return the description of this Resource Manager Configuration, null if none has been
   *         specified.
   */
  public String getDescription();

  /**
   * Sets the description of this Resource Manager Configuration.
   * 
   * @param aDescription
   *          the description of this Resource Manager Configuration
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Gets the vendor of this Resource Manager Configuration.
   * 
   * @return the vendor of this Resource Manager Configuration, as a String
   */
  public String getVendor();

  /**
   * Sets the vendor of this Resource Manager Configuration.
   * 
   * @param aVendor
   *          the vendor of this Resource Manager Configuration, as a String, null if none has been
   *          specified.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVendor(String aVendor);

  /**
   * Gets the imports declared by this Resource Manager Configuration.
   * 
   * @return an array of imports declared by this Resource Manager Configuration.
   */
  public Import[] getImports();

  /**
   * Sets the imports declared by this Resource Manager Configuration.
   * 
   * @param aImports
   *          an array of imports declared by this Resource Manager Configuration.
   */
  public void setImports(Import[] aImports);

  /**
   * Gets the import declared by this Resource Manager Configuration, if any.
   * 
   * @return an object that defines how to locate an external XML file defining the resource manager
   *         configuration. Returns null if there is no import.
   * @deprecated Use {@link #getImports()} instead. There may be many imports; this method only
   *             returns the first.
   */
  @Deprecated
  public Import getImport();

  /**
   * Sets the import declared by this Resource Manager Configuration, if any.
   * 
   * @param aImport
   *          an object that defines how to locate an external XML file defining the resource
   *          manager configuration. Null indicates that there is no import.
   * @deprecated Use {@link #setImports(Import[])} instead.
   */
  @Deprecated
  public void setImport(Import aImport);

  /**
   * Gets the descriptions of the external resources to be instantiated and managed by the resource
   * manager.
   * 
   * @return an array of {@link org.apache.uima.resource.ExternalResourceDescription} objects that
   *         describe the external resources.
   */
  public ExternalResourceDescription[] getExternalResources();

  /**
   * Sets the descriptions of the external resources to be instantiated and managed by the resource
   * manager.
   * 
   * @param aDescriptions
   *          an array of {@link org.apache.uima.resource.ExternalResourceDescription} objects that
   *          describe the external resources.
   */
  public void setExternalResources(ExternalResourceDescription[] aDescriptions);

  /**
   * Adds a External Resource to this configuration
   * 
   * @param aExternalResourceDescription
   *          the ExternalResourceDescription to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addExternalResource(ExternalResourceDescription aExternalResourceDescription);

  /**
   * Removes an ExternalResource from this configuration.
   * 
   * @param aExternalResourceDescription
   *          the ExternalResourceDescription to remove (must be == with an
   *          ExternalResourceDescription in this collection, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeExternalResource(ExternalResourceDescription aExternalResourceDescription);

  /**
   * Gets the bindings between an Analysis Engine's
   * {@link org.apache.uima.resource.ExternalResourceDependency ExternalResourceDependencies} and
   * the {@link org.apache.uima.resource.ExternalResourceDescription} objects defined in this
   * configuration.
   * 
   * @return an array of {@link org.apache.uima.resource.metadata.ExternalResourceBinding} objects
   *         that bind dependencies to resources.
   */
  public ExternalResourceBinding[] getExternalResourceBindings();

  /**
   * Sets the bindings between an Analysis Engine's
   * {@link org.apache.uima.resource.ExternalResourceDependency ExternalResourceDependencies} and
   * the {@link org.apache.uima.resource.ExternalResourceDescription} objects defined in this
   * configuration.
   * 
   * @param aBindings
   *          an array of {@link org.apache.uima.resource.metadata.ExternalResourceBinding} objects
   *          that bind dependencies to resources.
   */
  public void setExternalResourceBindings(ExternalResourceBinding[] aBindings);

  /**
   * Adds a External ResourceBinding to this configuration
   * 
   * @param aExternalResourceBinding
   *          the ExternalResourceBinding to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addExternalResourceBinding(ExternalResourceBinding aExternalResourceBinding);

  /**
   * Removes an ExternalResourceBinding from this configuration.
   * 
   * @param aExternalResourceBinding
   *          the ExternalResourceBinding to remove (must be == with an ExternalResourceBinding in
   *          this collection, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeExternalResourceBinding(ExternalResourceBinding aExternalResourceBinding);

  /**
   * Resolves any import declarations in this resource manager configuration, adding the imported
   * external resources and external resource bindings directly onto this
   * ResourceManagerConfiguration's {@link #getExternalResources() externalResources} and
   * {@link #getExternalResourceBindings() externalResourceBindings} lists.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations in this resource manager configuration, adding the imported
   * external resources and external resource bindings directly onto this
   * ResourceManagerConfiguration's {@link #getExternalResources() externalResources} and
   * {@link #getExternalResourceBindings() externalResourceBindings} lists.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate the XML file imported by name. For example, the
   *          path in which to locate the imported files can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Resolves any import declarations in this resource manager configuration, adding the imported
   * external resources and external resource bindings directly onto this
   * ResourceManagerConfiguration's {@link #getExternalResources() externalResources} and
   * {@link #getExternalResourceBindings() externalResourceBindings} lists.
   * <p>
   * This version is used internally to resolve nested imports.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate the XML file imported by name. For example, the
   *          path in which to locate the imported files can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * @param aAlreadyImportedURLs
   *          names of already imported URLs, so we don't import them again.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(Collection<String> aAlreadyImportedURLs, ResourceManager aResourceManager)
          throws InvalidXMLException;

}
