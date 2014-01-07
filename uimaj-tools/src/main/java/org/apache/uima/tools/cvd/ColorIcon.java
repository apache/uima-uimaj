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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * TODO: add type comment for <code>ColorIcon</code>.
 * 
 * 
 */
public class ColorIcon implements Icon {

  private static final int size = 20;

  private Color color = Color.black;

  
  public ColorIcon() {
    super();
  }

  public ColorIcon(Color color) {
    this();
    this.color = color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
   */
  public void paintIcon(Component arg0, Graphics graphics, int x, int y) {
    graphics.setColor(this.color);
    graphics.fill3DRect(x, y, size, size, true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.Icon#getIconWidth()
   */
  public int getIconWidth() {
    return size;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.Icon#getIconHeight()
   */
  public int getIconHeight() {
    return size;
  }

}
