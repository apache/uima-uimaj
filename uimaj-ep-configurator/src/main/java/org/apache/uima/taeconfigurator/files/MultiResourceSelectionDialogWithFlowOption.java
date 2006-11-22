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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

public class MultiResourceSelectionDialogWithFlowOption extends MultiResourceSelectionDialog {

  private Button autoAddToFlowButton;

  private boolean m_bAutoAddToFlow = true;

  /**
   * @param parentShell
   * @param rootElement
   * @param message
   * @param excludeDescriptor
   */
  public MultiResourceSelectionDialogWithFlowOption(Shell parentShell, IAdaptable rootElement,
                  String message, IPath excludeDescriptor, MultiPageEditor editor) {
    super(parentShell, rootElement, message, excludeDescriptor, editor);
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    new Label(composite, SWT.WRAP).setText(""); //$NON-NLS-1$

    String sAddToFlowPrompt = Messages
                    .getString("MultiResourceSelectionDialogWithFlowOption.addSelectedAEsToEndOfFlow"); //$NON-NLS-1$
    FormToolkit factory = new FormToolkit(TAEConfiguratorPlugin.getDefault().getFormColors(
                    parent.getDisplay()));

    autoAddToFlowButton = factory.createButton(composite, sAddToFlowPrompt, SWT.CHECK);
    autoAddToFlowButton.setSelection(m_bAutoAddToFlow);
    autoAddToFlowButton.setBackground(null);

    return composite;
  }

  protected void okPressed() {
    m_bAutoAddToFlow = autoAddToFlowButton.getSelection();
    super.okPressed();
  }

  public boolean getAutoAddToFlow() {
    return m_bAutoAddToFlow;
  }

}
