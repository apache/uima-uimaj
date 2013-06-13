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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.StandardStrings;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.MultiPageEditorContributor;
import org.apache.uima.taeconfigurator.model.BuiltInTypes;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.PopupList;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class AbstractSection extends SectionPart /* extends FormSection */
implements Listener, StandardStrings {

  protected FormToolkit toolkit;

  public MultiPageEditor editor;

  public FormToolkit getToolkit() {
    return toolkit;
  }

  public final static String IMPORTABLE_PART_CONTEXT = "ipc";

  public final static String PLUGIN_ID = "org.apache.uima.desceditor";

  public final static boolean SELECTED = true;

  public final static boolean NOT_SELECTED = false;

  public final static boolean ENABLED = true;

  public final static boolean EQUAL_WIDTH = true;

  public final static TreeItem[] treeItemArray0 = new TreeItem[0];

  public final static ConfigurationGroup[] configurationGroup0 = new ConfigurationGroup[0];

  public final static ConfigurationParameter[] configurationParameter0 = new ConfigurationParameter[0];

  public final static Capability[] capabilityArray0 = new Capability[0];

  public final static FeatureDescription[] featureDescriptionArray0 = new FeatureDescription[0];

  public final static SofaMapping[] sofaMapping0 = new SofaMapping[0];

  public final static FsIndexDescription[] fsIndexDescription0 = new FsIndexDescription[0];

  public final static ExternalResourceBinding[] externalResourceBinding0 = new ExternalResourceBinding[0];

  public final static ExternalResourceDescription[] externalResourceDescription0 = new ExternalResourceDescription[0];

  public final static TypeDescription[] typeDescription0 = new TypeDescription[0];

  public final static TypePriorityList[] typePriorityList0 = new TypePriorityList[0];

  protected int initialFormWidth; // width of the form before putting controls in it

  public AbstractSection(MultiPageEditor aEditor, Composite parent, String headerText,
          String description) {
    super(parent, aEditor.getToolkit(), ((null != description) ? Section.DESCRIPTION : 0)
            | Section.TWISTIE | Section.EXPANDED);
    toolkit = aEditor.getToolkit();
    getSection().setText(headerText);
    getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    toolkit.createCompositeSeparator(getSection());
    if (null != description)
      getSection().setDescription(description);
    editor = aEditor;
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);
    getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
  }

  // **************************************************
  // * Subclasses need to implement these methods
  // **************************************************

  public abstract void enable();

  // **************************************************
  // * convenience methods
  // **************************************************
  protected void setFileDirty() {
    editor.setFileDirty();
  }

  // **************************************************
  // * Creating Composites
  // **************************************************
  public Composite newComposite(Composite parent) {
    return newNcolumnComposite(parent, 1);
  }

  public Composite new2ColumnComposite(Composite parent) {
    return newNcolumnComposite(parent, 2);
  }

  public Composite new3ColumnComposite(Composite parent) {
    return newNcolumnComposite(parent, 3);
  }

  public Composite new4ColumnComposite(Composite parent) {
    return newNcolumnComposite(parent, 4);
  }

  public Composite newNcolumnComposite(Composite parent, int cols) {
    Composite composite = toolkit.createComposite(parent);
    if (parent instanceof ExpandableComposite)
      ((ExpandableComposite) parent).setClient(composite);
    GridLayout layout = new GridLayout(cols, !EQUAL_WIDTH);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    return composite;
  }

  public void setMargins(Composite composite, int height, int width) {
    GridLayout g = (GridLayout) composite.getLayout();
    g.marginHeight = height;
    g.marginWidth = width;
  }

  public void enableBorders(Composite composite) {
    GridLayout g = (GridLayout) composite.getLayout();
    if (g.marginHeight < 2)
      g.marginHeight = 2;
    if (g.marginWidth < 1)
      g.marginWidth = 1;
  }

  // **************************************************
  // * Special Composites to hold buttons
  // **************************************************
  final static public int VERTICAL_BUTTONS = 1;

  final static public int HORIZONTAL_BUTTONS = 2;

  public Composite newButtonContainer(Composite parent) {
    return newButtonContainer(parent, VERTICAL_BUTTONS, 0);
  }

  public Composite newButtonContainer(Composite parent, int style, int widthMin) {
    Composite buttonContainer = toolkit.createComposite(parent);
    GridLayout gl = new GridLayout();
    GridData gd = null;
    switch (style) {
      case VERTICAL_BUTTONS:
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalAlignment = SWT.FILL;
        // gd.widthHint = widthMin; // 70, 100
        break;
      case HORIZONTAL_BUTTONS:
        gl.marginWidth = 20;
        gl.numColumns = 2;
        gl.makeColumnsEqualWidth = true;
        gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gd.widthHint = widthMin; // 180
    }

    buttonContainer.setLayout(gl);
    buttonContainer.setLayoutData(gd);
    return buttonContainer;
  }

  // public Composite newLabeledButtonContainer(Composite parent, int cellsToSpan, int nbrCols) {
  // Composite buttonContainer = toolkit.createComposite(parent);
  // GridLayout gl = new GridLayout();
  // GridData gd = null;
  // gl.marginWidth = 0;
  // gl.numColumns = nbrCols;
  // gl.makeColumnsEqualWidth = false;
  // gd = new GridData();
  // gd.horizontalSpan = cellsToSpan;
  // buttonContainer.setLayout(gl);
  // buttonContainer.setLayoutData(gd);
  // return buttonContainer;
  // }

  // **************************************************
  // * Getting internationalized text
  // **************************************************

  // **************************************************
  // * Widgets
  // **************************************************

  protected Text newLabeledTextField(Composite parent, String label, String tip) {
    return newLabeledTextField(parent, label, tip, SWT.NONE);
  }

  protected Text newLabeledTextField(Composite parent, String labelKey, String textToolTip,
          int style) {
    enableBorders(parent);
    Label label = toolkit.createLabel(parent, labelKey);
    label.setToolTipText(textToolTip);
    if ((style & SWT.V_SCROLL) == SWT.V_SCROLL) {
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    }
    return newTextWithTip(parent, "", style, textToolTip); //$NON-NLS-1$
  }

  protected Text newTextWithTip(Composite parent, String initialTxt, String tip) {
    return newTextWithTip(parent, initialTxt, SWT.NONE, tip);
  }

  protected Text newTextWithTip(Composite parent, String text, int style, String tip) {
    Text t = toolkit.createText(parent, text, style);
    t.setToolTipText(tip);
    if ((style & SWT.V_SCROLL) == SWT.V_SCROLL) {
      t.setLayoutData(new GridData(GridData.FILL_BOTH));
    } else {
      t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
    addListenerForPastableWidget(t);
    return t;
  }

  public Label newLabelWithData(Composite parent, String text) {
    return newLabelWithTip(parent, text, ""); //$NON-NLS-1$
  }

  public Label newLabelWithTip(Composite parent, String text, String tip) {
    return newLabelWithTip(parent, text, tip, SWT.NULL);
  }

  public Label newUnUpdatableTextWithTip(Composite parent, String text, String tip) {
    Label label = newLabelWithTip(parent, text, tip, SWT.BORDER);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    return label;
  }

  public Label newLabelWithTip(Composite parent, String text, String tip, int style) {
    Label t = toolkit.createLabel(parent, text, style);
    if ((tip != null) && (tip.length()) > 0)
      t.setToolTipText(tip);
    return t;
  }

  protected CCombo newLabeledCComboWithTip(Composite parent, String labelKey, String tip) {
    newLabelWithTip(parent, labelKey, tip);
    return newCComboWithTip(parent, tip);
  }

  protected CCombo newCComboWithTip(Composite parent, String tip) {
    CCombo ccombo = new CCombo(parent, SWT.FLAT | SWT.READ_ONLY);
    toolkit.adapt(ccombo, false, false);
    ccombo.setToolTipText(tip);
    ccombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    ccombo.addListener(SWT.Selection, this);
    // Make the CCombo's border visible since CCombo is NOT a widget supported
    // by FormToolkit.
    // needed apparently by RedHat Linux 
    ccombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
    return ccombo;
  }

  protected Text newDescriptionTextBox(Composite parent, String tip) {
    return newLabeledTextField(parent, S_DESCRIPTION, tip, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
  }

  // **************************************************
  // * Widgets Buttons
  // **************************************************
  public Button newRadioButton(Composite parent, String label, String toolTip, boolean selected) {
    Button button = toolkit.createButton(parent, label, SWT.RADIO);
    button.setToolTipText(toolTip);
    button.setSelection(selected);
    button.addListener(SWT.Selection, this);
    return button;
  }

  /**
   * add pushbutton to container, set enabled, add listener for it
   * 
   * @param parent
   * @param root
   * @return
   */
  public Button newPushButton(Composite parent, String label, String tip) {
    return newPushButton(parent, label, tip, true); // set enabled by default
  }

  /**
   * Add a push button to a container, add a listener for it too
   * 
   * @param parent
   * @param root
   * @param enabled
   * @return
   */
  public Button newPushButton(Composite parent, String label, String tip, boolean enabled) {
    return newPushButton(parent, label, tip, enabled, 0);
  }

  public Button newPushButton(Composite parent, String label, String tip, boolean enabled, int style) {
    Button button = toolkit.createButton(parent, label, SWT.PUSH | style);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
    button.setLayoutData(gd);
    button.pack(false);
    button.setToolTipText(tip);
    button.setEnabled(enabled);
    button.addListener(SWT.Selection, this);
    Point buttonSize = button.getSize();
    gd.heightHint = buttonSize.y - 2;
    gd.widthHint = buttonSize.x - 2;
    return button;
  }

  public Button newCheckBox(Composite parent, String label, String tip) {
    Button button = toolkit.createButton(parent, label, SWT.CHECK);
    button.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING
            | GridData.HORIZONTAL_ALIGN_FILL));
    button.pack();
    button.setToolTipText(tip);
    button.addListener(SWT.Selection, this);
    return button;
  }

  public static void spacer(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setVisible(false);
    // toolkit.createLabel(container, " ");
  }

  // **************************************************
  // * Tables
  // **************************************************
  final public static int NO_MIN_HEIGHT = -1;

  final public static int NOTHING_SELECTED = -1;

  // these flags can be or-ed together
  final public static int LINES_VISIBLE = 1;

  final public static int HEADER_VISIBLE = 2;

  final public static int WIDTH_NOT_SPECIFIED = 0;

  protected Table newTable(Composite parent) {
    return newTable(parent, SWT.FULL_SELECTION, NO_MIN_HEIGHT, 0);
  }

  protected Table newTable(Composite parent, int style, int minHeight) {
    return newTable(parent, style, minHeight, 0);
  }

  protected Table newTable(Composite parent, int style, int minHeight, int flags) {
    Table table = toolkit.createTable(parent, style);
    GridData gd = new GridData(GridData.FILL_BOTH);
    if (minHeight != NO_MIN_HEIGHT)
      gd.heightHint = minHeight;
    table.setLayoutData(gd);

    table.setLinesVisible(0 != (flags & LINES_VISIBLE));
    table.setHeaderVisible(0 != (flags & HEADER_VISIBLE));
    table.addListener(SWT.Selection, this);
    table.addListener(SWT.KeyUp, this); // delete key
    return table;
  }

  protected Tree newTree(Composite parent) {
    Tree tree = toolkit.createTree(parent, SWT.SINGLE);
    tree.setLayoutData(new GridData(GridData.FILL_BOTH));
    tree.addListener(SWT.Selection, this);
    tree.addListener(SWT.KeyUp, this);
    return tree;
  }

  protected TreeItem getPreviousSelection(TreeItem[] items, TreeItem nextItem) {
    TreeItem prevItem = nextItem.getParentItem();
    for (int i = 0; i < items.length; i++) {
      if (nextItem == items[i])
        return prevItem;
      prevItem = items[i];
    }
    return prevItem;
  }

  protected int getItemIndex(TreeItem[] items, TreeItem item) {
    for (int i = 0; i < items.length; i++) {
      if (items[i] == item)
        return i;
    }
    return -1;
  }

  /**
   * @param parent
   * @param style
   *          SWT.SINGLE SWT.MULTI SWT.CHECK SWT.FULL_SELECTION
   * @return
   */
  protected TableTree newTableTree(Composite parent, int style) {
    TableTree tt = new TableTree(parent, style);
    tt.setLayoutData(new GridData(GridData.FILL_BOTH));
    toolkit.adapt(tt, true, true);
    tt.addListener(SWT.Selection, this);
    tt.getTable().addListener(SWT.KeyUp, this); // for delete key
    tt.getTable().addListener(SWT.MouseDoubleClick, this); // for edit
    tt.addListener(SWT.Expand, this);
    tt.addListener(SWT.Collapse, this);
 
    // Make the TableTree's border visible since TableTree is NOT a widget supported
    // by FormToolkit.  Needed by RedHat Linux
    tt.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
    return tt;
  }

  public void packTable(Table table) {
    TableColumn[] columns = table.getColumns();
    for (int i = 0; i < columns.length; i++) {
      columns[i].pack();
    }
  }

  public static int getIndex(TableItem item) {
    Table parent = item.getParent();
    TableItem[] items = parent.getItems();
    for (int i = items.length - 1; i >= 0; i--) {
      if (items[i] == item)
        return i;
    }
    throw new InternalErrorCDE("invalid state"); //$NON-NLS-1$
  }

  public static int getIndex(TableTreeItem item) {
    TableTreeItem parent = item.getParentItem();
    TableTreeItem[] items = (null == parent) ? item.getParent().getItems() : parent.getItems();
    for (int i = items.length - 1; i >= 0; i--) {
      if (items[i] == item)
        return i;
    }
    throw new InternalErrorCDE("invalid state"); //$NON-NLS-1$
  }

  protected void removeChildren(TableTreeItem item) {
    TableTreeItem[] items = item.getItems();
    if (null != items)
      for (int i = 0; i < items.length; i++) {
        items[i].dispose();
      }
  }

  protected void removeChildren(TreeItem item) {
    TreeItem[] items = item.getItems();
    if (null != items)
      for (int i = 0; i < items.length; i++) {
        items[i].dispose();
      }
  }

  // **********************************
  // * Table Column with header setting
  // **********************************
  protected TableColumn newTableColumn(Table table) {
    return newTableColumn(table, ""); //$NON-NLS-1$
  }

  protected TableColumn newTableColumn(Table container, String header) {
    return newTableColumn(container, 50, SWT.LEFT, header);
  }

  protected TableColumn newTableColumn(Table container, int width, int alignment, String header) {
    TableColumn tc = new TableColumn(container, alignment);
    if (header != null && (!header.equals(""))) { //$NON-NLS-1$
      tc.setText(header);
    }
    tc.setWidth(width);
    return tc;
  }

  protected TableColumn newTableColumn(Table container, int width) {
    return newTableColumn(container, width, SWT.LEFT, Messages.getString("AbstractSection.0")); //$NON-NLS-1$
  }

  // **************************************************
  // * Model Access
  // **************************************************

  public boolean isPrimitive() {
    return editor.isPrimitive();
  }

  public boolean isAggregate() {
    return editor.isAggregate();
  }

  public boolean isAeDescriptor() {
    return editor.isAeDescriptor();
  }

  public boolean isTypeSystemDescriptor() {
    return editor.isTypeSystemDescriptor();
  }

  public boolean isIndexDescriptor() {
    return editor.isFsIndexCollection();
  }

  public boolean isTypePriorityDescriptor() {
    return editor.isTypePriorityDescriptor();
  }

  public boolean isExtResAndBindingsDescriptor() {
    return editor.isExtResAndBindingsDescriptor();
  }

  public boolean isCollectionReaderDescriptor() {
    return editor.isCollectionReaderDescriptor();
  }

  public boolean isCasInitializerDescriptor() {
    return editor.isCasInitializerDescriptor();
  }

  public boolean isCasConsumerDescriptor() {
    return editor.isCasConsumerDescriptor();
  }

  public boolean isFlowControllerDescriptor() {
    return editor.isFlowControllerDescriptor();
  }

  public boolean isLocalProcessingDescriptor() {
    return editor.isLocalProcessingDescriptor();
  }

  public AnalysisEngineMetaData getAnalysisEngineMetaData() {
    return editor.getAeDescription().getAnalysisEngineMetaData();
  }

  public FlowControllerDeclaration getFlowControllerDeclaration() {
    return editor.getAeDescription().getFlowControllerDeclaration();
  }

  public void setFlowControllerDeclaration(FlowControllerDeclaration fcd) {
    editor.getAeDescription().setFlowControllerDeclaration(fcd);
  }

  public OperationalProperties getOperationalProperties() {
    return editor.getAeDescription().getAnalysisEngineMetaData().getOperationalProperties();
  }

  public SofaMapping[] getSofaMappings() {
    SofaMapping[] sofaMappings = editor.getAeDescription().getSofaMappings();
    return null == sofaMappings ? sofaMapping0 : sofaMappings;
  }

  public static SofaMapping[] getSofaMappings(MultiPageEditor pEditor) {
    SofaMapping[] sofaMappings = pEditor.getAeDescription().getSofaMappings();
    return null == sofaMappings ? sofaMapping0 : sofaMappings;
  }

  public Map getDelegateAnalysisEngineSpecifiersWithImports() {
    return editor.getAeDescription().getDelegateAnalysisEngineSpecifiersWithImports();
  }

  public Capability[] getCapabilities() {
    Capability[] c = getAnalysisEngineMetaData().getCapabilities();
    if (null == c)
      return capabilityArray0;
    return c;
  }

  protected TypeSystemDescription getMergedTypeSystemDescription() {
    return editor.getMergedTypeSystemDescription();
  }

  protected TypeSystemDescription getTypeSystemDescription() {
    return editor.getTypeSystemDescription();
  }

  protected TypePriorities getTypePriorities() {
    TypePriorities tps = getAnalysisEngineMetaData().getTypePriorities();
    if (null == tps) {
      getAnalysisEngineMetaData().setTypePriorities(
              tps = UIMAFramework.getResourceSpecifierFactory().createTypePriorities());
    }
    return tps;
  }

  public final static String[] stringArray0 = new String[0];

  public final static ConfigurationParameter[] configurationParameterArray0 = new ConfigurationParameter[0];

  public final static ConfigurationGroup[] configurationGroupArray0 = new ConfigurationGroup[0];

  public final static NameValuePair[] nameValuePairArray0 = new NameValuePair[0];

  public String[] getAvailableTypeNames(Set excluded) {
    Map allTypes = editor.allTypes.get();
    Collection availableTypes = new ArrayList();
    Iterator it = allTypes.keySet().iterator();
    while (it.hasNext()) {
      String item = (String) it.next();
      if (!excluded.contains(item))
        availableTypes.add(item);
    }
    return (String[]) availableTypes.toArray(stringArray0);
  }

  // ************
  // * Parameters
  // ************
  public boolean isParmGroup() {
    ConfigurationParameterDeclarations lcpd = getAnalysisEngineMetaData()
            .getConfigurationParameterDeclarations();
    return (lcpd.getCommonParameters() != null && lcpd.getCommonParameters().length > 0)
            || (lcpd.getConfigurationGroups() != null && lcpd.getConfigurationGroups().length > 0);
  }

  public ConfigurationParameterDeclarations getConfigurationParameterDeclarations() {
    return editor.getAeDescription().getAnalysisEngineMetaData()
            .getConfigurationParameterDeclarations();
  }

  // **************************************************
  // * Common GUI state access
  // **************************************************

  public ResourceManagerConfiguration getResourceManagerConfiguration() {
    ResourceManagerConfiguration rmc = editor.getAeDescription().getResourceManagerConfiguration();
    if (null == rmc) {
      rmc = UIMAFramework.getResourceSpecifierFactory().createResourceManagerConfiguration();
      editor.getAeDescription().setResourceManagerConfiguration(rmc);
    }
    return rmc;
  }

  public ExternalResourceDependency[] getExternalResourceDependencies() {
    ExternalResourceDependency[] erd = editor.getAeDescription().getExternalResourceDependencies();
    if (null == erd) {
      return new ExternalResourceDependency[0];
    }
    return erd;
  }

  public ExternalResourceBinding[] getExternalResourceBindings() {
    ExternalResourceBinding[] erb = getResourceManagerConfiguration().getExternalResourceBindings();
    if (null == erb)
      getResourceManagerConfiguration().setExternalResourceBindings(
              erb = new ExternalResourceBinding[0]);
    return erb;
  }

  public ExternalResourceDescription[] getExternalResources() {
    ExternalResourceDescription[] erd = getResourceManagerConfiguration().getExternalResources();
    if (null == erd)
      getResourceManagerConfiguration().setExternalResources(
              erd = new ExternalResourceDescription[0]);
    return erd;
  }

  // **************************************************
  // * Common Listener things
  // **************************************************
  protected void addListenerForPastableWidget(Widget w) {
    w.addListener(SWT.KeyUp, this);
    w.addListener(SWT.MouseUp, this); // for paste operation
  }

  // **************************************************
  // * Common Actions in Handlers
  // **************************************************

  protected boolean valueChanged;

  protected String setValueChanged(String newValue, String oldValue) {
    if (null == newValue)
      valueChanged = valueChanged || (null != oldValue);
    else if (!newValue.equals(oldValue))
      valueChanged = true;
    return newValue;
  }

  protected int setValueChangedInt(int newValue, int oldValue) {
    if (newValue != oldValue)
      valueChanged = true;
    return newValue;
  }

  protected boolean setValueChangedBoolean(boolean newValue, boolean oldValue) {
    if (newValue != oldValue)
      valueChanged = true;
    return newValue;
  }

  protected Boolean setValueChangedCapitalBoolean(Boolean newValue, Boolean oldValue) {
    if (null == newValue) {
      valueChanged |= null != oldValue;
    } else if (null == oldValue) {
      valueChanged = true;
    } else if (newValue.booleanValue() != oldValue.booleanValue())
      valueChanged = true;
    return newValue;
  }

  protected FsIndexKeyDescription[] setValueChangedKeys(FsIndexKeyDescription[] newKeys,
          FsIndexKeyDescription[] oldKeys) {
    if (valueChanged) {
    } else if (oldKeys == null && newKeys == null) {
    } else if (oldKeys != null && Arrays.equals(oldKeys, newKeys)) {
    } else if (Arrays.equals(newKeys, oldKeys)) // newKeys must be non-null here
    {
    } else
      valueChanged = true;

    return newKeys;
  }

  protected boolean isValidAe() {
    if (editor.isValidAE(editor.getAeDescription()))
      return true;
    return false;
  }

  protected void revertTypeSystem(TypeSystemDescription tsd) {
    try {
      editor.setTypeSystemDescription(tsd);
    } catch (ResourceInitializationException e) {
    }
  }

  protected void revertMsg(String msgTitle, String msgTxt, String exceptionMessage) {
    Utility.popMessage(msgTitle, msgTxt + "\r\n" + exceptionMessage, //$NON-NLS-1$
            MessageDialog.ERROR);
    return;
  }

  /**
   * 
   * @param msg
   * @param msgDetails
   * @return true to revert, false to continue
   */
  public static boolean revertOrContinue(String msg, String msgDetails) {
    if (Window.CANCEL == Utility.popMessage(msg, msgDetails
            + "\nDo you want to continue, or Abort the last action?", MessageDialog.QUESTION,
            new String[] { "Continue", "Abort" }))
      return true; // for closing the window or hitting Undo
    return false;
  }

  public void markStale(IFormPart section) {
    if (section != null)
      ((AbstractFormPart) section).markStale();
  }

  protected void markRestOfPageStale(IManagedForm mform, AbstractSection section) {
    if (null == mform)
      return;
    IFormPart[] parts = mform.getParts();
    for (int i = 0; i < parts.length; i++) {
      markStaleIfDifferent(section, parts[i]);
    }
  }

  protected void markStaleIfDifferent(IFormPart thisOne, IFormPart otherOne) {
    if (thisOne != otherOne)
      markStale(otherOne);
  }

  protected String multiLineFix(String s) {
    if (null == s)
      return null;
    return s.replaceAll("\\r\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /*************************************************************************************************
   * Augment arrays (make new array, add one item to it at end
   * *********************************************************
   */

  public String[] stringArrayAdd(String[] a, String s) {
    if (a == null)
      return new String[] { s };

    String[] newA = new String[a.length + 1];
    System.arraycopy(a, 0, newA, 0, a.length);
    newA[newA.length - 1] = s;
    return newA;
  }

  public String[] stringArrayRemove(String[] a, String s) {
    String[] result = new String[a.length - 1];
    for (int i = 0, j = 0; i < a.length; i++) {
      if (!a[i].equals(s))
        result[j++] = a[i];
    }
    return result;
  }

  public TypeOrFeature[] typeOrFeatureArrayAdd(TypeOrFeature[] a, TypeOrFeature item) {
    if (null == a)
      return new TypeOrFeature[] { item };

    TypeOrFeature[] result = new TypeOrFeature[a.length + 1];
    System.arraycopy(a, 0, result, 0, a.length);
    result[result.length - 1] = item;
    return result;
  }

  public TypeOrFeature[] typeOrFeatureArrayRemove(TypeOrFeature[] a, TypeOrFeature item) {
    TypeOrFeature[] result = new TypeOrFeature[a.length - 1];
    for (int i = 0, j = 0; i < a.length; i++) {
      if (a[i] != item)
        result[j++] = a[i];
    }
    return result;
  }

  public TypeOrFeature[] typeOrFeatureArrayRemove(TypeOrFeature[] a, String s) {
    TypeOrFeature[] result = new TypeOrFeature[a.length - 1];
    for (int i = 0, j = 0; i < a.length; i++) {
      if (!a[i].getName().equals(s)) {
        // debug
        if (j == a.length - 1)
          throw new InternalErrorCDE("feature or type not found: looking for " + s);
        result[j++] = a[i];
      }
    }
    return result;
  }

  public static TypeOrFeature getTypeOrFeature(TypeOrFeature[] a, String name) {
    if (null == a)
      return null;
    for (int i = 0; i < a.length; i++) {
      if (a[i].getName().equals(name))
        return a[i];
    }
    return null;
  }

  // tool tips seem to require no blanks following /n on Windows.
  protected static String parseToFitInToolTips(String text) {
    if (null == text)
      return "";
    StringBuffer buffer = new StringBuffer();
    final int MAGIC_LENGTH = 65;
    StringTokenizer tokenizer = new StringTokenizer(text);
    int lengthAccumulator = 0;
    while (tokenizer.hasMoreTokens()) {
      if (lengthAccumulator > 0) {
        buffer.append(' ');
      }
      String nextToken = tokenizer.nextToken();
      buffer.append(nextToken);
      lengthAccumulator += (nextToken.length() + 1);
      if (lengthAccumulator > MAGIC_LENGTH && tokenizer.hasMoreTokens()) {
        // this is to avoid a final blank line
        buffer.append("\n"); //$NON-NLS-1$
        lengthAccumulator = 0;
      }
    }
    return new String(buffer);
  }

  public String formatName(String name) {
    if (null == name)
      return ""; //$NON-NLS-1$
    if (MultiPageEditorContributor.getUseQualifiedTypes())
      return name;
    return getShortName(name);
  }

  public static String getShortName(String name) {
    if (null == name)
      return ""; //$NON-NLS-1$
    int i = name.lastIndexOf('.');
    if (i < 0)
      return name;
    return name.substring(i + 1);
  }

  public static String getShortFeatureName(String name) {
    return (name.substring(name.indexOf(':') + 1));
  }

  public static String getTypeFromFullFeatureName(String name) {
    return (name.substring(0, name.indexOf(':')));
  }

  public static String getNameSpace(String name) {
    int i = name.lastIndexOf('.');
    if (i < 0)
      return "";
    return (name.substring(0, i));
  }

  /**
   * gets a feature description for a type, including supertypes.
   * 
   * @param td
   * @param featureName
   * @return
   */
  public FeatureDescription getFeature(TypeDescription td, String featureName) {
    FeatureDescription[] features = td.getFeatures();
    String supertypeName;
    if (null != features)
      for (int i = 0; i < features.length; i++) {
        if (featureName.equals(features[i].getName()))
          return features[i];
      }
    if (null != (supertypeName = td.getSupertypeName()))
      if (!CAS.TYPE_NAME_TOP.equals(supertypeName)) { //$NON-NLS-1$
        TypeDescription supertype = getMergedTypeSystemDescription().getType(supertypeName);
        if (null == supertype)
          supertype = (TypeDescription) BuiltInTypes.typeDescriptions.get(supertypeName);
        return getFeature(supertype, featureName);
      }
    return null;
  }

  // means is this range allowed in the UIMA Index Spec as a Key
  public static boolean isIndexableRange(String rangeName) {
    return CAS.TYPE_NAME_BYTE.equals(rangeName) || CAS.TYPE_NAME_SHORT.equals(rangeName)
            || CAS.TYPE_NAME_INTEGER.equals(rangeName) || CAS.TYPE_NAME_LONG.equals(rangeName)
            || CAS.TYPE_NAME_FLOAT.equals(rangeName) || CAS.TYPE_NAME_DOUBLE.equals(rangeName)
            || CAS.TYPE_NAME_STRING.equals(rangeName);
  }

  public static void setToolTipText(Control w, String text) {
    if (null != text)
      w.setToolTipText(parseToFitInToolTips(text));
  }

  public static String maybeShortenFileName(String filePathName) {
    if (filePathName.length() > 65) {
      String pathName = filePathName.replace('\\', '/');
      int nLoc = pathName.lastIndexOf('/');
      return filePathName.substring(0, 61 - (pathName.length() - nLoc)) + ".../"
              + filePathName.substring(nLoc + 1);
    }
    return filePathName;
  }

  public static void swapTableTreeItems(TableTreeItem itemBelow, int newSelection) {
    TableTreeItem parent = itemBelow.getParentItem();
    if (null == parent)
      throw new InternalErrorCDE("invalid arg");
    int i = getIndex(itemBelow);
    TableTreeItem itemAbove = parent.getItems()[i - 1];
    TableTreeItem newItemAbove = new TableTreeItem(parent, SWT.NONE, i - 1);
    copyTableTreeItem(newItemAbove, itemBelow);
    TableTreeItem newItemBelow = new TableTreeItem(parent, SWT.NONE, i);
    copyTableTreeItem(newItemBelow, itemAbove);
    itemAbove.dispose();
    itemBelow.dispose();
    parent.getParent().setSelection(new TableTreeItem[] { parent.getItems()[newSelection] });
  }

  public static void copyTableTreeItem(TableTreeItem target, TableTreeItem source) {
    int columnCount = target.getParent().getTable().getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      String text = source.getText(i);
      if (null != text)
        target.setText(i, text);
    }
    target.setData(source.getData());
  }

  public static void swapIndexKeys(TableTreeItem itemBelow, int newSelection) {
    TableTreeItem parent = itemBelow.getParentItem();
    FsIndexDescription fsid = getFsIndexDescriptionFromTableTreeItem(parent);
    int i = getIndex(itemBelow);
    FsIndexKeyDescription[] keys = fsid.getKeys();
    FsIndexKeyDescription temp = keys[i];
    keys[i] = keys[i - 1];
    keys[i - 1] = temp;

    // swap items in the GUI
    swapTableTreeItems(itemBelow, newSelection);
  }

  public static void swapTableItems(TableItem itemBelow, int newSelection) {
    Table parent = itemBelow.getParent();
    int i = getIndex(itemBelow);
    TableItem itemAbove = parent.getItems()[i - 1];
    TableItem newItemAbove = new TableItem(parent, SWT.NONE, i - 1);
    copyTableItem(newItemAbove, itemBelow);
    TableItem newItemBelow = new TableItem(parent, SWT.NONE, i);
    copyTableItem(newItemBelow, itemAbove);
    itemAbove.dispose();
    itemBelow.dispose();
    parent.setSelection(newSelection);
  }

  public static void copyTableItem(TableItem target, TableItem source) {
    int columnCount = target.getParent().getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      String text = source.getText(i);
      if (null != text)
        target.setText(i, text);
    }
    target.setData(source.getData());
  }

  public static FsIndexDescription getFsIndexDescriptionFromTableTreeItem(TableTreeItem item) {
    return (FsIndexDescription) item.getData();
  }

  public String[][] getCapabilitySofaNames() {
    Set[] inOut = getCapabilitySofaNames(editor.getAeDescription(), null);

    String[] inputSofas = (String[]) inOut[0].toArray(stringArray0);
    String[] outputSofas = (String[]) inOut[1].toArray(stringArray0);
    Arrays.sort(inputSofas);
    Arrays.sort(outputSofas);

    return new String[][] { inputSofas, outputSofas };
  }

  public static Capability[] getCapabilities(ResourceSpecifier rs) {
    if (rs instanceof ResourceCreationSpecifier)
      return ((ProcessingResourceMetaData) ((ResourceCreationSpecifier) rs).getMetaData())
              .getCapabilities();
    return null;
  }

  protected static Set[] getCapabilitySofaNames(ResourceCreationSpecifier rs, String componentKey) {
    Capability[] cs = getCapabilities(rs);
    Set inputSofasSet = new TreeSet();
    Set outputSofasSet = new TreeSet();
    for (int i = 0; i < cs.length; i++) {
      Capability c = cs[i];
      mergeSofaNames(inputSofasSet, c.getInputSofas(), componentKey);
      mergeSofaNames(outputSofasSet, c.getOutputSofas(), componentKey);
    }
    return new Set[] { inputSofasSet, outputSofasSet };
  }

  private static void mergeSofaNames(Set set, String[] items, String componentKey) {
    if (null != items) {
      for (int i = 0; i < items.length; i++) {
        if (null != componentKey)
          set.add(componentKey + '/' + items[i]);
        else
          set.add(items[i]);
      }
    } else if (null != componentKey)
      set.add(componentKey);
  }

  protected Capability addCapabilitySet() {
    Capability newCset = UIMAFramework.getResourceSpecifierFactory().createCapability();
    // update the model
    AnalysisEngineMetaData md = getAnalysisEngineMetaData();
    Capability[] c = getCapabilities();
    if (c == null)
      md.setCapabilities(new Capability[] { newCset });
    else {
      Capability[] newC = new Capability[c.length + 1];
      System.arraycopy(c, 0, newC, 0, c.length);
      newC[c.length] = newCset;
      md.setCapabilities(newC);
    }
    return newCset;
  }

  /**
   * Get the metadata for a local or remote descriptor. If the descriptor is remote, but cannot be
   * currently connected to, return null. Note that this make take some time to determine.
   * 
   * @param r
   *          is the AnalysisEngineDescription or the URISpecifier for remotes.
   * @return AnalysisEngineMetaData or null
   */
  public ResourceMetaData getMetaDataFromDescription(ResourceSpecifier o) {
    if (o instanceof ResourceCreationSpecifier) {
      return ((ResourceCreationSpecifier) o).getMetaData();
    }
    if (o instanceof URISpecifier) {
      URISpecifier uriSpec = ((URISpecifier) o);
      AnalysisEngine ae = null;
      try {
        setVnsHostAndPort(o);
        ae = UIMAFramework.produceAnalysisEngine(uriSpec);
      } catch (ResourceInitializationException e) {
        return null;
      }
      AnalysisEngineMetaData aemd = ae.getAnalysisEngineMetaData();
      ae.destroy();
      return aemd;
    }
    
    throw new InternalErrorCDE("invalid call");
  }

  public static void setVnsHostAndPort(String vnsHost, String vnsPort) {
    MultiPageEditorContributor.setVnsHost(vnsHost);
    MultiPageEditorContributor.setVnsPort(vnsPort);
  }

  public static void setVnsHostAndPort(Object descriptor) {
    String vnsHost = MultiPageEditorContributor.getCDEVnsHost();
    String vnsPort = MultiPageEditorContributor.getCDEVnsPort();
    // don't need this next part - the framework does it itself
    // if (null != descriptor) {
    // if (descriptor instanceof URISpecifier) {
    // URISpecifier rd = (URISpecifier) descriptor;
    // Parameter[] parms = rd.getParameters();
    // if (null != parms) {
    // for (int i = 0; i < parms.length; i++) {
    // if ("VNS_HOST".equals(parms[i].getName()))
    // vnsHost = parms[i].getValue();
    // else if ("VNS_PORT".equals(parms[i].getName()))
    // vnsPort = parms[i].getValue();
    // }
    // }
    // }
    // }
    setVnsHostAndPort(vnsHost, vnsPort);
  }

  protected void requestPopUpOverImport(Import importItem, Control control, Event event) {
    String path = editor.getAbsolutePathFromImport(importItem);
    IPath iPath = new Path(path);
    IFile[] files = editor.getProject().getWorkspace().getRoot().findFilesForLocation(iPath);
    if (null == files || files.length != 1)
      return;

    String filePathName = files[0].getLocation().toOSString();
    XMLizable inputDescription;
    try {
      inputDescription = parseDescriptor(new XMLInputSource(filePathName));
    } catch (InvalidXMLException e) {
      return;
    } catch (IOException e) {
      return;
    }

    PopupList popupList = new PopupList(control.getShell());
    String[] items = { "Open in new window..." };
    popupList.setItems(items);
    int HACK_MARGIN = 30;
    Point absPoint = getAbsoluteLocation(control, event.x, event.y + HACK_MARGIN);
    Rectangle rect = new Rectangle(absPoint.x, absPoint.y, 150, 25);
    control.setToolTipText("");
    String res = popupList.open(rect);

    // code to open selected file, by location or by name
    if (null != res) {
      if ((inputDescription instanceof URISpecifier) ||
          isJmsDescriptor(inputDescription)) {
        editor.openTextEditor(path);
      }
      else
        editor.open(path);
    }
  }
  
  protected boolean isJmsDescriptor (XMLizable inputDescription) {
    return (inputDescription instanceof CustomResourceSpecifier) &&
    ("org.apache.uima.aae.jms_adapter.JmsAnalysisEngineServiceAdapter".equals(
        ((CustomResourceSpecifier)inputDescription).getResourceClassName()));
  }

  private Point getAbsoluteLocation(Control control, int x, int y) {
    Point point = new Point(x, y);
    Composite composite = control.getParent();
    while (composite != null) {
      point.x += composite.getLocation().x;
      point.y += composite.getLocation().y;
      composite = composite.getParent();
    }
    return point;
  }

  public static String convertNull(String s) {
    if (null == s)
      return "";
    return s;
  }

  public Import createImport(String fileName, boolean isByName) {
    if (isByName) {
      return createByNameImport(fileName);
    } else
      try {
        return createLocationImport(fileName);
      } catch (MalformedURLException e1) {
        throw new InternalErrorCDE("unhandled exception", e1);
      }
  }

  /**
   * @param location
   * @return
   * @throws MalformedURLException
   */
  public Import createLocationImport(String location) throws MalformedURLException {

    String sDescriptorRelativePath = editor.getDescriptorRelativePath(location);
    // If relative path is not "relative", on Windows might get back
    // an absolute path starting with C: or something like it.
    // If a path starts with "C:", it must be preceeded by
    // file:/ so the C: is not interpreted as a "scheme".
    if (sDescriptorRelativePath.indexOf("file:/") == -1 //$NON-NLS-1$
            && sDescriptorRelativePath.indexOf(":/") > -1) { //$NON-NLS-1$
      sDescriptorRelativePath = "file:/" + sDescriptorRelativePath; //$NON-NLS-1$
    }

    Import imp = new Import_impl();
    // fails on unix? URL url = new URL("file:/" + getDescriptorDirectory());
    // Set relative Path Base
    // a version that might work on all platforms
    URL url = new File(editor.getDescriptorDirectory()).toURL();
    ((Import_impl) imp).setSourceUrl(url);

    imp.setLocation(sDescriptorRelativePath);
    return imp;
  }

  public Import createByNameImport(String fileName) {
    if (fileName.endsWith(".xml"))
      fileName = fileName.substring(0, fileName.length() - 4);
    fileName = fileName.replace('\\', '/');
    fileName = fileName.replace('/', '.');
    int i = fileName.indexOf(":");
    if (i >= 0)
      fileName = fileName.substring(i + 1);
    if (fileName.charAt(0) == '.')
      fileName = fileName.substring(1);
    int partStart = 0;

    Import imp = UIMAFramework.getResourceSpecifierFactory().createImport();
    ResourceManager rm = editor.createResourceManager();

    for (;;) {
      imp.setName(fileName.substring(partStart));
      try {
        imp.findAbsoluteUrl(rm);
      } catch (InvalidXMLException e) {
        partStart = fileName.indexOf('.', partStart) + 1;
        if (0 == partStart)
          return imp; // not found -outer code will catch error later
        continue;
      }
      return imp;
    }
  }

  // subtype of FSLists should not match
  public static boolean isFSArrayOrListType(String type) {
    return (null != type)
            && (type.equals(CAS.TYPE_NAME_FS_ARRAY) || type.equals(CAS.TYPE_NAME_FS_LIST));
  }

  public static boolean isArrayOrListType(String type) {
    return (null != type)
            && (type.equals(CAS.TYPE_NAME_FS_ARRAY) || type.equals(CAS.TYPE_NAME_FS_LIST)
                    || type.equals(CAS.TYPE_NAME_STRING_LIST)
                    || type.equals(CAS.TYPE_NAME_FLOAT_LIST)
                    || type.equals(CAS.TYPE_NAME_INTEGER_LIST)
                    || type.equals(CAS.TYPE_NAME_STRING_ARRAY)
                    || type.equals(CAS.TYPE_NAME_FLOAT_ARRAY)

                    || type.equals(CAS.TYPE_NAME_BOOLEAN_ARRAY)
                    || type.equals(CAS.TYPE_NAME_BYTE_ARRAY)
                    || type.equals(CAS.TYPE_NAME_SHORT_ARRAY)
                    || type.equals(CAS.TYPE_NAME_INTEGER_ARRAY)
                    || type.equals(CAS.TYPE_NAME_LONG_ARRAY) || type
                    .equals(CAS.TYPE_NAME_DOUBLE_ARRAY));
  }

  private final static int RIDICULOUSLY_LARGE = 10000;

  /**
   * Produce Unique key for a newly added descriptor file
   * 
   * @param fileName
   * @return
   */
  protected String produceUniqueComponentKey(String fileName) {
    // get existing set of delegates from model, with imports
    Set existingKeyNames = new HashSet(getDelegateAnalysisEngineSpecifiersWithImports().keySet());
    FlowControllerDeclaration fcd = getFlowControllerDeclaration();
    if (null != fcd && null != fcd.getKey() && !"".equals(fcd.getKey()))
      existingKeyNames.add(fcd.getKey());
    String keyName = fileName;
    String keyNameLowerCase = keyName.toLowerCase();
    keyName = keyName.substring(0, keyNameLowerCase.indexOf(".xml"));
    if (!existingKeyNames.contains(keyName)) {
      return keyName;
    }

    for (int i = 2; i < RIDICULOUSLY_LARGE; i++) {
      String sKeyName = keyName + i;
      if (!existingKeyNames.contains(sKeyName)) {
        return sKeyName;
      }
    }
    Utility.popMessage("Failed to create unique key", "The Flow Controller name, '" + fileName
            + "', could not be "
            + "converted to a unique key name -- tried with 10000 different suffixes",
            MessageDialog.ERROR);
    return null;
  }

  private static URL urlForResourceSpecifierSchema;
  static {
    try {
      urlForResourceSpecifierSchema = new URL("file:resourceSpecifierSchema.xsd");
    } catch (MalformedURLException e) {
      urlForResourceSpecifierSchema = null;
    }
  }

  public static XMLizable parseDescriptor(XMLInputSource input) throws InvalidXMLException {
    return parseDescriptor(input, false);
  }
  
  public static XMLizable parseDescriptor(XMLInputSource input, boolean preserveComments) throws InvalidXMLException {
    // turn off environment variable expansion
    XMLParser.ParsingOptions parsingOptions = new XMLParser.ParsingOptions(false);
    parsingOptions.preserveComments = preserveComments;
    XMLParser parser = UIMAFramework.getXMLParser();
    // disabled - error messages from XML validation not very helpful
    // parser.enableSchemaValidation(true);
    return parser.parse(input, "http://uima.apache.org/resourceSpecifier",
            urlForResourceSpecifierSchema, parsingOptions);
  }

  protected void showExceptionReadingImportedDescriptor(Exception e) {
    StringBuffer msg = new StringBuffer(1000);
    msg
            .append("There was an exception raised while reading and parsing an imported descriptor. "
                    + "If this is a ''not found'' message for a remote descriptor imported by name, insure that the class path or data path includes an entry where this file should be found.\n");
    msg.append(editor.getMessagesToRootCause(e));
    Utility.popMessage("Exception reading Imported File", msg.toString(), MessageDialog.ERROR);
  }

  /**
   * Update the model while checking for validity If invalid - ask if want to continue or not
   * 
   * @return
   */
  protected boolean isValidAggregateChange() {

    // doing this check here is expensive, but gives the best error location information
    if (!editor.isValidAE(editor.getAeDescription()))
      if (revertOrContinue("Continue or Abort",
              "Because of errors in validating the resulting Analysis Engine:\n"))
        return false; // want to revert

    try {
      editor.setMergedTypeSystemDescription();
    } catch (ResourceInitializationException e) {
      // no error here - continue if possible
    }

    try {
      editor.setResolvedExternalResourcesAndBindings();
    } catch (InvalidXMLException e3) {
      // no error here - continue if possible
    }
    try {
      editor.setResolvedFlowControllerDeclaration();
    } catch (InvalidXMLException e3) {
      // no error here - continue if possible
    }
    try {
      editor.setMergedFsIndexCollection();
    } catch (ResourceInitializationException e1) {
      // no error here - continue if possible
    }
    try {
      editor.setMergedTypePriorities();
    } catch (ResourceInitializationException e2) {
      // no error here - continue if possible
    }
    return true;
  }

  protected void finishAggregateChangeAction() {

    editor.setFileDirty();
    editor.getTypePage().markStale();
    editor.getIndexesPage().markStale();
    editor.getCapabilityPage().markStale();
    SectionPart s = editor.getParameterPage().getParameterDelegatesSection();
    if (null != s)
      s.markStale();
    editor.getResourcesPage().markStale();
  }

  protected XMLizable readImport(Import imp, String fileName, boolean isImportByName) {
    URL byNameURL;
    XMLInputSource input;
    if (isImportByName) {
      try {
        byNameURL = imp.findAbsoluteUrl(editor.createResourceManager());
      } catch (InvalidXMLException e) {
        showExceptionReadingImportedDescriptor(e);
        return null;
      }

      try {
        input = new XMLInputSource(byNameURL.openStream(), new File(byNameURL.getFile())
                .getParentFile());
      } catch (IOException e) {
        showExceptionReadingImportedDescriptor(e);
        return null;
      }
    } else {
      try {
        input = new XMLInputSource(new File(fileName));
      } catch (IOException e) {
        throw new InternalErrorCDE("invalid state");
      }
    }
    // read the content and merge into our model
    XMLizable inputDescription;
    try {
      inputDescription = parseDescriptor(input);
    } catch (InvalidXMLException e1) {
      showExceptionReadingImportedDescriptor(e1);
      return null;
    }
    return inputDescription;
  }

  protected static void enableCtrl(Control c, boolean enabled) {
    if (null != c)
      c.setEnabled(enabled);
  }

  protected static void setButtonSelection(Button c, boolean selected) {
    if (null != c)
      c.setSelection(selected);
  }

  /**
   * Return a String made from the description of a given resource specifier. If the specifier is
   * for a remote, try and connect to the remote and get its info.
   * 
   * @param fileRef
   * @param rs
   */
  private ResourceSpecifier lastResourceForDescription = null;

  private String lastDescriptionFromDescriptor = "";

  private long lastTimeDescriptionRequested = 0;

  private static final long TABLE_HOVER_REQUERY_TIME = 15000;

  protected String getDescriptionForDescriptor(String fileRef, ResourceSpecifier rs) {
    if (null == fileRef || "".equals(fileRef) || null == rs)
      return "";
    String sDesc;
    long lCurrentTimeInMillis = System.currentTimeMillis();
    if (rs == lastResourceForDescription
            && ((lCurrentTimeInMillis - lastTimeDescriptionRequested) < TABLE_HOVER_REQUERY_TIME)) {
      return lastDescriptionFromDescriptor;
    } else {
      sDesc = fileRef + ":\n";
      if (rs instanceof PearSpecifier) {
        sDesc += " (Pear descriptor)";
      } else {
        ResourceMetaData resourceMetaData = getMetaDataFromDescription(rs);
        if (null == resourceMetaData) {
          sDesc += "(Remote service is not responding)";
        } else {
          String description = resourceMetaData.getDescription();
          if (null != description && !description.equals("")) {
            sDesc += parseToFitInToolTips(description);
          } else
            sDesc += "(No Description)";
        }
      }
      lastResourceForDescription = rs;
      lastTimeDescriptionRequested = System.currentTimeMillis();
      lastDescriptionFromDescriptor = sDesc;
    }
    return sDesc;
  }

  protected PrintWriter setupToPrintFile(String filePath) {
    if (new File(filePath).exists())
      if (Window.CANCEL == Utility.popOkCancel("File exists, OK to replace?", MessageFormat.format(
              "The file ''{0}'' exists. Press OK if it can be replaced; otherwise press Cancel.",
              new Object[] { filePath }), MessageDialog.WARNING))
        return null;
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filePath);
      return new PrintWriter(fos);
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      return null;
    }
  }

  protected FsIndexCollection getFsIndexCollection() {
    FsIndexCollection fsic = getAnalysisEngineMetaData().getFsIndexCollection();
    if (null == fsic)
      getAnalysisEngineMetaData().setFsIndexCollection(
              fsic = UIMAFramework.getResourceSpecifierFactory().createFsIndexCollection());
    return fsic;
  }

  public static String handleDefaultIndexKind(String indexKind) {
    if (null == indexKind)
      return "sorted";
    return indexKind;
  }
}
