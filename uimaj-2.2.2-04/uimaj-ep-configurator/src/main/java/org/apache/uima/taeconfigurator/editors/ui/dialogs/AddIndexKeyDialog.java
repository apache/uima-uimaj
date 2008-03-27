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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class AddIndexKeyDialog extends AbstractDialog {

  static private final String ASCENDING = "Ascending (Standard)";

  static private final String DESCENDING = "Descending (Reverse)";

  static private final String TYPE_PRIORITY = "Type Priority";

  public boolean typePriority;

  public String featureName;

  public int direction;

  private String[] features;

  private FsIndexKeyDescription existingKey = null;

  private List alreadyUsedKeys;

  private String indexKind; // bag, sorted, set

  private CCombo featureUI;

  // private Button browseButton;
  private CCombo kindUI;

  private Label featureLabel;

  // private Composite tc;

  public AddIndexKeyDialog(AbstractSection aSection, String typeName, String indexKind,
          List alreadyUsedKeys) {
    super(aSection, "Add index key", "Add or edit an index key for a type");
    this.indexKind = indexKind;
    this.alreadyUsedKeys = alreadyUsedKeys;
    features = getSortableFeatureNames(typeName);
  }

  public AddIndexKeyDialog(AbstractSection aSection, String typeName, String indexKind,
          List alreadyUsedKeys, FsIndexKeyDescription existingKey) {
    this(aSection, typeName, indexKind, alreadyUsedKeys);
    this.existingKey = existingKey;
  }

  /**
   * 
   * @param taeDescription
   * @param selectedType
   * @return an array of features whose range is primitive
   */
  private String[] getSortableFeatureNames(String selectedTypeName) {
    Type selectedType = section.editor.getCurrentView().getTypeSystem().getType(selectedTypeName);
    List feats = selectedType.getFeatures();
    Collection sortableFeatureNames = new ArrayList();

    for (int i = 0; i < feats.size(); i++) {
      Feature feature = (Feature) feats.get(i);
      Type rangeType = feature.getRange();
      if (AbstractSection.isIndexableRange(rangeType.getName())) {
        if (!alreadyUsedKeys.contains(feature.getShortName()))
          sortableFeatureNames.add(feature.getShortName());
      }
    }
    String[] result = (String[]) sortableFeatureNames.toArray(stringArray0);
    Arrays.sort(result);
    return result;
  }

  // Kind: combo (up, down, or typePriority)
  // Feature: text with assist <browse>

  protected Control createDialogArea(Composite parent) {
    // create composite
    Composite mainComposite = (Composite) super.createDialogArea(parent, existingKey);
    Composite twoCol = new2ColumnComposite(mainComposite);

    if ("sorted".equals(indexKind)) {
      kindUI = newLabeledCCombo(twoCol, "Sort order, or Type Priority",
              "Specify the sort direction, or specify Type Priorities");
      kindUI.add(ASCENDING);
      kindUI.add(DESCENDING);
      kindUI.add(TYPE_PRIORITY);
    }

    featureLabel = new Label(twoCol, SWT.NONE);
    featureLabel.setText("Feature Name");
    featureUI = newCCombo(twoCol, "Pick a feature to use as a key from the available features");
    for (int i = 0; i < features.length; i++) {
      featureUI.add(features[i]);
    }

    if (null == existingKey) { // default initialization
      if ("sorted".equals(indexKind))
        kindUI.setText(kindUI.getItem(0));
    } else if ("sorted".equals(indexKind)) {
      kindUI.setText(existingKey.isTypePriority() ? TYPE_PRIORITY
              : existingKey.getComparator() == FSIndexComparator.STANDARD_COMPARE ? ASCENDING
                      : DESCENDING);
      if (!existingKey.isTypePriority())
        featureUI.setText(existingKey.getFeatureName());
    } else
      featureUI.setText(existingKey.getFeatureName());

    boolean makeFeatureVisible = "set".equals(indexKind) || !TYPE_PRIORITY.equals(kindUI.getText());
    featureUI.setVisible(makeFeatureVisible);
    featureLabel.setVisible(makeFeatureVisible);
    if ("sorted".equals(indexKind))
      kindUI.addListener(SWT.Modify, this);
    return mainComposite;
  }

  public void handleEvent(Event event) {
    if (event.widget == kindUI) {
      boolean makeFeatureVisible = "set".equals(indexKind)
              || !TYPE_PRIORITY.equals(kindUI.getText());
      featureUI.setVisible(makeFeatureVisible);
      featureLabel.setVisible(makeFeatureVisible);
    }
    super.handleEvent(event);
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(typePriority || !(null != featureName && "".equals(featureName)));
  }

  public void copyValuesFromGUI() {
    if ("sorted".equals(indexKind) && TYPE_PRIORITY.equals(kindUI.getText())) {
      typePriority = true;
      featureName = "";
    } else {
      typePriority = false;
      if ("sorted".equals(indexKind)) {
        direction = ASCENDING.equals(kindUI.getText()) ? FSIndexComparator.STANDARD_COMPARE
                : FSIndexComparator.REVERSE_STANDARD_COMPARE;
      } else
        direction = FSIndexComparator.STANDARD_COMPARE;
      featureName = featureUI.getText();
    }
  }

  public boolean isValid() {
    return true;
  }

}
