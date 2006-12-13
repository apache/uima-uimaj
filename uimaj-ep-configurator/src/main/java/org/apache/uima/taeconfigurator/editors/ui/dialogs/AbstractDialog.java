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
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.fieldassist.TextControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistField;

public abstract class AbstractDialog extends Dialog implements Listener, StandardStrings {
  
  public final static char[] contentAssistActivationChars  = new char[0];
  public final static KeyStroke contentAssistActivationKey;
  static {
    try {
      contentAssistActivationKey = KeyStroke.getInstance("Ctrl+Space");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  protected MultiPageEditor editor;

  protected AbstractSection section;

  protected Button okButton;

  protected Label errorMessageUI;

  protected String title;

  protected String dialogDescription;

  /**
   * @param parentShell
   */
  protected AbstractDialog(AbstractSection aSection, String title, String description) {
    // maintainers: don't use new shell; see comment in Dialog class
    super(aSection.getSection().getShell());
    section = aSection;
    editor = section.editor;
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.title = title;
    this.dialogDescription = description;
  }

  protected Control createDialogArea(Composite parent) {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);
    createWideLabel(composite, dialogDescription);
    return composite;
  }

  protected Control createDialogArea(Composite parent, Object existing) {
    Composite composite = (Composite) super.createDialogArea(parent);
    createWideLabel(composite, dialogDescription);
    if (null != existing)
      getShell().setText(getShell().getText().replaceFirst("Add", "Edit"));
    return composite;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  protected Label createWideLabel(Composite parent, String message) {
    Label label = null;

    label = new Label(parent, SWT.WRAP);
    label.setText(null != message ? message : "");
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    label.setLayoutData(data);
    return label;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    okButton = getButton(IDialogConstants.OK_ID);
    okButton.setEnabled(false);
  }

  public static final String[] stringArray0 = new String[0];

  protected void setTextAndTip(Button c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  protected void setTextAndTip(CCombo c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  protected void setTextAndTip(Label c, String label, String tip) {
    c.setText(label);
    c.setToolTipText(tip);
  }

  protected Composite new2ColumnComposite(Composite parent) {
    Composite twoCol = new Composite(parent, SWT.NONE);
    twoCol.setLayout(new GridLayout(2, false)); // false = not equal width
    ((GridLayout) twoCol.getLayout()).marginHeight = 0;
    ((GridLayout) twoCol.getLayout()).marginWidth = 0;
    twoCol.setLayoutData(new GridData(GridData.FILL_BOTH));
    return twoCol;
  }

  protected Text newDescription(Composite twoCol, String tip) {
    setTextAndTip(new Label(twoCol, SWT.NONE), S_DESCRIPTION, tip);
    Text t = new Text(twoCol, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    t.setLayoutData(new GridData(GridData.FILL_BOTH));
    ((GridData) t.getLayoutData()).heightHint = 100;
    t.setToolTipText(tip);
    return t;
  }

  protected CCombo newLabeledCCombo(Composite parent, String label, String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newCCombo(parent, tip);
  }

  protected CCombo newCCombo(Composite parent, String tip) {
    final CCombo cc = new CCombo(parent, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
    cc.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
    cc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    cc.addListener(SWT.Selection, this);
    cc.setToolTipText(tip);
    cc.addKeyListener(new KeyListener() {
      private final StringBuffer b = new StringBuffer();

      public void keyPressed(KeyEvent e) {
      }

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

  protected Button newButton(Composite parent, int style, String name, String tip) {
    Button b = new Button(parent, style);
    setTextAndTip(b, name, tip);
    b.addListener(SWT.Selection, this);
    return b;
  }

  protected Text newText(Composite parent, int style, String tip) {
    Text t = new Text(parent, style | SWT.BORDER);
    t.setToolTipText(tip);
    t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    t.addListener(SWT.KeyUp, this);
    t.addListener(SWT.MouseUp, this); // for paste operation
    return t;
  }

  protected Text newLabeledText(Composite parent, int style, String label, String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newText(parent, style, tip);
  }

  /**
   * Styles = SWT.SINGLE / MULTI / CHECK / FULL_SELECTION / HIDE_SELECTION
   * 
   * @param parent
   * @param style
   * @return
   */
  protected Table newTable(Composite parent, int style) {
    Table table = new Table(parent, style | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gd);
    table.addListener(SWT.Selection, this);
    table.addListener(SWT.KeyUp, this); // delete key
    return table;
  }

  public int getHitColumn(TableItem item, Point p) {
    for (int i = item.getParent().getColumnCount() - 1; i >= 0; i--) {
      Rectangle columnBounds = item.getBounds(i);
      if (columnBounds.contains(p))
        return i;
    }
    return -1;
  }

  public Composite newButtonContainer(Composite parent) {
    Composite buttonContainer = new Composite(parent, SWT.NONE);
    buttonContainer.setLayout(new GridLayout());
    buttonContainer.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING
            | GridData.HORIZONTAL_ALIGN_FILL));
    return buttonContainer;
  }

  public Button newPushButton(Composite parent, String label, String tip) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    button.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING
            | GridData.HORIZONTAL_ALIGN_FILL));
    // button.pack();
    button.setToolTipText(tip);
    button.addListener(SWT.Selection, this);
    return button;
  }

  protected void newErrorMessage(Composite c) {
    newErrorMessage(c, 1);
  }

  protected void newErrorMessage(Composite twoCol, int span) {
    Label m = new Label(twoCol, SWT.WRAP);
    m.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    ((GridData) m.getLayoutData()).horizontalSpan = span;
    ((GridData) m.getLayoutData()).widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    errorMessageUI = m;
  }

  protected void setErrorMessage(String msg) {
    errorMessageUI.setText(msg);
    Composite shell = errorMessageUI.getParent();
    while (!(shell instanceof Shell))
      shell = shell.getParent();
    shell.setSize(shell.computeSize(-1, -1));
  }

  // overridden where needed
  public void handleEvent(Event event) {
    enableOK();
  }

  protected void superButtonPressed(int buttonId) {
    super.buttonPressed(buttonId);
  }

  // overridden where needed
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      copyValuesFromGUI();
      if (!isValid())
        return; // keeps dialog open
    }
    super.buttonPressed(buttonId);
  }

  public abstract void copyValuesFromGUI();

  public abstract boolean isValid();

  protected Control createButtonBar(Composite c) {
    Control returnValue = super.createButtonBar(c);
    enableOK();
    return returnValue;
  }

  public abstract void enableOK();

  public boolean useQualifiedTypes() {
    return MultiPageEditorContributor.getUseQualifiedTypes();
  }

  protected String[] getAllTypesAsSortedArray() {
    String[] allTypes = (String[]) section.editor.allTypes.get().keySet().toArray(stringArray0);
    Arrays.sort(allTypes, new Comparator() {
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

  protected Text newLabeledTypeInput(AbstractSection aSection, Composite parent, String label, String tip) {
    setTextAndTip(new Label(parent, SWT.NONE), label, tip);
    return newTypeInput(aSection, parent);
  }

  /**
   * @param twoCol
   */
  protected Text newTypeInput(AbstractSection aSection, Composite twoCol) {
    Composite tc = new2ColumnComposite(twoCol);
    final TypesWithNameSpaces candidatesToPickFrom = getTypeSystemInfoList(); // provide an ArrayList of

    ContentAssistField caf = new ContentAssistField(tc, SWT.BORDER, new TextControlCreator(), 
            new TextContentAdapter(), candidatesToPickFrom,
            null, null);
    caf.getContentAssistCommandAdapter().setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    final Text text = (Text)caf.getControl();
    text.setToolTipText("Enter a Type name. Content Assist is available on Eclipse 3.2 and beyond (press Ctrl + Space)");
    text.getParent().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    text.addListener(SWT.KeyUp, this);
    text.addListener(SWT.MouseUp, this); // for paste operation
    
//    newText(tc, SWT.NONE,
//    "Enter a Type name. Content Assist is available on Eclipse 3.2 and beyond (press Ctrl + Space)");

//    ContentProposalAdapter adapter = new ContentProposalAdapter(
//            text, new TextContentAdapter(),
//            candidatesToPickFrom,
//            contentAssistActivationKey, 
//            contentAssistActivationChars);
//    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    
    Button browseButton = newPushButton(tc, "Browse", "Click here to browse possible types");
    browseButton.removeListener(SWT.Selection, this);
    final AbstractSection finalSection = aSection;
    browseButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        errorMessageUI.setText("");
        SelectTypeDialog dialog = new SelectTypeDialog(finalSection, candidatesToPickFrom); 
//          OpenTypeSystemSelectionDialog dialog = 
//              new OpenTypeSystemSelectionDialog(getShell(), typeList);
        if (dialog.open() != IDialogConstants.OK_ID)
          return;
        
        text.setText(dialog.nameSpaceName + "." + dialog.typeName);
        enableOK();
    /*
        Object[] types = dialog.getResult();
        if (types != null && types.length > 0) {
          ITypeSystemInfo selectedType = (ITypeSystemInfo) types[0];
          text.setText(selectedType.getFullName());
          enableOK();
        }
    */
      }
    });
    /*
    TypeSystemCompletionProcessor processor = new TypeSystemCompletionProcessor(
            candidatesToPickFrom);
    ControlContentAssistHelper.createTextContentAssistant(text, processor);
    text.addListener(SWT.KeyDown, new Listener() {
      public void handleEvent(Event e) {
        errorMessageUI.setText("");
      }
    });
    text.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event e) {
        textModifyCallback(e);
      }
    });
    */
    return text;
  }

  public void textModifyCallback(Event e) {
  }
 
  // default implementation - always overridden when used
  public TypesWithNameSpaces getTypeSystemInfoList() {
    return new TypesWithNameSpaces();
  }

  protected boolean typeContainedInTypeSystemInfoList(String fullTypeName, TypesWithNameSpaces types) {
    String key = AbstractSection.getShortName(fullTypeName);
    String nameSpace = AbstractSection.getNameSpace(fullTypeName);
    
    Set s = (Set)types.sortedNames.get(key);
    
    if (null == s) 
      return false;
    
    return s.contains(nameSpace);
  }

  /**
   * In XML, a 0 - length string is represented as <xxx/>, while a null value causes the element to
   * be omitted. Fix up values to be null if empty.
   * 
   * @param v
   * @return
   */
  public static String nullIf0lengthString(String v) {
    if ("".equals(v))
      return null;
    return v;
  }

  public static String convertNull(String v) {
    if (null == v)
      return "";
    return v;
  }
}
