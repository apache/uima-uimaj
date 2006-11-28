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
import org.eclipse.ui.forms.IManagedForm;

public class ParameterPage extends HeaderPageWithSash {

  private ParameterSection parameterSection;

  private ParameterDelegatesSection parameterDelegatesSection;

  public ParameterPage(MultiPageEditor editor) {
    super(editor, "Configuration Parameters");
  }

  /**
   * Called by the framework to fill in the contents
   */
  protected void createFormContent(IManagedForm managedForm) {
    managedForm.getForm().setText("Parameter Definitions");
    Form2Panel form2Panel = setup2ColumnLayout(managedForm, 55, 45);
    managedForm.addPart(parameterSection = new ParameterSection(editor, form2Panel.left));
    managedForm.addPart(parameterDelegatesSection = new ParameterDelegatesSection(editor,
            form2Panel.right));
    createToolBarActions(managedForm);
  }

  public ParameterDelegatesSection getParameterDelegatesSection() {
    return parameterDelegatesSection;
  }

  public ParameterSection getParameterSection() {
    return parameterSection;
  }

}
