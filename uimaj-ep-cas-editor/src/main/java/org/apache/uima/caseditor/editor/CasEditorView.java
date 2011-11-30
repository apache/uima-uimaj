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
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

/**
 * Base class for views which show information about the {@link CAS} opened
 * in the editor.
 * <p>
 * The view page created with {@link #doCreatePage(ICasEditor)} will be disposed
 * and re-created on every Cas Editor input change or CAS view change.
 * <p>
 * In the case the view should no be re-created on a CAS view change
 * {@link #isRecreatePageOnCASViewSwitch()} must be overridden and return false.
 *
 */
public abstract class CasEditorView extends PageBookView {

  private final String editorNotAvailableMessage;

  private Map<ICasEditor, ICasEditorInputListener> editorListenerMap =
          new HashMap<ICasEditor, ICasEditorInputListener>();
  
  private Map<ICasEditor, ICasDocumentListener> documentListenerMap =
          new HashMap<ICasEditor, ICasDocumentListener>();
  
  public CasEditorView(String editorNotAvailableMessage) {
    this.editorNotAvailableMessage = editorNotAvailableMessage;
  }

  /**
   * Implementors should overwrite if they want that. Default is false.
   * <p>
   * Note:<br>
   * The implementation uses the ICasDocumentListener.viewChanged event
   * to recognize view changes. If the view implementation also listens for
   * this event the view might already be disposed when the listener is called.
   * It is therefore strongly recommended either to listen for the event and
   * update the view or don't list for the event and rely on a page re-creation.
   * 
   * @return true if page should be disposed/re-created on CAS view change,
   * or false if not.
   */
  protected boolean isRecreatePageOnCASViewSwitch() {
    return false;
  }
  
  @Override
  protected IPage createDefaultPage(PageBook book) {
    MessagePage page = new MessagePage();
    initPage(page);
    page.createControl(book);
    page.setMessage(editorNotAvailableMessage);
    return page;
  }

  // Creates a new page if document is available and CAS view is compatible
  // Will be recreated on view switch (need a flag to disable that) and input cas switch
  protected abstract IPageBookViewPage doCreatePage(ICasEditor editor);

  private void createViewPage( CasEditorViewPage casViewPageBookedPage, ICasEditor editor) {
    
    IPageBookViewPage page = doCreatePage(editor);
    if (page != null) {
      try {
        page.init(new SubPageSite(casViewPageBookedPage.getSite()));
      } catch (PartInitException e) {
        CasEditorPlugin.log(e);
      }
      
      casViewPageBookedPage.setCASViewPage(page);
    }
    else {
      casViewPageBookedPage.setCASViewPage(null);
    }
  }
  
  @Override
  protected final PageRec doCreatePage(final IWorkbenchPart part) {

    PageRec result = null;
    
    if (part instanceof ICasEditor) {
      final ICasEditor editor = (ICasEditor) part;
      
      final CasEditorViewPage casViewPageBookedPage = new CasEditorViewPage(editorNotAvailableMessage);
      
      if (editor.getDocument() != null) {
        ICasDocumentListener documentListener = new AbstractDocumentListener() {
          @Override
          public void viewChanged(String oldViewName, String newViewName) {
            if (isRecreatePageOnCASViewSwitch()) {
              createViewPage(casViewPageBookedPage, editor);
            }
          }
        };
        
        editor.getDocument().addChangeListener(documentListener);
        
        // remember on map
        documentListenerMap.put(editor, documentListener);
      }
      
      ICasEditorInputListener inputListener = new ICasEditorInputListener() {
        
        public void casDocumentChanged(IEditorInput oldInput, ICasDocument oldDocument,
                IEditorInput newInput, ICasDocument newDocument) {
          
          createViewPage(casViewPageBookedPage, editor);
          
          ICasDocumentListener changeListener = documentListenerMap.get(editor);
          
          if (changeListener != null) {
            if (oldDocument != null)
              oldDocument.removeChangeListener(changeListener);
              
            if (newDocument != null)
              newDocument.addChangeListener(changeListener);
          }
          
        }
      };
      editorListenerMap.put(editor, inputListener);
      editor.addCasEditorInputListener(inputListener);
      
      // BUG: This does not work! 
      
      
      initPage(casViewPageBookedPage);
      
      casViewPageBookedPage.createControl(getPageBook());
      
      createViewPage(casViewPageBookedPage, editor);
      
      result = new PageRec(editor, casViewPageBookedPage);
    }
    
    return result;
  }

  @Override
  protected IWorkbenchPart getBootstrapPart() {
    return getSite().getPage().getActiveEditor();
  }

  @Override
  protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
    
    if (part instanceof ICasEditor) {
      ICasEditor editor = (ICasEditor) part;
      ICasEditorInputListener editorListener = editorListenerMap.remove(part);
      
      if (editorListener != null) {
        editor.removeCasEditorInputListener(editorListener);
      }
      
      ICasDocumentListener documentListener = documentListenerMap.remove(part);
      ICasDocument document = editor.getDocument();
      
      if (documentListener != null && document != null) {
        document.removeChangeListener(documentListener);
      }
    }
    
    pageRecord.page.dispose();
    pageRecord.dispose();
  }

  @Override
  protected boolean isImportant(IWorkbenchPart part) {
    // only interested in annotation editors
    return part instanceof AnnotationEditor;
  }

  /**
   * Look at {@link IPartListener#partBroughtToTop(IWorkbenchPart)}.
   *
   * @param part
   */
  @Override
  public void partBroughtToTop(IWorkbenchPart part) {
    partActivated(part);
  }
  
  @Override
  public void dispose() {
    
    for (Map.Entry<ICasEditor, ICasEditorInputListener> entry :
      editorListenerMap.entrySet()) {
      entry.getKey().removeCasEditorInputListener(entry.getValue());
    }
    
    editorListenerMap.clear();
    
    for (Map.Entry<ICasEditor, ICasDocumentListener> entry :
      documentListenerMap.entrySet()) {
      
      ICasDocument document = entry.getKey().getDocument();
      if (document != null)
        document.removeChangeListener(entry.getValue());
    }
    
    documentListenerMap.clear();
    
    super.dispose();
  }
}
