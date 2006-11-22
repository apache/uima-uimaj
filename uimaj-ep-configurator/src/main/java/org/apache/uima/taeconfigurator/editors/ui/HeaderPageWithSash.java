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

import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 */
public class HeaderPageWithSash extends HeaderPage {

  /**
   * @param formEditor
   * @param id
   * @param keyPageTitle
   */

  protected Action haction;

  protected Action vaction;

  public HeaderPageWithSash(MultiPageEditor formEditor, String id, String keyPageTitle) {
    super(formEditor, id, keyPageTitle);
  }

  /**
   * @param formEditor
   * @param pageTitle
   */
  public HeaderPageWithSash(MultiPageEditor formEditor, String pageTitle) {
    super(formEditor, pageTitle);
  }

  protected void createToolBarActions(IManagedForm managedForm) {
    final ScrolledForm form = managedForm.getForm();

    haction = new Action("hor", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
      public void run() {
        sashForm.setOrientation(SWT.HORIZONTAL);
        form.reflow(true);
      }
    };
    haction.setChecked(true);
    haction.setToolTipText("Horizontal Orientation");
    haction.setImageDescriptor(TAEConfiguratorPlugin
                    .getImageDescriptor(TAEConfiguratorPlugin.IMAGE_TH_HORIZONTAL));
    haction.setDisabledImageDescriptor(TAEConfiguratorPlugin
                    .getImageDescriptor(TAEConfiguratorPlugin.IMAGE_TH_HORIZONTAL));

    vaction = new Action("ver", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
      public void run() {
        sashForm.setOrientation(SWT.VERTICAL);
        form.reflow(true);
      }
    };
    vaction.setChecked(false);
    vaction.setToolTipText("Vertical Orientation");
    vaction.setImageDescriptor(TAEConfiguratorPlugin
                    .getImageDescriptor(TAEConfiguratorPlugin.IMAGE_TH_VERTICAL));
    vaction.setDisabledImageDescriptor(TAEConfiguratorPlugin
                    .getImageDescriptor(TAEConfiguratorPlugin.IMAGE_TH_VERTICAL));
    form.getToolBarManager().add(haction);
    form.getToolBarManager().add(vaction);
    form.updateToolBar();
    maybeInitialize(managedForm);
  }

}
