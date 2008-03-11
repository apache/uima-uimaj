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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import org.apache.uima.tools.images.Images;

/**
 * 
 * A composite component comprising a JList and associated controls used to add and remove list
 * elements and to control their ordering.
 */
public class ListSelector extends JPanel implements ActionListener {
  private static final long serialVersionUID = 6426556774940666223L;

  private DefaultListModel listModel = new DefaultListModel();

  private JList list;

  private JTextField addField;

  private SmallButton addButton;

  private SmallButton removeButton;

  private ImageButton moveUpButton;

  private ImageButton moveDownButton;

  public ListSelector(Object[] listData) {
    for (int i = 0; i < listData.length; i++)
      listModel.addElement(listData[i]);

    setLayout(new BorderLayout(4, 4));
    list = new JList(listModel);
    list.setFixedCellWidth(200);
    list.setVisibleRowCount(3);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Border etchedBorder = BorderFactory.createEtchedBorder();
    list.setBorder(etchedBorder);

    JScrollPane scrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    add(scrollPane, BorderLayout.CENTER);

    JPanel controlPanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(2, 2, 2, 2);
    controlPanel.setLayout(gbl);

    addField = new JTextField(6);
    addField.addActionListener(this);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.NORTHEAST;
    controlPanel.add(addField, gbc);

    gbc.gridx = 1;
    gbc.anchor = GridBagConstraints.NORTHWEST;

    addButton = new SmallButton("Add");
    addButton.addActionListener(this);
    controlPanel.add(addButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;

    JPanel movePanel = new JPanel();
    movePanel.setLayout(new GridLayout(1, 2, 4, 4));

    moveUpButton = new ImageButton(Images.UP);
    moveUpButton.addActionListener(this);
    movePanel.add(moveUpButton);

    moveDownButton = new ImageButton(Images.DOWN);
    moveDownButton.addActionListener(this);
    movePanel.add(moveDownButton);

    controlPanel.add(movePanel, gbc);

    gbc.gridx = 1;
    gbc.anchor = GridBagConstraints.WEST;

    gbc.anchor = GridBagConstraints.WEST;
    removeButton = new SmallButton("Remove");
    removeButton.addActionListener(this);
    controlPanel.add(removeButton, gbc);

    add(controlPanel, BorderLayout.EAST);
  }

  public void populate(Object[] listData) {
    listModel.clear();

    for (int i = 0; i < listData.length; i++)
      listModel.addElement(listData[i]);

    validate();
  }

  public String[] getValues() {
    Object[] valuesArray = listModel.toArray();
    if (valuesArray.length == 0) {
      return null;
    } else {
      String[] strArray = new String[valuesArray.length];
      for (int i = 0; i < valuesArray.length; i++)
        strArray[i] = valuesArray[i].toString();
      return strArray;
    }
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == addField || source == addButton) {
      String text = addField.getText();

      if (text.length() > 0 && !listModel.contains(text)) {
        listModel.addElement(text);
        list.ensureIndexIsVisible((listModel.getSize() - 1));
      }

      addField.setText("");
    } else if (source == removeButton) {
      Object selectedValue = list.getSelectedValue();
      if (selectedValue != null) {
        int rv = JOptionPane
                .showConfirmDialog((Component) source, "Are you sure you want to remove "
                        + selectedValue, null, JOptionPane.YES_NO_OPTION);

        if (rv == JOptionPane.YES_OPTION)
          listModel.remove(list.getSelectedIndex());
      } else {
        JOptionPane.showMessageDialog(this, "You must first select an item to be removed", null,
                JOptionPane.WARNING_MESSAGE);
      }
    } else // It's a move button
    {
      Object selectedValue = list.getSelectedValue();
      int selectedIndex = list.getSelectedIndex();
      int maxIndex = (listModel.getSize() - 1);
      if (selectedValue != null) {
        if ((source == moveUpButton && selectedIndex == 0)
                || (source == moveDownButton && selectedIndex == maxIndex)) {
          Toolkit.getDefaultToolkit().beep();
          list.clearSelection();
          return;
        } else {
          int newIndex = 0;
          if (source == moveUpButton)
            newIndex = selectedIndex - 1;
          else
            newIndex = selectedIndex + 1;

          listModel.remove(selectedIndex);
          listModel.insertElementAt(selectedValue, newIndex);
          list.setSelectedIndex(newIndex);
          list.ensureIndexIsVisible(newIndex);
        }
      } else {
        JOptionPane.showMessageDialog(this, "You must first select an item to reorder", null,
                JOptionPane.WARNING_MESSAGE);
      }
    }
  }

  static class SmallButton extends JButton {
    private static final long serialVersionUID = -4311761385714783114L;

    public SmallButton(String s) {
      super(s);
    }

    public Insets getInsets() {
      return new Insets(3, 6, 3, 6);
    }
  }
}
