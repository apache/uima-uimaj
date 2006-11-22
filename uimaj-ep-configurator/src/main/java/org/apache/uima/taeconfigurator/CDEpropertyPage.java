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

package org.apache.uima.taeconfigurator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class CDEpropertyPage extends PropertyPage {

  private static final String DATAPATH_LABEL = "&Data Path:";

  private static final String DATAPATH_PROPERTY_KEY = "CDEdataPath";

  private static final String DEFAULT_DATAPATH = "";

  private static final String BY_DEFAULT_PROPERTY_KEY = "CDEByDefault";

  // private static final int TEXT_FIELD_WIDTH = 50;

  private Text dataPathUI;

  public CDEpropertyPage() {
    super();
  }

  /**
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite parent) {
    Composite composite = create2ColComposite(parent);

    Label instructions = new Label(composite, SWT.WRAP);
    instructions
                    .setText("Enter the data path to use for finding resources by name;\n"
                                    + "This is a series of absolute paths, separated by\n"
                                    + "whatever character this platform uses for path separation (similar to class paths).\n\n");
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    gd.grabExcessHorizontalSpace = true;
    instructions.setLayoutData(gd);

    new Label(composite, SWT.NONE).setText(DATAPATH_LABEL);

    dataPathUI = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    gd = new GridData(GridData.FILL_BOTH);
    dataPathUI.setLayoutData(gd);

    try {
      String dataPath = ((IResource) getElement()).getPersistentProperty(new QualifiedName("",
                      DATAPATH_PROPERTY_KEY));
      dataPathUI.setText((dataPath != null) ? dataPath : DEFAULT_DATAPATH);
    } catch (CoreException e) {
      dataPathUI.setText(DEFAULT_DATAPATH);
    }
    return composite;
  }

  private Composite create2ColComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);

    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    return composite;
  }

  protected void performDefaults() {
    dataPathUI.setText(DEFAULT_DATAPATH);
  }

  public boolean performOk() {
    // store the value in the owner text field
    try {
      ((IResource) getElement()).setPersistentProperty(
                      new QualifiedName("", DATAPATH_PROPERTY_KEY), dataPathUI.getText());
    } catch (CoreException e) {
      return false;
    }
    return true;
  }

  public static String getDataPath(IProject project) {
    String dataPath;
    try {
      dataPath = project.getPersistentProperty(new QualifiedName("", DATAPATH_PROPERTY_KEY));
    } catch (CoreException e) {
      dataPath = "";
    }
    if (null == dataPath)
      dataPath = "";
    return dataPath;
  }

  public static void setDataPath(IProject project, String dataPath) {
    try {
      project.setPersistentProperty(new QualifiedName("", DATAPATH_PROPERTY_KEY), dataPath);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
  }

  public static String getImportByDefault(IProject project) {
    String byDefault;
    try {
      byDefault = project.getPersistentProperty(new QualifiedName("", BY_DEFAULT_PROPERTY_KEY));
    } catch (CoreException e) {
      byDefault = "";
    }
    if (null == byDefault)
      byDefault = "";
    return byDefault;
  }

  public static void setImportByDefault(IProject project, String byDefault) {
    try {
      project.setPersistentProperty(new QualifiedName("", BY_DEFAULT_PROPERTY_KEY), byDefault);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
  }

}
