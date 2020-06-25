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
package org.apache.uima.taeconfigurator.editors.point;

import org.eclipse.core.runtime.IProgressMonitor;



/**
 * This interface is used to redirect the editing functions from CDE
 * to the external editor for a new type of Resource Specifier.
 */
public interface IUimaMultiPageEditor {
  
  /**
   * Adds the pages for current editor.
   */
  public void addPagesForCurrentEditor();
  
  /**
   * Page change for current editor.
   *
   * @param newPageIndex the new page index
   */
  public void pageChangeForCurrentEditor(int newPageIndex);
  
  /**
   * Do save for current editor.
   *
   * @param monitor the monitor
   */
  public void doSaveForCurrentEditor(IProgressMonitor monitor);
  
  /**
   * Do save as for current editor.
   */
  public void doSaveAsForCurrentEditor();

}
