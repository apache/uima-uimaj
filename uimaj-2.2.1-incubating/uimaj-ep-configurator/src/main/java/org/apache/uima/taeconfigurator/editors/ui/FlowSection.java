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

import java.text.MessageFormat;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.impl.CapabilityLanguageFlow_impl;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.analysis_engine.metadata.impl.FlowControllerDeclaration_impl;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.FindComponentDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.PickTaeForTypesDialog;
import org.apache.uima.taeconfigurator.files.MultiResourceSelectionDialog;
import org.apache.uima.taeconfigurator.model.FlowNodes;
import org.apache.uima.util.XMLizable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;

public class FlowSection extends AbstractSection {

  public static final String FIXED_FLOW = "Fixed Flow"; //$NON-NLS-1$

  public static final String CAPABILITY_LANGUAGE_FLOW = "Capability Language Flow"; //$NON-NLS-1$

  public static final String USER_DEFINED_FLOW = "User-defined Flow";

  private CCombo flowControllerChoice;

  Table flowList; // need access from inner class

  private Button upButton;

  private Button downButton;

  private Label flowControllerGUI;

  private Button specifyFlowControllerImportButton;

  private Button findFlowControllerDescriptorButton;

  private Label flowControllerLabel;

  private Label flowControllerKeyLabel;

  private Label flowControllerKeyGUI;

  // private Label flowControllerSpecifierLabel;
  private Label flowChoiceLabel;

  private boolean bDisableToolTipHelp;

  /**
   * creates a section only for aggregate specifiers to define the flow of their delegates
   * 
   * @param editor
   *          the referenced multipage editor
   */
  public FlowSection(MultiPageEditor aEditor, Composite parent) {
    super(aEditor, parent, Messages.getString("FlowSection.ComponentEngineFlowTitle"), //$NON-NLS-1$
            Messages.getString("FlowSection.ComponentEngineFlowDescription")); //$NON-NLS-1$
  }

  /*
   * Called by the page constructor after all sections are created, to initialize them.
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */
  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = new2ColumnComposite(this.getSection());
    // sectionClient (2 col)
    // label / ccombo
    // comp2
    // comp2a fcButtonContainer
    // label / text

    ((GridData) sectionClient.getLayoutData()).grabExcessHorizontalSpace = false;

    flowChoiceLabel = newLabelWithTip(sectionClient, Messages.getString("FlowSection.FlowKind"), //$NON-NLS-1$
            Messages.getString("FlowSection.FlowKindTip")); //$NON-NLS-1$ 
    flowControllerChoice = newCComboWithTip(sectionClient, Messages
            .getString("FlowSection.FlowKindTip")); //$NON-NLS-1$ 

    flowControllerChoice.add(FIXED_FLOW);
    flowControllerChoice.add(CAPABILITY_LANGUAGE_FLOW);
    flowControllerChoice.add(USER_DEFINED_FLOW);

    Composite comp2 = new2ColumnComposite(sectionClient);
    ((GridData) comp2.getLayoutData()).horizontalSpan = 2;
    ((GridData) comp2.getLayoutData()).grabExcessVerticalSpace = false;

    Composite comp2a = new2ColumnComposite(comp2);
    Composite fcButtonContainer = newButtonContainer(comp2, VERTICAL_BUTTONS, 0);

    flowControllerLabel = newLabelWithTip(comp2a, "Flow Controller:",
            "The XML descriptor for the Custom Flow Controller");
    flowControllerGUI = newUnUpdatableTextWithTip(comp2a, "",
            "The XML descriptor for the Custom Flow Controller");

    flowControllerGUI.addListener(SWT.MouseDown, this);
    flowControllerGUI.addListener(SWT.MouseHover, this);

    flowControllerKeyLabel = newLabelWithTip(comp2a, "Key Name:",
            "A unique key name for this Flow Controller");
    flowControllerKeyGUI = newUnUpdatableTextWithTip(comp2a, "",
            "A unique key name for this Flow Controller");

    // flowControllerSpecifierLabel = newLabelWithTip(sectionClient, "Specify:",
    // "Click the Browse or Search button to specify a Flow Controller");
    // Composite fcButtonContainer = newButtonContainer(sectionClient, HORIZONTAL_BUTTONS, 150);
    specifyFlowControllerImportButton = newPushButton(fcButtonContainer, "Browse...",
            "Click here to specify a locally defined Flow Controller", ENABLED);
    findFlowControllerDescriptorButton = newPushButton(fcButtonContainer, "Search",
            "Click here to search for a Flow Controller", ENABLED);

    // flow list

    Composite flowComposite = new2ColumnComposite(sectionClient);
    ((GridData) flowComposite.getLayoutData()).horizontalSpan = 2;
    enableBorders(flowComposite);
    toolkit.paintBordersFor(flowComposite);

    flowList = newTable(flowComposite, SWT.FULL_SELECTION, 0);

    // Buttons
    final Composite buttonContainer = newButtonContainer(flowComposite, VERTICAL_BUTTONS, 70);

    upButton = newPushButton(buttonContainer, S_UP,
            Messages.getString("FlowSection.upTip"), !ENABLED); //$NON-NLS-1$
    downButton = newPushButton(buttonContainer, S_DOWN,
            Messages.getString("FlowSection.downTip"), !ENABLED); //$NON-NLS-1$
    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();

    flowList.removeAll();
    if (!isAggregate()) {
      getSection().setText(Messages.getString("FlowSection.notUsed")); //$NON-NLS-1$
      getSection().setDescription(Messages.getString("FlowSection.onlyForAggregates")); //$NON-NLS-1$
    } else {
      getSection().setText(Messages.getString("FlowSection.ComponentEngineFlowTitle")); //$NON-NLS-1$
      getSection().setDescription(Messages.getString("FlowSection.ComponentEngineFlowDescription")); //$NON-NLS-1$

      FlowControllerDeclaration fcd = getFlowControllerDeclaration();
      flowList.setEnabled(true);
      FlowConstraints flowConstraints = getModelFlow();
      FlowNodes nodesModel = new FlowNodes(flowConstraints);
      String[] nodes = nodesModel.getFlow();
      if (null == nodes)
        nodesModel.setFlow(nodes = stringArray0);
      // add them to the list
      for (int i = 0; i < nodes.length; i++) {
        TableItem item = new TableItem(flowList, SWT.NONE);
        item.setImage(TAEConfiguratorPlugin.getImage(TAEConfiguratorPlugin.IMAGE_ANNOTATOR) //$NON-NLS-1$
                );
        item.setText(0, nodes[i]);
      }
      packTable(flowList);

      if (null == fcd) {
        // Not a custom, user-defined flow
        FixedFlow ff;
        if (null == flowConstraints) {
          // force fixed flow if nothing is specified
          getAnalysisEngineMetaData().setFlowConstraints(
                  flowConstraints = ff = new FixedFlow_impl());
          ff.setFixedFlow(stringArray0);
        }
        String modelFlowType = flowConstraints.getFlowConstraintsType();

        flowControllerChoice.setText(modelFlowType
                .equals(CapabilityLanguageFlow.FLOW_CONSTRAINTS_TYPE) ? CAPABILITY_LANGUAGE_FLOW
                : FIXED_FLOW);

        enableFlowControllerGUI(false);
      } else {
        // User-specified Flow Controller defined
        refreshFcd(fcd);
      }
    }
    enable();
  }

  private void refreshFcd(FlowControllerDeclaration fcd) {
    enableFlowControllerGUI(true);
    String keyName;
    if (null == fcd.getKey() || "".equals(fcd.getKey())) {
      keyName = "Warning: no key name is specified";
      flowControllerKeyGUI.setToolTipText(
              "Use Source tab below to specify a key name " +
              "in the <flowController> element, or the imported <flowController>");
    } else
      keyName = fcd.getKey();
    flowControllerKeyGUI.setText(keyName);
    Import fcdImport = fcd.getImport();
    String fileName = null;
    if (null != fcdImport) {
      fileName = fcdImport.getLocation();
      if (null == fileName || (0 == fileName.length()))
        fileName = fcdImport.getName();
    }
    flowControllerGUI.setText(null == fileName ? 
            "Warning: no <import> in <flowController>" : fileName);
    flowControllerChoice.setText(USER_DEFINED_FLOW);
    // must follow label updates
    // because this method also does the redraw
    // otherwise, label updates are not redrawn...
    flowControllerGUI.getParent().redraw();
  }

  private void enableFlowControllerGUI(boolean enableState) {
    flowControllerLabel.setEnabled(enableState);
    flowControllerGUI.setEnabled(enableState);
    specifyFlowControllerImportButton.setEnabled(enableState);
    findFlowControllerDescriptorButton.setEnabled(enableState);
    flowControllerKeyLabel.setEnabled(enableState);
    // flowControllerSpecifierLabel.setEnabled(enableState);
    flowControllerKeyGUI.setEnabled(enableState);
    // if (!enableState) {
    // flowControllerKeyGUI.setText("");
    // flowControllerGUI.setText("");
    // }
  }

  /**
   * @return
   */
  private FlowConstraints getModelFlow() {
    return getAnalysisEngineMetaData().getFlowConstraints();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.widget == flowControllerChoice) {
      String flowTypeGUI = flowControllerChoice.getText();
      if (null == flowTypeGUI || flowTypeGUI.equals(S_))
        return;

      String prevFlowTypeGUI;
      FlowControllerDeclaration fcd = getFlowControllerDeclaration();
      FlowConstraints modelFlow = getModelFlow();
      if (null != fcd)
        prevFlowTypeGUI = USER_DEFINED_FLOW;
      else {
        if (null == modelFlow)
          prevFlowTypeGUI = "";
        else {
          String prevFlowType = modelFlow.getFlowConstraintsType();
          if (CapabilityLanguageFlow.FLOW_CONSTRAINTS_TYPE.equals(prevFlowType))
            prevFlowTypeGUI = CAPABILITY_LANGUAGE_FLOW;
          else
            prevFlowTypeGUI = FIXED_FLOW;
        }
      }

      if (prevFlowTypeGUI.equals(flowTypeGUI))
        return;

      CapabilityLanguageFlow clf = null;
      FixedFlow ff = null;

      String[] nodes = new FlowNodes(modelFlow).getFlow();

      if (flowTypeGUI.equals(CAPABILITY_LANGUAGE_FLOW)) {
        setFlowControllerDeclaration(null);
        getAnalysisEngineMetaData().setFlowConstraints(clf = new CapabilityLanguageFlow_impl());
        clf.setCapabilityLanguageFlow(nodes);
      } else if (flowTypeGUI.equals(FIXED_FLOW)) {
        setFlowControllerDeclaration(null);
        getAnalysisEngineMetaData().setFlowConstraints(ff = new FixedFlow_impl());
        ff.setFixedFlow(nodes);
      } else if (flowTypeGUI.equals(USER_DEFINED_FLOW)) {
        // use case: user set user-defined-flow from some other state.
        // user will have to fill in the import (CDE only supports that format)
        // flow nodes kept, put under "Fixed Flow" arbitrarily
        FlowControllerDeclaration newFcd = new FlowControllerDeclaration_impl();
        setFlowControllerDeclaration(newFcd);
        refreshFcd(newFcd);
        getAnalysisEngineMetaData().setFlowConstraints(ff = new FixedFlow_impl());
        ff.setFixedFlow(nodes);
      }
      enable();
      setFileDirty();
    }

    else if (event.widget == upButton) {
      String[] nodes = new FlowNodes(getModelFlow()).getFlow();
      // update both model and gui: swap nodes
      int selection = flowList.getSelectionIndex();
      if (selection == 0)
        return; // can't move up 0
      String temp = nodes[selection - 1];
      nodes[selection - 1] = nodes[selection];
      nodes[selection] = temp;
      finishFlowAction();
      flowList.setSelection(selection - 1);
      enable(); // after setting selection
    } else if (event.widget == downButton) {
      String[] nodes = new FlowNodes(getModelFlow()).getFlow();
      // update both model and gui: swap nodes
      int selection = flowList.getSelectionIndex();
      if (selection == flowList.getItemCount() - 1)
        return; // can't move down at end of list
      String temp = nodes[selection + 1];
      nodes[selection + 1] = nodes[selection];
      nodes[selection] = temp;
      finishFlowAction();
      flowList.setSelection(selection + 1);
      enable(); // after setting selection
    } else if (event.widget == specifyFlowControllerImportButton) {
      handleSpecifyFlowController();
    } else if (event.widget == findFlowControllerDescriptorButton) {
      handleFindFlowController();
    } else if (event.widget == flowControllerGUI && event.type == SWT.MouseDown
            && event.button == 3) {
      handleContextMenuRequest(event);
    } else if (event.widget == flowControllerGUI && event.type == SWT.MouseHover
            && !bDisableToolTipHelp) {
      handleHoverHelp(event);
    }

    // handle selection
    else if (event.widget == flowList) {
      if (event.type == SWT.Selection) {
        enable();
      }

      if (event.character == SWT.DEL) {
        if (flowList.getSelectionIndex() > -1) {
          handleRemove();
        }
      }
    }
  }

  private void handleHoverHelp(Event event) {
    String sDesc = "";
    FlowControllerDeclaration fcd = editor.getResolvedFlowControllerDeclaration();
    String text = flowControllerGUI.getText();
    if (null != fcd && !"".equals(text) && null != fcd.getSpecifier()) {
      sDesc = getDescriptionForDescriptor(flowControllerGUI.getText(), fcd.getSpecifier());
    }
    flowControllerGUI.setToolTipText(sDesc);
  }

  private void handleContextMenuRequest(Event event) {
    Import imp = getFlowControllerDeclaration().getImport();

    bDisableToolTipHelp = true;
    requestPopUpOverImport(imp, flowControllerGUI, event);
    bDisableToolTipHelp = false;
  }

  private void finishFlowAction() {
    setFileDirty();
    refresh(); // calls enable
  }

  public void handleRemove() {
    // get node to remove
    int removedItemIndex = flowList.getSelectionIndex();
    String[] nodes = new FlowNodes(getModelFlow()).getFlow();
    int origItemCount = nodes.length;
    // remove node from array by copying additional items above down 1
    for (int i = removedItemIndex + 1; i < origItemCount; i++) {
      nodes[i - 1] = nodes[i];
    }
    // copy array in a smaller one
    String newNodes[] = new String[origItemCount - 1];
    if (newNodes.length > 0) {
      System.arraycopy(nodes, 0, newNodes, 0, newNodes.length);
    }

    new FlowNodes(getModelFlow()).setFlow(newNodes);

    finishFlowAction(); // does also enable() and refresh()
    flowList.setSelection(removedItemIndex - 1);
    enable(); // after setting selection
  }

  /**
   * Adds a node to the flowList
   * 
   * @param node
   *          the key of the delegate
   */
  public void addNode(String node) {
    FlowConstraints flowConstraints = getModelFlow();
    if (null == flowConstraints) {
      // no constraints declared
      // set up Fix Flow style of contraints
      //   This can happen if the style is user-defined flow
      flowConstraints = UIMAFramework.getResourceSpecifierFactory().createFixedFlow();
      getAnalysisEngineMetaData().setFlowConstraints(flowConstraints);
    }
    FlowNodes flowNodes = new FlowNodes(flowConstraints);
    String[] nodes = flowNodes.getFlow();

    if (nodes == null) {
      nodes = new String[] { node };
      flowNodes.setFlow(nodes);
    } else {
      // create a new String array and copy old Strings
      String newNodes[] = new String[nodes.length + 1];
      System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
      newNodes[newNodes.length - 1] = node;
      flowNodes.setFlow(newNodes);
    }
    finishFlowAction(); // setFileDirty, enable() and setFocus()
  }

  /**
   * Enables and disables section, enables and disables buttons after content.
   * 
   */
  public void enable() {

    // if annotator is primitive disable whole section
    if (!isAggregate()) {
      upButton.setEnabled(false);
      downButton.setEnabled(false);
      flowChoiceLabel.setEnabled(false);
      flowControllerChoice.setEnabled(false);
      enableFlowControllerGUI(false);
    } else {
      FlowControllerDeclaration fcd = getFlowControllerDeclaration();
      enableFlowControllerGUI(null != fcd);
      flowControllerChoice.setEnabled(true);
      flowChoiceLabel.setEnabled(true);
      int items = flowList.getItemCount();
      int selection = flowList.getSelectionIndex();

      if (items == 0 || selection == -1) {
        editor.getAggregatePage().getAggregateSection().getRemoveFromFlowButton().setEnabled(false);
        upButton.setEnabled(false);
        downButton.setEnabled(false);

      } else {
        editor.getAggregatePage().getAggregateSection().getRemoveFromFlowButton().setEnabled(true);
        // disable if first item is selected
        upButton.setEnabled(!(selection == 0));
        // disable if last item is selected
        downButton.setEnabled(!(selection == items - 1));
      }
    }

  }

  /**
   * Proofs if a node is contained in the list of nodes
   * 
   * @param node
   * @return whether the node is in the list or not
   */
  public boolean containsNode(String node) {
    String[] nodes = new FlowNodes(getModelFlow()).getFlow();
    if (null == nodes)
      return false;

    for (int i = 0; i < nodes.length; i++) {
      if (node.equals(nodes[i]))
        return true;
    }
    return false;
  }

  /**
   * @return
   */
  public Button getDownButton() {
    return downButton;
  }

  /**
   * @return
   */
  public Table getFlowList() {
    return flowList;
  }

  /**
   * @return
   */
  public Button getUpButton() {
    return upButton;
  }

  public void removeAll() {
    FlowConstraints flow = getModelFlow();
    if (flow != null) {
      flow.setAttributeValue("fixedFlow", new String[0]); //$NON-NLS-1$
      editor.setFileDirty();
      editor.getAggregatePage().getAggregateSection().refresh();
    }
  }

  private static final String[] flowControllerHeadersLC = new String[] { "<flowcontrollerdescription" }; // don't

  // end
  // in a
  // blank
  // -
  // could
  // mismatch
  // a
  // cr/lf

  private void handleFindFlowController() {
    FindComponentDialog dialog1 = new FindComponentDialog(this, "Find a Flow Controller",
            "Specify a name pattern and/or other constraints, and then push the Search button",
            flowControllerHeadersLC);
    if (Window.CANCEL == dialog1.open())
      return;

    List matchingDelegateComponentDescriptors = dialog1.getMatchingDelegateComponentDescriptors();
    List matchingDelegateComponentDescriptions = dialog1.getMatchingDelegateComponentDescriptions();

    if (matchingDelegateComponentDescriptors.size() == 0) {
      Utility.popMessage("No matching Components",
              "There are no Components matching your search criteria.", MessageDialog.ERROR);
      return;
    }

    PickTaeForTypesDialog dialog2 = new PickTaeForTypesDialog(this, editor.getFile().getName(),
            matchingDelegateComponentDescriptors, matchingDelegateComponentDescriptions);
    if (Window.CANCEL == dialog2.open())
      return;

    String[] selectedDelegateComponentDescriptors = dialog2
            .getSelectedDelegateComponentDescriptors();

    if (selectedDelegateComponentDescriptors == null
            || selectedDelegateComponentDescriptors.length == 0) {
      return;
    }
    if (checkForOneSelection(selectedDelegateComponentDescriptors.length)) {
      String fileName = selectedDelegateComponentDescriptors[0].replace('\\', '/');
      int nLastSlashLoc = fileName.lastIndexOf('/');
      String shortName;
      if (nLastSlashLoc == -1) {
        shortName = fileName;
      } else {
        shortName = fileName.substring(nLastSlashLoc + 1);
      }
      produceKeyAddFlowController(shortName,
              editor.getFullPathFromDescriptorRelativePath(fileName), dialog2.isImportByName);
    }
  }

  private void produceKeyAddFlowController(String shortName, String fullPathFileName,
          boolean isImportByName) {
    Import imp = createImport(fullPathFileName, isImportByName);

    // key is shortName plus a suffix if needed to make it unique.
    // The set of existing keys is obtained from the model,
    // not from the GUI table (as was the case in earlier design)
    FlowControllerDeclaration oldFcd = getFlowControllerDeclaration();
    setFlowControllerDeclaration(null);

    String keyName = produceUniqueComponentKey(shortName);
    if (null == keyName) {
      setFlowControllerDeclaration(oldFcd); // revert
      return;
    }

    XMLizable inputDescription = readImport(imp, fullPathFileName, isImportByName);
    if (null == inputDescription) {
      setFlowControllerDeclaration(oldFcd); // revert
      return;
    }

    if (!(inputDescription instanceof FlowControllerDescription)) {
      Utility.popMessage("Invalid kind of descriptor", MessageFormat.format(
              "Operation cancelled: The descriptor ''{0}'' being added is not a FlowController.",
              new Object[] { maybeShortenFileName(fullPathFileName) }), MessageDialog.ERROR);
      setFlowControllerDeclaration(oldFcd); // revert
      return;
    }

    FlowControllerDeclaration fcd = new FlowControllerDeclaration_impl();
    fcd.setKey(keyName);
    fcd.setImport(imp);
    setFlowControllerDeclaration(fcd);

    // before adding the import, see if the merge type system is OK
    if (!isValidAggregateChange()) {
      setFlowControllerDeclaration(oldFcd); // revert
      return;
    }

    finishAggregateChangeAction();
    editor.addDirtyTypeName("<Aggregate>"); // force running jcasgen
    refresh(); // refresh every time to capture the order of items added
  }

  private void handleSpecifyFlowController() {
    MultiResourceSelectionDialog dialog = new MultiResourceSelectionDialog(getSection().getShell(),
            editor.getFile().getProject().getParent(), "Flow Controller Selection", editor
                    .getFile().getLocation(), editor);
    dialog.setTitle("Flow Controller Selection");
    dialog.setMessage("Select a Flow Controller descriptor from the workspace:");
    dialog.open();
    Object[] files = dialog.getResult();

    if (files != null && checkForOneSelection(files.length)) {
      FileAndShortName fsn = new FileAndShortName(files[0]);
      produceKeyAddFlowController(fsn.shortName, fsn.fileName, dialog.isImportByName);
    }
  }

  private boolean checkForOneSelection(int numberSelected) {
    if (numberSelected > 1) {
      Utility.popMessage("Error - Multiple selection",
              "Only one Flow Controller can be selected. Please try again.", MessageDialog.ERROR);
      return false;
    }
    return true;
  }

}
