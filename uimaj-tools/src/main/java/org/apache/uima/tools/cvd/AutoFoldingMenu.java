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

package org.apache.uima.tools.cvd;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Auto-folding menu. It overrides the add(JMenuItem) method only.
 * 
 * <p>
 * Works, but it's not pretty.
 */
public class AutoFoldingMenu extends JMenu {

  private static final long serialVersionUID = 7077427321467278198L;

  /** The Constant DEFAULT_MENU_SIZE. */
  public static final int DEFAULT_MENU_SIZE = 12;

  /** The max size. */
  private final int maxSize;

  /** The count. */
  private int count = 0;

  /** The submenu. */
  private AutoFoldingMenu submenu = null;

  /**
   * Instantiates a new auto folding menu.
   *
   * @param title
   *          the title
   * @param max
   *          the max
   */
  public AutoFoldingMenu(String title, int max) {
    super(title);
    this.maxSize = max;
  }

  /**
   * Instantiates a new auto folding menu.
   *
   * @param title
   *          the title
   */
  public AutoFoldingMenu(String title) {
    this(title, DEFAULT_MENU_SIZE);
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.JMenu#add(javax.swing.JMenuItem)
   */
  @Override
  public JMenuItem add(JMenuItem c) {
    if (this.count < this.maxSize) {
      ++this.count;
      return super.add(c);
    }
    if (this.submenu == null) {
      this.submenu = new AutoFoldingMenu("More...", this.maxSize);
      addSeparator();
      super.add(this.submenu);
    }
    return this.submenu.add(c);

  }

}
