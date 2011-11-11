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

package org.apache.uima.caseditor.editor.outline;

import org.apache.uima.caseditor.editor.CasEditorViewPage;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class OutlinePageBook extends CasEditorViewPage 
        implements IContentOutlinePage, ISelectionChangedListener {

  private ListenerList selectionChangedListeners = new ListenerList();
  
  private Viewer viewer;
  
  public OutlinePageBook() {
    super("An outline it not available!");
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.add(listener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.remove(listener);
  }
  
  @Override
  protected void initializeAndShowPage(IPageBookViewPage page) {
    
    if (viewer != null)
      viewer.removeSelectionChangedListener(this);
    
    super.initializeAndShowPage(page);
    
    if (book != null) {
      if (page != null) {
        viewer = ((AnnotationOutline) page).getViewer();
        viewer.addSelectionChangedListener(this);
      }
      else {
        viewer = null;
      }
    }
  }
  
  public ISelection getSelection() {
    if (viewer != null)
      return viewer.getSelection();
    else
      return StructuredSelection.EMPTY;
  }

  public void setSelection(ISelection selection) {
    if (viewer != null)
      viewer.setSelection(selection);
  }

  public void selectionChanged(final SelectionChangedEvent event) {
    
    for (Object listener : selectionChangedListeners.getListeners()) {
      
      final ISelectionChangedListener selectionChangedListener = 
              (ISelectionChangedListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          selectionChangedListener.selectionChanged(event);
        }
      });
    }
  }
}