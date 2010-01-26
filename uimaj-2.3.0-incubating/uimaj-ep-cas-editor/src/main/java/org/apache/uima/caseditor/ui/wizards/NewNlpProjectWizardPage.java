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

package org.apache.uima.caseditor.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * TODO:
 * should not be possible to crate an existing project
 * check if project name contains illegal chars
 */
public class NewNlpProjectWizardPage extends WizardPage {

  private Text projectNameField;

  protected NewNlpProjectWizardPage() {
    super("NewProjectWizard");
  }

  public void createControl(Composite parent) {

    Composite base = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    base.setLayout(layout);
    base.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label projectLabel = new Label(base, SWT.NONE);
    projectLabel.setText("Project name:");
    projectLabel.setFont(parent.getFont());

    projectNameField = new Text(base, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = 123;
    projectNameField.setLayoutData(data);
    projectNameField.setFont(parent.getFont());

    setControl(base);
  }

  public IProject getProjectHandle() {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(
            projectNameField.getText().trim());
  }

  public IPath getLocationPath() {
    return getProjectHandle().getParent().getLocation();
  }
}
