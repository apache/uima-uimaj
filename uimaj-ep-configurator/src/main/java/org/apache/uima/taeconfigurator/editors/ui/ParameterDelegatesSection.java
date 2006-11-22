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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.model.ConfigGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

public class ParameterDelegatesSection extends AbstractSectionParm {

  private Composite sectionClient;

  private ParameterSection parmSection;

  private boolean createNonSharedOverride;

  private Button createOverrideButton;

  private Button createNonSharedOverrideButton;

  public ParameterDelegatesSection(MultiPageEditor editor, Composite parent) {
    super(
                    editor,
                    parent,
                    "Delegate Component Parameters",
                    "This section shows all delegate components by their Key names, and what parameters they have.\nDouble-click a parameter or a group if you want to specify overrides for these parameters in this aggregate; this will add a default Configuration Parameter in this Aggregate for that parameter, and set the overrides.");
  }

  /*
   * Called by the page constructor after all sections are created, to initialize them.
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */
  public void initialize(IManagedForm form) {

    parmSection = editor.getParameterPage().getParameterSection();

    super.initialize(form);
    sectionClient = newComposite(getSection());

    tree = newTree(sectionClient);
    Composite buttonContainer = new2ColumnComposite(sectionClient);
    ((GridData) buttonContainer.getLayoutData()).grabExcessVerticalSpace = false;
    createOverrideButton = newPushButton(buttonContainer, "Create Override",
                    "Click here to create a new override for this parameter");
    createNonSharedOverrideButton = newPushButton(buttonContainer, "Create non-shared Override",
                    "Click here to create a non-shared override for this parameter");

    tree.addListener(SWT.MouseDoubleClick, this); // edit gesture
    tree.addListener(SWT.MouseHover, this); // hover

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
    parmSection = editor.getParameterPage().getParameterSection();

    tree.removeAll();
    if (!isAggregate()) {
      getSection().setText("Not Used");
      getSection().setDescription("This part is only used for Aggregate Descriptors");
    } else {
      getSection().setText("Delegate Component Parameters");
      getSection()
                      .setDescription(
                                      "This section shows all delegate components by their Key names, and what parameters they have.\nDouble-click a parameter or a group if you want to specify overrides for these parameters in this aggregate; this will add a default Configuration Parameter in this Aggregate for that parameter, and set the overrides.");

      cpd = getAnalysisEngineMetaData().getConfigurationParameterDeclarations();
      for (Iterator it = editor.getResolvedDelegates().entrySet().iterator(); it.hasNext();) {
        addDelegateToGUI((Map.Entry) it.next());
      }
      FlowControllerDeclaration fcd = editor.getResolvedFlowControllerDeclaration();
      if (null != fcd) {
        addDelegateToGUI(fcd.getKey(), fcd.getSpecifier());
      }
      TreeItem[] items = tree.getItems();
      if (items.length > 0)
        // scrolls to top, also
        tree.setSelection(new TreeItem[] { items[0] });
    }
    enable();
  }

  private void addDelegateToGUI(Map.Entry entry) {
    addDelegateToGUI((String) entry.getKey(), (ResourceSpecifier) entry.getValue());
    // String key = (String) entry.getKey();
    // ResourceSpecifier delegate = (ResourceSpecifier)entry.getValue();
    // if (delegate instanceof AnalysisEngineDescription || delegate instanceof
    // CasConsumerDescription) {
    // TreeItem d = new TreeItem(tree, SWT.NONE);
    // d.setText(DELEGATE_HEADER + key);
    // d.setData(key);
    // addDelegateGroupsToGUI(d, (ResourceCreationSpecifier)delegate);
    // d.setExpanded(true);
    // }
  }

  private void addDelegateToGUI(String key, ResourceSpecifier delegate) {
    if (delegate instanceof AnalysisEngineDescription || delegate instanceof CasConsumerDescription
                    || delegate instanceof FlowControllerDescription) {
      TreeItem d = new TreeItem(tree, SWT.NONE);
      d.setText(((delegate instanceof FlowControllerDescription) ? FLOWCTLR_HEADER
                      : DELEGATE_HEADER)
                      + key);
      d.setData(key);
      addDelegateGroupsToGUI(d, (ResourceCreationSpecifier) delegate);
      d.setExpanded(true);
    }
  }

  private void addDelegateGroupsToGUI(TreeItem parent, ResourceCreationSpecifier delegate) {
    ConfigurationParameterDeclarations cpd1 = delegate.getMetaData()
                    .getConfigurationParameterDeclarations();
    // if (delegate instanceof AnalysisEngineDescription)
    // cpd1 = ((AnalysisEngineDescription)delegate)
    // .getAnalysisEngineMetaData().getConfigurationParameterDeclarations();
    // else if (delegate instanceof CasConsumerDescription)
    // cpd1 = ((CasConsumerDescription)delegate)
    // .getMetaData().getConfigurationParameterDeclarations();
    // else
    // throw new InternalErrorCDE("Invalid state");

    ConfigGroup noGroup = new ConfigGroup(cpd1, ConfigGroup.NOT_IN_ANY_GROUP);
    ConfigGroup commonGroup = new ConfigGroup(cpd1, ConfigGroup.COMMON);

    addDelegateGroupToGUI(parent, noGroup);
    addDelegateGroupToGUI(parent, commonGroup);
    ConfigurationGroup[] cgs = cpd1.getConfigurationGroups();
    if (cgs != null) {
      for (int i = 0; i < cgs.length; i++) {
        addDelegateGroupToGUI(parent, new ConfigGroup(cpd1, cgs[i]));
      }
    }
  }

  private void addDelegateGroupToGUI(TreeItem parent, ConfigGroup cg) {
    ConfigurationParameter[] cps = cg.getConfigParms();
    if (null != cps && cps.length > 0) {
      TreeItem d = new TreeItem(parent, SWT.NONE);
      d.setData(cg);
      setGroupText(d, cg.getName());
      addDelegateParmsToGUI(d, cps);
      d.setExpanded(true);
    }
  }

  private void addDelegateParmsToGUI(TreeItem parent, ConfigurationParameter[] cps) {
    if (null != cps) {
      for (int i = 0; i < cps.length; i++) {
        TreeItem d = new TreeItem(parent, SWT.NONE);
        d.setData(cps[i]);
        d.setText(parmGuiString(cps[i]));
        String[] overrides = cps[i].getOverrides();
        if (null != overrides && overrides.length > 0) {
          addDelegateParmOverridesToGUI(d, overrides);
          d.setExpanded(true);
        }
      }
    }
  }

  private void addDelegateParmOverridesToGUI(TreeItem parent, String[] overrides) {
    for (int i = 0; i < overrides.length; i++) {
      TreeItem d = new TreeItem(parent, SWT.NONE);
      d.setText(OVERRIDE_HEADER + overrides[i]);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.type == SWT.MouseHover) {
      showDescriptionAsToolTip(event);
    } else if (event.type == SWT.MouseDoubleClick) {
      addOverrides(0 != (event.stateMask & SWT.SHIFT));
    } else if (event.widget == createOverrideButton) {
      addOverrides(false);
    } else if (event.widget == createNonSharedOverrideButton) {
      addOverrides(true);
    }
  }

  private void addOverrides(boolean nonShared) {
    TreeItem item = tree.getSelection()[0];
    createNonSharedOverride = nonShared;
    if (isParameter(item))
      addNewParameter(item);
    else if (isGroup(item))
      addAllParameters(item.getItems());
    else if (isDelegate(item))
      addAllGroups(item.getItems());
  }

  public ConfigurationParameter getConfigurationParameterFromTreeItem(TreeItem item) {
    return (ConfigurationParameter) item.getData();
  }

  public ConfigGroup getConfigGroupFromTreeItem(TreeItem item) {
    return (ConfigGroup) item.getData();
  }

  private String getKeyNameFromTreeItem(TreeItem item) {
    return (String) item.getData();
  }

  private void addNewParameter(TreeItem item) {
    addNewParameter(getConfigurationParameterFromTreeItem(item), getConfigGroupFromTreeItem(item
                    .getParentItem()), getKeyNameFromTreeItem(item.getParentItem().getParentItem()));
  }

  private void addAllParameters(TreeItem[] items) {
    for (int i = 0; i < items.length; i++) {
      addNewParameter(items[i]);
    }
  }

  private void addAllGroups(TreeItem[] items) {
    for (int i = 0; i < items.length; i++) {
      addAllParameters(items[i].getItems());
    }
  }

  private void addNewParameter(ConfigurationParameter parm, ConfigGroup delegateGroup, String key) {

    ConfigGroup group = getCorrespondingModelGroup(delegateGroup);

    if (null == group) {
      group = parmSection.addGroup(delegateGroup);
    }
    ConfigurationParameter parmInGroup;
    String override = key + "/" + parm.getName();
    String overrideParmName;
    if (null != (overrideParmName = getOverridingParmName(override, cpd))) {
      Utility.popMessage("Only one override allowed",
                      "This delegate parameter already is being overridden by '" + overrideParmName
                                      + "'.  To override "
                                      + "with a different parameter, first remove this override",
                      MessageDialog.ERROR);
      return;
    }
    if (null != (parmInGroup = getSameNamedParmInGroup(parm, group))) {
      if ((!createNonSharedOverride) && parmSpecMatches(parm, parmInGroup)) {

        if (0 <= getOverrideIndex(parmInGroup, override)) {
          return; // trying to add existing override
        } else {
          parmSection.addOverride(parmInGroup, override);
        }
      } else {
        String newName = generateUniqueName(parm.getName());
        parmSection.addParm(newName, parm, group, override);
      }
    } else {
      if (ParameterSection.parameterNameAlreadyDefinedNoMsg(parm.getName(),
                      getConfigurationParameterDeclarations())) {
        // parm names must be unique across this descriptor, even among different groups
        String newName = generateUniqueName(parm.getName());
        parmSection.addParm(newName, parm, group, override);
      } else {
        parmSection.addParm(parm.getName(), parm, group, override);
      }
    }
  }

  public static String getOverridingParmName(String override, ConfigurationParameterDeclarations cpd) {
    String result;

    if (null != (result = getOverridingParmName(override, cpd.getConfigurationParameters())))
      return result;
    if (null != (result = getOverridingParmName(override, cpd.getCommonParameters())))
      return result;
    ConfigurationGroup[] groups = cpd.getConfigurationGroups();
    if (null != groups)
      for (int i = 0; i < groups.length; i++) {
        if (null != (result = getOverridingParmName(override, groups[i]
                        .getConfigurationParameters())))
          return result;
      }
    return null;
  }

  private static String getOverridingParmName(String override, ConfigurationParameter[] cps) {
    if (null != cps)
      for (int i = 0; i < cps.length; i++) {
        String[] overrides = cps[i].getOverrides();
        if (null != overrides)
          for (int j = 0; j < overrides.length; j++) {
            if (override.equals(overrides[j]))
              return cps[i].getName();
          }
      }
    return null;
  }

  /**
   * Add a suffix to the name to make it unique within all parameters defined for the cpd
   * 
   * @param name
   * @return
   */
  private String generateUniqueName(String name) {
    List allNames = new ArrayList();
    addParmNames(allNames, cpd.getConfigurationParameters());
    addParmNames(allNames, cpd.getCommonParameters());
    ConfigurationGroup[] cgs = cpd.getConfigurationGroups();
    if (null != cgs) {
      for (int i = 0; i < cgs.length; i++) {
        addParmNames(allNames, cgs[i].getConfigurationParameters());
      }
    }
    int suffix = 1;
    String nameTry = name + suffix;
    while (allNames.contains(nameTry))
      nameTry = name + ++suffix;
    return nameTry;
  }

  private void addParmNames(List list, ConfigurationParameter[] parms) {
    if (null != parms) {
      for (int i = 0; i < parms.length; i++) {
        list.add(parms[i].getName());
      }
    }
  }

  private int getOverrideIndex(ConfigurationParameter parm, String override) {
    String[] overrides = parm.getOverrides();
    if (null == overrides)
      return -1;
    for (int i = 0; i < overrides.length; i++) {
      if (overrides[i].equals(override))
        return i;
    }
    return -1;
  }

  private ConfigurationParameter getSameNamedParmInGroup(ConfigurationParameter parm,
                  ConfigGroup group) {
    ConfigurationParameter[] cps = group.getConfigParms();
    String parmName = parm.getName();
    for (int i = 0; i < cps.length; i++) {
      if (cps[i].getName().equals(parmName))
        return cps[i];
    }
    return null;
  }

  private boolean parmSpecMatches(ConfigurationParameter p, ConfigurationParameter q) {
    if (!p.getType().equals(q.getType()))
      return false;
    if (p.isMandatory() != q.isMandatory())
      return false;
    if (p.isMultiValued() != q.isMultiValued())
      return false;
    return true;
  }

  private ConfigGroup getCorrespondingModelGroup(ConfigGroup delegateGroup) {
    switch (delegateGroup.getKind()) {
      case ConfigGroup.NOT_IN_ANY_GROUP:
        return new ConfigGroup(cpd, ConfigGroup.NOT_IN_ANY_GROUP);
      case ConfigGroup.COMMON:
        return getCorrespondingModelGroup(getAllGroupNames(delegateGroup.getCPD()));
      case ConfigGroup.NAMED_GROUP:
        return getCorrespondingModelGroup(delegateGroup.getNameArray());
    }
    throw new InternalErrorCDE("invalid state");
  }

  private ConfigGroup getCorrespondingModelGroup(String[] nameArray) {
    ConfigurationGroup[] cgs = cpd.getConfigurationGroups();
    for (int i = 0; i < cgs.length; i++) {
      if (setEquals(cgs[i].getNames(), nameArray)) {
        return new ConfigGroup(cpd, cgs[i]);
      }
    }
    return null;
  }

  /**
   * Compare for set equals, assuming sets have no duplicates
   * 
   * @param a
   * @param b
   * @return
   */
  private boolean setEquals(Object[] a, Object[] b) {
    if (null == a && null == b)
      return true;
    if (null == a || null == b)
      return false;
    if (a.length != b.length)
      return false;
    for (int i = 0; i < a.length; i++) {
      boolean foundB = false;
      for (int j = 0; j < b.length; j++) {
        if (a[i].equals(b[j])) {
          foundB = true;
          break;
        }
      }
      if (!foundB)
        return false;
    }
    return true;
  }

  public void enable() {
    createOverrideButton.setEnabled(tree.getSelectionCount() == 1);
    createNonSharedOverrideButton.setEnabled(tree.getSelectionCount() == 1);
  }

}
