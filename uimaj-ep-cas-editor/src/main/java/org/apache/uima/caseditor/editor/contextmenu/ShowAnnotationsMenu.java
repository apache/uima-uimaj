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
import org.apache.uima.caseditor.editor.EditorAnnotationStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Creates the show annotations context sub menu.
 */
public class ShowAnnotationsMenu extends TypeMenu {

	private Set<IShowAnnotationsListener> listeners = new HashSet<IShowAnnotationsListener>();

	/**
	 * This collection contains all type names which are displayed in the
	 * editor.
	 */
	private Collection<Type> typesToDisplay = new HashSet<Type>();

	/**
	 * Editor annotation mode type. This variable is only set if the editor
	 * annotation mode type is not already included in the typesToDisplay
	 * collection
	 */
	private Type editorAnnotationMode;

	/**
	 * Initializes a new instance.
	 * 
	 * @param type
	 * @param typeSystem
	 */
	public ShowAnnotationsMenu(EditorAnnotationStatus status,
			TypeSystem typeSystem) {
		super(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION), typeSystem);

		for (String typeName : status.getDisplayAnnotations()) {
			typesToDisplay.add(typeSystem.getType(typeName));
		}
	}

	public void addListener(IShowAnnotationsListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IShowAnnotationsListener listener) {
		listeners.remove(listener);
	}

	@Override
	protected void insertAction(final Type type, Menu parentMenu) {
		final MenuItem actionItem = new MenuItem(parentMenu, SWT.CHECK);
		actionItem.setText(type.getShortName());

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
			public void handleEvent(Event e) {
				if (actionItem.getSelection()) {
					typesToDisplay.add(type);

				} else {
					typesToDisplay.remove(type);
				}

				for (IShowAnnotationsListener listener : listeners) {
					listener.selectionChanged(getSelectedTypes());
				}
			}
		});
	}

	public Collection<Type> getSelectedTypes() {
		Collection<Type> selectedTypes = new LinkedList<Type>();

		if (editorAnnotationMode != null) {
			selectedTypes.add(editorAnnotationMode);
		}
		
		for (Type type : typesToDisplay) {
			selectedTypes.add(type);
		}

		return Collections.unmodifiableCollection(selectedTypes);
	}

	public void setSelectedTypes(Collection<Type> types) {
		typesToDisplay = new HashSet<Type>();

		for (Type type : types) {
			typesToDisplay.add(type);
		}

		for (IShowAnnotationsListener listener : listeners) {
			listener.selectionChanged(getSelectedTypes());
		}
	}

	public void setEditorAnnotationMode(Type newMode) {

		if (typesToDisplay.contains(newMode)) {
			if (editorAnnotationMode != null) {
				editorAnnotationMode = null;
				for (IShowAnnotationsListener listener : listeners) {
					listener.selectionChanged(getSelectedTypes());
				}	
			}
		} else {
			editorAnnotationMode = newMode;

			for (IShowAnnotationsListener listener : listeners) {
				listener.selectionChanged(getSelectedTypes());
			}
		}
	}
}