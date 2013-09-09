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

package org.apache.uima.tools.cpm;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.tools.util.gui.FileSelector;
import org.apache.uima.tools.util.gui.FormPanel;
import org.apache.uima.tools.util.gui.ListSelector;

/**
 * 
 * A dynamically generated form panel with components generated from configuration parameters
 * specified as ResourceMetaData. The components are either text fields, checkboxes, FileSelectors
 * or ListSelectors. These are allocated via the populate method.
 * 
 * @see org.apache.uima.tools.cpm.ConfigField
 */

public class MetaDataPanel extends FormPanel {
  private static final long serialVersionUID = 2002216386886772644L;

  ResourceMetaData metaData;

  ArrayList fieldsList = new ArrayList();

  // Contains ConfigFields

  public MetaDataPanel() {
  }

  public MetaDataPanel(int nrColumns) {
    super(nrColumns);
  }

  public void populate(ResourceMetaData md, CasProcessorConfigurationParameterSettings overrides) {
    metaData = md;

    ConfigurationParameterDeclarations cpd = metaData.getConfigurationParameterDeclarations();

    ConfigurationParameter[] parameters = cpd.getConfigurationParameters();
    ConfigurationParameterSettings cps = metaData.getConfigurationParameterSettings();

    if (parameters == null || cps == null)
      return;

    // Loop through parameters, creating label captions and
    // an appropriate component for the data type:

    for (int i = 0; i < parameters.length; i++) {
      String name = parameters[i].getName();
      String type = parameters[i].getType();
      boolean multiValued = parameters[i].isMultiValued();

      boolean requiresFileSelector = false;
      if ((name.endsWith("Dir") || name.endsWith("Directory") || name.endsWith("Descriptor") || name
              .indexOf("File") != -1)
              && type.equals("String"))
        requiresFileSelector = true;

      boolean justDirectories = false;
      if (requiresFileSelector && (name.endsWith("Dir") || name.endsWith("Directory"))) {
        justDirectories = true;
      }

      String caption = getCaptionFromName(name);
      add(new JLabel(caption));

      JComponent field = null;

      Object parameterValue = cps.getParameterValue(name);

      if (type.equals("Boolean"))
        field = new JCheckBox((String) null, (parameterValue == null) ? false
                : ((Boolean) parameterValue).booleanValue());
      else if (multiValued == false) {
        if (requiresFileSelector == false) {
          String stringValue = (parameterValue == null) ? "" : parameterValue.toString();
          field = new JTextField(stringValue, 20);
        } else {
          String filePath;
          if (parameterValue == null)
            filePath = "";
          else {
            File file = new File((String) parameterValue);
            filePath = file.getPath();
          }
          int selectionMode = justDirectories ? JFileChooser.DIRECTORIES_ONLY
                  : JFileChooser.FILES_AND_DIRECTORIES;

          field = new FileSelector(filePath, caption, selectionMode);
        }
      } else
      // It's a multi-valued array:
      {
        if (parameterValue instanceof Object[]) {
          field = new ListSelector((Object[]) parameterValue);
        } else {
          field = new ListSelector(new Object[0]);
        }
      }

      add(field);

      fieldsList.add(new ConfigField(name, type, multiValued, field));
    }
    // apply overrides
    if (overrides != null) {
      NameValuePair[] nvps = overrides.getParameterSettings();
      for (int i = 0; i < nvps.length; i++) {
        setValue(nvps[i].getName(), nvps[i].getValue());
      }
    }
  }

  public ResourceMetaData getMetaData() {
    return metaData;
  }

  public List getValues() {
    return fieldsList;
  }

  /**
   * @param fieldName
   *          Configuration parameter field name
   * @param fieldValue
   *          Field value
   */
  public void setValue(String fieldName, Object fieldValue) {
    // Find fieldName in fieldList:
    for (int i = 0; i < fieldsList.size(); i++) {
      ConfigField field = (ConfigField) fieldsList.get(i);
      if (field.getParameterName().equals(fieldName)) {
        field.setFieldValue(fieldValue);
        return;
      }
    }
  }

  /** Removes all fields */
  public void clearAll() {
    Component components[] = gridBagPanel.getComponents();
    for (int i = (components.length - 1); i >= 0; i--) {
      gridBagPanel.remove(i);
    }
    componentIndex = 0;
    fieldsList.clear();
  }

  /**
   * Returns whether this panel has been modified from its original configuration. Note that this is
   * not affected by saves. For that, use isDirty().
   */
  public boolean isModified() {
    List fields = getValues();
    Iterator it = fields.iterator();
    while (it.hasNext()) {
      ConfigField fld = (ConfigField) it.next();
      if (fld.isModified())
        return true;
    }
    return false;
  }

  /**
   * Returns whether this panel is dirty; that is, whether a field has been modified since
   * clearDirty() was last called.
   * 
   * @return whether this panel is dirty
   */
  public boolean isDirty() {
    List fields = getValues();
    Iterator it = fields.iterator();
    while (it.hasNext()) {
      ConfigField fld = (ConfigField) it.next();
      if (fld.isDirty())
        return true;
    }
    return false;
  }

  /** Marks all fields in this panel is not dirty. */
  public void clearDirty() {
    List fields = getValues();
    Iterator iterator = fields.iterator();
    while (iterator.hasNext()) {
      ConfigField configField = (ConfigField) iterator.next();
      configField.clearDirty();
    }
  }

}
