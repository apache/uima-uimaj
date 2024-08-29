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

import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.taeconfigurator.editors.Form2Panel;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Common part of most pages.
 */
public abstract class HeaderPage extends FormPage {

  /** The Constant EQUAL_WIDTH. */
  public static final boolean EQUAL_WIDTH = true;

  /** The sash form. */
  protected SashForm sashForm;

  /** The editor. */
  protected MultiPageEditor editor;

  /** The toolkit. */
  protected FormToolkit toolkit;

  /** The left panel. */
  protected Composite leftPanel;

  /** The right panel. */
  protected Composite rightPanel;

  /** The sash form width. */
  protected int sashFormWidth;

  /** The left panel percent. */
  protected float leftPanelPercent;

  /** The right panel percent. */
  protected float rightPanelPercent;

  /** The left panel width. */
  protected int leftPanelWidth;

  /** The right panel width. */
  protected int rightPanelWidth;

  /**
   * Instantiates a new header page.
   *
   * @param formEditor
   *          the form editor
   * @param id
   *          the id
   * @param keyPageTitle
   *          the key page title
   */
  public HeaderPage(MultiPageEditor formEditor, String id, String keyPageTitle) {
    super(formEditor, id, keyPageTitle);
    editor = formEditor;
    toolkit = editor.getToolkit();
  }

  /**
   * Instantiates a new header page.
   *
   * @param formEditor
   *          the form editor
   * @param pageTitle
   *          the page title
   */
  public HeaderPage(MultiPageEditor formEditor, String pageTitle) {
    this(formEditor, "UID_" + pageTitle, pageTitle);
  }

  /**
   * Maybe initialize.
   *
   * @param managedForm
   *          the managed form
   */
  protected void maybeInitialize(IManagedForm managedForm) {
    // if (TAEConfiguratorPlugin.is30version)
    // ((ManagedForm) managedForm).initialize();
  }

  /*
   * General methods to create sub areas on pages
   */

  /**
   * New composite.
   *
   * @param parent
   *          the parent
   * @return the composite
   */
  public Composite newComposite(Composite parent) {
    return newnColumnSection(parent, 1);
  }

  /**
   * New centered 2 span composite.
   *
   * @param parent
   *          the parent
   * @return the composite
   */
  // this composite spans col 1 & 2, out of a 4 column (0, 1, 2, 3) grid layout
  public Composite newCentered2SpanComposite(Composite parent) {
    spacer(parent); // skip over col 0
    Composite composite = newComposite(parent);
    ((GridData) composite.getLayoutData()).horizontalSpan = 2;
    return composite;
  }

  /**
   * Newn column section.
   *
   * @param parent
   *          the parent
   * @param cols
   *          the cols
   * @return the composite
   */
  public Composite newnColumnSection(Composite parent, int cols) {
    Composite section = toolkit.createComposite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = cols;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    section.setLayout(layout);
    section.setLayoutData(new GridData(GridData.FILL_BOTH));
    return section;
  }

  /**
   * Setup 1 column layout.
   *
   * @param managedForm
   *          the managed form
   * @return the composite
   */
  public Composite setup1ColumnLayout(IManagedForm managedForm) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    form.setLayout(new GridLayout(1, false)); // this is required !
    Composite xtra = toolkit.createComposite(form);
    xtra.setLayout(new GridLayout(1, false));
    xtra.setLayoutData(new GridData(GridData.FILL_BOTH));

    Control c = form.getParent();
    while (!(c instanceof ScrolledComposite)) {
      c = c.getParent();
    }
    ((GridData) xtra.getLayoutData()).widthHint = c.getSize().x;
    return xtra;
  }

  /**
   * Setup 2 column grid.
   *
   * @param managedForm
   *          the managed form
   * @param equalWidth
   *          the equal width
   * @return the composite
   */
  // this method creates no new composites.
  public Composite setup2ColumnGrid(IManagedForm managedForm, boolean equalWidth) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    GridLayout layout = new GridLayout(2, equalWidth);
    form.setLayout(layout);
    form.setLayoutData(new GridData(GridData.FILL_BOTH));
    return form;
  }

  /**
   * Setup 2 column layout.
   *
   * @param managedForm
   *          the managed form
   * @param w1
   *          the w 1
   * @param w2
   *          the w 2
   * @return the form 2 panel
   */
  public Form2Panel setup2ColumnLayout(IManagedForm managedForm, int w1, int w2) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    form.setLayout(new GridLayout(1, false)); // this is required !
    Composite xtra = toolkit.createComposite(form);
    xtra.setLayout(new GridLayout(1, false));
    xtra.setLayoutData(new GridData(GridData.FILL_BOTH));
    Control c = xtra.getParent();
    while (!(c instanceof ScrolledComposite)) {
      c = c.getParent();
    }
    ((GridData) xtra.getLayoutData()).widthHint = c.getSize().x;
    ((GridData) xtra.getLayoutData()).heightHint = c.getSize().y;
    sashForm = new SashForm(xtra, SWT.HORIZONTAL);

    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH)); // needed

    leftPanel = newComposite(sashForm);
    ((GridLayout) leftPanel.getLayout()).marginHeight = 5;
    ((GridLayout) leftPanel.getLayout()).marginWidth = 5;
    rightPanel = newComposite(sashForm);
    ((GridLayout) rightPanel.getLayout()).marginHeight = 5;
    ((GridLayout) rightPanel.getLayout()).marginWidth = 5;
    sashForm.setWeights(new int[] { w1, w2 });
    leftPanelPercent = (float) w1 / (float) (w1 + w2);
    rightPanelPercent = (float) w2 / (float) (w1 + w2);

    rightPanel.addControlListener(new ControlAdapter() {
      @Override

      public void controlResized(ControlEvent e) {
        setSashFormWidths();
      }
    });

    return new Form2Panel(form, leftPanel, rightPanel);
  }

  /**
   * Sets the sash form widths.
   */
  void setSashFormWidths() {

    sashFormWidth = sashForm.getSize().x;
    int[] ws = sashForm.getWeights();

    leftPanelWidth = (int) (((float) ws[0] / (float) (ws[0] + ws[1])) * sashFormWidth);
    rightPanelWidth = (int) (((float) ws[1] / (float) (ws[0] + ws[1])) * sashFormWidth);
  }

  /**
   * Sash impl.
   *
   * @param managedForm
   *          the managed form
   * @param equalWidth
   *          the equal width
   * @return the form 2 panel
   */
  public Form2Panel setup2ColumnLayout(IManagedForm managedForm, boolean equalWidth) {
    if (equalWidth) {
      return setup2ColumnLayout(managedForm, 50, 50);
    } else {
      // a hack - based on first column more likely being wider
      return setup2ColumnLayout(managedForm, 60, 40);
    }
  }

  /**
   * Setup 2 column layout not sash.
   *
   * @param managedForm
   *          the managed form
   * @param equalWidth
   *          the equal width
   * @return the form 2 panel
   */
  public Form2Panel setup2ColumnLayoutNotSash(IManagedForm managedForm, boolean equalWidth) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    GridLayout layout = new GridLayout(2, equalWidth);
    form.setLayout(layout);
    form.setLayoutData(new GridData(GridData.FILL_BOTH));
    leftPanel = newComposite(form);
    rightPanel = newComposite(form);
    // strategy for setting size hints of right and left panels
    // Why set hints? Because they make the inner containing things
    // scroll horiz. if they're too wide (useful for aggregates having
    // long names).
    // What hint to set? The hint should be the smallest size you want
    // with child scrolling, before the tabbed page container itself
    // gets a scroll bar.
    // When in the life cycle to do this? Only need to do it once, but
    // after the Grid Layout has happened. This can be the first resizing
    // event (learned this by debugging)

    sform.addListener(SWT.Resize, new Listener() {
      @Override
      public void handleEvent(Event event) {
        float col1CurrentWidth = leftPanel.getSize().x;
        float col2CurrentWidth = rightPanel.getSize().x;
        final int minLeftPanelWidth = 250; // in pels
        final int minRightPanelWidth = (int) (col2CurrentWidth * minLeftPanelWidth
                / col1CurrentWidth);
        ((GridData) leftPanel.getLayoutData()).widthHint = minLeftPanelWidth;
        ((GridData) rightPanel.getLayoutData()).widthHint = minRightPanelWidth;
        sform.removeListener(SWT.Resize, this); // only do this one time
      }
    });
    return new Form2Panel(form, leftPanel, rightPanel);
  }

  /**
   * Setup 2 column layout on 4 grid.
   *
   * @param managedForm
   *          the managed form
   * @param equalWidth
   *          the equal width
   * @return the form 2 panel
   */
  public Form2Panel setup2ColumnLayoutOn4Grid(IManagedForm managedForm, boolean equalWidth) {
    Form2Panel f = setup2ColumnLayoutNotSash(managedForm, equalWidth);
    ((GridLayout) f.form.getLayout()).numColumns = 4;
    ((GridData) f.left.getLayoutData()).horizontalSpan = 2;
    ((GridData) f.right.getLayoutData()).horizontalSpan = 2;
    return f;
  }

  /**
   * Spacer.
   *
   * @param container
   *          the container
   */
  protected void spacer(Composite container) {
    toolkit.createLabel(container, " ");
  }

  /**
   * Mark stale.
   */
  public void markStale() {
    if (getManagedForm() != null) {
      IFormPart[] parts = getManagedForm().getParts();
      for (int i = 0; i < parts.length; i++) {
        ((AbstractFormPart) parts[i]).markStale();
      }
    }
  }

  // **************************************************
  // * Model Access
  // **************************************************

  /**
   * Checks if is primitive.
   *
   * @return true, if is primitive
   */
  protected boolean isPrimitive() {
    return editor.getAeDescription().isPrimitive();
  }

  /**
   * Checks if is ae descriptor.
   *
   * @return true, if is ae descriptor
   */
  protected boolean isAeDescriptor() {
    return editor.isAeDescriptor();
  }

  /**
   * Checks if is type system descriptor.
   *
   * @return true, if is type system descriptor
   */
  public boolean isTypeSystemDescriptor() {
    return editor.isTypeSystemDescriptor();
  }

  /**
   * Checks if is index descriptor.
   *
   * @return true, if is index descriptor
   */
  public boolean isIndexDescriptor() {
    return editor.isFsIndexCollection();
  }

  /**
   * Checks if is type priority descriptor.
   *
   * @return true, if is type priority descriptor
   */
  public boolean isTypePriorityDescriptor() {
    return editor.isTypePriorityDescriptor();
  }

  /**
   * Checks if is ext res and bindings descriptor.
   *
   * @return true, if is ext res and bindings descriptor
   */
  public boolean isExtResAndBindingsDescriptor() {
    return editor.isExtResAndBindingsDescriptor();
  }

  /**
   * Checks if is collection reader descriptor.
   *
   * @return true, if is collection reader descriptor
   */
  public boolean isCollectionReaderDescriptor() {
    return editor.isCollectionReaderDescriptor();
  }

  /**
   * Checks if is cas initializer descriptor.
   *
   * @return true, if is cas initializer descriptor
   */
  public boolean isCasInitializerDescriptor() {
    return editor.isCasInitializerDescriptor();
  }

  /**
   * Checks if is cas consumer descriptor.
   *
   * @return true, if is cas consumer descriptor
   */
  public boolean isCasConsumerDescriptor() {
    return editor.isCasConsumerDescriptor();
  }

  /**
   * Checks if is local processing descriptor.
   *
   * @return true, if is local processing descriptor
   */
  public boolean isLocalProcessingDescriptor() {
    return editor.isLocalProcessingDescriptor();
  }

  /**
   * Gets the analysis engine meta data.
   *
   * @return the analysis engine meta data
   */
  protected AnalysisEngineMetaData getAnalysisEngineMetaData() {
    return editor.getAeDescription().getAnalysisEngineMetaData();
  }

  // **************************************************
  // * Common Actions in Handlers
  // **************************************************

}
