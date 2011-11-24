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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;

public class CasEditorViewPage extends Page implements ISelectionProvider {

  private ListenerList selectionChangedListeners = new ListenerList();
  
  private final String notAvailableMessage;
  
  protected PageBook book;
  
  protected IPageBookViewPage casViewPage;

  private SubActionBars subActionBar;
  
  private Text messageText;
  
  protected CasEditorViewPage(String notAvailableMessage) {
    this.notAvailableMessage = notAvailableMessage;
  }
  
  @SuppressWarnings("rawtypes")
  private void refreshActionHandlers() {

    IActionBars actionBars = getSite().getActionBars();
    actionBars.clearGlobalActionHandlers();

    Map newActionHandlers = subActionBar
        .getGlobalActionHandlers();
    if (newActionHandlers != null) {
      Set keys = newActionHandlers.entrySet();
      Iterator iter = keys.iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        actionBars.setGlobalActionHandler((String) entry.getKey(),
            (IAction) entry.getValue());
      }
    }
  }

  // These are called from the outside, even if the page is not active ...
  // this leads to the processing of events which should not be processed!
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.add(listener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.remove(listener);
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
  
  public ISelection getSelection() {
    if (casViewPage != null && casViewPage.getSite().getSelectionProvider() != null) {
      return casViewPage.getSite().getSelectionProvider().getSelection();
    }
    else {
      return StructuredSelection.EMPTY;
    }
  }

  public void setSelection(ISelection selection) {
    if (casViewPage != null && casViewPage.getSite().getSelectionProvider() != null) {
      casViewPage.getSite().getSelectionProvider().setSelection(selection);
    }
  }
  
  @Override
  public void createControl(Composite parent) {
    book = new PageBook(parent, SWT.NONE);
    
    messageText = new Text(book, SWT.WRAP | SWT.READ_ONLY);
    messageText.setBackground(parent.getShell().getBackground());
    messageText.setText(notAvailableMessage);
    
    getSite().setSelectionProvider(this);
    
    // Page might be set before the page is initialized
    initializeAndShowPage(casViewPage);
  }
  
  /**
   * Creates and shows the page, if page is null
   * the not available message will be shown.
   * 
   * @param page
   */
  protected void initializeAndShowPage(IPageBookViewPage page) {
    if (book != null) {
      if (page != null) {
        page.createControl(book);
        casViewPage = page;
        
        // Note: If page is in background event listening must be disabled!
        ISelectionProvider selectionProvider = page.getSite().getSelectionProvider();
        selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {
          
          public void selectionChanged(SelectionChangedEvent event) {
            CasEditorViewPage.this.selectionChanged(event);
          }
        });
        
        subActionBar = (SubActionBars) casViewPage.getSite().getActionBars();
        
        casViewPage.setActionBars(subActionBar);

        subActionBar.activate();
        subActionBar.updateActionBars();

        refreshActionHandlers();
        
        book.showPage(page.getControl());
      }
      else {
        book.showPage(messageText);
        getSite().getActionBars().updateActionBars();
      }
    }
  }
  
  public void setCASViewPage(IPageBookViewPage page) {
    
    if (book != null && casViewPage != null) {
      casViewPage.dispose();
      subActionBar.dispose();
    }
    
    casViewPage = page;
    
    initializeAndShowPage(page);
  }
  
  @Override
  public Control getControl() {
    return book;
  }

  @Override
  public void setFocus() {
    book.setFocus();
  }
  
  @Override
  public void dispose() {
    super.dispose();

    if (casViewPage != null) {
      casViewPage.dispose();
      subActionBar.dispose();
    }
  }
}
