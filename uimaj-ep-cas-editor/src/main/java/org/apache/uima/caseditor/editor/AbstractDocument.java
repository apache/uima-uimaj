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

import org.apache.uima.cas.FeatureStructure;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;

/**
 * Abstract base class for document implementations.
 */
public abstract class AbstractDocument implements ICasDocument {
  /**
   * Contains the change listener objects.
   */
  private ListenerList mListener = new ListenerList();

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
  protected void fireAddedFeatureStructure(final FeatureStructure annotation) {
    
    for (Object listener : mListener.getListeners()) {
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.added(annotation);
        }
      });
    }
  }

  /**
   * Sends an added message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireAddedFeatureStructure(final Collection<? extends FeatureStructure> annotations) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.added(Collections.unmodifiableCollection(annotations));
        }
      });
    }
  }

  /**
   * Sends a removed message to registered listeners.
   * 
   * @param annotation
   */
  protected void fireRemovedFeatureStructure(final FeatureStructure annotation) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.removed(annotation);
        }
      });
    }
  }

  /**
   * Sends a removed message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireRemovedFeatureStructure(final Collection<? extends FeatureStructure> annotations) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.removed(Collections.unmodifiableCollection(annotations));
        }
      });
    }
  }

  /**
   * Sends an updated message to registered listeners.
   * 
   * @param annotation
   */
  protected void fireUpdatedFeatureStructure(final FeatureStructure annotation) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.updated(annotation);
        }
      });
    }
  }

  /**
   * Sends an updated message to registered listeners.
   * 
   * @param annotations
   */
  protected void fireUpdatedFeatureStructure(final Collection<? extends FeatureStructure> annotations) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.updated(Collections.unmodifiableCollection(annotations));
        }
      });
    }
  }

  protected void fireChanged() {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.changed();
        }
      });
    }
  }

  protected void fireViewChanged(final String oldViewName, final String newViewName) {
    for (Object listener : mListener.getListeners()) {
      
      final ICasDocumentListener documentListener = (ICasDocumentListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          documentListener.viewChanged(oldViewName, newViewName);
        }
      });
    }
  }
  
  public String getTypeSystemText() {
    return null;
  }
  
  
}
