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

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;

public class CasEditorViewPage extends Page {
  
  private final String notAvailableMessage;
  
  protected PageBook book;
  
  private IPageBookViewPage casViewPage;

  private Text messageText;
  
  protected CasEditorViewPage(String notAvailableMessage) {
    this.notAvailableMessage = notAvailableMessage;
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
        
        try {
          page.init(getSite());
        } catch (PartInitException e) {
          CasEditorPlugin.log(e);
        }
        
        page.createControl(book);
        casViewPage = page;
        book.showPage(page.getControl());
      }
      else {
        book.showPage(messageText);
      }
    }
  }
  
  public void setCASViewPage(IPageBookViewPage page) {
    
    if (book != null && casViewPage != null)
      casViewPage.dispose();
    
    casViewPage = page;
    
    initializeAndShowPage(page);
  }
  
  @Override
  public void createControl(Composite parent) {
    book = new PageBook(parent, SWT.NONE);
    
    messageText = new Text(book, SWT.WRAP | SWT.READ_ONLY);
    messageText.setBackground(parent.getShell().getBackground());
    messageText.setText(notAvailableMessage);
    
    // Page might be set before the page is initialized
    initializeAndShowPage(casViewPage);
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

    if (casViewPage != null)
      casViewPage.dispose();
  }
}
