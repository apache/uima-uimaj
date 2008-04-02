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

package org.apache.uima.taeconfigurator.editors.xml;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

public class XMLEditor extends TextEditor {

  MultiPageEditor editor;

  private ColorManager colorManager;

  private EditorsTextListener m_textListener = new EditorsTextListener();

  // next set to true when we are setting the text of the
  // editor so that just switching to source page doesn't
  // cause editor to think source file is dirty
  boolean m_bIgnoreTextEvent = false;

  public class EditorsTextListener implements ITextListener {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.ITextListener#textChanged(org.eclipse.jface.text.TextEvent)
     */
    public void textChanged(TextEvent event) {
      if (!m_bIgnoreTextEvent) {
        editor.sourceChanged = true;
        editor.setFileDirty();
      }
    }
  }

  public XMLEditor(MultiPageEditor editor) {
    super();
    colorManager = new ColorManager();
    setSourceViewerConfiguration(new XMLConfiguration(colorManager));
    setDocumentProvider(new XMLDocumentProvider());
    this.editor = editor;
  }

  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    getSourceViewer().addTextListener(m_textListener);
  }

  public void dispose() {
    colorManager.dispose();
    super.dispose();
  }

  public void doSaveAs() {
    IProgressMonitor progressMonitor = getProgressMonitor();
    Shell shell = getSite().getShell();
    IEditorInput input = getEditorInput();

    SaveAsDialog dialog = new SaveAsDialog(shell);

    IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile()
            : null;
    if (original != null)
      dialog.setOriginalFile(original);

    dialog.create();

    IDocumentProvider provider = getDocumentProvider();
    if (provider == null) {
      // editor has programatically been closed while the dialog was open
      return;
    }

    if (provider.isDeleted(input) && original != null) {
      String message = "The original file, '" + original.getName() + "' has been deleted";
      dialog.setErrorMessage(null);
      dialog.setMessage(message, IMessageProvider.WARNING);
    }

    if (dialog.open() == Dialog.CANCEL) {
      if (progressMonitor != null)
        progressMonitor.setCanceled(true);
      editor.setSaveAsStatus(MultiPageEditor.SAVE_AS_CANCELLED);
      return;
    }

    IPath filePath = dialog.getResult();
    if (filePath == null) {
      if (progressMonitor != null)
        progressMonitor.setCanceled(true);
      editor.setSaveAsStatus(MultiPageEditor.SAVE_AS_CANCELLED);
      return;
    }

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IFile file = workspace.getRoot().getFile(filePath);
    final IEditorInput newInput = new FileEditorInput(file);

    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
      public void execute(final IProgressMonitor monitor) throws CoreException {
        getDocumentProvider().saveDocument(monitor, newInput,
                getDocumentProvider().getDocument(getEditorInput()), true);
      }
    };

    boolean success = false;
    try {

      provider.aboutToChange(newInput);
      new ProgressMonitorDialog(shell).run(false, true, op);
      success = true;

    } catch (InterruptedException x) {
    } catch (InvocationTargetException x) {

      Throwable targetException = x.getTargetException();

      String title = "Error saving"; // TextEditorMessages.getString("Editor.error.save.title");
      // //$NON-NLS-1$
      String msg = "Error occurred during save operation"; // MessageFormat.format(TextEditorMessages.getString("Editor.error.save.message"),
      // new Object[] {
      // targetException.getMessage()});
      // //$NON-NLS-1$

      if (targetException instanceof CoreException) {
        CoreException coreException = (CoreException) targetException;
        IStatus status = coreException.getStatus();
        if (status != null) {
          switch (status.getSeverity()) {
            case IStatus.INFO:
              MessageDialog.openInformation(shell, title, msg);
              break;
            case IStatus.WARNING:
              MessageDialog.openWarning(shell, title, msg);
              break;
            default:
              MessageDialog.openError(shell, title, msg);
          }
        } else {
          MessageDialog.openError(shell, title, msg);
        }
      }

    } finally {
      provider.changed(newInput);
      if (success) {
        setInput(newInput);
        editor.setSaveAsStatus(MultiPageEditor.SAVE_AS_CONFIRMED);
      } else {
        editor.setSaveAsStatus(MultiPageEditor.SAVE_AS_CANCELLED);
      }
    }

    if (progressMonitor != null)
      progressMonitor.setCanceled(!success);
  }

  public void setIgnoreTextEvent(boolean bIgnoreTextEvent) {
    m_bIgnoreTextEvent = bIgnoreTextEvent;
  }

}
