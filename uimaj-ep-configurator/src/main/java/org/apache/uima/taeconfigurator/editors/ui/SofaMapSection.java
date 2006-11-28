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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.EditSofaBindingsDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

public class SofaMapSection extends AbstractSection {

  private Composite sectionClient;

  private Button addButton;

  private Button editButton;

  private Button removeButton;

  private Tree tree;

  private static final String INPUTS = "Inputs";

  private static final String OUTPUTS = "Outputs";

  private static final boolean INPUT = true;

  private static final boolean OUTPUT = false;

  private static final String titleMsg = "This section shows all defined Sofas for an Aggregate and their mappings to the component Sofas.\n"
          + "Add Aggregate Sofa Names using the Capabilities section; Select an Aggregate Sofa Name and Add/Edit mappings for that Sofa in this section.\n";

  public SofaMapSection(MultiPageEditor aEditor, Composite parent) {
    super(aEditor, parent, "Sofa Mappings (Only used in aggregate Descriptors)", titleMsg);
  }

  /*
   * Called by the page constructor after all sections are created, to initialize them.
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */
  public void initialize(IManagedForm form) {

    super.initialize(form);
    sectionClient = new2ColumnComposite(getSection());

    tree = newTree(sectionClient);
    Composite buttonContainer = newButtonContainer(sectionClient);
    addButton = newPushButton(buttonContainer, S_ADD, "Click here to add a component Sofa binding.");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);

    tree.addListener(SWT.MouseDoubleClick, this); // edit gesture

    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);
    if (!isAggregate())
      getSection().setExpanded(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();
    tree.removeAll();
    if (!isAggregate()) {
      getSection().setText("Sofa Mappings (Only used in aggregate Descriptors)");
      getSection().setDescription("This part is only used for Aggregate Descriptors");
      getSection().setExpanded(false);
    } else {
      getSection().setDescription(titleMsg);

      String[][] sns = getCapabilitySofaNames();
      String[] inputSofaNames = sns[0];
      String[] outputSofaNames = sns[1];
      // getAggrSofas the names declared in the capability
      // plus any undeclared names in the mappings
      // sorted... alphabetically
      String[] inputAggrSofas = getAggrSofas(inputSofaNames, outputSofaNames);

      fillMap(inputAggrSofas, INPUT);
      fillMap(outputSofaNames, OUTPUT);
      tree.setSelection(new TreeItem[] { tree.getItems()[0] });

      if (0 == (inputAggrSofas.length + outputSofaNames.length)) {
        getSection().setText("Sofa Mappings (No Sofas are defined)");
        getSection().setExpanded(false);
      } else {
        getSection().setText("Sofa Mappings");
        getSection().setExpanded(true);
      }

    }
    enable();
  }

  private String[] getAggrSofas(String[] inputCapabilityNames, String[] outputCapabilityNames) {
    SofaMapping[] allMappings = getSofaMappings();
    Set names = new TreeSet();
    Set undeclaredNames = new TreeSet();
    if (null != inputCapabilityNames)
      for (int i = 0; i < inputCapabilityNames.length; i++)
        names.add(inputCapabilityNames[i]);
    if (null != allMappings) {
      for (int i = 0; i < allMappings.length; i++) {
        String sofaName = allMappings[i].getAggregateSofaName();
        if (0 > Arrays.binarySearch(inputCapabilityNames, sofaName)
                && 0 > Arrays.binarySearch(outputCapabilityNames, sofaName))
          undeclaredNames.add(sofaName);
      }
    }

    // It is an error to have a mapping without having the aggregate name
    // declared as either an input our output. If the name is not
    // declared, (silently) consider it to have been an input.
    if (undeclaredNames.size() > 0)
      names.addAll(undeclaredNames);

    return (String[]) names.toArray(stringArray0);
  }

  private void fillMap(String[] aggrKeys, boolean isInput) {

    TreeItem d = new TreeItem(tree, SWT.NONE);
    d.setText(isInput ? INPUTS : OUTPUTS);
    for (int i = 0; i < aggrKeys.length; i++) {
      TreeItem a = new TreeItem(d, SWT.NONE);
      a.setText(aggrKeys[i]);
      fillBindings(a, aggrKeys[i]);
      a.setExpanded(true);
    }
    d.setExpanded(true);
  }

  private void fillBindings(TreeItem parent, String aggrSofa) {
    // bindings are a string of key-name / sofa-name or "<default>"
    String[] bindings = getSofaBindingsForAggrSofa(aggrSofa);
    for (int j = 0; j < bindings.length; j++) {
      TreeItem b = new TreeItem(parent, SWT.NONE);
      b.setText(bindings[j]);
    }
  }

  private String[] getSofaBindingsForAggrSofa(String aggrSofa) {
    SofaMapping[] sofaMappings = getSofaMappings();
    if (null == sofaMappings)
      return stringArray0;
    Set bindings = new TreeSet();
    for (int i = 0; i < sofaMappings.length; i++) {
      SofaMapping sofaMapping = sofaMappings[i];
      if (sofaMapping.getAggregateSofaName().equals(aggrSofa))
        if (null != sofaMapping.getComponentSofaName()
                && !"".equals(sofaMapping.getComponentSofaName()))
          bindings.add(sofaMapping.getComponentKey() + '/' + sofaMapping.getComponentSofaName());
        else
          bindings.add(sofaMapping.getComponentKey());
    }
    String[] results = (String[]) bindings.toArray(stringArray0);
    Arrays.sort(results);
    return results;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    // Note: to add aggrSofa names, use capabilitySection.
    // Updates there are propagated here.

    // only enabled when one existing AggrSofa
    // or its child is selected
    if (event.widget == editButton || event.type == SWT.MouseDoubleClick) {
      if (!editButton.getEnabled())
        return;
      // Edit a map: a new aggr capability + a delegate sofa, or
      // a delegate sofa to an existing map.
      TreeItem selected = tree.getSelection()[0];
      TreeItem parent = selected.getParentItem();
      if (null != parent.getParentItem()) {
        selected = parent;
      }
      editAggrMap(selected);
    } else if (event.widget == addButton) {
      // Add one or more new mappings
      TreeItem selected = tree.getSelection()[0];
      TreeItem parent = selected.getParentItem();
      if (null != parent.getParentItem()) {
        selected = parent;
      }
      addAggrMap(selected);
    } else if (event.widget == removeButton) {
      // only enabled for aggr or component
      TreeItem selected = tree.getSelection()[0];
      TreeItem parent = selected.getParentItem();
      if (null == parent.getParentItem())
        removeAggr(selected);
      else
        removeComponentFromAggr(selected);
    }
    enable();
  }

  private static final boolean AVAIL_ONLY = true;

  private void editAggrMap(TreeItem selected) {
    // pop up window: shows all available component mappings
    // plus current mappings for this aggrSofa
    // Available: a) not mapped
    // User selects mappings to update
    // update model: add (multiple) mappings
    // Remove all under item
    // update model.
    String aggrSofa = selected.getText();
    Map availAndBoundSofas = getAvailAndBoundSofas(aggrSofa, !AVAIL_ONLY);
    if (availAndBoundSofas.size() == 0) {
      Utility
              .popMessage(
                      "No available sofas",
                      "Because there are no sofas in the delegates that are not already bound, no sofa mapping can be created.",
                      MessageDialog.WARNING);
      return;
    }

    EditSofaBindingsDialog dialog = new EditSofaBindingsDialog(this, aggrSofa, availAndBoundSofas);
    if (dialog.open() == Window.CANCEL)
      return;
    removeAggr(aggrSofa);
    addAggr(aggrSofa, dialog.selectedSofaNames);
    removeChildren(selected);
    fillBindings(selected, aggrSofa);
    selected.setExpanded(true);
    setFileDirty();
  }

  private void addAggrMap(TreeItem selected) {
    // pop up window: shows all available component mappings
    // minus current mappings for this aggrSofa
    // Available: a) not mapped,
    // User selects mappings to add
    String aggrSofa = selected.getText();
    Map availAndBoundSofas = getAvailAndBoundSofas(aggrSofa, AVAIL_ONLY);
    if (availAndBoundSofas.size() == 0) {
      Utility
              .popMessage(
                      "No available sofas",
                      "Because there are no sofas in the delegates that are not already bound, no sofa mapping can be created.",
                      MessageDialog.WARNING);
      return;
    }

    EditSofaBindingsDialog dialog = new EditSofaBindingsDialog(this, aggrSofa, availAndBoundSofas);
    if (dialog.open() == Window.CANCEL)
      return;
    addAggr(aggrSofa, dialog.selectedSofaNames);
    removeChildren(selected);
    fillBindings(selected, aggrSofa);
    selected.setExpanded(true);
    setFileDirty();
  }

  private void addSofasToAllComponentSofaMap(Map allComponentSofas, String key,
          ResourceSpecifier delegate, boolean isInput) {
    // delegates can be AnalysisEngines, CasConsmers, flowControllers, or remotes
    if (delegate instanceof AnalysisEngineDescription || delegate instanceof CasConsumerDescription
            || delegate instanceof FlowControllerDescription) {
      Set[] inAndOut = getCapabilitySofaNames((ResourceCreationSpecifier) delegate, key);
      Set inOut = inAndOut[isInput ? 0 : 1];
      if (!isInput) { // Aggr "output" can be mapped to delegate "input"
        inOut.addAll(inAndOut[0]);
      }
      if (inOut.size() == 0) {
        // no sofas defined in this delegate
        // create default sofa
        allComponentSofas.put(key, null);
      }
      for (Iterator i2 = inOut.iterator(); i2.hasNext();) {
        allComponentSofas.put(i2.next(), null);
      }
    }
  }

  /**
   * 
   * @param aggrSofa
   * @return a Map, keys = component/sofaname, value = aggrsofa or null
   */
  private Map getAvailAndBoundSofas(String aggrSofa, boolean availOnly) {
    boolean isInput = isInput(aggrSofa);
    Map allComponentSofas = new TreeMap(); // key = component/sofa, value = AggrSofa bound to

    // put all delegate component/sofa items in a Map
    for (Iterator it = editor.getResolvedDelegates().entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      addSofasToAllComponentSofaMap(allComponentSofas, (String) entry.getKey(),
              (ResourceSpecifier) entry.getValue(), isInput);
    }
    // pick up any sofa info from flow controller
    FlowControllerDeclaration fcd = editor.getResolvedFlowControllerDeclaration();
    if (null != fcd) {
      addSofasToAllComponentSofaMap(allComponentSofas, fcd.getKey(), fcd.getSpecifier(), isInput);
    }

    // mark the bound ones with a value which is the aggr sofa they're bound to
    // also, add any that were not spec'd in the delegates (because
    // for instance, they were remote)
    SofaMapping[] sofaMappings = getSofaMappings();
    for (int i = 0; i < sofaMappings.length; i++) {
      SofaMapping sm = sofaMappings[i];
      String key = sm.getComponentKey();
      if (null != sm.getComponentSofaName())
        key = key + '/' + sm.getComponentSofaName();
      if (availOnly)
        allComponentSofas.remove(key);
      else
        allComponentSofas.put(key, sm.getAggregateSofaName());
    }

    // remove all that are bound to other Aggr sofa names
    // because although an Aggr sofa can be bound to many delegate sofas,
    // a delegate sofa can only be bound to one aggr one.

    for (Iterator i3 = allComponentSofas.entrySet().iterator(); i3.hasNext();) {
      Map.Entry entry = (Map.Entry) i3.next();
      String boundAggrSofa = (String) entry.getValue();
      if (null != boundAggrSofa && !boundAggrSofa.equals(aggrSofa))
        i3.remove();
    }

    return allComponentSofas;
  }

  private boolean isInput(String sofaName) {
    String[][] sns = getCapabilitySofaNames();
    for (int i = 0; i < sns[0].length; i++) {
      if (sofaName.equals(sns[0][i]))
        return true;
    }
    return false;
  }

  private void addAggr(String aggrSofa, String[] sofaNames) {
    SofaMapping[] newSofas = new SofaMapping[sofaNames.length];
    for (int i = 0; i < sofaNames.length; i++) {
      newSofas[i] = UIMAFramework.getResourceSpecifierFactory().createSofaMapping();
      newSofas[i].setAggregateSofaName(aggrSofa);
      newSofas[i].setComponentKey(getComponentOnly(sofaNames[i]));
      newSofas[i].setComponentSofaName(getSofaOnly(sofaNames[i]));
    }
    SofaMapping[] oldSofas = getSofaMappings();
    SofaMapping[] result = new SofaMapping[oldSofas.length + newSofas.length];
    System.arraycopy(oldSofas, 0, result, 0, oldSofas.length);
    System.arraycopy(newSofas, 0, result, oldSofas.length, newSofas.length);
    editor.getAeDescription().setSofaMappings(result);
  }

  private void removeAggr(String aggrSofa) {
    Comparator comparator = new Comparator() {
      public int compare(Object aggrSofaName, Object o2) {
        SofaMapping sofaMapping = (SofaMapping) o2;
        if (sofaMapping.getAggregateSofaName().equals(aggrSofaName))
          return 0;
        else
          return -1;
      }
    };
    editor.getAeDescription().setSofaMappings(
            (SofaMapping[]) Utility.removeElementsFromArray(getSofaMappings(), aggrSofa,
                    SofaMapping.class, comparator));
  }

  private void removeAggr(TreeItem selected) {
    if (Window.CANCEL == Utility
            .popOkCancel(
                    "Confirm delete of sofa mappings",
                    "Please confirm deletion of all sofa mappings for this Aggregate Sofa name.  Note this will not delete the Sofa name.  To do that, remove the name from the Component Capabilities panel (the other panel on this page).",
                    MessageDialog.WARNING))
      return;
    removeAggr(selected.getText());
    removeChildren(selected);
    setFileDirty();
  }

  /**
   * Removes a delegate map from a particular aggr sofa mapping.
   * 
   * @param selected
   */
  private void removeComponentFromAggr(TreeItem selected) {
    final String aggrName = selected.getParentItem().getText();
    Comparator comparator = new Comparator() {
      public int compare(Object componentAndSofa, Object o2) {
        SofaMapping sofaMapping = (SofaMapping) o2;
        if (!sofaMapping.getAggregateSofaName().equals(aggrName))
          return -1;
        String component = getComponentOnly((String) componentAndSofa);
        if (!sofaMapping.getComponentKey().equals(component))
          return -1;
        String sofa = getSofaOnly((String) componentAndSofa);
        if (null == sofa || sofa.equals(""))
          if (null == sofaMapping.getComponentSofaName()
                  || "".equals(sofaMapping.getComponentSofaName()))
            return 0;
          else
            return -1;
        else if (sofa.equals(sofaMapping.getComponentSofaName()))
          return 0;
        else
          return -1;
      }
    };

    editor.getAeDescription().setSofaMappings(
            (SofaMapping[]) Utility.removeElementsFromArray(getSofaMappings(), selected.getText(),
                    SofaMapping.class, comparator));
    selected.dispose();
    setFileDirty();
  }

  /**
   * Called when removing a delegate from the aggr. Removes from the sofaMappings, any and all
   * mappings associated with the delegate.
   * 
   * @param componentSofa
   */
  public static void removeSofaMappings(String componentKey, ResourceSpecifier delegate,
          MultiPageEditor pEditor) {
    if (delegate instanceof AnalysisEngineDescription || delegate instanceof CasConsumerDescription) {
      Set[] inOut = getCapabilitySofaNames((ResourceCreationSpecifier) delegate, componentKey);
      inOut[0].addAll(inOut[1]);
      final Set allDelegateComponentSofas = inOut[0];
      Comparator comparator = new Comparator() {
        public int compare(Object ignore, Object elementOfArray) {
          SofaMapping sofaMapping = (SofaMapping) elementOfArray;
          String key = sofaMapping.getComponentKey();
          if (null != sofaMapping.getComponentSofaName())
            key = key + '/' + sofaMapping.getComponentSofaName();
          if (allDelegateComponentSofas.contains(key)) {
            return 0;
          }
          return -1;
        }
      };

      pEditor.getAeDescription().setSofaMappings(
              (SofaMapping[]) Utility.removeElementsFromArray(getSofaMappings(pEditor), null,
                      SofaMapping.class, comparator));
    }
  }

  private String getSofaOnly(String componentAndSofa) {
    int locOfSlash = componentAndSofa.indexOf('/');
    if (locOfSlash < 0)
      return null;
    return componentAndSofa.substring(locOfSlash + 1);
  }

  private String getComponentOnly(String componentAndSofa) {
    int locOfSlash = componentAndSofa.indexOf('/');
    if (locOfSlash < 0)
      return componentAndSofa;
    return componentAndSofa.substring(0, locOfSlash);
  }

  public void enable() {
    boolean oneSelected = tree.getSelectionCount() == 1;
    boolean topLevelSelected = false;
    if (oneSelected) {
      TreeItem selected = tree.getSelection()[0];
      topLevelSelected = (null == selected.getParentItem());
    }
    addButton.setEnabled(oneSelected && !topLevelSelected);
    editButton.setEnabled(oneSelected && !topLevelSelected);
    removeButton.setEnabled(oneSelected && !topLevelSelected);
  }

}
