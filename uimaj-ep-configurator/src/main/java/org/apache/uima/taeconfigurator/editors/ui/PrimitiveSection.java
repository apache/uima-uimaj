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

import java.text.MessageFormat;

import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.IManagedForm;

/**
 * This class is misnamed - refers really to Runtime Information
 * 
 */
public class PrimitiveSection extends AbstractSection {

  public void enable() {
  }

  private Label implNameLabel;

  private Text implName;

  private Button findButton;

  private Button multipleDeploymentAllowed;

  private Button modifiesCas;

  private Button outputsNewCASes;

  /**
   * Creates a section with a text field for the name of the annotator. Only enabled if annotator is
   * primitive Also has the operational parameters
   */
  public PrimitiveSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Runtime Information",
                    "This section describes information about how to run this component");
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);

    modifiesCas = newCheckBox(sectionClient, "updates the CAS",
                    "check this if this component updates the CAS");
    spacer(sectionClient);
    if (isAeDescriptor() || isCasConsumerDescriptor() || isFlowControllerDescriptor()) {
      multipleDeploymentAllowed = newCheckBox(sectionClient, "multiple deployment allowed",
                      "check this to allow multiple instances of this engine to be deployed that can run in parallel");
      spacer(sectionClient);
    }
    if (isAeDescriptor()) {
      outputsNewCASes = newCheckBox(sectionClient, "Outputs new CASes",
                      "check this for primitive components that output new CASes, "
                                      + "or for aggregates which contain a CAS Multiplier, "
                                      + "where the new CASes are returned out of the aggregate.");
      spacer(sectionClient);
    }

    implNameLabel = newLabelWithData(sectionClient, "");
    implName = newTextWithTip(sectionClient, "", "");

    spacer(sectionClient); // skip first column

    findButton = newPushButton(sectionClient, "Browse", "", true, SWT.RIGHT);
    // next line makes the browse button just big enough for the text,
    // otherwise it's very long...
    findButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

    toolkit.paintBordersFor(sectionClient);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();

    // set description for enter field (either .class or .dll file)
    String implKind = editor.getAeDescription().getFrameworkImplementation();
    if ("TAF".equals(implKind) || "org.apache.uima.cpp".equals(implKind)) {
      implNameLabel.setText("Name of the .dll file");
      implName.setToolTipText("Enter the name of the .dll file here.");
      findButton.setToolTipText("Browse the file system for the .dll file.");
    } else {
      implNameLabel.setText("Name of the Java class file");
      implName.setToolTipText("Enter the name of the Java class that implements this component.");
      findButton.setToolTipText("Browse for the Java class that implmenets this component.");
    }

    if (isPrimitive()) {
      setEnabled(true);

      // AnnotatorImplementationName may be null
      // due to change from aggregate to primitive
      String modelImplName = editor.getAeDescription().getAnnotatorImplementationName();
      implName.setText(convertNull(modelImplName));
      this.getSection().layout();
    } else {
      implName.setText("");
      setEnabled(false);
    }
    OperationalProperties ops = getOperationalProperties();
    if (null != ops) {
      setButtonSelection(modifiesCas, ops.getModifiesCas());
      setButtonSelection(multipleDeploymentAllowed, ops.isMultipleDeploymentAllowed());
      setButtonSelection(outputsNewCASes, ops.getOutputsNewCASes());
    } else {
      setButtonSelection(modifiesCas, true);
      setButtonSelection(multipleDeploymentAllowed, false);
      setButtonSelection(outputsNewCASes, false);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    valueChanged = false;
    OperationalProperties ops = getOperationalProperties();
    if (event.widget == findButton) {
      String className = null;
      try {
        String implKind = editor.getAeDescription().getFrameworkImplementation();
        if ("TAF".equals(implKind) || "org.apache.uima.cpp".equals(implKind)) {
          FileDialog dialog = new FileDialog(getSection().getShell(), SWT.NONE);
          String[] extensions = { "*.dll" };
          dialog.setFilterExtensions(extensions);
          String sStartDir = Platform.getLocation().toString();
          dialog.setFilterPath(sStartDir);
          className = dialog.open();

        } else {
          SelectionDialog typeDialog = JavaUI.createTypeDialog(getSection().getShell(), editor
                          .getEditorSite().getWorkbenchWindow(), editor
                          .getSearchScopeForDescriptorType(),
                          IJavaElementSearchConstants.CONSIDER_CLASSES, false, "*");
          typeDialog.setTitle(MessageFormat.format("Choose the {0} implementation class",
                          new Object[] { editor.descriptorTypeString() }));
          typeDialog.setMessage("Filter/mask:");
          if (typeDialog.open() == Window.CANCEL)
            return;
          Object[] result = typeDialog.getResult();
          if (result != null && result.length > 0)
            className = ((IType) result[0]).getFullyQualifiedName();
        }
        if (className == null || className.equals("")) //$NON-NLS-1$
          return;
        implName.setText(className);
        editor.getAeDescription().setAnnotatorImplementationName(className);
        valueChanged = true;
      } catch (JavaModelException e) {
        throw new InternalErrorCDE("unexpected Exception", e);
      }
    } else if (event.widget == modifiesCas) {
      ops.setModifiesCas(setValueChangedBoolean(modifiesCas.getSelection(), ops.getModifiesCas()));
    } else if (event.widget == multipleDeploymentAllowed) {
      ops.setMultipleDeploymentAllowed(setValueChangedBoolean(multipleDeploymentAllowed
                      .getSelection(), ops.isMultipleDeploymentAllowed()));
    } else if (event.widget == outputsNewCASes) {
      ops.setOutputsNewCASes(setValueChangedBoolean(outputsNewCASes.getSelection(), ops
                      .getOutputsNewCASes()));
    } else if (event.widget == implName) {
      editor.getAeDescription().setAnnotatorImplementationName(
                      setValueChanged(implName.getText(), editor.getAeDescription()
                                      .getAnnotatorImplementationName()));
    }
    if (valueChanged)
      editor.setFileDirty();
  }

  /**
   * @param enabled
   *          indicator for the section to be enabled.
   */
  public void setEnabled(boolean enabled) {
    implNameLabel.setEnabled(enabled);
    implName.setEnabled(enabled);
    findButton.setEnabled(enabled);
    enableCtrl(modifiesCas, true);
    enableCtrl(multipleDeploymentAllowed, true);
    enableCtrl(outputsNewCASes, true);
  }
}
