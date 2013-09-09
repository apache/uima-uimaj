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

  protected static class ElementInfo {
    public int referenceCount;

    public final Object element;

    protected ElementInfo(Object element) {
      this.element = element;
    }
  }

  /**
   * Error status code to indicate that a type system is not available.
   */
  public static final int TYPE_SYSTEM_NOT_AVAILABLE_STATUS_CODE = 12;

  private Set<IElementStateListener> elementStateListeners = new HashSet<IElementStateListener>();

  /**
   * The method {@link #createDocument(Object)} put error status objects for the given element in
   * this map, if something with document creation goes wrong.
   * 
   * The method {@link #getStatus(Object)} can then retrieve and return the status.
   */
  protected Map<Object, IStatus> elementErrorStatus = new HashMap<Object, IStatus>();

  protected ElementInfo createElementInfo(Object element) {
    return new ElementInfo(element);
  }

  protected void disposeElementInfo(Object element, ElementInfo info) {
  }

  /**
   * Creates the a new {@link AnnotationDocument} from the given {@link IEditorInput} element. For
   * all other elements null is returned.
   */
  protected abstract ICasDocument createDocument(Object element) throws CoreException;

  protected abstract void doSaveDocument(IProgressMonitor monitor, Object element,
          ICasDocument document, boolean overwrite) throws CoreException;

  public IStatus getStatus(Object element) {
    return elementErrorStatus.get(element);
  }

  /**
   * Retrieves the persistent per type system preference store. This store is usually saved in
   * relation to the type system, e.g. an ide plugin could save a preference file next to the type
   * system file.
   * 
   * @param element
   * @return the preference store or null if it cannot be retrieved, e.g no document was created for the input.
   */
  // Problem: Keys maybe should be pre-fixed depending on the plugin which is storing values
  // TODO: Should it be renamed to getPersistentPreferenceStore?
  public abstract IPreferenceStore getTypeSystemPreferenceStore(Object element);

  // Might fail silently, only log an error
  public abstract void saveTypeSystemPreferenceStore(Object element);

  /**
   * Retrieves the session preference store. This preference store is used to store session data
   * which should be used to initialize a freshly opened editor.
   * 
   * @param element
   * 
   * @return the session preference store
   */
  public abstract IPreferenceStore getSessionPreferenceStore(Object element);

  // TODO: Redesign the editor annotation status ... maybe this could be a session and ts scoped
  // pref store?
  // protected abstract EditorAnnotationStatus getEditorAnnotationStatus(Object element);

  // protected abstract void setEditorAnnotationStatus(Object element,
  // EditorAnnotationStatus editorAnnotationStatus);

  public abstract Composite createTypeSystemSelectorForm(ICasEditor editor, Composite parent,
          IStatus status);

  public void addElementStateListener(IElementStateListener listener) {
    elementStateListeners.add(listener);
  }

  public void removeElementStateListener(IElementStateListener listener) {
    elementStateListeners.remove(listener);
  }

  protected void fireElementDeleted(Object element) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementDeleted(element);
    }
  }

  protected void fireElementChanged(Object element) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementContentReplaced(element);
    }
  }

  protected void fireElementDirtyStateChanged(Object element, boolean isDirty) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementDirtyStateChanged(element, isDirty);
    }
  }
}
