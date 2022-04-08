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

import org.apache.uima.Constants;
import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
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

/**
 * The Class AddRemoteServiceDialog.
 */
public class AddRemoteServiceDialog extends AbstractDialog {

  /** The m selected service type name. */
  private String m_selectedServiceTypeName = null;

  /** The m selected uri. */
  private String m_selectedUri = null;

  /** The m selected key. */
  private String m_selectedKey = null;

  /** The m b auto add to flow. */
  private boolean m_bAutoAddToFlow;

  /** The vns port. */
  public String vnsPort;

  /** The vns host. */
  public String vnsHost;

  /** The timeout. */
  public String timeout;

  /** The ae or cc. */
  public String aeOrCc;

  /** The service type combo. */
  private CCombo serviceTypeCombo;

  /** The uri text. */
  private Text uriText;

  /** The endpoint text. */
  private Text endpointText;

  /** The key text. */
  private Text keyText;

  /** The timeout text. */
  private Text timeoutText;

  /** The auto add to flow button. */
  private Button autoAddToFlowButton;

  /** The import by name UI. */
  private Button importByNameUI;

  /** The import by location UI. */
  private Button importByLocationUI;

  /** The is import by name. */
  public boolean isImportByName;

  /** The root path. */
  private String rootPath;

  /** The m dialog modify listener. */
  private DialogModifyListener m_dialogModifyListener = new DialogModifyListener();

  /** The gen file path UI. */
  private Text genFilePathUI;

  /** The gen file path. */
  public String genFilePath;

  /** The key text prev. */
  private String keyTextPrev;

  /** The vns host label. */
  private Label vnsHostLabel;

  /** The vns host UI. */
  private Text vnsHostUI;

  /** The vns port label. */
  private Label vnsPortLabel;

  /** The vns port UI. */
  private Text vnsPortUI;

  /** The port number was bad. */
  private boolean portNumberWasBad;

  /** The port number is OK. */
  private boolean portNumberIsOK;

  /** The ae or cc combo. */
  private CCombo aeOrCcCombo;

  /** The endpoint label. */
  private Label endpointLabel;

  /** The uri label. */
  private Label uriLabel;

  /** The timeout process label. */
  private Label timeoutProcessLabel;

  /** The timeout jms getmeta label. */
  private Label timeoutJmsGetmetaLabel;

  /** The timeout getmeta text. */
  private Text timeoutGetmetaText;

  /** The getmeta timeout. */
  public String getmetaTimeout;

  /** The endpoint. */
  public String endpoint;

  /** The timeout jms cpc text. */
  private Text timeoutJmsCpcText;

  /** The timeout jms cpc label. */
  private Label timeoutJmsCpcLabel;

  /** The binary serialization label. */
  private Label binarySerializationLabel;

  /** The binary serialization combo. */
  private CCombo binarySerializationCombo;

  /** The ignore process errors label. */
  private Label ignoreProcessErrorsLabel;

  /** The ignore process errors combo. */
  private CCombo ignoreProcessErrorsCombo;

  /** The cpc timeout. */
  public String cpcTimeout;

  /** The binary serialization. */
  public String binary_serialization;

  /** The ignore process errors. */
  public String ignore_process_errors;

  /**
   * The listener interface for receiving dialogModify events. The class that is interested in
   * processing a dialogModify event implements this interface, and the object created with that
   * class is registered with a component using the component's <code>addDialogModifyListener</code>
   * method. When the dialogModify event occurs, that object's appropriate method is invoked.
   *
   * @see DialogModifyEvent
   */
  private class DialogModifyListener implements ModifyListener {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
     */
    @Override
    public void modifyText(ModifyEvent e) {
      String text = genFilePathUI.getText();
      int pos = text.lastIndexOf(keyTextPrev);
      if (pos == -1) {
        pos = text.length();
      }
      keyTextPrev = keyText.getText() + ".xml";
      genFilePathUI.setText(text.substring(0, pos) + keyTextPrev);
      if (okButton != null) {
        enableOK();
      }
    }
  }

  /**
   * The listener interface for receiving dialogVerify events. The class that is interested in
   * processing a dialogVerify event implements this interface, and the object created with that
   * class is registered with a component using the component's <code>addDialogVerifyListener</code>
   * method. When the dialogVerify event occurs, that object's appropriate method is invoked.
   *
   * @see DialogVerifyEvent
   */
  private class DialogVerifyListener implements VerifyListener {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.VerifyListener#verifyText(org.eclipse.swt.events.VerifyEvent)
     */
    @Override
    public void verifyText(VerifyEvent e) {
      if (0 <= e.text.indexOf('.')) {
        setErrorMessage(
                MessageFormat.format("invalid character(s): ''{0}''", new Object[] { e.text }));
        e.doit = false;
      } else {
        setErrorMessage("");
      }
    }
  }

  /**
   * Instantiates a new adds the remote service dialog.
   *
   * @param aSection
   *          the a section
   * @param aRootPath
   *          the a root path
   */
  public AddRemoteServiceDialog(AbstractSection aSection, String aRootPath) {
    super(aSection, "Add Remote Service",
            "Fill in the information about the remote service and press OK");
    rootPath = aRootPath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.
   * swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    Composite composite = (Composite) super.createDialogArea(parent);

    Composite tc1 = new2ColumnComposite(composite);
    Label tempLabel;

    setTextAndTip(tempLabel = new Label(tc1, SWT.WRAP), "Service kind:", S_, SWT.BEGINNING, false);
    aeOrCcCombo = wideCCombo(tc1,
            "Specify whether the Service is an Analysis Engine or a Cas Consumer", "AnalysisEngine",
            "CasConsumer");

    setTextAndTip(tempLabel = new Label(tc1, SWT.WRAP), "Protocol Service Type", S_, SWT.BEGINNING,
            false);
    serviceTypeCombo = wideCCombo(tc1, S_, "UIMA-AS JMS", Constants.PROTOCOL_SOAP,
            Constants.PROTOCOL_VINCI);

    setTextAndTip(uriLabel = new Label(tc1, SWT.NONE), "URI of service or JMS Broker:",
            "The URI for the service, e.g. localhost", SWT.BEGINNING, false);
    uriText = wideTextInput(tc1, S_, m_dialogModifyListener);

    setTextAndTip(endpointLabel = new Label(tc1, SWT.NONE), "Endpoint Name (JMS Service):",
            "For UIMA-AS JMS Services only, the endpoint name", SWT.BEGINNING, false);

    endpointText = wideTextInput(tc1, S_, m_dialogModifyListener);

    setTextAndTip(binarySerializationLabel = new Label(tc1, SWT.NONE),
            "Binary Serialization (JMS Service):",
            "For UIMA-AS JMS Services only, use binary serialzation (requires all type systems be identical)",
            SWT.BEGINNING, false);

    binarySerializationCombo = wideCComboTF(tc1, S_);

    setTextAndTip(ignoreProcessErrorsLabel = new Label(tc1, SWT.NONE),
            "Ignore Process Errors (JMS Service):",
            "For UIMA-AS JMS Services only, ignore processing errors");
    ignoreProcessErrorsLabel
            .setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

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

    createWideLabel(composite,
            "Timeouts, in milliseconds.  This is ignored for the Vinci protocol.  Specify 0 to wait forever. If not specified, a default timeout is used.");

    tc1 = new2ColumnComposite(composite);

    setTextAndTip(timeoutProcessLabel = new Label(tc1, SWT.NONE), "Timeout: Process:",
            "Timeout for processing a CAS", SWT.BEGINNING, false);
    timeoutText = wideTextInput(tc1, S_);

    setTextAndTip(timeoutJmsGetmetaLabel = new Label(tc1, SWT.NONE), "Timeout: (JMS) GetMeta:",
            "Timeout for querying the metadata from a JMS service", SWT.BEGINNING, false);
    timeoutGetmetaText = wideTextInput(tc1, S_);

    setTextAndTip(timeoutJmsCpcLabel = new Label(tc1, SWT.NONE),
            "Timeout: (JMS) Collection Processing Complete:",
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

  /**
   * Wide C combo.
   *
   * @param tc
   *          the tc
   * @param tip
   *          the tip
   * @param entries
   *          the entries
   * @return the c combo
   */
  private CCombo wideCCombo(Composite tc, String tip, String... entries) {
    CCombo cc = newCCombo(tc, tip);
    for (String e : entries) {
      cc.add(e);
    }
    ((GridData) cc.getLayoutData()).grabExcessHorizontalSpace = true;
    ;
    cc.select(0);
    return cc;
  }

  /**
   * Wide C combo TF.
   *
   * @param tc
   *          the tc
   * @param tip
   *          the tip
   * @return the c combo
   */
  private CCombo wideCComboTF(Composite tc, String tip) {
    return wideCCombo(tc, tip, "false", "true");
  }

  /**
   * Wide text input.
   *
   * @param tc
   *          the tc
   * @param tip
   *          the tip
   * @return the text
   */
  private Text wideTextInput(Composite tc, String tip) {
    return wideTextInput(tc, tip, null);
  }

  /**
   * Wide text input.
   *
   * @param tc
   *          the tc
   * @param tip
   *          the tip
   * @param listener
   *          the listener
   * @return the text
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
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

    boolean bEnableOk = (serviceTypeCombo.getText() != null
            && !serviceTypeCombo.getText().equals(""))
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

    if (isJms && (endpointText.getText() == null || endpointText.getText().trim().equals(""))) {
      bEnableOk = false;
      setErrorMessage("missing JMS endpoint");
    }

    okButton.setEnabled(bEnableOk);
    if (bEnableOk) {
      setErrorMessage("");
      portNumberWasBad = false;
    }
  }

  /**
   * Gets the selected service type name.
   *
   * @return the selected service type name
   */
  public String getSelectedServiceTypeName() {
    return m_selectedServiceTypeName;
  }

  /**
   * Gets the selected uri.
   *
   * @return the selected uri
   */
  public String getSelectedUri() {
    return m_selectedUri;
  }

  /**
   * Gets the selected key.
   *
   * @return the selected key
   */
  public String getSelectedKey() {
    return m_selectedKey;
  }

  /**
   * Gets the auto add to flow.
   *
   * @return the auto add to flow
   */
  public boolean getAutoAddToFlow() {
    return m_bAutoAddToFlow;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

}
