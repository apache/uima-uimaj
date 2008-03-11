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

/*
 * Created on 15.07.2003
 * 
 * Redesign Feb 7 2005 Redesign Feb 9 2005 Changed Description to hover style Feb 27
 * 
 * Add <Not in any group> Two case: primitive and aggregate Primitive: allow groups to be defined,
 * including <Not in any group> <Common> Allow multi def of parmnames (with same decls) as long as
 * they're in different groups. Same name not allowed in both <Common> and named group but are
 * allowed in both <Common> and <Not in any group>
 * 
 * A parm name declared in different groups which don't overlap can have a different declaration.
 * 
 * Parm names are unique within a within the group-set formed by the union of all groups where the
 * parm is defined. This means that a parm name P1 in group g1, g2 must match the definition of a
 * parm name P1 in group g2, g3.
 * 
 * Two parm names with the same name, in different non-intersecting groups, are distinct.
 * 
 * Two parm names with the same name, in different delegates, even if in the same group, are
 * distinct. They can have different definitions. They can be seperately overridden in each
 * delegate. This allows separate development of primitives, with future combining.
 * 
 * Two parm names with the same name in the same primitive that is used twice in the delegates list
 * are distinct. They can be separately overridden, with different values. In this case, though, the
 * definitions (type, multi-value, mandatory) are the same.
 * 
 * Aggregate: groups are the union of immediate delegate groups, except <Common> Common at lower
 * level defines parms that are in all named groups at that level.
 * 
 * Treatment of <Common>: <Common> in a delegate is translated into G1, G2, ... Gn (all groups
 * defined in that delegate).
 * 
 * Common at aggregate level defines parms that are in all named groups at aggregate level. This is
 * a bigger set of group names than exist at any lower delegate level. Note: Cannot add or remove
 * groups at aggregate level: the groups are pre-defined based on the delegates
 * 
 * Check of the model done at first refresh to validate it. If model has extra groups (not in
 * delegates) - these are removed. If model is missing groups (in delegates) - these are added.
 * 
 * 2 panel design for aggregate. Right panel is tree, delegate-keys, groups, parms Left panel is
 * like primitive panel.
 * 
 * Operations: Adding an override: double click on right panel element. click on delegate: add all
 * parms all groups click on group: add group and all parms click on parm: add parm
 * 
 * On left panel: add override - adds additional override. Edit - applies to override - change the
 * override. Remove - applies to override, to param, to group
 * 
 * When adding overriding parm for a delegate, use as a default the same name as the name being
 * overridden. Change name if there exists parm in the groups with same name but different def
 * Change name if there exists parm but group membership is not equal.
 * 
 * User may change the name in any case. If user does this, check for name collision as above. This
 * is useful, for instance, when more than one delegate defines that parameter name - to allow each
 * delegate to have a different override.
 * 
 * Adding a parm under a group: set default, allow overtyping - auto fill in type, mandatory, and
 * multivalue from picked parm name setting override: based on what was clicked
 * 
 * OK ToDo: When adding a parm, if it is an aggregate, include <overrides> OK ToDo; When adding a
 * parm, have custom dialog, set multivalued, mandatory, and 1st override OK ToDo: add button add
 * override (for case: aggregate) OK ToDo: make remove work for override, and edit too Later: ToDo:
 * For overrides: lookup key names, lookup parm names in delegates. If delegate is another
 * aggregate, do additional key All this done as optional - in case sub things not yet defined. What
 * about a "browse" like file system? for descending thru aggr to parm?
 * 
 * Todos: MaybeNot 1) make model for all parmDefs following other models MaybeNot 2) make model for
 * all parmSettings following other models 2a) Have all changes (add, edit, and remove) affect
 * ParameterSettingsSection 2aa) For Edit of each kind including changing name consider effect on
 * settings OK 3) Checking: if change type - what to do about existing setting? Message it will be
 * deleted OK 4) Checking: if change MultiV - same as above. OK 5) Checking: overrides: only show
 * parm names which match on type and mv and mandatory < override OK 6) Model for editing overrides:
 * using same dialog, plus combo to select which override, autoselected when dbl-clicking / edit
 * existing , or adding new. Make base parm info read-only to avoid accidents NO 7) detail window:
 * for groups: has list of group names; line per name, editable. for parm: has description; editable
 * for overrides: has override string; line per segment, editable. OK 8) Checking: overrides: last
 * level must have parm style match. 9) add value fixups and change awareness; use change awareness
 * to trigger model updates OK 10) add name character-set checking OK 11) Fix shash for borders, for
 * initial ratio, move to HeaderSection. Note: not appropriate for things with centered import box
 * on bottom.
 * 
 * March 24 2005 - finish impl of overrides for aggregates: Disallow editing of parameters or adding
 * parameters (parameters only to be created via the double-click on the delegate parameter).
 * 
 * Have the double-click on the deletage handle Common properly by setting up a special group with
 * all the groups for that delegate.
 * 
 * Allow adding additional overrides. Do in pickOverrides dialog.
 * 
 * 
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.impl.ConfigurationGroup_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddParameterDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.CommonInputDialog;
import org.apache.uima.taeconfigurator.files.PickOverrideKeysAndParmName;
import org.apache.uima.taeconfigurator.model.ConfigGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

public class ParameterSection extends AbstractSectionParm {

  public final static int NEW_OVERRIDE = -1;

  public final static boolean REMOVE_FROM_GUI = true;

  public final static boolean GIVE_WARNING_MESSAGE = true;

  private Text defaultGroup;

  private CCombo searchStrategy;

  private Button addButton;

  private Button addGroupButton;

  private Button editButton;

  private Button removeButton;

  private Button usingGroupsButton;

  private Composite groupingControl;

  private boolean firstTime = true;

  /**
   * Creates a section to show a list of all parameters
   * 
   * @param editor
   *          backpointer to the main multipage editor
   * @param parent
   *          the Composite where this section lives
   */
  public ParameterSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Configuration Parameters",
            "This section shows all configuration parameters defined for this engine.");
  }

  // This page shows the configuration parameters
  // Organization: top section for switching between simple and groups, and
  // for groups: default Group name, + search strat (combo)
  // Main body: a tree, 2 hierarchies:
  // 1) for groups
  // 2) for aggregates: overrides
  // Tree is one of 3 entries:
  // parm entry: multi/single, req/opt, boolean/string/int/float, name
  // group entry: <Group> , , , names
  // (aggregates) <Overrides> key/key/.../key/parmname
  // 
  // Checking: parmNames are unique for primitive and each aggr level
  // overrides: lookup valid keys and parmnames
  // names: well-formed with proper char sets, like java pkg names
  //
  // Editing: via double click, or edit button + select.
  // using same dialog as for create
  //
  // Tree and corresponding tree (if created) in ParameterSettings are
  // both incrementally updated, so rebuild is not needed. This preserves
  // user-specified expansion of nodes.

  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = newComposite(getSection());

    usingGroupsButton = newCheckBox(sectionClient, "Use Parameter Groups",
            "Check this box if Groups are being used with Parameters");

    groupingControl = new2ColumnComposite(sectionClient);
    ((GridData) groupingControl.getLayoutData()).grabExcessVerticalSpace = false;
    enableBorders(groupingControl);
    toolkit.paintBordersFor(groupingControl);

    defaultGroup = newLabeledTextField(groupingControl, "Default Group",
            "Specify the name of the default group.");
    newLabelWithData(groupingControl, "SearchStrategy");
    searchStrategy = newCComboWithTip(groupingControl, "SearchStrategyToolTip");
    searchStrategy.add("language_fallback");
    searchStrategy.add("default_fallback");
    searchStrategy.add("none");

    // main table + buttons on left

    Composite tableContainer = new2ColumnComposite(sectionClient);
    enableBorders(tableContainer);
    toolkit.paintBordersFor(tableContainer);

    // SWT.SINGLE to support deselecting
    // SWT.FULL_SELECTION to select whole row
    parameterSectionTree = tree = newTree(tableContainer);

    // Buttons
    Composite buttonContainer = newButtonContainer(tableContainer);
    addButton = newPushButton(buttonContainer, S_ADD, "Click here to add a new parameter");
    addGroupButton = newPushButton(buttonContainer, "AddGroup",
            "Click here to add a group specification.  A group specification names one or more group names.");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);

    tree.addListener(SWT.MouseDoubleClick, this); // for Editing
    tree.addListener(SWT.MouseHover, this); // for Description
  }

  /*
   * refresh() called when UI is stale with respect to the model. Updates the UI to be in sync with
   * the model Not called when model updated thru UI interaction. Called initially, and called when
   * switching from sourcePage because user may have arbitrarilly changed things. (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */

  public void refresh() {
    super.refresh(); // clears stale and dirty bits in AbstractFormPart
    // superclass

    cpd = getAnalysisEngineMetaData().getConfigurationParameterDeclarations();

    if ((firstTime && isParmGroup()) || isAggregate()
            || (!firstTime && usingGroupsButton.getSelection())) {
      usingGroupsButton.setSelection(true);
      groupingControl.setVisible(true);

      defaultGroup.setText(convertNull(cpd.getDefaultGroupName()));
      if (null == cpd.getSearchStrategy())
        cpd.setSearchStrategy("language_fallback");
      searchStrategy.setText(cpd.getSearchStrategy());
    } else {
      groupingControl.setVisible(false);
      usingGroupsButton.setSelection(false);
    }
    firstTime = false;
    showOverrides = true;
    splitGroupNames = false;
    clearAndRefillTree(usingGroupsButton.getSelection());

    tree.setSelection(new TreeItem[] { tree.getItems()[0] });
    enable();

    // sync settings page to catch use case of switching from sourceEditor
    // to this page after having shown settings page - may be out of date
    ParameterSettingsSection settingsSection = editor.getSettingsPage()
            .getParameterSettingsSection();

    if (null != settingsSection) {
      setSettings(settingsSection);
      settings.refresh();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    cpd = getAnalysisEngineMetaData().getConfigurationParameterDeclarations();

    if (event.type == SWT.MouseHover) {
      showDescriptionAsToolTip(event);
    } else if (event.widget == usingGroupsButton) {
      if (usingGroupsButton.getSelection()) {
      } else {
        if (Window.CANCEL == Utility
                .popOkCancel(
                        "Confirm Remove Groups",
                        "This action will delete any group information that may be present in this descriptor.  Proceed?",
                        MessageDialog.WARNING)) {
          usingGroupsButton.setSelection(true);
          return;
        }
        // remove all top level items (groups) except the 1st one,
        // which is NOT_IN_ANY_GROUP
        TreeItem[] items = tree.getItems();
        TreeItem[] removeItems = new TreeItem[items.length - 1];
        System.arraycopy(items, 1, removeItems, 0, removeItems.length);
        removeItems(removeItems, !GIVE_WARNING_MESSAGE);
        cpd.setCommonParameters(AbstractSection.configurationParameterArray0);
        cpd.setConfigurationGroups(AbstractSection.configurationGroupArray0);
      }
      setFileDirty(); // maybe slightly overkill
      refresh();
    } else if (event.widget == searchStrategy) {
      valueChanged = false;
      cpd.setSearchStrategy(setValueChanged(searchStrategy.getText(), cpd.getSearchStrategy()));
      if (valueChanged)
        setFileDirty();
    } else if (event.widget == defaultGroup) {
      valueChanged = false;
      cpd.setDefaultGroupName(setValueChanged(defaultGroup.getText(), cpd.getDefaultGroupName()));
      if (valueChanged)
        setFileDirty();
    } else if (event.widget == addGroupButton) {
      CommonInputDialog dialog = new CommonInputDialog(
              this,
              "Add Group",
              "Specify one or more unique group names, separated by 1 space character, and press OK",
              CommonInputDialog.GROUP_NAMES);

      for (;;) { // stay in loop until get "true" return from add below
        // used for looping while group name set is duplicate
        if (dialog.open() == Window.CANCEL)
          return;
        if (addNewOrEditExistingGroup(dialog.getValue(), null))
          break;
      }
      commonActionFinish();
    } else if (event.widget == addButton) { // add parameter or add override action

      boolean valid = tree.getSelectionCount() == 1;
      if (isPrimitive())
        valid = valid && (isGroupSelection() || isParmSelection());
      else
        valid = valid && (isParmSelection() || isOverrideSelection());

      if (!valid) {
        if (isPrimitive())
          Utility.popMessage("Wrong item selected",
                  "Please first select the group under which to add a parameter",
                  MessageDialog.ERROR);
        else
          Utility.popMessage("Wrong item selected",
                  "Please first select the parameter under which to add an override",
                  MessageDialog.ERROR);
        return;
      }

      if (isPrimitive()) { // adding a parameter
        TreeItem parentGroup = tree.getSelection()[0];
        if (isParmSelection())
          parentGroup = parentGroup.getParentItem();

        AddParameterDialog dialog = new AddParameterDialog(this);
        if (dialog.open() == Window.CANCEL)
          return;

        addNewConfigurationParameter(dialog, parentGroup);

        if (null != parentGroup)
          parentGroup.setExpanded(true);
        commonActionFinish();
      } else { // is aggregate - add an override
        TreeItem parentParm = tree.getSelection()[0];
        if (isOverride(parentParm))
          parentParm = parentParm.getParentItem();
        addOrEditOverride(parentParm, -1);
      }
    } else if ((event.widget == editButton) || (event.type == SWT.MouseDoubleClick)) {

      TreeItem editItem = tree.getSelection()[0];
      if (isParameter(editItem) && isPrimitive()) {
        AddParameterDialog dialog = new AddParameterDialog(this,
                getCorrespondingModelParm(editItem));
        if (dialog.open() == Window.CANCEL)
          return;

        // update the existing item
        alterExistingConfigurationParameter(dialog, editItem);
        // TODO consequences of changes in rest of model?
        commonActionFinishDirtyIfChange();
      } else if (isParameter(editItem) && isAggregate()) {
        // can edit name and description, but not Type (set from override)
        ConfigurationParameter existingCP = getCorrespondingModelParm(editItem);

        AddParameterDialog dialog = new AddParameterDialog(this, existingCP);
        if (dialog.open() == Window.CANCEL)
          return;
        alterExistingConfigurationParameter(dialog, editItem);
        // TODO consequences of changes in rest of model?
        commonActionFinishDirtyIfChange();

      } else if (isOverride(editItem)) {
        TreeItem parent = editItem.getParentItem();
        int overrideIndex = getItemIndex(parent, editItem);
        addOrEditOverride(parent, overrideIndex);
      } else if (isGroup(editItem)) {
        String groupNames = getName(editItem.getText());
        if (groupNames.equals(COMMON_GROUP) || groupNames.equals(NOT_IN_ANY_GROUP))
          return; // can't change the name of these groups

        CommonInputDialog dialog = new CommonInputDialog(
                this,
                "Edit group",
                "Specify one or more unique group names, separated by 1 space character, and press OK",
                CommonInputDialog.GROUP_NAMES, groupNames);

        for (;;) {
          if (dialog.open() == Window.CANCEL)
            return;

          if (addNewOrEditExistingGroup(dialog.getValue(), editItem))
            break;
        }
        commonActionFinishDirtyIfChange();
      }
    } else if ((event.widget == removeButton)
            || (event.widget == tree && event.type == SWT.KeyUp && event.character == SWT.DEL)) {

      // handle remove - of all selected items
      // if a group is selected, removing the group also removes all the parms in the group
      // Because this is dangerous, we issue an "are you sure?" prompt.

      // Other side effects: Any parameter settings for the removed
      // parameters are also removed.

      valueChanged = removeItems(tree.getSelection(), GIVE_WARNING_MESSAGE);
      commonActionFinishDirtyIfChange();

      // TODO remove settings for any parameters removed
    } // end of remove action

    // handle selection changes
    else if (event.widget == tree && event.type == SWT.Selection) {

    }
    enable();
  }

  private void addOrEditOverride(TreeItem parent, int overrideIndex) {
    ConfigurationParameter cp = getCorrespondingModelParm(parent);

    Map delegateMap1 = editor.getResolvedDelegates();
    Map delegateMap = null;
    if (null != delegateMap1) {
      delegateMap = new HashMap(delegateMap1.size());
      delegateMap.putAll(delegateMap1);
      FlowControllerDeclaration fcd = editor.getResolvedFlowControllerDeclaration();
      if (null != fcd) {
        delegateMap.put(fcd.getKey(), fcd.getSpecifier());
      }
    }
    // only picks one override key - but code is from earlier design where multiple keys were
    // possible
    PickOverrideKeysAndParmName dialog = new PickOverrideKeysAndParmName(this, delegateMap,
            "Override Keys and Parameter Name Selection", cp, cpd, overrideIndex == -1);

    dialog.setTitle("Delegate Keys and Parameter Name Selection");
    dialog
            .setMessage("Select the override key path from the left panel, and the overridden parameter from the right panel.\nOnly valid parameters will be shown.");
    if (dialog.open() == Window.CANCEL)
      return;

    String delegateKeyName = dialog.delegateKeyName;
    String delegateParameterName = dialog.delegateParameterName;
    // update the existing item
    // have to do a 3 step update because the getOverrides returns a
    // cloned array
    valueChanged = false;
    String overrideSpec = delegateKeyName + '/' + delegateParameterName;
    // updateOneOverride(cp, overrideIndex, dialog.overrideSpec);
    if (overrideIndex < 0) {
      addOverride(cp, overrideSpec);
      valueChanged = true;
    } else {
      String[] overrides = cp.getOverrides();
      overrides[overrideIndex] = setValueChanged(overrideSpec, overrides[overrideIndex]);
      cp.setOverrides(overrides);
      parent.getItems()[overrideIndex].setText(OVERRIDE_HEADER + overrideSpec);
    }
    // TODO consequences of changes in rest of model?
    commonActionFinishDirtyIfChange();
  }
  

  private boolean removeItems(TreeItem[] itemsToRemove, boolean giveWarningMsg) {
    String[] namesToRemove = new String[itemsToRemove.length];
    boolean[] isGroup = new boolean[itemsToRemove.length];
    StringBuffer msgGroup = new StringBuffer();
    StringBuffer msg = new StringBuffer();
    StringBuffer oMsg = new StringBuffer();

    for (int i = 0; i < itemsToRemove.length; i++) {
      namesToRemove[i] = getName(itemsToRemove[i].getText());
      isGroup[i] = isGroup(itemsToRemove[i]);
      if (isGroup[i]) {
        if (NOT_IN_ANY_GROUP.equals(namesToRemove[i]))
          msgGroup
                  .append("\nThis action removes all parameter descriptions in the <Not in any group> section.");
        else {
          if (i > 0)
            msgGroup.append(", ");
          else if (COMMON_GROUP.equals(namesToRemove[i]))
            msgGroup
                    .append("\nThis action removes all parameter descriptions in the <Common> section.");
          else
            msgGroup
                    .append("\nGroups being removed, together with their parameter definitions defined here: \n");
          if (!COMMON_GROUP.equals(namesToRemove[i]))
            msgGroup.append(namesToRemove[i]);
        }
      } else if (isParameter(itemsToRemove[i])) {
        if (i > 0)
          msg.append(", ");
        else
          msg.append("\nParameters being removed: \n");
        msg.append(namesToRemove[i]);
      } else if (isOverride(itemsToRemove[i])) {
        if (i > 0)
          oMsg.append(", ");
        else
          oMsg.append("\nOverride being removed: \n");
        oMsg.append(namesToRemove[i]);
      } else
        throw new InternalErrorCDE("invalid state");
    }

    if (giveWarningMsg
            && Window.CANCEL == Utility.popOkCancel("Confirm Remove",
                    "Please confirm remove, or Cancel.\n" + msgGroup.toString() + msg.toString()
                            + oMsg.toString(), MessageDialog.WARNING))
      return false;

    // loop thru all things being removed, and remove them
    for (int i = 0; i < itemsToRemove.length; i++) {
      if (isGroup[i]) {
        removeGroup(itemsToRemove[i], namesToRemove[i]);
      } else if (isParameter(itemsToRemove[i])) { // just a plain parameter being
        // removed
        removeParameter(itemsToRemove[i], namesToRemove[i]);
      } else if (isOverride(itemsToRemove[i])) {
        TreeItem parentItem = itemsToRemove[i].getParentItem();
        ConfigurationParameter cp = getCorrespondingModelParm(parentItem);
        cp.setOverrides(removeOverride(cp, getItemIndex(parentItem, itemsToRemove[i])));
        itemsToRemove[i].dispose();
        if (cp.getOverrides().length == 0) {
          removeParameter(parentItem, getName(parentItem));
        }
      } else
        throw new InternalErrorCDE("Invalid state");
    }
    return true;
  }

  private void removeParameter(TreeItem itemToRemove, String nameToRemove) {
    TreeItem parentItem = itemToRemove.getParentItem();
    ConfigurationGroup cg = null;
    String parentGroupName = getName(parentItem.getText());
    if (parentGroupName.equals(NOT_IN_ANY_GROUP))
      cpd.setConfigurationParameters(removeConfigurationParameter(cpd.getConfigurationParameters(),
              nameToRemove));
    else if (parentGroupName.equals(COMMON_GROUP))
      cpd.setCommonParameters(commonParms = removeConfigurationParameter(cpd.getCommonParameters(),
              nameToRemove));
    else {
      cg = getConfigurationGroup(parentGroupName);
      cg.setConfigurationParameters(removeConfigurationParameter(cg.getConfigurationParameters(),
              nameToRemove));

    }
    removeParmSettingFromMultipleGroups(itemToRemove, REMOVE_FROM_GUI);
    itemToRemove.dispose();

    if (null != cg && cg.getConfigurationParameters().length == 0) {
      removeGroup(parentItem, getName(parentItem));
    }
  }

  private void removeGroup(TreeItem itemToRemove, String nameToRemove) {
    if (nameToRemove.equals(COMMON_GROUP)) {
      removeCommonParmSettingsFromMultipleGroups();
      cpd.setCommonParameters(configurationParameterArray0);
      commonParms = configurationParameterArray0;
      // can't really remove the <Common> group so remove all the parms
      disposeAllChildItems(itemToRemove);

    } else if (nameToRemove.equals(NOT_IN_ANY_GROUP)) {
      // remove settings for all non-group parm definitions
      removeIncludedParmSettingsFromSingleGroup(NOT_IN_ANY_GROUP, null);
      cpd.setConfigurationParameters(configurationParameterArray0);
      // remove all non-group parm definitions
      disposeAllChildItems(itemToRemove);

    } else {
      ConfigurationGroup cg = getConfigurationGroup(nameToRemove);
      // remove settings for all parms in the group too
      // also updates the settings GUI if the GUI is initialized
      removeIncludedParmSettingsFromMultipleGroups(cg.getNames(), cg.getConfigurationParameters());

      // remove group
      cpd.setConfigurationGroups(removeConfigurationGroup(cpd.getConfigurationGroups(), cg));
      itemToRemove.dispose(); // also disposes children of group in
      // GUI
    }
  }

  public void addParm(String name, ConfigurationParameter modelParm, ConfigGroup group,
          String override) {
    TreeItem parentGroup = getTreeItemGroup(group);
    AddParameterDialog dialog = new AddParameterDialog(this);
    dialog.parmName = name;
    dialog.description = modelParm.getDescription();
    dialog.mandatory = modelParm.isMandatory();
    dialog.multiValue = modelParm.isMultiValued();
    dialog.parmType = modelParm.getType();
    // dialog.overrideSpec = override;
    ConfigurationParameter parmInGroup = addNewConfigurationParameter(dialog, parentGroup);
    addOverride(parmInGroup, override);
    parentGroup.setExpanded(true);
    commonActionFinish();
  }

  private ConfigurationGroup getConfigurationGroup(String groupName) {
    if (groupName.equals(COMMON_GROUP))
      throw new InternalErrorCDE("invalid call");
    ConfigurationGroup[] groups = cpd.getConfigurationGroups();
    for (int i = 0; i < groups.length; i++) {
      if (groupName.equals(groupNameArrayToString(groups[i].getNames())))
        return groups[i];
    }
    throw new InternalErrorCDE("invalid state");
  }

  private ConfigurationGroup[] removeConfigurationGroup(ConfigurationGroup[] groups,
          ConfigurationGroup cg) {
    return (ConfigurationGroup[]) Utility.removeElementFromArray(groups, cg,
            ConfigurationGroup.class);
  }

  private ConfigurationParameter[] removeConfigurationParameter(ConfigurationParameter[] parms,
          String nameToRemove) {
    ConfigurationParameter[] newParms = new ConfigurationParameter[parms.length - 1];
    for (int i = 0, j = 0; i < newParms.length; i++, j++) {
      if (parms[j].getName().equals(nameToRemove))
        j++;
      newParms[i] = parms[j];
    }
    return newParms;
  }

  private String[] removeOverride(ConfigurationParameter cp, int i) {
    String[] oldOverrides = cp.getOverrides();
    String[] newOverrides = new String[oldOverrides.length - 1];
    if (i > 0)
      System.arraycopy(oldOverrides, 0, newOverrides, 0, i);
    if (oldOverrides.length - 1 - i > 0)
      System.arraycopy(oldOverrides, i + 1, newOverrides, i, oldOverrides.length - 1 - i);
    return newOverrides;
  }

  /**
   * Called to add group to aggregate parm decl based on delegate group
   * 
   * @param group
   *          the delegate group needing to be added to the aggregate
   */
  public ConfigGroup addGroup(ConfigGroup group) {
    String groupName = group.getName();
    String[] groupNameArray = group.getNameArray();
    if (group.getKind() == ConfigGroup.COMMON) {
      groupNameArray = getAllGroupNames(group.getCPD());
      groupName = groupNameArrayToString(groupNameArray);
    }
    ConfigurationGroup cg = new ConfigurationGroup_impl();
    cg.setConfigurationParameters(configurationParameterArray0);
    TreeItem item = addGroupToGUI(groupName, cg);
    // fill(commonParms, item); // don't add common parsm, they're added by definition
    addGroupToModel(cg);
    cg.setNames(groupNameArray);
    tree.setSelection(new TreeItem[] { item });
    return new ConfigGroup(cpd, cg);
  }

  // existing, if not null, doesn't point to <Common> which can't be edited
  /**
   * @param names -
   *          a sequence of group names separated by blanks
   * @param existing -
   *          null or an existing tree item being edited
   */
  private boolean addNewOrEditExistingGroup(String names, TreeItem existing) {
    valueChanged = true; // preset
    ConfigGroup mcg = null;
    String[] oldGroupNames = stringArray0;
    String[] newGroupNames = groupNamesToArray(names);
    String[] groupNamesToAdd;
    String[] groupNamesToDrop = stringArray0;

    if (null != existing) {
      mcg = getCorrespondingModelGroup(existing);
      oldGroupNames = mcg.getNameArray();

      groupNamesToDrop = setDiff(oldGroupNames, newGroupNames);
      groupNamesToAdd = setDiff(newGroupNames, oldGroupNames);
    } else {
      groupNamesToAdd = newGroupNames;
    }

    // it is legal to define a group name more than once, but the same set of group names
    // shouldn't be defined more than once
    if (groupNameAlreadyDefined(newGroupNames)) {
      Utility.popMessage("Group Already Defined",
              "This set of group names has already been defined." + "\n\nGroup: " + names,
              MessageDialog.ERROR);
      return false;
    }

    TreeItem item;
    if (existing == null) {
      ConfigurationGroup cg = new ConfigurationGroup_impl();
      cg.setConfigurationParameters(configurationParameterArray0);
      cg.setNames(groupNamesToArray(names));
      item = addGroupToGUI(names, cg);
      addGroupToModel(cg);
    } else { // editing existing group
      valueChanged = groupNamesToDrop.length != 0 || groupNamesToAdd.length != 0;
      item = existing;
      setGroupText(item, names);

      for (int i = 0; i < groupNamesToDrop.length; i++) {
        removeIncludedParmSettingsFromSingleGroup(groupNamesToDrop[i], mcg.getConfigParms());
      }
      mcg.setNameArray(groupNamesToArray(names));
      if (null != settings) {
        for (int i = 0; i < groupNamesToAdd.length; i++) {
          TreeItem settingsItem = getSettingsTreeGroup(groupNamesToAdd[i]);
          if (null == settingsItem) {
            settingsItem = new TreeItem(settingsTree, SWT.NONE);
            setGroupText(settingsItem, groupNamesToAdd[i]);
            settingsItem.setData(null);
            fill(mcg.getConfigParms(), settingsItem);
            fill(commonParms, settingsItem);
          } else {
            fillInFrontOfCommon(mcg.getConfigParms(), settingsItem);
          }
        }
      }

    }

    tree.setSelection(new TreeItem[] { item });
    return true;
  }

  private void fillInFrontOfCommon(ConfigurationParameter[] parms, TreeItem settingsTreeGroup) {
    if (parms != null) {
      for (int i = parms.length - 1; i >= 0; i--) {
        fillParmItem(new TreeItem(settingsTreeGroup, SWT.NONE, 0), parms[i]);
      }
    }
  }

  /**
   * Calculate s1 - s2 set
   * 
   * @param s1
   * @param s2
   * @return
   */
  private String[] setDiff(String[] s1, String[] s2) {
    Set result = new TreeSet();
    for (int i = 0; i < s1.length; i++)
      result.add(s1[i]);
    for (int i = 0; i < s2.length; i++)
      result.remove(s2[i]);
    return (String[]) result.toArray(stringArray0);
  }

  private boolean setEqual(String[] s1, String[] s2) {
    if (null == s1 && null == s2)
      return true;
    if (null == s1 || null == s2)
      return false;
    if (s1.length != s2.length)
      return false;
    if (setDiff(s1, s2).length == 0)
      return true;
    return false;
  }

  /**
   * Called from ParameterDelegatesSection to add an override
   * 
   * @param parmInGroup
   * @param override
   */
  public void addOverride(ConfigurationParameter parmInGroup, String override) {
    addOverride(override, getTreeItemParm(parmInGroup), parmInGroup);
  }

  /**
   * add an override item
   * 
   * @param parent
   * @param override
   */
  private void addOverrideToGUI(TreeItem parent, String override) {
    // addOverride(dialog.overrideSpec, parent, cp);
    TreeItem item = new TreeItem(parent, SWT.NONE);
    item.setText(OVERRIDE_HEADER + override);
  }

  /**
   * Called by addNewConfigurationParameter, and fill (via refresh) to add overrides to the tree
   * list
   * 
   * @param parent
   * @param modelCP
   */
  protected void fillOverrides(TreeItem parent, ConfigurationParameter modelCP) {
    if (isAggregate()) {
      String[] overrides = modelCP.getOverrides();
      if (overrides != null) {
        for (int i = 0; i < overrides.length; i++) {
          addOverrideToGUI(parent, overrides[i]);
        }
        parent.setExpanded(true); // show added overrides
      }
    }
  }

  /**
   * called from add Override action
   * 
   * @param dialog
   * @param parent
   * @param cp
   */
  private void addOverride(String override, TreeItem parent, ConfigurationParameter cp) {
    cp.setOverrides(addOverrideToArray(cp.getOverrides(), override));
    addOverrideToGUI(parent, override);
    parent.setExpanded(true);
    commonActionFinish();
  }

  private void alterExistingConfigurationParameter(AddParameterDialog dialog,
          TreeItem existingTreeItem) {
    ConfigurationParameter existingCP = getCorrespondingModelParm(existingTreeItem);
    ConfigurationParameter previousCP = existingCP;
    previousCP = (ConfigurationParameter) previousCP.clone();
    fillModelParm(dialog, existingCP);
    fillParmItem(existingTreeItem, existingCP);

    // the following may have changed in an existing param spec, that could
    // affect the setting:
    // 1) the name, 2) the type, 3) the multi-value aspect
    // Description or mandatory changes have no effect on the settings

    // If the multi-value aspect changes, drop all the settings
    // If the type changes, drop all the settings
    // If the name changes, change existing settings for that parm name in all groups

    if ((!previousCP.getType().equals(existingCP.getType()))
            || (previousCP.isMultiValued() != existingCP.isMultiValued())) {
      removeParmSettingFromMultipleGroups(existingTreeItem, !REMOVE_FROM_GUI);
    }

    commonParmUpdate(existingTreeItem, existingCP, previousCP.getName());
  }

  private void commonParmUpdate(TreeItem existingTreeItem, ConfigurationParameter existingCP,
          String prevName) {
    updateParmInSettingsGUI(existingCP, existingTreeItem, prevName);

    String newName = existingCP.getName();
    if (!newName.equals(prevName)) {
      // name changed; update the settings model
      ConfigurationParameterSettings cps = getModelSettings();
      String[] allGroupNames = new String[] { null };
      if (usingGroupsButton.getSelection()) {
        allGroupNames = (String[]) Utility
                .addElementToArray(getAllGroupNames(), null, String.class);
      }
      Object value;

      for (int i = 0; i < allGroupNames.length; i++) {
        if (null != (value = cps.getParameterValue(allGroupNames[i], prevName))) {
          cps.setParameterValue(allGroupNames[i], newName, value);
          cps.setParameterValue(allGroupNames[i], prevName, null);
        }
      }
    }
  }

  /**
   * Fills in the model Configuration Parm from the Add/Edit dialog. called from
   * addNewConfigurationParameter, and alterExistingConfigurationParameter
   * 
   * @param dialog
   * @param existingCP
   */
  private void fillModelParm(AddParameterDialog dialog, ConfigurationParameter existingCP) {
    valueChanged = false;
    existingCP.setName(setValueChanged(dialog.parmName, existingCP.getName()));
    existingCP.setDescription(setValueChanged(multiLineFix(dialog.description), existingCP
            .getDescription()));
    existingCP.setMandatory(setValueChangedBoolean(dialog.mandatory, existingCP.isMandatory()));
    existingCP
            .setMultiValued(setValueChangedBoolean(dialog.multiValue, existingCP.isMultiValued()));
    existingCP.setType(setValueChanged(dialog.parmType, existingCP.getType()));
    if (valueChanged)
      setFileDirty();
  }

  /**
   * Called from UI when adding a new Configuraton Parameter Called from refresh when filling params
   * Called when adding override to new parm
   * 
   * @param dialog
   * @param group
   * @return
   */
  private ConfigurationParameter addNewConfigurationParameter(AddParameterDialog dialog,
          TreeItem group) {
    ConfigurationParameter newCP = new ConfigurationParameter_impl();
    fillModelParm(dialog, newCP);

    if (null != group) {
      String groupName = getName(group.getText());
      if (groupName.equals(COMMON_GROUP)) {
        cpd.setCommonParameters(commonParms = addParmToArray(cpd.getCommonParameters(), newCP));
      } else if (groupName.equals(NOT_IN_ANY_GROUP)) {
        cpd.setConfigurationParameters(addParmToArray(cpd.getConfigurationParameters(), newCP));
      } else {
        ConfigurationGroup cg = getConfigurationGroup(groupName);
        cg.setConfigurationParameters(addParmToArray(cg.getConfigurationParameters(), newCP));
      }
    } else { // no groups
      throw new InternalErrorCDE("invalid state");
    }
    addNewConfigurationParameterToGUI(newCP, group);
    return newCP;
  }

  private void addGroupToModel(ConfigurationGroup newCg) {
    ConfigurationGroup[] oldCgs = cpd.getConfigurationGroups();
    ConfigurationGroup[] newCgs;
    if (null == oldCgs) {
      newCgs = new ConfigurationGroup[1];
    } else {
      newCgs = new ConfigurationGroup[oldCgs.length + 1];
      System.arraycopy(oldCgs, 0, newCgs, 0, oldCgs.length);
    }
    newCgs[newCgs.length - 1] = newCg;
    cpd.setConfigurationGroups(newCgs);
  }

  private String[] addOverrideToArray(String[] overrides, String newOverride) {
    if (null == overrides)
      return new String[] { newOverride };
    String[] newOverrides = new String[overrides.length + 1];
    System.arraycopy(overrides, 0, newOverrides, 0, overrides.length);
    newOverrides[overrides.length] = newOverride;
    return newOverrides;
  }

  private ConfigurationParameter[] addParmToArray(ConfigurationParameter[] cps,
          ConfigurationParameter newCP) {

    if (null == cps) {
      return new ConfigurationParameter[] { newCP };
    }
    ConfigurationParameter[] newCps = new ConfigurationParameter[cps.length + 1];
    System.arraycopy(cps, 0, newCps, 0, cps.length);
    newCps[cps.length] = newCP;
    return newCps;
  }

  /**
   * 
   * @param names
   * @return true if there is a group whose names are the same set
   */
  private boolean groupNameAlreadyDefined(String[] names) {
    ConfigurationGroup[] cgs = cpd.getConfigurationGroups();
    if (null != cgs) {
      for (int i = 0; i < cgs.length; i++) {
        if (setEqual(names, cgs[i].getNames()))
          return true;
      }
    }
    return false;
  }

  public static boolean parameterNameAlreadyDefinedNoMsg(String name,
          ConfigurationParameterDeclarations pCpd) {
    if (pCpd.getCommonParameters() != null) {
      if (parameterInArray(name, pCpd.getCommonParameters()))
        return true;
    }
    if (pCpd.getConfigurationParameters() != null) {
      if (parameterInArray(name, pCpd.getConfigurationParameters()))
        return true;
    }
    ConfigurationGroup[] groups;
    if ((groups = pCpd.getConfigurationGroups()) != null) {
      for (int i = 0; i < groups.length; i++) {
        if (parameterInArray(name, groups[i].getConfigurationParameters()))
          return true;
      }
    }
    return false;
  }

  public boolean parameterNameAlreadyDefined(String name) {
    boolean alreadyDefined = parameterNameAlreadyDefinedNoMsg(name, cpd);
    if (alreadyDefined) {
      Utility.popMessage("Parameter Already Defined",
              "The following parameter is already defined in the list. Parameter names must be unique."
                      + "\n\nParameter: " + name, MessageDialog.ERROR);
    }
    return alreadyDefined;
  }

  private static boolean parameterInArray(String name, ConfigurationParameter[] cps) {
    for (int i = 0; i < cps.length; i++) {
      if (name.equals(cps[i].getName()))
        return true;
    }
    return false;
  }

  private void commonActionFinish() {
    valueChanged = true;
    commonActionFinishDirtyIfChange();
  }

  /**
   * called by Edit operations which might not make any changes They set the dirty state if any
   * changes occur, so don't set it here.
   * 
   */
  private void commonActionFinishDirtyIfChange() {
    if (valueChanged)
      setFileDirty();
    enable();
  }

  public void enable() {

    usingGroupsButton.setEnabled(!isAggregate());
    boolean usingGroups = usingGroupsButton.getSelection();
    groupingControl.setVisible(usingGroups);

    addButton.setEnabled(isPrimitive() || tree.getSelectionCount() == 1
            && (isParmSelection() || isOverrideSelection()));

    addGroupButton.setEnabled(isPrimitive() && usingGroups);

    removeButton.setEnabled(tree.getSelectionCount() == 1
            && (isParmSelection() || isGroupSelection() || isOverrideSelection()));

    editButton
            .setEnabled(tree.getSelectionCount() == 1
                    && ((/* isPrimitive() && */isParmSelection()) || isOverrideSelection() || (isPrimitive()
                            && isGroupSelection() && !isCommonGroupSelection())));
  }

  public Tree getTree() {
    return tree;
  }

  /**
   * Given a ConfigurationParameter, find the corresponding item in the tree. Note: parameters with
   * the same name can exist in different groups, so we don't match using the parm name, but rather
   * do an "EQ" test
   * 
   * @param p
   * @return
   */
  private TreeItem getTreeItemParm(ConfigurationParameter p) {
    TreeItem[] groups = tree.getItems();
    for (int i = 0; i < groups.length; i++) {
      TreeItem[] parms = groups[i].getItems();
      for (int j = 0; j < parms.length; j++) {
        if (getCorrespondingModelParm(parms[j]) == p)
          return parms[j];
      }
    }
    throw new InternalErrorCDE("invalid state");
  }

  /**
   * Given a ConfigGroup - find the corresponding tree item. Match is done against the display form
   * of the name(s), with special casing for the not-in-any-group and common.
   * 
   * @param g
   * @return
   */
  private TreeItem getTreeItemGroup(ConfigGroup g) {
    switch (g.getKind()) {
      case ConfigGroup.NOT_IN_ANY_GROUP:
        return tree.getItems()[0];
      case ConfigGroup.COMMON:
        return tree.getItems()[1];
    }
    TreeItem[] items = tree.getItems();
    for (int i = 2; i < items.length; i++) {
      if (getName(items[i].getText()).equals(g.getName()))
        return items[i];
    }
    throw new InternalErrorCDE("invalid state");
  }

  private TreeItem getSettingsTreeGroup(String groupName) {
    TreeItem[] items = settingsTree.getItems();
    for (int i = 0; i < items.length; i++) {
      if (groupName.equals(getName(items[i].getText())))
        return items[i];
    }
    return null;
  }

}
