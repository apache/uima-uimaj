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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;


/**
 * The Class ResourcePickerDialog.
 */
public class ResourcePickerDialog extends AbstractDialog {

  /** The resources UI. */
  protected Tree resourcesUI;
  
  /** The resources U ic 1. */
  protected TreeColumn resourcesUIc1;
  
  /** The resources U ic 2. */
  protected TreeColumn resourcesUIc2;
  
  /** The picked resource. */
  public IResource pickedResource;
  
  /** The result. */
  protected Object [] result;
    
  /**
   * Instantiates a new resource picker dialog.
   *
   * @param shell the shell
   */
  public ResourcePickerDialog(Shell shell) {
    super(shell, "Select a File", "Use this panel to select a file in the Workspace");
  }
  
  /** The Constant resourceComparator. */
  private static final Comparator resourceComparator = new Comparator () {
    @Override
    public int compare(Object arg0, Object arg1) {
       IResource r0 = (IResource) arg0;   
       IResource r1 = (IResource) arg1;   
        return r0.getName().compareTo(r1.getName());
    }
  };
  
  /**
   * Populate.
   *
   * @param parent the parent
   * @param resources the resources
   */
  private void populate(TreeItem parent, IResource[] resources) {
    Arrays.sort(resources, resourceComparator);
    for (int i = 0; i < resources.length; i++) {
      TreeItem item = new TreeItem(parent, SWT.NULL);
      IResource r = resources[i];
      item.setText(r.getName());
      item.setData(r);
      if (r instanceof IContainer) {
        new TreeItem(item, SWT.NULL);
      }
    }
  }
 
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent);
    
    resourcesUI = newTree(mainArea, SWT.SINGLE);
    ((GridData)resourcesUI.getLayoutData()).heightHint = 400;
    
    resourcesUIc1 = new TreeColumn(resourcesUI, SWT.LEFT);
    resourcesUIc2 = new TreeColumn(resourcesUI, SWT.LEFT);
        
    setupResourcesByLocation();
    return mainArea;
  }
  
  /**
   * Setup resources by location.
   */
  protected void setupResourcesByLocation() {
    resourcesUI.removeAll();
    resourcesUI.removeListener(SWT.Expand, this);    // remove to prevent triggering while setting up
    resourcesUI.removeListener(SWT.Selection, this); // remove to prevent triggering while setting up
    resourcesUIc1.setWidth(500);
    resourcesUIc2.setWidth(0);
    resourcesUI.setHeaderVisible(false);
    
    TreeItem topItem = new TreeItem(resourcesUI, SWT.NONE);
    topItem.setText("Workspace");
    IWorkspaceRoot root = TAEConfiguratorPlugin.getWorkspace().getRoot().getWorkspace().getRoot();
    try {
    IResource[] projects = root.members();
    populate(topItem, projects);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
    topItem.setExpanded(true);
    resourcesUI.addListener(SWT.Expand, this);
    resourcesUI.addListener(SWT.Selection, this);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.widget == resourcesUI &&
        event.type == SWT.Expand) {
      TreeItem expandedNode = (TreeItem) event.item;
      TreeItem maybeDummy = expandedNode.getItem(0);
      if (null == maybeDummy.getData()) {
        maybeDummy.dispose();
        IResource parentResource = (IResource)expandedNode.getData();
        try {
          populate(expandedNode, ((IContainer)parentResource).members());
        } catch (CoreException e) {
          throw new InternalErrorCDE("unhandled exception", e);
        }
      }
    } else if (event.widget == resourcesUI && event.type == SWT.Selection) {
      copyValuesFromGUI();
    }
    super.handleEvent(event);
  }
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    if (resourcesUI.getSelectionCount() > 0) {
      pickedResource = (IResource)resourcesUI.getSelection()[0].getData();
      IPath ipath = (null == pickedResource) ? null : pickedResource.getFullPath();
      result = (null == ipath ||
      		      (2 > ipath.segmentCount())) // project name alone cant be given to getFile
      	 ? null 
         : new IFile[] {TAEConfiguratorPlugin.getWorkspace().getRoot().getFile(ipath)};        
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    if ( (0 < resourcesUI.getSelectionCount()) &&
            (resourcesUI.getSelection()[0].getData() instanceof IFile)) {
      okButton.setEnabled(true);
    } else {
      okButton.setEnabled(false);
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

  /**
   * Gets the result.
   *
   * @return the result
   */
  public Object[] getResult() {
    return result; 
  }
  
  /**
   * Sets the result.
   *
   * @param aResult the new result
   */
  public void setResult(List aResult) {
    if (null == aResult) {
      result = null;
    } else {
      aResult.toArray(result = new Object[aResult.size()]);
    }
  }
  
}
