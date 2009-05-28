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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.annotation.EclipseAnnotationPeer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

/**
 * Provides the {@link org.apache.uima.caseditor.editor.ICasDocument} for the
 * {@link AnnotationEditor}.
 */
public abstract class CasDocumentProvider extends AbstractDocumentProvider {

  /**
   * The method {@link #createDocument(Object)} put error status objects for the given element in
   * this map, if something with document creation goes wrong.
   * 
   * The method {@link #getStatus(Object)} can then retrieve and return the status.
   */
  protected Map<Object, IStatus> elementErrorStatus = new HashMap<Object, IStatus>();

  @Override
  protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
    return new IAnnotationModel() {

      private org.apache.uima.caseditor.editor.ICasDocument mDocument;
      private int numberOfConnectedDocuments;

      public void addAnnotation(Annotation annotation, Position position) {
      }

      public void addAnnotationModelListener(IAnnotationModelListener listener) {
      }

      public void connect(IDocument document) {
        mDocument = (org.apache.uima.caseditor.editor.ICasDocument) document;
        
        numberOfConnectedDocuments++;
      }

      public void disconnect(IDocument document) {
        numberOfConnectedDocuments--;
        
        if (numberOfConnectedDocuments == 0) {
          mDocument = null;
        }
      }

      public Iterator<EclipseAnnotationPeer> getAnnotationIterator() {
        
        final Iterator<FeatureStructure> mAnnotations =
                mDocument.getCAS().getAnnotationIndex().iterator();
        
        return new Iterator<EclipseAnnotationPeer>() {

          public boolean hasNext() {
            return mAnnotations.hasNext();
          }

          public EclipseAnnotationPeer next() {
            AnnotationFS annotation = (AnnotationFS) mAnnotations.next();

            EclipseAnnotationPeer peer =
                    new EclipseAnnotationPeer(annotation.getType().getName(), false, "");
            peer.setAnnotation(annotation);
            return peer;
          }

          public void remove() {
          }
        };
      }

      public Position getPosition(Annotation annotation) {
        EclipseAnnotationPeer peer = (EclipseAnnotationPeer) annotation;
        AnnotationFS annotationFS = peer.getAnnotationFS();
        return new Position(annotationFS.getBegin(), annotationFS.getEnd()
                - annotationFS.getBegin());
      }

      public void removeAnnotation(Annotation annotation) {
      }

      public void removeAnnotationModelListener(IAnnotationModelListener listener) {
      }
    };
  }

  /**
   * Creates the a new {@link AnnotationDocument} from the given {@link FileEditorInput} element.
   * For all other elements null is returned.
   */
  @Override
  protected abstract IDocument createDocument(Object element) throws CoreException;

  @Override
  protected abstract void doSaveDocument(IProgressMonitor monitor, Object element,
          IDocument document, boolean overwrite) throws CoreException;

  @Override
  protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
    return null;
  }

  @Override
  public IStatus getStatus(Object element) {
    IStatus status = elementErrorStatus.get(element);

    if (status == null) {
      status = super.getStatus(element);
    }

    return status;
  }

  protected abstract AnnotationStyle getAnnotationStyle(Object element, Type type);

  protected abstract EditorAnnotationStatus getEditorAnnotationStatus(Object element);

  protected abstract void setEditorAnnotationStatus(Object element,
          EditorAnnotationStatus editorAnnotationStatus);
}
