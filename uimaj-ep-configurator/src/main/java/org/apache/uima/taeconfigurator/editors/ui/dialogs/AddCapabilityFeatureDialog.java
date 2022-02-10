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
import java.util.List;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.CapabilitySection;
import org.apache.uima.taeconfigurator.editors.ui.Utility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The Class AddCapabilityFeatureDialog.
 */
public class AddCapabilityFeatureDialog extends AbstractDialogMultiColTable {

  /** The features. */
  public String[] features; // this is the selection

  /** The inputs. */
  public boolean[] inputs;

  /** The outputs. */
  public boolean[] outputs;

  /** The all features input. */
  public boolean allFeaturesInput = false;

  /** The all features output. */
  public boolean allFeaturesOutput = false;

  /** The all features. */
  private Feature[] allFeatures;

  /** The capability. */
  private Capability capability;

  /** The selected type. */
  private Type selectedType;

  /** The input not allowed. */
  private boolean inputNotAllowed = true;

  /** The feature array 0. */
  private static Feature[] featureArray0 = new Feature[0];

  /**
   * Instantiates a new adds the capability feature dialog.
   *
   * @param aSection
   *          the a section
   * @param aSelectedType
   *          the a selected type
   * @param c
   *          the c
   */
  public AddCapabilityFeatureDialog(AbstractSection aSection, Type aSelectedType, Capability c) {
    super(aSection, "Specify features input and / or output",
            "Designate by mouse clicking one or more features in the Input and/or Output column, to designate as Input and/or Output press \"OK\"");
    selectedType = aSelectedType;
    allFeatures = selectedType.getFeatures().toArray(featureArray0);
    Arrays.sort(allFeatures);

    capability = c;
    TypeOrFeature[] localInputs = c.getInputs();
    String typeName = selectedType.getName();
    if (null != localInputs) {
      for (int i = 0; i < localInputs.length; i++) {
        if (localInputs[i].isType() && typeName.equals(localInputs[i].getName())) {
          inputNotAllowed = false;
          break;
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.
   * swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    Tree tree = newTree(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
    ((GridData) tree.getLayoutData()).heightHint = 100;
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);
    new TreeColumn(tree, SWT.NONE).setText("Feature Name");
    new TreeColumn(tree, SWT.NONE).setText("Input");
    new TreeColumn(tree, SWT.NONE).setText("Output");

    TreeItem item = new TreeItem(tree, SWT.NONE);
    item.setText(0, CapabilitySection.ALL_FEATURES);
    TypeOrFeature tof = AbstractSection.getTypeOrFeature(capability.getInputs(),
            selectedType.getName());
    setChecked(item, 1, null == tof ? false : tof.isAllAnnotatorFeatures());
    tof = AbstractSection.getTypeOrFeature(capability.getOutputs(), selectedType.getName());
    setChecked(item, 2, null == tof ? false : tof.isAllAnnotatorFeatures());

    for (int i = 0; i < allFeatures.length; i++) {
      item = new TreeItem(tree, SWT.NONE);
      item.setText(0, allFeatures[i].getShortName());
      setChecked(item, 1, CapabilitySection.isInput(getTypeFeature(allFeatures[i]), capability));
      setChecked(item, 2, CapabilitySection.isOutput(getTypeFeature(allFeatures[i]), capability));
    }

    tree.removeListener(SWT.Selection, this);
    tree.addListener(SWT.MouseDown, this); // for i / o toggling
    section.packTree(tree);
    newErrorMessage(composite);
    return composite;
  }

  /**
   * return the actual type name : short-feature-name.
   *
   * @param feature
   *          the feature
   * @return the actual type name : short-feature-name
   */
  protected String getTypeFeature(Feature feature) {
    return selectedType.getName() + ':' + feature.getShortName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialogMultiColTable#toggleValue(org.
   * eclipse.swt.widgets.TableItem, int)
   */
  @Override
  protected void toggleValue(TableItem item, int col) {
    if (1 == col && inputNotAllowed) {
      Utility.popMessage("Input not allowed",
              "Input not allowed unless the type itself is also marked as an input.",
              MessageDialog.ERROR);
      return;
    }
    super.toggleValue(item, col); // updates numberChecked for this item
    if (item.getText(col).equals(checkedIndicator(col)))
      if (item.getText(0).equals(CapabilitySection.ALL_FEATURES))
        uncheckAllOtherFeatures(col);
      else
        setChecked(f_tree.getItem(0), col, false); // uncheck all-features
  }

  /**
   * Uncheck all other features.
   *
   * @param column
   *          the column
   */
  private void uncheckAllOtherFeatures(int column) {
    TreeItem[] items = f_tree.getItems();
    for (int i = 1; i < items.length; i++) {
      setChecked(items[i], column, false);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    List<String> names = new ArrayList<>();
    List<Boolean> ins = new ArrayList<>();
    List<Boolean> outs = new ArrayList<>();

    for (int i = f_tree.getItemCount() - 1; i >= 1; i--) {
      TreeItem item = f_tree.getItem(i);
      if (item.getText(1).equals(checkedIndicator(1))
              || item.getText(2).equals(checkedIndicator(2))) {
        names.add(item.getText(0));
        ins.add(item.getText(1).equals(checkedIndicator(1)));
        outs.add(item.getText(2).equals(checkedIndicator(2)));
      }
    }

    features = (String[]) names.toArray(stringArray0);
    inputs = new boolean[features.length];
    outputs = new boolean[features.length];
    for (int i = 0; i < features.length; i++) {
      inputs[i] = (Boolean) ins.get(i);
      outputs[i] = (Boolean) outs.get(i);
    }

    TreeItem item = f_tree.getItem(0);
    allFeaturesInput = item.getText(1).equals(checkedIndicator(1));
    allFeaturesOutput = item.getText(2).equals(checkedIndicator(2));
  }

}
