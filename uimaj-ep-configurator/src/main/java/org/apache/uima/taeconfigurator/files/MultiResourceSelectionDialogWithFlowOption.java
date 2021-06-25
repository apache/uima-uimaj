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

import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;


/**
 * The Class MultiResourceSelectionDialogWithFlowOption.
 */
public class MultiResourceSelectionDialogWithFlowOption extends MultiResourceSelectionDialog {

  /** The auto add to flow button. */
  private Button autoAddToFlowButton;

  /** The m b auto add to flow. */
  private boolean m_bAutoAddToFlow = true;

  /**
   * Instantiates a new multi resource selection dialog with flow option.
   *
   * @param parentShell the parent shell
   * @param rootElement the root element
   * @param message the message
   * @param excludeDescriptor the exclude descriptor
   * @param editor the editor
   */
  public MultiResourceSelectionDialogWithFlowOption(Shell parentShell, IAdaptable rootElement,
          String message, IPath excludeDescriptor, MultiPageEditor editor) {
    super(parentShell, rootElement, message, excludeDescriptor, editor);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.files.MultiResourceSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    new Label(composite, SWT.WRAP).setText(""); //$NON-NLS-1$

    String sAddToFlowPrompt = Messages
            .getString("MultiResourceSelectionDialogWithFlowOption.addSelectedAEsToEndOfFlow"); //$NON-NLS-1$
    FormToolkit factory = new FormToolkit(TAEConfiguratorPlugin.getDefault().getFormColors(
            parent.getDisplay()));

    autoAddToFlowButton = factory.createButton(composite, sAddToFlowPrompt, SWT.CHECK);
    m_bAutoAddToFlow = "false".equals(CDEpropertyPage.getAddToFlow(editor.getProject())) ? false : true;
    autoAddToFlowButton.setSelection(m_bAutoAddToFlow);
    autoAddToFlowButton.setBackground(null);

    return composite;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    m_bAutoAddToFlow = autoAddToFlowButton.getSelection();
    CDEpropertyPage.setAddToFlow(editor.getProject(), m_bAutoAddToFlow ? "true" : "false");
    super.okPressed();
  }

  /**
   * Gets the auto add to flow.
   *
   * @return the auto add to flow
   */
  public boolean getAutoAddToFlow() {
    return m_bAutoAddToFlow;
  }

}
