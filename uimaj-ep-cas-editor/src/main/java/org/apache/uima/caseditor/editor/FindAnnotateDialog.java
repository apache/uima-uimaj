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
 * KIND, either express or implied.  See the L0icense for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.caseditor.editor;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.Images;
import org.apache.uima.caseditor.core.TaeError;
import org.apache.uima.caseditor.editor.action.LowerLeftAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.LowerRightAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.WideLeftAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.WideRightAnnotationSideAction;
import org.apache.uima.caseditor.editor.fsview.TypeCombo;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog to find and annotate a piece of text in the document.
 * 
 * TODO:
 * Add option to only search in text which is not covered by the annotation type.
 * Add buttons to modify annotation bounds
 * Match whole annotation: e.g. Token
 * Scope search in annotations of type
 * Add history to search field
 */
class FindAnnotateDialog extends Dialog {

  private static final int FIND_BUTTON = 100;
  private static final int ANNOTATE_FIND_BUTTON = 101;
  private static final int ANNOTATE_BUTTON = 102;
  private static final int ANNOTATE_ALL_BUTTON = 103;
  private static int CLOSE_BUTTON = 104;
  
  private final IFindReplaceTarget findReplaceTarget;
  private final ICasDocument document;
  private final Type modeType;
  
  private Combo findField;
  private TypeCombo typeField;
  
  private Button wideLeftSideButton;
  private Button lowerLeftSideButton;
  private Button lowerRightSideButton;
  private Button wideRightSideButton;
  
  private Button forwardRadioButton;
  
  /**
   * Annotation created by the user with the Annotate button.
   * Contains null if no search was done or search was continued after 
   * an annotation was created.
   */
  private AnnotationFS currentAnnotation;
  
  FindAnnotateDialog(Shell parentShell, ICasDocument document, IFindReplaceTarget findReplaceTarget, Type modeType) {
    super(parentShell);
    this.document = document;
    this.findReplaceTarget = findReplaceTarget;
    this.modeType = modeType;
  }

  @Override
  public void create() {
    // calls createContents first
    super.create();

    getShell().setText("Find/Annotate");

    findField.setText(findReplaceTarget.getSelectionText());

    // TODO: set history
  }

  /**
   * Creates the search string input field.
   * 
   * @param parent
   * 
   * @return
   */
  private Composite createInputPanel(Composite parent) {
    Composite panel= new Composite(parent, SWT.NULL);
    GridLayout layout= new GridLayout();
    layout.numColumns= 2;
    panel.setLayout(layout);

    // find label
    Label findLabel= new Label(panel, SWT.LEFT);
    findLabel.setText("Find:");

    GridData labelData = new GridData();
    labelData.horizontalAlignment =  SWT.LEFT;
    findLabel.setLayoutData(labelData);

    // find combo box
    findField = new Combo(panel, SWT.DROP_DOWN | SWT.BORDER);

    GridData findFieldData = new GridData();
    findFieldData.horizontalAlignment = SWT.FILL;
    findFieldData.grabExcessHorizontalSpace = true;
    findField.setLayoutData(findFieldData);

    // type label
    Label typeLabel= new Label(panel, SWT.LEFT);
    typeLabel.setText("Type:");
    GridData typeData = new GridData();
    typeData.horizontalAlignment =  SWT.LEFT;
    typeLabel.setLayoutData(typeData);

    typeField = new TypeCombo(panel);
    typeField.setInput(document.getCAS().getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION),
            document.getCAS().getTypeSystem());

    typeField.select(modeType);
    GridData typeFieldData = new GridData();
    typeFieldData.horizontalAlignment = SWT.FILL;
    typeFieldData.grabExcessHorizontalSpace = true;
    typeField.setLayoutData(typeFieldData);

    return panel;
  }

  /**
   * Creates the group to specify the direction of the search.
   * 
   * @param parent
   * 
   * @return
   */
  private Composite createDirectionGroup(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    panel.setLayout(layout);

    Group group= new Group(panel, SWT.SHADOW_ETCHED_IN);
    group.setText("Direction");
    GridLayout groupLayout= new GridLayout();
    groupLayout.numColumns = 2;
    group.setLayout(groupLayout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    forwardRadioButton = new Button(group, SWT.RADIO | SWT.LEFT);
    forwardRadioButton.setText("Forward");
    forwardRadioButton.setSelection(true);

    Button backwardRadioButton= new Button(group, SWT.RADIO | SWT.LEFT);
    backwardRadioButton.setText("Backward");

    return panel;
  }

  private void createAnnotationButtons(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    panel.setLayout(layout);

    Group group= new Group(panel, SWT.SHADOW_ETCHED_IN);
    group.setText("Annotation");
    GridLayout groupLayout= new GridLayout();
    groupLayout.numColumns = 4;
    group.setLayout(groupLayout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    wideLeftSideButton = new Button(group, SWT.PUSH | SWT.LEFT);
    wideLeftSideButton.setImage(CasEditorPlugin.getTaeImageDescriptor(
            Images.WIDE_LEFT_SIDE).createImage());
    wideLeftSideButton.setEnabled(false);
    
    wideLeftSideButton.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent e) {
        // never called, do not implement
      }

      public void widgetSelected(SelectionEvent e) {
        WideLeftAnnotationSideAction.wideLeftAnnotationSide(document, currentAnnotation);
      }
    });
    
    lowerLeftSideButton = new Button(group, SWT.PUSH | SWT.LEFT);
    lowerLeftSideButton.setImage(CasEditorPlugin.getTaeImageDescriptor(
            Images.LOWER_LEFT_SIDE).createImage());
    lowerLeftSideButton.setEnabled(false);
    lowerLeftSideButton.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent e) {
        // never called, do not implement
      }

      public void widgetSelected(SelectionEvent e) {
        LowerLeftAnnotationSideAction.lowerLeftAnnotationSide(document, currentAnnotation);
      }
    });

    lowerRightSideButton = new Button(group, SWT.PUSH | SWT.LEFT);
    lowerRightSideButton.setImage(CasEditorPlugin.getTaeImageDescriptor(
            Images.LOWER_RIGHT_SIDE).createImage());
    lowerRightSideButton.setEnabled(false);
    lowerRightSideButton.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent e) {
        // never called, do not implement
      }

      public void widgetSelected(SelectionEvent e) {
        LowerRightAnnotationSideAction.lowerRightAnnotationSide(document, currentAnnotation);
      }
    });

    wideRightSideButton = new Button(group, SWT.PUSH | SWT.LEFT);
    wideRightSideButton.setImage(CasEditorPlugin.getTaeImageDescriptor(
            Images.WIDE_RIGHT_SIDE).createImage());
    wideRightSideButton.setEnabled(false);
    wideRightSideButton.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent e) {
        // never called, do not implement
      }

      public void widgetSelected(SelectionEvent e) {
        WideRightAnnotationSideAction.wideRightAnnotationSide(document, currentAnnotation);
      }
    });
  }

  private void updateAnnotationButtons() {

    boolean areButtonsEnabled = currentAnnotation != null;
    wideLeftSideButton.setEnabled(areButtonsEnabled);
    lowerLeftSideButton.setEnabled(areButtonsEnabled);
    lowerRightSideButton.setEnabled(areButtonsEnabled);
    wideRightSideButton.setEnabled(areButtonsEnabled);
  }

  /**
   * Creates the find and annotate buttons.
   * 
   * @param parent
   * 
   * @return
   */
  private Composite createButtonSection(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();

    // Number of columns should be 2, for each button added to the panel the
    // createButton methods increments numColumns. That means after adding 
    // 3 buttons numColumns is 2.
    layout.numColumns = -1;

    panel.setLayout(layout);

    createButton(panel, FIND_BUTTON, "Fi&nd",true);

    createButton(panel, ANNOTATE_FIND_BUTTON, "Annotate/Fin&d", false);

    createButton(panel, ANNOTATE_BUTTON, "&Annotate", false);

    //  createButton(panel, ANNOTATE_ALL_BUTTON, "Anno&tate All", false);

    return panel;
  }

  private Composite createStatusAndCloseButton(Composite parent) {

    Composite panel= new Composite(parent, SWT.NULL);
    GridLayout layout= new GridLayout();
    layout.numColumns= 2;
    layout.marginWidth= 0;
    layout.marginHeight= 0;
    panel.setLayout(layout);

    Label statusLabel= new Label(panel, SWT.LEFT);
    GridData statusData = new GridData();
    statusData.horizontalAlignment = SWT.FILL;
    statusData.grabExcessHorizontalSpace= true;
    statusLabel.setLayoutData(statusData);

    Button closeButton = createButton(panel, CLOSE_BUTTON, "Close", false);
    GridData closeData = new GridData();
    closeData.horizontalAlignment = SWT.RIGHT;
    closeButton.setLayoutData(closeData);
    //    setGridData(closeButton, SWT.RIGHT, false, SWT.BOTTOM, false);

    return panel;
  }

  @Override
  protected Control createContents(Composite parent) {

    Composite panel = new Composite(parent, SWT.NULL);
    GridLayout layout= new GridLayout();
    layout.numColumns= 1;
    layout.makeColumnsEqualWidth= true;
    panel.setLayout(layout);

    Composite inputPanel = createInputPanel(panel);
    GridData inputPanelData = new GridData();
    inputPanelData.horizontalAlignment =  SWT.FILL;
    inputPanelData.grabExcessHorizontalSpace = true;
    inputPanelData.verticalAlignment = SWT.TOP;
    inputPanelData.grabExcessVerticalSpace = false;
    inputPanel.setLayoutData(inputPanelData);

    createDirectionGroup(panel);

    createAnnotationButtons(panel);
    
    Composite buttonFindAnnotatePanel = createButtonSection(panel);
    GridData buttonFindAnnotatePanelData = new GridData();
    buttonFindAnnotatePanelData.horizontalAlignment = SWT.RIGHT;
    buttonFindAnnotatePanelData.grabExcessHorizontalSpace = true;
    buttonFindAnnotatePanel.setLayoutData(buttonFindAnnotatePanelData);

    Composite statusAndClosePanel = createStatusAndCloseButton(panel);
    GridData statusAndClosePanelData = new GridData();
    statusAndClosePanelData.horizontalAlignment = SWT.FILL;
    statusAndClosePanelData.grabExcessVerticalSpace = true;
    statusAndClosePanel.setLayoutData(statusAndClosePanelData);

    applyDialogFont(panel);

    return panel;
  }

  /**
   * Finds the next occurrence of the search string and selects it in
   * the editor.
   */
  private void findAndSelectNext() {

    boolean isForwardSearch = forwardRadioButton.getSelection();

    int textOffset = -1;

    if (isForwardSearch) {
      textOffset = findReplaceTarget.getSelection().x + findReplaceTarget.getSelection().y;
    }
    else {
      textOffset = findReplaceTarget.getSelection().x - 1;
    }

    int result = findReplaceTarget.findAndSelect(textOffset, findField.getText(), isForwardSearch, false, false);

    if (result == -1) {
      findReplaceTarget.findAndSelect(-1, findField.getText(), isForwardSearch, false, false);
    }
  }

  private AnnotationFS annotateSelection() {
    Point selection = findReplaceTarget.getSelection();

    AnnotationFS newAnnotation = document.getCAS().createAnnotation(
            typeField.getType(), selection.x, selection.x + selection.y);
    document.getCAS().addFsToIndexes(newAnnotation);
    document.addFeatureStructure(newAnnotation);
    
    return newAnnotation;
  }

  @Override
  protected void buttonPressed(int buttonID) {

    if (FIND_BUTTON == buttonID) {
      findAndSelectNext();
      currentAnnotation = null;
    }
    else if (ANNOTATE_BUTTON == buttonID) {
      currentAnnotation = annotateSelection();
    }
    else if (ANNOTATE_FIND_BUTTON == buttonID) {
      annotateSelection();
      findAndSelectNext();
      
      // TODO: Should be disabled when annotate was pressed
    }
    else if (ANNOTATE_ALL_BUTTON == buttonID) {
      // TODO: Should it be implemented ???
    }
    else if (CLOSE_BUTTON == buttonID) {
      close();
      return;
    }
    else {
      throw new TaeError("Unkown button!");
    }
    
    updateAnnotationButtons();
  }
}
