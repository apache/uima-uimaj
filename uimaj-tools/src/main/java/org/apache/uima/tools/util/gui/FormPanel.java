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

package org.apache.uima.tools.util.gui;

/*
 * Created on Jun 12, 2003
 * 
 * A specialist component used to create input forms in a 2 or 4 column layout with right-aligned
 * labels on the left and corresponding component left-aligned in the right column. Components are
 * arranged in the order that they are added.
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Class FormPanel.
 */
public class FormPanel extends JPanel {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 6935744171267722900L;

  /** The Constant weights. */
  private static final double weights[] = { 0.0, 0.0, 0.0, 0.0 };

  /** The Constant anchors. */
  private static final int anchors[] = { GridBagConstraints.NORTHEAST, GridBagConstraints.NORTHWEST,
      GridBagConstraints.NORTHEAST, GridBagConstraints.NORTHWEST };

  /** The gbl. */
  private GridBagLayout gbl;

  /** The gbc. */
  private GridBagConstraints gbc;

  /** The regular insets. */
  private Insets regularInsets = new Insets(1, 4, 1, 4);

  /** The check box insets. */
  private Insets checkBoxInsets = new Insets(1, 0, 1, 0);

  /** The nr columns. */
  private int nrColumns = 2; // Default

  /** The grid bag panel. */
  protected JPanel gridBagPanel; // Inner panel

  /** The component index. */
  protected int componentIndex = 0;

  /**
   * Instantiates a new form panel.
   *
   * @param nrColumns
   *          the nr columns
   */
  public FormPanel(int nrColumns) {
    this();
    this.nrColumns = nrColumns;
  }

  /**
   * Instantiates a new form panel.
   */
  public FormPanel() {
    setLayout(new BorderLayout());
    gridBagPanel = new JPanel();

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gridBagPanel.setLayout(gbl);

    add(gridBagPanel, BorderLayout.NORTH);
  }

  /**
   * Adds the.
   *
   * @param c
   *          the c
   */
  public void add(JComponent c) {
    if (c instanceof JCheckBox)
      gbc.insets = checkBoxInsets;
    else
      gbc.insets = regularInsets;

    gbc.gridx = (componentIndex % nrColumns);
    gbc.gridy = componentIndex / nrColumns;
    gbc.anchor = anchors[gbc.gridx];
    gbc.weightx = weights[gbc.gridx];
    gridBagPanel.add(c, gbc);

    componentIndex++;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.JComponent#setEnabled(boolean)
   */
  /*
   * Enables or disables the panel and all its components.
   * 
   * @see java.awt.Component#setEnabled(boolean)
   */
  @Override
  public void setEnabled(boolean onOff) {
    super.setEnabled(onOff);
    Component components[] = gridBagPanel.getComponents();
    for (int i = 0; i < components.length; i++)
      components[i].setEnabled(onOff);
  }

  /**
   * Gets the nr components.
   *
   * @return the nr components
   */
  public int getNrComponents() {
    return componentIndex;
  }

  /**
   * Given a configuration parameter name, this method returns a suitable field caption string. e.g.
   * outputDir would return Output Dir:
   * 
   * @param name
   *          The configuration parameter name
   * @return The field caption string
   */
  protected String getCaptionFromName(String name) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (i == 0)
        sb.append(String.valueOf(c).toUpperCase());
      else {
        if (c >= 'A' && c <= 'Z')
          sb.append(" ");

        sb.append(c);
      }
    }
    sb.append(':');

    return sb.toString();
  }
}
