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
 * 
 */
public class OverviewPage extends HeaderPageWithSash {

  private PrimitiveSection primitiveSection;

  /**
   */
  public OverviewPage(MultiPageEditor aEditor) {
    super(aEditor, "UID_OverviewPage", "Overview");
  }

  /**
   * Called by the 3.0 framework to fill in the contents
   */
  protected void createFormContent(IManagedForm managedForm) {
    final Form2Panel form = setup2ColumnLayout(managedForm, EQUAL_WIDTH);
    managedForm.getForm().setText("Overview");

    if (isLocalProcessingDescriptor()) {
      managedForm.addPart(new GeneralSection(editor, form.left));
      managedForm.addPart(primitiveSection = new PrimitiveSection(editor, form.left));
      managedForm.addPart(new MetaDataSection(editor, form.right));
    } else {
      managedForm.addPart(new MetaDataSection(editor, form.left));
    }
    createToolBarActions(managedForm);
    sashForm.setOrientation(SWT.HORIZONTAL);
    vaction.setChecked(false);
    haction.setChecked(true);
    if (!isLocalProcessingDescriptor()) {
      sashForm.setWeights(new int[] { 95, 5 });
    }
  }

  public PrimitiveSection getPrimitiveSection() {
    return primitiveSection;
  }

}
