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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.apache.uima.taeconfigurator.StandardStrings;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.MultiPageEditorContributor;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The Class AbstractDialog.
 */
public abstract class AbstractDialog extends Dialog implements Listener, StandardStrings {

  /** The Constant contentAssistAvailable. */
  private static final boolean contentAssistAvailable;
  static {
    boolean contentAssistIsOK = false;
    try {
      Class.forName("org.eclipse.ui.fieldassist.ContentAssistField");
      contentAssistIsOK = true;
    } catch (ClassNotFoundException e) {
    }
    contentAssistAvailable = contentAssistIsOK;
  }

  /** The editor. */
  protected MultiPageEditor editor;

  /** The section. */
  protected AbstractSection section;

  /** The ok button. */
  protected Button okButton;

  /** The error message UI. */
  protected Label errorMessageUI;

  /** The title. */
  protected String title;

  /** The dialog description. */
  protected String dialogDescription;

  /**
   * Instantiates a new abstract dialog.
   *
   * @param section
   *          the section
   * @param title
   *          the title
   * @param description
   *          the description
   */
  protected AbstractDialog(AbstractSection section, String title, String description) {
    // maintainers: don't use new shell; see comment in Dialog class
    super(section.getSection().getShell());
    commonInit(section, title, description);
  }

  /**
   * Instantiates a new abstract dialog.
   *
   * @param shell
   *          the shell
   * @param title
   *          the title
   * @param description
   *          the description
   */
  protected AbstractDialog(Shell shell, String title, String description) {
    super(shell);
    commonInit(null, title, description);
  }

  /**
   * Common init.
   *
   * @param aSection
   *          the a section
   * @param aTitle
   *          the a title
   * @param aDescription
   *          the a description
   */
  private void commonInit(AbstractSection aSection, String aTitle, String aDescription) {
    section = aSection;
    editor = (null == section) ? null : section.editor;
    setShellStyle(getShellStyle() | SWT.RESIZE);
    title = aTitle;
    dialogDescription = aDescription;
  }

  /**
   * Sets the title.
   *
   * @param title
   *          the new title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the message.
   *
   * @param msg
   *          the new message
   */
  public void setMessage(String msg) {
    this.dialogDescription = msg;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);
    createWideLabel(composite, dialogDescription);
    return composite;
  }

  /**
   * Creates the dialog area.
   *
   * @param parent
   *          the parent
   * @param existing
   *          the existing
   * @return the control
   */
  protected Control createDialogArea(Composite parent, Object existing) {
    Composite composite = (Composite) super.createDialogArea(parent);
    createWideLabel(composite, dialogDescription);
    if (null != existing)
      getShell().setText(getShell().getText().replaceFirst("Add", "Edit"));
    return composite;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  /**
   * Creates the wide label.
   *
   * @param parent
   *          the parent
   * @param message
   *          the message
   * @return the label
   */
  protected Label createWideLabel(Composite parent, String message) {
    Label label = null;

    label = new Label(parent, SWT.WRAP);
    label.setText(null != message ? message : "");
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    label.setLayoutData(data);
    return label;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    okButton = getButton(IDialogConstants.OK_ID);
    okButton.setEnabled(false);
  }

  /** The Constant stringArray0. */
  public static final String[] stringArray0 = new String[0];

  /**
   * Sets the text and tip.
   *
   * @param c
   *          the c
   * @param label
   *          the label
   * @param tip
   *          the tip
   */
  protected void setTextAndTip(Button c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  /**
   * Sets the text and tip.
   *
   * @param c
   *          the c
   * @param label
   *          the label
   * @param tip
   *          the tip
   */
  protected void setTextAndTip(CCombo c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  /**
   * Sets the text and tip.
   *
   * @param c
   *          the c
   * @param label
   *          the label
   * @param tip
   *          the tip
   */
  protected void setTextAndTip(Label c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  /**
   * Sets the text and tip.
   *
   * @param c
   *          the c
   * @param label
   *          the label
   * @param tip
   *          the tip
   * @param horizStyle
   *          the horiz style
   * @param horizGrab
   *          the horiz grab
   */
  protected void setTextAndTip(Label c, String label, String tip, int horizStyle,
          boolean horizGrab) {
    setTextAndTip(c, label, tip);
    c.setLayoutData(new GridData(horizStyle, SWT.BEGINNING, horizGrab, false));
  }

  /**
   * New 2 column composite.
   *
   * @param parent
   *          the parent
   * @return the composite
   */
  protected Composite new2ColumnComposite(Composite parent) {
    Composite twoCol = new Composite(parent, SWT.NONE);
    twoCol.setLayout(new GridLayout(2, false)); // false = not equal width
    ((GridLayout) twoCol.getLayout()).marginHeight = 0;
    ((GridLayout) twoCol.getLayout()).marginWidth = 0;
    twoCol.setLayoutData(new GridData(GridData.FILL_BOTH));
    return twoCol;
  }

  /**
   * New description.
   *
   * @param twoCol
   *          the two col
   * @param tip
   *          the tip
   * @return the text
   */
  protected Text newDescription(Composite twoCol, String tip) {
    setTextAndTip(new Label(twoCol, SWT.NONE), S_DESCRIPTION, tip);
    Text t = new Text(twoCol, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    t.setLayoutData(new GridData(GridData.FILL_BOTH));
    ((GridData) t.getLayoutData()).heightHint = 100;
    t.setToolTipText(tip);
    return t;
  }

  /**
   * New labeled C combo.
   *
   * @param parent
   *          the parent
   * @param label
   *          the label
   * @param tip
   *          the tip
   * @return the c combo
   */
  protected CCombo newLabeledCCombo(Composite parent, String label, String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newCCombo(parent, tip);
  }

  /**
   * New C combo.
   *
   * @param parent
   *          the parent
   * @param tip
   *          the tip
   * @return the c combo
   */
  protected CCombo newCCombo(Composite parent, String tip) {
    final CCombo cc = new CCombo(parent, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
    cc.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
    cc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // without these next lines, the height of the ccombo is too small
    // especially on a Mac
    FontData[] fontData = cc.getFont().getFontData();
    ((GridData) cc.getLayoutData()).heightHint = 2 * fontData[0].getHeight();
    cc.addListener(SWT.Selection, this);
    cc.setToolTipText(tip);
    cc.addKeyListener(new KeyListener() {
      private final StringBuffer b = new StringBuffer();

      @Override
      public void keyPressed(KeyEvent e) {
      }

      @Override
      public void keyReleased(KeyEvent e) {
        if (e.keyCode == SWT.BS) {
          if (b.length() > 0)
            b.deleteCharAt(b.length() - 1);
        } else if (Character.isJavaIdentifierPart(e.character) || e.character == '.')
          b.append(e.character);
        else
          return;
        final String[] ccItems = cc.getItems();
        final String partial = b.toString();
        int iBefore = -1;
        for (int i = 0; i < ccItems.length; i++) {
          if (ccItems[i].startsWith(partial)) {
            iBefore = i;
            break;
          }
        }
        if (iBefore >= 0)
          cc.setText(cc.getItem(iBefore));
      }
    });
    return cc;
  }

  /**
   * New button.
   *
   * @param parent
   *          the parent
   * @param style
   *          the style
   * @param name
   *          the name
   * @param tip
   *          the tip
   * @return the button
   */
  protected Button newButton(Composite parent, int style, String name, String tip) {
    Button b = new Button(parent, style);
    setTextAndTip(b, name, tip);
    b.addListener(SWT.Selection, this);
    return b;
  }

  /**
   * New text.
   *
   * @param parent
   *          the parent
   * @param style
   *          the style
   * @param tip
   *          the tip
   * @return the text
   */
  protected Text newText(Composite parent, int style, String tip) {
    Text t = new Text(parent, style | SWT.BORDER);
    t.setToolTipText(tip);
    t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    t.addListener(SWT.KeyUp, this);
    t.addListener(SWT.MouseUp, this); // for paste operation
    return t;
  }

  /**
   * New labeled text.
   *
   * @param parent
   *          the parent
   * @param style
   *          the style
   * @param label
   *          the label
   * @param tip
   *          the tip
   * @return the text
   */
  protected Text newLabeledText(Composite parent, int style, String label, String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newText(parent, style, tip);
  }

  /**
   * New tree.
   *
   * @param parent
   *          the parent
   * @param style
   *          the style
   * @return the tree
   */
  protected Tree newTree(Composite parent, int style) {
    Tree tree = new Tree(parent, style | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    tree.setLayoutData(gd);
    tree.addListener(SWT.Selection, this);
    tree.addListener(SWT.KeyUp, this); // delete key
    return tree;
  }

  /**
   * Styles = SWT.SINGLE / MULTI / CHECK / FULL_SELECTION / HIDE_SELECTION
   *
   * @param parent
   *          the parent
   * @param style
   *          the style
   * @return the new table widget
   */
  protected Table newTable(Composite parent, int style) {
    Table table = new Table(parent, style | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gd);
    table.addListener(SWT.Selection, this);
    table.addListener(SWT.KeyUp, this); // delete key
    return table;
  }

  /**
   * Gets the hit column.
   *
   * @param item
   *          the item
   * @param p
   *          the p
   * @return the hit column
   */
  public int getHitColumn(TreeItem item, Point p) {
    for (int i = item.getParent().getColumnCount() - 1; i >= 0; i--) {
      Rectangle columnBounds = item.getBounds(i);
      if (columnBounds.contains(p))
        return i;
    }
    return -1;
  }

  /**
   * New button container.
   *
   * @param parent
   *          the parent
   * @return the composite
   */
  public Composite newButtonContainer(Composite parent) {
    Composite buttonContainer = new Composite(parent, SWT.NONE);
    buttonContainer.setLayout(new GridLayout());
    buttonContainer.setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));
    return buttonContainer;
  }

  /**
   * New push button.
   *
   * @param parent
   *          the parent
   * @param label
   *          the label
   * @param tip
   *          the tip
   * @return the button
   */
  public Button newPushButton(Composite parent, String label, String tip) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    button.setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));
    // button.pack();
    button.setToolTipText(tip);
    button.addListener(SWT.Selection, this);
    return button;
  }

  /**
   * New error message.
   *
   * @param c
   *          the c
   */
  protected void newErrorMessage(Composite c) {
    newErrorMessage(c, 1);
  }

  /**
   * New error message.
   *
   * @param twoCol
   *          the two col
   * @param span
   *          the span
   */
  protected void newErrorMessage(Composite twoCol, int span) {
    Label m = new Label(twoCol, SWT.WRAP);
    m.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    ((GridData) m.getLayoutData()).horizontalSpan = span;
    ((GridData) m.getLayoutData()).widthHint = convertHorizontalDLUsToPixels(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    errorMessageUI = m;
  }

  /**
   * Sets the error message.
   *
   * @param msg
   *          the new error message
   */
  protected void setErrorMessage(String msg) {
    errorMessageUI.setText(msg);
    Composite shell = errorMessageUI.getParent();
    while (!(shell instanceof Shell))
      shell = shell.getParent();
    shell.setSize(shell.computeSize(-1, -1));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  // subclasses override, and often use super.handleEvent to call this
  @Override
  public void handleEvent(Event event) {
    if (okButton != null) // may be null if handler called from
                          // main area setText event, during construction
                          // because button bar is constructed after main area
      enableOK();
  }

  /**
   * Super button pressed.
   *
   * @param buttonId
   *          the button id
   */
  protected void superButtonPressed(int buttonId) {
    super.buttonPressed(buttonId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
   */
  // overridden where needed
  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      copyValuesFromGUI();
      if (!isValid())
        return; // keeps dialog open
    }
    super.buttonPressed(buttonId);
  }

  /**
   * Copy values from GUI.
   */
  public abstract void copyValuesFromGUI();

  /**
   * Checks if is valid.
   *
   * @return true, if is valid
   */
  public abstract boolean isValid();

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite c) {
    Control returnValue = super.createButtonBar(c);
    enableOK();
    return returnValue;
  }

  /**
   * Enable OK.
   */
  public abstract void enableOK();

  /**
   * Use qualified types.
   *
   * @return true, if successful
   */
  public boolean useQualifiedTypes() {
    return MultiPageEditorContributor.getUseQualifiedTypes();
  }

  /**
   * Gets the all types as sorted array.
   *
   * @return the all types as sorted array
   */
  protected String[] getAllTypesAsSortedArray() {
    String[] allTypes = (String[]) section.editor.allTypes.get().keySet().toArray(stringArray0);
    Arrays.sort(allTypes, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        String shortName1 = AbstractSection.getShortName((String) o1);
        String shortName2 = AbstractSection.getShortName((String) o2);
        if (!shortName1.equals(shortName2))
          return shortName1.compareTo(shortName2);

        String namespace1 = AbstractSection.getNameSpace((String) o1);
        String namespace2 = AbstractSection.getNameSpace((String) o2);
        return namespace1.compareTo(namespace2);
      }
    });
    return allTypes;
  }

  /**
   * New labeled type input.
   *
   * @param aSection
   *          the a section
   * @param parent
   *          the parent
   * @param label
   *          the label
   * @param tip
   *          the tip
   * @return the text
   */
  protected Text newLabeledTypeInput(AbstractSection aSection, Composite parent, String label,
          String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newTypeInput(aSection, parent);
  }

  /**
   * New type input.
   *
   * @param aSection
   *          the a section
   * @param twoCol
   *          the two col
   * @return the text
   */
  protected Text newTypeInput(AbstractSection aSection, Composite twoCol) {
    Composite tc = new2ColumnComposite(twoCol);
    final TypesWithNameSpaces candidatesToPickFrom = getTypeSystemInfoList(); // provide an
                                                                              // ArrayList of

    final Text text;
    if (contentAssistAvailable) {
      ContentAssistField32 caf = new ContentAssistField32(tc, candidatesToPickFrom);
      text = caf.getControl();
    } else {
      text = newText(tc, SWT.BORDER, "");
    }

    text.setToolTipText("Enter a Type name."
            + (contentAssistAvailable ? "Content Assist is available (press Ctrl + Space)" : ""));
    text.getParent().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    text.addListener(SWT.KeyUp, this);
    text.addListener(SWT.MouseUp, this); // for paste operation
    text.addListener(SWT.Modify, this); // for content assist

    // newText(tc, SWT.NONE,
    // "Enter a Type name. Content Assist is available on Eclipse 3.2 and beyond (press Ctrl +
    // Space)");

    // ContentProposalAdapter adapter = new ContentProposalAdapter(
    // text, new TextContentAdapter(),
    // candidatesToPickFrom,
    // contentAssistActivationKey,
    // contentAssistActivationChars);
    // adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

    Button browseButton = newPushButton(tc, "Browse", "Click here to browse possible types");
    browseButton.removeListener(SWT.Selection, this);
    final AbstractSection finalSection = aSection;
    browseButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        errorMessageUI.setText("");
        SelectTypeDialog dialog = new SelectTypeDialog(finalSection, candidatesToPickFrom);
        // OpenTypeSystemSelectionDialog dialog =
        // new OpenTypeSystemSelectionDialog(getShell(), typeList);
        if (dialog.open() != IDialogConstants.OK_ID)
          return;

        text.setText(
                (null == dialog.nameSpaceName || "".equals(dialog.nameSpaceName)) ? dialog.typeName
                        : dialog.nameSpaceName + "." + dialog.typeName);
        if (okButton != null)
          enableOK();
        /*
         * Object[] types = dialog.getResult(); if (types != null && types.length > 0) {
         * ITypeSystemInfo selectedType = (ITypeSystemInfo) types[0];
         * text.setText(selectedType.getFullName()); enableOK(); }
         */
      }
    });
    /*
     * TypeSystemCompletionProcessor processor = new TypeSystemCompletionProcessor(
     * candidatesToPickFrom); ControlContentAssistHelper.createTextContentAssistant(text,
     * processor); text.addListener(SWT.KeyDown, new Listener() { public void handleEvent(Event e) {
     * errorMessageUI.setText(""); } }); text.addListener(SWT.Modify, new Listener() { public void
     * handleEvent(Event e) { textModifyCallback(e); } });
     */
    return text;
  }

  /**
   * Text modify callback.
   *
   * @param e
   *          the e
   */
  public void textModifyCallback(Event e) {
  }

  /**
   * Gets the type system info list.
   *
   * @return the type system info list
   */
  // default implementation - always overridden when used
  public TypesWithNameSpaces getTypeSystemInfoList() {
    return new TypesWithNameSpaces();
  }

  /**
   * Type contained in type system info list.
   *
   * @param fullTypeName
   *          the full type name
   * @param types
   *          the types
   * @return true, if successful
   */
  protected boolean typeContainedInTypeSystemInfoList(String fullTypeName,
          TypesWithNameSpaces types) {
    String key = AbstractSection.getShortName(fullTypeName);
    String nameSpace = AbstractSection.getNameSpace(fullTypeName);

    Set s = (Set) types.sortedNames.get(key);

    if (null == s)
      return false;

    return s.contains(nameSpace);
  }

  /**
   * In XML, a 0 - length string is represented as &lt;xxx/&gt;, while a null value causes the
   * element to be omitted. Fix up values to be null if empty.
   *
   * @param v
   *          the v
   * @return null for 0 length string
   */
  public static String nullIf0lengthString(String v) {
    if ("".equals(v))
      return null;
    return v;
  }

  /**
   * Convert null.
   *
   * @param v
   *          the v
   * @return the string
   */
  public static String convertNull(String v) {
    if (null == v)
      return "";
    return v;
  }
}
