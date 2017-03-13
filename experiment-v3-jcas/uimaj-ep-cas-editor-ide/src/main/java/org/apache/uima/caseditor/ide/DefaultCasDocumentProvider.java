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

package org.apache.uima.caseditor.ide;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DefaultColors;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.caseditor.editor.CasDocumentProvider;
import org.apache.uima.caseditor.editor.DocumentUimaImpl;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.apache.uima.caseditor.editor.ICasEditor;
import org.apache.uima.caseditor.ide.searchstrategy.ITypeSystemSearchStrategy;
import org.apache.uima.caseditor.ide.searchstrategy.TypeSystemSearchStrategyFactory;
import org.apache.uima.util.CasIOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
   * Listens for resource events: If the input file for the editor is removed the editor will be
   * closed and if the input file is modified, then the CAS needs to be updated and all views needs
   * to be notified.
   */
  private class ModifyElementListener implements IResourceChangeListener {

    private FileEditorInput fileInput;

    public ModifyElementListener(FileEditorInput fileInput) {
      this.fileInput = fileInput;
    }

    public void resourceChanged(IResourceChangeEvent event) {
      IResourceDelta delta = event.getDelta();
      try {
        IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
          public boolean visit(IResourceDelta delta) throws CoreException {
            if (delta.getFlags() != IResourceDelta.MARKERS
                    && delta.getResource().getType() == IResource.FILE) {
              IResource resource = delta.getResource();
              if (resource.equals(fileInput.getFile())) {
                if (delta.getKind() == IResourceDelta.REMOVED) {
                  handleElementDeleted(fileInput);
                } else if (delta.getKind() == IResourceDelta.CHANGED) {
                  
                  if (isFileChangeTrackingEnabled)
                    handleElementChanged(fileInput);
                }
              }
            }

            return true;
          }
        };

        delta.accept(visitor);
      } catch (CoreException e) {
        CasEditorPlugin.log(e);
      }
    }
  }

  private static class FileElementInfo extends ElementInfo {

    private ModifyElementListener deleteListener;

    FileElementInfo(ElementInfo info) {
      super(info.element);
    }
  }

  private class SaveSessionPreferencesTrigger implements IPropertyChangeListener {
    private Object element;

    SaveSessionPreferencesTrigger(Object element) {
      this.element = element;
    }

    public void propertyChange(PropertyChangeEvent event) {
      IResource tsFile = ResourcesPlugin.getWorkspace().getRoot()
              .findMember((getTypesystemId(element)));

      PreferenceStore prefStore = (PreferenceStore) getSessionPreferenceStore(element);

      ByteArrayOutputStream prefBytes = new ByteArrayOutputStream();
      try {
        prefStore.save(prefBytes, "");
      } catch (IOException e) {
        CasEditorIdePlugin.log(e);
      }

      try {
        tsFile.setPersistentProperty(new QualifiedName("", CAS_EDITOR_SESSION_PROPERTIES),
                new String(prefBytes.toByteArray(), "UTF-8"));
      } catch (CoreException e) {
        CasEditorIdePlugin.log(e);
      } catch (IOException e) {
        CasEditorIdePlugin.log(e);
      }
    }
  }

  private static final String CAS_EDITOR_SESSION_PROPERTIES = "CAS_EDITOR_SESSION_PROPERTIES";

  /**
   * This map resolved an opened document to its associated style object id.
   * 
   * The tracking is done in the provider because the document element itself does not has any link
   * to the style object.
   */
  private Map<String, String> documentToTypeSystemMap = new HashMap<String, String>();

  /**
   * This map stores temporarily the type system that should be used to open the next document.
   * This functionality is separated from documentToTypeSystemMap since the preference for using 
   * the previously selected type system can be deactivated. The inlined file choose, for example, 
   * uses this field to remember the chosen type system.
   */
  private Map<String, String> typeSystemForNextDocumentOnly = new HashMap<String, String>();
  
  private Map<String, IPreferenceStore> sessionPreferenceStores = new HashMap<String, IPreferenceStore>();

  /**
   * This map resolves a type system to a style. It is used to cache type system preference instance
   * while the editor is open.
   */
  private Map<String, PreferenceStore> typeSystemPreferences = new HashMap<String, PreferenceStore>();
  
  private boolean isFileChangeTrackingEnabled = true;

  // UIMA-2245 Remove this method together with the migration code below one day
  private String getStyleFileForTypeSystem(String typeSystemFile) {
    int lastSlashIndex = typeSystemFile.lastIndexOf("/");

    String styleId = typeSystemFile.substring(0, lastSlashIndex + 1);
    styleId = styleId + ".style-" + typeSystemFile.substring(lastSlashIndex + 1);

    return styleId;
  }

  private String getPreferenceFileForTypeSystem(String typeSystemFile) {
    int lastSlashIndex = typeSystemFile.lastIndexOf("/");

    String styleId = typeSystemFile.substring(0, lastSlashIndex + 1);
    styleId = styleId + ".pref-" + typeSystemFile.substring(lastSlashIndex + 1);

    return styleId;
  }

  private Collection<AnnotationStyle> getConfiguredAnnotationStyles(IPreferenceStore store,
          TypeSystem types) {

    Collection<AnnotationStyle> styles = new HashSet<AnnotationStyle>();

    // TODO: for each annotation type, try to retrieve annotation styles

    return styles;
  }

  @Override
  protected ICasDocument createDocument(Object element) throws CoreException {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile casFile = fileInput.getFile();

      // Try to find a type system for the CAS file
      // TODO: Change to only use full path
      IFile typeSystemFile = null;

      // First check if a type system is already known or was
      // set by the editor for this specific CAS.
      // apply that type system only if the setting is active in the preferences
      String typeSystemFileString = null;
      
      String document = casFile.getFullPath().toPortableString();
      if(typeSystemForNextDocumentOnly.get(document) != null) {
        // the type system was already set internally. Use this one and forget the information.
        typeSystemFileString = typeSystemForNextDocumentOnly.get(document);
        typeSystemForNextDocumentOnly.put(document, null);
      }
      
      IPreferenceStore prefStore = CasEditorIdePlugin.getDefault().getPreferenceStore();
      boolean useLastTypesystem = prefStore
              .getBoolean(CasEditorIdePreferenceConstants.CAS_EDITOR_REMEMBER_TYPESYSTEM);
      if (typeSystemFileString == null && useLastTypesystem) {
        typeSystemFileString = documentToTypeSystemMap
                .get(document);
      }
      if (typeSystemFileString != null)
        typeSystemFile = ResourcesPlugin.getWorkspace().getRoot()
                .getFile(new Path(typeSystemFileString));

      // use search strategies for finding the type system
      if (typeSystemFile == null || !typeSystemFile.exists()) {
        Map<Integer, ITypeSystemSearchStrategy> searchStrategies = TypeSystemSearchStrategyFactory
                .instance().getSearchStrategies();
        // TODO sort again for user preference settings
        Collection<ITypeSystemSearchStrategy> values = searchStrategies.values();
        for (ITypeSystemSearchStrategy eachStrategy : values) {
          IFile findTypeSystem = eachStrategy.findTypeSystem(casFile);
          if (findTypeSystem != null && findTypeSystem.exists()) {
            typeSystemFile = findTypeSystem;
            break;
          }
        }
      }

      // If non was found get it from project
      if (typeSystemFile == null)
        typeSystemFile = TypeSystemLocationPropertyPage.getTypeSystemLocation(casFile.getProject());

      if (typeSystemFile != null && typeSystemFile.exists()) {

        if (!typeSystemFile.isSynchronized(IResource.DEPTH_ZERO)) {
          typeSystemFile.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
        }

        // TODO: Update this comment!
        // Try to load a style file for the type system
        // Should be named: ts file name, prefixed with .style-
        // If it does not exist, create it when it is changed
        // Creating it after the default is changed means that
        // colors could change completely when the a type is
        // added or removed to the type system

        IFile prefFile = ResourcesPlugin
                .getWorkspace()
                .getRoot()
                .getFile(
                        new Path(getPreferenceFileForTypeSystem(typeSystemFile.getFullPath()
                                .toPortableString())));

        PreferenceStore tsPrefStore = typeSystemPreferences.get(prefFile.getFullPath()
                .toPortableString());

        // If lookup for store failed ...
        if (tsPrefStore == null) {
          if (prefFile.exists()) {
            tsPrefStore = new PreferenceStore(prefFile.getName());
            try {
              tsPrefStore.load(prefFile.getContents()); // TODO: Close stream!
            } catch (IOException e) {
              e.printStackTrace(); // TODO: Handle this correctly!
            }
          } else {

            // UIMA-2245
            // DotCorpus to Eclipse PreferenceStore migration code.
            // If there is DotCorpus style file and not yet a preference store file
            // the settings from the DotCorpus style file should be written into a preference store
            // file.
            IFile styleFile = ResourcesPlugin
                    .getWorkspace()
                    .getRoot()
                    .getFile(
                            new Path(getStyleFileForTypeSystem(typeSystemFile.getFullPath()
                                    .toPortableString())));

            if (styleFile.exists()) {
              InputStream styleFileIn = null;
              DotCorpus dotCorpus = null;
              try {
                styleFileIn = styleFile.getContents();
                dotCorpus = DotCorpusSerializer.parseDotCorpus(styleFileIn);

              } finally {
                if (styleFileIn != null)
                  try {
                    styleFileIn.close();
                  } catch (IOException e) {
                    CasEditorPlugin.log(e);
                  }
              }

              if (dotCorpus != null) {
                tsPrefStore = new PreferenceStore(prefFile.getName());
                for (AnnotationStyle style : dotCorpus.getAnnotationStyles()) {
                  AnnotationStyle.putAnnotatationStyleToStore(tsPrefStore, style);
                }

                for (String shownType : dotCorpus.getShownTypes()) {
                  tsPrefStore.putValue(shownType + ".isShown", "true");
                }

                ByteArrayOutputStream prefOut = new ByteArrayOutputStream();
                try {
                  tsPrefStore.save(prefOut, "");
                } catch (IOException e) {
                  // Should never happen!
                  CasEditorPlugin.log(e);
                }

                // TODO: Do we need to handle exceptions here?
                prefFile.create(new ByteArrayInputStream(prefOut.toByteArray()), IFile.FORCE, null);
              }
            }
          }

          // No preference defined, lets use defaults
          if (tsPrefStore == null) {
            tsPrefStore = new PreferenceStore(prefFile.getName());

            CAS cas = DocumentUimaImpl.getVirginCAS(typeSystemFile);
            TypeSystem ts = cas.getTypeSystem();

            Collection<AnnotationStyle> defaultStyles = getConfiguredAnnotationStyles(tsPrefStore,
                    ts);

            Collection<AnnotationStyle> newStyles = DefaultColors.assignColors(ts, defaultStyles);

            // TODO: Settings defaults must be moved to the AnnotationEditor
            for (AnnotationStyle style : newStyles) {
              AnnotationStyle.putAnnotatationStyleToStore(tsPrefStore, style);
            }
          }

          typeSystemPreferences.put(prefFile.getFullPath().toPortableString(), tsPrefStore);
        }

        documentToTypeSystemMap.put(document, typeSystemFile
                .getFullPath().toPortableString());

        IPreferenceStore store = sessionPreferenceStores.get(getTypesystemId(element));

        if (store == null) {
          PreferenceStore newStore = new PreferenceStore();
          sessionPreferenceStores.put(getTypesystemId(element), newStore);
          newStore.addPropertyChangeListener(new SaveSessionPreferencesTrigger(element));

          String sessionPreferenceString = typeSystemFile.getPersistentProperty(new QualifiedName(
                  "", CAS_EDITOR_SESSION_PROPERTIES));

          if (sessionPreferenceString != null) {
            try {
              newStore.load(new ByteArrayInputStream(sessionPreferenceString.getBytes("UTF-8")));
            } catch (IOException e) {
              CasEditorPlugin.log(e);
            }
          }
        }

        // TODO:
        // Preferences are bound to the type system
        // Changed in one place, then it should change in all places

        CAS cas = DocumentUimaImpl.getVirginCAS(typeSystemFile);

        ICasDocument   doc = new DocumentUimaImpl(cas, casFile, typeSystemFile.getFullPath().makeRelative().toString());

        elementErrorStatus.remove(element);

        return doc;
      } else {

        String message = null;

        if (typeSystemFile != null) {
          message = "Cannot find type system!\nPlease place a valid type system in this path:\n"
                  + typeSystemFile.getFullPath().toString();
        } else
          message = "Type system is not set, please choose a type system to open the CAS.";

        IStatus status = new Status(IStatus.ERROR, "org.apache.uima.dev",
                CasDocumentProvider.TYPE_SYSTEM_NOT_AVAILABLE_STATUS_CODE, message, null);

        elementErrorStatus.put(element, status);
      }
    }

    return null;
  }

  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, ICasDocument document,
          boolean overwrite) throws CoreException {

    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      if (document instanceof DocumentUimaImpl) {

        DocumentUimaImpl documentImpl = (DocumentUimaImpl) document;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream(40000);
        documentImpl.serialize(outStream);

        InputStream stream = new ByteArrayInputStream(outStream.toByteArray());
        
        isFileChangeTrackingEnabled = false;
        
        try {
          file.setContents(stream, true, false, null);
        }
        finally {
          isFileChangeTrackingEnabled = true;
        }
      }
    }

    // tell everyone that the element changed and is not dirty any longer
    fireElementDirtyStateChanged(element, false);
  }

  private String getTypesystemId(Object element) {
    if (element instanceof FileEditorInput) {
      FileEditorInput editorInput = (FileEditorInput) element;
      return documentToTypeSystemMap.get(editorInput.getFile().getFullPath().toPortableString());
    }

    return null;
  }

  @Override
  public void saveTypeSystemPreferenceStore(Object element) {
    String prefereceFileId = getPreferenceFileForTypeSystem(getTypesystemId(element));

    PreferenceStore preferences = typeSystemPreferences.get(prefereceFileId);

    // serialize ...
    IFile preferenceFile = ResourcesPlugin.getWorkspace().getRoot()
            .getFile(Path.fromPortableString(prefereceFileId));

    ByteArrayOutputStream preferenceBytes = new ByteArrayOutputStream();

    try {
      preferences.save(preferenceBytes, "");
    } catch (IOException e) {
      // will not fail, writing to memory
      CasEditorPlugin.log(e);
    }

    try {
      if (preferenceFile.exists()) {
        preferenceFile.setContents(new ByteArrayInputStream(preferenceBytes.toByteArray()), true,
                false, null);
      } else {
        preferenceFile.create(new ByteArrayInputStream(preferenceBytes.toByteArray()), true, null);
      }
    } catch (CoreException e) {
      // might fail if writing is not possible
      // for some reason
      CasEditorPlugin.log(e);
    }
  }

  @Override
  public IPreferenceStore getTypeSystemPreferenceStore(Object element) {
    String tsId = getTypesystemId(element);

    if (tsId != null)
      return typeSystemPreferences.get(getPreferenceFileForTypeSystem(tsId));
    else
      return null;
  }

  @Override
  public IPreferenceStore getSessionPreferenceStore(Object element) {
    return sessionPreferenceStores.get(getTypesystemId(element));
  }

  void setTypeSystem(String document, String typeSystem) {
    documentToTypeSystemMap.put(document, typeSystem);
  }

  void setTypeSystemForNextDocumentOnly(String document, String typeSystem) {
    typeSystemForNextDocumentOnly.put(document, typeSystem);
  }
  
  
  @Override
  public Composite createTypeSystemSelectorForm(final ICasEditor editor, Composite parent,
          IStatus status) {

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
        IResource resource = WorkspaceResourceDialog.getWorkspaceResourceElement(Display
                .getCurrent().getActiveShell(), ResourcesPlugin.getWorkspace().getRoot(),
                "Select a Type System", "Please select a Type System:");

        if (resource != null) {

          FileEditorInput editorInput = (FileEditorInput) editor.getEditorInput();
          setTypeSystemForNextDocumentOnly(editorInput.getFile().getFullPath().toPortableString(),
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

  @Override
  protected ElementInfo createElementInfo(Object element) {

    FileElementInfo info = new FileElementInfo(super.createElementInfo(element));

    // Register listener to listens for deletion events,
    // if the file opened in this editor is deleted, the editor should be closed!

    info.deleteListener = new ModifyElementListener((FileEditorInput) element);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(info.deleteListener,
            IResourceChangeEvent.POST_CHANGE);

    return info;
  }

  @Override
  protected void disposeElementInfo(Object element, ElementInfo info) {

    FileElementInfo fileInfo = (FileElementInfo) info;
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(fileInfo.deleteListener);

    super.disposeElementInfo(element, info);
  }

  private void handleElementDeleted(Object element) {
    fireElementDeleted(element);
  }

  private void handleElementChanged(Object element) {
    fireElementChanged(element);
  }

}
