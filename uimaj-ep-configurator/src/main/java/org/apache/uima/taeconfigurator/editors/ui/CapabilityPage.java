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

import org.apache.uima.taeconfigurator.editors.Form2Panel;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IManagedForm;


/**
 * The Class CapabilityPage.
 */
public class CapabilityPage extends HeaderPageWithSash {

  /** The sofa map section. */
  private SofaMapSection sofaMapSection;

  /**
   * Instantiates a new capability page.
   *
   * @param aEditor the a editor
   */
  public CapabilityPage(MultiPageEditor aEditor) {
    super(aEditor, "Capabilities");
  }

  /**
   * Called by the framework to fill in the contents.
   *
   * @param managedForm the managed form
   */
  @Override
  protected void createFormContent(IManagedForm managedForm) {
    final Form2Panel form2Panel = setup2ColumnLayout(managedForm, editor.isAggregate() ? 50 : 90,
            editor.isAggregate() ? 50 : 10);
    managedForm.getForm().setText("Capabilities: Inputs and Outputs");
    managedForm.addPart(/* inputSection = */new CapabilitySection(editor, form2Panel.left));
    managedForm.addPart(sofaMapSection = new SofaMapSection(editor, form2Panel.right));
    createToolBarActions(managedForm);
    sashForm.setOrientation(SWT.VERTICAL);
    vaction.setChecked(true);
    haction.setChecked(false);
    managedForm.getForm().reflow(true);
  }

  /**
   * Gets the sofa map section.
   *
   * @return the sofa map section
   */
  public SofaMapSection getSofaMapSection() {
    return sofaMapSection;
  }
}
