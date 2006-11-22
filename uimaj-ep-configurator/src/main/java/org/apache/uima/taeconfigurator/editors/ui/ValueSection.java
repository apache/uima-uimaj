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

import java.util.Arrays;

import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.CommonInputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;

public class ValueSection extends AbstractSectionParm {

  private Composite valueTextStack;

  private StackLayout valueTextStackLayout;

  private Text valueText;

  private CCombo valueTextCombo;

  private Composite vtc1;

  private Composite vtc2;

  private Table valueTable;

  private ParameterSettingsSection master;

  private Button addButton;

  private Button editButton;

  private Button removeButton;

  private Button upButton;

  private Button downButton;

  private Composite buttonContainer;

  private ConfigurationParameter selectedCP;

  private ConfigurationParameterSettings modelSettings;

  public ValueSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Values", "Specify the value of the selected configuration parameter.");
  }

  /*
   * Called by the page constructor after all sections are created, to initialize them.
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */
  public void initialize(IManagedForm form) {
    super.initialize(form);

    master = editor.getSettingsPage().getParameterSettingsSection();

    Composite sectionClient = new3ColumnComposite(this.getSection());
    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);

    valueTextStack = newComposite(sectionClient);
    valueTextStack.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                    + GridData.VERTICAL_ALIGN_FILL));
    ((GridData) valueTextStack.getLayoutData()).horizontalSpan = 2;
    valueTextStack.setLayout(valueTextStackLayout = new StackLayout());
    valueTextStackLayout.marginHeight = 5;
    valueTextStackLayout.marginWidth = 5;
    vtc1 = new2ColumnComposite(valueTextStack);
    vtc2 = new2ColumnComposite(valueTextStack);
    enableBorders(vtc1);
    enableBorders(vtc2);
    toolkit.paintBordersFor(vtc1);
    toolkit.paintBordersFor(vtc2);
    valueText = newLabeledTextField(vtc1, "Value", "Use the combo pulldown to pick True or False",
                    SWT.NONE);
    valueTextCombo = newLabeledCComboWithTip(vtc2, "Value",
                    "Use the combo pulldown to pick True or False");
    valueTextCombo.add("true");
    valueTextCombo.add("false");

    spacer(sectionClient);

    Label valueListLabel = toolkit.createLabel(sectionClient, "Value list:");
    valueListLabel.setLayoutData(new GridData(SWT.TOP));
    valueTable = newTable(sectionClient, SWT.MULTI, 0);

    // no column spec in table is an idiom that makes it a fancy list

    // Buttons
    buttonContainer = newButtonContainer(sectionClient);

    addButton = newPushButton(buttonContainer, S_ADD, "Click here to add a value to the list.");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    upButton = newPushButton(buttonContainer, S_UP, S_UP_TIP);
    downButton = newPushButton(buttonContainer, S_DOWN, S_DOWN_TIP);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();
    master = editor.getSettingsPage().getParameterSettingsSection();
    valueTextStackLayout.topControl = vtc1;

    selectedCP = master.getSelectedModelParameter();

    if (selectedCP == null) { // no param selected
      valueText.setText("");
      valueTable.removeAll();
    } else { // parm selected in master view
      Object modelValue;
      modelSettings = getModelSettings();
      String groupName = master.getSelectedParamGroupName();
      String parmName = selectedCP.getName();

      modelValue = (NOT_IN_ANY_GROUP.equals(groupName)) ? modelSettings.getParameterValue(parmName)
                      : modelSettings.getParameterValue(groupName, parmName);

      if (selectedCP.isMultiValued()) {
        // use list, not text field
        valueText.setText("");
        valueTable.removeAll();
        if (modelValue != null && modelValue instanceof Object[]) {
          Object[] valArr = (Object[]) modelValue;
          for (int i = 0; i < valArr.length; i++) {
            TableItem item = new TableItem(valueTable, SWT.NONE);
            item.setText(valArr[i].toString());
          }
        }
      } else { // single-valued parameter - use Text field
        valueTable.removeAll();
        valueText.setText((modelValue == null) ? "" : modelValue.toString());
        if ("Boolean".equals(selectedCP.getType())) {
          valueTextCombo.setText((modelValue == null) ? "" : modelValue.toString());
          valueTextStackLayout.topControl = vtc2;
        } else {
          valueText.setText((modelValue == null) ? "" : modelValue.toString());
          valueTextStackLayout.topControl = vtc1;
        }
      }
    }
    valueTextStack.layout();
    enable();
  }

  public void enable() {

    boolean mvValue = (null != selectedCP) && (selectedCP.isMultiValued());
    valueText.setVisible((null != selectedCP) && (!selectedCP.isMultiValued()));
    valueTextCombo.setVisible((null != selectedCP) && (!selectedCP.isMultiValued()));

    addButton.setEnabled(mvValue);
    int selected = valueTable.getSelectionIndex();
    editButton.setEnabled(mvValue && selected > -1);
    removeButton.setEnabled(mvValue && selected > -1);
    upButton.setEnabled(mvValue && selected > 0);
    downButton.setEnabled(mvValue && (selected > -1)
                    && (selected < (valueTable.getItemCount() - 1)));
    valueText.getParent().redraw();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {

    if (event.widget == valueText) {
      setParmValue(valueText.getText());
    }

    else if (event.widget == valueTextCombo) {
      setParmValue(valueTextCombo.getText());
    }

    else if (event.widget == addButton) {
      // open dialog to enter value
      String dataType = selectedCP.getType();
      int validationFilter = "Boolean".equals(dataType) ? CommonInputDialog.TRUE_FALSE : "Integer"
                      .equals(dataType) ? CommonInputDialog.INTEGER
                      : "Float".equals(dataType) ? CommonInputDialog.FLOAT
                                      : CommonInputDialog.ALLOK;

      CommonInputDialog dialog = new CommonInputDialog(this, "Add value", "Enter a value",
                      validationFilter);

      if (dialog.open() == Window.CANCEL)
        return;
      TableItem item = new TableItem(valueTable, SWT.NONE);
      item.setText(dialog.getValue());
      // update model
      setCurrentParameterValue(valueTable.getItems());
    }

    else if (event.widget == editButton) {
      // open dialog to enter value
      TableItem item = valueTable.getItems()[valueTable.getSelectionIndex()];
      CommonInputDialog dialog = new CommonInputDialog(this, "Add value", "Enter a value",
                      CommonInputDialog.ALLOK, item.getText());

      if (dialog.open() == Window.CANCEL)
        return;

      item.setText(dialog.getValue());
      // update model
      setCurrentParameterValue(valueTable.getItems());
    }

    else if (event.widget == upButton) {
      // update both model and gui: swap nodes
      int selection = valueTable.getSelectionIndex();
      TableItem[] items = valueTable.getItems();
      String temp = items[selection - 1].getText();
      items[selection - 1].setText(items[selection].getText());
      items[selection].setText(temp);
      valueTable.setSelection(selection - 1);
      setCurrentParameterValue(valueTable.getItems());
    } else if (event.widget == downButton) {
      // update both model and gui: swap nodes
      int selection = valueTable.getSelectionIndex();
      TableItem[] items = valueTable.getItems();
      String temp = items[selection + 1].getText();
      items[selection + 1].setText(items[selection].getText());
      items[selection].setText(temp);
      valueTable.setSelection(selection + 1);
      setCurrentParameterValue(valueTable.getItems());
    }

    else if (event.widget == removeButton
                    || (event.widget == valueTable && event.character == SWT.DEL)) {
      handleRemove(event);
    }

    enable();
  }

  private void setParmValue(String value) {
    if (null != value) {
      if ("".equals(value))
        value = null; // means clear the value
      setCurrentParameterValue(value);
    }
  }

  public void handleRemove(Event event) {
    valueTable.remove(valueTable.getSelectionIndices());
    // update model
    setCurrentParameterValue(valueTable.getItems());
  }

  public Button getAddButton() {
    return addButton;
  }

  public Button getRemoveButton() {
    return removeButton;
  }

  public Table getValueTable() {
    return valueTable;
  }

  public Text getValueText() {
    return valueText;
  }

  /**
   * Sets the currently selected parameter to the specified value. The string value will be
   * converted to the appropriate data type. This method works only for single-valued parameters.
   * 
   * @param aValueString
   */
  private void setCurrentParameterValue(String aValueString) {

    Object value = null;
    if (null != aValueString) {
      String paramType = selectedCP.getType();
      try {
        if (ConfigurationParameter.TYPE_STRING.equals(paramType)) {
          value = aValueString;
        } else if (ConfigurationParameter.TYPE_INTEGER.equals(paramType)) {
          value = Integer.valueOf(aValueString);
        } else if (ConfigurationParameter.TYPE_FLOAT.equals(paramType)) {
          value = Float.valueOf(aValueString);
        } else if (ConfigurationParameter.TYPE_BOOLEAN.equals(paramType)) {
          value = Boolean.valueOf(aValueString);
        }
      } catch (NumberFormatException e) {
        Utility
                        .popMessage(
                                        "Invalid Number",
                                        "If typing a floating point exponent, please complete the exponent.\nOtherwise, please retype the proper kind of number",
                                        MessageDialog.ERROR);
        return;
      }
    }
    setModelValue(value);
  }

  /**
   * Sets the currently selected parameter to the specified value. This method works only for
   * multi-valued parameters. The Table Items will be converted to the appropriate data type.
   * 
   * @param aValues
   *          Table Items, one for each value of the multi-valued param
   */
  private void setCurrentParameterValue(TableItem[] aValues) {

    Object[] valueArr = null;
    String paramType = selectedCP.getType();
    try {
      if (ConfigurationParameter.TYPE_STRING.equals(paramType)) {
        valueArr = new String[aValues.length];
        for (int i = 0; i < valueArr.length; i++) {
          valueArr[i] = aValues[i].getText();
        }
      } else if (ConfigurationParameter.TYPE_INTEGER.equals(paramType)) {
        valueArr = new Integer[aValues.length];
        for (int i = 0; i < valueArr.length; i++) {
          valueArr[i] = Integer.valueOf(aValues[i].getText());
        }
      } else if (ConfigurationParameter.TYPE_FLOAT.equals(paramType)) {
        valueArr = new Float[aValues.length];
        for (int i = 0; i < valueArr.length; i++) {
          valueArr[i] = Float.valueOf(aValues[i].getText());
        }
      } else if (ConfigurationParameter.TYPE_BOOLEAN.equals(paramType)) {
        valueArr = new Boolean[aValues.length];
        for (int i = 0; i < valueArr.length; i++) {
          valueArr[i] = Boolean.valueOf(aValues[i].getText());
        }
      } else
        throw new InternalErrorCDE("invalid state");
    } catch (NumberFormatException e) {
      Utility
                      .popMessage(
                                      "Invalid Number",
                                      "One or more values is not of the proper kind of number."
                                                      + " If this entry is the only one with the wrong numeric type,"
                                                      + " Please retype the proper kind of number. Otherwise,"
                                                      + " use the source page to change all the values to the proper type.",
                                      MessageDialog.ERROR);
      return;
    }
    setModelValue(valueArr);
  }

  private void setModelValue(Object value) {
    String groupName = master.getSelectedParamGroupName();
    boolean changed = false;
    if (COMMON_GROUP.equals(groupName)) {
      ConfigurationGroup[] groups = getConfigurationParameterDeclarations()
                      .getConfigurationGroups();
      for (int i = 0; i < groups.length; i++) {
        String[] groupNames = groups[i].getNames();
        for (int j = 0; j < groupNames.length; j++) {
          if (isSameValue(value, modelSettings.getParameterValue(groupNames[j], selectedCP
                          .getName())))
            continue;
          modelSettings.setParameterValue(groupNames[j], selectedCP.getName(), value);
          changed = true;
        }
      }
    } else if (NOT_IN_ANY_GROUP.equals(groupName)) {
      if (!isSameValue(value, modelSettings.getParameterValue(selectedCP.getName()))) {
        modelSettings.setParameterValue(selectedCP.getName(), value);
        changed = true;
      }
    } else {
      if (!isSameValue(value, modelSettings.getParameterValue(groupName, selectedCP.getName()))) {
        modelSettings.setParameterValue(groupName, selectedCP.getName(), value);
        changed = true;
      }
    }
    if (changed)
      editor.setFileDirty();
  }

  private boolean isSameValue(Object v1, Object v2) {
    if (v1 instanceof Object[]) {
      return (Arrays.equals((Object[]) v1, (Object[]) v2));
    } else {
      if (null == v1)
        return null == v2;
      return v1.equals(v2);
    }
  }

}
