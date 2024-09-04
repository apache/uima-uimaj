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

/*
 * The types shown are those that are explicitly defined; in other words, built-in types are not
 * shown, unless there is an explicit extending of some built-in type. In this case, the "merged"
 * type system is updated to include the features from the built-in type.
 * 
 * Redesign 2/17/05 Two panels: This one and an "Import" panel This panel: A TreeTable with buttons
 * Items (top level) are Types Nested 1 down: Features col 0 = Type or Feature name col 1 =
 * SuperType or Range Name or allowed value
 * 
 * Hover shows Description
 * 
 * Types that are imported are shown in grey font color Hover shows type system they were imported
 * from, plus description, right click to open it
 * 
 * Types for aggregates are "read-only". No right-click on hover - is ambiguous where type came
 * from.
 * 
 * Double-click or press Edit button to edit
 * 
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddAllowedValueDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddFeatureDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddTypeDialog;
import org.apache.uima.taeconfigurator.wizards.TypeSystemNewWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 * The Class TypeSection.
 */
public class TypeSection extends AbstractImportablePartSection {

  /** The Constant CASCADE_MESSAGE. */
  public static final String CASCADE_MESSAGE = "This will cause a cascading deletion of an associated "
          + "input, output, index, or type priority, unless this deletion exposes a "
          + "built-in or imported type or feature of the same name.  Ok to continue?";

  /** The Constant CASCADE_DELETE_WARNING. */
  public static final String CASCADE_DELETE_WARNING = "Cascade Delete Warning";

  /** The Constant NAME_COL. */
  public static final int NAME_COL = 0;

  /** The Constant SUPER_COL. */
  public static final int SUPER_COL = 1;

  /** The Constant RANGE_COL. */
  public static final int RANGE_COL = 1;

  /** The Constant MULTIPLE_REF_OK_COL. */
  public static final int MULTIPLE_REF_OK_COL = 2;

  /** The Constant ELEMENT_TYPE_COL. */
  public static final int ELEMENT_TYPE_COL = 3;

  /** The Constant AV_COL. */
  public static final int AV_COL = 1;

  /** The Constant HEADER_ALLOWED_VALUE. */
  public static final String HEADER_ALLOWED_VALUE = "Allowed Value:";

  /** The tt. */
  private Tree tt;

  /** The add type button. */
  private Button addTypeButton;

  /** The add button. */
  private Button addButton;

  /** The edit button. */
  private Button editButton;

  /** The remove button. */
  private Button removeButton;

  /** The jcas gen button. */
  private Button jcasGenButton;

  /** The export button. */
  private Button exportButton;

  /** The limit J cas gen to project scope. */
  private Button limitJCasGenToProjectScope;

  // private TypeSystemDescription tsdLocal; // for this descriptor, no imports

  /** The Constant ALLOWED. */
  private static final boolean ALLOWED = true;

  /**
   * Instantiates a new type section.
   *
   * @param editor
   *          the editor
   * @param parent
   *          the parent
   */
  public TypeSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Types (or Classes)",
            "The following types (classes) are defined in this analysis engine descriptor.\nThe grayed out items are imported or merged from other descriptors, and cannot be edited here. (To edit them, edit their source files).");
  }

  // **********************************************************************
  // * Called by the page constructor after all sections are created, to
  // initialize them.
  // * (non-Javadoc)
  // * @see
  // org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
  // **********************************************************************/
  @Override
  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);

    tt = newTree(sectionClient, SWT.SINGLE | SWT.FULL_SELECTION);
    new TreeColumn(tt, SWT.NONE).setText("Type Name or Feature Name");
    new TreeColumn(tt, SWT.NONE).setText("SuperType or Range");
    new TreeColumn(tt, SWT.NONE).setText(" "); // space for icon
    new TreeColumn(tt, SWT.NONE).setText("Element Type");
    tt.setHeaderVisible(true);

    tt.addListener(SWT.MouseHover, this); // to show description and more

    Composite buttonContainer = newButtonContainer(sectionClient);
    addTypeButton = newPushButton(buttonContainer, "Add Type", "Click here to add a new type.");
    addButton = newPushButton(buttonContainer, S_ADD,
            "Click here to add a feature or allowed-value to the selected type.");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    exportButton = newPushButton(buttonContainer, S_EXPORT, S_EXPORT_TIP);

    spacer(buttonContainer);
    jcasGenButton = newPushButton(buttonContainer, "JCasGen",
            "Click here to run JCasGen on this type system.");
    limitJCasGenToProjectScope = newCheckBox(buttonContainer, "limited",
            "JCasGen only those types defined within this project"
                    + "\n  You can change the default in UIMA prefs or in the UIMA menu");
    boolean defaultValue = editor.getLimitJCasGenToProjectScope();
    limitJCasGenToProjectScope.setSelection(defaultValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractTableSection#update()
   */
  @Override
  public void refresh() {
    super.refresh();

    tt.removeAll();

    TypeSystemDescription tsdFull = getMergedTypeSystemDescription();

    TypeDescription[] tdsFull = tsdFull.getTypes();
    if (null != tdsFull) {
      for (int i = 0; i < tdsFull.length; i++) {
        addTypeToGUI(tdsFull[i]);
      }
    }

    if (tt.getItemCount() > 0)
      tt.setSelection(tt.getItems()[0]);
    packTree(tt);
    enable();
  }

  /**
   * Sets the difference.
   *
   * @param all
   *          the all
   * @param subset
   *          the subset
   * @return the feature description[]
   */
  private FeatureDescription[] setDifference(FeatureDescription[] all,
          FeatureDescription[] subset) {
    if (null == all)
      return featureDescriptionArray0;
    if (null == subset)
      return all;
    List<FeatureDescription> result = new ArrayList<>();

    outer: for (int i = 0; i < all.length; i++) {
      String name = all[i].getName();
      for (int j = 0; j < subset.length; j++) {
        if (subset[j].getName().equals(name))
          continue outer;
      }
      result.add(all[i]);
    }
    return (FeatureDescription[]) result.toArray(new FeatureDescription[result.size()]);
  }

  /**
   * Adds the type to GUI.
   *
   * @param td
   *          the td
   */
  private void addTypeToGUI(TypeDescription td) {
    TreeItem item = new TreeItem(tt, SWT.NONE);
    item.setText(NAME_COL, formatName(td.getName()));
    item.setText(SUPER_COL, formatName(td.getSupertypeName()));
    item.setData(td);
    setItemColor(item, isLocalType(td));

    FeatureDescription[] features = td.getFeatures();
    addFeaturesToGui(td, item, features);

    TypeDescription builtInTd = getBuiltInTypeDescription(td);
    if (null != builtInTd) {
      FeatureDescription[] additionalBuiltInFeatures = setDifference(builtInTd.getFeatures(),
              td.getFeatures());
      addFeaturesToGui(td, item, additionalBuiltInFeatures);
    }

    AllowedValue[] avs = td.getAllowedValues();
    if (null != avs) {
      for (int i = 0; i < avs.length; i++) {
        TreeItem avItem = new TreeItem(item, SWT.NONE);
        avItem.setText(NAME_COL, HEADER_ALLOWED_VALUE);
        avItem.setText(AV_COL, convertNull(avs[i].getString()));
        avItem.setData(avs[i]);
        setItemColor(avItem, null != getLocalAllowedValue(td, avs[i]));
      }
    }
    // No built-ins have "allowed values" so we don't have to add any
    item.setExpanded(true);
  }

  /**
   * Adds the features to gui.
   *
   * @param td
   *          the td
   * @param item
   *          the item
   * @param features
   *          the features
   */
  private void addFeaturesToGui(TypeDescription td, TreeItem item, FeatureDescription[] features) {
    if (null != features) {
      for (int i = 0; i < features.length; i++) {
        TreeItem fItem = new TreeItem(item, SWT.NONE);
        updateGuiFeature(fItem, features[i], td);
      }
    }
  }

  /**
   * Update gui feature.
   *
   * @param fItem
   *          the f item
   * @param fd
   *          the fd
   * @param td
   *          the td
   */
  private void updateGuiFeature(TreeItem fItem, FeatureDescription fd, TypeDescription td) {
    String rangeType;
    fItem.setText(NAME_COL, fd.getName());
    fItem.setText(RANGE_COL, formatName(rangeType = fd.getRangeTypeName()));
    fItem.setData(fd);
    setItemColor(fItem, null != getLocalFeatureDefinition(td, fd));
    if (isArrayOrListType(rangeType)) {
      Boolean mra = fd.getMultipleReferencesAllowed();
      fItem.setImage(MULTIPLE_REF_OK_COL,
              (null != mra && mra)
                      ? TAEConfiguratorPlugin.getImage(TAEConfiguratorPlugin.IMAGE_MREFOK)
                      : TAEConfiguratorPlugin.getImage(TAEConfiguratorPlugin.IMAGE_NOMREF));
    } else {
      fItem.setImage(MULTIPLE_REF_OK_COL, null);
    }

    String ert = fd.getElementType();
    fItem.setText(ELEMENT_TYPE_COL,
            (isFSArrayOrListType(rangeType) && ert != null) ? formatName(ert) : "");

  }

  /**
   * Sets the item color.
   *
   * @param item
   *          the item
   * @param isLocal
   *          the is local
   */
  private void setItemColor(TreeItem item, boolean isLocal) {
    if (isLocal)
      return;
    item.setForeground(editor.getFadeColor());
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
    if (event.widget == addTypeButton) {
      handleAddType();
    } else if (event.widget == addButton) {
      TreeItem parent = tt.getSelection()[0];
      if (null != parent.getParentItem())
        parent = parent.getParentItem();

      if (isSubtypeOfString(parent))
        handleAddAllowedValue(parent);
      else
        handleAddFeature(parent);

    } else if (event.widget == editButton) {
      handleEdit();
    } else if (event.type == SWT.MouseDoubleClick && (!isAggregate()) && // can't edit aggregates
            isLocalItem(tt.getSelection()[0])) {
      handleEdit();
    } else if (event.widget == removeButton) {
      handleRemove();
    } else if (event.widget == exportButton) {
      editor.getTypePage().getTypeImportSection().exportImportablePart("<typeSystemDescription>",
              TypeSystemNewWizard.TYPESYSTEM_TEMPLATE);
      refresh();
    } else if (event.widget == jcasGenButton) {
      editor.doJCasGenChkSrc(null);
    } else if (event.widget == limitJCasGenToProjectScope) {
      editor.setLimitJCasGenToProjectScope(limitJCasGenToProjectScope.getSelection());
    } else if (event.type == SWT.MouseHover) {
      handleHover(event);
    }
    enable();
  }

  /**
   * Handle hover.
   *
   * @param event
   *          the event
   */
  public void handleHover(Event event) {
    // next getItem call requires that table have SWT.FULL_SELECTION Style
    TreeItem item = tt.getItem(new Point(event.x, event.y));
    if (null != item) {
      Object o = item.getData();
      if (null == o)
        throw new InternalErrorCDE("invalid state");

      if (o instanceof TypeDescription) {
        setToolTipText(tt, ((TypeDescription) o).getDescription());
      } else if (o instanceof FeatureDescription) {
        FeatureDescription fd = (FeatureDescription) o;
        if (item.getBounds(MULTIPLE_REF_OK_COL).contains(event.x, event.y)
                && isArrayOrListType(fd.getRangeTypeName())) {
          Boolean mra = fd.getMultipleReferencesAllowed();
          setToolTipText(tt, (mra != null && mra) ? "Multiple References Allowed"
                  : "Multiple References Not Allowed");
        } else
          setToolTipText(tt, fd.getDescription());
      } else if (o instanceof AllowedValue) {
        setToolTipText(tt, ((AllowedValue) o).getDescription());
      }
    } else
      tt.setToolTipText("");
  }

  /**
   * Gets the type description from table tree item.
   *
   * @param item
   *          the item
   * @return the type description from table tree item
   */
  public TypeDescription getTypeDescriptionFromTableTreeItem(TreeItem item) {
    return (TypeDescription) item.getData();
  }

  /**
   * Handle add allowed value.
   *
   * @param parent
   *          the parent
   */
  // disabled unless type having String as supertype is selected
  public void handleAddAllowedValue(TreeItem parent) {
    boolean refreshNeeded = false;

    TypeDescription td = getTypeDescriptionFromTableTreeItem(parent);
    // guaranteed non-null - otherwise can't add an allowed value
    TypeDescription localTd = getLocalTypeDefinition(td);

    AddAllowedValueDialog dialog = new AddAllowedValueDialog(this, null);
    if (dialog.open() == Window.CANCEL) {
      return;
    }

    AllowedValue av = UIMAFramework.getResourceSpecifierFactory().createAllowedValue();
    allowedValueUpdate(av, dialog);
    addAllowedValue(localTd, av);

    if (!Utility.arrayContains(td.getAllowedValues(), av))
      addAllowedValue(td, (AllowedValue) av.clone());
    else
      refreshNeeded = true;

    // update the GUI
    if (refreshNeeded)
      refresh();
    else {
      TreeItem item = new TreeItem(parent, SWT.NONE);
      item.setText(NAME_COL, HEADER_ALLOWED_VALUE);
      item.setText(AV_COL, convertNull(av.getString()));
      item.setData(av);

      parent.setExpanded(true);
    }

    editor.addDirtyTypeName(td.getName());
    finishActionPack();
  }

  /**
   * Adds the allowed value.
   *
   * @param td
   *          the td
   * @param av
   *          the av
   */
  private void addAllowedValue(TypeDescription td, AllowedValue av) {
    td.setAllowedValues((AllowedValue[]) Utility.addElementToArray(td.getAllowedValues(), av,
            AllowedValue.class));
  }

  /**
   * Removes the allowed value.
   *
   * @param td
   *          - local or merged (2 callers)
   * @param av
   *          the av
   */
  private void removeAllowedValue(TypeDescription td, AllowedValue av) {
    td.setAllowedValues((AllowedValue[]) Utility.removeEqualElementFromArray(td.getAllowedValues(),
            av, AllowedValue.class));
  }

  /**
   * Handle add feature.
   *
   * @param parent
   *          the parent
   */
  // disabled unless type is selected
  public void handleAddFeature(TreeItem parent) {

    TypeDescription td = getTypeDescriptionFromTableTreeItem(parent);
    // guaranteed non-null - otherwise add button disabled
    TypeDescription localTd = getLocalTypeDefinition(td);
    //
    AddFeatureDialog dialog = new AddFeatureDialog(this, td, null);
    if (dialog.open() == Window.CANCEL) {
      return;
    }

    FeatureDescription fd = localTd.addFeature(null, null, null);
    featureUpdate(fd, dialog);

    editor.addDirtyTypeName(td.getName());

    // update the GUI

    // if this type is merged with an import or with a built-in,
    // need to "refresh"
    if (isImportedType(td) || isBuiltInType(td)) {
      if (!isImportedFeature(dialog.featureName, td)) {
        // don't need to check builtin Feature because gui doesn't let you
        // define a built-in feature again
        fd = td.addFeature(null, null, null);
        featureUpdate(fd, dialog);
      }
      refresh();

      selectTypeInGui(td);

      finishAction();
    } else {
      fd = td.addFeature(null, null, null);
      featureUpdate(fd, dialog);
      TreeItem item = new TreeItem(parent, SWT.NONE);
      updateGuiFeature(item, fd, td);
      parent.setExpanded(true);
      finishActionPack();
    }
  }

  /**
   * Select type in gui.
   *
   * @param td
   *          the td
   */
  private void selectTypeInGui(TypeDescription td) {
    TreeItem[] items = tt.getItems();
    for (int i = 0; i < items.length; i++) {
      if (td.getName().equals(((TypeDescription) items[i].getData()).getName())) {
        tt.setSelection(items[i]);
        return;
      }
    }
  }

  /**
   * Allowed value update.
   *
   * @param av
   *          the av
   * @param dialog
   *          the dialog
   */
  public void allowedValueUpdate(AllowedValue av, AddAllowedValueDialog dialog) {
    valueChanged = false;
    av.setString(setValueChanged(dialog.allowedValue, av.getString()));
    av.setDescription(setValueChanged(dialog.description, av.getDescription()));
  }

  /**
   * Feature update.
   *
   * @param fd
   *          the fd
   * @param dialog
   *          the dialog
   */
  public void featureUpdate(FeatureDescription fd, AddFeatureDialog dialog) {
    valueChanged = false;
    String v = setValueChanged(dialog.featureName, fd.getName());
    fd.setName(v);
    v = setValueChanged(multiLineFix(dialog.description), fd.getDescription());
    fd.setDescription(v);
    String range = setValueChanged(dialog.featureRangeName, fd.getRangeTypeName());
    fd.setRangeTypeName(range);
    if (isArrayOrListType(range)) {
      Boolean b = setValueChangedCapitalBoolean(dialog.multiRef, fd.getMultipleReferencesAllowed());
      fd.setMultipleReferencesAllowed(b);
      if (isFSArrayOrListType(range)) {
        v = setValueChanged(dialog.elementRangeName, fd.getElementType());
        fd.setElementType(v);
      } else {
        fd.setElementType(null);
      }
    } else {
      fd.setMultipleReferencesAllowed(null);
      fd.setElementType(null);
    }
  }

  /**
   * Handle add type.
   */
  public void handleAddType() {
    AddTypeDialog dialog = new AddTypeDialog(this);
    if (dialog.open() == Window.CANCEL)
      return;

    TypeSystemDescription tsd = getMergedTypeSystemDescription();
    TypeSystemDescription localTsd = getTypeSystemDescription();

    TypeDescription td = localTsd.addType(dialog.typeName, multiLineFix(dialog.description),
            dialog.supertypeName);

    if (!isImportedType(dialog.typeName)) {
      td = tsd.addType(dialog.typeName, multiLineFix(dialog.description), dialog.supertypeName);
      addTypeToGUI(td);
    } else {
      rebuildMergedTypeSystem();
    }

    if (isImportedType(dialog.typeName) || isBuiltInType(dialog.typeName)) {
      refresh();
      selectTypeInGui(td);
    }

    editor.addDirtyTypeName(dialog.typeName);
    finishActionPack();
  }

  /**
   * Finish action pack.
   */
  private void finishActionPack() {
    packTree(tt);
    finishAction();
  }

  /**
   * Finish action.
   */
  private void finishAction() {

    if (isLocalProcessingDescriptor()) {
      editor.getIndexesPage().markStale();
      editor.getCapabilityPage().markStale();
    }
    setFileDirty();
  }

  /**
   * Handle edit.
   */
  private void handleEdit() {
    TreeItem item = tt.getSelection()[0];
    TreeItem parentType = item.getParentItem();

    if (null == parentType) { // editing a type, not a feature
      editType(item);
    } else if (item.getText(NAME_COL).equals(HEADER_ALLOWED_VALUE)) {
      editAllowedValue(item, parentType);
    } else {
      editFeature(item, parentType);
    }
  }

  /**
   * Gets the feature description from table tree item.
   *
   * @param item
   *          the item
   * @return the feature description from table tree item
   */
  public FeatureDescription getFeatureDescriptionFromTableTreeItem(TreeItem item) {
    return (FeatureDescription) item.getData();
  }

  /**
   * Edits the feature.
   *
   * @param item
   *          the item
   * @param parent
   *          the parent
   */
  private void editFeature(TreeItem item, TreeItem parent) {
    boolean remergeNeeded = false;
    boolean refreshNeeded = false;
    TypeDescription td = getTypeDescriptionFromTableTreeItem(parent);
    FeatureDescription fd = getFeatureDescriptionFromTableTreeItem(item);
    FeatureDescription localFd = getLocalFeatureDefinition(td, fd);
    String oldFeatureName = fd.getName();
    AddFeatureDialog dialog = new AddFeatureDialog(this, td, fd);
    if (dialog.open() == Window.CANCEL)
      return;

    featureUpdate(localFd, dialog);
    if (!valueChanged)
      return;

    if (!dialog.featureName.equals(oldFeatureName)) {
      if (isImportedFeature(oldFeatureName, td)) {
        Utility.popMessage("Imported Feature not changed", "Changing the feature name from '"
                + oldFeatureName + "' to '" + dialog.featureName
                + "' will not affect the corresponding imported type with the previous feature name.  "
                + "Both features will be included in the type.", MessageDialog.INFORMATION);
        remergeNeeded = true;
        refreshNeeded = true;
      }
      if (isBuiltInFeature(oldFeatureName, td)) {
        Utility.popMessage("BuiltIn Feature not changed", "Changing the feature name from '"
                + oldFeatureName + "' to '" + dialog.featureName
                + "' will not affect the corresponding built-in type with the previous feature name.  "
                + "Both features will be included in the type.", MessageDialog.INFORMATION);
        refreshNeeded = true; // no remerge needed - builtins not merged
      }
    }
    if (remergeNeeded)
      rebuildMergedTypeSystem();
    else
      featureUpdate(fd, dialog);

    if (refreshNeeded)
      refresh();
    else {
      // update the GUI
      updateGuiFeature(item, fd, td);
    }
    alterFeatureMentions(oldFeatureName, fd.getName(), td.getName());
    editor.addDirtyTypeName(td.getName());
    finishActionPack();
  }

  /**
   * Gets the allowed value from table tree item.
   *
   * @param item
   *          the item
   * @return the allowed value from table tree item
   */
  public AllowedValue getAllowedValueFromTableTreeItem(TreeItem item) {
    return (AllowedValue) item.getData();
  }

  /**
   * Edits the allowed value.
   *
   * @param item
   *          the item
   * @param parent
   *          the parent
   */
  private void editAllowedValue(TreeItem item, TreeItem parent) {

    TypeDescription td = getTypeDescriptionFromTableTreeItem(parent);
    AllowedValue av = getAllowedValueFromTableTreeItem(item);
    AllowedValue localAv = getLocalAllowedValue(td, av); // must use unmodified value of "av"
    AddAllowedValueDialog dialog = new AddAllowedValueDialog(this, av);
    if (dialog.open() == Window.CANCEL)
      return;

    allowedValueUpdate(av, dialog);
    allowedValueUpdate(localAv, dialog);
    if (!valueChanged)
      return;

    // update the GUI
    item.setText(AV_COL, av.getString());

    editor.addDirtyTypeName(td.getName());
    finishActionPack();
  }

  /**
   * New Feature test: return null if OK, error message otherwise.
   *
   * @param td
   *          the td
   * @param dialog
   *          the dialog
   * @return error message or null
   */
  private String newFeatureTests(TypeDescription td, AddFeatureDialog dialog) {
    FeatureDescription fd;

    if (isLocalFeature(dialog.featureName, td))
      return "Duplicate Feature Name in this Descriptor";
    if (isBuiltInFeature(dialog.featureName, td))
      return "Feature Name duplicates built-in feature for this type";

    if (null != (fd = getFeature(td, dialog.featureName)))
      // verify the range is the same
      if (!fd.getRangeTypeName().equals(dialog.featureRangeName))
        return "Range Name not the same as the range from an imported type/feature description";
    return null;
  }

  /**
   * verify a new or edited feature is valid. For new features: The name must be unique locally. It
   * may duplicate a non-local feature that isn't also built-in. (We presume built-in features are
   * fixed). (We allow dupl non-local feature in case this type system is used without the import,
   * in some other context?) We don't use the TCas because it isn't necessarily being updated
   * 
   * For edited features: If the name changed, do "new" test above on new name If the name changed,
   * do "remove" test on old name: If used in an index or capability, but merged/built-in name not
   * still there, warn about index being invalidated (not done here - done during feature update
   * itself). If name used in index, and range is not indexable - error
   *
   * @param dialog
   *          the dialog
   * @param td
   *          the td
   * @param oldFd
   *          the old fd
   * @return error message or null
   */
  public String checkFeature(AddFeatureDialog dialog, TypeDescription td,
          FeatureDescription oldFd) {
    if (null == oldFd) { // adding new feature
      return newFeatureTests(td, dialog);
    }

    String errMsg = null;
    // modifying existing feature
    if (!oldFd.getName().equals(dialog.featureName)) { // name changed
      errMsg = newFeatureTests(td, dialog);
      if (null != errMsg)
        return errMsg;
      return null;
    }

    // Note: this test is different from above: it tests current name, not old name
    if (isFeatureUsedInIndex(td, dialog.featureName))
      if (!isIndexableRange(dialog.featureRangeName))
        return ("This feature is used in an index - it must have an indexable Range");
    return null;
  }

  /**
   * Check allowed value.
   *
   * @param dialog
   *          the dialog
   * @param td
   *          the td
   * @param av
   *          the av
   * @return the string
   */
  public String checkAllowedValue(AddAllowedValueDialog dialog, TypeDescription td,
          AllowedValue av) {
    if (isLocalAllowedValue(dialog.allowedValue, td)) {
      return "Duplicate Allowed Value in this Descriptor";
    }
    return null;
  }

  /**
   * Edits the type.
   *
   * @param item
   *          the item
   */
  // type is always local; could be merging with import(s) or built-in
  private void editType(TreeItem item) {

    boolean mergeAndRefreshNeeded = false;
    boolean refreshNeeded = false;
    TypeDescription td = getTypeDescriptionFromTableTreeItem(item);
    AddTypeDialog dialog = new AddTypeDialog(this, td);
    if (dialog.open() == Window.CANCEL)
      return;

    // dialog disallows supertype specs inconsistent with existing features or allowed values
    // dialog has already checked for dup type name
    String newTypeName = dialog.typeName;
    String oldTypeName = td.getName();
    String[] typesRequiringThisOne = stringArray0;
    if (!oldTypeName.equals(newTypeName)) {
      typesRequiringThisOne = showTypesRequiringThisOneMessage(oldTypeName, ALLOWED);
      if (null == typesRequiringThisOne) // null is cancel signal
        return;

      if (isImportedType(oldTypeName) && Window.CANCEL == Utility.popOkCancel(
              "Type define via Import",
              "The type '" + oldTypeName
                      + "' is also defined in 1 or more imports.  Changing the type name here will not change it in the imported type file, causing both types to be in the type system, together. Please confirm this is what you intend.",
              MessageDialog.WARNING))
        return;
      if (isBuiltInType(oldTypeName) && Window.CANCEL == Utility.popOkCancel(
              "Type was extending a built-in",
              "The type '" + oldTypeName
                      + "' was extending a builtin type of the same name. Changing the type name here will not change the built-in type, causing both types to be in the type system, together. Please confirm this is what you intend.",
              MessageDialog.WARNING))
        return;
      if (isImportedType(oldTypeName) || isImportedType(newTypeName) || isBuiltInType(oldTypeName)
              || isBuiltInType(newTypeName))
        mergeAndRefreshNeeded = true;
    }
    valueChanged = false;

    // guaranteed non-null because otherwise edit not allowed
    TypeDescription localTd = getLocalTypeDefinition(td);
    typeUpdate(localTd, dialog);
    if (!valueChanged)
      return;
    if (mergeAndRefreshNeeded) {
      rebuildMergedTypeSystem();
      td = getMergedTypeSystemDescription().getType(newTypeName);
    } else {
      typeUpdate(td, dialog);
      updateGuiType(item, td);
    }

    editor.removeDirtyTypeName(oldTypeName);
    editor.addDirtyTypeName(td.getName());
    // interesting case: renaming a type causes another type
    // which has a shadow to get a new super (should cause merge error)
    // or which has a shadow to get a new range (should cause merge error)
    // or which has a shadow of a built-in to get new range or super
    // (should cause error) (but only if that feature is defined in the
    // non-local version)

    refreshNeeded |= alterTypeMentionsInOtherTypes(oldTypeName, td.getName());
    if (refreshNeeded || mergeAndRefreshNeeded)
      refresh();
    alterTypeMentions(oldTypeName, td.getName());

    finishActionPack();
  }

  /**
   * Update gui type.
   *
   * @param item
   *          the item
   * @param td
   *          the td
   */
  private void updateGuiType(TreeItem item, TypeDescription td) {
    item.setText(NAME_COL, formatName(td.getName()));
    item.setText(SUPER_COL, formatName(td.getSupertypeName()));
  }

  /**
   * Rebuild merged type system.
   */
  private void rebuildMergedTypeSystem() {
    try {
      editor.setMergedTypeSystemDescription();
    } catch (ResourceInitializationException e) {
      throw new InternalErrorCDE(e);
    }
  }

  /**
   * Type update.
   *
   * @param td
   *          the td
   * @param dialog
   *          the dialog
   */
  private void typeUpdate(TypeDescription td, AddTypeDialog dialog) {
    td.setName(setValueChanged(dialog.typeName, td.getName()));
    td.setDescription(setValueChanged(multiLineFix(dialog.description), td.getDescription()));
    td.setSupertypeName(setValueChanged(dialog.supertypeName, td.getSupertypeName()));

  }

  /**
   * returns null or error message about a duplicate type name Cases: Dupl type in local descriptor
   * not allowed. Type that duplicates imported type is OK. Type that duplicates built-in type is
   * OK.
   *
   * @param newTypeName
   *          the new type name
   * @return the string
   */
  public String checkDuplTypeName(String newTypeName) {
    if (isLocalType(newTypeName)) {
      return "The type '" + newTypeName + "' is already defined locally in this descriptor.";
    }
    return null;
  }

  /**
   * Handle remove.
   */
  /*
   * Note that the Remove action is disabled if the item selected is imported only
   */
  private void handleRemove() {
    TreeItem item = tt.getSelection()[0];
    TreeItem parent = item.getParentItem();

    if (null == parent)
      handleRemoveType(item);
    else if (item.getText(NAME_COL).equals(HEADER_ALLOWED_VALUE))
      handleRemoveAllowedValue(item);
    else
      handleRemoveFeature(item);
  }

  /**
   * Handle remove allowed value.
   *
   * @param item
   *          the item
   */
  private void handleRemoveAllowedValue(TreeItem item) {
    TypeDescription td = getTypeDescriptionFromTableTreeItem(item.getParentItem());
    AllowedValue av = getAllowedValueFromTableTreeItem(item);
    // guaranteed non-null -otherwise remove button disabled
    removeAllowedValue(getLocalTypeDefinition(td), av);
    if (!isImportedAllowedValue(td, av)) {
      removeAllowedValue(td, av);

      // update GUI
      setSelectionOneUp(tt, item);
      item.dispose();
    } else {
      refresh();
    }

    editor.addDirtyTypeName(td.getName());
    finishAction();
  }

  // cases: removing a feature which is merged with an identical named imported feature
  /**
   * Handle remove feature.
   *
   * @param item
   *          the item
   */
  // same - for built-in <could be created outside of the CDE>
  private void handleRemoveFeature(final TreeItem item) {
    TypeDescription td = getTypeDescriptionFromTableTreeItem(item.getParentItem());
    FeatureDescription fd = getFeatureDescriptionFromTableTreeItem(item);

    String featureName = fd.getName();

    boolean bFeatureInUseElsewhere = isFeatureInUseElsewhere(td, featureName);
    if (bFeatureInUseElsewhere) {
      String sCascadeDeleteTitle = CASCADE_DELETE_WARNING;
      String sCascadeDeleteMessage = CASCADE_MESSAGE;
      boolean bContinue = MessageDialog.openConfirm(getSection().getShell(), sCascadeDeleteTitle,
              sCascadeDeleteMessage);
      if (!bContinue)
        return;
    }
    TypeDescription localTd = getLocalTypeDefinition(td);
    FeatureDescription localFd = getLocalFeatureDefinition(td, fd);

    removeFeature(localTd, localFd);
    if (isImportedFeature(featureName, td))
      refresh(); // don't remove from merged set
    else {
      removeFeature(td, fd);
      if (isBuiltInFeature(featureName, td))
        refresh();
      else {
        // update GUI
        setSelectionOneUp(tt, item);
        // tt.getTable().setSelection(tt.getTable().getSelectionIndex() - 1);
        item.dispose();
      }
    }

    if (bFeatureInUseElsewhere && !isImportedFeature(featureName, td)
            && !isBuiltInFeature(featureName, td)) {
      deleteTypeOrFeatureMentions(featureName, FEATURES, localTd.getName());
    }

    editor.addDirtyTypeName(td.getName());
    finishAction();
  }

  /**
   * The Class TypeFeature.
   */
  private static class TypeFeature {

    /** The type name. */
    String typeName;

    /** The feature name. */
    String featureName; // short feature name

    /**
     * Instantiates a new type feature.
     *
     * @param type
     *          the type
     * @param feat
     *          the feat
     */
    TypeFeature(String type, String feat) {
      typeName = type;
      featureName = feat;
    }
  }

  /** The Constant typeFeature0. */
  private static final TypeFeature[] typeFeature0 = new TypeFeature[0];

  /**
   * CALL THIS FN after removing and remerging. Assuming the localTd is removed, it may result in
   * the mergedTd having fewer features. Compute the features that would be removed. Do it not just
   * for this type, but for all subtypes of this type. NOTE: if mergeTd is null, it means the type
   * is removed completely (no shadowing of built-in or imported types), so no special feature
   * removal is needed.
   *
   * @param localTd
   *          the local td
   * @param mergedTd
   *          The "remerged" value of the type.
   * @return array of string names of features to be removed
   */

  private TypeFeature[] computeFeaturesToRemove(TypeDescription localTd, TypeDescription mergedTd) {
    if (null == mergedTd)
      return typeFeature0;
    FeatureDescription[] locallyDefinedFeatures = localTd.getFeatures();
    if (null == locallyDefinedFeatures || locallyDefinedFeatures.length == 0)
      return typeFeature0;

    FeatureDescription[] remainingFeatures = mergedTd.getFeatures();
    ArrayList deletedFeatures = new ArrayList();

    outer: for (int i = 0; i < locallyDefinedFeatures.length; i++) {
      String fname = locallyDefinedFeatures[i].getName();
      if (null != remainingFeatures)
        for (int j = 0; j < remainingFeatures.length; j++) {
          if (fname.equals(remainingFeatures[j].getName()))
            continue outer;
        }
      deletedFeatures.add(fname);
    }

    // have list of features really disappearing (not kept present by imports or built-ins)
    // return all types/features of these for types which are subtypes of the passed-in type

    CAS tcas = editor.getCurrentView();
    TypeSystem typeSystem = tcas.getTypeSystem();
    Type thisType = typeSystem.getType(localTd.getName());
    List subsumedTypesList = typeSystem.getProperlySubsumedTypes(thisType);
    subsumedTypesList.add(thisType);
    Type[] subsumedTypes = (Type[]) subsumedTypesList.toArray(new Type[0]);

    String[] featNameArray = (String[]) deletedFeatures.toArray(new String[deletedFeatures.size()]);
    ArrayList result = new ArrayList();
    for (int i = 0; i < subsumedTypes.length; i++) {
      Type t = subsumedTypes[i];
      for (int j = 0; j < featNameArray.length; j++) {
        if (null != t.getFeatureByBaseName(featNameArray[j]))
          result.add(new TypeFeature(t.getName(), featNameArray[j]));
      }
    }
    return (TypeFeature[]) result.toArray(typeFeature0);
  }

  /**
   * Handle remove type.
   *
   * @param item
   *          the item
   */
  private void handleRemoveType(final TreeItem item) {

    TypeDescription td = getTypeDescriptionFromTableTreeItem(item);

    String sTypeNameToRemove = td.getName();

    // pop a dialog mentioning typesRequiringThisOne, saying that others must be
    // deleted first....
    if (null == showTypesRequiringThisOneMessage(sTypeNameToRemove, !ALLOWED))
      return;

    boolean bTypeInUseElsewhere = isTypeInUseElsewhere(sTypeNameToRemove);
    if (bTypeInUseElsewhere) {
      String sCascadeDeleteTitle = CASCADE_DELETE_WARNING;
      String sCascadeDeleteMessage = CASCADE_MESSAGE;
      boolean bContinue = MessageDialog.openConfirm(getSection().getShell(), sCascadeDeleteTitle,
              sCascadeDeleteMessage);
      if (!bContinue) {
        return;
      }
    }

    TypeDescription localTd = getLocalTypeDefinition(td);
    removeType(localTd, getTypeSystemDescription());

    if (isImportedType(td)) {
      // although the type itself still remains in the merged type system,
      // features may be removed by this action, so
      // a remerge is needed
      rebuildMergedTypeSystem();
      refresh();
    } else {
      removeType(td, getMergedTypeSystemDescription());
      // update GUI
      setSelectionOneUp(tt, item);
      item.dispose();
    }

    TypeFeature[] featuresToRemove = computeFeaturesToRemove(localTd,
            getMergedTypeSystemDescription().getType(td.getName()));

    if (bTypeInUseElsewhere && !isImportedType(td) && !isBuiltInType(td)) {
      deleteTypeOrFeatureMentions(sTypeNameToRemove, TYPES, null);
    }

    // if removing a type which is also imported or built-in, which is a supertype of something
    // this action can change the feature set.

    if (null != featuresToRemove)
      for (int i = 0; i < featuresToRemove.length; i++) {
        deleteTypeOrFeatureMentions(featuresToRemove[i].featureName, FEATURES,
                featuresToRemove[i].typeName);
      }

    editor.removeDirtyTypeName(sTypeNameToRemove);
    finishAction();
  }

  /**
   * Called when removing a type (or renaming a type - treating old value like remove).
   *
   * @param existingTypeName
   *          the existing type name
   * @param allowed
   *          the allowed
   * @return null means "cancel", stringArray0 means no problem, stringArrayN = dependent types
   */
  private String[] showTypesRequiringThisOneMessage(String existingTypeName, boolean allowed) {
    // if type is imported or built-in, in addition to being defined in this
    // descriptor, removing it won't make the type unavailable.
    if (isImportedType(existingTypeName) || isBuiltInType(existingTypeName))
      return stringArray0; // imported or built-in type will remain
    String[] typesRequiringThisOne = getTypesRequiringThisOne(existingTypeName);
    if (typesRequiringThisOne != null && typesRequiringThisOne.length > 0) {
      String sMsg = existingTypeName + " has the following dependent type(s): ";
      for (int i = 0; i < typesRequiringThisOne.length; i++) {
        if (i > 0) {
          sMsg += ", ";
        }
        sMsg += typesRequiringThisOne[i];
      }
      if (!allowed) {
        sMsg += ".  Please delete dependent types first.";
        Utility.popMessage("Can''t Remove Needed Type", sMsg, MessageDialog.WARNING);
        return null;
      }
      sMsg += ".  If you proceed, the dependent types which are updatable will be updated."
              + " Non-updatable types (imported, etc.) you will have to update manually.  Please confirm.";
      if (Window.OK == Utility.popOkCancel("Confirm renaming type update actions", sMsg,
              MessageDialog.WARNING))
        return typesRequiringThisOne;
      return null;
    }
    return stringArray0;
  }

  /**
   * Removes the type.
   *
   * @param td
   *          the td
   * @param tsd
   *          the tsd
   */
  private void removeType(TypeDescription td, TypeSystemDescription tsd) {
    tsd.setTypes((TypeDescription[]) Utility.removeElementFromArray(tsd.getTypes(), td,
            TypeDescription.class));
  }

  /**
   * Removes the feature.
   *
   * @param td
   *          the td
   * @param fd
   *          the fd
   */
  private void removeFeature(TypeDescription td, FeatureDescription fd) {
    td.setFeatures((FeatureDescription[]) Utility.removeElementFromArray(td.getFeatures(), fd,
            FeatureDescription.class));
  }

  /**
   * Gets the types requiring this one.
   *
   * @param typeName
   *          the type name
   * @return the types requiring this one
   */
  private String[] getTypesRequiringThisOne(String typeName) {
    List upstreamTypeNames = new ArrayList();

    TypeSystemDescription typeSystem = getMergedTypeSystemDescription();

    TypeDescription[] types = typeSystem.getTypes();
    for (int i = 0; i < types.length; i++) {
      if (!types[i].getName().equals(typeName)) {
        if (typeRequiresType(types[i], typeName)) {
          upstreamTypeNames.add(types[i].getName());
        }
      }
    }
    return (String[]) upstreamTypeNames.toArray(new String[upstreamTypeNames.size()]);
  }

  /**
   * Type requires type.
   *
   * @param upstreamType
   *          the upstream type
   * @param typeName
   *          the type name
   * @return true, if successful
   */
  private boolean typeRequiresType(TypeDescription upstreamType, String typeName) {
    if (null == typeName)
      return false;
    if (typeName.equals(upstreamType.getSupertypeName())) {
      return true;
    }

    FeatureDescription[] features = upstreamType.getFeatures();
    if (features == null) {
      return false;
    }

    for (int i = 0; i < features.length; i++) {
      if (typeName.equals(features[i].getRangeTypeName())) {
        return true;
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractSection#enable()
   */
  @Override
  public void enable() {
    jcasGenButton.setEnabled(tt.getItemCount() > 0);
    TypeSystemDescription tsd = getTypeSystemDescription();
    exportButton.setEnabled(false);
    if (null != tsd) {
      TypeDescription[] tsa = tsd.getTypes();
      if (null != tsa)
        exportButton.setEnabled(tsa.length > 0);
    }
    if (isAggregate()) {
      addTypeButton.setEnabled(false);
      addButton.setEnabled(false);
      editButton.setEnabled(false);
      removeButton.setEnabled(false);
    } else {
      addTypeButton.setEnabled(true);
      boolean editable = tt.getSelectionCount() == 1 && isLocalItem(tt.getSelection()[0]);
      addButton.setEnabled(editable);
      editButton.setEnabled(editable);
      removeButton.setEnabled(editable);
    }

  }

  /**
   * Checks if is subtype of string.
   *
   * @param item
   *          the item
   * @return true, if is subtype of string
   */
  private boolean isSubtypeOfString(TreeItem item) {
    TypeDescription td = getTypeDescriptionFromTableTreeItem(item);
    return CAS.TYPE_NAME_STRING.equals(td.getSupertypeName());
  }

  /**
   * Checks if is type in use elsewhere.
   *
   * @param sTypeName
   *          the s type name
   * @return true, if is type in use elsewhere
   */
  private boolean isTypeInUseElsewhere(final String sTypeName) {
    if (!isLocalProcessingDescriptor()) {
      return false;
    }
    final String sTypeName_Colon = sTypeName + ":";

    CapabilityVisitor cv = new CapabilityVisitor() {
      @Override
      boolean visit(TypeOrFeature i_o) {
        if (i_o.isType() && i_o.getName().equals(sTypeName))
          return true;
        else if (i_o.getName().startsWith(sTypeName_Colon))
          return true;
        return false;
      }
    };
    if (capabilityVisit(cv))
      return true;

    FsIndexDescription[] indexes = getAnalysisEngineMetaData().getFsIndexes();

    if (indexes != null) {
      for (int i = 0; i < indexes.length; i++) {
        if (indexes[i].getTypeName().equals(sTypeName)) {
          return true;
        }
      }
    }

    return typePriorityListsVisit(FIND_EQUAL_TYPE, sTypeName);
  }

  /** The Constant FIND_EQUAL_TYPE. */
  private static final int FIND_EQUAL_TYPE = 1;

  /** The Constant REMOVE_EQUAL_TYPE. */
  private static final int REMOVE_EQUAL_TYPE = 2;

  /** The Constant UPDATE_TYPE_NAME. */
  private static final int UPDATE_TYPE_NAME = 4;

  /**
   * Type priority lists visit.
   *
   * @param kind
   *          the kind
   * @param typeName
   *          the type name
   * @return true, if successful
   */
  private boolean typePriorityListsVisit(int kind, String typeName) {
    return typePriorityListsVisit(kind, typeName, null);
  }

  /**
   * Type priority lists visit.
   *
   * @param kind
   *          the kind
   * @param typeName
   *          the type name
   * @param newTypeName
   *          the new type name
   * @return FIND_EQUAL_TYPE: true if found, false otherwise REMOVE_EQUAL_TYPE: true if found, false
   *         if nothing done UPDATE_TYPE_NAME: true if found & updated, false if nothing done
   */
  private boolean typePriorityListsVisit(int kind, String typeName, String newTypeName) {
    boolean returnValue = false;
    TypePriorities priorities = getAnalysisEngineMetaData().getTypePriorities();
    if (priorities != null) {
      TypePriorityList[] priorityLists = priorities.getPriorityLists();
      if (priorityLists != null) {
        for (int i = 0; i < priorityLists.length; i++) {
          String[] typeNames = priorityLists[i].getTypes();
          if (typeNames != null) {
            if (kind == FIND_EQUAL_TYPE) {
              for (int j = 0; j < typeNames.length; j++) {
                if (typeNames[j].equals(typeName))
                  return true;
              }
            } else if (kind == UPDATE_TYPE_NAME) {
              boolean bChanged = false;
              for (int j = 0; j < typeNames.length; j++) {
                if (typeNames[j].equals(typeName)) {
                  typeNames[j] = newTypeName;
                  bChanged = true;
                  break;
                }
              }
              if (bChanged) {
                priorityLists[i].setTypes(typeNames);
                returnValue = true;
              }
            } else if (kind == REMOVE_EQUAL_TYPE) {
              Object[] newTypeNames = Utility.removeElementsFromArray(typeNames, typeName,
                      String.class);
              if (newTypeNames != typeNames) {
                priorityLists[i].setTypes((String[]) newTypeNames);
                returnValue = true;
              }
            } else
              throw new InternalErrorCDE("invalid argument");
          }
        }
      }
    }
    return returnValue;
  }

  /**
   * Delete type or feature from capability.
   *
   * @param io_s
   *          the io s
   * @param isType
   *          the is type
   * @param name
   *          the name
   * @param typeName
   *          the type name
   * @return the type or feature[]
   */
  private TypeOrFeature[] deleteTypeOrFeatureFromCapability(TypeOrFeature[] io_s,
          final boolean isType, String name, String typeName) {

    if (!isType) // is feature
      name = typeName + ':' + name;
    return (TypeOrFeature[]) Utility.removeElementsFromArray(io_s, name, TypeOrFeature.class,
            isType ? capabilityTypeCompare : capabilityFeatureCompare);
  }

  /** The Constant TYPES. */
  private static final boolean TYPES = true;

  /** The Constant FEATURES. */
  private static final boolean FEATURES = false;

  /** The Constant fsIndexDescCompare. */
  private static final Comparator fsIndexDescCompare = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      return ((FsIndexDescription) o2).getTypeName().equals(o1) ? 0 : 1;
    }
  };

  /** The Constant fsIndexKeyDescCompare. */
  private static final Comparator fsIndexKeyDescCompare = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      return (o1.equals(((FsIndexKeyDescription) o2).getFeatureName())) ? 0 : 1;
    }
  };

  /** The Constant capabilityTypeCompare. */
  private static final Comparator capabilityTypeCompare = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      TypeOrFeature tf = (TypeOrFeature) o2;
      return ((tf.isType() == true && tf.getName().equals(o1)) ||
      // remove features belong to type if type is removed
      tf.getName().startsWith(((String) o1) + ':')) ? 0 : 1;
    }
  };

  /** The Constant capabilityFeatureCompare. */
  private static final Comparator capabilityFeatureCompare = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      TypeOrFeature tf = (TypeOrFeature) o2;
      return (tf.isType() == false && tf.getName().equals(o1)) ? 0 : 1;
    }
  };

  /**
   * feature name if used, is short-name.
   *
   * @param typeOrFeatureName
   *          - if feature is short feature name
   * @param isType
   *          the is type
   * @param typeName
   *          the type name
   */
  private void deleteTypeOrFeatureMentions(final String typeOrFeatureName, boolean isType,
          final String typeName) {
    if (!isLocalProcessingDescriptor()) {
      return;
    }
    Capability[] c = getCapabilities();
    for (int ic = 0; ic < c.length; ic++) {
      c[ic].setInputs(deleteTypeOrFeatureFromCapability(c[ic].getInputs(), isType,
              typeOrFeatureName, typeName));
      c[ic].setOutputs(deleteTypeOrFeatureFromCapability(c[ic].getOutputs(), isType,
              typeOrFeatureName, typeName));
    }

    CapabilityPage p = editor.getCapabilityPage();
    if (null != p)
      p.markStale();

    final FsIndexCollection indexCollection = editor.getFsIndexCollection();

    FsIndexDescription[] indexes = (null == indexCollection) ? null
            : indexCollection.getFsIndexes();

    boolean somethingChanged = false;
    if (indexes != null) {
      if (isType) {
        FsIndexDescription[] newFsid = (FsIndexDescription[]) Utility.removeElementsFromArray(
                indexes, typeOrFeatureName, FsIndexDescription.class, fsIndexDescCompare);
        if (newFsid != indexes) {
          somethingChanged = true;
          indexCollection.setFsIndexes(newFsid);
        }
      } else { // is feature
        for (int i = 0; i < indexes.length; i++) {
          if (typeName.equals(indexes[i].getTypeName())) {
            FsIndexKeyDescription[] newFsKeys = (FsIndexKeyDescription[]) Utility
                    .removeElementsFromArray(indexes[i].getKeys(), typeOrFeatureName,
                            FsIndexKeyDescription.class, fsIndexKeyDescCompare);
            if (newFsKeys != indexes[i].getKeys()) {
              somethingChanged = true;
              indexes[i].setKeys(newFsKeys);
            }
          }
        }
      }

      if (somethingChanged) {
        try {
          editor.setMergedFsIndexCollection();
        } catch (ResourceInitializationException e) {
          throw new InternalErrorCDE("unexpected exception", e);
        }
        if (null != editor.getIndexesPage())
          editor.getIndexesPage().markStale();
      }
    }

    if (isType) {
      if (typePriorityListsVisit(REMOVE_EQUAL_TYPE, typeOrFeatureName)) {
        try {
          editor.setMergedTypePriorities();
        } catch (ResourceInitializationException e) {
          throw new InternalErrorCDE("unexpected exception");
        }
        if (null != editor.getIndexesPage())
          editor.getIndexesPage().markStale();
      }
    }
  }

  /**
   * return true if refresh is needed.
   *
   * @param oldTypeName
   *          the old type name
   * @param newTypeName
   *          the new type name
   * @return true if refresh is needed
   */
  private boolean alterTypeMentionsInOtherTypes(String oldTypeName, String newTypeName) {
    // only modify locally modifiable types, but scan all types to give appropriate error msgs
    TypeSystemDescription typeSystem = getMergedTypeSystemDescription();
    boolean refreshNeeded = false;
    boolean remergeNeeded = false;
    TypeDescription[] types = typeSystem.getTypes();
    for (int i = 0; i < types.length; i++) {
      TypeDescription td = types[i];
      TypeDescription localTd = getLocalTypeDefinition(td);
      String typeName = td.getName();
      if (td.getSupertypeName().equals(oldTypeName)) {
        if (null != localTd) { // is a local type
          if (isImportedType(typeName)) {
            Utility.popMessage("Imported type won't be changed",
                    "There is both a local and imported version of type, '" + typeName
                            + "', which has a supertype which is the item being renamed.  Although the local version will be updated, but the imported one won't."
                            + "This may cause an error when you save.",
                    MessageDialog.WARNING);
          }
          if (isBuiltInType(typeName)) {
            // invalid: changed some type name which was a supertype of a built in - but
            // all the supertypes of built-ins are unchangable.
            throw new InternalErrorCDE("invalid state");
          }
        } else { // is not a local type
          // can't be a built-in type because all the supertypes of built-ins are unchangeable
          Utility.popMessage("Imported type not changed", "There is an imported type, '" + typeName
                  + "', which has a supertype which is the item being renamed.  It won't be updated - this may cause an error when you save this descriptor."
                  + "  If it does, you will need to edit the imported type to change it.",
                  MessageDialog.WARNING);
          continue;
        }
        // guaranteed to have local type def here
        localTd.setSupertypeName(newTypeName);

        if (isImportedType(typeName)) {
          remergeNeeded = true;
          refreshNeeded = true;
        } else {
          td.setSupertypeName(newTypeName);
          updateGuiType(tt.getItems()[i], td);
        }
      }
      FeatureDescription fds[] = td.getFeatures();
      FeatureDescription localFds[] = (null == localTd) ? null : localTd.getFeatures();
      if (null != fds) {
        for (int j = 0; j < fds.length; j++) {
          FeatureDescription fd = fds[j];
          if (oldTypeName.equals(fd.getRangeTypeName())) {
            if (warnAndSkipIfImported(typeName))
              continue; // skipped if feature not present in local td, or no local td.

            setNamedFeatureDescriptionRange(localFds, fd.getName(), newTypeName);
            if (isImportedType(typeName)) {
              remergeNeeded = true;
              refreshNeeded = true;
            } else {
              fd.setRangeTypeName(newTypeName);
              updateGuiFeature(tt.getItems()[i].getItems()[j], fd, td);
            }
          }
        }
      }
    }
    if (remergeNeeded)
      rebuildMergedTypeSystem();
    return refreshNeeded;
  }

  /**
   * Sets the named feature description range.
   *
   * @param localFds
   *          the local fds
   * @param featureName
   *          the feature name
   * @param rangeName
   *          the range name
   */
  // this function to set the corresponding feature in the "local" type's fd array
  private void setNamedFeatureDescriptionRange(FeatureDescription[] localFds, String featureName,
          final String rangeName) {
    if (null != localFds) {
      for (int i = 0; i < localFds.length; i++) {
        FeatureDescription fd = localFds[i];
        if (fd.getName().equals(featureName)) {
          fd.setRangeTypeName(rangeName);
          return;
        }
      }
    }
  }

  /**
   * Warn and skip if imported.
   *
   * @param typeName
   *          the type name
   * @return true, if successful
   */
  private boolean warnAndSkipIfImported(String typeName) {
    if (isLocalType(typeName)) {
      if (isImportedType(typeName)) {
        Utility.popMessage("Imported type won't be changed",
                "There is both a local and imported version of type, '" + typeName
                        + "', which has a feature whose range type is the item being renamed.  Although the local version will be updated, but the imported one won't."
                        + "This may cause an error when you save.",
                MessageDialog.WARNING);
      }
      return false;
    } else { // is not a local type
      Utility.popMessage("Imported feature range not changed", "There is an imported type, '"
              + typeName
              + "', which has a a feature whose range which is the item being renamed.  It won't be updated - this may cause an error when you save this descriptor."
              + "  If it does, you will need to edit the imported type to change it.",
              MessageDialog.WARNING);
      return true;
    }
  }

  /**
   * Alter type mentions.
   *
   * @param sOldTypeName
   *          the s old type name
   * @param sNewTypeName
   *          the s new type name
   */
  private void alterTypeMentions(final String sOldTypeName, final String sNewTypeName) {
    if (sOldTypeName.equals(sNewTypeName) || (!isLocalProcessingDescriptor()))
      return;

    // update types (which are locally editable)
    // that reference the old type name, either as
    // ranges of features or
    // supertype
    final boolean[] capabilityChanged = new boolean[1];
    capabilityChanged[0] = false;
    final String oldTypeName_colon = sOldTypeName + ':';
    CapabilityVisitor cv = new CapabilityVisitor() {
      @Override
      boolean visit(TypeOrFeature i_o) {
        if (i_o.isType() && i_o.getName().equals(sOldTypeName)) {
          capabilityChanged[0] = true;
          i_o.setName(sNewTypeName);
        } else if (!i_o.isType() && i_o.getName().startsWith(oldTypeName_colon)) {
          capabilityChanged[0] = true;
          i_o.setName(sNewTypeName + ':' + i_o.getName().substring(oldTypeName_colon.length()));
        }
        return false;
      }
    };

    capabilityVisit(cv);
    if (capabilityChanged[0])
      if (null != editor.getCapabilityPage())
        editor.getCapabilityPage().markStale();

    FsIndexCollection indexCollection = getAnalysisEngineMetaData().getFsIndexCollection();

    FsIndexDescription[] indexes = (null == indexCollection) ? null
            : indexCollection.getFsIndexes();

    boolean somethingChanged = false;
    boolean markStale = false;

    if (indexes != null) {
      for (int i = 0; i < indexes.length; i++) {
        if (indexes[i].getTypeName().equals(sOldTypeName)) {
          indexes[i].setTypeName(sNewTypeName);
          somethingChanged = true;
        }
      }
    }

    if (somethingChanged) {
      markStale = true;
      try {
        editor.setMergedFsIndexCollection();
      } catch (ResourceInitializationException e) {
        throw new InternalErrorCDE("unexpected exception");
      }
    }

    // interesting use case not handled:
    // changing a type name to another type name which
    // already exists (in an import).
    if (typePriorityListsVisit(UPDATE_TYPE_NAME, sOldTypeName, sNewTypeName)) {
      markStale = true;
      try {
        editor.setMergedTypePriorities();
      } catch (ResourceInitializationException e) {
        throw new InternalErrorCDE("unexpected exception");
      }
    }

    if (markStale) {
      editor.getIndexesPage().markStale();
    }

  }

  /**
   * Checks if is feature used in index.
   *
   * @param td
   *          the td
   * @param featureName
   *          the feature name
   * @return true, if is feature used in index
   */
  private boolean isFeatureUsedInIndex(TypeDescription td, String featureName) {
    return isFeatureUsedInIndex(td.getName() + ":" + featureName);
  }

  /**
   * Checks if is feature used in index.
   *
   * @param fullFeatureName
   *          the full feature name
   * @return true, if is feature used in index
   */
  private boolean isFeatureUsedInIndex(String fullFeatureName) {
    if (!isLocalProcessingDescriptor()) {
      return false;
    }

    FsIndexCollection indexCollection = editor.getMergedFsIndexCollection();
    FsIndexDescription[] fsid = (null == indexCollection) ? null : indexCollection.getFsIndexes();
    if (null != fsid) {
      for (int i = 0; i < fsid.length; i++) {
        FsIndexKeyDescription[] keys = fsid[i].getKeys();
        if (null != keys) {
          for (int j = 0; j < keys.length; j++) {
            if (keys[j].getFeatureName().equals(fullFeatureName))
              return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if is feature in use elsewhere.
   *
   * @param td
   *          the td
   * @param featureName
   *          the feature name
   * @return true, if is feature in use elsewhere
   */
  private boolean isFeatureInUseElsewhere(TypeDescription td, String featureName) {
    return isFeatureInUseElsewhere(td.getName() + ':' + featureName);
  }

  /**
   * Checks if is feature in use elsewhere.
   *
   * @param typePlusFeature
   *          the type plus feature
   * @return true, if is feature in use elsewhere
   */
  private boolean isFeatureInUseElsewhere(final String typePlusFeature) {
    if (!isLocalProcessingDescriptor()) { // not strictly true for imports....
      return false;
    }
    CapabilityVisitor v = new CapabilityVisitor() {
      @Override
      boolean visit(TypeOrFeature i_o) {
        if ((!i_o.isType()) && i_o.getName().equals(typePlusFeature)) {
          return true;
        }
        return false;
      }
    };

    if (capabilityVisit(v))
      return true;

    return isFeatureUsedInIndex(typePlusFeature);
  }

  /**
   * The Class CapabilityVisitor.
   */
  private abstract class CapabilityVisitor {

    /**
     * Visit.
     *
     * @param i_o
     *          the i o
     * @return true, if successful
     */
    abstract boolean visit(TypeOrFeature i_o);
  }

  /**
   * Capability visit.
   *
   * @param v
   *          the v
   * @return true, if successful
   */
  private boolean capabilityVisit(CapabilityVisitor v) {
    Capability[] c = getCapabilities();
    for (int ic = 0; ic < c.length; ic++) {
      TypeOrFeature[] inputs = c[ic].getInputs();

      for (int i = 0; i < inputs.length; i++) {
        if (v.visit(inputs[i]))
          return true;
      }

      TypeOrFeature[] outputs = c[ic].getOutputs();

      for (int i = 0; i < outputs.length; i++) {
        if (v.visit(outputs[i]))
          return true;
      }
    }
    return false;
  }

  /**
   * Alter feature mentions.
   *
   * @param sOldFeatureName
   *          the s old feature name
   * @param sNewFeatureName
   *          the s new feature name
   * @param typeName
   *          the type name
   */
  private void alterFeatureMentions(final String sOldFeatureName, final String sNewFeatureName,
          final String typeName) {
    final boolean[] somethingChanged = new boolean[1];
    somethingChanged[0] = false;
    if (sOldFeatureName.equals(sNewFeatureName) || (!isLocalProcessingDescriptor()))
      return;
    final String oldFullFeatureName = typeName + ':' + sOldFeatureName;
    final String newFullFeatureName = typeName + ':' + sNewFeatureName;
    CapabilityVisitor v = new CapabilityVisitor() {
      @Override
      boolean visit(TypeOrFeature i_o) {
        if (!i_o.isType() && i_o.getName().equals(oldFullFeatureName)) {
          somethingChanged[0] = true;
          i_o.setName(newFullFeatureName);
        }
        return false;
      }
    };

    capabilityVisit(v);
    if (somethingChanged[0])
      if (null != editor.getCapabilityPage())
        editor.getCapabilityPage().markStale();

    somethingChanged[0] = false;
    FsIndexDescription[] fsid = getAnalysisEngineMetaData().getFsIndexes();
    if (null != fsid) {
      for (int i = 0; i < fsid.length; i++) {
        if (typeName.equals(fsid[i].getTypeName())) {
          FsIndexKeyDescription[] keys = fsid[i].getKeys();
          if (null != keys) {
            for (int j = 0; j < keys.length; j++) {
              if (keys[j].getFeatureName().equals(sOldFeatureName)) {
                somethingChanged[0] = true;
                keys[j].setFeatureName(sNewFeatureName);
              }
            }
          }
        }
      }
    }

    if (somethingChanged[0]) {
      try {
        editor.setMergedFsIndexCollection(); // overkill
      } catch (ResourceInitializationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (null != editor.getIndexesPage())
        editor.getIndexesPage().markStale();
    }
  }

}
