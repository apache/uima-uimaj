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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class EditSofaBindingsDialog extends AbstractDialog {
 
  public String [] selectedSofaNames;  //this is the selection indexes into next
	private Map availAndBoundSofas;
	private String aggrSofaName;
	
	private Table table;
	
	/**
	 * @param parentShell
	 */
	public EditSofaBindingsDialog(
	    AbstractSection aSection, 
	    String aAggrSofaName,
		  Map aAvailAndBoundSofas) {
		super(aSection, "Assign Components and their sofas to an Aggregate Sofa Name",
		    "Change the selection as needed to reflect bindings.");	
		availAndBoundSofas = aAvailAndBoundSofas;
		aggrSofaName = aAggrSofaName;
	}
	
	protected Control createDialogArea(Composite parent) {
		// create composite
		//   
		//   Bindings for aggregate sofa name xxxxxx:
		//   <Table with multi-select, one column>
		//     component/sofa-name
		//     component
		//
		
		Composite composite = (Composite)super.createDialogArea(parent);
		Label info = new Label(composite, SWT.NONE); 
    info.setText("Select all the delegate sofas from the list below which should be " +
    		"associated with the aggregate sofa name \""
    		+ aggrSofaName + "\".\n" +
    		"Hold down the Shift or Control keys to select multiple items.");
		
		table = newTable(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		
		int i = 0;
		for(Iterator it = availAndBoundSofas.entrySet().iterator(); it.hasNext(); i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			Map.Entry entry = (Map.Entry)it.next();
			item.setText((String)entry.getKey());
			if (null != entry.getValue())
				table.select(i);
		}
		table.pack();

		return composite;
	}
	
  public void copyValuesFromGUI() {
		selectedSofaNames = new String[table.getSelectionCount()];
		for(int i = 0, j = 0; i < table.getItemCount(); i++) {
			if(table.isSelected(i)) {
				selectedSofaNames[j++] = table.getItem(i).getText();
			}
		}
  }

  public boolean isValid() {
    // TODO Auto-generated method stub
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  public void enableOK() {
    okButton.setEnabled(true);    
  }
}
