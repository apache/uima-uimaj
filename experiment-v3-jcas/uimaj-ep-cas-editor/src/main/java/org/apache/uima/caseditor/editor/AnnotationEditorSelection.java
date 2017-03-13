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
   * @param textSelection the text selection
   * @param structuredSelection the structured selection
   */
  AnnotationEditorSelection(ITextSelection textSelection, IStructuredSelection structuredSelection) {
    this.textSelection = textSelection;
    this.structuredSelection = structuredSelection;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextSelection#getOffset()
   */
  @Override
  public int getOffset() {
    return textSelection.getOffset();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextSelection#getLength()
   */
  @Override
  public int getLength() {
    return textSelection.getLength();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextSelection#getStartLine()
   */
  @Override
  public int getStartLine() {
    return textSelection.getStartLine();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextSelection#getEndLine()
   */
  @Override
  public int getEndLine() {
    return textSelection.getEndLine();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextSelection#getText()
   */
  @Override
  public String getText() {
    return textSelection.getText();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelection#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return structuredSelection.isEmpty() && textSelection.isEmpty();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredSelection#getFirstElement()
   */
  @Override
  public Object getFirstElement() {
    return structuredSelection.getFirstElement();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredSelection#iterator()
   */
  @Override
  public Iterator iterator() {
    return structuredSelection.iterator();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredSelection#size()
   */
  @Override
  public int size() {
    return structuredSelection.size();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredSelection#toArray()
   */
  @Override
  public Object[] toArray() {
    return structuredSelection.toArray();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredSelection#toList()
   */
  @Override
  public List toList() {
    return structuredSelection.toList();
  }
}
