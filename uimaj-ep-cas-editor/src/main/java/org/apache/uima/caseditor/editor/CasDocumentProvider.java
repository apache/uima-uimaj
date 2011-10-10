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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.Type;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.IElementStateListener;

/**
 * Provides the {@link org.apache.uima.caseditor.editor.ICasDocument} for the
 * {@link AnnotationEditor}.
 * 
 * Note: This document provider is still experimental and its API might change without
 * any notice, even on a revision release!
 */
public abstract class CasDocumentProvider {

  protected static class ElementInfo {
    public int referenceCount;
    public final Object element;
    
    protected ElementInfo(Object element) {
      this.element = element;
    }
  }
  
  public static final int TYPE_SYSTEM_NOT_AVAILABLE_STATUS_CODE = 12;
  
  private Set<IElementStateListener> elementStateListeners =
          new HashSet<IElementStateListener>();
  
  private Set<IAnnotationStyleListener> annotationStyleListeners =
      new HashSet<IAnnotationStyleListener>();
  
  protected ElementInfo createElementInfo(Object element) {
    return new ElementInfo(element);
  }
  
  protected void disposeElementInfo(Object element, ElementInfo info) {
  }
  
  /**
   * The method {@link #createDocument(Object)} put error status objects for the given element in
   * this map, if something with document creation goes wrong.
   * 
   * The method {@link #getStatus(Object)} can then retrieve and return the status.
   */
  protected Map<Object, IStatus> elementErrorStatus = new HashMap<Object, IStatus>();

  /**
   * Creates the a new {@link AnnotationDocument} from the given {@link IEditorInput} element.
   * For all other elements null is returned.
   */
  protected abstract ICasDocument createDocument(Object element) throws CoreException;

  protected abstract void doSaveDocument(IProgressMonitor monitor, Object element,
          ICasDocument document, boolean overwrite) throws CoreException;

  public IStatus getStatus(Object element) {
    return elementErrorStatus.get(element);
  }

  // Problem: Keys maybe should be pre-fixed depending on the plugin which is storing values
  public abstract IPreferenceStore getTypeSystemPreferenceStore(Object element);
  
  // Might fail silently, only log an error
  public abstract void saveTypeSystemPreferenceStore(Object element);
  
  
  // TODO: We also need a set method here
  // TODO: This is somehow duplicated, once in the editor annotation status
  //       and once in the 
  Collection<String> getShownTypes(Object element) {
    PreferenceStore prefStore = (PreferenceStore) getTypeSystemPreferenceStore(element);
    
    Set<String> shownTypes = new HashSet<String>();
    
    for (String prefName : prefStore.preferenceNames()) {
      if (prefName.endsWith(".isShown")) {
        if (prefStore.getBoolean(prefName))
          shownTypes.add(prefName.substring(0, prefName.lastIndexOf(".isShown")));
      }
    }
    
    return shownTypes;
  }
  
  // TODO: Move to Annotation Editor
  void addShownType(Object element, Type type) {
    IPreferenceStore prefStore = getTypeSystemPreferenceStore(element);
    prefStore.setValue(type.getName() + ".isShown", Boolean.TRUE.toString());
    
    saveTypeSystemPreferenceStore(element);
  }
  
  // TODO: Move to Annotation Editor
  void removeShownType(Object element, Type type) {
    IPreferenceStore prefStore = getTypeSystemPreferenceStore(element);
    prefStore.setValue(type.getName() + ".isShown", Boolean.FALSE.toString());
    
    saveTypeSystemPreferenceStore(element);
  }
  
  // TODO: Redesign the editor annotation status ... maybe this could be a session and ts scoped pref store?
  protected abstract EditorAnnotationStatus getEditorAnnotationStatus(Object element);

  protected abstract void setEditorAnnotationStatus(Object element,
          EditorAnnotationStatus editorAnnotationStatus);
  
  // TODO:
  // This case only works if there is a single shared state between all editors
  // An implementation should track the listeners per shared type system
  //
  // Must that be already done for multiple CAS Editor projects, or do they have an instance
  // per project ???
  // Is there one doc provider instance per editor, or one for many editors ?!
  
  // TODO: Replace this with preference listener
  // TODO: Write a Annotation Style Listener which outputs the current events,
  //       based on preference store changes ... non-relevant chanegs need to be filtered!
  public void addAnnotationStyleListener(Object element, IAnnotationStyleListener listener) {
    annotationStyleListeners.add(listener);
  }
  
//TODO: Replace this with preference listener
  public void removeAnnotationStyleListener(Object element, IAnnotationStyleListener listener) {
    annotationStyleListeners.remove(listener);
  }
  
//TODO: Replace this with preference listener
  public void fireAnnotationStyleChanged(Object element, Collection<AnnotationStyle> styles) {
    for (IAnnotationStyleListener listener : annotationStyleListeners) {
      listener.annotationStylesChanged(styles);
    }
  }
  
  public abstract Composite createTypeSystemSelectorForm(ICasEditor editor, Composite parent, IStatus status);
  
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
  
  protected void fireElementDirtyStateChanged(Object element, boolean isDirty) {
    for (IElementStateListener listener : elementStateListeners) {
      listener.elementDirtyStateChanged(element, isDirty);
    }
  }
}
