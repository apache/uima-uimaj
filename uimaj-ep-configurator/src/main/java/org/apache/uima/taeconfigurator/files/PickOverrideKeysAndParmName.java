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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.taeconfigurator.editors.ui.ParameterDelegatesSection;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddParameterDialog;

/**
 */
public class PickOverrideKeysAndParmName extends LimitedResourceSelectionDialog {

  final static private Object[] objectArray0 = new Object[0];

  private AddParameterDialog parameterDialog = null; // not currently used

  private ConfigurationParameter cp;

  private ITreeContentProvider keyTreeProvider;

  private ConfigurationParameterDeclarations cpd;

  private boolean adding;

  // rootElement is the map returned from getDelegateAnalysisEngineSpecifiers()

  public PickOverrideKeysAndParmName(Shell parentShell,
                  Object rootElement, // is editor.getResolvedDelegates(), a Map, key = keyname,
                                      // value = delegate description object
                  String message, ConfigurationParameter aCp,
                  ConfigurationParameterDeclarations aCpd, boolean aAdding) {
    super(parentShell, rootElement, message);
    cp = aCp;
    cpd = aCpd;
    adding = aAdding; // true if we're adding, not editing
  }

  protected TreeGroup createTreeGroup(Composite composite, Object rootObject) {
    return new TreeGroup(composite, rootObject, (keyTreeProvider = new KeyTreeProvider()),
                    new KeyTreeLabelProvider(), new ParmNameProvider(),
                    new ParmNameLabelProvider(), SWT.NONE, -1, -1, true); // set single selection
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

    /**
     * for a given map of delegates, return an array of maps representing the those delegates having
     * children. Not called for the top element, but called for subsequent layers
     */
    public Object[] getChildren(Object parentElement) {
      return objectArray0;
    }

    public Object getParent(Object element) {
      return keyTreeParent.get(element);
    }

    public boolean hasChildren(Object element) {
      return false;
    }

    /**
     * returns an array of Map.Entry elements: Key and AE or flow ctlr Description. Called only for
     * the top element
     */
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
  }

  /**
   * elements are Map.Entry
   */
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

  /**
   * Element is: Map Entry Set ConfigurationParameterDeclarations
   */
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

    /*
     * Get elements (which are overridable parameters) for one delegate
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
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

    /*
     * Filter overridable parameters to exclude:
     *  - already overridden (can't override same parameter twice) - those with different type or
     * multi-valued-ness (Group match not required)
     */
    private void addSelectedParms(ConfigurationParameter[] parms, AbstractList items, String keyName) {
      boolean isMultiValued = (null != parameterDialog) ? parameterDialog.multiValueUI
                      .getSelection() : cp.isMultiValued();
      String type = (null != parameterDialog) ? parameterDialog.parmTypeUI.getText() : cp.getType();

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
          if (adding && null != ParameterDelegatesSection.getOverridingParmName(override, cpd))
            continue;

          items.add(parms[i].getName());
        }
      }

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

}
