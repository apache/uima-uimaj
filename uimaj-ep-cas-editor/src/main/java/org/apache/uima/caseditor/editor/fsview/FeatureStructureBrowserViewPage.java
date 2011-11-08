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

package org.apache.uima.caseditor.editor.fsview;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.Images;
import org.apache.uima.caseditor.editor.AbstractAnnotationDocumentListener;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.apache.uima.caseditor.editor.ICasEditor;
import org.apache.uima.caseditor.editor.ICasEditorInputListener;
import org.apache.uima.caseditor.editor.ModelFeatureStructure;
import org.apache.uima.caseditor.editor.action.DeleteFeatureStructureAction;
import org.apache.uima.caseditor.editor.util.StrictTypeConstraint;
import org.apache.uima.jcas.cas.StringArray;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.Page;

/**
 * The actual view page which contains the ui code for this view.
 */
public final class FeatureStructureBrowserViewPage extends Page implements ICasEditorInputListener {

  private static final String LAST_SELECTED_FS_TYPE = "lastSelectedFeatureStructureBrowserViewType";

  final class FeatureStructureTreeContentProvider extends AbstractAnnotationDocumentListener
          implements ITreeContentProvider, ICasEditorInputListener {

    private Type mCurrentType;

    private ICasEditor mEditor;

    FeatureStructureTreeContentProvider(ICasEditor editor) {
      mEditor = editor;
      mEditor.addCasEditorInputListener(this);
      
      if (mEditor.getDocument() != null)
        mEditor.getDocument().addChangeListener(this);
    }

    public Object[] getElements(Object inputElement) {
      if (mCurrentType == null) {
        return new Object[] {};
      }

      StrictTypeConstraint typeConstrain = new StrictTypeConstraint(mCurrentType);

      ICasDocument document = mEditor.getDocument();
      
      FSIterator<FeatureStructure> strictTypeIterator = document.getCAS().createFilteredIterator(
              document.getCAS().getIndexRepository().getAllIndexedFS(mCurrentType), typeConstrain);

      LinkedList<ModelFeatureStructure> featureStrucutreList = new LinkedList<ModelFeatureStructure>();

      while (strictTypeIterator.hasNext()) {
        featureStrucutreList.add(new ModelFeatureStructure(document, strictTypeIterator.next()));
      }

      ModelFeatureStructure[] featureStructureArray = new ModelFeatureStructure[featureStrucutreList
              .size()];

      featureStrucutreList.toArray(featureStructureArray);

      return featureStructureArray;
    }

    public void dispose() {
      if (mEditor.getDocument() != null)
        mEditor.getDocument().removeChangeListener(this);
      
      mEditor.removeCasEditorInputListener(this);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

      mCurrentType = (Type) newInput;

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          mFSList.refresh();
        }
      });
    }

    /**
     * Retrieves children for a FeatureStrcuture and for FeatureValues if they have children.
     * 
     * @param parentElement
     * @return the children
     */
    public Object[] getChildren(Object parentElement) {
      Collection<Object> childs = new LinkedList<Object>();

      FeatureStructure featureStructure;

      if (parentElement instanceof ModelFeatureStructure) {
        featureStructure = ((ModelFeatureStructure) parentElement).getStructre();
      } else if (parentElement instanceof FeatureValue) {
        FeatureValue value = (FeatureValue) parentElement;

        featureStructure = (FeatureStructure) value.getValue();
      } else {
        assert false : "Unexpected element!";

        return new Object[] {};
      }

      Type type = featureStructure.getType();

      for (Feature feature : type.getFeatures()) {
        childs.add(new FeatureValue(mEditor.getDocument(), featureStructure, feature));
      }

      assert childs.size() > 0;

      return childs.toArray();
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      if (element instanceof IAdaptable
              && ((IAdaptable) element).getAdapter(FeatureStructure.class) != null) {
        return true;
      } else if (element instanceof FeatureValue) {
        FeatureValue featureValue = (FeatureValue) element;

        if (featureValue.getFeature().getRange().isPrimitive()) {
          Object value = featureValue.getValue();

          if (value == null) {
            return false;
          }

          if (value instanceof StringArray) {
            StringArray array = (StringArray) featureValue.getValue();

            if (array.size() > 0) {
              return true;
            } else {
              return false;
            }
          }

          return false;
        } else {
          return featureValue.getValue() != null ? true : false;
        }
      } else {
        assert false : "Unexpected element";

        return false;
      }
    }

    @Override
    protected void addedAnnotation(Collection<AnnotationFS> annotations) {

      final LinkedList<ModelFeatureStructure> featureStrucutreList = new LinkedList<ModelFeatureStructure>();

      for (AnnotationFS annotation : annotations) {
        if (annotation.getType() == mCurrentType) {
          featureStrucutreList.add(new ModelFeatureStructure(mEditor.getDocument(), annotation));
        }
      }

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          mFSList.add(featureStrucutreList.toArray());
        }
      });
    }

    @Override
    public void added(Collection<FeatureStructure> structres) {
      final LinkedList<ModelFeatureStructure> featureStrucutreList = new LinkedList<ModelFeatureStructure>();

      for (FeatureStructure structure : structres) {
        if (structure.getType() == mCurrentType) {
          featureStrucutreList.add(new ModelFeatureStructure(mEditor.getDocument(), structure));
        }
      }

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          mFSList.add(featureStrucutreList.toArray());
        }
      });
    }

    @Override
    protected void removedAnnotation(Collection<AnnotationFS> annotations) {

      final LinkedList<ModelFeatureStructure> featureStrucutreList = new LinkedList<ModelFeatureStructure>();

      for (AnnotationFS annotation : annotations) {
        if (annotation.getType() == mCurrentType) {
          featureStrucutreList.add(new ModelFeatureStructure(mEditor.getDocument(), annotation));
        }
      }

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          mFSList.remove(featureStrucutreList.toArray());
        }
      });

    }

    @Override
    public void removed(Collection<FeatureStructure> structres) {
      final LinkedList<ModelFeatureStructure> featureStrucutreList = new LinkedList<ModelFeatureStructure>();

      for (FeatureStructure structure : structres) {
        if (structure.getType() == mCurrentType) {
          featureStrucutreList.add(new ModelFeatureStructure(mEditor.getDocument(), structure));
        }
      }

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          mFSList.remove(featureStrucutreList.toArray());
        }
      });
    }

    @Override
    protected void updatedAnnotation(Collection<AnnotationFS> annotations) {
      // ignore
    }

    public void viewChanged(String oldViewName, String newViewName) {
      changed();
    }

    public void changed() {
      mFSList.refresh();
    }

    public void casDocumentChanged(IEditorInput oldInput, ICasDocument oldDocument, 
            IEditorInput newInput, ICasDocument newDocument) {
      
      if (oldDocument != null)
        oldDocument.removeChangeListener(this);

      if (newDocument != null)
        newDocument.addChangeListener(this);
    }
  }

  private class CreateAction extends Action {
    // TOOD: extract it and add setType(...)
    @Override
    public void run() {
      // TODO: check if an AnnotationFS was created, if so
      // add it to the document

      // inserts a new feature structure of current type
      if (mCurrentType == null) {
        return;
      }

      FeatureStructure newFeatureStructure = mCasEditor.getDocument().getCAS().createFS(mCurrentType);

      mCasEditor.getDocument().addFeatureStructure(newFeatureStructure);

      mFSList.refresh(); // TODO: Should not be necessary?
    }
  }

  private class SelectAllAction extends Action {
    @Override
    public void run() {
      mFSList.getList().selectAll();
      mFSList.setSelection(mFSList.getSelection());
    }
  }

//  private ICasDocument mDocument;

  private ICasEditor mCasEditor;

  private TypeCombo typeCombo;
  
  private ListViewer mFSList;

  private Composite mInstanceComposite;

  private Type mCurrentType;

  private DeleteFeatureStructureAction mDeleteAction;

  private Action mSelectAllAction;

  private Collection<Type> filterTypes;

  /**
   * Initializes a new instance.
   * 
   * @param document
   */
  public FeatureStructureBrowserViewPage(ICasEditor editor) {

    if (editor == null)
      throw new IllegalArgumentException("editor parameter must not be null!");

    mCasEditor = editor;

    mDeleteAction = new DeleteFeatureStructureAction(editor);

    mSelectAllAction = new SelectAllAction();

    TypeSystem ts = editor.getDocument().getCAS().getTypeSystem();

    filterTypes = new HashSet<Type>();
    filterTypes.add(ts.getType(CAS.TYPE_NAME_ARRAY_BASE));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_BOOLEAN_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_BYTE_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_LONG_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_SHORT_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_DOUBLE_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_BYTE));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_SHORT));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_LONG));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_DOUBLE));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_BOOLEAN));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_EMPTY_FLOAT_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_EMPTY_FS_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_EMPTY_STRING_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FLOAT_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FS_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_FS_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_INTEGER));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_INTEGER_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_INTEGER_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_LIST_BASE));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_SOFA));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_STRING));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_STRING_ARRAY));
    filterTypes.add(ts.getType(CAS.TYPE_NAME_STRING_LIST));
  }

  @Override
  public void createControl(Composite parent) {
    mInstanceComposite = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();

    layout.numColumns = 1;

    mInstanceComposite.setLayout(layout);

    Composite typePanel = new Composite(mInstanceComposite, SWT.NULL);

    GridData typePanelData = new GridData();
    typePanelData.grabExcessHorizontalSpace = true;
    typePanelData.grabExcessVerticalSpace = false;
    typePanelData.horizontalAlignment = SWT.FILL;
    typePanel.setLayoutData(typePanelData);

    GridLayout typePanelLayout = new GridLayout();
    typePanelLayout.numColumns = 2;
    typePanel.setLayout(typePanelLayout);

    Label typeLabel = new Label(typePanel, SWT.NONE);
    typeLabel.setText("Type: ");

    GridData typeLabelData = new GridData();
    typeLabelData.horizontalAlignment = SWT.LEFT;
    typeLabel.setLayoutData(typeLabelData);

    GridData typeComboData = new GridData();
    
    typeCombo = new TypeCombo(typePanel);
    
    typeComboData.horizontalAlignment = SWT.FILL;
    typeComboData.grabExcessHorizontalSpace = true;
    typeCombo.setLayoutData(typeComboData);

    
    final IPreferenceStore store = mCasEditor.getCasDocumentProvider().getSessionPreferenceStore(
            mCasEditor.getEditorInput());

    typeCombo.addListener(new ITypePaneListener() {

      public void typeChanged(Type newType) {
        store.setValue(LAST_SELECTED_FS_TYPE, newType.getName());
      }
    });

    mFSList = new ListViewer(mInstanceComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
    GridData instanceListData = new GridData();
    instanceListData.grabExcessHorizontalSpace = true;
    instanceListData.grabExcessVerticalSpace = true;
    instanceListData.horizontalAlignment = SWT.FILL;
    instanceListData.verticalAlignment = SWT.FILL;
    mFSList.getList().setLayoutData(instanceListData);
    mFSList.setContentProvider(new FeatureStructureTreeContentProvider(mCasEditor));
    mFSList.setLabelProvider(new FeatureStructureLabelProvider());

    mFSList.setUseHashlookup(true);

    typeCombo.addListener(new ITypePaneListener() {
      public void typeChanged(Type newType) {
        mCurrentType = newType;

        mFSList.setInput(newType);
      }
    });

    getSite().setSelectionProvider(mFSList);
    
    // That call sets the content on the type combo
    casDocumentChanged(null, null, mCasEditor.getEditorInput(), mCasEditor.getDocument());
  }

  /**
   * Retrieves the control
   * 
   * @return the control
   */
  @Override
  public Control getControl() {
    return mInstanceComposite;
  }

  /**
   * Adds the following actions to the toolbar: {@link CreateAction} {@link DereferenceAction}
   * {@link DeleteAction}
   * 
   * @param menuManager
   * @param toolBarManager
   * @param statusLineManager
   */
  @Override
  public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
          IStatusLineManager statusLineManager) {
    // create
    Action createAction = new CreateAction();
    createAction.setText("Create");
    createAction.setImageDescriptor(CasEditorPlugin.getTaeImageDescriptor(Images.ADD));
    toolBarManager.add(createAction);

    // delete
    toolBarManager.add(ActionFactory.DELETE.create(getSite().getWorkbenchWindow()));
  }

  /**
   * Sets global action handlers for: delete select all
   * 
   * @param actionBars
   */
  @Override
  public void setActionBars(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), mDeleteAction);

    getSite().getSelectionProvider().addSelectionChangedListener(mDeleteAction);

    actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), mSelectAllAction);

    super.setActionBars(actionBars);
  }

  /**
   * Sets the focus.
   */
  @Override
  public void setFocus() {
    mInstanceComposite.setFocus();
  }

  public void casDocumentChanged(IEditorInput oldInput, ICasDocument oldDocument,
          IEditorInput newInput, ICasDocument newDocument) {
    
    typeCombo.setInput(newDocument.getCAS().getTypeSystem().getType(CAS.TYPE_NAME_TOP),
            newDocument.getCAS().getTypeSystem(), filterTypes);
    
    final IPreferenceStore store = mCasEditor.getCasDocumentProvider().getSessionPreferenceStore(
            mCasEditor.getEditorInput());
    
    Type lastUsedType = newDocument.getType(store.getString(LAST_SELECTED_FS_TYPE));

    if (lastUsedType != null) {
      typeCombo.select(lastUsedType);
    }
    
    if (lastUsedType != null)
      mFSList.setInput(lastUsedType);
  }
}
