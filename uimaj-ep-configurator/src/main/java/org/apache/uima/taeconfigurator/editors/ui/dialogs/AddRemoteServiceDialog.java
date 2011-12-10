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

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class AddRemoteServiceDialog extends AbstractDialog {

  private String m_selectedServiceTypeName = null;

  private String m_selectedUri = null;

  private String m_selectedKey = null;

  private boolean m_bAutoAddToFlow;

  public String vnsPort;

  public String vnsHost;

  public String timeout;

  public String aeOrCc;

  private CCombo serviceTypeCombo;

  private Text uriText;
  
  private Text endpointText;

  private Text keyText;

  private Text timeoutText;

  private Button autoAddToFlowButton;

  private Button importByNameUI;

  private Button importByLocationUI;

  public boolean isImportByName;

  private String rootPath;

  private DialogModifyListener m_dialogModifyListener = new DialogModifyListener();

  private Text genFilePathUI;

  public String genFilePath;

  private String keyTextPrev;

  private Label vnsHostLabel;

  private Text vnsHostUI;

  private Label vnsPortLabel;

  private Text vnsPortUI;

  private boolean portNumberWasBad;

  private boolean portNumberIsOK;

  private CCombo aeOrCcCombo;
  
  private Label endpointLabel;
  
  private Label uriLabel;

  private Label timeoutProcessLabel;

  private Label timeoutJmsGetmetaLabel;

  private Text timeoutGetmetaText;

  public String getmetaTimeout;

  public String endpoint;

  private Text timeoutJmsCpcText;

  private Label timeoutJmsCpcLabel;

  private Label binarySerializationLabel;

  private CCombo binarySerializationCombo;

  private Label ignoreProcessErrorsLabel;

  private CCombo ignoreProcessErrorsCombo;

  public String cpcTimeout;

  public String binary_serialization;

  public String ignore_process_errors;

  private class DialogModifyListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      String text = genFilePathUI.getText();
      int pos = text.lastIndexOf(keyTextPrev);
      if (pos == -1)
        pos = text.length();
      keyTextPrev = keyText.getText() + ".xml";
      genFilePathUI.setText(text.substring(0, pos) + keyTextPrev);
      if (okButton != null)
        enableOK();
    }
  }

  private class DialogVerifyListener implements VerifyListener {
    public void verifyText(VerifyEvent e) {
      if (0 <= e.text.indexOf('.')) {
        setErrorMessage(MessageFormat.format("invalid character(s): ''{0}''",
                new Object[] { e.text }));
        e.doit = false;
      } else
        setErrorMessage("");
    }
  }

  public AddRemoteServiceDialog(AbstractSection aSection, String aRootPath) {
    super(aSection, "Add Remote Service",
            "Fill in the information about the remote service and press OK");
    rootPath = aRootPath;
  }

  protected Control createDialogArea(Composite parent) {

    Composite composite = (Composite) super.createDialogArea(parent);
    
    Composite tc1 = new2ColumnComposite(composite);
    Label tempLabel;
    
    setTextAndTip(tempLabel = new Label(tc1, SWT.WRAP), "Service kind:", S_, SWT.BEGINNING, false);    
    aeOrCcCombo = wideCCombo(tc1, "Specify whether the Service is an Analysis Engine or a Cas Consumer",
            "AnalysisEngine", "CasConsumer");

    setTextAndTip(tempLabel = new Label(tc1, SWT.WRAP), "Protocol Service Type", S_, SWT.BEGINNING, false);
    serviceTypeCombo = wideCCombo(tc1, S_, "UIMA-AS JMS", "SOAP", "Vinci");

    setTextAndTip(uriLabel = new Label(tc1, SWT.NONE), "URI of service or JMS Broker:",
       "The URI for the service, e.g. localhost", SWT.BEGINNING, false);
    uriText = wideTextInput(tc1, S_, m_dialogModifyListener);

    setTextAndTip(endpointLabel = new Label(tc1, SWT.NONE), "Endpoint Name (JMS Service):",
    "For UIMA-AS JMS Services only, the endpoint name", SWT.BEGINNING, false);

    endpointText = wideTextInput(tc1, S_, m_dialogModifyListener);

    setTextAndTip(binarySerializationLabel = new Label(tc1, SWT.NONE), "Binary Serialization (JMS Service):",
        "For UIMA-AS JMS Services only, use binary serialzation (requires all type systems be identical)", 
        SWT.BEGINNING, false);

    binarySerializationCombo = wideCComboTF(tc1, S_);
    
    setTextAndTip(ignoreProcessErrorsLabel = new Label(tc1, SWT.NONE), "Ignore Process Errors (JMS Service):",
    "For UIMA-AS JMS Services only, ignore processing errors");
    ignoreProcessErrorsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    ignoreProcessErrorsCombo = wideCComboTF(tc1, S_);   

    setTextAndTip(tempLabel = new Label(tc1, SWT.NONE), "Key (a short mnemonic for this service):",
        "also used as part of the file name", SWT.BEGINNING, false);
    keyText = wideTextInput(tc1, S_, m_dialogModifyListener);
    keyText.addVerifyListener(new DialogVerifyListener());
    keyTextPrev = ".xml";

    createWideLabel(composite, "Where the generated remote descriptor file will be stored:");
    genFilePathUI = new Text(composite, SWT.BORDER | SWT.H_SCROLL);
    genFilePathUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    genFilePathUI.setText(rootPath + ".xml");

    createWideLabel(
            composite,
            "Timeouts, in milliseconds.  This is ignored for the Vinci protocol.  Specify 0 to wait forever. If not specified, a default timeout is used.");
    
    tc1 = new2ColumnComposite(composite);
    
    setTextAndTip(timeoutProcessLabel = new Label(tc1, SWT.NONE), "Timeout: Process:",
        "Timeout for processing a CAS", SWT.BEGINNING, false);   
    timeoutText = wideTextInput(tc1, S_);

    setTextAndTip(timeoutJmsGetmetaLabel = new Label(tc1, SWT.NONE), "Timeout: (JMS) GetMeta:",
    "Timeout for querying the metadata from a JMS service", SWT.BEGINNING, false);
    timeoutGetmetaText = wideTextInput(tc1, S_);

    setTextAndTip(timeoutJmsCpcLabel = new Label(tc1, SWT.NONE), "Timeout: (JMS) Collection Processing Complete:",
    "Timeout for Collection Processing Complete", SWT.BEGINNING, false);   
    timeoutJmsCpcText = wideTextInput(tc1, S_);
    createWideLabel(composite,
            "For the Vinci protocol, you can optionally specify the Host/Port for the Vinci Name Service");
    Composite tc = new2ColumnComposite(composite);
    setTextAndTip(vnsHostLabel = new Label(tc, SWT.NONE), "VNS HOST",
            "An IP name or address, e.g. localhost");
    vnsHostUI = newText(tc, SWT.NONE, "An IP name or address, e.g. localhost");
    setTextAndTip(vnsPortLabel = new Label(tc, SWT.NONE), "VNS PORT", "A port number, e.g. 9000");
    vnsPortUI = newText(tc, SWT.NONE, "A port number, e.g. 9000");

    newErrorMessage(composite);

    autoAddToFlowButton = new Button(composite, SWT.CHECK);
    autoAddToFlowButton.setText("Add to end of flow");
    autoAddToFlowButton.setSelection(true);

    new Label(composite, SWT.NONE).setText("");
    importByNameUI = new Button(composite, SWT.RADIO);
    importByNameUI.setText("Import by Name");
    importByNameUI
            .setToolTipText("Importing by name looks up the name on the classpath and datapath.");
    importByNameUI.setSelection(true);

    importByLocationUI = new Button(composite, SWT.RADIO);
    importByLocationUI.setText("Import By Location");
    importByLocationUI.setToolTipText("Importing by location requires a relative or absolute URL");

    String defaultBy = CDEpropertyPage.getImportByDefault(editor.getProject());
    if (defaultBy.equals("location")) {
      importByNameUI.setSelection(false);
      importByLocationUI.setSelection(true);
    } else {
      importByNameUI.setSelection(true);
      importByLocationUI.setSelection(false);
    }

    return composite;
  }

  private CCombo wideCCombo(Composite tc, String tip, String ... entries) {
    CCombo cc = newCCombo(tc, tip);
    for (String e : entries) {
      cc.add(e);
    }
    ((GridData) cc.getLayoutData()).grabExcessHorizontalSpace = true;;
    cc.select(0);
    return cc;
  }
  
  private CCombo wideCComboTF(Composite tc, String tip) {
    return wideCCombo(tc, tip, "false", "true");
  }
 
  private Text wideTextInput(Composite tc, String tip) {
    return wideTextInput(tc, tip, null);
  }
  
  private Text wideTextInput(Composite tc, String tip, DialogModifyListener listener) {
    Text t = newText(tc, SWT.BORDER, tip);
    t.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    if (listener != null) {
      t.addModifyListener(listener);
    } else {
      t.setEnabled(false);
    }
    return t;
  }
  
  
  public void enableOK() {
    boolean isVinci = serviceTypeCombo.getSelectionIndex() == 2;
    boolean isJms = serviceTypeCombo.getSelectionIndex() == 0;
    vnsHostLabel.setEnabled(isVinci);
    vnsHostUI.setEnabled(isVinci);
    vnsPortLabel.setEnabled(isVinci);
    vnsPortUI.setEnabled(isVinci);
    timeoutText.setEnabled(!isVinci);
    timeoutProcessLabel.setEnabled(!isVinci);
    timeoutJmsGetmetaLabel.setEnabled(isJms);
    timeoutGetmetaText.setEnabled(isJms);
    endpointLabel.setEnabled(isJms);
    endpointText.setEnabled(isJms);
    timeoutJmsCpcLabel.setEnabled(isJms);
    timeoutJmsCpcText.setEnabled(isJms);
    binarySerializationLabel.setEnabled(isJms);
    binarySerializationCombo.setEnabled(isJms);
    binarySerializationCombo.setVisible(isJms);
    ignoreProcessErrorsLabel.setEnabled(isJms);
    ignoreProcessErrorsCombo.setEnabled(isJms);
    ignoreProcessErrorsCombo.setVisible(isJms);

    boolean bEnableOk = (serviceTypeCombo.getText() != null && !serviceTypeCombo.getText().equals(
            ""))
            && (uriText != null && !uriText.getText().trim().equals(""))
            && (keyText != null && !keyText.getText().trim().equals(""));
    
    if (!bEnableOk) {
      setErrorMessage("missing URI or key");
    }

    portNumberIsOK = true;
    if (isVinci && vnsPortUI.getText().length() > 0) {
      try {
        Integer.parseInt(vnsPortUI.getText());
      } catch (NumberFormatException e) {
        bEnableOk = false;
        portNumberWasBad = true;
        portNumberIsOK = false;
        setErrorMessage("Invalid number, please correct.");
      }
    }
    
    if (isJms && 
        (endpointText.getText() == null || 
         endpointText.getText().trim().equals(""))) {
      bEnableOk = false;
      setErrorMessage("missing JMS endpoint");
    }    
    
    okButton.setEnabled(bEnableOk);
    if (bEnableOk) {
      setErrorMessage("");
      portNumberWasBad = false;
    }
  }

  public String getSelectedServiceTypeName() {
    return m_selectedServiceTypeName;
  }

  public String getSelectedUri() {
    return m_selectedUri;
  }

  public String getSelectedKey() {
    return m_selectedKey;
  }

  public boolean getAutoAddToFlow() {
    return m_bAutoAddToFlow;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  public void copyValuesFromGUI() {
    genFilePath = genFilePathUI.getText();
    isImportByName = importByNameUI.getSelection();
    m_selectedServiceTypeName = serviceTypeCombo.getText();
    aeOrCc = aeOrCcCombo.getText();
    m_selectedUri = uriText.getText();
    m_selectedKey = keyText.getText();
    m_bAutoAddToFlow = autoAddToFlowButton.getSelection();
    timeout = timeoutText.getText();
    vnsHost = vnsHostUI.getText();
    vnsPort = vnsPortUI.getText();
    CDEpropertyPage.setImportByDefault(editor.getProject(), isImportByName ? "name" : "location");
    getmetaTimeout = timeoutGetmetaText.getText();
    cpcTimeout = timeoutJmsCpcText.getText();
    endpoint = endpointText.getText();
    binary_serialization = binarySerializationCombo.getText();
    ignore_process_errors = ignoreProcessErrorsCombo.getText();
  }

  public boolean isValid() {
    return true;
  }

}
