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

package org.apache.uima.caseditor.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DefaultColors;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.apache.uima.caseditor.ui.property.TypeSystemLocationPropertyPage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.FileEditorInput;

public class DefaultCasDocumentProvider extends
        org.apache.uima.caseditor.editor.CasDocumentProvider {

  /**
   * This map resolved an opened document to its associated style object id.
   * 
   * The tracking is done in the provider because the document element itself
   * does not has any link to the style object.
   */
  private Map<String, String> documentToTypeSystemMap = new HashMap<String, String>();
  
  private Map<String, EditorAnnotationStatus> sharedEditorStatus = new HashMap<String, EditorAnnotationStatus>();
  
  /**
   * This map resolves a type system to a style.
   * 
   * TODO: Right now styles are not closed, how are they
   * deleted when they are not longer needed ?!
   */
  private Map<String, DotCorpus> styles = new HashMap<String, DotCorpus>();
  
  private String getStyleFileForTypeSystem(String typeSystemFile) {
    int lastSlashIndex = typeSystemFile.lastIndexOf("/");
    
    String styleId = typeSystemFile.substring(0, lastSlashIndex + 1);
    styleId = styleId + ".style-" + typeSystemFile.substring(lastSlashIndex + 1);
    
    return styleId;
  }
  
  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile casFile = fileInput.getFile();

      // Try to find a type system for the CAS file
      // TODO: Change to only use full path
      IFile typeSystemFile = null; 
      
      // First check if a type system is already known or was
      // set by the editor for this specific CAS
      String typeSystemFileString = documentToTypeSystemMap.get(casFile.getFullPath().toPortableString());
      
      if (typeSystemFileString != null)
        typeSystemFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(typeSystemFileString));
      
      // If non was found get it from project
      if (typeSystemFile == null)
        typeSystemFile = TypeSystemLocationPropertyPage.getTypeSystemLocation(casFile.getProject());
      
      if (typeSystemFile != null && typeSystemFile.exists()) {
        
        // Try to load a style file for the type system
        // Should be named: ts file name, prefixed with .style-
        // If it does not exist, create it when it is changed
        // Creating it after the default is changed means that
        // colors could change completely when the a type is
        // added or removed to the type system
        
        IFile styleFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(
                getStyleFileForTypeSystem(typeSystemFile.getFullPath().toPortableString())));
        
        DotCorpus dotCorpus = styles.get(styleFile.getFullPath().toPortableString());
        
        if (dotCorpus == null) {
          if (styleFile.exists()) {
           InputStream styleFileIn = null;;
           try {
             styleFileIn = styleFile.getContents();
             dotCorpus = DotCorpusSerializer.parseDotCorpus(styleFileIn);
           }
           finally {
             if (styleFileIn != null)
              try {
                styleFileIn.close();
              } catch (IOException e) {
                CasEditorPlugin.log(e);
              }
           }
          }
          
          if (dotCorpus == null) {
            dotCorpus = new DotCorpus();
            
            // Initialize colors
            CAS cas = DocumentUimaImpl.getVirginCAS(typeSystemFile);
            TypeSystem ts = cas.getTypeSystem();
            
            Collection<AnnotationStyle> defaultStyles = dotCorpus.getAnnotationStyles();
            
            Collection<AnnotationStyle> newStyles = DefaultColors.assignColors(ts, defaultStyles);
            
            for (AnnotationStyle style : newStyles) {
             dotCorpus.setStyle(style);
            }
          }
          
          styles.put(styleFile.getFullPath().toPortableString(), dotCorpus);
        }
        
        documentToTypeSystemMap.put(casFile.getFullPath().toPortableString(),
                typeSystemFile.getFullPath().toPortableString());

        // TODO:
        // Preferences are bound to the type system
        // Changed in one place, then it should change in all places
        
        CAS cas = DocumentUimaImpl.getVirginCAS(typeSystemFile);
  
        DocumentFormat documentFormat;
  
        // Which file format to use ?
        if (casFile.getName().endsWith("xmi")) {
          documentFormat = DocumentFormat.XMI;
        } else if (casFile.getName().endsWith("xcas")) {
          documentFormat = DocumentFormat.XCAS;
        } else {
          throw new CoreException(new Status(IStatus.ERROR, "org.apache.uima.dev",
                  "Unkown file format!"));
        }
  
        InputStream casIn = casFile.getContents();

        org.apache.uima.caseditor.editor.ICasDocument doc;

        try {
          doc = new DocumentUimaImpl(cas, casIn, documentFormat);
        } finally {
          try {
            casIn.close();
          } catch (IOException e) {
            // Unable to close file after loading it
            //
            // In the current implementation the user
            // does not notice the error and can just
            // edit the file, tough saving it might fail
            // if the io error persists
            
            CasEditorPlugin.log(e);
          }
        }

        AnnotationDocument document = new AnnotationDocument();
        document.setDocument(doc);
        
        elementErrorStatus.remove(element);
        
        return document;
      }
      else {
        
        String message = null;
        
        if (typeSystemFile != null) {
          message = "Cannot find type system!\nPlease place a valid type system in this path:\n" +
                  typeSystemFile.getFullPath().toString();
        }
        else
          message = "Type system is not set, please choose a type system to open the CAS.";
        
        IStatus status = new Status(IStatus.ERROR, "org.apache.uima.dev", 12, message, null);
        
        elementErrorStatus.put(element, status);
      }
    }

    return null;
  }

  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
          boolean overwrite) throws CoreException {

    fireElementStateChanging(element);

    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      if (document instanceof AnnotationDocument) {
        
        AnnotationDocument annotationDocument = (AnnotationDocument) document;
        DocumentUimaImpl documentImpl = (DocumentUimaImpl) annotationDocument.getDocument();
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(40000); 
        documentImpl.serialize(outStream);
        
        InputStream stream = new ByteArrayInputStream(outStream.toByteArray());

        file.setContents(stream, true, false, null);
      }
    }

    // tell everyone that the element changed and is not
    // dirty any longer
    fireElementDirtyStateChanged(element, false);
  }

  private String getTypesystemId(Object element) {
    if (element instanceof FileEditorInput) {
      FileEditorInput editorInput = (FileEditorInput) element;
      return documentToTypeSystemMap.get(editorInput.getFile().getFullPath().toPortableString());
    }
    
    return null;
  }
  
  // get access to style for element
  private DotCorpus getStyle(Object element) {
      String tsId = getTypesystemId(element);
       
      return styles.get(getStyleFileForTypeSystem(tsId));
  }
  
  private void saveStyles(Object element) {
    String styleId = getStyleFileForTypeSystem(getTypesystemId(element));
    
    DotCorpus style = styles.get(styleId);
    
    // serialize ... 
    IFile dotCorpusFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            Path.fromPortableString(styleId));
    
    ByteArrayOutputStream dotCorpusBytes = new ByteArrayOutputStream();
    
    try {
      DotCorpusSerializer.serialize(style, dotCorpusBytes);
    } catch (CoreException e) {
      // will not fail, writing to memory
      CasEditorPlugin.log(e);
    }
    
    try {
      if (dotCorpusFile.exists()) {
        dotCorpusFile.setContents(new ByteArrayInputStream(dotCorpusBytes.toByteArray()),
                true, false, null);
      }
      else {
        dotCorpusFile.create(new ByteArrayInputStream(dotCorpusBytes.toByteArray()),
                true, null);
      }
    }
    catch (CoreException e) {
      // might fail if writing is not possible
      // for some reason
      CasEditorPlugin.log(e);
    }
  }
  
  @Override
  public AnnotationStyle getAnnotationStyle(Object element, Type type) {
    
    if (type == null)
    	throw new IllegalArgumentException("type parameter must not be null!");
    
    DotCorpus dotCorpus = getStyle(element);
    
    return dotCorpus.getAnnotation(type);
  }

  // TODO: Disk must be accessed for every changed annotation style
  // add a second method which can take all changed styles
  @Override
  public void setAnnotationStyle(Object element, AnnotationStyle style) {

    DotCorpus dotCorpus = getStyle(element);
    dotCorpus.setStyle(style);
    
    saveStyles(element); 
  }
  
  @Override
  protected Collection<String> getShownTypes(Object element) {
    DotCorpus dotCorpus = getStyle(element);
    return dotCorpus.getShownTypes();
  }
  
  @Override
  protected void addShownType(Object element, Type type) {
    DotCorpus dotCorpus = getStyle(element);
    dotCorpus.setShownType(type.getName());
    
    saveStyles(element);
  }
  
  @Override
  protected void removeShownType(Object element, Type type) {
    DotCorpus dotCorpus = getStyle(element);
    dotCorpus.removeShownType(type.getName());
    
    saveStyles(element);
  }
  
  @Override
  protected EditorAnnotationStatus getEditorAnnotationStatus(Object element) {
    EditorAnnotationStatus status = sharedEditorStatus.get(getTypesystemId(element));
    
    if (status == null)
      status = new EditorAnnotationStatus(CAS.TYPE_NAME_ANNOTATION, null, CAS.NAME_DEFAULT_SOFA);
    
    return status;
  }

  @Override
  protected void setEditorAnnotationStatus(Object element,
          EditorAnnotationStatus editorAnnotationStatus) {
    sharedEditorStatus.put(getTypesystemId(element), editorAnnotationStatus);
  }
  
  void setTypeSystem(String document, String typeSystem) {
    documentToTypeSystemMap.put(document, typeSystem);
  }
  
  @Override
  public Composite createTypeSystemSelectorForm(final ICasEditor editor,
          Composite parent, IStatus status) {
    
    // Note:
    // If the editor is not active and the user clicks on the button
    // the editor gets activated and an exception is logged
    // on the second click the button is selected
    // How to fix the exception ?!
    // Only tested on OS X Snow Leopard
    
    Composite provideTypeSystemForm = new Composite(parent, SWT.NONE);
    provideTypeSystemForm.setLayout(new GridLayout(1, false));
    Label infoLabel = new Label(provideTypeSystemForm, SWT.NONE);
    infoLabel.setText(status.getMessage());
    Button retryButton = new Button(provideTypeSystemForm, SWT.NONE);
    retryButton.setText("Choose Type System ...");
    retryButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        
        // Open a dialog to let the user choose a type system
        IResource resource = WorkspaceResourceDialog.getWorkspaceResourceElement(Display.getCurrent().getActiveShell(),
                ResourcesPlugin.getWorkspace().getRoot(),
                "Select a Type System", "Please select a Type System:");
        
        if (resource != null) {
          
          FileEditorInput editorInput = (FileEditorInput) editor.getEditorInput();
          setTypeSystem(editorInput.getFile().getFullPath().toPortableString(),
                  resource.getFullPath().toString());
          
          // Now set the input again to open the editor with the
          // specified type system
          editor.reopenEditorWithNewTypeSystem();
        }
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        throw new IllegalStateException("Never be called!");
      }
    });
    
    return provideTypeSystemForm;
  }
}
