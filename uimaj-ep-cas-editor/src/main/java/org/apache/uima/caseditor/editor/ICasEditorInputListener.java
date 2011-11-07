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

import org.eclipse.ui.IEditorInput;

public interface ICasEditorInputListener {

  /**
   * This method is called if an {@link ICasDocument} is exchanged.
   * The arguments of this methods can be null under certain circumstances.
   * For example, if a document is opened where the type system cannot be found 
   * for, then the new document will be null.
   * 
   * @param oldDocument
   *          - the replaced, old document {@link ICasDocument}.
   * @param newDocument
   *          - the new, current document {@link ICasDocument}.
   */
  void casDocumentChanged(IEditorInput oldInput, ICasDocument oldDocument, IEditorInput newInput, ICasDocument newDocument);
  
}
