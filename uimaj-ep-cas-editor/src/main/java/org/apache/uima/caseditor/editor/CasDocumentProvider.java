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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IElementStateListener;

/**
 * Provides the {@link org.apache.uima.caseditor.editor.ICasDocument} for the
 * {@link AnnotationEditor}.
 * 
 * Note: This document provider is still experimental and its API might change without any notice,
 * even on a revision release!
 */
public abstract class CasDocumentProvider {

  /**
   * The Class ElementInfo.
   */
  protected static class ElementInfo {

    /** The reference count. */
    public int referenceCount;

    /** The element. */
    public final Object element;

    /**
     * Instantiates a new element info.
     *
     * @param element
     *          the element
     */
    protected ElementInfo(Object element) {
      this.element = element;
    }
  }

  /**
   * Error status code to indicate that a type system is not available.
   */
  public static final int TYPE_SYSTEM_NOT_AVAILABLE_STATUS_CODE = 12;

  /** The element state listeners. */
  private Set<IElementStateListener> elementStateListeners = new HashSet<>();

  /**
   * The method {@link #createDocument(Object)} put error status objects for the given element in
   * this map, if something with document creation goes wrong.
   * 
   * The method {@link #getStatus(Object)} can then retrieve and return the status.
   */
  protected Map<Object, IStatus> elementErrorStatus = new HashMap<>();

  /**
   * Creates the element info.
   *
   * @param element
   *          the element
   * @return the element info
   */
  protected ElementInfo createElementInfo(Object element) {
    return new ElementInfo(element);
  }

  /**
   * Dispose element info.
   *
   * @param element
   *          the element
   * @param info
   *          the info
   */
  protected void disposeElementInfo(Object element, ElementInfo info) {
  }

  /**
   * Creates the a new {@link AnnotationDocument} from the given {@link IEditorInput} element. For
   * all other elements null is returned.
   *
   * @param element
   *          the element
   * @return the i cas document
   * @throws CoreException
   *           the core exception
   */
  protected abstract ICasDocument createDocument(Object element) throws CoreException;

  /**
   * Do save document.
   *
   * @param monitor
   *          the monitor
   * @param element
   *          the element
   * @param document
   *          the document
   * @param overwrite
   *          the overwrite
   * @throws CoreException
   *           the core exception
   */
  protected abstract void doSaveDocument(IProgressMonitor monitor, Object element,
          ICasDocument document, boolean overwrite) throws CoreException;

  /**
   * Gets the status.
   *
   * @param element
   *          the element
   * @return the status
   */
  public IStatus getStatus(Object element) {
    return elementErrorStatus.get(element);
  }

  /**
   * Retrieves the persistent per type system preference store. This store is usually saved in
   * relation to the type system, e.g. an ide plugin could save a preference file next to the type
   * system file.
   *
   * @param element
   *          the element
   * @return the preference store or null if it cannot be retrieved, e.g no document was created for
   *         the input.
   */
  // Problem: Keys maybe should be pre-fixed depending on the plugin which is storing values
  // TODO: Should it be renamed to getPersistentPreferenceStore?
  public abstract IPreferenceStore getTypeSystemPreferenceStore(Object element);

  /**
   * Save type system preference store.
   *
   * @param element
   *          the element
   */
  // Might fail silently, only log an error
  public abstract void saveTypeSystemPreferenceStore(Object element);

  /**
   * Retrieves the session preference store. This preference store is used to store session data
   * which should be used to initialize a freshly opened editor.
   *
   * @param element
   *          the element
   * @return the session preference store
   */
  public abstract IPreferenceStore getSessionPreferenceStore(Object element);

  // TODO: Redesign the editor annotation status ... maybe this could be a session and ts scoped
  // pref store?
  // protected abstract EditorAnnotationStatus getEditorAnnotationStatus(Object element);

  // protected abstract void setEditorAnnotationStatus(Object element,
  // EditorAnnotationStatus editorAnnotationStatus);

  /**
   * Creates the type system selector form.
   *
   * @param editor
   *          the editor
   * @param parent
   *          the parent
   * @param status
   *          the status
   * @return the composite
   */
  public abstract Composite createTypeSystemSelectorForm(ICasEditor editor, Composite parent,
          IStatus status);

  /**
   * Adds the element state listener.
   *
   * @param listener
   *          the listener
   */
  public void addElementStateListener(IElementStateListener listener) {
    elementStateListeners.add(listener);
  }

  /**
   * Removes the element state listener.
   *
   * @param listener
   *          the listener
   */
  public void removeElementStateListener(IElementStateListener listener) {
    elementStateListeners.remove(listener);
  }

  /**
   * Fire element deleted.
   *
   * @param element
   *          the element
   */
  protected void fireElementDeleted(Object element) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementDeleted(element);
    }
  }

  /**
   * Fire element changed.
   *
   * @param element
   *          the element
   */
  protected void fireElementChanged(Object element) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementContentReplaced(element);
    }
  }

  /**
   * Fire element dirty state changed.
   *
   * @param element
   *          the element
   * @param isDirty
   *          the is dirty
   */
  protected void fireElementDirtyStateChanged(Object element, boolean isDirty) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementDirtyStateChanged(element, isDirty);
    }
  }
}
