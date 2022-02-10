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

/**
 * The Class CDEpropertyPage.
 */
public class CDEpropertyPage extends PropertyPage {

  /** The Constant DATAPATH_LABEL. */
  private static final String DATAPATH_LABEL = "&Data Path:";

  /** The Constant DATAPATH_PROPERTY_KEY. */
  private static final String DATAPATH_PROPERTY_KEY = "CDEdataPath";

  /** The Constant DEFAULT_DATAPATH. */
  private static final String DEFAULT_DATAPATH = "";

  /** The Constant BY_DEFAULT_PROPERTY_KEY. */
  private static final String BY_DEFAULT_PROPERTY_KEY = "CDEByDefault";

  /** The Constant ADD_TO_FLOW_PROPERTY_KEY. */
  private static final String ADD_TO_FLOW_PROPERTY_KEY = "CDEAddToFlow";

  // private static final int TEXT_FIELD_WIDTH = 50;

  /** The data path UI. */
  private Text dataPathUI;

  /**
   * Instantiates a new CD eproperty page.
   */
  public CDEpropertyPage() {
  }

  /**
   * Creates the contents.
   *
   * @param parent
   *          the parent
   * @return the control
   * @see PreferencePage#createContents(Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite composite = create2ColComposite(parent);

    Label instructions = new Label(composite, SWT.WRAP);
    instructions.setText("Enter the data path to use for finding resources by name;\n"
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
      String dataPath = ((IResource) getElement())
              .getPersistentProperty(new QualifiedName("", DATAPATH_PROPERTY_KEY));
      dataPathUI.setText((dataPath != null) ? dataPath : DEFAULT_DATAPATH);
    } catch (CoreException e) {
      dataPathUI.setText(DEFAULT_DATAPATH);
    }
    return composite;
  }

  /**
   * Creates the 2 col composite.
   *
   * @param parent
   *          the parent
   * @return the composite
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    dataPathUI.setText(DEFAULT_DATAPATH);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    // store the value in the owner text field
    try {
      ((IResource) getElement()).setPersistentProperty(new QualifiedName("", DATAPATH_PROPERTY_KEY),
              dataPathUI.getText());
    } catch (CoreException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the data path.
   *
   * @param project
   *          the project
   * @return the data path
   */
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

  /**
   * Sets the data path.
   *
   * @param project
   *          the project
   * @param dataPath
   *          the data path
   */
  public static void setDataPath(IProject project, String dataPath) {
    try {
      project.setPersistentProperty(new QualifiedName("", DATAPATH_PROPERTY_KEY), dataPath);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
  }

  /**
   * Gets the import by default.
   *
   * @param project
   *          the project
   * @return the import by default
   */
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

  /**
   * Sets the import by default.
   *
   * @param project
   *          the project
   * @param byDefault
   *          the by default
   */
  public static void setImportByDefault(IProject project, String byDefault) {
    try {
      project.setPersistentProperty(new QualifiedName("", BY_DEFAULT_PROPERTY_KEY), byDefault);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
  }

  /**
   * Gets the adds the to flow.
   *
   * @param project
   *          the project
   * @return the adds the to flow
   */
  public static String getAddToFlow(IProject project) {
    String byDefault;
    try {
      byDefault = project.getPersistentProperty(new QualifiedName("", ADD_TO_FLOW_PROPERTY_KEY));
    } catch (CoreException e) {
      byDefault = "";
    }
    if (null == byDefault)
      byDefault = "";
    return byDefault;
  }

  /**
   * Sets the add to flow.
   *
   * @param project
   *          the project
   * @param byDefault
   *          the by default
   */
  public static void setAddToFlow(IProject project, String byDefault) {
    try {
      project.setPersistentProperty(new QualifiedName("", ADD_TO_FLOW_PROPERTY_KEY), byDefault);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled exception", e);
    }
  }

}
