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

package org.apache.uima.caseditor.ui.property;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DotCorpusElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.model.TypesystemElement;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * This is the <code>AnnotationPropertyPage</code>. this page configures the project dependent
 * and type dependent annotation appearance in the <code>AnnotationEditor</code>.
 */
public abstract class AnnotationPropertyPage extends PropertyPage {

  private boolean isTypeSystemPresent = true;
  
  private Combo mStyleCombo;

  private ColorSelector mColorSelector;

  private TableViewer mTypeList;

  private AnnotationStyle mCurrentSelectedAnnotation = null;

  private Map<String, AnnotationStyle> changedStyles = new HashMap<String, AnnotationStyle>();

  private AnnotationStyle getDefaultAnnotation() {

    IStructuredSelection selection = (IStructuredSelection) mTypeList.getSelection();

    Type selectedType = (Type) selection.getFirstElement();

    return new AnnotationStyle(selectedType.getName(), AnnotationStyle.DEFAULT_STYLE,
            AnnotationStyle.DEFAULT_COLOR, mCurrentSelectedAnnotation.getLayer());
  }

  protected abstract AnnotationStyle getAnnotationStyle(Type type);
  
  // does not make sense, just give it a list with new annotation styles,
  // to save them and notify other about the chagne
  
  protected final void setAnnotationStyle(AnnotationStyle style) {
    changedStyles.put(mCurrentSelectedAnnotation.getAnnotation(), mCurrentSelectedAnnotation);
  }
  
  protected abstract TypeSystem getTypeSystem();
  
  private void itemSelected() {
    IStructuredSelection selection = (IStructuredSelection) mTypeList.getSelection();

    Type selectedType = (Type) selection.getFirstElement();

    if( selectedType != null) {
      
      AnnotationStyle style = getAnnotationStyle(selectedType);
  
      mCurrentSelectedAnnotation = style;
  
      if (style == null) {
        style = new AnnotationStyle(selectedType.getName(), AnnotationStyle.DEFAULT_STYLE,
                AnnotationStyle.DEFAULT_COLOR, mCurrentSelectedAnnotation.getLayer());
      }
  
      mStyleCombo.setText(style.getStyle().name());
      mStyleCombo.setEnabled(true);
  
      Color color = style.getColor();
      mColorSelector.setColorValue(new RGB(color.getRed(), color.getGreen(), color.getBlue()));
      mColorSelector.setEnabled(true);
      
      // TODO: Enable move up down buttons
    }
    else {
      // no type selected
      mStyleCombo.setEnabled(false);
      mColorSelector.setEnabled(false);
      
      // TODO: Disable up down button
    }
  }
  
  /**
   * Creates the annotation property page controls.
   */
  @Override
  protected Control createContents(Composite parent) {

    TypeSystem typeSystem = getTypeSystem();

    if (typeSystem == null) {
      
      isTypeSystemPresent = false;
      
      Label message = new Label(parent, SWT.NONE);
      message.setText("Please set a valid typesystem file first.");

      return message;
    }

    Composite base = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    base.setLayout(layout);

    // type text
    Label typeText = new Label(base, SWT.NONE);
    typeText.setText("Annotation types:");

    GridData typeTextGridData = new GridData();
    typeTextGridData.horizontalSpan = 2;
    typeText.setLayoutData(typeTextGridData);

    // type list
    mTypeList = new TableViewer(base, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
    GridData typeListGridData = new GridData();
    typeListGridData.horizontalAlignment = SWT.FILL;
    typeListGridData.grabExcessVerticalSpace = true;
    typeListGridData.verticalAlignment = SWT.FILL;
    typeListGridData.verticalSpan = 2;
    mTypeList.getControl().setLayoutData(typeListGridData);

    mTypeList.getTable().setHeaderVisible(true);

    TableViewerColumn typeColumn = new TableViewerColumn(mTypeList, SWT.LEFT);
    typeColumn.getColumn().setText("Type");
    typeColumn.getColumn().setWidth(250);
    typeColumn.setLabelProvider(new CellLabelProvider(){
      @Override
      public void update(ViewerCell cell) {

        Type type = (Type) cell.getElement();

        cell.setText(type.getName());
      }});

    TableViewerColumn layerColumn = new TableViewerColumn(mTypeList, SWT.LEFT);
    layerColumn.getColumn().setText("Layer");
    layerColumn.getColumn().setWidth(50);

    layerColumn.setLabelProvider(new CellLabelProvider() {

      @Override
      public void update(ViewerCell cell) {

        Type type = (Type) cell.getElement();

        AnnotationStyle style = getAnnotationStyle(type);

        cell.setText(Integer.toString(style.getLayer()));
      }});

    Type annotationType = typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);

    List<Type> types = typeSystem.getProperlySubsumedTypes(annotationType);

    for (Type type : types) {
      // inserts objects with type Type
      mTypeList.add(type);
    }

    mTypeList.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
        itemSelected();
      }
    });

    Composite settingsComposite = new Composite(base, SWT.NONE);

    GridLayout settingsLayout = new GridLayout();
    settingsLayout.numColumns = 2;
    settingsComposite.setLayout(settingsLayout);

    // text style combo
    Label styleText = new Label(settingsComposite, SWT.READ_ONLY);

    styleText.setText("Style:");

    // style combo
    mStyleCombo = new Combo(settingsComposite, SWT.READ_ONLY | SWT.DROP_DOWN);
    mStyleCombo.setEnabled(false);
    mStyleCombo.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        if (mCurrentSelectedAnnotation == null) {
          mCurrentSelectedAnnotation = getDefaultAnnotation();
        }

        mCurrentSelectedAnnotation = new AnnotationStyle(
                mCurrentSelectedAnnotation.getAnnotation(), AnnotationStyle.Style
                        .valueOf(mStyleCombo.getText()), mCurrentSelectedAnnotation.getColor(),
                mCurrentSelectedAnnotation.getLayer());

        setAnnotationStyle(mCurrentSelectedAnnotation);
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        // not needed
      }

    });
    AnnotationStyle.Style possibleStyles[] = AnnotationStyle.Style.values();

    for (AnnotationStyle.Style style : possibleStyles) {
      mStyleCombo.add(style.name());
    }

    // text color label
    Label colorText = new Label(settingsComposite, SWT.NONE);
    colorText.setText("Color:");

    mColorSelector = new ColorSelector(settingsComposite);
    mColorSelector.setEnabled(false);
    mColorSelector.addListener(new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (mCurrentSelectedAnnotation == null) {
          mCurrentSelectedAnnotation = getDefaultAnnotation();
        }

        RGB colorRGB = mColorSelector.getColorValue();

        Color color = new Color(colorRGB.red, colorRGB.green, colorRGB.blue);

        mCurrentSelectedAnnotation = new AnnotationStyle(
                mCurrentSelectedAnnotation.getAnnotation(), mCurrentSelectedAnnotation.getStyle(),
                color, mCurrentSelectedAnnotation.getLayer());

        setAnnotationStyle(mCurrentSelectedAnnotation);
      }
    });

    Button moveLayerUpButton = new Button(settingsComposite, SWT.NONE);
    moveLayerUpButton.setText("Move layer up");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(moveLayerUpButton);
    moveLayerUpButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        if (mCurrentSelectedAnnotation == null) {
          mCurrentSelectedAnnotation = getDefaultAnnotation();
        }

        mCurrentSelectedAnnotation = new AnnotationStyle(
                mCurrentSelectedAnnotation.getAnnotation(), AnnotationStyle.Style
                        .valueOf(mStyleCombo.getText()), mCurrentSelectedAnnotation.getColor(),
                mCurrentSelectedAnnotation.getLayer() + 1);

        setAnnotationStyle(mCurrentSelectedAnnotation);

        mTypeList.refresh(((IStructuredSelection) mTypeList.getSelection()).getFirstElement(),
                true);
      }
    });

    Button moveLayerDownButton = new Button(settingsComposite, SWT.NONE);
    moveLayerDownButton.setText("Move layer down");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(moveLayerDownButton);

    moveLayerDownButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        if (mCurrentSelectedAnnotation != null && mCurrentSelectedAnnotation.getLayer() - 1 >= 0) {
          mCurrentSelectedAnnotation = getDefaultAnnotation();

          mCurrentSelectedAnnotation = new AnnotationStyle(mCurrentSelectedAnnotation
                  .getAnnotation(), AnnotationStyle.Style.valueOf(mStyleCombo.getText()),
                  mCurrentSelectedAnnotation.getColor(), mCurrentSelectedAnnotation.getLayer() - 1);

          setAnnotationStyle(mCurrentSelectedAnnotation);

          mTypeList.update(((IStructuredSelection) mTypeList.getSelection()).getFirstElement(),
                  null);
        }
      }
    });

    mTypeList.getTable().select(0);

    if (mTypeList.getTable().getSelectionIndex() != -1) {
      itemSelected();
    }

    return base;
  }

  protected abstract boolean saveChanges(Collection<AnnotationStyle> changedStyles);
  
  /**
   * Executed after the OK button was pressed.
   */
  @Override
  public boolean performOk() {
    
    if (!isTypeSystemPresent)
      return true;
    
    if (!saveChanges(changedStyles.values()))
      return false;
    
    changedStyles.clear();
    
    return true;
  }
}