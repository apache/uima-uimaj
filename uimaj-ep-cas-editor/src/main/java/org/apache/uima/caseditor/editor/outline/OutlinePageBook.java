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

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.CasEditorViewPage;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class OutlinePageBook extends CasEditorViewPage 
        implements IContentOutlinePage, ISelectionChangedListener {

  private final class SubPageSite implements IPageSite {

    private SubActionBars subActionBars; 
    private ISelectionProvider selectionProvider;

    public boolean hasService(@SuppressWarnings("rawtypes") Class api) {
      return getSite().hasService(api);
    }

    public Object getService(@SuppressWarnings("rawtypes") Class api) {
      return getSite().getService(api);
    }

    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
      return getSite().getAdapter(adapter);
    }

    public void setSelectionProvider(ISelectionProvider provider) {
      selectionProvider = provider;
    }

    public IWorkbenchWindow getWorkbenchWindow() {
      return getSite().getWorkbenchWindow();
    }

    public Shell getShell() {
      return getSite().getShell();
    }

    public ISelectionProvider getSelectionProvider() {
      return selectionProvider;
    }

    public IWorkbenchPage getPage() {
      return getSite().getPage();
    }

    public void registerContextMenu(String menuId, MenuManager menuManager,
            ISelectionProvider selectionProvider) {
      getSite().registerContextMenu(menuId, menuManager, selectionProvider);
    }

    public IActionBars getActionBars() {
      
      if (subActionBars == null) {
        subActionBars = new SubActionBars(getSite().getActionBars());
      }

      return subActionBars;
    }
  }

  private Viewer viewer;
  
  public OutlinePageBook() {
    super("An outline it not available!");
  }

  
  @Override
  protected void initializeAndShowPage(final IPageBookViewPage page) {
    
    if (viewer != null)
      viewer.removeSelectionChangedListener(this);
    
    IPageSite site = new SubPageSite();

    if (book != null && page != null) {
      try {
        page.init(site);
      } catch (PartInitException e) {
        CasEditorPlugin.log(e);
      }
    }

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
  
  public IPageBookViewPage getCasViewPage() {
    return casViewPage;
  }
}
