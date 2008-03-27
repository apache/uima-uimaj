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

import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.XMLizable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

/**
 * The interface implemented by an external editor
 *
 */
public interface IUimaEditorExtension {
  
  /**
   * The first method called by the CDE after the implementation class of this 
   * interface is instantiated.
   * 
   * @return void
   */
  public void init ();
  
  /**
   * When a new type of descriptor is encountered and cannot edit, CDE will
   * called this method to test if the external editor can edit this new type of descriptor.
   * 
   * @param cde       An instance of CDE
   * @param xmlizable New type of descriptor to be edited
   * @return boolean  Return true if the external editor can edit the specified type of descriptor
   */
  public boolean canEdit (MultiPageEditor cde, XMLizable xmlizable);
  
  /**
   * Called by CDE to activate the external editor for editing the new type of descriptor.
   *    * 
   * @param site
   *            The site for which this part is being created; must not be
   *            <code>null</code>.
   * @param editorInput
   *            The input on which this editor should be created; must not be
   *            <code>null</code>.
   * @param cde       An instance of CDE
   * @param xmlizable New type of descriptor to be edited
   * @throws PartInitException
   *             If the initialization of the part fails 
   * @return void
   */
  public void activateEditor(IEditorSite site, IEditorInput editorInput,
          MultiPageEditor cde, XMLizable xmlizable) throws PartInitException;
}
