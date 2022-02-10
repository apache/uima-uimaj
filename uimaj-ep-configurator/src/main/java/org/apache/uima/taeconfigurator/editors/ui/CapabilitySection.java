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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.impl.TypeOrFeature_impl;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddCapabilityFeatureDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddCapabilityTypeDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddSofaDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.CommonInputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 * The Class CapabilitySection.
 */
public class CapabilitySection extends AbstractSection {

  /** The Constant CAPABILITY_SET. */
  private final static String CAPABILITY_SET = "Set";

  /** The Constant INPUT. */
  private final static String INPUT = "Input";

  /** The Constant OUTPUT. */
  private final static String OUTPUT = "Output";

  /** The Constant ALL_FEATURES. */
  public final static String ALL_FEATURES = "<all features>";

  /** The Constant TYPE_TITLE. */
  private final static String TYPE_TITLE = "Type:";

  /** The Constant FEAT_TITLE. */
  private final static String FEAT_TITLE = "F:"; // not shown, in data field

  /** The Constant NAME_TITLE. */
  private final static String NAME_TITLE = "Name                ";

  /** The Constant NAMESPACE_TITLE. */
  private final static String NAMESPACE_TITLE = "Name Space";

  /** The Constant LANGS_TITLE. */
  private final static String LANGS_TITLE = "Languages";

  /** The Constant LANG_TITLE. */
  private final static String LANG_TITLE = "L:"; // not shown, in datafield

  /** The Constant SOFAS_TITLE. */
  private final static String SOFAS_TITLE = "Sofas";

  /** The Constant SOFA_TITLE. */
  private final static String SOFA_TITLE = "S:"; // not shown, in data field

  /** The Constant CS. */
  private final static int CS = 1;

  /** The Constant TYPE. */
  private final static int TYPE = 1 << 1;

  /** The Constant FEAT. */
  private final static int FEAT = 1 << 2;

  /** The Constant LANG. */
  private final static int LANG = 1 << 3;

  /** The Constant LANG_ITEM. */
  private final static int LANG_ITEM = 1 << 4;

  /** The Constant SOFA. */
  private final static int SOFA = 1 << 5;

  /** The Constant SOFA_ITEM. */
  private final static int SOFA_ITEM = 1 << 6;

  /** The Constant TITLE_COL. */
  public final static int TITLE_COL = 0;

  /** The Constant NAME_COL. */
  public final static int NAME_COL = 1;

  /** The Constant INPUT_COL. */
  public final static int INPUT_COL = 2;

  /** The Constant OUTPUT_COL. */
  public final static int OUTPUT_COL = 3;

  /** The Constant NAMESPACE_COL. */
  public final static int NAMESPACE_COL = 4;

  /** The tt. */
  Tree tt; // for inner class access

  /** The add capability button. */
  private Button addCapabilityButton;

  /** The add lang button. */
  private Button addLangButton;

  /** The add type button. */
  private Button addTypeButton;

  /** The add sofa button. */
  private Button addSofaButton;

  /** The add edit feature button. */
  private Button addEditFeatureButton;

  /** The edit button. */
  private Button editButton;

  /** The remove button. */
  private Button removeButton;

  /** The type info. */
  private Map typeInfo;

  /** The sofa map section. */
  private SofaMapSection sofaMapSection;

  /**
   * Instantiates a new capability section.
   *
   * @param aEditor
   *          the a editor
   * @param parent
   *          the parent
   */
  public CapabilitySection(MultiPageEditor aEditor, Composite parent) {
    super(aEditor, parent, "Component Capabilities",
            "This section describes the languages handled, and the inputs needed and outputs provided in terms of the Types and Features.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.AbstractSection#initialize(org.eclipse.ui.forms.
   * IManagedForm)
   */
  @Override
  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);

    tt = newTree(sectionClient, SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
    // Table table = tt.getTable();

    // work around for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=65865
    new TreeColumn(tt, SWT.NONE).setText("                 ");

    newTreeColumn(tt, SWT.NONE).setText(NAME_TITLE); // type or feat name
    newTreeColumn(tt, SWT.NONE).setText(INPUT);
    newTreeColumn(tt, SWT.NONE).setText(OUTPUT);
    newTreeColumn(tt, SWT.NONE).setText(NAMESPACE_TITLE); // rest of typename
    tt.setHeaderVisible(true);
    tt.setLinesVisible(true);
    tt.addListener(SWT.MouseHover, this); // to show description

    final Composite buttonContainer = newButtonContainer(sectionClient);
    addCapabilityButton = newPushButton(buttonContainer, "Add Capability Set",
            "Analysis Engines can have one or more sets of capabilities; each one describes a set of outputs that are produced, given a particular set of inputs. Click here to add a capability set.");
    addLangButton = newPushButton(buttonContainer, "Add Language",
            "Click here to add a Language Capability to the selected set.");
    addTypeButton = newPushButton(buttonContainer, "Add Type",
            "Click here to add a Type to the selected capability set.");
    addSofaButton = newPushButton(buttonContainer, "Add Sofa",
            "Click here to add a Subject of Analysis (Sofa) to the selected capability set.");
    addEditFeatureButton = newPushButton(buttonContainer, "Add/Edit Features",
            "Click here to specify the features of a selected type as input or output");
    editButton = newPushButton(buttonContainer, S_EDIT,
            "Edit the selected item. You can also double-click the item to edit it.");
    removeButton = newPushButton(buttonContainer, "Remove",
            "Remove the selected item.  You can also press the Delete key to remove an item.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  @Override
  public void refresh() {
    super.refresh();
    sofaMapSection = editor.getCapabilityPage().getSofaMapSection();
    tt.getParent().setRedraw(false);
    tt.removeAll();

    Capability[] cs = getCapabilities();
    if (null != cs) {
      for (int i = 0; i < cs.length; i++) {
        TreeItem item = new TreeItem(tt, SWT.NONE);
        item.setText(TITLE_COL, CAPABILITY_SET);
        item.setData(cs[i]);
        tt.setSelection(item); // set default selection
        fillCapability(item, cs[i]);
        // if (0 == i) {
        item.setExpanded(true);
        TreeItem[] types = item.getItems();
        if (types != null)
          for (int j = 0; j < types.length; j++) {
            types[j].setExpanded(true);
          }
        // }
      }
    }
    packTree(tt);
    enable();
    tt.getParent().setRedraw(true);
  }

  /**
   * value of hash table keyed on type name.
   */
  private static class TypeCapability {

    /** The is input type. */
    boolean isInputType; // true if mentioned in <type>

    /** The is output type. */
    boolean isOutputType; // true if mentioned in <type>

    /** The features. */
    Map features = new TreeMap();
  }

  /**
   * The Class FeatureCapability.
   */
  private static class FeatureCapability {

    /** The is input feature. */
    boolean isInputFeature = false;

    /** The is output type. */
    boolean isOutputType = false;

    /** The is output update. */
    boolean isOutputUpdate = false;
  }

  /**
   * Creates the language header gui.
   *
   * @param parent
   *          the parent
   * @return the table tree item
   */
  private TreeItem createLanguageHeaderGui(TreeItem parent) {
    TreeItem langHdr = new TreeItem(parent, SWT.NONE);
    langHdr.setText(TITLE_COL, LANGS_TITLE);
    langHdr.setData(LANGS_TITLE);
    return langHdr;
  }

  /**
   * Creates the sofa header gui.
   *
   * @param parent
   *          the parent
   * @return the table tree item
   */
  private TreeItem createSofaHeaderGui(TreeItem parent) {
    TreeItem sofaHdr = new TreeItem(parent, SWT.NONE);
    sofaHdr.setText(TITLE_COL, SOFAS_TITLE);
    sofaHdr.setData(SOFAS_TITLE);
    return sofaHdr;
  }

  /**
   * Fill capability.
   *
   * @param parent
   *          the parent
   * @param c
   *          the c
   */
  private void fillCapability(TreeItem parent, Capability c) {
    // first output language capabilities
    TreeItem langHdr = createLanguageHeaderGui(parent);
    String[] languages = c.getLanguagesSupported();
    if (null != languages) {
      for (int i = 0; i < languages.length; i++) {
        TreeItem lItem = new TreeItem(langHdr, SWT.NONE);
        lItem.setData(LANG_TITLE);
        lItem.setText(NAME_COL, languages[i]);
      }
    }

    // second, output Sofas
    TreeItem sofaHdr = createSofaHeaderGui(parent);
    String[] inputSofaNames = c.getInputSofas();
    String[] outputSofaNames = c.getOutputSofas();
    Arrays.sort(inputSofaNames);
    Arrays.sort(outputSofaNames);
    for (int i = 0; i < inputSofaNames.length; i++) {
      TreeItem item = new TreeItem(sofaHdr, SWT.NONE);
      setGuiSofaName(item, inputSofaNames[i], true);
    }
    for (int i = 0; i < outputSofaNames.length; i++) {
      TreeItem item = new TreeItem(sofaHdr, SWT.NONE);
      setGuiSofaName(item, outputSofaNames[i], false);
    }

    // scan capability, collecting for each type:
    // inputs, outputs, updatesToInputs(features)
    // (updatesToInputs are output features without corresponding output type)
    // <noFeatures>, or <allFeatures> or feature set
    // For each item, generate minimal number of Type items:

    TypeCapability tc = null;
    FeatureCapability fc;
    TypeOrFeature[] inputs = c.getInputs();
    TypeOrFeature[] outputs = c.getOutputs();
    typeInfo = new TreeMap();

    if (null != inputs) {
      for (int i = 0; i < inputs.length; i++) {
        String name = inputs[i].getName();
        if (inputs[i].isType()) {
          tc = getTypeCapability(name);
          tc.isInputType = true;
          if (inputs[i].isAllAnnotatorFeatures()) {
            fc = getFeatureCapability(tc, ALL_FEATURES);
            fc.isInputFeature = true;
          }
        } else {
          tc = getTypeCapability(getTypeNameFromFullFeatureName(name)); // create a typecapability
          // if one doesn't exist
          fc = getFeatureCapability(tc, getShortFeatureName(name));
          fc.isInputFeature = true;
        }
      }
    }

    if (null != outputs) {
      for (int i = 0; i < outputs.length; i++) {
        String name = outputs[i].getName();
        if (outputs[i].isType()) {
          tc = getTypeCapability(name);
          tc.isOutputType = true;
          if (outputs[i].isAllAnnotatorFeatures()) {
            fc = getFeatureCapability(tc, ALL_FEATURES);
            fc.isOutputType = true;
          }
        } else {
          tc = getTypeCapability(getTypeNameFromFullFeatureName(name));
          fc = getFeatureCapability(tc, getShortFeatureName(name));
          fc.isOutputUpdate = true;
        }
      }
    }

    for (Iterator it = typeInfo.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String typeName = (String) entry.getKey();
      tc = (TypeCapability) entry.getValue();

      TreeItem item = new TreeItem(parent, SWT.NONE);
      setGuiTypeName(item, typeName);
      if (tc.isInputType)
        item.setText(INPUT_COL, INPUT);
      if (tc.isOutputType)
        item.setText(OUTPUT_COL, OUTPUT);

      for (Iterator fit = tc.features.entrySet().iterator(); fit.hasNext();) {
        Map.Entry fEntry = (Map.Entry) fit.next();
        String featName = (String) fEntry.getKey();
        fc = (FeatureCapability) fEntry.getValue();

        TreeItem fItem = new TreeItem(item, SWT.NONE);
        fItem.setData(FEAT_TITLE);
        fItem.setText(NAME_COL, featName);
        if (fc.isInputFeature)
          fItem.setText(INPUT_COL, INPUT);
        if (fc.isOutputUpdate || fc.isOutputType) {
          fItem.setText(OUTPUT_COL, OUTPUT);
        }
      }
    }
  }

  /**
   * Sets the gui type name.
   *
   * @param item
   *          the item
   * @param typeName
   *          the type name
   */
  private void setGuiTypeName(TreeItem item, String typeName) {
    item.setText(TITLE_COL, TYPE_TITLE);
    item.setText(NAME_COL, getShortName(typeName));
    item.setText(NAMESPACE_COL, getNameSpace(typeName));
  }

  /**
   * Sets the gui sofa name.
   *
   * @param item
   *          the item
   * @param sofaName
   *          the sofa name
   * @param isInput
   *          the is input
   */
  private void setGuiSofaName(TreeItem item, String sofaName, boolean isInput) {
    item.setData(SOFA_TITLE);
    item.setText(NAME_COL, sofaName);
    if (isInput) {
      item.setText(INPUT_COL, INPUT);
      item.setText(OUTPUT_COL, "");
    } else {
      item.setText(OUTPUT_COL, OUTPUT);
      item.setText(INPUT_COL, "");
    }
  }

  /**
   * Gets the type capability.
   *
   * @param typeName
   *          the type name
   * @return the type capability
   */
  private TypeCapability getTypeCapability(String typeName) {
    TypeCapability typeCapability = (TypeCapability) typeInfo.get(typeName);
    if (null == typeCapability) {
      typeInfo.put(typeName, typeCapability = new TypeCapability());
    }
    return typeCapability;
  }

  /**
   * Gets the feature capability.
   *
   * @param tc
   *          the tc
   * @param featureShortName
   *          the feature short name
   * @return the feature capability
   */
  private FeatureCapability getFeatureCapability(TypeCapability tc, String featureShortName) {
    FeatureCapability fc = (FeatureCapability) tc.features.get(featureShortName);
    if (null == fc) {
      tc.features.put(featureShortName, fc = new FeatureCapability());
    }
    return fc;
  }

  /**
   * Gets the type name from full feature name.
   *
   * @param name
   *          the name
   * @return the type name from full feature name
   */
  public String getTypeNameFromFullFeatureName(String name) {
    return (name.substring(0, name.indexOf(":")));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.AbstractTableSection#handleEvent(org.eclipse.swt.
   * widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.type == SWT.Expand || event.type == SWT.Collapse) {
      pack04();
      return;
    }
    if (event.widget == addCapabilityButton) {
      handleAddCapability();
      enable();
      return;
    }

    TreeItem selItem = tt.getSelection()[0];
    int itemKind = getItemKind(selItem);

    if (event.widget == addLangButton) {
      handleAddLang(selItem, itemKind);
    } else if (event.widget == addTypeButton) {
      handleAddType(selItem, itemKind);
    } else if (event.widget == addSofaButton) {
      handleAddSofa(selItem, itemKind);
    } else if (event.widget == addEditFeatureButton) {
      handleAddEditFeature(selItem, itemKind);
    } else if (event.widget == editButton || event.type == SWT.MouseDoubleClick) {
      handleEdit(selItem, itemKind);
    } else if (event.widget == removeButton
            || (event.widget == tt && event.type == SWT.KeyUp && event.character == SWT.DEL)) {
      handleRemove(selItem, itemKind);
    }

    enable();
  }

  /**
   * Handle add capability.
   */
  private void handleAddCapability() {
    Capability newCset = addCapabilitySet();

    // update the GUI
    TreeItem item = new TreeItem(tt, SWT.NONE);
    item.setText(CAPABILITY_SET);
    item.setData(newCset);
    createLanguageHeaderGui(item);
    createSofaHeaderGui(item);

    item.setExpanded(true);
    tt.setSelection(item);
    if (tt.getItemCount() == 1)
      tt.getColumn(TITLE_COL).pack();
    finishAction();
  }

  /**
   * Gets the or create all feat item.
   *
   * @param editItem
   *          the edit item
   * @param column
   *          the column
   * @param inOrOut
   *          the in or out
   * @return the or create all feat item
   */
  private void getOrCreateAllFeatItem(TreeItem editItem, int column, String inOrOut) {
    TreeItem allFeatItem = getAllFeatItem(editItem);
    if (null == allFeatItem) {
      allFeatItem = new TreeItem(editItem, SWT.NONE, 0);
      allFeatItem.setData(FEAT_TITLE);
      allFeatItem.setText(NAME_COL, ALL_FEATURES);
    }
    allFeatItem.setText(column, inOrOut);
  }

  /**
   * Gets the all feat item.
   *
   * @param editItem
   *          the edit item
   * @return the all feat item
   */
  private TreeItem getAllFeatItem(TreeItem editItem) {
    TreeItem[] subItems = editItem.getItems();
    if (null == subItems || subItems.length == 0)
      return null;
    TreeItem item = subItems[0];
    if (ALL_FEATURES.equals(item.getText(NAME_COL)))
      return item;
    return null;
  }

  /**
   * Removes the all feat item gui.
   *
   * @param editItem
   *          the edit item
   * @param column
   *          the column
   */
  private void removeAllFeatItemGui(TreeItem editItem, int column) {
    TreeItem allFeatItem = getAllFeatItem(editItem);
    if (null == allFeatItem)
      // throw new InternalErrorCDE("invalid state");
      return; // happens when no allfeat is set
    allFeatItem.setText(column, "");
    String otherCol = allFeatItem.getText((column == INPUT_COL) ? OUTPUT_COL : INPUT_COL);
    if (null == otherCol || "".equals(otherCol))
      allFeatItem.dispose();
  }

  /**
   * Handle edit.
   *
   * @param editItem
   *          the edit item
   * @param itemKind
   *          the item kind
   */
  private void handleEdit(TreeItem editItem, int itemKind) {
    Capability c = getCapability(editItem);
    switch (itemKind) {
      case SOFA_ITEM: {
        boolean existingIsInput = INPUT.equals(editItem.getText(INPUT_COL));
        String existingSofaName = editItem.getText(NAME_COL);
        AddSofaDialog dialog = new AddSofaDialog(this, c, existingSofaName, existingIsInput);
        if (dialog.open() == Window.CANCEL)
          return;

        if (dialog.isInput == existingIsInput && dialog.sofaName.equals(existingSofaName))
          return;

        // MODEL UPDATING
        // if rename,
        // update entry or remove / add entry
        // DO FOR ALL INSTANCES IN ALL CAPABILITY SETS.
        // change name in all mappings
        // if switch from input to output, delete from one array, add to other,
        // mappings: no change (maybe the user switches the other items too).
        if (Window.CANCEL == Utility.popOkCancel("Confirm Change to all Capability Sets",
                "This edit operation will change the Sofa in all Capability Sets in which it is defined.  Please confirm.",
                MessageDialog.WARNING))
          return;
        final Capability[] cSets = getCapabilities();
        for (int i = 0; i < cSets.length; i++) {
          boolean wasRemoved;
          String[] prevSofas;
          if (existingIsInput) {
            cSets[i].setInputSofas((String[]) Utility.removeElementsFromArray(
                    prevSofas = cSets[i].getInputSofas(), existingSofaName, String.class));
            wasRemoved = prevSofas != cSets[i].getInputSofas();
          } else {
            cSets[i].setOutputSofas((String[]) Utility.removeElementsFromArray(
                    prevSofas = cSets[i].getOutputSofas(), existingSofaName, String.class));
            wasRemoved = prevSofas != cSets[i].getOutputSofas();
          }
          if (wasRemoved) {
            if (dialog.isInput) {
              cSets[i].setInputSofas(stringArrayAdd(cSets[i].getInputSofas(), dialog.sofaName));
            } else {
              cSets[i].setOutputSofas(stringArrayAdd(cSets[i].getOutputSofas(), dialog.sofaName));
            }
          }
        }
        if (!dialog.sofaName.equals(existingSofaName)) {
          // rename in mappings
          SofaMapping[] mappings = getSofaMappings();
          for (int i = 0; i < mappings.length; i++) {
            if (existingSofaName.equals(mappings[i].getAggregateSofaName())) {
              mappings[i].setAggregateSofaName(dialog.sofaName);
            }
          }
        }

        // GUI updating:
        // setGuiSofaName(editItem, dialog.sofaName, dialog.isInput);
        refresh(); // because multiple capability sets may have changed
        sofaMapSection.markStale();
        finishAction();
        pack04();
        break;
      }
      case TYPE: {
        AddCapabilityTypeDialog dialog = new AddCapabilityTypeDialog(this, c, editItem);
        if (dialog.open() == Window.CANCEL)
          return;

        TypeOrFeature typeInput = getTypeOrFeature(c.getInputs(), getFullyQualifiedName(editItem));

        if (dialog.inputs[0]) {
          if (null == typeInput) {
            c.addInputType(dialog.types[0], true);
            // add all-features
            getOrCreateAllFeatItem(editItem, INPUT_COL, INPUT);
          }
        } else if (null != typeInput) { // check for any input features done in dialog
          c.setInputs(typeOrFeatureArrayRemove(c.getInputs(), typeInput));
          removeAllFeatItemGui(editItem, INPUT_COL);
        }

        TypeOrFeature typeOutput = getTypeOrFeature(c.getOutputs(),
                getFullyQualifiedName(editItem));

        if (dialog.outputs[0]) {
          if (null == typeOutput) {
            c.addOutputType(dialog.types[0], true);
            getOrCreateAllFeatItem(editItem, OUTPUT_COL, OUTPUT);
          }
        } else if (null != typeOutput) {
          c.setOutputs(typeOrFeatureArrayRemove(c.getOutputs(), typeOutput));
          removeAllFeatItemGui(editItem, OUTPUT_COL);
        }

        if (dialog.inputs[0] || dialog.outputs[0]) {
          editItem.setText(INPUT_COL, dialog.inputs[0] ? INPUT : "");
          editItem.setText(OUTPUT_COL, dialog.outputs[0] ? OUTPUT : "");
        } else {
          editItem.dispose();
          pack04();
        }
        finishAction();
        break;
      }
      case LANG_ITEM: {
        CommonInputDialog dialog = new CommonInputDialog(this, "Edit Language",
                "Enter a two letter ISO-639 language code, followed optionally by a two-letter ISO-3166 country code (Examples: fr or fr-CA)",
                CommonInputDialog.LANGUAGE, editItem.getText(NAME_COL));
        if (dialogForLanguage(c, dialog) == Window.CANCEL)
          return;
        c.getLanguagesSupported()[getIndex(editItem)] = dialog.getValue();
        // update GUI
        editItem.setText(NAME_COL, dialog.getValue());
        finishAction();
        break;
      }

      case FEAT: {
        TreeItem typeItem = editItem.getParentItem();
        String typeName = getFullyQualifiedName(typeItem);

        // using the CAS to get all the inherited features
        Type type = editor.getCurrentView().getTypeSystem().getType(typeName);

        AddCapabilityFeatureDialog dialog = new AddCapabilityFeatureDialog(this, type, c);
        if (dialog.open() == Window.CANCEL)
          return;

        addOrEditFeature(dialog, typeName, typeItem, c);
        break;
      }
      default:
        break; // happens when mouse double click on non-editable item - ignore
    }
  }

  /**
   * Any capability set declares sofa.
   *
   * @param name
   *          the name
   * @param isInput
   *          the is input
   * @return true, if successful
   */
  private boolean anyCapabilitySetDeclaresSofa(String name, boolean isInput) {
    final Capability[] cSets = getAnalysisEngineMetaData().getCapabilities();
    for (int i = 0; i < cSets.length; i++) {
      final String[] sofaNames = isInput ? cSets[i].getInputSofas() : cSets[i].getOutputSofas();
      for (int j = 0; j < sofaNames.length; j++) {
        if (name.equals(sofaNames[j]))
          return true;
      }
    }
    return false;
  }

  /**
   * Handle remove.
   *
   * @param removeItem
   *          the remove item
   * @param itemKind
   *          the item kind
   */
  private void handleRemove(TreeItem removeItem, int itemKind) {
    int selectionIndex = tt.indexOf(tt.getSelection()[0]);
    Capability c = getCapability(removeItem);
    switch (itemKind) {
      case CS: {
        if (Window.CANCEL == Utility.popOkCancel("Confirm Remove",
                "This action will remove an entire capability set.  Please confirm.",
                MessageDialog.WARNING)) {
          maybeSetSelection(tt, selectionIndex + 1);
          return;
        }
        removeCapabilitySet(c);
        removeItem.dispose();
        break;
      }
      case LANG_ITEM: {
        c.setLanguagesSupported(
                stringArrayRemove(c.getLanguagesSupported(), removeItem.getText(NAME_COL)));
        removeItem.dispose();
        break;
      }
      case SOFA_ITEM: {
        if (Window.CANCEL == Utility.popOkCancel("Confirm Removal of Sofa",
                "This action will remove this Sofa as a capability, and delete its mappings if no other capability set declares this Sofa."
                        + "  Please confirm.",
                MessageDialog.WARNING)) {
          maybeSetSelection(tt, selectionIndex + 1);
          return;
        }
        String sofaName = removeItem.getText(NAME_COL);
        boolean isInput = INPUT.equals(removeItem.getText(INPUT_COL));
        if (isInput)
          c.setInputSofas((String[]) Utility.removeElementFromArray(c.getInputSofas(), sofaName,
                  String.class));
        else
          c.setOutputSofas((String[]) Utility.removeElementFromArray(c.getOutputSofas(), sofaName,
                  String.class));
        removeItem.dispose();

        if (!anyCapabilitySetDeclaresSofa(sofaName, isInput)) {
          Comparator comparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
              String name = (String) o1;
              SofaMapping sofaMapping = (SofaMapping) o2;
              if (name.equals(sofaMapping.getAggregateSofaName()))
                return 0;
              return 1;
            }
          };
          editor.getAeDescription().setSofaMappings((SofaMapping[]) Utility.removeElementsFromArray(
                  getSofaMappings(), sofaName, SofaMapping.class, comparator));

          sofaMapSection.markStale();
        }
        break;
      }
      case TYPE: {
        if (Window.CANCEL == Utility.popOkCancel("Confirm Removal of Type",
                "This action will remove this type as a capability.  Please confirm.",
                MessageDialog.WARNING)) {
          maybeSetSelection(tt, selectionIndex + 1);
          return;
        }
        TreeItem[] features = removeItem.getItems();
        if (null != features)
          for (int i = 0; i < features.length; i++) {
            removeFeature(c, features[i]);
          }
        String typeNameToRemove = getFullyQualifiedName(removeItem);
        if (isInput(removeItem))
          c.setInputs(typeOrFeatureArrayRemove(c.getInputs(), typeNameToRemove));
        if (isOutput(removeItem) /* || isUpdate(removeItem) */)
          c.setOutputs(typeOrFeatureArrayRemove(c.getOutputs(), typeNameToRemove));

        removeItem.dispose();
        break;
      }
      case FEAT: {
        removeFeature(c, removeItem);
        break;
      }
      default:
        throw new InternalErrorCDE("invalid state");
    }

    maybeSetSelection(tt, selectionIndex - 1);
    finishAction();
  }

  /**
   * Removes the capability set.
   *
   * @param c
   *          the c
   */
  private void removeCapabilitySet(Capability c) {
    Capability[] cs = getAnalysisEngineMetaData().getCapabilities();
    Capability[] newCs = new Capability[cs.length - 1];
    for (int i = 0, j = 0; i < newCs.length; i++) {
      if (cs[i] != c)
        newCs[j++] = cs[i];
    }
    getAnalysisEngineMetaData().setCapabilities(newCs);
  }

  /**
   * Checks if is input.
   *
   * @param item
   *          the item
   * @return true, if is input
   */
  private boolean isInput(TreeItem item) {
    return INPUT.equals(item.getText(INPUT_COL)); // works if getText() returns null
  }

  /**
   * Checks if is output.
   *
   * @param item
   *          the item
   * @return true, if is output
   */
  private boolean isOutput(TreeItem item) {
    return OUTPUT.equals(item.getText(OUTPUT_COL));
  }

  /**
   * Checks if is input.
   *
   * @param fullFeatureName
   *          the full feature name
   * @param c
   *          the c
   * @return true, if is input
   */
  public static boolean isInput(String fullFeatureName, Capability c) {
    return null != getTypeOrFeature(c.getInputs(), fullFeatureName);
  }

  /**
   * Checks if is output.
   *
   * @param fullFeatureName
   *          the full feature name
   * @param c
   *          the c
   * @return true, if is output
   */
  public static boolean isOutput(String fullFeatureName, Capability c) {
    return null != getTypeOrFeature(c.getOutputs(), fullFeatureName);
  }

  /**
   * Removes the feature.
   *
   * @param c
   *          the c
   * @param removeItem
   *          the remove item
   */
  private void removeFeature(Capability c, TreeItem removeItem) {
    String shortFeatureName = removeItem.getText(NAME_COL);
    if (shortFeatureName.equals(ALL_FEATURES)) {
      if (isInput(removeItem)) {
        TypeOrFeature tfItem = getTypeOrFeature(c.getInputs(),
                getFullyQualifiedName(removeItem.getParentItem()));
        tfItem.setAllAnnotatorFeatures(false);
      }
      if (isOutput(removeItem) /* || isUpdate(removeItem) */) {
        TypeOrFeature tfItem = getTypeOrFeature(c.getOutputs(),
                getFullyQualifiedName(removeItem.getParentItem()));
        tfItem.setAllAnnotatorFeatures(false);
      }
    } else {
      String featureNameToRemove = getFullyQualifiedName(removeItem.getParentItem()) + ":"
              + removeItem.getText(NAME_COL);
      if (isInput(removeItem))
        c.setInputs(typeOrFeatureArrayRemove(c.getInputs(), featureNameToRemove));
      if (isOutput(removeItem) /* || isUpdate(removeItem) */)
        c.setOutputs(typeOrFeatureArrayRemove(c.getOutputs(), featureNameToRemove));
    }
    removeItem.dispose();
  }

  /**
   * Gets the capability from table tree item.
   *
   * @param item
   *          the item
   * @return the capability from table tree item
   */
  public Capability getCapabilityFromTreeItem(TreeItem item) {
    return (Capability) item.getData();
  }

  /**
   * Handle add lang.
   *
   * @param selItem
   *          the sel item
   * @param itemKind
   *          the item kind
   */
  private void handleAddLang(TreeItem selItem, int itemKind) {
    if (itemKind == CS)
      selItem = selItem.getItems()[0]; // lang is 1st item in capability set
    else if (itemKind == LANG_ITEM)
      selItem = selItem.getParentItem();
    else if (itemKind == TYPE || itemKind == SOFA)
      selItem = selItem.getParentItem().getItems()[0];
    else if (itemKind == FEAT || itemKind == SOFA_ITEM)
      selItem = selItem.getParentItem().getParentItem().getItems()[0];
    Capability c = getCapabilityFromTreeItem(selItem.getParentItem());
    CommonInputDialog dialog = new CommonInputDialog(this, "Add Language",
            "Enter a two letter ISO-639 language code, followed optionally by a two-letter ISO-3166 country code (Examples: fr or fr-CA)",
            CommonInputDialog.LANGUAGE);
    if (dialogForLanguage(c, dialog) == Window.CANCEL)
      return;

    c.setLanguagesSupported(stringArrayAdd(c.getLanguagesSupported(), dialog.getValue()));

    // update GUI
    TreeItem lItem = new TreeItem(selItem, SWT.NONE);
    lItem.setData(LANG_TITLE);
    lItem.setText(NAME_COL, dialog.getValue());
    selItem.setExpanded(true);
    pack04();
    finishAction();
  }

  /**
   * Handle add type.
   *
   * @param selItem
   *          the sel item
   * @param itemKind
   *          the item kind
   */
  private void handleAddType(TreeItem selItem, int itemKind) {
    if (itemKind == LANG || itemKind == TYPE || itemKind == SOFA)
      selItem = selItem.getParentItem();
    else if (itemKind == LANG_ITEM || itemKind == FEAT || itemKind == SOFA_ITEM)
      selItem = selItem.getParentItem().getParentItem();
    Capability c = getCapabilityFromTreeItem(selItem);
    AddCapabilityTypeDialog dialog = new AddCapabilityTypeDialog(this, c);
    if (dialog.open() == Window.CANCEL)
      return;

    for (int i = 0; i < dialog.types.length; i++) {

      if (dialog.inputs[i])
        c.addInputType(dialog.types[i], dialog.inputs[i]);

      if (dialog.outputs[i])
        c.addOutputType(dialog.types[i], dialog.outputs[i]);

      TreeItem item = new TreeItem(selItem, SWT.NONE);
      setGuiTypeName(item, dialog.types[i]);
      item.setText(INPUT_COL, dialog.inputs[i] ? INPUT : "");
      item.setText(OUTPUT_COL, dialog.outputs[i] ? OUTPUT : "");

      TreeItem fItem = new TreeItem(item, SWT.NONE);
      fItem.setData(FEAT_TITLE);
      fItem.setText(NAME_COL, ALL_FEATURES);
      fItem.setText(INPUT_COL, dialog.inputs[i] ? INPUT : "");
      fItem.setText(OUTPUT_COL, dialog.outputs[i] ? OUTPUT : "");

      item.setExpanded(true);
    }
    pack04();
    selItem.setExpanded(true);
    finishAction();
  }

  /**
   * Handle add sofa.
   *
   * @param selItem
   *          the sel item
   * @param itemKind
   *          the item kind
   */
  private void handleAddSofa(TreeItem selItem, int itemKind) {
    if (itemKind == CS)
      selItem = selItem.getItems()[1];
    else if (itemKind == LANG || itemKind == TYPE)
      selItem = selItem.getParentItem().getItems()[1];
    else if (itemKind == LANG_ITEM || itemKind == FEAT || itemKind == SOFA_ITEM)
      selItem = selItem.getParentItem().getParentItem().getItems()[1];

    Capability c = getCapabilityFromTreeItem(selItem.getParentItem());
    AddSofaDialog dialog = new AddSofaDialog(this, c);
    if (dialog.open() == Window.CANCEL)
      return;

    // dialog.isInput, dialog.sofaName
    if (dialog.isInput)
      c.setInputSofas(stringArrayAdd(c.getInputSofas(), dialog.sofaName));
    else
      c.setOutputSofas(stringArrayAdd(c.getOutputSofas(), dialog.sofaName));

    TreeItem item = new TreeItem(selItem, SWT.NONE);
    setGuiSofaName(item, dialog.sofaName, dialog.isInput);
    selItem.setExpanded(true);
    pack04();

    sofaMapSection.markStale();
    finishAction();
  }

  /**
   * Handle add edit feature.
   *
   * @param selItem
   *          the sel item
   * @param itemKind
   *          the item kind
   */
  private void handleAddEditFeature(TreeItem selItem, int itemKind) {
    if (itemKind == FEAT)
      selItem = selItem.getParentItem();

    Capability c = getCapabilityFromTreeItem(selItem.getParentItem());
    String typeName = getFullyQualifiedName(selItem);

    // using the CAS to get all the inherited features
    Type type = editor.getCurrentView().getTypeSystem().getType(typeName);

    AddCapabilityFeatureDialog dialog = new AddCapabilityFeatureDialog(this, type, c);
    if (dialog.open() == Window.CANCEL)
      return;

    addOrEditFeature(dialog, typeName, selItem, c);
  }

  /**
   * Adds the or edit feature.
   *
   * @param dialog
   *          the dialog
   * @param typeName
   *          the type name
   * @param parentItem
   *          the parent item
   * @param c
   *          the c
   */
  private void addOrEditFeature(AddCapabilityFeatureDialog dialog, String typeName, // fully
          // qualified
          TreeItem parentItem, Capability c) {
    // set the <all features> flag on the type in the model, for input and output
    c.setInputs(setAllFeatures(c.getInputs(), typeName, dialog.allFeaturesInput));
    // The logic for output features is complicated. Output features are always listed in the
    // outputs section of the capability.
    // Their type must be in either the output section or the input section.
    // When a feature is added here as an output, and the input section has a type,
    // we don't require that there be a type capability with output marked.
    // If the user wants to have an output type,
    // they can add one explicitly.
    // There must be either an input or an output Type in order for a feature to be added.
    //
    // For the "all features" case, we can't set an output state for all features on an input type.
    if (dialog.allFeaturesOutput && (null == getTypeOrFeature(c.getOutputs(), typeName))) {
      Utility.popMessage("Unable to set AllFeatures",
              "Skipping setting of <All Features> for output, because you must have the type specified itself"
                      + " as an output in order to set the <All Features>.  You can individually set all the features, instead.",
              MessageDialog.WARNING);
      dialog.allFeaturesOutput = false;
    } else
      c.setOutputs(setAllFeatures(c.getOutputs(), typeName, dialog.allFeaturesOutput));

    TreeItem[] prevFeatGUI = parentItem.getItems();
    for (int i = 0; i < prevFeatGUI.length; i++) {
      prevFeatGUI[i].dispose();
    }

    // update GUI for <all features> - add element if needed
    if (dialog.allFeaturesInput || dialog.allFeaturesOutput) {
      TreeItem item = new TreeItem(parentItem, SWT.NONE);
      item.setData(FEAT_TITLE);
      item.setText(NAME_COL, ALL_FEATURES);
      item.setText(INPUT_COL, dialog.allFeaturesInput ? INPUT : "");
      item.setText(OUTPUT_COL, dialog.allFeaturesOutput ? OUTPUT : "");
    }

    List inputsL = new ArrayList();
    List outputsL = new ArrayList();

    for (int i = 0; i < dialog.features.length; i++) {
      String fullName = typeName + ":" + dialog.features[i];
      if (dialog.inputs[i])
        inputsL.add(newFeature(fullName));
      if (dialog.outputs[i])
        outputsL.add(newFeature(fullName));
      // update the GUI
      TreeItem item = new TreeItem(parentItem, SWT.NONE);
      item.setData(FEAT_TITLE);
      item.setText(NAME_COL, dialog.features[i]);
      item.setText(INPUT_COL, dialog.inputs[i] ? INPUT : "");
      item.setText(OUTPUT_COL, dialog.outputs[i] ? OUTPUT : "");
    }
    parentItem.setExpanded(true);
    tt.getColumn(NAME_COL).pack();
    tt.setSelection(parentItem);

    c.setInputs(replaceFeaturesKeepingTypes(c.getInputs(), typeName, inputsL));
    c.setOutputs(replaceFeaturesKeepingTypes(c.getOutputs(), typeName, outputsL));

    finishAction();
  }

  /**
   * New feature.
   *
   * @param name
   *          the name
   * @return the type or feature
   */
  private TypeOrFeature newFeature(String name) {
    TypeOrFeature result = new TypeOrFeature_impl();
    result.setType(false);
    result.setName(name);
    return result;
  }

  /**
   * Gets the fully qualified name.
   *
   * @param item
   *          the item
   * @return the fully qualified name
   */
  public String getFullyQualifiedName(TreeItem item) {
    String namespace = item.getText(NAMESPACE_COL);
    String name = item.getText(NAME_COL);
    return "".equals(namespace) ? name : namespace + "." + name;
  }

  /**
   * Gets the fully qualified name.
   *
   * @param namespace
   *          the namespace
   * @param name
   *          the name
   * @return the fully qualified name
   */
  // used by dialog table -has different columns
  public String getFullyQualifiedName(String namespace, String name) {
    return (null == namespace || "".equals(namespace)) ? name : namespace + "." + name;
  }

  /**
   * Given a current list of inputs/ outputs, made up of "Types" and "features", make a new list
   * keeping all the types, and keeping all the features that belong to other types, and adding the
   * features that are passed in for one particular type in the "features" parameter.
   *
   * @param items
   *          the items
   * @param typeName
   *          the type name
   * @param features
   *          - associated with the type
   * @return the type or feature[]
   */
  private TypeOrFeature[] replaceFeaturesKeepingTypes(TypeOrFeature[] items, String typeName,
          List features) {
    List newItems = new ArrayList();
    typeName = typeName + ':';
    if (null != items)
      for (int i = 0; i < items.length; i++) {
        if (items[i].isType() || !items[i].getName().startsWith(typeName))
          newItems.add(items[i]);
      }

    for (Iterator it = features.iterator(); it.hasNext();) {
      newItems.add(it.next());
    }
    return (TypeOrFeature[]) newItems.toArray(new TypeOrFeature[newItems.size()]);
  }

  /**
   * Sets the all features.
   *
   * @param items
   *          Existing array of TypeOrFeature items (input or output)
   * @param typeName
   *          the type name
   * @param isAllFeatures
   *          AllFeatures value
   * @return the type or feature[]
   */
  private TypeOrFeature[] setAllFeatures(TypeOrFeature[] items, String typeName,
          boolean isAllFeatures) {

    TypeOrFeature type = getTypeOrFeature(items, typeName);

    if (null != type) {
      type.setAllAnnotatorFeatures(isAllFeatures);
      return items;
    }

    // If get here, case = Type declared as Output(input) or not at all while all Features
    // declared as Input (output)
    // Need to add the Type and set the all annotator features value
    if (isAllFeatures)
      throw new InternalErrorCDE("invalid state");
    return items;
  }

  /**
   * Finish action.
   */
  private void finishAction() {
    setFileDirty();
  }

  /**
   * Pack 04.
   */
  private void pack04() {
    tt.getColumn(TITLE_COL).pack();
    tt.getColumn(NAME_COL).pack();
    tt.getColumn(NAMESPACE_COL).pack();
  }

  /**
   * Gets the item kind.
   *
   * @param item
   *          the item
   * @return the item kind
   */
  private int getItemKind(TreeItem item) {
    String itemID = item.getText(TITLE_COL);

    if (CAPABILITY_SET.equals(itemID))
      return CS;
    if (TYPE_TITLE.equals(itemID))
      return TYPE;
    itemID = (String) item.getData();
    if (LANGS_TITLE.equals(itemID))
      return LANG;
    if (FEAT_TITLE.equals(itemID))
      return FEAT;
    if (LANG_TITLE.equals(itemID))
      return LANG_ITEM;
    if (SOFAS_TITLE.equals(itemID))
      return SOFA;
    if (SOFA_TITLE.equals(itemID))
      return SOFA_ITEM;
    throw new InternalErrorCDE("invalid state");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractSection#enable()
   */
  @Override
  public void enable() {
    addCapabilityButton.setEnabled(true);

    boolean selectOK = tt.getSelectionCount() == 1;
    TreeItem item = selectOK ? tt.getSelection()[0] : null;
    int kind = selectOK ? getItemKind(item) : 0;

    addLangButton.setEnabled(selectOK);
    addTypeButton.setEnabled(selectOK);
    addSofaButton.setEnabled(selectOK);
    addEditFeatureButton.setEnabled((kind & (FEAT + TYPE)) > 0);
    editButton.setEnabled((kind & (SOFA_ITEM + LANG_ITEM + FEAT + TYPE)) > 0);
    removeButton.setEnabled((kind & (CS + SOFA_ITEM + LANG_ITEM + FEAT + TYPE)) > 0);
  }

  /**
   * Dialog for language.
   *
   * @param c
   *          the c
   * @param dialog
   *          the dialog
   * @return the int
   */
  private int dialogForLanguage(Capability c, CommonInputDialog dialog) {
    for (;;) {
      if (dialog.open() == Window.CANCEL)
        return Window.CANCEL;

      String[] languages = c.getLanguagesSupported();
      boolean alreadySpecified = false;
      for (int i = 0; i < languages.length; i++) {
        if (languages[i].equals(dialog.getValue())) {
          Utility.popMessage("Language spec already defined",
                  "The language specification you entered is already specified.\nPlease enter a different specification, or Cancel this operation."
                          + "\n\nLanguage: " + dialog.getValue(),
                  MessageDialog.ERROR);
          alreadySpecified = true;
          break;
        }
      }
      if (!alreadySpecified)
        break;
    }
    return Window.OK;
  }

  /**
   * Gets the capability.
   *
   * @param item
   *          the item
   * @return the capability
   */
  private Capability getCapability(TreeItem item) {
    while (null != item.getParentItem())
      item = item.getParentItem();
    return getCapabilityFromTreeItem(item);
  }

}
