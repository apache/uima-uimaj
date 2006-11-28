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
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractImportablePartSection extends AbstractSection {

  /**
   * @param editor
   * @param parent
   * @param headerText
   * @param description
   */
  public AbstractImportablePartSection(MultiPageEditor aEditor, Composite parent,
          String headerText, String description) {
    super(aEditor, parent, headerText, description);
  }

  // ********************************
  // * GUI methods
  // ********************************
  protected boolean isLocalItem(TableTreeItem item) {
    return !item.getForeground().equals(editor.getFadeColor());
  }

  // ********************************
  // * Universal Getters
  // ********************************
  /**
   * returns null if no feature by this name
   * 
   * @param name
   * @param td
   * @return
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

  protected TypeDescription getLocalTypeDefinition(TypeDescription td) {
    TypeSystemDescription tsdLocal = getTypeSystemDescription();
    if (null == tsdLocal)
      return null;
    return tsdLocal.getType(td.getName());
  }

  protected FeatureDescription getLocalFeatureDefinition(TypeDescription td, FeatureDescription fd) {
    return getLocalFeatureDefinition(td, fd.getName());
  }

  protected FeatureDescription getLocalFeatureDefinition(TypeDescription td, String featureName) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return null;
    return getFeatureFromTypeDescription(featureName, localTd);
  }

  protected AllowedValue getLocalAllowedValue(TypeDescription td, AllowedValue unchangedAv) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return null;
    return getAllowedValue(unchangedAv.getString(), localTd);
  }

  // ********************************
  // * Built-in Getters
  // * used to do GUI "merge" with built-in things
  // ********************************
  public TypeDescription getBuiltInTypeDescription(TypeDescription td) {
    return (TypeDescription) BuiltInTypes.typeDescriptions.get(td.getName());
  }

  // ********************************
  // * Local Testers
  // ********************************
  protected boolean isLocalType(TypeDescription td) {
    return (null != getLocalTypeDefinition(td));
  }

  protected boolean isLocalType(String typeName) {
    return null != editor.getTypeSystemDescription().getType(typeName);
  }

  protected boolean isLocalFeature(String featureName, TypeDescription td) {
    return (null != getLocalFeatureDefinition(td, featureName));
  }

  protected boolean isLocalAllowedValue(String avString, TypeDescription td) {
    TypeDescription localTd = getLocalTypeDefinition(td);
    if (null == localTd)
      return false;
    return Utility.arrayContains(localTd.getAllowedValues(), avString);
  }

  // ********************************
  // * Imported Testers
  // ********************************

  public boolean isImportedType(String typeName) {
    return null != editor.getImportedTypeSystemDesription().getType(typeName);
  }

  protected boolean isImportedType(TypeDescription td) {
    return null != editor.getImportedTypeSystemDesription().getType(td.getName());
  }

  protected boolean isImportedFeature(String name, TypeDescription td) {
    TypeDescription importedTd = editor.getImportedTypeSystemDesription().getType(td.getName());
    if (null == importedTd)
      return false;
    return null != getFeatureFromTypeDescription(name, importedTd);
  }

  protected boolean isImportedAllowedValue(TypeDescription td, AllowedValue av) {
    TypeDescription importedTd = editor.getImportedTypeSystemDesription().getType(td.getName());
    if (null == importedTd)
      return false;
    return null != getAllowedValue(av.getString(), importedTd);
  }

  // ********************************
  // * Built-in Testers
  // ********************************
  protected boolean isBuiltInType(TypeDescription td) {
    return null != getBuiltInTypeDescription(td);
  }

  protected boolean isBuiltInType(String typeName) {
    return null != BuiltInTypes.typeDescriptions.get(typeName);
  }

  protected boolean isBuiltInFeature(String name, TypeDescription td) {
    TypeDescription builtInTd = (TypeDescription) BuiltInTypes.typeDescriptions.get(td.getName());
    if (null == builtInTd)
      return false;
    return null != getFeatureFromTypeDescription(name, builtInTd);
  }

}
