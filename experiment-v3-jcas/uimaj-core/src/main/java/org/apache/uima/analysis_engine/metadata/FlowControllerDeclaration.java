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

package org.apache.uima.analysis_engine.metadata;

import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.InvalidXMLException;

/**
 * Declares which FlowController is used by the Aggregate Analysis Engine. This can be done either
 * by import or by direct inclusion of a ResourceSpecifier (descriptor) for the FlowController.
 * <p>
 * If an import is used, it is not automatically resolved when this object is deserialized from XML.
 * To resolve the imports, call the {@link #resolveImports()} method. Import resolution is done
 * automatically during AnalysisEngine instantiation.
 */
public interface FlowControllerDeclaration extends MetaDataObject {
  /**
   * Gets the key that can be used to refer to the FlowController in configuration parameter
   * overrides and Sofa mappings.
   * 
   * @return the key assigned to the FlowController
   */
  public String getKey();

  /**
   * Sets the key that can be used to refer to the FlowController in configuration parameter
   * overrides and Sofa mappings.
   * 
   * @param aKey
   *          the key to assign to the FlowController
   */
  public void setKey(String aKey);

  /**
   * Gets the import that references the FlowController specifier.
   * 
   * @return an object containing the import information, or null if no import was used
   */
  public Import getImport();

  /**
   * Sets the import that references the FlowController specifier.
   * 
   * @param aImport
   *          an object containing the import information, or null if no import is to be used
   */
  public void setImport(Import aImport);

  /**
   * Retrieves the <code>ResourceSpecifier</code> used to determine which FlowController is used
   * by the AnalysisEngine.
   * 
   * @return the <code>ResourceSpecifier</code> that specifies a FlowController.
   */
  public ResourceSpecifier getSpecifier();

  /**
   * Sets the <code>ResourceSpecifier</code> used to determine which FlowController is used by the
   * AnalysisEngine.
   * 
   * @param aSpecifier
   *          a <code>ResourceSpecifier</code> that specifies a FlowController
   */
  public void setSpecifier(ResourceSpecifier aSpecifier);

  /**
   * Resolves an imported FlowController specifier, if there is one. The <code>specifier</code>property
   * of this object is set to the result of parsing the imported descriptor. The import is then
   * deleted.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves an imported FlowController specifier, if there is one. The <code>specifier</code>property
   * of this object is set to the result of parsing the imported descriptor. The import is then
   * deleted.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate an XML file imported by name
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

}
