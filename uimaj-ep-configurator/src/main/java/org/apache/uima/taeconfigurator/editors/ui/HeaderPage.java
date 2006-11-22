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
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
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
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Common part of most pages
 */
public abstract class HeaderPage extends FormPage {

  final public static boolean EQUAL_WIDTH = true;

  protected SashForm sashForm;

  protected MultiPageEditor editor;

  protected FormToolkit toolkit;

  protected Composite leftPanel;

  protected Composite rightPanel;

  protected int sashFormWidth;

  protected float leftPanelPercent;

  protected float rightPanelPercent;

  protected int leftPanelWidth;

  protected int rightPanelWidth;

  public HeaderPage(MultiPageEditor formEditor, String id, String keyPageTitle) {
    super(formEditor, id, keyPageTitle);
    editor = formEditor;
    toolkit = editor.getToolkit();
  }

  public HeaderPage(MultiPageEditor formEditor, String pageTitle) {
    this(formEditor, "UID_" + pageTitle, pageTitle);
  }

  protected void maybeInitialize(IManagedForm managedForm) {
    if (TAEConfiguratorPlugin.is30version)
      ((ManagedForm) managedForm).initialize();
  }

  /*
   * General methods to create sub areas on pages
   */

  public Composite newComposite(Composite parent) {
    return newnColumnSection(parent, 1);
  }

  // this composite spans col 1 & 2, out of a 4 column (0, 1, 2, 3) grid layout
  public Composite newCentered2SpanComposite(Composite parent) {
    spacer(parent); // skip over col 0
    Composite composite = newComposite(parent);
    ((GridData) composite.getLayoutData()).horizontalSpan = 2;
    return composite;
  }

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

  public Composite setup1ColumnLayout(IManagedForm managedForm) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    form.setLayout(new GridLayout(1, false)); // this is required !
    Composite xtra = toolkit.createComposite(form);
    xtra.setLayout(new GridLayout(1, false));
    xtra.setLayoutData(new GridData(GridData.FILL_BOTH));

    Control c = form.getParent();
    while (!(c instanceof ScrolledComposite))
      c = c.getParent();
    ((GridData) xtra.getLayoutData()).widthHint = c.getSize().x;
    return xtra;
  }

  // this method creates no new composites.
  public Composite setup2ColumnGrid(IManagedForm managedForm, boolean equalWidth) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    GridLayout layout = new GridLayout(2, equalWidth);
    form.setLayout(layout);
    form.setLayoutData(new GridData(GridData.FILL_BOTH));
    return form;
  }

  public Form2Panel setup2ColumnLayout(IManagedForm managedForm, int w1, int w2) {
    final ScrolledForm sform = managedForm.getForm();
    final Composite form = sform.getBody();
    form.setLayout(new GridLayout(1, false)); // this is required !
    Composite xtra = toolkit.createComposite(form);
    xtra.setLayout(new GridLayout(1, false));
    xtra.setLayoutData(new GridData(GridData.FILL_BOTH));
    Control c = xtra.getParent();
    while (!(c instanceof ScrolledComposite))
      c = c.getParent();
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
      public void controlResized(ControlEvent e) {
        setSashFormWidths();
      }
    });

    return new Form2Panel(form, leftPanel, rightPanel);
  }

  void setSashFormWidths() {

    sashFormWidth = sashForm.getSize().x;
    int[] ws = sashForm.getWeights();

    leftPanelWidth = (int) (((float) ws[0] / (float) (ws[0] + ws[1])) * sashFormWidth);
    rightPanelWidth = (int) (((float) ws[1] / (float) (ws[0] + ws[1])) * sashFormWidth);
  }

  /**
   * Sash impl
   * 
   * @param managedForm
   * @param equalWidth
   * @return
   */
  public Form2Panel setup2ColumnLayout(IManagedForm managedForm, boolean equalWidth) {
    if (equalWidth)
      return setup2ColumnLayout(managedForm, 50, 50);
    else
      // a hack - based on first column more likely being wider
      return setup2ColumnLayout(managedForm, 60, 40);
  }

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
      public void handleEvent(Event event) {
        float col1CurrentWidth = leftPanel.getSize().x;
        float col2CurrentWidth = rightPanel.getSize().x;
        final int minLeftPanelWidth = 250; // in pels
        final int minRightPanelWidth = (int) (col2CurrentWidth * minLeftPanelWidth / col1CurrentWidth);
        ((GridData) leftPanel.getLayoutData()).widthHint = minLeftPanelWidth;
        ((GridData) rightPanel.getLayoutData()).widthHint = minRightPanelWidth;
        sform.removeListener(SWT.Resize, this); // only do this one time
      }
    });
    return new Form2Panel(form, leftPanel, rightPanel);
  }

  public Form2Panel setup2ColumnLayoutOn4Grid(IManagedForm managedForm, boolean equalWidth) {
    Form2Panel f = setup2ColumnLayoutNotSash(managedForm, equalWidth);
    ((GridLayout) f.form.getLayout()).numColumns = 4;
    ((GridData) f.left.getLayoutData()).horizontalSpan = 2;
    ((GridData) f.right.getLayoutData()).horizontalSpan = 2;
    return f;
  }

  protected void spacer(Composite container) {
    toolkit.createLabel(container, " ");
  }

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

  protected boolean isPrimitive() {
    return editor.getAeDescription().isPrimitive();
  }

  protected boolean isAeDescriptor() {
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

  public boolean isLocalProcessingDescriptor() {
    return editor.isLocalProcessingDescriptor();
  }

  protected AnalysisEngineMetaData getAnalysisEngineMetaData() {
    return editor.getAeDescription().getAnalysisEngineMetaData();
  }

  // **************************************************
  // * Common Actions in Handlers
  // **************************************************

}
