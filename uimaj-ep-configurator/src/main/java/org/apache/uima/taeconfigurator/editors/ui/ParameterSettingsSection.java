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

import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 * display parameters on the settings page.
 * If groups, show by groups (using Tree metaphor)
 * Note:  The tree displayed here is an expanded version of the
 * ParameterSection Tree.  It differs in 3 ways, when Groups are being used:
 *   1) no "overrides" info
 *   2) Groups with multiple names are split; each group name has a different setting
 * 
 */
public class ParameterSettingsSection extends AbstractSectionParm {
 
	public ParameterSettingsSection(MultiPageEditor editor, Composite parent) {
		super(editor, parent, "Configuration Parameters", "This section list all configuration parameters, either as plain parameters, or as part of one or more groups.  Select one to show, or set the value in the right hand panel.");
	}

  /* Called by the page constructor after all sections are created, to initialize them.
   *  (non-Javadoc)
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */  
	public void initialize(IManagedForm form) {
	  super.initialize(form); 
		Composite sectionClient = new2ColumnComposite(this.getSection());
    enableBorders(sectionClient);
		toolkit.paintBordersFor(sectionClient);	
		tree = newTree(sectionClient);
		
		ParameterSection ps = editor.getParameterPage().getParameterSection();
		if (null != ps)
		  ps.setSettings(this);
		tree.addListener(SWT.MouseHover, this); // for Description
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#refresh()
	 */

	public void refresh() {
    super.refresh();
    
    // only called at beginning.
    // Subsequently, incrementally updated as parameters and groups
    // change. 
    // 
    showOverrides = false;
    splitGroupNames = true;
    clearAndRefillTree(isParmGroup());
	}

//  public void enable() {}
  
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
    if (event.type == SWT.MouseHover) {
      showDescriptionAsToolTip(event);
    }
    else if (event.widget == tree && event.type == SWT.Selection) {
			editor.getSettingsPage().getValueSection().refresh();
		}
	}

	public Tree getTree() {return tree;}
	

	
	/**
	 * called by the Values section 
	 * @return
	 */
	public String getSelectedParamName(){
		TreeItem[] items = tree.getSelection();
		if(items.length == 0)
		  return null;
		
		TreeItem item = items[0];
		if (isParameter(item))  
      return getName(item.getText());
    return null;  
	}

	public String getSelectedParamGroupName(){
		TreeItem[] items = tree.getSelection();
		if(items.length == 0)
		  return null;
		
		TreeItem item = items[0];
		if (isParameter(item)) {
		  TreeItem parent = item.getParentItem();
		  if (null == parent)
		    throw new InternalErrorCDE("invalid state");
		  return getName(parent.getText());
		}
    return null;  
	}
  /**
   * 
   * @return
   */
	public ConfigurationParameter getSelectedModelParameter() {
		TreeItem[] items = tree.getSelection();
		if(items.length == 0)
		  return null;
		
		TreeItem item = items[0];
		if (isParameter(item)) {
		  TreeItem group = item.getParentItem();
		  ConfigurationParameterDeclarations cpds = getConfigurationParameterDeclarations();
		  String groupName = (null == group) ? null : getName(group.getText());
		  if (NOT_IN_ANY_GROUP.equals(groupName))
		    return cpds.getConfigurationParameter(null, getName(item.getText()));
		  return cpds.getConfigurationParameter(groupName, getName(item.getText()));
		}
    return null;
	}

}
