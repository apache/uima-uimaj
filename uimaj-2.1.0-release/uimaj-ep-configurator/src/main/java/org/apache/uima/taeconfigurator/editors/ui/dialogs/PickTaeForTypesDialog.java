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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class PickTaeForTypesDialog extends AbstractDialog {
  private java.util.List m_DelegateComponentDescriptors, m_delegateComponentDescriptions;

  private String[] m_selectedDelegateComponentDescriptors;

  // private boolean m_bAutoAddToFlow;
  private String m_aggregateFileName;

  private Text delegateComponentDescriptionText;

  List delegateComponentListGUI;

  private List inputTypesList;

  private List outputTypesList;

  // private Button autoAddToFlowButton;
  private DialogSelectionListener dialogSelectionListener = new DialogSelectionListener();

  private Button importByNameUI;

  private Button importByLocationUI;

  public boolean isImportByName;

  public class DialogSelectionListener implements SelectionListener {
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == delegateComponentListGUI) {
        update();
      } else {
        enableOK();
      }
    }

    public void widgetDefaultSelected(SelectionEvent e) {
      // nothing to do in this case
    }
  }

  /**
   * @param parentShell
   */
  public PickTaeForTypesDialog(AbstractSection aSection, String aggregateFileName,
          java.util.List delegateComponentDescriptors, java.util.List delegateComponentDescriptions) {

    super(aSection, "Select Delegate Component Descriptor(s)",
            "Select one or more delegate components to add and press OK");
    m_aggregateFileName = aggregateFileName;
    m_DelegateComponentDescriptors = delegateComponentDescriptors;
    m_delegateComponentDescriptions = delegateComponentDescriptions;
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);
    Label specialMsgLabel = new Label(composite, SWT.WRAP);
    AbstractSection.spacer(composite);

    // create m_taePrompt
    createWideLabel(composite, "Delegate Components:");

    delegateComponentListGUI = new List(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.heightHint = 100;
    delegateComponentListGUI.setLayoutData(gridData);
    boolean bContainsConstituentsAlreadyInAggregate = false;
    boolean bContainsAggregate = false;
    for (int i = 0; i < m_DelegateComponentDescriptors.size(); i++) {
      String sAdditional = "";
      if (m_aggregateFileName.equals(m_DelegateComponentDescriptors.get(i))) {
        sAdditional = "**";
        bContainsAggregate = true;
      }
      delegateComponentListGUI.add((String) m_DelegateComponentDescriptors.get(i) + sAdditional);
    }
    delegateComponentListGUI.addSelectionListener(dialogSelectionListener);

    if (bContainsConstituentsAlreadyInAggregate && bContainsAggregate) {
      specialMsgLabel
              .setText("(* indicates delegate component is already part of aggregate, ** is aggregate currently being configured)");
    } else if (bContainsConstituentsAlreadyInAggregate) {
      specialMsgLabel.setText("(* indicates delegate component is already part of aggregate)");
    } else if (bContainsAggregate) {
      specialMsgLabel.setText("(** is aggregate currently being configured)");
    }

    createWideLabel(composite, "Delegate Component Description:");

    delegateComponentDescriptionText = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL
            | SWT.BORDER);
    delegateComponentDescriptionText.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridData dcgd = new GridData(GridData.FILL_HORIZONTAL);
    dcgd.heightHint = 50;
    delegateComponentDescriptionText.setLayoutData(dcgd);
    delegateComponentDescriptionText.setEditable(false);

    createWideLabel(composite, "Input Types:");

    inputTypesList = new List(composite, SWT.BORDER | SWT.V_SCROLL);
    inputTypesList.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createWideLabel(composite, "Output Types:");

    outputTypesList = new List(composite, SWT.BORDER | SWT.V_SCROLL);
    outputTypesList.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // autoAddToFlowButton = new Button(composite, SWT.CHECK);
    // autoAddToFlowButton.setText("Add selected delegate components to end of flow");
    // autoAddToFlowButton.setSelection(true);
    // autoAddToFlowButton.setBackground(null);

    importByNameUI = newButton(composite, SWT.RADIO, "Import by Name",
            "Importing by name looks up the name on the classpath and datapath.");
    importByLocationUI = newButton(composite, SWT.RADIO, "Import by Location",
            "Importing by location requires a relative or absolute URL");

    String defaultBy = CDEpropertyPage.getImportByDefault(editor.getProject());
    if (defaultBy.equals("location")) {
      importByNameUI.setSelection(false);
      importByLocationUI.setSelection(true);
    } else {
      importByNameUI.setSelection(true);
      importByLocationUI.setSelection(false);
    }
    return composite;
  }

  public void update() {

    int nSelectedAeIndex = delegateComponentListGUI.getSelectionIndices()[0];
    ResourceSpecifier rs = (ResourceSpecifier) m_delegateComponentDescriptions
            .get(nSelectedAeIndex);

    String description = rs instanceof ResourceCreationSpecifier ? ((ResourceCreationSpecifier) rs)
            .getMetaData().getDescription() : "No Description - remote service descriptor";
    delegateComponentDescriptionText.setText(convertNull(description));

    inputTypesList.removeAll();
    outputTypesList.removeAll();
    Capability[] capabilities = AbstractSection.getCapabilities(rs);
    if (capabilities != null && capabilities.length > 0) {
      TypeOrFeature[] inputs = capabilities[0].getInputs();
      if (inputs != null) {
        for (int i = 0; i < inputs.length; i++) {
          if (inputs[i].isType()) {
            inputTypesList.add(section.formatName(inputs[i].getName()));
          }
        }
      }

      TypeOrFeature[] outputs = capabilities[0].getOutputs();
      if (outputs != null) {
        for (int i = 0; i < outputs.length; i++) {
          if (outputs[i].isType()) {
            outputTypesList.add(section.formatName(outputs[i].getName()));
          }
        }
      }
    }
    enableOK();
  }

  public void enableOK() {
    boolean bEnableOk = false;
    String[] selections = delegateComponentListGUI.getSelection();
    for (int i = 0; i < selections.length; i++) {
      if (!selections[i].endsWith("*")) {
        bEnableOk = true;
        i = selections.length;
      }
    }
    okButton.setEnabled(bEnableOk);
  }

  public String[] getSelectedDelegateComponentDescriptors() {
    return (String[]) m_selectedDelegateComponentDescriptors.clone();
  }

  // public boolean getAutoAddToFlow() {
  // return m_bAutoAddToFlow;
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  public void copyValuesFromGUI() {
    // this is where we do rollup and detect if we need any additional
    // types based on types of features and whether we are a supertype
    // of a type that is not yet defined

    int[] selIndices = delegateComponentListGUI.getSelectionIndices();
    int nRealCount = 0;
    for (int i = 0; i < selIndices.length; i++) {
      if (!delegateComponentListGUI.getItem(selIndices[i]).endsWith("*")) {
        nRealCount++;
      }
    }
    m_selectedDelegateComponentDescriptors = new String[nRealCount];

    for (int i = 0, j = 0; i < selIndices.length; i++) {
      if (!delegateComponentListGUI.getItem(selIndices[i]).endsWith("*")) {
        m_selectedDelegateComponentDescriptors[j] = (String) m_DelegateComponentDescriptors
                .get(selIndices[i]);
      }
    }

    // m_bAutoAddToFlow = autoAddToFlowButton.getSelection();
    isImportByName = importByNameUI.getSelection();
    CDEpropertyPage.setImportByDefault(editor.getProject(), isImportByName ? "name" : "location");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  public boolean isValid() {
    return true;
  }

}
