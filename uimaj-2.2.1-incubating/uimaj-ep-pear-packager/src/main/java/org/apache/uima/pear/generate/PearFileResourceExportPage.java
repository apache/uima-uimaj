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

package org.apache.uima.pear.generate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.pear.PearPlugin;
import org.apache.uima.pear.insd.edit.InsdConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerSorter;

/**
 * 
 * Wizard page for exporting resource to a PEAR file
 */
public class PearFileResourceExportPage extends WizardPage implements InsdConstants {

  /** Preference store key for the last pear file export location */
  protected static final String PEAR_FILE = "pear_file"; //$NON-NLS-1$

  /** Plugins preference store */
  protected final IPreferenceStore fPreferenceStore;

  /** Folder or Project that contains the Pear structure */
  protected final IContainer fCurrentContainer;

  /** Initial selection for the TreeViewer */
  protected final IStructuredSelection fSelection;

  /** TreeViewer that shows resources to export */
  protected ContainerCheckedTreeViewer fTreeViewer;

  /** Text input for the */
  protected Text fDestinationFileInput;

  /** Checkbox for choosing whether pear file should be compressed */
  protected Button fCompressCheckbox;

  /**
   * constructor
   * 
   * @param selection
   *          Initial selection for the export TreeViewer
   * @param currentContainer
   *          Container (project or folder) with Pear structure
   */
  public PearFileResourceExportPage(final IStructuredSelection selection,
          final IContainer currentContainer) {
    super("pearFileResourceExportPage"); //$NON-NLS-1$
    final PearPlugin plugin = PearPlugin.getDefault();
    fPreferenceStore = plugin.getPreferenceStore();
    setTitle(PearExportMessages.getString("PearExport.exportTitle")); //$NON-NLS-1$
    setDescription(PearExportMessages.getString("PearExport.description")); //$NON-NLS-1$
    fCurrentContainer = currentContainer;
    fSelection = selection;
  }

  /**
   * Update buttons and messages
   */
  protected void pageStateChanged() {
    // check whether the metadata folder is included for export
    if (!isMetadataIncluded()) {
      setErrorMessage(PearExportMessages.getString("PearExport.metadataRequired")); //$NON-NLS-1$
      setPageComplete(false);
      return;
    }

    // check whether a directory is selected to export to
    final String filename = getDestinationValue();
    if (new File(filename).isDirectory()) {
      setErrorMessage(PearExportMessages.getString("PearFileResourceExportPage.DirectorySelected")); //$NON-NLS-1$
      setPageComplete(false);
      return;
    }

    // check whether a file is selected to export to
    if (filename == null || filename.trim().equals("")) { //$NON-NLS-1$
      setErrorMessage(null);
      setMessage(PearExportMessages.getString("PearFileResourceExportPage.SelectFile"), //$NON-NLS-1$
              IMessageProvider.INFORMATION);
      setPageComplete(false);
      return;
    }

    // If the file to export to exists, display an info stating that
    if (new File(filename).exists()) {
      setErrorMessage(null);
      setMessage(PearExportMessages.format(PearExportMessages
              .getString("PearFileResourceExportPage.FileExistsInfo"), //$NON-NLS-1$
              new Object[] { filename }), IMessageProvider.INFORMATION);
      setPageComplete(true);
      return;
    }

    // Otherwise, remove all messages
    setMessage(null);
    setErrorMessage(null);
    setPageComplete(true);
  }

  /**
   * @return <code>true</code> if all files in the metadata folder are selected for export or the
   *         members cannot be determined, <code>false</code> otherwise
   */
  protected boolean isMetadataIncluded() {
    final Object[] grayed = fTreeViewer.getGrayedElements();
    if (isMetadataIncluded(grayed)) {
      return false;
    }

    final Object[] checked = fTreeViewer.getCheckedElements();
    return isMetadataIncluded(checked);
  }

  /**
   * @param checked
   * @return <code>true</code> if all files in the metadata folder are selected for export or the
   *         members cannot be determined, <code>false</code> otherwise
   */
  private boolean isMetadataIncluded(final Object[] checked) {
    for (int i = 0; i < checked.length; ++i) {
      if (checked[i] instanceof IAdaptable) {
        final IAdaptable adaptable = (IAdaptable) checked[i];
        final IResource resource = (IResource) adaptable.getAdapter(IResource.class);
        if (resource != null && "metadata".equals(resource.getName())) { //$NON-NLS-1$
          // ContainerCheckedTreeViewer reports it as checked if _any_
          // element is checked
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Opens a file selection dialog to select a pear file as export location and sets the chosen
   * value to the input field
   */
  protected void handleDestinationBrowseButtonPressed() {
    final FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
    dialog.setFilterExtensions(new String[] { "*.pear", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
    dialog.setText(PearExportMessages.getString("PearExport.selectDestinationTitle")); //$NON-NLS-1$
    final String destination = getDestinationValue();
    final int lastDirectoryIndex = destination.lastIndexOf(File.separator);
    if (lastDirectoryIndex != -1) {
      dialog.setFilterPath(destination.substring(0, lastDirectoryIndex));
    }

    final String selectedFileName = dialog.open();
    if (selectedFileName != null) {
      fDestinationFileInput.setText(selectedFileName);
      saveDestinationValue(selectedFileName);
    }
  }

  /**
   * Stores the Pear filename in the preference store
   */
  protected void saveDestinationValue(final String filename) {
    fPreferenceStore.setValue(PEAR_FILE, filename);
    fPreferenceStore.needsSaving();
  }

  /**
   * @return The value of the Pear file export destination as chosen by the user, or the last used
   *         one if the widget was not created yet
   */
  protected String getDestinationValue() {
    if (fDestinationFileInput != null) {
      return fDestinationFileInput.getText();
    }

    return fPreferenceStore.getString(PEAR_FILE);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(final Composite parent) {
    final Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout());
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

    createSourceControl(container);
    createDestinationControl(container);
    createOptionsGroup(container);

    pageStateChanged();

    setControl(container);
  }

  /**
   * Create the options group with the compression checkbox
   * 
   * @param parent
   *          the parent composite
   */
  protected void createOptionsGroup(final Composite parent) {
    final Group group = new Group(parent, SWT.NONE);
    group.setText(PearExportMessages.getString("PearFileResourceExportPage.Options")); //$NON-NLS-1$
    group.setLayout(new GridLayout());
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

    fCompressCheckbox = new Button(group, SWT.CHECK);
    fCompressCheckbox.setSelection(true);
    fCompressCheckbox.setText(PearExportMessages
            .getString("PearFileResourceExportPage.CompressContents")); //$NON-NLS-1$
  }

  /**
   * Create the TreeViewer for selection of files to export in the Pear file and select/deselect all
   * buttons
   * 
   * @param parent
   *          the parent composite
   */
  protected void createSourceControl(final Composite parent) {
    final Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout());
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

    fTreeViewer = createTreeViewer(container);
    fTreeViewer.setInput(fCurrentContainer);
    fTreeViewer.setCheckedElements(fSelection.toArray());
    fTreeViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(final CheckStateChangedEvent event) {
        pageStateChanged();
      }
    });

    final Composite buttonsComposite = new Composite(container, SWT.NONE);
    buttonsComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

    final Button selectAllButton = new Button(buttonsComposite, SWT.PUSH);
    selectAllButton.setText(PearExportMessages.getString("PearFileResourceExportPage.SelectAll")); //$NON-NLS-1$
    selectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        setAllChecked(fTreeViewer.getTree().getItems(), true);
        pageStateChanged(); // above doesn't trigger a checkStateChanged
        // event
      }
    });

    final Button deselectAllButton = new Button(buttonsComposite, SWT.PUSH);
    deselectAllButton.setText(PearExportMessages
            .getString("PearFileResourceExportPage.DeselectAll")); //$NON-NLS-1$
    deselectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        setAllChecked(fTreeViewer.getTree().getItems(), false);
        pageStateChanged(); // above doesn't trigger a checkStateChanged
        // event
      }
    });
  }

  /**
   * @param items
   *          A set of TreeItems that should be (un)checked, including their children
   * @param checked
   *          <code>true</code> to check all items, <code>false</code> to uncheck all items
   */
  protected void setAllChecked(final TreeItem[] items, final boolean checked) {
    for (int i = 0; i < items.length; i++) {
      items[i].setChecked(checked);
      final TreeItem[] children = items[i].getItems();
      setAllChecked(children, checked);
    }
  }

  /**
   * Creates the Pear file export destination controls, i.e. label, input field and browse button
   * 
   * @param parent
   *          the parent composite
   */
  protected void createDestinationControl(final Composite parent) {
    final Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(3, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

    final Label toPearFileLabel = new Label(container, SWT.NONE);
    toPearFileLabel.setText(PearExportMessages.getString("PearExport.destinationLabel")); //$NON-NLS-1$

    fDestinationFileInput = new Text(container, SWT.BORDER);
    fDestinationFileInput.setText(fPreferenceStore.getString(PEAR_FILE));
    fDestinationFileInput.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
    fDestinationFileInput.addModifyListener(new ModifyListener() {
      public void modifyText(final ModifyEvent e) {
        pageStateChanged();
        saveDestinationValue(fDestinationFileInput.getText());
      }
    });

    final Button destinationBrowseButton = new Button(container, SWT.PUSH);
    destinationBrowseButton.setText(PearExportMessages
            .getString("PearFileResourceExportPage.Browse")); //$NON-NLS-1$
    destinationBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent e) {
        handleDestinationBrowseButtonPressed();
      }
    });
  }

  /**
   * @param parent
   *          the parent composite
   * @return TreeViewer that shows uses Workbench Content- and LabelProvider
   */
  protected ContainerCheckedTreeViewer createTreeViewer(Composite parent) {
    final ContainerCheckedTreeViewer treeViewer = new ContainerCheckedTreeViewer(parent);

    final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.heightHint = 150;
    treeViewer.getTree().setLayoutData(gridData);

    treeViewer.setContentProvider(new WorkbenchContentProvider());
    treeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    treeViewer.setSorter(new WorkbenchViewerSorter());

    return treeViewer;
  }

  /**
   * @return An {@link IRunnableWithProgress} that will export the files chosen in the TreeViewer to
   *         the file chosen in the destination input field when run
   */
  public IRunnableWithProgress getExportRunnable() {
    final List files = new ArrayList();
    final Object[] checked = fTreeViewer.getCheckedElements();
    for (int i = 0; i < checked.length; ++i) {
      if (checked[i] instanceof IAdaptable) {
        final IAdaptable adaptable = (IAdaptable) checked[i];
        final IResource resource = (IResource) adaptable.getAdapter(IResource.class);
        if (resource != null && resource.getType() == IResource.FILE) {
          files.add(resource);
        }
      }
    }

    final IFile[] exports = new IFile[files.size()];
    for (int i = 0; i < files.size(); ++i) {
      exports[i] = (IFile) files.get(i);
    }

    return new PearExportOperation(exports, fCurrentContainer, getDestinationValue(),
            fCompressCheckbox.getSelection());
  }
}
