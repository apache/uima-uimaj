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

package org.apache.uima.caseditor.editor.contextmenu;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Creates the show annotations context sub menu.
 */
public class ShowAnnotationsMenu extends TypeMenu {

  /** The listeners. */
  private Set<IShowAnnotationsListener> listeners = new HashSet<>();

  /**
   * This collection contains all type names which are displayed in the editor.
   */
  private Collection<Type> typesToDisplay = new HashSet<>();

  /**
   * Editor annotation mode type. This variable is only set if the editor annotation mode type is
   * not already included in the typesToDisplay collection
   */
  private Type editorAnnotationMode;

  /**
   * Initializes a new instance.
   *
   * @param typeSystem
   *          the type system
   * @param shownTypes
   *          the shown types
   */
  public ShowAnnotationsMenu(TypeSystem typeSystem, Collection<Type> shownTypes) {
    super(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION), typeSystem);

    typesToDisplay.addAll(shownTypes);
  }

  /**
   * Adds the listener.
   *
   * @param listener
   *          the listener
   */
  public void addListener(IShowAnnotationsListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the listener.
   *
   * @param listener
   *          the listener
   */
  public void removeListener(IShowAnnotationsListener listener) {
    listeners.remove(listener);
  }

  @Override
  protected void insertAction(final Type type, Menu parentMenu) {
    final MenuItem actionItem = new MenuItem(parentMenu, SWT.CHECK);
    actionItem.setText(type.getName());

    // TODO: find another way to select the annotation mode also
    if (editorAnnotationMode != null && editorAnnotationMode.equals(type)) {
      actionItem.setSelection(true);
    }

    if (typesToDisplay.contains(type)) {
      actionItem.setSelection(true);
    }

    // TODO: move this to an action
    // do not access mTypesToDisplay directly !!!
    actionItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (actionItem.getSelection()) {
          typesToDisplay.add(type);

        } else {
          typesToDisplay.remove(type);
        }

        fireChanged();
      }
    });
  }

  /**
   * Gets the selected types.
   *
   * @return the selected types
   */
  public Collection<Type> getSelectedTypes() {
    Collection<Type> selectedTypes = new LinkedList<>();

    if (editorAnnotationMode != null) {
      selectedTypes.add(editorAnnotationMode);
    }

    selectedTypes.addAll(typesToDisplay);

    return Collections.unmodifiableCollection(selectedTypes);
  }

  /**
   * Fire changed.
   */
  private void fireChanged() {
    for (IShowAnnotationsListener listener : listeners) {
      listener.selectionChanged(getSelectedTypes());
    }
  }

  /**
   * Sets the selected type.
   *
   * @param type
   *          the type
   * @param isShown
   *          the is shown
   */
  public void setSelectedType(Type type, boolean isShown) {

    if (typesToDisplay.contains(type)) {
      if (!isShown) {
        typesToDisplay.remove(type);
        fireChanged();
      }
    } else {
      if (isShown) {
        typesToDisplay.add(type);
        fireChanged();
      }
    }
  }

  /**
   * Sets the selected types.
   *
   * @param types
   *          the new selected types
   */
  public void setSelectedTypes(Collection<Type> types) {
    typesToDisplay = new HashSet<>();
    typesToDisplay.addAll(types);

    for (IShowAnnotationsListener listener : listeners) {
      listener.selectionChanged(getSelectedTypes());
    }
  }

  /**
   * Sets the editor annotation mode.
   *
   * @param newMode
   *          the new editor annotation mode
   */
  public void setEditorAnnotationMode(Type newMode) {

    if (typesToDisplay.contains(newMode)) {
      if (editorAnnotationMode != null) {
        editorAnnotationMode = null;
        fireChanged();
      }
    } else {
      editorAnnotationMode = newMode;

      fireChanged();
    }
  }
}
