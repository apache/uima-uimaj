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

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddRemoteServiceDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.FindComponentDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.PickTaeForTypesDialog;
import org.apache.uima.taeconfigurator.files.MultiResourceSelectionDialogWithFlowOption;
import org.apache.uima.taeconfigurator.model.FlowNodes;
import org.apache.uima.util.XMLizable;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;

public class AggregateSection extends AbstractSection {

  private Table filesTable;

  private Button addButton;

  private Button addRemoteButton;

  private Button findAnalysisEngineButton;

  private Button removeButton;

  private Button addToFlowButton;

  private Button removeFromFlowButton;

  public Button getRemoveFromFlowButton() {
    return removeFromFlowButton;
  }

  private boolean bDisableToolTipHelp = false;

  /**
   * Creates a section for aggregate specifiers to add their delegates
   * 
   * @param editor
   *          backpointer to the main multipage editor
   * @param parent
   *          the Composite where this section lives
   */
  public AggregateSection(MultiPageEditor aEditor, Composite parent) {
    super(aEditor, parent, "Component Engines",
            "The following engines are included in this aggregate.");
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

    // Table Container has table and buttons on bottom
    Composite tableContainer = newComposite(sectionClient);
    enableBorders(tableContainer);
    toolkit.paintBordersFor(tableContainer);

    filesTable = newTable(tableContainer, SWT.FULL_SELECTION, 150);

    filesTable.setHeaderVisible(true);

    newTableColumn(filesTable, 50, SWT.LEFT, "Delegate");
    newTableColumn(filesTable, 75, SWT.LEFT, "Key Name");

    // This little code fragment is an attempt to get the right sizing for the buttons
    // Was wrong on Mac platforms
    //   Word below is the longer of the two words in the button container
    Button tempForSize = toolkit.createButton(tableContainer, "Remove", SWT.PUSH);
    Point p = tempForSize.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    tempForSize.dispose();
    
    Composite bottomButtonContainer = newButtonContainer(tableContainer, HORIZONTAL_BUTTONS, 3* p.x);

    addButton = newPushButton(bottomButtonContainer, S_ADD,
            "Click here to add a locally defined AE or CAS Consumer delegate", ENABLED);
    removeButton = newPushButton(bottomButtonContainer, "Remove",
            "Click here to remove the selected item", ENABLED);

    Composite sideButtonContainer = newButtonContainer(sectionClient, VERTICAL_BUTTONS, 80);

    // this next just serves as a spacer
    spacer(sideButtonContainer);
    spacer(sideButtonContainer);

    addToFlowButton = newPushButton(sideButtonContainer, ">>",
            "Click here to add the selected item to the flow", !ENABLED);
    removeFromFlowButton = newPushButton(sideButtonContainer, "<<",
            "Click here to remove the selected item from the flow", !ENABLED);

    spacer(sideButtonContainer);

    addRemoteButton = newPushButton(sideButtonContainer, "AddRemote",
            "Click here to add a Remote Analysis Engine", ENABLED);
    findAnalysisEngineButton = newPushButton(sideButtonContainer, "Find AE",
            "Click here to search for an Analysis Engine", ENABLED);

    addButton.setSize(removeButton.getSize());

    filesTable.addListener(SWT.MouseDown, this);
    filesTable.addListener(SWT.MouseHover, this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();

    // if annotator is primitive - page should be disabled
    if (!isAggregate()) {
      filesTable.removeAll();
      getSection().setText("Not Used");
      getSection().setDescription("This section is only applicable for Aggregate descriptors.");
    } else {
      getSection().setText("Component Engines");
      getSection().setDescription("The following engines are included in this aggregate.");

      // these can be changed by direct editing of source
      Map delegates = getDelegateAnalysisEngineSpecifiersWithImports();

      // first clear list
      // (we do this carefully to preserve order)
      String[] priorOrderedKeys = new String[filesTable.getItemCount()];
      for (int i = 0; i < priorOrderedKeys.length; i++) {
        priorOrderedKeys[i] = filesTable.getItem(i).getText(1);
      }
      filesTable.removeAll();
      // get delegate keys
      HashSet keys = new HashSet();
      if (delegates != null) {
        keys.addAll(delegates.keySet());
      }

      // first add keys that we know about in order as we knew it
      for (int i = 0; i < priorOrderedKeys.length; i++) {
        if (keys.contains(priorOrderedKeys[i])) {
          Object o = delegates.get(priorOrderedKeys[i]);
          if (o instanceof Import)
            addFile(o, priorOrderedKeys[i]);
          keys.remove(priorOrderedKeys[i]);
        }
      }

      Iterator itKeys = keys.iterator();
      // add what's left to list
      while (itKeys.hasNext()) {
        String key = (String) itKeys.next();
        Object o = delegates.get(key);
        if (o instanceof Import)
          addFile(o, key);
      }
      packTable(filesTable);
    }
    enable();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.widget == addButton)
      handleAdd();
    else if (event.widget == removeButton
            || (event.type == SWT.KeyUp && event.character == SWT.DEL))
      handleRemove();
    else if (event.widget == addToFlowButton)
      handleAddToFlow();
    else if (event.widget == removeFromFlowButton)
      handleRemoveFromFlow();
    else if (event.widget == addRemoteButton)
      handleAddRemote();
    else if (event.widget == findAnalysisEngineButton)
      handleFindAnalysisEngine();

    // actions on table
    else if (event.widget == filesTable) {
      if (event.type == SWT.Selection) {
        // if no delegate is selected disable edit and remove
        boolean bEnableButtons = (filesTable.getSelectionCount() > 0);
        removeButton.setEnabled(bEnableButtons);
        addToFlowButton.setEnabled(bEnableButtons);
      } else if (event.type == SWT.MouseDown && 
                  (event.button == 3  || 
                          // this is for Macintosh - they just have one button
                   (event.button == 1 && (0 != (event.stateMask & SWT.CTRL))))) {
        handleTableContextMenuRequest(event);
      } else if (event.type == SWT.MouseHover && !bDisableToolTipHelp) {
        handleTableHoverHelp(event);
      } 
      // Don't need this. Next mouse hover kills tool tip anyway
      // else if (event.type == SWT.MouseMove) {
      // filesTable.setToolTipText("");
      // }
    }
  }

  private void handleAdd() {

    MultiResourceSelectionDialogWithFlowOption dialog = new MultiResourceSelectionDialogWithFlowOption(
            getSection().getShell(), editor.getFile().getProject().getParent(),
            "Component Engine Selection", editor.getFile().getLocation(), editor);
    dialog.setTitle("Component Engine Selection");
    dialog.setMessage("Select one or more component engines from the workspace:");
    dialog.open();
    Object[] files = dialog.getResult();

    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        FileAndShortName fsn = new FileAndShortName(files[i]);
        produceKeyAddDelegate(fsn.shortName, fsn.fileName, dialog.getAutoAddToFlow(),
                dialog.isImportByName);
      }
    }
  }

  private void produceKeyAddDelegate(String shortName, String fullPathFileName, boolean addToFlow,
          boolean isImportByName) {
    boolean bSuccess = false;

    // key is shortName plus a suffix if needed to make it unique.
    // The set of existing keys is obtained from the model,
    // not from the GUI table (as was the case in earlier design)

    String keyName = produceUniqueComponentKey(shortName);
    if (null != keyName) {
      bSuccess = addDelegate(fullPathFileName, shortName, keyName, isImportByName);

      if (bSuccess) {
        editor.addDirtyTypeName("<Aggregate>"); // force running jcasgen
        refresh(); // refresh every time to capture the order of items added
        if (addToFlow) {
          addNodeToFlow(keyName);
        }
      }
    }
  }

  private void handleRemove() {
    // get the keyName to remove
    int nSelectionIndex = filesTable.getSelectionIndex();
    String key = filesTable.getItem(nSelectionIndex).getText(1);

    // if delegate is still on flow list warn that it may be removed
    if (editor.getAggregatePage().getFlowSection().containsNode(key)) {
      String sCascadeDeleteTitle = "Cascade delete warning";
      String sCascadeDeleteMessage = "This will cause a cascading deletion of an associated input, output, index, or type priority.  Ok to continue?";
      boolean bContinue = MessageDialog.openConfirm(getSection().getShell(), sCascadeDeleteTitle,
              sCascadeDeleteMessage);
      if (!bContinue) {
        return;
      }
    }
    ResourceSpecifier delegate = (ResourceSpecifier) editor.getResolvedDelegates().get(key);
    // remove the selected delegate from delegate list

    Map delegatesWithImport = editor.getAeDescription()
            .getDelegateAnalysisEngineSpecifiersWithImports();
    Object savedDelegate1 = delegatesWithImport.get(key);
    delegatesWithImport.remove(key);

    Object savedDelegate2 = getDelegateAnalysisEngineSpecifiersWithImports().get(key);
    getDelegateAnalysisEngineSpecifiersWithImports().remove(key);

    // update the model: flow lists: remove the item from the flow list
    // This has to be done before validation - otherwise get validation error.
    // Support undo

    FlowNodes flow = new FlowNodes(getAnalysisEngineMetaData().getFlowConstraints());
    String[] savedFlowNodes = flow.getFlow();
    if (null == savedFlowNodes) 
      savedFlowNodes = stringArray0;

    // item may be in the flow 0, 1 or more times

    List nodes = new ArrayList(savedFlowNodes.length);
    for (int i = 0; i < savedFlowNodes.length; i++) {
      String flowNode = savedFlowNodes[i];
      if (!flowNode.equals(key)) {
        nodes.add(flowNode);
      }
    }
    flow.setFlow((String[]) nodes.toArray(stringArray0));

    if (!isValidAggregateChange()) {
      getDelegateAnalysisEngineSpecifiersWithImports().put(key, savedDelegate2);
      delegatesWithImport.put(key, savedDelegate1);
      flow.setFlow(savedFlowNodes);
      return;
    }

    Map typeNameHash = editor.allTypes.get();

    boolean bInputsChanged = !editor.validateInputs(typeNameHash);
    boolean bOutputsChanged = !editor.validateOutputs(typeNameHash);

    if (bInputsChanged || bOutputsChanged) {
      String msg = "Some of the following are no longer valid and have been deleted (or appropriately altered): \n\n";
      if (bInputsChanged) {
        msg += "Inputs \n";
      }
      if (bOutputsChanged) {
        msg += "Outputs \n";
      }
      Utility.popMessage("Capabilities Changed", msg, MessageDialog.INFORMATION);
    }
    SofaMapSection.removeSofaMappings(key, delegate, editor);
    editor.getAggregatePage().getFlowSection().refresh();
    if (filesTable.getItemCount() > nSelectionIndex) {
      filesTable.setSelection(nSelectionIndex);
      filesTable.setFocus();
      enable();
    }
    refresh();
    finishAggregateChangeAction();

    // remove still must handle removal of parameters and param settings
    // removed delegate (if params dont appear elsewhere)
  }

  private void handleAddToFlow() {
    String node = filesTable.getSelection()[0].getText(1);
    addNodeToFlow(node);
    getTable().setSelection(-1);
    enable();
    Table flowList = editor.getAggregatePage().getFlowSection().getFlowList();
    flowList.setSelection(flowList.getItemCount() - 1);
    editor.getAggregatePage().getFlowSection().enable();
    flowList.setFocus();
  }

  /**
   * @param node
   */
  private void addNodeToFlow(String node) {
    FlowSection fs = editor.getAggregatePage().getFlowSection();
    fs.addNode(node);
//    fs.refresh();  // the fs.addNode does a refresh
  }

  private void handleRemoveFromFlow() {
    FlowSection fs = editor.getAggregatePage().getFlowSection();
    String selectedKey = fs.getFlowList().getSelection()[0].getText();
    fs.handleRemove();
    for (int i = 0; i < getTable().getItemCount(); i++) {
      String thisKey = getTable().getItem(i).getText(1);
      if (selectedKey.equals(thisKey)) {
        getTable().setSelection(i);
        enable();
        getTable().setFocus();
        break;
      }
    }
  }

  private final static String REMOTE_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
          + "<uriSpecifier xmlns=\"http://uima.apache.org/resourceSpecifier\">\n"
          + "  <resourceType>{0}</resourceType>\n" + // AnalysisEngine CasConsumer
          "  <uri>{1}</uri> \n" + // sURI
          "  <protocol>{2}</protocol>\n" + // SOAP or Vinci
          "  <timeout>{3}</timeout>" + "  {4}" + // <parameters> for VNS </parameters>
          "\n</uriSpecifier>";

  private void handleAddRemote() {
    String sDescriptorPath = editor.getFile().getParent().getLocation().toString() + '/';
    AddRemoteServiceDialog dialog = new AddRemoteServiceDialog(this, sDescriptorPath);
    dialog.open();
    if (dialog.getReturnCode() == InputDialog.CANCEL)
      return;

    String sServiceType = dialog.getSelectedServiceTypeName();
    if (sServiceType != null && !sServiceType.equals("SOAP") && !sServiceType.equals("Vinci")) {
      return;
    }
    String sURI = dialog.getSelectedUri();
    String sKey = dialog.getSelectedKey();

    if (!isNewKey(sKey)) {
      Utility.popMessage("Duplicate Key", "You have specified a duplicate key.  Please try again.",
              MessageDialog.ERROR);
      return;
    }

    PrintWriter printWriter = setupToPrintFile(dialog.genFilePath);
    if (null != printWriter) {
      String vnsHostPort = "";
      if (dialog.vnsHost.length() > 0) {
        vnsHostPort = MessageFormat.format("    <parameter name=\"VNS_HOST\" value=\"{0}\"/>\n",
                new Object[] { dialog.vnsHost });
      }
      if (dialog.vnsPort.length() > 0) {
        vnsHostPort += MessageFormat.format("    <parameter name=\"VNS_PORT\" value=\"{0}\"/>\n",
                new Object[] { dialog.vnsPort });
      }
      if (vnsHostPort.length() > 0)
        vnsHostPort = "\n  <parameters>" + vnsHostPort + "  </parameters>";
      printWriter.println(MessageFormat.format(REMOTE_TEMPLATE, new Object[] { dialog.aeOrCc, sURI,
          sServiceType, dialog.timeout, vnsHostPort }));
      printWriter.close();

      boolean bSuccess = addDelegate(dialog.genFilePath, sKey, sKey, dialog.isImportByName);
      if (bSuccess) {
        boolean bAutoAddToFlow = dialog.getAutoAddToFlow();
        if (bAutoAddToFlow) {
          addNodeToFlow(sKey);
        }
        refresh();
      }
    }
  }

  private static final String[] delegateComponentStringHeadersLC = new String[] {
      "<analysisenginedescription", "<casconsumerdescription", "<taedescription" };

  private void handleFindAnalysisEngine() {
    FindComponentDialog dialog1 = new FindComponentDialog(
            this,
            "Find an Analysis Engine (AE), CAS Consumer, or Remote Service Descriptor",
            "Specify a name pattern and/or additional constraints, and then push the Search button",
            delegateComponentStringHeadersLC);
    if (Window.CANCEL == dialog1.open())
      return;

    List matchingDelegateComponentDescriptors = dialog1.getMatchingDelegateComponentDescriptors();
    List matchingDelegateComponentDescriptions = dialog1.getMatchingDelegateComponentDescriptions();

    if (matchingDelegateComponentDescriptors.size() == 0) {
      Utility.popMessage("No matching Delegate Components",
              "There are no Delegate Components matching your search criteria.",
              MessageDialog.ERROR);
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

    for (int i = 0; i < selectedDelegateComponentDescriptors.length; i++) {
      String fileName = selectedDelegateComponentDescriptors[i].replace('\\', '/');
      int nLastSlashLoc = fileName.lastIndexOf('/');
      String shortName;
      if (nLastSlashLoc == -1) {
        shortName = fileName;
      } else {
        shortName = fileName.substring(nLastSlashLoc + 1);
      }
      produceKeyAddDelegate(shortName, editor.getFullPathFromDescriptorRelativePath(fileName),
      // dialog2.getAutoAddToFlow(),
              true, dialog2.isImportByName);
    }
    finishAggregateChangeAction();
  }

  private void handleTableContextMenuRequest(Event event) {
    TableItem item = filesTable.getItem(new Point(event.x, event.y));
    if (null == item) {
      return;
    }

    String thisKey = item.getText(1);
    Import imp = (Import) getDelegateAnalysisEngineSpecifiersWithImports().get(thisKey);

    bDisableToolTipHelp = true;
    requestPopUpOverImport(imp, filesTable, event);
    bDisableToolTipHelp = false;
  }

  private void handleTableHoverHelp(Event event) {
    TableItem item = filesTable.getItem(new Point(event.x, event.y));
    String sDesc = "";
    if (null != item) {
      Map dels = editor.getResolvedDelegates();
      if (null != dels) {
        sDesc = getDescriptionForDescriptor(item.getText(0), (ResourceSpecifier) dels.get(item
                .getText(1)));
      }
    }
    filesTable.setToolTipText(sDesc);
  }

  private boolean addDelegate(String fileName, String shortName, String keyName,
          boolean isImportByName) {
    Import imp;
    Map delegatesWithImport = getDelegateAnalysisEngineSpecifiersWithImports();

    // if the delegate is not a remote, read / parse it and add ae to
    // the delegate hash map
    // -- also add the import
    // If it is a remote - try and get it's metadata (we can if it is running)

    // first: create import, needed in both cases
    imp = createImport(fileName, isImportByName);

    // read the content and merge into our model
    XMLizable inputDescription = readImport(imp, fileName, isImportByName);
    if (null == inputDescription)
      return false;

    if (!(inputDescription instanceof AnalysisEngineDescription)
            && !(inputDescription instanceof CasConsumerDescription)
            && !(inputDescription instanceof URISpecifier)) {
      Utility
              .popMessage(
                      "Invalid kind of descriptor",
                      MessageFormat
                              .format(
                                      "Operation cancelled: The descriptor ''{0}'' being added is not an Analysis Engine or a CAS Consumer or a Remote Service.",
                                      new Object[] { maybeShortenFileName(fileName) }),
                      MessageDialog.ERROR);
      return false;
    }

    editor.getResolvedDelegates().put(keyName, inputDescription);
    delegatesWithImport.put(keyName, imp);

    // before adding the import, see if the merge type system is OK
    if (!isValidAggregateChange()) {
      // revert
      editor.getResolvedDelegates().remove(keyName);
      delegatesWithImport.remove(keyName);
      return false;
    }

    finishAggregateChangeAction();
    return true;
  }

  private boolean isNewKey(String keyName) {
    for (int i = 0; i < filesTable.getItemCount(); i++) {
      if (filesTable.getItem(i).getText(1).equals(keyName)) {
        return false;
      }
    }
    return true;
  }

  public void addParametersForDelegate(AnalysisEngineDescription tae) {
    ConfigurationParameter[] candidateNewParams = tae.getAnalysisEngineMetaData()
            .getConfigurationParameterDeclarations().getConfigurationParameters();

    NameValuePair[] candidateSettings = tae.getAnalysisEngineMetaData()
            .getConfigurationParameterSettings().getParameterSettings();

    ConfigurationParameter[] oldParams = getAnalysisEngineMetaData()
            .getConfigurationParameterDeclarations().getConfigurationParameters();

    NameValuePair[] oldSettings = getAnalysisEngineMetaData().getConfigurationParameterSettings()
            .getParameterSettings();

    if (candidateNewParams == null || candidateNewParams.length == 0) {
      return;
    }

    if (oldParams == null || oldParams.length == 0) {
      getAnalysisEngineMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(candidateNewParams);
      getAnalysisEngineMetaData().getConfigurationParameterSettings().setParameterSettings(
              candidateSettings);
    } else {
      // first do parameters
      Vector newParams = new Vector();
      for (int i = 0; i < candidateNewParams.length; i++) {
        boolean bNew = true;
        for (int j = 0; j < oldParams.length; j++) {
          if (candidateNewParams[i].getName().equals(oldParams[j].getName())
                  && candidateNewParams[i].getType().equals(oldParams[j].getType())) {
            bNew = false;
          }
        }
        if (bNew) {
          newParams.add(candidateNewParams[i]);
        }
      }

      ConfigurationParameter[] newPlusOldParams = new ConfigurationParameter[oldParams.length
              + newParams.size()];
      for (int i = 0; i < oldParams.length; i++) {
        newPlusOldParams[i] = oldParams[i];
      }
      for (int i = 0; i < newParams.size(); i++) {
        newPlusOldParams[oldParams.length + i] = (ConfigurationParameter) newParams.elementAt(i);
      }
      getAnalysisEngineMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(newPlusOldParams);

      // next do settings
      Vector newSettings = new Vector();
      if (candidateSettings != null) {
        for (int i = 0; i < candidateSettings.length; i++) {
          boolean bNew = true;
          for (int j = 0; j < oldSettings.length; j++) {
            if (candidateSettings[i].getName().equals(oldSettings[j].getName())) {
              bNew = false;
            }
          }
          if (bNew) {
            newSettings.add(candidateSettings[i]);
          }
        }
      }

      NameValuePair[] newPlusOldSettings = new NameValuePair[oldSettings.length
              + newSettings.size()];
      for (int i = 0; i < oldSettings.length; i++) {
        newPlusOldSettings[i] = oldSettings[i];
      }
      for (int i = 0; i < newSettings.size(); i++) {
        newPlusOldSettings[oldSettings.length + i] = (NameValuePair) newSettings.elementAt(i);
      }
      getAnalysisEngineMetaData().getConfigurationParameterSettings().setParameterSettings(
              newPlusOldSettings);
    }
  }

  public void enable() {
    boolean isPrimitive = isPrimitive();
    boolean bEnable = (filesTable.getSelectionIndex() > -1);

    addButton.setEnabled(!isPrimitive);
    addRemoteButton.setEnabled(!isPrimitive);
    findAnalysisEngineButton.setEnabled(!isPrimitive);
    removeButton.setEnabled(bEnable);
    addToFlowButton.setEnabled(bEnable);
  }

  /**
   * adds a tableItem to the table
   * 
   * @param fileName
   * @param keyName
   */
  private void addFile(Object o, String keyName) {
    Import impItem = (Import) o;
    String fileName = impItem.getLocation();
    if (null == fileName || (0 == fileName.length()))
      fileName = impItem.getName();
    // create new TableItem
    TableItem item = new TableItem(filesTable, SWT.NONE);
    item.setImage(TAEConfiguratorPlugin.getImage(TAEConfiguratorPlugin.IMAGE_ANNOTATOR));
    item.setText(0, fileName);
    item.setText(1, keyName);
  }

  public Table getTable() {
    return filesTable;
  }

}
