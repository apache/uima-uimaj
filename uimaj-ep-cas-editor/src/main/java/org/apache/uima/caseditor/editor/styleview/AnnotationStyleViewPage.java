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

package org.apache.uima.caseditor.editor.styleview;

import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.Images;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.caseditor.editor.AnnotationStyleChangeListener;
import org.apache.uima.caseditor.editor.IAnnotationEditorModifyListener;
import org.apache.uima.caseditor.ui.property.EditorAnnotationPropertyPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.dialogs.PropertyDialog;
import org.eclipse.ui.part.Page;

// TODO: Subscribe to style change events
// create new listener interface for this
class AnnotationStyleViewPage extends Page {

  static class AnnotationTypeContentProvider implements ITreeContentProvider {

    private AnnotationTypeNode[] annotationTypes;
    
    private AnnotationEditor editor;
    
    AnnotationTypeContentProvider(AnnotationEditor editor) {
      this.editor = editor;
    }
    
    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      
      if (newInput instanceof TypeSystem) {
        TypeSystem ts = (TypeSystem) newInput;
        
        List<Type> annotationTypeList = 
          ts.getProperlySubsumedTypes(ts.getType(CAS.TYPE_NAME_ANNOTATION));
        annotationTypeList.add(ts.getType(CAS.TYPE_NAME_ANNOTATION));
        
        annotationTypes = new AnnotationTypeNode[annotationTypeList.size()];
        
        for (int i = 0; i < annotationTypeList.size(); i++) {
          annotationTypes[i] = new AnnotationTypeNode(editor, annotationTypeList.get(i));
        }
      }
      else {
        annotationTypes = null;
      }
      
    }

    public Object[] getElements(Object inputElement) {
      return annotationTypes;
    }

    public Object[] getChildren(Object parentElement) {
      return null;
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return false;
    }
  }
  
  static class AnnotationStylingLabelProvider implements ITableLabelProvider, IColorProvider {

    private static int TYPE_NAME_COLUMN = 0;
    private static int STYLE_NAME_COLUMN = 1;
    
    private AnnotationEditor editor;
    
    AnnotationStylingLabelProvider(AnnotationEditor editor) {
      this.editor = editor;
    }
    
    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public String getColumnText(Object element, int columnIndex) {
      
      AnnotationTypeNode typeNode = (AnnotationTypeNode) element;
      
      Type type = typeNode.getAnnotationType();
      
      // TODO: Get this information trough the editor ... its easier
      AnnotationStyle style = editor.getAnnotationStyle(type);
      
      if (TYPE_NAME_COLUMN == columnIndex) {
        return type.getShortName().trim();
      }
      else if (STYLE_NAME_COLUMN == columnIndex) {
        return style.getStyle().toString();
      }
      else {
        throw new IllegalStateException("Unkown column!");
      }
    }

    public Color getForeground(Object element) {
      return null;
    }

    public Color getBackground(Object element) {
      
      AnnotationTypeNode typeNode = (AnnotationTypeNode) element;
      
      Type type = typeNode.getAnnotationType();
      
      AnnotationStyle style = editor.getAnnotationStyle(type);
      
      return new Color(Display.getCurrent(), style.getColor().getRed(),
              style.getColor().getGreen(), style.getColor().getBlue());
    }
  }
  
  private AnnotationEditor editor;

  private IAnnotationEditorModifyListener editorListener;
  
  private AnnotationStyleChangeListener changeListener;
  
  private CheckboxTableViewer treeViewer;

  AnnotationStyleViewPage(AnnotationEditor editor) {
    this.editor = editor;
  }

  private static AnnotationTypeNode[] typesToNodes(Collection<Type> types, AnnotationEditor editor) {
    Collection<Type> shownTypes = editor.getShownAnnotationTypes();
    
    AnnotationTypeNode[] selectedNodes = new AnnotationTypeNode[shownTypes.size()];
    
    int typeIndex = 0;
    for (Type shownType : shownTypes) {
      selectedNodes[typeIndex++] = new AnnotationTypeNode(editor, shownType);
    }
    
    return selectedNodes;
  }
  
  @Override
  public void createControl(Composite parent) {

    Table table = new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
            SWT.CHECK | SWT.NO_FOCUS);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    
    final Color defaultForegroundColor = table.getForeground();
    
    table.addListener(SWT.EraseItem, new Listener() {
      public void handleEvent(Event event) {
          if((event.detail & SWT.SELECTED) != 0 ){
              event.detail &= ~SWT.SELECTED;
              event.gc.setForeground(defaultForegroundColor);
          }
      }
    });
    
    treeViewer = new CheckboxTableViewer(table);

    TableColumn typeColumn = new TableColumn(table, SWT.LEFT);
    typeColumn.setAlignment(SWT.LEFT);
    typeColumn.setText("Type");
    typeColumn.setWidth(120);
    
    TableColumn stlyeColumn = new TableColumn(table, SWT.LEFT);
    stlyeColumn.setAlignment(SWT.LEFT);
    stlyeColumn.setText("Style");
    stlyeColumn.setWidth(100);

    treeViewer.setContentProvider(new AnnotationTypeContentProvider(editor));
    treeViewer.setLabelProvider(new AnnotationStylingLabelProvider(editor));
    
    treeViewer.setInput(editor.getDocument().getCAS().getTypeSystem());
    getSite().setSelectionProvider(treeViewer);
    
    changeListener = new AnnotationStyleChangeListener() {
      
      
      public void annotationStylesChanged(Collection<AnnotationStyle> styles) {
        
        // Update all changed style elements in the table
        
        AnnotationTypeNode typeNodes[] = new AnnotationTypeNode[styles.size()];
        
        int i = 0;
        for (AnnotationStyle style : styles) {
          
          typeNodes[i++] = new AnnotationTypeNode(editor,
                  editor.getDocument().getType(style.getAnnotation()));
        }
        
        treeViewer.update(typeNodes, null);
      }
    };
    
    editor.getCasDocumentProvider().getTypeSystemPreferenceStore(editor.getEditorInput()).
        addPropertyChangeListener(changeListener);
    
    treeViewer.setCheckedElements(typesToNodes(editor.getShownAnnotationTypes(), editor));
    
    treeViewer.setGrayed(new AnnotationTypeNode(editor, editor.getAnnotationMode()), true);
    
    treeViewer.addCheckStateListener(new ICheckStateListener() {
      
      public void checkStateChanged(CheckStateChangedEvent event) {
        
       AnnotationTypeNode typeNode = (AnnotationTypeNode) event.getElement();
       
       // The grayed mode annotation cannot be unselected, if the
       // user clicks on it prevent the state change
       if (typeNode.getAnnotationType().equals(editor.getAnnotationMode())) {
         treeViewer.setChecked(event.getElement(), true);
       }
       else {
           editor.setShownAnnotationType(typeNode.getAnnotationType(),
                   event.getChecked());
       }
      }
    });
    
    editorListener = new IAnnotationEditorModifyListener() {
      
      public void showAnnotationsChanged(Collection<Type> shownAnnotationTypes) {
        treeViewer.setCheckedElements(typesToNodes(shownAnnotationTypes, editor));
      }
      
      public void annotationModeChanged(Type newMode) {
        // maybe slow if there are many types
        treeViewer.setAllGrayed(false); 
        treeViewer.setGrayed(new AnnotationTypeNode(editor, newMode), true);
      }
    };
    
    // TODO: must this listener be removed ?!
    editor.addAnnotationListener(editorListener);
    
    // register a listener to change state
    // create a action handle to push changes to editor
  }

  @Override
  public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
          IStatusLineManager statusLineManager) {
    super.makeContributions(menuManager, toolBarManager, statusLineManager);
    
    // TODO: Figure out how to use open properties dialog action here correctly
    // see http://wiki.eclipse.org/FAQ_How_do_I_open_a_Property_dialog%3F
    
    IAction action = new Action() {
      @Override
      public void run() {
        super.run();
        
        ISelection sel = new StructuredSelection(new AnnotationTypeNode(editor, null));
        PropertyPage page = new EditorAnnotationPropertyPage();
        page.setElement(new AnnotationTypeNode(editor, null));
        page.setTitle("Styles");
        PreferenceManager mgr = new PreferenceManager();
        IPreferenceNode node = new PreferenceNode("1", page);
        mgr.addToRoot(node);
        PropertyDialog dialog = new PropertyDialog(getSite().getShell(), mgr, sel);
        dialog.create();
        dialog.setMessage(page.getTitle());
        dialog.open();
      }
    };
    
    action.setImageDescriptor(CasEditorPlugin
            .getTaeImageDescriptor(Images.MODEL_PROCESSOR_FOLDER));
    
    toolBarManager.add(action);
  }
  
  @Override
  public Control getControl() {
    return treeViewer.getControl();
  }

  @Override
  public void setFocus() {
    treeViewer.getControl().setFocus();
  }
  
  @Override
  public void dispose() {
    super.dispose();
    
    IPreferenceStore store = editor.getCasDocumentProvider().getTypeSystemPreferenceStore(
            editor.getEditorInput()); // TODO: Use old input, which was used when the view was created!
    
    if (store != null && changeListener != null)        
      store.removePropertyChangeListener(changeListener);
    
    editor.removeAnnotationListener(editorListener);
  }
}
