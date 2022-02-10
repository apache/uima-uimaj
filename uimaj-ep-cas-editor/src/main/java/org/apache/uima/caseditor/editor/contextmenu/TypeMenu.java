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

import java.util.List;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Abstract base class for menus which display the type hierarchy.
 */
abstract class TypeMenu extends ContributionItem {

  /** The m parent type. */
  private Type mParentType;

  /** The m type system. */
  private TypeSystem mTypeSystem;

  /**
   * Initializes a new instance.
   *
   * @param parentType
   *          the parent type
   * @param typeSystem
   *          the type system
   */
  TypeMenu(Type parentType, TypeSystem typeSystem) {
    mParentType = parentType;
    mTypeSystem = typeSystem;
  }

  /**
   * Fills the menu with type entries.
   *
   * @param menu
   *          the menu
   * @param index
   *          the index
   */
  @Override
  public void fill(Menu menu, int index) {
    fillTypeMenu(mParentType, menu, false);
  }

  /**
   * Fill type menu.
   *
   * @param parentType
   *          the parent type
   * @param parentMenu
   *          the parent menu
   * @param isParentIncluded
   *          the is parent included
   */
  private void fillTypeMenu(Type parentType, Menu parentMenu, boolean isParentIncluded) {

    List<Type> childs = mTypeSystem.getDirectSubtypes(parentType);

    Menu newSubMenu;

    // has this type sub types ?
    // yes
    if (childs.size() != 0) {

      if (isParentIncluded) {
        MenuItem subMenuItem = new MenuItem(parentMenu, SWT.CASCADE);
        subMenuItem.setText(parentType.getName());

        newSubMenu = new Menu(subMenuItem);
        subMenuItem.setMenu(newSubMenu);
      } else {
        newSubMenu = parentMenu;
      }

      insertAction(parentType, newSubMenu);

      for (Type child : childs) {
        fillTypeMenu(child, newSubMenu, true);
      }
    }
    // no
    else {
      insertAction(parentType, parentMenu);
    }
  }

  /**
   * Implementing classes must implement this method to insert actions into the type menu.
   *
   * @param type
   *          the type
   * @param parentMenu
   *          the parent menu
   */
  protected abstract void insertAction(final Type type, Menu parentMenu);
}
