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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

/**
 * Abstract base class for document implementations.
 */
public abstract class AbstractDocument implements ICasDocument {
  /**
   * Contains the change listener objects.
   */
  private Set<ICasDocumentListener> mListener = new HashSet<ICasDocumentListener>();

  /**
   * Registers a change listener.
   * 
   * @param listener
   */
  public void addChangeListener(final ICasDocumentListener listener) {
    mListener.add(listener);
  }

  /**
   * Unregisters a change listener.
   * 
   * @param listener
   */
  public void removeChangeListener(ICasDocumentListener listener) {
    mListener.remove(listener);
  }

  /**
   * Sends an added message to registered listeners.
   * 
   * @param annotation
   */
  protected void fireAddedFeatureStructure(FeatureStructure annotation) {
    for (ICasDocumentListener listener : mListener) {
      listener.added(annotation);
    }
  }

  /**
   * Sends an added message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireAddedFeatureStructure(Collection<FeatureStructure> annotations) {
    for (ICasDocumentListener listener : mListener) {
      listener.added(Collections.unmodifiableCollection(annotations));
    }
  }

  /**
   * Sends a removed message to registered listeners.
   * 
   * @param annotation
   */
  protected void fireRemovedFeatureStructure(FeatureStructure annotation) {
    for (ICasDocumentListener listener : mListener) {
      listener.removed(annotation);
    }
  }

  /**
   * Sends a removed message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireRemovedFeatureStructure(Collection<? extends FeatureStructure> annotations) {
    for (ICasDocumentListener listener : mListener) {
      listener.removed(Collections.unmodifiableCollection(annotations));
    }
  }

  /**
   * Sends an updated message to registered listeners.
   * 
   * @param annotation
   */
  protected void fireUpdatedFeatureStructure(FeatureStructure annotation) {
    for (ICasDocumentListener listener : mListener) {
      listener.updated(annotation);
    }
  }

  /**
   * Sends an updated message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireUpdatedFeatureStructure(Collection<? extends FeatureStructure> annotations) {
    for (ICasDocumentListener listener : mListener) {
      listener.updated(Collections.unmodifiableCollection(annotations));
    }
  }

  protected void fireChanged() {
    for (ICasDocumentListener listener : mListener) {
      listener.changed();
    }
  }

  protected void fireViewChanged(String oldViewName, String newViewName) {
    for (ICasDocumentListener listener : mListener) {
      listener.viewChanged(oldViewName, newViewName);
    }
  }

}
