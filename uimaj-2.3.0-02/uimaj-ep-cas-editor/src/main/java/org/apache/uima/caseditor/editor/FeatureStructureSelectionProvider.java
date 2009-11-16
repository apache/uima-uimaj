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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * This class is a {@link ISelectionProvider} and informs its listeners about the currently selected
 * {@link FeatureStructure}s.
 */
class FeatureStructureSelectionProvider implements ISelectionProvider {

  private IStructuredSelection mCurrentSelection = new StructuredSelection();

  private Set<ISelectionChangedListener> mListeners = new HashSet<ISelectionChangedListener>();

  /**
   * Adds an {@link ISelectionChangedListener} to this provider.
   *
   * @param listener
   */
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    Assert.isNotNull(listener);

    mListeners.add(listener);
  }

  /**
   * Retrieves the current selection.
   *
   * @return selection
   */
  public ISelection getSelection() {
    return mCurrentSelection;
  }

  /**
   * Removes a registered selection listener.
   *
   * @param listener
   *          the listener to remove
   */
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    mListeners.remove(listener);
  }

  /**
   * Sets the current selection.
   *
   * @param selection
   */
  public void setSelection(ISelection selection) {
    Assert.isNotNull(selection);

    IStructuredSelection structuredSelection = (IStructuredSelection) selection;

    mCurrentSelection = structuredSelection;

    for (ISelectionChangedListener listener : mListeners) {
      SelectionChangedEvent event = new SelectionChangedEvent(this, mCurrentSelection);

      listener.selectionChanged(event);
    }
  }

  /**
   * Sets the current selection to the given {@link AnnotationFS} object.
   *
   * @param annotation
   */
  public void setSelection(ICasDocument document, AnnotationFS annotation) {
    if (annotation == null) {
      throw new IllegalArgumentException("annotation must not be null!");
    }

    setSelection(new StructuredSelection(new ModelFeatureStructure(document, annotation)));
  }

  public void setSelection(ICasDocument document, List<AnnotationFS> selection) {
    setSelection(new StructuredSelection(ModelFeatureStructure.create(document, selection)));
  }

  /**
   * Replaces the current selection with an empty selection.
   */
  public void clearSelection() {
    setSelection(new StructuredSelection());
  }

  /**
   * Replaces the current selection with an empty selection without notifying listeners about it.
   *
   * Use it if the selection object is removed and a new selection was already made by an other
   * {@link ISelectionProvider} instance.
   */
  public void clearSelectionSilently() {
    mCurrentSelection = new StructuredSelection();
  }
}