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

import java.util.HashSet;
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
 * Creates the mode context sub menu.
 */
public class ModeMenu extends TypeMenu {
	
  private Set<IModeMenuListener> listeners = new HashSet<IModeMenuListener>();

  /**
   * Initializes a new instance.
   *
   * @param type
   * @param typeSystem
   */
  public ModeMenu(TypeSystem typeSystem) {
    super(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION), typeSystem);
  }

  public void addListener(IModeMenuListener listener) {
	  listeners.add(listener);
  }
  
  public void removeListener(IModeMenuListener listener) {
	  listeners.remove(listener);
  }
  
  @Override
  protected void insertAction(final Type type, Menu parentMenu) {
    MenuItem actionItem = new MenuItem(parentMenu, SWT.PUSH);
    actionItem.setText(type.getShortName());

    actionItem.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {

    	for (IModeMenuListener listener : listeners) {
    		listener.modeChanged(type);
    	}
      }
    });
  }
}