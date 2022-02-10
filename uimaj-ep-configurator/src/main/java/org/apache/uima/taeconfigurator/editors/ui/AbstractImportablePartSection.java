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

package org.apache.uima.taeconfigurator.editors.ui;

import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.model.BuiltInTypes;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The Class AbstractImportablePartSection.
 */
public abstract class AbstractImportablePartSection extends AbstractSection {

  /**
   * Instantiates a new abstract importable part section.
   *
   * @param aEditor
   *          the a editor
   * @param parent
   *          the parent
   * @param headerText
   *          the header text
   * @param description
   *          the description
   */
  public AbstractImportablePartSection(MultiPageEditor aEditor, Composite parent, String headerText,
          String description) {
    super(aEditor, parent, headerText, description);
  }

  // ********************************
  // * GUI methods
  /**
   * Checks if is local item.
   *
   * @param item
   *          the item
   * @return true, if is local item
   */
  // ********************************
  protected boolean isLocalItem(TreeItem item) {
    return !item.getForeground().equals(editor.getFadeColor());
  }

  // ********************************
  // * Universal Getters
  // ********************************
  /**
   * returns null if no feature by this name.
   *
   * @param name
   *          the name
   * @param td
   *          the td
   * @return the feature from type description
   */
  public FeatureDescription getFeatureFromTypeDescription(String name, TypeDescription td) {
    FeatureDescription[] fds = td.getFeatures();
    if (fds == null)
      return null;
    for (int i = 0; i < fds.length; i++) {
      if (name.equals(fds[i].getName()))
        return fds[i];
    }
    return null;
  }

  /**
   * Gets the allowed value.
   *
   * @param value
   *          the value
   * @param td
   *          the td
   * @return the allowed value
   */
  public AllowedValue getAllowedValue(String value, TypeDescription td) {
    AllowedValue[] avs = td.getAllowedValues();
    if (null == avs)
      return null;
    for (int i = 0; i < avs.length; i++) {
      if (value.equals(avs[i].getString()))
        return avs[i];
    }
    return null;
  }

  // ********************************
  // * Local Getters
  // ********************************

  /**
   * Gets the local type definition.
   *
   * @param td
   *          the td
   * @return the local type definition
   */
  protected TypeDescription getLocalTypeDefinition(TypeDescription td) {
    TypeSystemDescription tsdLocal = getTypeSystemDescription();
    if (null == tsdLocal)
      return null;
    return tsdLocal.getType(td.getName());
  }

  /**
   * Gets the local feature definition.
   *
   * @param td
   *          the td
   * @param fd
   *          the fd
   * @return the local feature definition
   */
  protected FeatureDescription getLocalFeatureDefinition(TypeDescription td,
          FeatureDescription fd) {
    return getLocalFeatureDefinition(td, fd.getName());
  }

  /**
   * Gets the local feature definition.
   *
   * @param td
   *          the td
   * @param featureName
   *          the feature name
   * @return the local feature definition
   */
  protected FeatureDescription getLocalFeatureDefinition(TypeDescription td, String featureName) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return null;
    return getFeatureFromTypeDescription(featureName, localTd);
  }

  /**
   * Gets the local allowed value.
   *
   * @param td
   *          the td
   * @param unchangedAv
   *          the unchanged av
   * @return the local allowed value
   */
  protected AllowedValue getLocalAllowedValue(TypeDescription td, AllowedValue unchangedAv) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return null;
    return getAllowedValue(unchangedAv.getString(), localTd);
  }

  // ********************************
  // * Built-in Getters
  // * used to do GUI "merge" with built-in things
  /**
   * Gets the built in type description.
   *
   * @param td
   *          the td
   * @return the built in type description
   */
  // ********************************
  public TypeDescription getBuiltInTypeDescription(TypeDescription td) {
    return (TypeDescription) BuiltInTypes.typeDescriptions.get(td.getName());
  }

  // ********************************
  // * Local Testers
  /**
   * Checks if is local type.
   *
   * @param td
   *          the td
   * @return true, if is local type
   */
  // ********************************
  protected boolean isLocalType(TypeDescription td) {
    return (null != getLocalTypeDefinition(td));
  }

  /**
   * Checks if is local type.
   *
   * @param typeName
   *          the type name
   * @return true, if is local type
   */
  protected boolean isLocalType(String typeName) {
    return null != editor.getTypeSystemDescription().getType(typeName);
  }

  /**
   * Checks if is local feature.
   *
   * @param featureName
   *          the feature name
   * @param td
   *          the td
   * @return true, if is local feature
   */
  protected boolean isLocalFeature(String featureName, TypeDescription td) {
    return (null != getLocalFeatureDefinition(td, featureName));
  }

  /**
   * Checks if is local allowed value.
   *
   * @param avString
   *          the av string
   * @param td
   *          the td
   * @return true, if is local allowed value
   */
  protected boolean isLocalAllowedValue(String avString, TypeDescription td) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return false;
    return Utility.arrayContains(localTd.getAllowedValues(), avString);
  }

  // ********************************
  // * Imported Testers
  // ********************************

  /**
   * Checks if is imported type.
   *
   * @param typeName
   *          the type name
   * @return true, if is imported type
   */
  public boolean isImportedType(String typeName) {
    return null != editor.getImportedTypeSystemDesription().getType(typeName);
  }

  /**
   * Checks if is imported type.
   *
   * @param td
   *          the td
   * @return true, if is imported type
   */
  protected boolean isImportedType(TypeDescription td) {
    return null != editor.getImportedTypeSystemDesription().getType(td.getName());
  }

  /**
   * Checks if is imported feature.
   *
   * @param name
   *          the name
   * @param td
   *          the td
   * @return true, if is imported feature
   */
  protected boolean isImportedFeature(String name, TypeDescription td) {
    TypeDescription importedTd = editor.getImportedTypeSystemDesription().getType(td.getName());
    if (null == importedTd)
      return false;
    return null != getFeatureFromTypeDescription(name, importedTd);
  }

  /**
   * Checks if is imported allowed value.
   *
   * @param td
   *          the td
   * @param av
   *          the av
   * @return true, if is imported allowed value
   */
  protected boolean isImportedAllowedValue(TypeDescription td, AllowedValue av) {
    TypeDescription importedTd = editor.getImportedTypeSystemDesription().getType(td.getName());
    if (null == importedTd)
      return false;
    return null != getAllowedValue(av.getString(), importedTd);
  }

  // ********************************
  // * Built-in Testers
  /**
   * Checks if is built in type.
   *
   * @param td
   *          the td
   * @return true, if is built in type
   */
  // ********************************
  protected boolean isBuiltInType(TypeDescription td) {
    return null != getBuiltInTypeDescription(td);
  }

  /**
   * Checks if is built in type.
   *
   * @param typeName
   *          the type name
   * @return true, if is built in type
   */
  protected boolean isBuiltInType(String typeName) {
    return null != BuiltInTypes.typeDescriptions.get(typeName);
  }

  /**
   * Checks if is built in feature.
   *
   * @param name
   *          the name
   * @param td
   *          the td
   * @return true, if is built in feature
   */
  protected boolean isBuiltInFeature(String name, TypeDescription td) {
    TypeDescription builtInTd = (TypeDescription) BuiltInTypes.typeDescriptions.get(td.getName());
    if (null == builtInTd)
      return false;
    return null != getFeatureFromTypeDescription(name, builtInTd);
  }

}
