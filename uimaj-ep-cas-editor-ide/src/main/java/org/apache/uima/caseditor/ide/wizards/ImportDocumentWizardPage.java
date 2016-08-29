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

package org.apache.uima.caseditor.ide.wizards;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.ide.CasEditorIdePlugin;
import org.apache.uima.caseditor.ide.CasEditorIdePreferenceConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * The main page of the <code>ImportDocumentWizard</code>.
 */
final class ImportDocumentWizardPage extends WizardPage {

  private static final Set<String> defaultEncodings;
  
  static {
    Set<String> encodings = new HashSet<String>();
    
    encodings.add("US-ASCII");
    encodings.add("ISO-8859-1");
    encodings.add("UTF-8");
    encodings.add("UTF-16BE");
    encodings.add("UTF-16LE");
    encodings.add("UTF-16");
    encodings.add(Charset.defaultCharset().displayName());
    
    defaultEncodings = Collections.unmodifiableSet(encodings);
  }
  
  private IPath importDestinationPath;

  private String importEncoding;
  
  private String language;
  
  private SerialFormat documentFormat;
  
  private TableViewer fileTable;

  private IContainer containerElement;

  protected ImportDocumentWizardPage(String pageName,
      IStructuredSelection currentResourceSelection) {
    super(pageName);

    setTitle("Import Text Files");

    // TODO: Pre select the selected foler ..
//    if (!currentResourceSelection.isEmpty()) {
//      if (currentResourceSelection.getFirstElement() instanceof CorpusElement) {
//        containerElement = (IContainer) currentResourceSelection.getFirstElement();
//        importDestinationPath = containerElement.getFullPath();
//      }
//    }
    
    setPageComplete(false);
  }

  private void updatePageState() {
	  
	boolean isEncodingSupported = false;
	
	try {
		isEncodingSupported = Charset.isSupported(importEncoding);
	}
	catch (IllegalCharsetNameException e) {
		// Name of the Charset is incorrect, that means
		// it cannot exist
		
	}
	
	String errorMessage = null;
	if (!isEncodingSupported)
		errorMessage ="Invalid text import encoding!";
	
	// error message is always displayed instead of status message
	// if both are set
	setErrorMessage(errorMessage);
	setMessage("Please select the documents to import.");
	
	
	
    setPageComplete(importDestinationPath != null && 
    		fileTable.getTable().getItemCount() > 0
    		&& isEncodingSupported);
  }

  public void createControl(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));

    fileTable = new TableViewer(composite);
    GridDataFactory.fillDefaults().grab(true, true).span(2, 4).applyTo(fileTable.getControl());

    Button addButton = new Button(composite, SWT.PUSH);
    addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    addButton.setText("Add");
    addButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Opens a file dialog and adds the selected files to the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {

        // open a file dialog
        FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.MULTI);
        fd.setText("Choose text files");
        fd.setFilterExtensions(new String[] { "*.txt;*.rtf", "*.*"});
        fd.setFilterNames(new String[] {"Text Files", "All Files (*)"});
        if (fd.open() != null) {
          for (String fileItem : fd.getFileNames()) {
            fileTable.add(new File(fd.getFilterPath() + File.separator + fileItem));
          }

          updatePageState();
        }
      }
    });

    Button removeButton = new Button(composite, SWT.PUSH);
    removeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    removeButton.setText("Remove");
    removeButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Removes selected elements from the file table viewer.
       */
      @SuppressWarnings("rawtypes")
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection selection = (IStructuredSelection) fileTable.getSelection();

        Iterator seletionIterator = selection.iterator();

        Object selectedElements[] = new Object[selection.size()];

        for (int i = 0; i < selection.size(); i++) {
          selectedElements[i] = seletionIterator.next();
        }

        fileTable.remove(selectedElements);

        updatePageState();
      }
    });

    Button selectAllButton = new Button(composite, SWT.PUSH);
    selectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    selectAllButton.setText("Select All");
    selectAllButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Selects all elements in the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {
        fileTable.getTable().selectAll();
        fileTable.setSelection(fileTable.getSelection());
      }
    });

    Button deselectAllButton = new Button(composite, SWT.PUSH);
    deselectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    deselectAllButton.setText("Deselect All");
    deselectAllButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Deselects all elements in the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {
        fileTable.getTable().deselectAll();
        fileTable.setSelection(fileTable.getSelection());
      }
    });

    // Into Corpus folder 
    Label intoFolderLabel = new Label(composite, SWT.NONE);
    intoFolderLabel.setText("Into folder:");

    final Text corpusText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    corpusText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    if (importDestinationPath != null) {
      corpusText.setText(importDestinationPath.toString());
    }

    Button browseForFolder = new Button(composite, SWT.NONE);
    browseForFolder.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    browseForFolder.setText("Browse");
    browseForFolder.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Opens the corpus folder chooser dialog and shows the chosen dialog in the corpus folder
       * text field.
       */
      public void widgetSelected(SelectionEvent e) {

        final ElementTreeSelectionDialog folderSelectionDialog = new ElementTreeSelectionDialog(
                getShell(), new DecoratingLabelProvider(new WorkbenchLabelProvider(), PlatformUI
                        .getWorkbench().getDecoratorManager().getLabelDecorator()),
                new BaseWorkbenchContentProvider());

        folderSelectionDialog.addFilter(new ContainerElementFilter());

        if (containerElement != null) {
          folderSelectionDialog.setInitialSelection(containerElement);
        }

        folderSelectionDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());

        folderSelectionDialog.setTitle("Choose folder");
        folderSelectionDialog.setMessage("Please choose a folder.");

        folderSelectionDialog.setValidator(new ISelectionStatusValidator() {
          public IStatus validate(Object[] selection) {

            if (selection.length == 1) {
              
              Object selectedElement = selection[0];
              
              if (selectedElement instanceof IAdaptable) {
                Object resourceElement = ((IAdaptable) selectedElement).getAdapter(IResource.class);
                if (resourceElement != null)
                  selectedElement = resourceElement;
              }
              
              if (selectedElement instanceof IContainer)
                return new Status(IStatus.OK, CasEditorPlugin.ID, 0, "", null);
            }

            return new Status(IStatus.ERROR, CasEditorPlugin.ID, 0, "Please select a folder!", null);
          }
        });

        folderSelectionDialog.open();

        Object[] results = folderSelectionDialog.getResult();

        if (results != null && results.length > 0) {
          // validator makes sure that an IContainer or an IAdaptable
          // element which can provide an IContainer is selected
          
          if (results[0] instanceof IContainer) {
            containerElement = (IContainer) results[0];
          }
          else if (results[0] instanceof IAdaptable) {
            IAdaptable adaptableElement = (IAdaptable) results[0];
            
            containerElement = (IContainer) adaptableElement.getAdapter(IResource.class);
          }
          else {
            throw new IllegalStateException("Unexpected selection!");
          }
          
          importDestinationPath = containerElement.getFullPath();

          corpusText.setText(importDestinationPath.toString());

          updatePageState();
        }
      }
    });

    Group importOptions = new Group(composite, SWT.NONE);
    importOptions.setText("Options");
    GridLayout importOptionsGridLayout = new GridLayout();
    importOptionsGridLayout.numColumns = 2;
    importOptions.setLayout(importOptionsGridLayout);
    GridData importOptionsGridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
    importOptionsGridData.horizontalSpan = 3;
    importOptions.setLayoutData(importOptionsGridData);
    
    Label languageLabel = new Label(importOptions, SWT.NONE);
    languageLabel.setText("Language:");
    
    final IPreferenceStore store = CasEditorIdePlugin.getDefault().getPreferenceStore();
    
    final Text languageText = new Text(importOptions, SWT.BORDER);
    languageText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    language = store.getString(
            CasEditorIdePreferenceConstants.CAS_IMPORT_WIZARD_LAST_USED_LANG);
    languageText.setText(language);
    languageText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent e) {
        language = languageText.getText();
        store.setValue(CasEditorIdePreferenceConstants.CAS_IMPORT_WIZARD_LAST_USED_LANG, language);
      }
    });
    
    // Text file encoding
    Label encodingLabel = new Label(importOptions, SWT.NONE);
    encodingLabel.setText("Text Encoding:");
    
    // combo box ...
    final Combo encodingCombo = new Combo(importOptions, SWT.NONE);
    encodingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    
    Set<String> charsets = new HashSet<String>();
    charsets.addAll(defaultEncodings);
    
    String lastUsedEncodingsString = 
            store.getString(CasEditorIdePreferenceConstants.CAS_IMPORT_WIZARD_LAST_USED_ENCODINGS);
    
    String lastUsedEncodings[] = lastUsedEncodingsString.split(CasEditorIdePreferenceConstants.STRING_DELIMITER);
    charsets.addAll(Arrays.asList(lastUsedEncodings));

    if (lastUsedEncodings.length > 0) {
      importEncoding = lastUsedEncodings[0];
    }
    else {
      importEncoding = Charset.defaultCharset().displayName();
    }
    
    encodingCombo.setItems(charsets.toArray(new String[charsets.size()]));
    encodingCombo.setText(importEncoding);
    encodingCombo.addSelectionListener(new SelectionListener() {
  		
  		public void widgetSelected(SelectionEvent e) {
  			importEncoding = encodingCombo.getText();
  			updatePageState();
  		}
  		
  		public void widgetDefaultSelected(SelectionEvent e) {
  		}
  	});
    
    encodingCombo.addKeyListener(new KeyListener() {
		
  		public void keyReleased(KeyEvent e) {
  			importEncoding = encodingCombo.getText();
  			updatePageState();
  		}
  		
  		public void keyPressed(KeyEvent e) {
  		}
  	});
    
    Label casFormatLabel = new Label(importOptions, SWT.NONE);
    casFormatLabel.setText("Cas Format:");
  
    final Combo casFormatCombo = new Combo(importOptions, SWT.READ_ONLY);
    casFormatCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    SerialFormat[] values = SerialFormat.values();
    String[] stringValues = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      stringValues[i] = values[i].toString();
    }
    casFormatCombo.setItems(stringValues);
    documentFormat = SerialFormat.XMI;
    casFormatCombo.select(0);
    
    casFormatCombo.addSelectionListener(new SelectionListener() {
  		
  		public void widgetSelected(SelectionEvent e) {
  			documentFormat = SerialFormat.valueOf(casFormatCombo.getText());
  		}
  		
  		public void widgetDefaultSelected(SelectionEvent e) {
  		}
  	});
    
    updatePageState();
    
    setControl(composite);
  }

  /**
   * Retrieves the import destination path.
   *
   * @return the path or null if none
   */
  IPath getImportDestinationPath() {
    return importDestinationPath;
  }

  List<File> getFilesToImport() {

    List<File> files = new ArrayList<File>(fileTable.getTable().getItemCount());

    for (int i = 0; i < fileTable.getTable().getItemCount(); i++) {
      files.add((File) fileTable.getElementAt(i));
    }

    return files;
  }
  
  String getTextEncoding() {
	  return importEncoding;
  }
  
  String getLanguage() {
    return language;
  }
  
  SerialFormat getCasFormat() {
	  return documentFormat;
  }
}
