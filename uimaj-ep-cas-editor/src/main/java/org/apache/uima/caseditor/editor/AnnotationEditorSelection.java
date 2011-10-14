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
import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

class AnnotationEditorSelection implements ITextSelection, IStructuredSelection {

  private ITextSelection textSelection;
  
  private IStructuredSelection structuredSelection;
  
  AnnotationEditorSelection(ITextSelection textSelection, IStructuredSelection structuredSelection) {
    this.textSelection = textSelection;
    this.structuredSelection = structuredSelection;
  }
  
  public int getOffset() {
    return textSelection.getOffset();
  }

  public int getLength() {
    return textSelection.getLength();
  }

  public int getStartLine() {
    return textSelection.getStartLine();
  }

  public int getEndLine() {
    return textSelection.getEndLine();
  }

  public String getText() {
    return textSelection.getText();
  }

  public boolean isEmpty() {
    return structuredSelection.isEmpty() && textSelection.isEmpty();
  }

  public Object getFirstElement() {
    return structuredSelection.getFirstElement();
  }

  public Iterator iterator() {
    return structuredSelection.iterator();
  }

  public int size() {
    return structuredSelection.size();
  }

  public Object[] toArray() {
    return structuredSelection.toArray();
  }

  public List toList() {
    return structuredSelection.toList();
  }
}
