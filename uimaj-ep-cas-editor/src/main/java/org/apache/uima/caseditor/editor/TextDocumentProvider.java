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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;

class TextDocumentProvider extends AbstractDocumentProvider {
  
  private class CasElementInfo extends AbstractDocumentProvider.ElementInfo {
    
    private CasDocumentProvider.ElementInfo casInfo;
    
    public CasElementInfo(IDocument document, IAnnotationModel model) {
      super(document, model);
    }
  }
  
  private final CasDocumentProvider documentProvider;
  
  public TextDocumentProvider(CasDocumentProvider documentProvider) {
    this.documentProvider = documentProvider;
    
    this.documentProvider.addElementStateListener(new IElementStateListener() {
      
      public void elementMoved(Object originalElement, Object movedElement) {
        fireElementMoved(originalElement, movedElement);
      }
      
      public void elementDirtyStateChanged(Object element, boolean isDirty) {
        fireElementDirtyStateChanged(element, isDirty);
      }
      
      public void elementDeleted(Object element) {
        fireElementDeleted(element);
      }
      
      public void elementContentReplaced(Object element) {
        fireElementContentReplaced(element);
      }
      
      public void elementContentAboutToBeReplaced(Object element) {
        fireElementContentAboutToBeReplaced(element);
      }
    });
  }
  
  @Override
  protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
    return new org.eclipse.jface.text.source.AnnotationModel();
  }
  
  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    ICasDocument casDocument =  documentProvider.createDocument(element);
    
    if (casDocument != null) {
      AnnotationDocument document = new AnnotationDocument();
      document.setDocument(casDocument);
      return document;
    }
    else {
      return null;
    }
  }

  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
          boolean overwrite) throws CoreException {
    
    if (document instanceof AnnotationDocument) {
      AnnotationDocument annotationDocument = (AnnotationDocument) document;
      documentProvider.doSaveDocument(monitor, element, annotationDocument.getDocument(), overwrite);
    }
    // TODO:
    // else throw exception ->
  }

  @Override
  protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
    return null;
  }
  
  @Override
  protected ElementInfo createElementInfo(Object element) throws CoreException {
    
    ElementInfo elementInfo = super.createElementInfo(element);
    CasElementInfo casElementInfo = new CasElementInfo(elementInfo.fDocument, elementInfo.fModel);
    casElementInfo.casInfo = documentProvider.createElementInfo(element);
    
    return casElementInfo;
  }
  
  @Override
  protected void disposeElementInfo(Object element, ElementInfo info) {
    
    super.disposeElementInfo(element, info);
    
    CasElementInfo casElementInfo = (CasElementInfo) info;
    documentProvider.disposeElementInfo(element, casElementInfo.casInfo);
  }
  
  @Override
  public IStatus getStatus(Object element) {
    IStatus status = documentProvider.getStatus(element);

    if (status == null) {
      status = super.getStatus(element);
    }

    return status;
  }
}
