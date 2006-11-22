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

package org.apache.uima.tools.cpm;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

/**
 * A JTabbedPane which has a close ('X') icon on each tab. To add a tab, use the method
 * addTab(String, Component) To have an extra icon on each tab (e.g. showing the file type) use the
 * method addTab(String, Component, Icon). Clicking the 'X' closes the tab.
 * 
 * @see org.apache.uima.tools.cpm.TabClosedListener
 */

public class JTabbedPaneWithCloseIcons extends JTabbedPane implements MouseListener {
  private static final long serialVersionUID = 7680554379341154297L;

  private TabClosedListener tabClosedListener;

  public JTabbedPaneWithCloseIcons() {
    super();
    addMouseListener(this);
  }

  public void addTabClosedListener(TabClosedListener aTabClosedListener) {
    this.tabClosedListener = aTabClosedListener;
  }

  public void addTab(String title, Component component) {
    this.addTab(title, component, null);
  }

  public void addTab(String title, Component component, Icon extraIcon) {
    super.addTab(title, new CloseTabIcon(extraIcon), component);
  }

  public void moveTab(int fromIndex, int toIndex) {
    Component componentToMove = this.getComponentAt(fromIndex);
    String title = this.getTitleAt(fromIndex);
    Icon icon = this.getIconAt(fromIndex);
    this.remove(fromIndex);
    this.add(componentToMove, icon, toIndex);
    this.setTitleAt(toIndex, title);
  }

  public void mouseClicked(MouseEvent e) {
    int tabIndex = getUI().tabForCoordinate(this, e.getX(), e.getY());
    if (tabIndex < 0)
      return;
    Rectangle rect = ((CloseTabIcon) getIconAt(tabIndex)).getBounds();
    if (rect.contains(e.getX(), e.getY())) {
      removeTabAt(tabIndex);
      if (tabClosedListener != null)
        tabClosedListener.tabClosed(this, tabIndex);
    }
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
  }

  public void mouseReleased(MouseEvent e) {
  }
}

/**
 * The class which generates the 'X' icon for the tabs. The constructor accepts an icon which is
 * extra to the 'X' icon, so you can have tabs like in JBuilder. This value is null if no extra icon
 * is required.
 */
class CloseTabIcon implements Icon {
  private int x_pos;

  private int y_pos;

  private int width;

  private int height;

  private Icon fileIcon;

  public CloseTabIcon(Icon fileIcon) {
    this.fileIcon = fileIcon;
    width = 16;
    height = 16;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    this.x_pos = x;
    this.y_pos = y;
    Color col = g.getColor();
    g.setColor(Color.black);
    int y_p = y + 2;
    g.drawLine(x + 1, y_p, x + 12, y_p);
    g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
    g.drawLine(x, y_p + 1, x, y_p + 12);
    g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);
    g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
    g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
    g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
    g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
    g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
    g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
    g.setColor(col);
    if (fileIcon != null)
      fileIcon.paintIcon(c, g, x + width, y_p);
  }

  public int getIconWidth() {
    return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
  }

  public int getIconHeight() {
    return height;
  }

  public Rectangle getBounds() {
    return new Rectangle(x_pos, y_pos, width, height);
  }
}
