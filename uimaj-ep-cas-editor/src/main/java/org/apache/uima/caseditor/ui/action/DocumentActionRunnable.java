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

package org.apache.uima.caseditor.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

/**
 * This class can be reused by ui actions which want to modify documents.
 */
abstract class DocumentActionRunnable implements IRunnableWithProgress {

  private final Collection<DocumentElement> documents;
  private final String taskName;

  /**
   * Initializes the current instance.
   *
   * @param documents
   */
  protected DocumentActionRunnable(String taskName, Collection<DocumentElement> documents) {
    this.taskName = taskName;
    this.documents = documents;
  }



  /**
   * This method is called before the processing of the documents is started.
   *
   * @throws InvocationTargetException
   */
  protected void initialize() throws InvocationTargetException {
  }

  /**
   * Processes the given cas object.
   *
   * @param cas
   *
   * @return true if the implementation changed the cas object otherwise false.
   *
   * @throws InvocationTargetException
   */
  protected abstract boolean process(CAS cas) throws InvocationTargetException;

  /**
   * This method is called after the processing of the documents.
   *
   * @throws InvocationTargetException
   */
  protected void completedProcessing(IProgressMonitor monitor) throws InvocationTargetException {
  }

  /**
   * Processes the provided documents. Modified documents are synchronized with
   * the file system or with the corresponding editor.
   */
  public final void run(IProgressMonitor monitor) throws InvocationTargetException,
          InterruptedException {

    monitor.beginTask(taskName, documents.size());

    monitor.subTask("Initializing");

    initialize();
    
    // TODO:
    // Now we have to ask the document provider to
    // do this kind of document element mapping
    Map<DocumentElement, AnnotationEditor> editorMap = new HashMap<DocumentElement, AnnotationEditor>();

    for (AnnotationEditor annotationEditor : AnnotationEditor.getAnnotationEditors()) {
    	// TODO: fix it
      // editorMap.put(annotationEditor.getDocument().getDocumentElement(), annotationEditor);
    }

    monitor.subTask("Processing documents, please wait!");

    for (DocumentElement documentElement : documents) {

      final ICasDocument doc;

      try {
        doc = documentElement.getDocument(false);
      } catch (CoreException e) {
        throw new InvocationTargetException(e);
      }

      boolean wasCasChanged = process(doc.getCAS());

      if (wasCasChanged) {
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            doc.changed();
          }
        });

        try {

          if (editorMap.get(documentElement) == null) {
            // file is not opened in any editor, just save the changes
            documentElement.saveDocument();
          } else if (!editorMap.get(documentElement).isDirty()) {
            // element is opened in editor and not dirty
            AnnotationEditor editor = editorMap.get(documentElement);
            editor.setDirty();
          } else {
            // element is opened in editor and dirty, do nothing
          }
        } catch (CoreException e) {

          // TODO: Show the user an error dialog
          CasEditorPlugin.log(e);
        }
      }

      monitor.worked(1);
    }

    monitor.subTask("Completing processing!");

    completedProcessing(monitor);

    monitor.done();
  }
}