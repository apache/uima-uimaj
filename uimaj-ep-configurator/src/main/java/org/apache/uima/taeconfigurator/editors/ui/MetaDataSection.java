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

import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.model.DescriptorMetaData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;


public class MetaDataSection extends AbstractSection {

  public void enable() {}
  
	private Text nameText;
	private Text descriptionText;
	private Text versionText;
	private Text vendorText;
	private DescriptorMetaData dmd = null;
	
	/**
	 * Creates a section to enter meta data. Including a text field for
	 * name, description, version and vendor.
	 * @param editor the referenced multipage editor
	 */
	public MetaDataSection(MultiPageEditor editor, Composite parent) {
		super(editor, parent, "Overall Identification Information",
		    "This section specifies the basic identification information for this descriptor"); 
	}

	public void initialize(IManagedForm form) {
	  super.initialize(form);
    Composite sectionClient = new2ColumnComposite(this.getSection());

    nameText = newLabeledTextField(sectionClient, "Name", "NameTextToolTip", SWT.NULL);
    versionText = newLabeledTextField(sectionClient, "Version",
        "VersionTextToolTip", SWT.NULL);
    vendorText = newLabeledTextField(sectionClient, "Vendor",
        "VendorTextToolTip", SWT.NULL);

    //description enter field
    descriptionText = newDescriptionTextBox(sectionClient, "Enter a description of this component here.");
    toolkit.paintBordersFor(sectionClient);
    
  }

	
/*
 *  (non-Javadoc)
 * @see org.eclipse.ui.forms.IFormPart#refresh()
 */
	public void refresh() {
		if (null == dmd)
      dmd = new DescriptorMetaData(editor);
    super.refresh();
		String name = null;
		String version = null;
		String description = null;
		String vendor = null;

		name = dmd.getName();
		version = dmd.getVersion();
		description = dmd.getDescription();
		vendor = dmd.getVendor();
		

		nameText.setText(convertNull(name));
		versionText.setText(convertNull(version));
		vendorText.setText(convertNull(vendor));
		descriptionText.setText(convertNull(description));
	}


	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
    valueChanged = false;
    
    dmd.setName(setValueChanged(nameText.getText(), dmd.getName()));        
    dmd.setVersion(setValueChanged(versionText.getText(), dmd.getVersion()));
    dmd.setVendor(setValueChanged(vendorText.getText(), dmd.getVendor()));
    dmd.setDescription(setValueChanged(multiLineFix(descriptionText.getText()), 
        dmd.getDescription()));
    

    if (valueChanged)
      setFileDirty();
  }


}
