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

/**
 * The Class AnnotationEditorSelection.
 */
class AnnotationEditorSelection implements ITextSelection, IStructuredSelection {

  /** The text selection. */
  private ITextSelection textSelection;

  /** The structured selection. */
  private IStructuredSelection structuredSelection;

  /**
   * Instantiates a new annotation editor selection.
   *
   * @param textSelection
   *          the text selection
   * @param structuredSelection
   *          the structured selection
   */
  AnnotationEditorSelection(ITextSelection textSelection,
          IStructuredSelection structuredSelection) {
    this.textSelection = textSelection;
    this.structuredSelection = structuredSelection;
  }

  @Override
  public int getOffset() {
    return textSelection.getOffset();
  }

  @Override
  public int getLength() {
    return textSelection.getLength();
  }

  @Override
  public int getStartLine() {
    return textSelection.getStartLine();
  }

  @Override
  public int getEndLine() {
    return textSelection.getEndLine();
  }

  @Override
  public String getText() {
    return textSelection.getText();
  }

  @Override
  public boolean isEmpty() {
    return structuredSelection.isEmpty() && textSelection.isEmpty();
  }

  @Override
  public Object getFirstElement() {
    return structuredSelection.getFirstElement();
  }

  @Override
  public Iterator iterator() {
    return structuredSelection.iterator();
  }

  @Override
  public int size() {
    return structuredSelection.size();
  }

  @Override
  public Object[] toArray() {
    return structuredSelection.toArray();
  }

  @Override
  public List toList() {
    return structuredSelection.toList();
  }
}
