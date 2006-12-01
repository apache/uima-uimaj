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

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.IManagedForm;

public class GeneralSection extends AbstractSection {

  public void enable() {
  }

  private Button cppButton;

  private Button javaButton;

  private Button primitiveButton;

  private Button aggregateButton;

  /**
   * Creates a section to edit general information like primitive or aggregate and C++ or Java
   * 
   * @param editor
   *          the referenced multipage editor
   */
  public GeneralSection(MultiPageEditor aEditor, Composite parent) {
    super(aEditor, parent, "Implementation Details", null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
   */
  public void initialize(IManagedForm form) {
    super.initialize(form);
    ((GridData) this.getSection().getLayoutData()).grabExcessVerticalSpace = false;
    Composite sectionClient = new2ColumnComposite(this.getSection());
    ((GridData) sectionClient.getLayoutData()).grabExcessVerticalSpace = false;
    // FrameworkImplementation choose, 2 radio buttons
    if (isAeDescriptor() || isCasConsumerDescriptor()) {
      toolkit.createLabel(sectionClient, "Implementation Language").setToolTipText(
              "Choose the implementation language here.");

      Composite buttons = new2ColumnComposite(sectionClient);
      cppButton = newRadioButton(buttons, "C/C++", "C/C++", NOT_SELECTED);
      javaButton = newRadioButton(buttons, "Java", "Java", SELECTED);

      // DescriptorType choose, 2 radio buttons
      toolkit.createLabel(sectionClient, "Engine Type").setToolTipText(
              "Choose the type of the engine here.");

      buttons = new2ColumnComposite(sectionClient);

      primitiveButton = newRadioButton(buttons, "Primitive", S_, SELECTED);
      aggregateButton = newRadioButton(buttons, "Aggregate", S_, NOT_SELECTED);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    super.refresh();
    boolean isPrimitive = isPrimitive();
    // select primitive or aggregate
    if (isAeDescriptor() || isCasConsumerDescriptor()) {
      primitiveButton.setSelection(isPrimitive);
      aggregateButton.setSelection(!isPrimitive);

      // select C++ or Java
      String implType = editor.getAeDescription().getFrameworkImplementation();
      cppButton.setSelection(Constants.CPP_FRAMEWORK_NAME.equals(implType));
      javaButton.setSelection(Constants.JAVA_FRAMEWORK_NAME.equals(implType));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.widget == primitiveButton || event.widget == aggregateButton) {
      boolean isPrimitive = primitiveButton.getSelection();
      // Note: events occur when button is selected or deselected
      if (event.widget == primitiveButton && !isPrimitive)
        return; // deselecting
      if (event.widget == aggregateButton && isPrimitive)
        return; // deselecting
      if (isPrimitive && isPrimitive())
        return; // nothing changed
      if (!isPrimitive && isAggregate())
        return; // nothing changed
      if (isPrimitive) {
        if (Window.CANCEL == Utility.popOkCancel("Switching from Aggregate",
                "This action will clear the capabilities, reset the delegates, "
                        + "reset the flow, reset the parameters, reset any resource information "
                        + "and start with an empty type system.  Are you sure?",
                MessageDialog.WARNING)) {
          aggregateButton.setSelection(true);
          primitiveButton.setSelection(false);
          return;
        }
        editor.getAeDescription().setAnnotatorImplementationName("");
      } else {
        // if (isLocalProcessingDescriptor() && !isAeDescriptor()) {
        // Utility.popMessage("Not Allowed",
        // "Cas Consumers, Cas Initializers, Collection Readers, and Flow Controllers cannot be
        // Aggregates.",
        // MessageDialog.ERROR);
        // primitiveButton.setSelection(true);
        // aggregateButton.setSelection(false);
        // return;
        // }
        if (Window.CANCEL == Utility.popOkCancel("Switching from Primitive AE",
                "This action will clear the capabilities, reset the delegates, "
                        + "reset the parameters, reset any resource information "
                        + "and reset the type system.  Are you sure?", MessageDialog.WARNING)) {
          primitiveButton.setSelection(true);
          aggregateButton.setSelection(false);
          return;
        }
        editor.getAeDescription().setAnnotatorImplementationName(null);
      }
      editor.getAeDescription().setPrimitive(isPrimitive);
      commonResets();
      try {
        editor.setAeDescription(editor.getAeDescription());
      } catch (ResourceInitializationException e) {
        throw new InternalErrorCDE("invalid state", e);
      }
      javaButton.setEnabled(isPrimitive);
      cppButton.setEnabled(isPrimitive);
      HeaderPage page = editor.getAggregatePage();
      if (null != page)
        page.markStale();
      page = editor.getParameterPage();
      if (null != page)
        page.markStale();
      page = editor.getSettingsPage();
      if (null != page)
        page.markStale();
      page = editor.getTypePage();
      if (null != page)
        markRestOfPageStale(page.getManagedForm(), null);
      page = editor.getCapabilityPage();
      if (null != page)
        page.markStale();
      page = editor.getIndexesPage();
      if (null != page)
        page.markStale();
      page = editor.getResourcesPage();
      if (null != page)
        page.markStale();
    }
    if (event.widget == javaButton || event.widget == cppButton) {
      valueChanged = false;
      if (cppButton.getSelection()) {
        editor.getAeDescription().setFrameworkImplementation(
                setValueChanged(Constants.CPP_FRAMEWORK_NAME, editor.getAeDescription()
                        .getFrameworkImplementation()));
      } else {
        editor.getAeDescription().setFrameworkImplementation(
                setValueChanged(Constants.JAVA_FRAMEWORK_NAME, editor.getAeDescription()
                        .getFrameworkImplementation()));
      }
      if (!valueChanged)
        return;
    }
    PrimitiveSection s = editor.getOverviewPage().getPrimitiveSection();
    if (null != s) {
      s.refresh();
      // next line makes the bounding rectangle show up
      s.getSection().getClient().redraw();
    }
    setFileDirty();
  }

  private void commonResets() {
    // clear the delegates
    getDelegateAnalysisEngineSpecifiersWithImports().clear();
    editor.getResolvedDelegates().clear();
    if (isAggregate()) {
      // reset the flow to fixed flow with null as the set
      FlowConstraints flowConstraints = UIMAFramework.getResourceSpecifierFactory()
              .createFixedFlow();
      flowConstraints.setAttributeValue("fixedFlow", stringArray0);
      getAnalysisEngineMetaData().setFlowConstraints(flowConstraints);
    } else
      getAnalysisEngineMetaData().setFlowConstraints(null);
    // clear capabilities
    getAnalysisEngineMetaData().setCapabilities(capabilityArray0);
    addCapabilitySet();
    // reset parameters
    // reset the parameters not declared in a group
    getConfigurationParameterDeclarations().setConfigurationParameters(
            new ConfigurationParameter[0]);
    // reset groups
    getConfigurationParameterDeclarations().setConfigurationGroups(
            AbstractSection.configurationGroupArray0);
    // reset common parameters
    getConfigurationParameterDeclarations().setCommonParameters(new ConfigurationParameter[0]);
    // reset default group name
    getConfigurationParameterDeclarations().setDefaultGroupName("");
    // reset search strategy
    getConfigurationParameterDeclarations().setSearchStrategy("");

    // reset the parm settings
    ConfigurationParameterSettings configParmSettings = UIMAFramework.getResourceSpecifierFactory()
            .createConfigurationParameterSettings();
    getAnalysisEngineMetaData().setConfigurationParameterSettings(configParmSettings);
    // reset typesystem, needed when going from primitive to aggregate
    getAnalysisEngineMetaData().setTypeSystem(null);
    // reset resources
    editor.getAeDescription().setExternalResourceDependencies(new ExternalResourceDependency[0]);
    ResourceManagerConfiguration rmc = editor.getAeDescription().getResourceManagerConfiguration();
    if (null != rmc) {
      rmc.setExternalResourceBindings(new ExternalResourceBinding[0]);
      rmc.setExternalResources(new ExternalResourceDescription[0]);
    }
    // reset indexes
    getAnalysisEngineMetaData().setFsIndexCollection(null);
    getAnalysisEngineMetaData().setFsIndexes(new FsIndexDescription[0]);
    // reset index type priorities
    getAnalysisEngineMetaData().setTypePriorities(null);
  }
}
