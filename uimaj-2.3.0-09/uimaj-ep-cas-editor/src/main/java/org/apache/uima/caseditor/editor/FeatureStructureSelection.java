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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 *
 */
public class FeatureStructureSelection {
  private List<FeatureStructure> mFeatureStructures;

  /**
   * Initializes a the current instance with all FeatureStructure object that are contained in the
   * {@link StructuredSelection}.
   *
   * @param selection
   */
  public FeatureStructureSelection(IStructuredSelection selection) {
    mFeatureStructures = new ArrayList<FeatureStructure>(selection.size());

    for (Object item : selection.toList()) {
      FeatureStructure annotation = (FeatureStructure) Platform.getAdapterManager().getAdapter(
              item, FeatureStructure.class);

      // TODO: fix it
      if (annotation == null && item instanceof IAdaptable) {
        annotation = (FeatureStructure) ((IAdaptable) item).getAdapter(FeatureStructure.class);
      }

      if (annotation != null) {
        mFeatureStructures.add(annotation);
      }
    }

    mFeatureStructures = Collections.unmodifiableList(mFeatureStructures);
  }

  /**
   * Retrieves the size of the collection.
   *
   * @return the size
   */
  public int size() {
    return mFeatureStructures.size();
  }

  /**
   * Indicates that the selection is empty.
   *
   * @return true if empty false otherwise
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Retrieves a list of {@link FeatureStructure} objects.
   *
   * @return all selected {@link FeatureStructure} objects
   */
  public List<FeatureStructure> toList() {
    return mFeatureStructures;
  }

  /**
   * Retrieves a human readable string.
   * @return human readable string
   */
  @Override
  public String toString() {
    return mFeatureStructures.toString();
  }
}