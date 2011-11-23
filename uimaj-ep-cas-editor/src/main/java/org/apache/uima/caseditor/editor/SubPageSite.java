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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPageSite;

public class SubPageSite implements IPageSite {

  private final IPageSite site;
  
  private SubActionBars subActionBars; 
  private ISelectionProvider selectionProvider;

  public SubPageSite(IPageSite site) {
    this.site = site;
  }
  
  public boolean hasService(@SuppressWarnings("rawtypes") Class api) {
    return site.hasService(api);
  }

  public Object getService(@SuppressWarnings("rawtypes") Class api) {
    return site.getService(api);
  }

  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    return site.getAdapter(adapter);
  }

  public void setSelectionProvider(ISelectionProvider provider) {
    selectionProvider = provider;
  }

  public IWorkbenchWindow getWorkbenchWindow() {
    return site.getWorkbenchWindow();
  }

  public Shell getShell() {
    return site.getShell();
  }

  public ISelectionProvider getSelectionProvider() {
    return selectionProvider;
  }

  public IWorkbenchPage getPage() {
    return site.getPage();
  }

  public void registerContextMenu(String menuId, MenuManager menuManager,
          ISelectionProvider selectionProvider) {
    site.registerContextMenu(menuId, menuManager, selectionProvider);
  }

  public IActionBars getActionBars() {
    
    if (subActionBars == null) {
      subActionBars = new SubActionBars(site.getActionBars());
    }

    return subActionBars;
  }
}