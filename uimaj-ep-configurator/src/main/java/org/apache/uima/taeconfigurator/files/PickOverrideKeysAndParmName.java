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

package org.apache.uima.taeconfigurator.files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.ParameterDelegatesSection;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog;
import org.apache.uima.taeconfigurator.model.ConfigGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


public class PickOverrideKeysAndParmName extends AbstractDialog {

  //private AddParameterDialog parameterDialog = null; // not currently used

  private ConfigurationParameter cp;

  private boolean adding;

  private Table paramsUI;

  private Table keysUI;

  private Map delegates;
  
  //returned values
  public String delegateKeyName;
  public String delegateParameterName;

  private ConfigGroup cg;

   /*
   * Shows 2 side-by-side windows.
   * 
   * Left one is a list of keys of next-level-delegates
   * Right one is a list of parms of those delegates that 
   *   match sufficiently to be overridden by this overriding parm
   * If adding to existing override - current overrides are not shown as
   *   candidates.
   * If editing - all are shown (you can delete)  
   *
   */
  public PickOverrideKeysAndParmName(AbstractSection aSection,
          Map delegateMap,
          String message, ConfigurationParameter aCp, ConfigGroup aCg,
          boolean aAdding) {
    super(aSection, "Delegate Keys and Parameter Name Selection", message);
    delegates = delegateMap;
    cp = aCp;
    cg = aCg;
    adding = aAdding; // true if we're adding, not editing
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite)super.createDialogArea(parent);
    
    Composite twoCol = new2ColumnComposite(mainArea);
    
    
    keysUI = newTable(twoCol, SWT.SINGLE);
    paramsUI = newTable(twoCol, SWT.SINGLE);
    
    for (Iterator it = delegates.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      TableItem item = new TableItem(keysUI, SWT.NULL);
      item.setText((String)entry.getKey());
      item.setData(entry);
    }
    keysUI.addListener(SWT.Selection, this);
    if (0 < keysUI.getItemCount()) {
      keysUI.setSelection(0);
    }
    
    return mainArea;
  }
    
  
  
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.widget == keysUI && event.type == SWT.Selection) {
      fillParameterCandidates();
      super.handleEvent(event);
    }
    
    else if (event.widget == paramsUI && event.type == SWT.Selection) {
      copyValuesFromGUI();
      super.handleEvent(event);
    }
  }

  private void fillParameterCandidates() {
    paramsUI.setRedraw(false);
    paramsUI.removeAll();
    TableItem selectedItem = keysUI.getSelection()[0];

    Map.Entry entry = (Map.Entry) selectedItem.getData();
    String keyName = (String) entry.getKey();
    // support CasConsumers also
    // support Flow Controllers too
    // and skip remote service descriptors

    ResourceSpecifier rs = (ResourceSpecifier) entry.getValue();
    if (rs instanceof AnalysisEngineDescription || rs instanceof CasConsumerDescription
            || rs instanceof FlowControllerDescription) {
      ConfigurationParameterDeclarations delegateCpd = ((ResourceCreationSpecifier) rs)
              .getMetaData().getConfigurationParameterDeclarations();
      
      addSelectedParms(delegateCpd, keyName);

    }
    if (0 < paramsUI.getItemCount()) {
      paramsUI.setSelection(0);
    }
    paramsUI.setRedraw(true);
  }

  
  /*
   * Filter overridable parameters to exclude: 
   * - any that are not in the same set of groups
   */
  private void addSelectedParms(ConfigurationParameterDeclarations delegateCpd, String keyName) {

    // If not using groups then it's easy
    if (cg.getKind() == ConfigGroup.NOT_IN_ANY_GROUP) {
      addSelectedParms(delegateCpd.getConfigurationParameters(), keyName);
      return;
    }

    // If a group override must find parameters that are in all of the groups.
    // First add all the matching parameters in the COMMON group
    addSelectedParms(delegateCpd.getCommonParameters(), keyName);
    
    // Then build a list of all config params in the first group
    String[] groupNames = cg.getNameArray();
    ConfigurationGroup[] dcgs = delegateCpd.getConfigurationGroupDeclarations(groupNames[0]);
    ArrayList<ConfigurationParameter> cps = new ArrayList<ConfigurationParameter>();
    for (ConfigurationGroup dcg : dcgs) {
      cps.addAll(Arrays.asList(dcg.getConfigurationParameters()));
    }
    
    // Then for each of the other groups keep only those parameters in that group
    // Quite inefficient as searches for each parameter in turn in the group, but .... !!
    for (int i = 1; i < groupNames.length; ++i) {
      ArrayList<ConfigurationParameter> newCps = new ArrayList<ConfigurationParameter>();
      for (ConfigurationParameter cp : cps) {
        ConfigurationParameter sameCp = delegateCpd.getConfigurationParameter(groupNames[i], cp.getName());
        if (sameCp != null) {
          newCps.add(cp);
        }
      }
      if (newCps.size() == 0) {
        return; // no parameters found in all groups
      }
      cps = newCps;  // Repeat for other groups using the filtered list
    }
    // Finally apply the type matching & add to display
    addSelectedParms(cps.toArray(new ConfigurationParameter[cps.size()]), keyName);
  }
  
  /*
   * Filter overridable parameters to exclude: 
   * - already overridden (can't override same parameter twice) 
   * - those with different type or multi-valued-ness 
   */
  private void addSelectedParms(ConfigurationParameter[] parms, String keyName) {
    /*boolean isMultiValued = (null != parameterDialog) ? parameterDialog.multiValueUI
            .getSelection() : cp.isMultiValued();
    String type = (null != parameterDialog) ? parameterDialog.parmTypeUI.getText() : cp.getType();*/
    boolean isMultiValued = cp.isMultiValued();
    String type = cp.getType();

    if (null != parms) {
      for (int i = 0; i < parms.length; i++) {
        // multi-valued-ness must match
        if ((isMultiValued != parms[i].isMultiValued()))
          continue;
        // types must match, but we also allow if no type is spec'd - not sure if this is useful
        if ((null != type && !"".equals(type) && //$NON-NLS-1$
        !type.equals(parms[i].getType())))
          continue;
        // parameter must not be already overridden, unless we're editing an existing one
        String override = keyName + '/' + parms[i].getName();
        if (adding && null != ParameterDelegatesSection.getOverridingParmName(override, cg))
          continue;

        TableItem tableItem = new TableItem(paramsUI, SWT.NULL);
        tableItem.setText(parms[i].getName());
        tableItem.setData(parms[i]);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  public void copyValuesFromGUI() {
    delegateKeyName = keysUI.getSelection()[0].getText();
    delegateParameterName = paramsUI.getSelection()[0].getText();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  public void enableOK() {
    okButton.setEnabled( (0 < keysUI.getSelectionCount()) && 
                         (0 < paramsUI.getSelectionCount()));   
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  public boolean isValid() {
    return true;
  }

    /*
  protected TreeGroup createTreeGroup(Composite composite, Object rootObject) {
    return new TreeGroup(composite, rootObject, (keyTreeProvider = new KeyTreeProvider()),
            new KeyTreeLabelProvider(), new ParmNameProvider(), new ParmNameLabelProvider(),
            SWT.NONE, -1, -1, true); // set single selection
    // mode
  }

  protected void okPressed() {
    Iterator resultEnum = selectionGroup.getAllCheckedTreeItems().iterator();
    ArrayList list = new ArrayList();
    while (resultEnum.hasNext()) {
      Map.Entry entry = (Map.Entry) resultEnum.next();
      list.add(entry);
      while (null != (entry = (Map.Entry) keyTreeProvider.getParent(entry)))
        list.add(entry);
    }

    super.okPressed();
    List result = new ArrayList(2);
    result.add(list.toArray(new Map.Entry[list.size()]));
    result.add(getResult());
    setResult(result);
  }

  Map keyTreeParent = new HashMap();

  class KeyTreeProvider implements ITreeContentProvider {
 */
    /**
     * for a given map of delegates, return an array of maps representing the those delegates having
     * children. Not called for the top element, but called for subsequent layers
     */
  /* 
  public Object[] getChildren(Object parentElement) {
      return objectArray0;
    }

    public Object getParent(Object element) {
      return keyTreeParent.get(element);
    }

    public boolean hasChildren(Object element) {
      return false;
    }
 */
    /**
     * returns an array of Map.Entry elements: Key and AE or flow ctlr Description. Called only for
     * the top element
     */
  /*
    public Object[] getElements(Object inputElement) {
      AbstractList items = new ArrayList();
      if (inputElement instanceof ArrayList)
        inputElement = ((ArrayList) inputElement).get(0);
      Map delegatesMap = (Map) inputElement;

      for (Iterator it = delegatesMap.entrySet().iterator(); it.hasNext();) {
        items.add(it.next());
      }
      return items.toArray();
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      keyTreeParent.clear();
    }
*/

  /**
   * elements are Map.Entry
   */
  /*
  static class KeyTreeLabelProvider implements ILabelProvider {

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      return ((String) ((Map.Entry) element).getKey());
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

  }
  */
  /**
   * Element is: Map Entry Set ConfigurationParameterDeclarations
   */
  /*
  class ParmNameProvider implements ITreeContentProvider {

    public Object[] getChildren(Object parentElement) {
      return null;
    }

    public Object getParent(Object element) {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean hasChildren(Object element) {
      // TODO Auto-generated method stub
      return false;
    }
 */
    /*
     * Get elements (which are overridable parameters) for one delegate
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
  /*
    public Object[] getElements(Object inputElement) {
      AbstractList items = new ArrayList();
      Map.Entry entry = (Map.Entry) inputElement;
      String keyName = (String) entry.getKey();
      // support CasConsumers also
      // support Flow Controllers too
      // and skip remote service descriptors

      ResourceSpecifier rs = (ResourceSpecifier) entry.getValue();
      if (rs instanceof AnalysisEngineDescription || rs instanceof CasConsumerDescription
              || rs instanceof FlowControllerDescription) {
        ConfigurationParameterDeclarations delegateCpd = ((ResourceCreationSpecifier) rs)
                .getMetaData().getConfigurationParameterDeclarations();
        addSelectedParms(delegateCpd.getCommonParameters(), items, keyName);

        ConfigurationGroup[] groups = delegateCpd.getConfigurationGroups();
        if (null != groups) {
          for (int i = 0; i < groups.length; i++) {
            addSelectedParms(groups[i].getConfigurationParameters(), items, keyName);
          }
        }
        addSelectedParms(delegateCpd.getConfigurationParameters(), items, keyName);
      }

      return items.toArray();
    }


    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  static class ParmNameLabelProvider implements ILabelProvider {

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      return (String) element;
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }
  }
 */


}
