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


/**
 * The Class SettingsPage.
 */
public class SettingsPage extends HeaderPageWithSash {

  /** The parameter settings section. */
  private ParameterSettingsSection parameterSettingsSection;

  /** The value section. */
  private ValueSection valueSection;

  /**
   * Instantiates a new settings page.
   *
   * @param editor the editor
   */
  public SettingsPage(MultiPageEditor editor) {
//IC see: https://issues.apache.org/jira/browse/UIMA-48
    super(editor, "Parameter Value Settings");
  }

  /**
   * Called by the framework to fill in the contents.
   *
   * @param managedForm the managed form
   */
  @Override
  protected void createFormContent(IManagedForm managedForm) {

    final Form2Panel form2Panel = setup2ColumnLayout(managedForm, EQUAL_WIDTH);

    managedForm.getForm().setText("Parameter Settings");
    managedForm.addPart(parameterSettingsSection = new ParameterSettingsSection(editor,
//IC see: https://issues.apache.org/jira/browse/UIMA-48
            form2Panel.left));
    managedForm.addPart(valueSection = new ValueSection(editor, form2Panel.right));
    createToolBarActions(managedForm);
  }

  /**
   * Gets the parameter settings section.
   *
   * @return the parameter settings section
   */
  public ParameterSettingsSection getParameterSettingsSection() {
    return parameterSettingsSection;
  }

  /**
   * Gets the value section.
   *
   * @return the value section
   */
  public ValueSection getValueSection() {
    return valueSection;
  }

}
