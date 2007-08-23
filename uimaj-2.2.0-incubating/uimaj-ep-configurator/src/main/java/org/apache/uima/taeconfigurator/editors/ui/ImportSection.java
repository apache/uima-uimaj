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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;

import org.apache.uima.resource.metadata.Import;
import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.CommonInputDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.ExportImportablePartDialog;
import org.apache.uima.taeconfigurator.files.MultiResourceSelectionDialog;
import org.apache.uima.util.InvalidXMLException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Imports - used by aggregates types type priorities indexes external resource specifications
 */
public abstract class ImportSection extends AbstractSection {

  protected abstract Import[] getModelImportArray();

  protected abstract void setModelImportArray(Import[] newImports);

  protected abstract boolean isValidImport(String title, String message);

  protected abstract void finishImportChangeAction();

  protected abstract String getDescriptionFromImport(String source) throws InvalidXMLException,
          IOException;

  protected abstract boolean isAppropriate(); // if false, don't show section

  protected abstract void clearModelBaseValue(); // used to clear exported value

  protected static final long TABLE_HOVER_REQUERY_TIME = 15000;

  protected TableItem lastTableHoverItem = null;

  protected long lLastTableHoverMillis = -1;

  protected String sLastTableHoverHelp = "";

  protected boolean bDisableToolTipHelp = false;

  private static final String TABLE_INDICATOR_BY_NAME = "By Name";

  private static final String TABLE_INDICATOR_BY_LOCATION = "By Location";

  protected Button addButton;

  private Button removeButton;

  private Button setDataPathButton;

  Table importTable;

  public ImportSection(MultiPageEditor aEditor, Composite parent, String title, String description) {
    super(aEditor, parent, title, description);
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);

    // set up Composite to hold widgets in the section
    Composite sectionClient = newComposite(getSection());
    enableBorders(sectionClient);

    Composite buttonContainer = new2ColumnComposite(sectionClient);
    ((GridData) buttonContainer.getLayoutData()).grabExcessVerticalSpace = false;
    ((GridData) buttonContainer.getLayoutData()).grabExcessHorizontalSpace = false;
    ((GridData) buttonContainer.getLayoutData()).horizontalAlignment = SWT.RIGHT;

    addButton = newPushButton(buttonContainer, "Add...", "Click here to add an import");
    removeButton = newPushButton(buttonContainer, "Remove",
            "Click here to remove the selected import.");
    setDataPathButton = newPushButton(buttonContainer, "Set DataPath",
            "Click here to view or set the data path to use when resolving imports by name.");
    importTable = newTable(sectionClient, SWT.FULL_SELECTION, NO_MIN_HEIGHT);

    newTableColumn(importTable).setText("Kind");
    newTableColumn(importTable).setText("Location/Name");

    importTable.setHeaderVisible(true);
    packTable(importTable);

    // in addition to normal keyup and mouse up:
    importTable.addListener(SWT.MouseHover, this);
    importTable.addListener(SWT.MouseDown, this);

    toolkit.paintBordersFor(sectionClient);
  }

  public void refresh() {
    super.refresh();
    importTable.removeAll();

    if (isAppropriate()) {

      Import[] importItems = getModelImportArray();
      if (importItems != null) {
        for (int i = 0; i < importItems.length; i++) {
          if (importItems[i] != null) {
            TableItem tableItem = new TableItem(importTable, SWT.NONE);
            if (importItems[i].getLocation() != null) {
              tableItem.setText(0, TABLE_INDICATOR_BY_LOCATION);
              tableItem.setText(1, importItems[i].getLocation());
            } else {
              tableItem.setText(0, TABLE_INDICATOR_BY_NAME);
              tableItem.setText(1, importItems[i].getName());
            }
          }
        }
        packTable(importTable);
      }
    }

    enable();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractTableSection#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {

    if (event.widget == addButton) {
      handleAdd();
    } else if (event.widget == removeButton) {
      handleRemove();
    } else if (event.widget == setDataPathButton) {
      handleSetDataPath();
    } else if (event.type == SWT.MouseDown && event.button == 3) {
      handleTableContextMenuRequest(event);
    } else if (event.type == SWT.MouseHover && !bDisableToolTipHelp) {
      handleTableHoverHelp(event);
    } else if (event.type == SWT.KeyUp) {
      if (event.character == SWT.DEL) {
        handleRemove();
      }
    } else if (event.widget == importTable && event.type == SWT.Selection) {
      enable();
    }
  }

  public void handleRemove() {
    int nSelectedIndex = importTable.getSelectionIndex();

    Import[] oldImports = getModelImportArray();

    setModelImportArray((Import[]) Utility.removeElementFromArray(oldImports,
            oldImports[nSelectedIndex], Import.class));

    if (!isValidImport("Error Removing Import", "An error was caused by removing an import.")) {
      setModelImportArray(oldImports);
      // no refresh here: the remove action didn't happen, so leave the GUI alone
      return;
    }

    refresh();
    importTable.setSelection(nSelectedIndex - 1);
    enable();
    finishImportChangeAction();
    setFileDirty();
  }

  private void handleSetDataPath() {
    CommonInputDialog dialog = new CommonInputDialog(
            this,
            "Set DataPath",
            "The DataPath is a series of locations which will be used when looking up imports and external resources.\nEnter a series of absolute path names, separated by the character used to separate classpath names on this platform.",
            CommonInputDialog.ALLOK, CDEpropertyPage.getDataPath(editor.getProject()));
    if (dialog.open() == Window.CANCEL)
      return;
    CDEpropertyPage.setDataPath(editor.getProject(), dialog.getValue());
  }

  private void handleAdd() {
    Shell shell = getSection().getShell();
    MultiResourceSelectionDialog dialog = new MultiResourceSelectionDialog(shell, editor.getFile()
            .getProject().getParent(), "Select one or more descriptor files to import:", editor
            .getFile().getLocation(), editor);
    dialog.setTitle("Import File(s) Selection");
    if (dialog.open() == Window.CANCEL)
      return;
    // results is an array of either IFile or File objects
    // depending on if the file was from the Eclipse Workspace or from the file system
    Object[] results = dialog.getResult();

    if (results == null || results.length == 0)
      return;

    if (!addImports(results, dialog.isImportByName))
      return;

    refresh();
    finishImportChangeAction();
    setFileDirty();

  }

  /**
   * Called with either byLocation non-null or byName non-null Adds multiple (by location) or one
   * (by name)
   * 
   * @param location
   *          objects returned from dialog
   * @param true
   *          if imports should be done by name
   * @return false if any import caused an error, true of all OK
   */
  public boolean addImports(Object[] locations, boolean isByName) {
    Import[] currentImports = getModelImportArray();
    Import imp;

    int nCountCurrentImports = (currentImports == null) ? 0 : currentImports.length;
    int numberToAdd = locations.length;

    Import[] newImports = new Import[nCountCurrentImports + numberToAdd];
    if (null != currentImports)
      System.arraycopy(currentImports, 0, newImports, 0, nCountCurrentImports);

    for (int i = 0; i < locations.length; i++) {
      FileAndShortName fsn = new FileAndShortName(locations[i]);
      imp = createImport(fsn.fileName, isByName);
      if (alreadyImported(imp))
        return false;
      newImports[nCountCurrentImports + i] = imp;
    }

    setModelImportArray(newImports);

    if (!isValidImport(
            "Error Adding Import(s)",
            "An error was caused by adding Import(s); operation cancelled.  Please correct the error and retry.")) {
      setModelImportArray(currentImports);
      return false;
    }
    return true;
  }

  private boolean alreadyImported(Import imp) {
    String currentFileBeingEdited = editor.getFile().getLocation().toString();
    currentFileBeingEdited = editor.getDescriptorRelativePath(currentFileBeingEdited);
    if (currentFileBeingEdited.equals(imp.getLocation())) {
      Utility
              .popMessage(
                      "Error - importing self",
                      MessageFormat
                              .format(
                                      "The import {0} is the same as the current file being edited. A file can''t be imported into itself.",
                                      new Object[] { imp.getLocation() }), MessageDialog.ERROR);
      return true;
    }

    Import[] currentImports = getModelImportArray();
    if (null == currentImports)
      return false;
    for (int i = 0; i < currentImports.length; i++) {
      if (currentImports[i].equals(imp)) {
        Utility.popMessage("Error - duplicate import", MessageFormat.format(
                "The import {0} is already present", new Object[] { null != imp.getName() ? imp
                        .getName() : imp.getLocation() }), MessageDialog.ERROR);
        return true;
      }
    }
    return false;
  }

  public void enable() {
    int nSelectionIndex = importTable.getSelectionIndex();
    boolean addEnable = (this instanceof TypeImportSection) ? (!isAggregate()) : true;

    addButton.setEnabled(addEnable);
    removeButton.setEnabled(nSelectionIndex > -1);
  }

  private void handleTableContextMenuRequest(Event event) {
    TableItem item = importTable.getItem(new Point(event.x, event.y));
    int nSelectedIndex = getIndex(item);
    bDisableToolTipHelp = true;
    requestPopUpOverImport(getModelImportArray()[nSelectedIndex], importTable, event);
    bDisableToolTipHelp = false;
  }

  private void handleTableHoverHelp(Event event) {
    TableItem item = importTable.getItem(new Point(event.x, event.y));
    if (null != item) {
      String sDesc;

      long lCurrentTimeInMillis = System.currentTimeMillis();
      if (item == lastTableHoverItem
              && lCurrentTimeInMillis - lLastTableHoverMillis < TABLE_HOVER_REQUERY_TIME) {
        sDesc = sLastTableHoverHelp;
      } else {
        int itemIndex = (event.y - importTable.getHeaderHeight()) / importTable.getItemHeight();
        String thisFile = item.getText(1);

        sDesc = thisFile + ' ';
        Import[] importItems = getModelImportArray();
        if (itemIndex < 0 || itemIndex >= importItems.length) {
          System.err.println("***ERROR Item index hover out of range" + itemIndex
                  + ", size of array = " + importItems.length);
          System.err.println(this.getClass().getName());
          return;
        }
        Import importItem = getModelImportArray()[itemIndex];
        // if by location, it's relative to the descriptor.
        String absolutePath = editor.getAbsolutePathFromImport(importItem);

        String description = null;
        try {
          description = getDescriptionFromImport(absolutePath);
        } catch (InvalidXMLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        if ((null != description) && (!description.equals(""))) {
          sDesc += "\n\n" + parseToFitInToolTips(description);
        }

        lastTableHoverItem = item;
        lLastTableHoverMillis = System.currentTimeMillis();
        sLastTableHoverHelp = sDesc;
      }

      importTable.setToolTipText(sDesc);
    } else {
      importTable.setToolTipText("");
    }
  }

  /**
   * 
   * @param xmlStartElement
   *          first element exported
   * @param partTemplate
   */
  protected void exportImportablePart(String xmlStartElement, String partTemplate) {
    String xmlEndElement = xmlStartElement.replaceFirst("<", "</");
    ExportImportablePartDialog dialog = new ExportImportablePartDialog(this);
    if (dialog.open() == Window.CANCEL)
      return;
    PrintWriter printWriter = setupToPrintFile(dialog.genFilePath);
    if (null != printWriter) {
      String wholeModel = editor.prettyPrintModel();
      int start = wholeModel.indexOf(xmlStartElement);
      int end = wholeModel.lastIndexOf(xmlEndElement);
      if (start < 0 || end < 0)
        throw new InternalErrorCDE("invalid state");
      start += xmlStartElement.length();
      printWriter.println(MessageFormat.format(partTemplate, new Object[] { dialog.baseFileName,
          wholeModel.substring(start, end) + "\n" }));
      printWriter.close();
      clearModelBaseValue();

      setFileDirty(); // do as soon as file changes, in case later error aborts processing
      Import imp = createImport(dialog.genFilePath, dialog.isImportByName);
      setModelImportArray((Import[]) Utility.addElementToArray(getModelImportArray(), imp,
              Import.class));
      isValidImport("Error Exporting a part and Importing it",
              "An unexpected error was caused by the export operation");
      refresh();
      Object file = editor.getIFileOrFile(dialog.genFilePath);
      if (file instanceof IFile) {
        try {
          IFile ifile = (IFile) file;
          ifile.refreshLocal(1, null);
          ((IFile) file).setPersistentProperty(
                  new QualifiedName(PLUGIN_ID, IMPORTABLE_PART_CONTEXT), editor.getFile()
                          .getLocation().toString());
        } catch (CoreException e) {
          throw new InternalErrorCDE("unexpected exception", e);
        }
      }

    }
  }

}
