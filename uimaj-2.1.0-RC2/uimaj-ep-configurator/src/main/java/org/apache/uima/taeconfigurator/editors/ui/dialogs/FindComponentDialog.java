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

import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class FindComponentDialog extends AbstractDialog {

  private Text searchByNameText, inputTypeText, outputTypeText;

  private CCombo lookInCombo;

  private List m_matchingDelegateComponetDescriptors;

  private List m_matchingDelegateComponentDescriptions;

  private Button cancelButton;

  public static final String ALL_PROJECTS = "All projects";

  private Label statusLabel1, statusLabel2;

  private SearchThread m_searchThread = null;

  private String[] componentHeaders;

  /**
   * @param parentShell
   */
  public FindComponentDialog(AbstractSection aSection, String title, String header,
          String[] componentHeaders) {
    super(aSection, title, header);
    this.componentHeaders = componentHeaders;
  }

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

  protected void createButtonsForButtonBar(Composite parent) {
    // create OK and Cancel buttons by default
    createButton(parent, IDialogConstants.OK_ID, "Search", true);
    cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, "Stop Search", false);

    cancelButton.setEnabled(false);
  }

  private String[] getProjectNames() {
    IProject[] projects = TAEConfiguratorPlugin.getWorkspace().getRoot().getProjects();
    String[] projectNames = new String[projects.length];
    for (int i = 0; i < projects.length; i++) {
      projectNames[i] = projects[i].getName();
    }

    return projectNames;
  }

  // also called by Search Monitoring Thread when
  // it notices the search thread is finished
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) { // start search
      if (null != m_searchThread)
        errorMessageUI.setText("Search already in progress");
      else
        copyValuesFromGUI();
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

  private static final String needToEscapeTheseChars = 
    ".+{}()\\";
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

  public List getMatchingDelegateComponentDescriptors() {
    return m_matchingDelegateComponetDescriptors;
  }

  public List getMatchingDelegateComponentDescriptions() {
    return m_matchingDelegateComponentDescriptions;
  }

  public Label getStatusLabel1() {
    return statusLabel1;
  }

  public Label getStatusLabel2() {
    return statusLabel2;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
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
  public boolean isValid() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  public void enableOK() {
  }

}
