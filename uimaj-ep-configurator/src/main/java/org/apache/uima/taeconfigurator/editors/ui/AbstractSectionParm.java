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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.model.ConfigGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 */
public abstract class AbstractSectionParm extends AbstractSection {

  public final static String NOT_IN_ANY_GROUP = Messages
          .getString("AbstractSectionParm.notInAnyGroup"); //$NON-NLS-1$

  public final static String COMMON_GROUP = Messages.getString("AbstractSectionParm.common"); //$NON-NLS-1$

  // maintainers note: names below have extra trailing blanks to get them to approximately line up
  // where possible
  protected final static String DELEGATE_HEADER = Messages
          .getString("AbstractSectionParm.delegateKeyName"); //$NON-NLS-1$

  protected final static String FLOWCTLR_HEADER = "Flow Controller Key Name: ";

  protected final static String GROUP_HEADER = Messages
          .getString("AbstractSectionParm.headerGroupNames"); //$NON-NLS-1$

  protected final static String COMMON_GROUP_HEADER = Messages
          .getString("AbstractSectionParm.headerCommon"); //$NON-NLS-1$

  protected final static String NOT_IN_ANY_GROUP_HEADER = Messages
          .getString("AbstractSectionParm.headerNotInAnyGroup"); //$NON-NLS-1$

  protected final String OVERRIDE_HEADER = Messages.getString("AbstractSectionParm.overrides"); // nonstatic

  // for
  // easy
  // ref
  // in
  // subclass
  // //$NON-NLS-1$

  protected final static String MULTI_VALUE_INDICATOR  = "Multi  "; //$NON-NLS-1$

  protected final static String SINGLE_VALUE_INDICATOR = "Single "; //$NON-NLS-1$

  protected final static String OPTIONAL_INDICATOR = "Opt "; //$NON-NLS-1$

  protected final static String REQUIRED_INDICATOR = "Req "; //$NON-NLS-1$
  
  protected final static String EXTERNAL_OVERRIDE_INDICATOR    = "XO "; //$NON-NLS-1$
  
  protected final static String NO_EXTERNAL_OVERRIDE_INDICATOR = "      ";
  
  protected final String nameHeader = "  Name: "; //$NON-NLS-1$

  protected final static Map<String,String> typeNamesW = new HashMap<String,String>(4);
  static { // map extra spaces to get these to take the same
    typeNamesW.put("Boolean", "Boolean "); //$NON-NLS-1$ //$NON-NLS-2$
    typeNamesW.put("Float",   "Float      "); //$NON-NLS-1$ //$NON-NLS-2$
    typeNamesW.put(Messages.getString("AbstractSectionParm.16"), 
                              "Integer   "); //$NON-NLS-1$ //$NON-NLS-2$
    typeNamesW.put("String",  "String     "); //$NON-NLS-1$ //$NON-NLS-2$
  }

  protected Tree tree;

  protected Tree parameterSectionTree = null;

  protected boolean showOverrides;

  protected boolean splitGroupNames;

  protected ConfigurationParameter[] commonParms;

  protected Map groupParms;

  protected ConfigurationParameterDeclarations cpd;

  // settings set by other page when it is created
  protected ParameterSettingsSection settings = null;

  protected Tree settingsTree = null;

  public void setSettings(ParameterSettingsSection v) {
    settings = v;
    settingsTree = v.getTree();
  }

  /**
   * @param editor
   * @param parent
   * @param hasDescription
   */
  public AbstractSectionParm(MultiPageEditor aEditor, Composite parent, String header,
          String description) {
    super(aEditor, parent, header, description);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractSection#enable()
   */
  public void enable() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public abstract void handleEvent(Event event);

  /*************************************************************************************************
   * methods shared with multiple param pages ****************************************
   */

  /**
   * Two modes: settingsDisplayMode - if true, shows groups one name at a time, and puts all
   * <common> parms in other groups
   */
  protected void clearAndRefillTree(boolean usingGroups) {
    cpd = getConfigurationParameterDeclarations();

    tree.removeAll();

    // when filling ParameterSection, it might or might not have
    // settings tree set, depending on which panel is shown first
    // Filling parmaterSection on initial refresh should not update, in any case, the
    // the settings page; it has its own refresh.
    // To prevent this, we turn off the settingsTree reference while filling
    // and restore it at the end

    Tree savedSettingsTree = settingsTree;
    settingsTree = null;
    groupParms = new TreeMap();
    try {

      // tree has 2 dummy groups
      // first is the <Not in any group>, 2nd is the <Common>
      if (null == cpd.getConfigurationParameters())
        cpd.setConfigurationParameters(configurationParameterArray0);

      fillGroup(cpd.getConfigurationParameters(), NOT_IN_ANY_GROUP, null);

      if (null == cpd.getCommonParameters())
        cpd.setCommonParameters(configurationParameterArray0);

      if (usingGroups) {
        fillGroup(cpd.getCommonParameters(), COMMON_GROUP, null);
      }

      ConfigurationGroup[] groups = cpd.getConfigurationGroups();
      for (int i = 0; i < groups.length; i++) {
        ConfigurationParameter[] cps = groups[i].getConfigurationParameters();
        if (null == cps)
          groups[i].setConfigurationParameters(cps = configurationParameterArray0);
        fillGroup(groups[i].getConfigurationParameters(), groups[i].getNames(), groups[i]);
      }

      if (splitGroupNames)
        fillGroupsFromGroupParms();

      expandAllItems(tree.getItems()); // expand for overrides or groups

    } finally {
      settingsTree = savedSettingsTree;
      groupParms = null;
    }
  }

  private void expandAllItems(TreeItem[] items) {
    TreeItem[] containedItems;
    for (int i = 0; i < items.length; i++) {
      items[i].setExpanded(true);
      containedItems = items[i].getItems();
      if (null != containedItems)
        expandAllItems(containedItems);
    }
  }

  /**
   * Called by refresh; add a normal named group and a set of parmaters
   */
  private void fillGroup(ConfigurationParameter[] parms, String[] names, ConfigurationGroup modelCG) {
    fillGroup(parms, groupNameArrayToString(names), modelCG);
  }

  /**
   * called by refresh() for <Common> and refresh() for named, via another path with names as array
   * first converted to concatenated string.
   * 
   * @param parms
   * @param names
   */
  private void fillGroup(ConfigurationParameter[] parms, String names, ConfigurationGroup modelCG) {
    if (splitGroupNames) {
      if (names.equals(COMMON_GROUP)) {
        commonParms = parms;
      } else {
        String[] nameArray = groupNamesToArray(names);
        if (nameArray.length == 1 && nameArray[0].equals(NOT_IN_ANY_GROUP)) {
          TreeItem groupItem = addGroupToGUI(nameArray[0], modelCG);
          fill(parms, groupItem);
        } else
          for (int i = 0; i < nameArray.length; i++) {
            List g = (List) groupParms.get(nameArray[i]);
            if (null == g) {
              g = new ArrayList();
              groupParms.put(nameArray[i], g);
            }
            g.add(new Object[] { modelCG, parms });
          }
      }
    } else {
      if (names.equals(COMMON_GROUP))
        commonParms = parms;
      TreeItem groupItem = addGroupToGUI(names, modelCG);
      fill(parms, groupItem);
    }
  }

  private void fillGroupsFromGroupParms() {
    for (Iterator grpInfo = groupParms.entrySet().iterator(); grpInfo.hasNext();) {
      Map.Entry entry = (Map.Entry) grpInfo.next();
      String key = (String) entry.getKey();
      List pairs = (List) entry.getValue();
      TreeItem groupItem = addGroupToGUI(key, null); // modelCG not available, but not used
      for (Iterator pi = pairs.iterator(); pi.hasNext();) {
        Object[] v = (Object[]) pi.next();
        ConfigurationParameter[] parms = (ConfigurationParameter[]) v[1];
        fill(parms, groupItem);
      }
      fill(commonParms, groupItem);
    }
  }

  /**
   * called by refresh() when no groups, just plain parm sets, also for group case
   * 
   * @param parms
   * @param group
   *          <Not in any group> if not in a group, otherwise the group tree item
   */
  protected void fill(ConfigurationParameter[] parms, TreeItem group) {
    if (parms == null)
      return;
    for (int i = 0; i < parms.length; i++) {
      addNewConfigurationParameterToGUI(parms[i], group);
    }
  }

  public static String groupNameArrayToString(String[] strings) {
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < strings.length; i++) {
      if (i > 0)
        b.append("   "); //$NON-NLS-1$
      b.append(strings[i]);
    }
    return b.toString();
  }

  protected String[] groupNamesToArray(String names) {
    if (names.equals(NOT_IN_ANY_GROUP))
      return new String[] { names };

    AbstractList items = new ArrayList();
    int start = 0;
    int end;

    while (start < names.length() && (names.charAt(start) == ' '))
      start++;

    for (; start < names.length();) {
      end = names.indexOf(' ', start);
      if (end == -1) {
        items.add(names.substring(start));
        break;
      }
      items.add(names.substring(start, end));
      start = end;
      while (start < names.length() && names.charAt(start) == ' ')
        start++;
    }
    return (String[]) items.toArray(stringArray0);
  }

  /**
   * Takes an existing model parm and fills a pre-allocated treeItem. 3 callers:
   * addNewConfigurationParameter, alterExistingConfigurationParamater (editing), fill (bulk update
   * from refresh)
   * 
   * @param item
   * @param parm
   */
  protected void fillParmItem(TreeItem item, ConfigurationParameter parm) {
    item.setText(parmGuiString(parm));

    // // set data if tree == parmsection tree
    // if (item.getParent() == parameterSectionTree)
    // back link used to find corresponding model parm decl from tree item
    item.setData(parm);
  }

  protected String parmGuiString(ConfigurationParameter parm) {
    return ((parm.isMultiValued()) ? MULTI_VALUE_INDICATOR : SINGLE_VALUE_INDICATOR)
            + ((parm.isMandatory()) ? REQUIRED_INDICATOR : OPTIONAL_INDICATOR)
            + typeNamesW.get(parm.getType())
            + ((parm.getExternalOverrideName()==null) ? NO_EXTERNAL_OVERRIDE_INDICATOR : EXTERNAL_OVERRIDE_INDICATOR)
            + nameHeader + parm.getName();    //$NON-NLS-1$
  }

  protected void setGroupText(TreeItem groupItem, String names) {
    if (names.equals(COMMON_GROUP))
      groupItem.setText(COMMON_GROUP_HEADER);
    else if (names.equals(NOT_IN_ANY_GROUP))
      groupItem.setText(NOT_IN_ANY_GROUP_HEADER);
    else
      // next line formats the names with the right number of spaces and makes it
      // possible to do future equal compares
      groupItem.setText(GROUP_HEADER + groupNameArrayToString(groupNamesToArray(names)));
  }

  protected TreeItem addGroupToGUI(String names, ConfigurationGroup cg) {
    TreeItem groupItem = new TreeItem(tree, SWT.NONE);
    setGroupText(groupItem, names);
    ConfigGroup mcg;
    if (names.equals(COMMON_GROUP))
      mcg = new ConfigGroup(cpd, ConfigGroup.COMMON);
    else if (names.equals(NOT_IN_ANY_GROUP))
      mcg = new ConfigGroup(cpd, ConfigGroup.NOT_IN_ANY_GROUP);
    else
      mcg = new ConfigGroup(cpd, cg);
    groupItem.setData(mcg);
    String[] nameArray = groupNamesToArray(names);
    if (null != settingsTree) {
      for (int i = 0; i < nameArray.length; i++) {
        TreeItem[] settingsItems = settingsTree.getItems();
        if (!containsGroup(nameArray[i], settingsItems)) {
          TreeItem settingsItem = new TreeItem(settingsTree, SWT.NONE);
          setGroupText(settingsItem, nameArray[i]);
          settingsItem.setData(null);
          fill(commonParms, settingsItem);
        }
      }
    }
    return groupItem;
  }

  private boolean containsGroup(String groupName, final TreeItem[] settingsItems) {
    for (int i = 0; i < settingsItems.length; i++) {
      if (groupName.equals(getName(settingsItems[i])))
        return true;
    }
    return false;
  }

  /**
   * This is called sometimes with Settings group
   * 
   * @param newCP
   * @param group -
   *          is never null. May be <Not in any group>, indicate no groups; may be the "<Common>"
   *          group; or may be a regular group with a set of group names
   */
  protected void addNewConfigurationParameterToGUI(ConfigurationParameter newCP, TreeItem group) {

    if (null == group)
      throw new InternalErrorCDE("invalid state"); //$NON-NLS-1$

    // is part of group but could be NOT_IN_ANY_GROUP
    if (null != settingsTree) {
      boolean isCommonOrNotInAnyGrp = COMMON_GROUP.equals(getName(group))
              || NOT_IN_ANY_GROUP.equals(getName(group));
      TreeItem[] groups = getSettingsGroups(group);
      for (int i = 0; i < groups.length; i++) {
        // this next test tries to add parms so that common ones come at the end,
        // and non-common ones come before the start of common ones.
        TreeItem newParmGuiItem = (isCommonOrNotInAnyGrp) ? new TreeItem(groups[i], SWT.NONE)
                : new TreeItem(groups[i], SWT.NONE, 0);
        fillParmItem(newParmGuiItem, newCP);
      }
    }

    // next only done for non-setting page
    if (group.getParent() != settingsTree) {
      TreeItem newItem;
      fillParmItem(newItem = new TreeItem(group, SWT.NONE), newCP);

      if (showOverrides) {
        fillOverrides(newItem, newCP);
      }
    }
  }

  // this is overriden where needed
  // here just make above fn compile OK
  protected void fillOverrides(TreeItem parent, ConfigurationParameter modelCP) {
  }

  protected boolean isOverride(TreeItem item) {
    return (item.getText().startsWith(OVERRIDE_HEADER));
  }

  protected boolean isParameter(TreeItem item) {
    String s = item.getText();
    return (!isGroup(item) && !s.startsWith(DELEGATE_HEADER) && !s.startsWith(FLOWCTLR_HEADER) && !item
            .getText().startsWith(OVERRIDE_HEADER));
  }

  // Note: rest of code considers NOT_IN_ANY_GROUP to be a kind of group
  protected boolean isGroup(TreeItem item) {
    String s = item.getText();
    return s.startsWith(GROUP_HEADER) || s.startsWith(COMMON_GROUP_HEADER)
            || s.startsWith(NOT_IN_ANY_GROUP_HEADER);
  }

  protected boolean isNOT_IN_ANY_GROUP(TreeItem item) {
    return item.getText().startsWith(NOT_IN_ANY_GROUP_HEADER);
  }

  protected boolean isCommonGroup(TreeItem item) {
    return item.getText().startsWith(COMMON_GROUP_HEADER);
  }

  protected boolean isDelegate(TreeItem item) {
    return item.getText().startsWith(DELEGATE_HEADER) || item.getText().startsWith(FLOWCTLR_HEADER);
  }

  protected boolean isGroupSelection() {
    return isGroup(tree.getSelection()[0]);
  }

  protected boolean isCommonGroupSelection() {
    return isCommonGroup(tree.getSelection()[0]);
  }

  protected boolean isOverrideSelection() {
    return isOverride(tree.getSelection()[0]);
  }

  protected boolean isParmSelection() {
    return isParameter(tree.getSelection()[0]);
  }

  protected String getName(TreeItem item) {
    return getName(item.getText());
  }

  protected String getName(String s) {

    if (s.startsWith(NOT_IN_ANY_GROUP_HEADER))
      return NOT_IN_ANY_GROUP;
    if (s.startsWith(COMMON_GROUP_HEADER))
      return COMMON_GROUP;

    if (s.startsWith(GROUP_HEADER))
      return s.substring(GROUP_HEADER.length());
    if (s.startsWith(OVERRIDE_HEADER))
      return s.substring(OVERRIDE_HEADER.length());
    // parameter
    return s.substring(s.indexOf(nameHeader) + nameHeader.length());
  }

  protected int getItemIndex(TreeItem parent, TreeItem child) {
    return getItemIndex(parent.getItems(), child);
  }

  protected int getItemIndex(Tree parent, TreeItem child) {
    return getItemIndex(parent.getItems(), child);
  }

  /**
   * Works between parameter tree and settings tree We don't use any relative index offsets.
   * Instead, we search for the item with the same parameter name.
   * 
   * 
   * @param containingGroup
   *          in parm section; if null = means all groups (common parms)
   * @return
   */
  protected TreeItem[] getSettingsParameter(TreeItem containingGroup, String sourceItemName) {
    if (null == settingsTree)
      return null;

    if (null != containingGroup && isNOT_IN_ANY_GROUP(containingGroup))
      return new TreeItem[] { findMatchingParm(settingsTree.getItems()[0], sourceItemName) };

    TreeItem[] groups = getSettingsGroups((null == containingGroup) ? tree.getItems()[1] // use
            // common
            // group,
            // will
            // return
            // all
            // groups
            // in
            // settings
            // pg
            : containingGroup);
    TreeItem[] results = new TreeItem[groups.length];

    for (int i = 0; i < groups.length; i++) {
      results[i] = findMatchingParm(groups[i], sourceItemName);
    }
    return results;
  }

  private TreeItem findMatchingParm(TreeItem group, String name) {
    final TreeItem[] items = group.getItems();
    for (int i = 0; i < items.length; i++) {
      if (name.equals(getName(items[i])))
        return items[i];
    }
    throw new InternalErrorCDE("invalid state");
  }

  /**
   * get set of settings group from settingsTree that correspond to parmsection group
   * 
   * @param group
   * @return
   */
  protected TreeItem[] getSettingsGroups(TreeItem group) {
    if (null == settingsTree)
      return null;

    if (isNOT_IN_ANY_GROUP(group))
      return new TreeItem[] { settingsTree.getItems()[0] };

    AbstractList results = new ArrayList();

    String[] groupNamesArray = groupNamesToArray(getName(group.getText()));
    TreeItem[] items = settingsTree.getItems();

    if (groupNamesArray.length == 1 && groupNamesArray[0].equals(COMMON_GROUP)) {
      // add parm to all groups except <Not in any group>
      TreeItem[] result = new TreeItem[items.length - 1];
      System.arraycopy(items, 1, result, 0, result.length);
      return result;
    }

    for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
      String name = getName(items[itemIndex].getText());
      for (int i = 0; i < groupNamesArray.length; i++) {
        if (name.equals(groupNamesArray[i]))
          results.add(items[itemIndex]);
      }
    }
    return (TreeItem[]) results.toArray(treeItemArray0);
  }

  /**
   * find settings tree item for group name
   * 
   * @param name
   * @return
   */
  protected TreeItem getSettingsGroupTreeItemByName(String name) {
    TreeItem[] items = settingsTree.getItems();
    for (int i = 0; i < items.length; i++) {
      if (name.equals(getName(items[i].getText())))
        return items[i];
    }
    throw new InternalErrorCDE("invalid state"); //$NON-NLS-1$
  }

  protected ConfigurationParameter getCorrespondingModelParm(TreeItem item) {
    if (!isParameter(item))
      throw new InternalErrorCDE("invalid argument"); //$NON-NLS-1$
    return (ConfigurationParameter) item.getData();
  }

  protected ConfigGroup getCorrespondingModelGroup(TreeItem item) {
    if (!isGroup(item))
      throw new InternalErrorCDE("invalid argument"); //$NON-NLS-1$
    return (ConfigGroup) item.getData();
  }

  public ConfigurationParameterSettings getConfigurationParameterSettings() {
    return editor.getAeDescription().getMetaData().getConfigurationParameterSettings();
  }

  /*************************************************************************************************
   * * methods affecting the parameter settings. * These run whether or not the settings page has
   * been instantiated. * If the settings page is instantiated, that GUI is also updated.
   * *******************************************************************
   */

  public ConfigurationParameterSettings getModelSettings() {
    return getAnalysisEngineMetaData().getConfigurationParameterSettings();
  }

  /**
   * Remove a parameter from all groups it lives in the Settings. If settings page is shown, also
   * update the GUI.
   * 
   * @param treeItem
   *          in ParameterSection of parameter belonging to (multiple) groups
   */
  public void removeParmSettingFromMultipleGroups(TreeItem parmItem, boolean removeFromGUI) {
    if (!isParameter(parmItem))
      throw new InternalErrorCDE("invalid argument"); //$NON-NLS-1$

    ConfigurationParameterSettings modelSettings = getModelSettings();
    String parmName = getName(parmItem);
    TreeItem parent = parmItem.getParentItem();
    String groupName = getName(parent.getText());
    if (!groupName.equals(NOT_IN_ANY_GROUP)) {
      String[] groupNames = (getName(parent.getText()).equals(COMMON_GROUP)) ? getAllGroupNames()
              : getCorrespondingModelGroup(parent).getNameArray();

      for (int i = 0; i < groupNames.length; i++) {
        modelSettings.setParameterValue(groupNames[i], parmName, null);
      }
    } else {
      modelSettings.setParameterValue(parmName, null);
    }

    if (null != settings) {
      if (removeFromGUI) {
        TreeItem[] settingsTreeParms = getSettingsParameter(parent, parmName);
        for (int i = 0; i < settingsTreeParms.length; i++) {
          settingsTreeParms[i].dispose();
        }
      } else { // leave parm but remove value
        editor.getSettingsPage().getValueSection().refresh();
      }
    }
  }

  public void updateParmInSettingsGUI(ConfigurationParameter existingCP, TreeItem existingTreeItem,
          String prevName) {
    if (null != settings) {
      TreeItem[] settingsTreeParms = getSettingsParameter(existingTreeItem.getParentItem(),
              prevName);
      for (int i = 0; i < settingsTreeParms.length; i++) {
        fillParmItem(settingsTreeParms[i], existingCP);
      }
    }
  }

  protected String[] getAllGroupNames() {
    return getAllGroupNames(cpd);
  }

  /**
   * 
   * @param aCpd
   * @return all named groups, excludes <Common> and <Not in any group>
   */
  protected String[] getAllGroupNames(ConfigurationParameterDeclarations aCpd) {
    ConfigurationGroup[] cgs = aCpd.getConfigurationGroups();
    Set results = new TreeSet();
    for (int i = 0; i < cgs.length; i++) {
      String[] names = cgs[i].getNames();
      for (int j = 0; j < names.length; j++) {
        results.add(names[j]);
      }
    }
    return (String[]) results.toArray(stringArray0);
  }

  public void removeCommonParmSettingsFromMultipleGroups() {
    ConfigurationParameterSettings modelSettings = getModelSettings();
    String[] allGroupNames = getAllGroupNames();
    // TreeItem [] items = new TreeItem[0]; // done to avoid may not have been initialized msg
    // int offset = 0;
    commonParms = cpd.getCommonParameters();

    for (int i = 0; i < allGroupNames.length; i++) {
      // if (null != settings) {
      // items = getSettingsGroupTreeItemByName(allGroupNames[i]).getItems();
      // offset = items.length - commonParms.length;
      // }
      for (int j = 0; j < commonParms.length; j++) {
        modelSettings.setParameterValue(allGroupNames[i], commonParms[j].getName(), null);
      }
    }

    if (null != settings) {
      for (int j = 0; j < commonParms.length; j++) {
        TreeItem[] settingsParms = getSettingsParameter(null, commonParms[j].getName());
        for (int k = 0; k < settingsParms.length; k++) {
          settingsParms[k].dispose();
        }
      }
    }
  }

  /**
   * Remove some of the parameter settings associated with this particular group, not all the
   * parameters for that group name (some parameters may be associated with other instances of a
   * particular group name.) If no other group-set contains a particular individual group name, in
   * the Settings: remove the common parameters, and remove the individual group itself. Remove the
   * particular group-set definition. Note that a group may be defined in more than one group-set.
   * 
   * Method: for the group-set, get the parms. Remove just those parms from all groups. Remove the
   * group on the settings page (together with common parms for it) if no other group-set has this
   * group name Remove the group-set.
   * 
   * @param groupNames
   * @param cps
   */
  public void removeIncludedParmSettingsFromMultipleGroups(String[] groupNames,
          ConfigurationParameter[] cps) {
    for (int j = 0; j < groupNames.length; j++) {
      removeIncludedParmSettingsFromSingleGroup(groupNames[j], cps);
    }
  }

  /**
   * 
   * @param treeItem
   *          in ParameterSection of items an array of tree items to remove Can be all items under a
   *          particular group, or a set of items from different groups
   */
  public void removeIncludedParmSettingsFromSingleGroup(String groupName,
          ConfigurationParameter[] cps) {
    ConfigurationParameterSettings modelSettings = getModelSettings();
    // modelSettings.setParameterValue()
    if (groupName.equals(COMMON_GROUP))
      throw new InternalErrorCDE("invalid state"); //$NON-NLS-1$

    if (groupName.equals(NOT_IN_ANY_GROUP)) {
      modelSettings.setParameterSettings(nameValuePairArray0);

    } else {
      for (int i = 0; i < cps.length; i++)
        modelSettings.setParameterValue(groupName, cps[i].getName(), null);
    }
    if (null != settings) {
      TreeItem settingGroup = getSettingsGroupTreeItemByName(groupName);
      if (groupName.equals(COMMON_GROUP) || groupName.equals(NOT_IN_ANY_GROUP)) {
        disposeAllChildItems(settingGroup);
      } else {
        if (getConfigurationParameterDeclarations().getConfigurationGroupDeclarations(groupName).length == 1) {
          settingGroup.dispose();
        } else {

          for (int i = 0; i < cps.length; i++) {
            findMatchingParm(settingGroup, cps[i].getName()).dispose();
          }
        }

      }
    }
  }

  public void disposeAllChildItems(TreeItem parent) {
    TreeItem[] items = parent.getItems();
    for (int j = 0; j < items.length; j++) {
      items[j].dispose();
    }
  }

  protected void showDescriptionAsToolTip(Event event) {
    TreeItem item = tree.getItem(new Point(event.x, event.y));
    String text = null;
    if (null != item && isParameter(item)) {
      text = getCorrespondingModelParm(item).getDescription();
      String extOvr = getCorrespondingModelParm(item).getExternalOverrideName();
      if (extOvr != null) {
        if (text == null) {
          text = "(ExternalOverrideName = " + extOvr + ")";
        } else {
          text += " (ExternalOverrideName = " + extOvr + ")";
        }
      }
    }
    if (text != null) {
      setToolTipText(tree, text);
    } else
      tree.setToolTipText(""); //$NON-NLS-1$
  }

}
