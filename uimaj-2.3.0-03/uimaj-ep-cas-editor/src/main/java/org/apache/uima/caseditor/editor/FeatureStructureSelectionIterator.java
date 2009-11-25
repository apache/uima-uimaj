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
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Iterates over all selected {@link FeatureStructure}s.
 */
public class FeatureStructureSelectionIterator implements Iterator<FeatureStructure> {

  private Iterator<?> mSelectionIterator;

  private FeatureStructure mNext;

  /**
   * Initializes the current instance.
   *
   * @param selection
   */
  public FeatureStructureSelectionIterator(IStructuredSelection selection) {
    mSelectionIterator = selection.iterator();
  }

  /**
   * Check if there is one more element.
   *
   * @return true if there is one more element.
   */
  public boolean hasNext() {
    while (mSelectionIterator.hasNext() && mNext == null) {
      Object item = mSelectionIterator.next();

      if (item instanceof IAdaptable) {
        mNext = (FeatureStructure) ((IAdaptable) item).getAdapter(AnnotationFS.class);
      }
    }

    return mNext != null;
  }

  /**
   * Retrieves the next element.
   *
   * @return the next element.
   */
  public FeatureStructure next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    FeatureStructure result = mNext;
    mNext = null;

    return result;
  }

  /**
   * Not supported, it throws an {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
