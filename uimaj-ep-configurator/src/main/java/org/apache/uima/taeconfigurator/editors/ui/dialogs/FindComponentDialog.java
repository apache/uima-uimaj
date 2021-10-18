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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import java.util.List;

import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The Class FindComponentDialog.
 */
public class FindComponentDialog extends AbstractDialog {

  /** The output type text. */
  private Text searchByNameText, inputTypeText, outputTypeText;

  /** The look in combo. */
  private CCombo lookInCombo;

  /** The m matching delegate componet descriptors. */
  private List m_matchingDelegateComponetDescriptors;

  /** The m matching delegate component descriptions. */
  private List m_matchingDelegateComponentDescriptions;

  /** The cancel button. */
  private Button cancelButton;

  /** The Constant ALL_PROJECTS. */
  public static final String ALL_PROJECTS = "All projects";

  /** The status label 2. */
  private Label statusLabel1, statusLabel2;

  /** The m search thread. */
  private SearchThread m_searchThread = null;

  /** The component headers. */
  private String[] componentHeaders;

  /**
   * Instantiates a new find component dialog.
   *
   * @param aSection the a section
   * @param title the title
   * @param header the header
   * @param componentHeaders the component headers
   */
  public FindComponentDialog(AbstractSection aSection, String title, String header,
          String[] componentHeaders) {
    super(aSection, title, header);
    this.componentHeaders = componentHeaders;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    AbstractSection.spacer(composite);

    new Label(composite, SWT.WRAP).setText("Descriptor file name pattern (e.g. ab*cde):");
    searchByNameText = new Text(composite, SWT.BORDER);
    searchByNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(composite, SWT.WRAP).setText("Descriptor must specify the input type:");
    inputTypeText = new Text(composite, SWT.BORDER);
    inputTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(composite, SWT.WRAP).setText("Descriptor must specify the output type:");
    outputTypeText = new Text(composite, SWT.BORDER);
    outputTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    new Label(composite, SWT.WRAP).setText("Look in:");

    lookInCombo = new CCombo(composite, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
    String[] projectNames = getProjectNames();
    lookInCombo.add(' ' + ALL_PROJECTS);
    for (int i = 0; i < projectNames.length; i++) {
      lookInCombo.add(' ' + projectNames[i]);
    }
    lookInCombo.setText(' ' + ALL_PROJECTS);

    statusLabel1 = new Label(composite, SWT.NONE);
    statusLabel1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    statusLabel2 = new Label(composite, SWT.NONE);
    statusLabel2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    newErrorMessage(composite);

    return composite;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // create OK and Cancel buttons by default
    createButton(parent, IDialogConstants.OK_ID, "Search", true);
    cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, "Stop Search", false);

    cancelButton.setEnabled(false);
  }

  /**
   * Gets the project names.
   *
   * @return the project names
   */
  private String[] getProjectNames() {
    IProject[] projects = TAEConfiguratorPlugin.getWorkspace().getRoot().getProjects();
    String[] projectNames = new String[projects.length];
    for (int i = 0; i < projects.length; i++) {
      projectNames[i] = projects[i].getName();
    }

    return projectNames;
  }

  // also called by Search Monitoring Thread when
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#buttonPressed(int)
   */
  // it notices the search thread is finished
  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) { // start search
      if (null != m_searchThread) {
        errorMessageUI.setText("Search already in progress");
    }
    else {
        copyValuesFromGUI();
    }
    } else { // cancel in-progress search
      if (m_searchThread.isDone()) {
        m_matchingDelegateComponetDescriptors = m_searchThread
                .getMatchingDelegateComponentDescriptors();
        m_matchingDelegateComponentDescriptions = m_searchThread
                .getMatchingDelegateComponentDescriptions();
        m_searchThread = null;
        super.superButtonPressed(IDialogConstants.OK_ID);
      } else {
        m_searchThread.setDieNow();
        this.handleShellCloseEvent();
      }
      return;
    }
  }

  /** The Constant needToEscapeTheseChars. */
  private static final String needToEscapeTheseChars = 
    ".+{}()\\";
  
  /**
   * Convert to regex search pattern.
   *
   * @param searchPattern the search pattern
   * @return the string
   */
  private String convertToRegexSearchPattern(String searchPattern) {
    if (searchPattern == null || searchPattern.equals("")) {
      return null;
    }
    String searchPatternLowerCase = searchPattern.toLowerCase();
    StringBuffer buffer = new StringBuffer("(?i)"); // case insensitive
    for (int i = 0; i < searchPatternLowerCase.length(); i++) {
      char ch = searchPatternLowerCase.charAt(i);
      if (ch == '*') {
        buffer.append(".*");
      } else if (0 <= needToEscapeTheseChars.indexOf(ch)) {
        buffer.append('\\').append(ch);
      } else {
        buffer.append(ch);
      }
    }

    return new String(buffer);
  }

  /**
   * Gets the matching delegate component descriptors.
   *
   * @return the matching delegate component descriptors
   */
  public List getMatchingDelegateComponentDescriptors() {
    return m_matchingDelegateComponetDescriptors;
  }

  /**
   * Gets the matching delegate component descriptions.
   *
   * @return the matching delegate component descriptions
   */
  public List getMatchingDelegateComponentDescriptions() {
    return m_matchingDelegateComponentDescriptions;
  }

  /**
   * Gets the status label 1.
   *
   * @return the status label 1
   */
  public Label getStatusLabel1() {
    return statusLabel1;
  }

  /**
   * Gets the status label 2.
   *
   * @return the status label 2
   */
  public Label getStatusLabel2() {
    return statusLabel2;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    String fileNameSearch = convertToRegexSearchPattern(searchByNameText.getText());
    String inputTypeSearch = convertToRegexSearchPattern(inputTypeText.getText());
    String outputTypeSearch = convertToRegexSearchPattern(outputTypeText.getText());
    String projectToSearch = lookInCombo.getText().substring(1);
    m_searchThread = new SearchThread(this, section, fileNameSearch, inputTypeSearch,
            outputTypeSearch, projectToSearch, componentHeaders);
    cancelButton.setEnabled(true);
    Thread searchThreadThread = new Thread(m_searchThread);
    searchThreadThread.start();

    SearchMonitoringThread monitoringThread = new SearchMonitoringThread(this, m_searchThread);
    monitoringThread.start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
  }

}
