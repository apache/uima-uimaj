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

package org.apache.uima.caseditor.editor.editview;

import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.Images;
import org.apache.uima.caseditor.editor.AnnotationDocument;
import org.apache.uima.caseditor.editor.ArrayValue;
import org.apache.uima.caseditor.editor.CasEditorError;
import org.apache.uima.caseditor.editor.FeatureStructureSelection;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.apache.uima.caseditor.editor.editview.validator.CellEditorValidatorFacotory;
import org.apache.uima.caseditor.editor.util.FeatureStructureTransfer;
import org.apache.uima.caseditor.editor.util.Primitives;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;

/**
 * The {@link EditViewPage} provides basic editing support for {@link FeatureStructure}s.
 * It shows always the selected {@link FeatureStructure}, but for editing a certain
 * {@link FeatureStructure} can be pinned to the view (selection updates disabled).
 *
 * For editing the {@link Feature}s of the {@link FeatureStructure} are shown in
 * a tree with two columns. The values of the {@link Feature} can be changed with
 * cell editors.
 *
 * Note: Lists type are currently not supported
 */
final class EditViewPage extends Page implements ISelectionListener {

  private final class ValueEditingSupport extends EditingSupport {
    private ValueEditingSupport(ColumnViewer viewer) {
      super(viewer);
    }

    @Override
    protected boolean canEdit(Object element) {

      if (element instanceof FeatureValue) {
        FeatureValue value = (FeatureValue) element;

        return value.getFeature().getRange().isPrimitive();
      }
      else if (element instanceof ArrayValue) {

        ArrayValue value = (ArrayValue) element;

        FeatureStructure arrayFS = value.getFeatureStructure();

        if (arrayFS instanceof ArrayFS) {
          return false;
        }
        else if (arrayFS instanceof CommonArrayFS ||
                arrayFS instanceof StringArrayFS) {
          return true;
        }
        else {
          throw new CasEditorError("Unkown array type");
        }
      }
      else {
        throw new CasEditorError("Unkown element type!");
      }
    }

    @Override
    protected CellEditor getCellEditor(Object element) {

      if (element instanceof FeatureValue) {
        FeatureValue value = (FeatureValue) element;

        if (value.getFeature().getRange().isPrimitive()) {

          CellEditor editor;

          if (value.getFeature().getRange().getName().equals(CAS.TYPE_NAME_BOOLEAN)) {
            editor = new ComboBoxCellEditor(viewer.getTree(), new String[]{"false", "true"},
                    SWT.READ_ONLY);
          }
          else {
            editor = new TextCellEditor(viewer.getTree());
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Primitives
                    .getPrimitiveClass(value.getFeature())));
          }

          return editor;
        }
        else {
          return null;
        }
      } else if (element instanceof ArrayValue) {

        ArrayValue arrayValue = (ArrayValue) element;

        FeatureStructure arrayFS = arrayValue.getFeatureStructure();

        CellEditor editor;

        if (arrayFS instanceof BooleanArrayFS) {
          editor = new ComboBoxCellEditor(viewer.getTree(), new String[]{"false", "true"},
                  SWT.READ_ONLY);
          editor.setStyle(SWT.READ_ONLY);
        }
        else {
          editor = new TextCellEditor(viewer.getTree());

          if (arrayFS instanceof ByteArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Byte.class));
          }
          else if (arrayFS instanceof ShortArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Short.class));
          }
          else if (arrayFS instanceof IntArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Integer.class));
          }
          else if (arrayFS instanceof LongArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Long.class));
          }
          else if (arrayFS instanceof FloatArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Float.class));
          }
          else if (arrayFS instanceof DoubleArrayFS) {
            editor.setValidator(CellEditorValidatorFacotory.createValidator(Double.class));
          }
          else if (arrayFS instanceof StringArrayFS) {
            // no validator needed
          }
          else {
            throw new CasEditorError("Unkown array type: " + arrayFS.getClass().getName());
          }
        }

        return editor;
      }
      else {
        throw new CasEditorError("Unknown element type: " + element.getClass().getName());
      }
    }

    private int booleanToInt(boolean value) {
      if (value) {
        return 1;
      }
      else {
        return 0;
      }
    }

    private boolean intToBoolean(int value) {
      return value == 1;
    }

    @Override
    protected Object getValue(Object element) {

      if (element instanceof FeatureValue) {
        FeatureValue featureValue = (FeatureValue) element;

        // if not a boolean return string value,
        // otherwise return boolean number
        if (!featureValue.getFeature().getRange().getName().equals(
                CAS.TYPE_NAME_BOOLEAN)) {
          return featureValue.getFeatureStructure()
            .getFeatureValueAsString(featureValue.getFeature());
        }
        else {
          // for booleans
          return booleanToInt(featureValue.getFeatureStructure().
              getBooleanValue(featureValue.getFeature()));
        }

      }
      else if (element instanceof ArrayValue) {
          ArrayValue value = (ArrayValue) element;

          // if not a boolean array return string value
          if (!(value.getFeatureStructure() instanceof BooleanArrayFS)) {
            return value.get().toString();
          }
          else {
            return booleanToInt((Boolean) value.get());
          }
      }
      else {
        throw new CasEditorError("Unkown element type!");
      }
    }

    @Override
    protected void setValue(Object element, Object value) {

      // if value is null, there was an invalid input
      if (value != null) {
        if (element instanceof FeatureValue) {

          FeatureValue featureValue = (FeatureValue) element;

          // for all other than boolean values
          if (!featureValue.getFeature().getRange().getName().equals(
                  CAS.TYPE_NAME_BOOLEAN)) {
            if (featureValue.getFeature().getRange().isPrimitive()) {

              // TODO: try to prevent setting of invalid annotation span values

              featureValue.getFeatureStructure().setFeatureValueFromString(featureValue.getFeature(),
                      (String) value);
            }
          }
          else {
            featureValue.getFeatureStructure().setBooleanValue(featureValue.getFeature(),
                    intToBoolean((Integer) value));
          }
          document.update(featureValue.getFeatureStructure());

          viewer.update(element, null);

        } else if (element instanceof ArrayValue) {

          ArrayValue arrayValue = (ArrayValue) element;

          if (!(arrayValue.getFeatureStructure() instanceof BooleanArrayFS)) {
            arrayValue.set((String) value);
          }
          else {
            arrayValue.set(Boolean.toString(
                    intToBoolean((Integer) value)).toString());
          }

          document.update(arrayValue.getFeatureStructure());

        } else {
          throw new CasEditorError("Unkown element type");
        }
      }
    }
  }

  final class DeleteFeatureStructureValue extends BaseSelectionListenerAction {

    protected DeleteFeatureStructureValue() {
      super("Delete");

      setEnabled(false);
    }

    @Override
    public void run() {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

      Object element = selection.getFirstElement();

      if (element instanceof FeatureValue) {
        FeatureValue featureValue = (FeatureValue) element;

        if (!featureValue.getFeature().getRange().isPrimitive()) {
          featureValue.getFeatureStructure().setFeatureValue(featureValue.getFeature(), null);

          document.update(featureValue.getFeatureStructure());
        }
      } else if (element instanceof ArrayValue) {
          ArrayValue arrayValue = (ArrayValue) element;

          ArrayFS array = (ArrayFS) arrayValue.getFeatureStructure();

          array.set(arrayValue.slot(), null);

          document.update(array);
      }
    }

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {

      boolean result = false;

      if (selection.size() == 1) {
        if (selection.getFirstElement() instanceof FeatureValue) {
          FeatureValue featureValue = (FeatureValue) selection.getFirstElement();

          result = !featureValue.getFeature().getRange().isPrimitive() &&
              featureValue.getFeatureStructure().getFeatureValue(featureValue.getFeature()) != null;
        }
        else if (selection.getFirstElement() instanceof ArrayValue) {
          ArrayValue arrayValue = (ArrayValue) selection.getFirstElement();

            if (arrayValue.getFeatureStructure() instanceof ArrayFS) {
              ArrayFS array = (ArrayFS) arrayValue.getFeatureStructure();

              result = array.get(arrayValue.slot()) != null;
            }
        }
      }

      return result;

    }
  }

  private final class CreateFeatureStructrueValue extends BaseSelectionListenerAction {

    protected CreateFeatureStructrueValue() {
      super("Create");

      setEnabled(false);
    }


    FeatureStructure createFS(Type type, int arraySize) {

      FeatureStructure fs;

      if (!type.isArray()) {
        fs = document.getCAS().createFS(type);
      }
      else {

        if (type.getName().equals(CAS.TYPE_NAME_BOOLEAN_ARRAY)) {
          fs = document.getCAS().createBooleanArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_BYTE_ARRAY)) {
          fs = document.getCAS().createByteArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_SHORT_ARRAY)) {
          fs = document.getCAS().createShortArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_INTEGER_ARRAY)) {
          fs = document.getCAS().createIntArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_LONG_ARRAY)) {
          fs = document.getCAS().createLongArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_FLOAT_ARRAY)) {
          fs = document.getCAS().createFloatArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_DOUBLE_ARRAY)) {
          fs = document.getCAS().createDoubleArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_STRING_ARRAY)) {
          fs = document.getCAS().createStringArrayFS(arraySize);
        } else if (type.getName().equals(CAS.TYPE_NAME_FS_ARRAY)) {
          fs = document.getCAS().createArrayFS(arraySize);
        } else {
          throw new CasEditorError("Unkown array type!");
        }
      }

      return fs;
    }

    @Override
    public void run() {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

      if (selection.getFirstElement() instanceof FeatureValue) {
        FeatureValue featureValue = (FeatureValue) selection.getFirstElement();

        FeatureStructure newValue;

        Type fsSuperType = featureValue.getFeature().getRange();

        if (!fsSuperType.isArray()) {
          List<Type> subTypes =
              document.getCAS().getTypeSystem().getProperlySubsumedTypes(fsSuperType);

          Type typeToCreate;
          int arraySize = -1;

          if (subTypes.size() == 0) {
            typeToCreate = fsSuperType;
          }
          else {
             CreateFeatureStructureDialog createFsDialog =
                 new CreateFeatureStructureDialog(Display.getCurrent()
                         .getActiveShell(), fsSuperType, document.getCAS().getTypeSystem());

             int returnCode = createFsDialog.open();

             if (returnCode == IDialogConstants.OK_ID) {
               typeToCreate = createFsDialog.getType();
               arraySize = createFsDialog.getArraySize();
             }
             else {
               return;
             }
          }

          newValue = createFS(typeToCreate, arraySize);

          document.addFeatureStructure(newValue);
        } else {
          Type arrayType = featureValue.getFeature().getRange();

          CreateFeatureStructureDialog createArrayDialog = new CreateFeatureStructureDialog(Display.getCurrent()
                  .getActiveShell(), arrayType, document.getCAS().getTypeSystem());

          int returnCode = createArrayDialog.open();

          if (returnCode == IDialogConstants.OK_ID) {
            newValue = createFS(arrayType, createArrayDialog.getArraySize());
          } else {
            return;
          }
        }

        featureValue.getFeatureStructure().setFeatureValue(featureValue.getFeature(), newValue);
        document.update(featureValue.getFeatureStructure());
      }
      else if (selection.getFirstElement() instanceof ArrayValue) {
        ArrayValue value = (ArrayValue) selection.getFirstElement();


        TypeSystem typeSystem = document.getCAS().getTypeSystem();

        CreateFeatureStructureDialog createFsDialog =
          new CreateFeatureStructureDialog(Display.getCurrent()
                  .getActiveShell(), typeSystem.getType(CAS.TYPE_NAME_TOP),
                  typeSystem);

        int returnCode = createFsDialog.open();

        if (returnCode == IDialogConstants.OK_ID) {

          FeatureStructure fs = createFS(createFsDialog.getType(), createFsDialog.getArraySize());

          ArrayFS array = (ArrayFS) value.getFeatureStructure();

          array.set(value.slot(), fs);

          document.update(value.getFeatureStructure());
        }
      }
    }

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {

      boolean result = false;

      if (selection.size() == 1) {
        if (selection.getFirstElement() instanceof FeatureValue) {
          FeatureValue featureValue = (FeatureValue) selection.getFirstElement();

          result = !featureValue.getFeature().getRange().isPrimitive() &&
              featureValue.getFeatureStructure().getFeatureValue(featureValue.getFeature()) == null;
        }
        else if (selection.getFirstElement() instanceof ArrayValue) {
          ArrayValue value = (ArrayValue) selection.getFirstElement();

          if (value.getFeatureStructure() instanceof ArrayFS) {
            ArrayFS array = (ArrayFS) value.getFeatureStructure();

            if (array.get(value.slot()) == null) {
              result = true;
            }
          }
        }
      }

      return result;
    }
  }

  private static final class PinAction extends Action {
    PinAction() {
      super("PinAction", IAction.AS_CHECK_BOX);
    }
  }

  private TreeViewer viewer;

  private AnnotationDocument document;

  private PinAction pinAction;

  private final EditView editView;

  EditViewPage(EditView editView, AnnotationDocument document) {

	if (editView == null || document == null)
        throw new IllegalArgumentException("Parameters must not be null!");

    this.editView = editView;
    this.document = document;
  }

  @Override
  public void createControl(Composite parent) {

    Tree tree = new Tree(parent, SWT.NONE);

    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);

    viewer = new TreeViewer(tree);

    TreeViewerColumn featureColumn = new TreeViewerColumn(viewer, SWT.LEFT);
    featureColumn.getColumn().setText("Feature");
    featureColumn.getColumn().setWidth(100);

    featureColumn.setLabelProvider(new FeatureColumnLabelProvider());

    TreeViewerColumn valueColumn = new TreeViewerColumn(viewer, SWT.LEFT);
    valueColumn.getColumn().setText("Value");
    valueColumn.getColumn().setWidth(100);

    valueColumn.setLabelProvider(new ValueColumnLabelProvider());

    valueColumn.setEditingSupport(new ValueEditingSupport(viewer));


    FeatureStructureContentProvider contentProvider =
        new FeatureStructureContentProvider(document);

    viewer.setContentProvider(contentProvider);

    viewer.setInput(null);
    document.addChangeListener(contentProvider);

    Transfer[] typesDropSupport = new Transfer[] { FeatureStructureTransfer.getInstance() };

    viewer.addDropSupport(DND.DROP_COPY, typesDropSupport, new DropTargetListener() {

      public void dragEnter(DropTargetEvent event) {
        // only the FeatureStructureTransfer is supported
        // set currentTransferType to FeatureStructureTransfer, if possible
        for (TransferData transferData : event.dataTypes) {
          if (FeatureStructureTransfer.getInstance().isSupportedType(transferData)) {
            event.currentDataType = transferData;
            break;
          }
        }
      }

      public void dragLeave(DropTargetEvent event) {
      }

      public void dragOperationChanged(DropTargetEvent event) {
      }

      public void dragOver(DropTargetEvent event) {

        // TODO: check range type during drag over, like its done in drop()

        if (FeatureStructureTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.detail = DND.DROP_COPY;
        } else {
          event.detail = DND.DROP_NONE;
        }
      }

      public void drop(DropTargetEvent event) {
        if (FeatureStructureTransfer.getInstance().isSupportedType(event.currentDataType)) {

          event.detail = DND.DROP_NONE;

          Widget tableItem = event.item;

          if (tableItem != null) {

            if (tableItem.getData() instanceof FeatureValue) {

              // this can fail
              FeatureValue value = (FeatureValue) tableItem.getData();

              Type range = value.getFeature().getRange();

              FeatureStructure dragFeatureStructure = (FeatureStructure) event.data;

              if (range.equals(dragFeatureStructure.getType())) {

                FeatureStructure target = value.getFeatureStructure();

                target.setFeatureValue(value.getFeature(), dragFeatureStructure);

                document.update(target);

                event.detail = DND.DROP_COPY;
              }
            } else if (tableItem.getData() instanceof ArrayValue) {
              ArrayValue value = (ArrayValue) tableItem.getData();

              if (value.getFeatureStructure() instanceof ArrayFS) {

                ArrayFS array = (ArrayFS) value.getFeatureStructure();

                array.set(value.slot(), (FeatureStructure) event.data);

                document.update(array);

                event.detail = DND.DROP_COPY;
              }

            } else {
              throw new CasEditorError("Unkown item type!");
            }
          }
        }
      }

      public void dropAccept(DropTargetEvent event) {
      }
    });


    DragSource source = new DragSource(viewer.getTree(), DND.DROP_COPY);

    source.setTransfer(new Transfer[] { FeatureStructureTransfer.getInstance() });

    source.addDragListener(new DragSourceListener() {
      TreeItem dragSourceItem = null;

      public void dragStart(DragSourceEvent event) {

        event.doit = false;

        TreeItem[] selection = viewer.getTree().getSelection();

        if (selection.length > 0) {

          dragSourceItem = selection[0];
          IAdaptable adaptable = (IAdaptable) dragSourceItem.getData();

          if (adaptable.getAdapter(FeatureStructure.class) != null) {
            event.doit = true;
          }
        }
      }

      public void dragSetData(DragSourceEvent event) {
        IAdaptable adaptable = (IAdaptable) dragSourceItem.getData();

        event.data = adaptable.getAdapter(FeatureStructure.class);
      }

      public void dragFinished(DragSourceEvent event) {
        // not needed
      }
    });

    // do this after viewer is ready to be used
    getSite().setSelectionProvider(viewer);
    getSite().getPage().addSelectionListener(this);
  }

  /**
   * Retrieves the main control of the edit view.
   */
  @Override
  public Control getControl() {
    return viewer.getControl();
  }

  @Override
  public void setFocus() {
  }

  @Override
  public void init(IPageSite pageSite) {
    super.init(pageSite);

    // pin action
    pinAction = new PinAction();

    pinAction.setText("Pin");
    pinAction.setImageDescriptor(CasEditorPlugin.getTaeImageDescriptor(Images.PIN));
    pageSite.getActionBars().getToolBarManager().add(pinAction);

  }

  @Override
  public void setActionBars(IActionBars actionBars) {
    CreateFeatureStructrueValue createAction = new CreateFeatureStructrueValue();
    createAction.setImageDescriptor(CasEditorPlugin.getTaeImageDescriptor(Images.ADD));
    getSite().getActionBars().getToolBarManager().add(createAction);

    // TODO: setActionBars is depreciated, but registration of change listener
    // does not work in init method
    getSite().getSelectionProvider().addSelectionChangedListener(createAction);

    // delete action
    DeleteFeatureStructureValue deleteAction = new DeleteFeatureStructureValue();
    getSite().getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);

    getSite().getSelectionProvider().addSelectionChangedListener(deleteAction);

    getSite().getActionBars().getToolBarManager().add(
            ActionFactory.DELETE.create(getSite().getWorkbenchWindow()));
  }

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {

    if (selection instanceof IStructuredSelection) {

      FeatureStructureSelection fsSelection = new FeatureStructureSelection(
              (IStructuredSelection) selection);

      if (fsSelection.size() == 1 && !pinAction.isChecked()) {

        // filter out selection which are cause by this view itself
        if (editView != part) {
          viewer.setInput(fsSelection.toList().get(0));
        }
      }
    }
  }

  @Override
  public void dispose() {
    getSite().getPage().removeSelectionListener(this);
  }
}