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
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/**
 * TODO: add type comment for <code>AnnotationDisplayCustomizationFrame</code>.
 * 
 * 
 */
public class AnnotationDisplayCustomizationFrame extends JFrame {

  private static final long serialVersionUID = -6695661439132793537L;

  private class TypeTreeSelectionListener implements TreeSelectionListener {
    /**
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    public void valueChanged(TreeSelectionEvent event) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) AnnotationDisplayCustomizationFrame.this.tree
          .getLastSelectedPathComponent();
      String typeName = (String) node.getUserObject();
      if (typeName.equals(AnnotationDisplayCustomizationFrame.this.currentTypeName)) {
        return;
      }
      setCustomizationPanel(typeName);
    }

  }

  private class CustomizeBgButtonHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      Color color = JColorChooser.showDialog(AnnotationDisplayCustomizationFrame.this,
          "Choose color", AnnotationDisplayCustomizationFrame.this.bgColor);
      if (color != null) {
        AnnotationDisplayCustomizationFrame.this.bgColor = color;
        AnnotationDisplayCustomizationFrame.this.bgIcon.setColor(color);
        StyleConstants.setBackground(AnnotationDisplayCustomizationFrame.this.currentStyle, color);
        setTextPane();
        enableButtons(true);
        AnnotationDisplayCustomizationFrame.this.repaint();
      }
    }

  }

  private class CustomizeFgButtonHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      Color color = JColorChooser.showDialog(AnnotationDisplayCustomizationFrame.this,
          "Choose color", AnnotationDisplayCustomizationFrame.this.fgColor);
      if (color != null) {
        AnnotationDisplayCustomizationFrame.this.fgColor = color;
        AnnotationDisplayCustomizationFrame.this.fgIcon.setColor(color);
        StyleConstants.setForeground(AnnotationDisplayCustomizationFrame.this.currentStyle, color);
        setTextPane();
        enableButtons(true);
        AnnotationDisplayCustomizationFrame.this.repaint();
      }
    }

  }

  private class AcceptButtonHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      Style style = AnnotationDisplayCustomizationFrame.this.styleMap
          .get(AnnotationDisplayCustomizationFrame.this.currentTypeName);
      if (style == null) {
        style = AnnotationDisplayCustomizationFrame.this.textPane.addStyle(
            AnnotationDisplayCustomizationFrame.this.currentTypeName,
            AnnotationDisplayCustomizationFrame.this.styleMap.get(CAS.TYPE_NAME_ANNOTATION));
      }
      StyleConstants.setForeground(style, StyleConstants
          .getForeground(AnnotationDisplayCustomizationFrame.this.currentStyle));
      StyleConstants.setBackground(style, StyleConstants
          .getBackground(AnnotationDisplayCustomizationFrame.this.currentStyle));
      AnnotationDisplayCustomizationFrame.this.styleMap.put(
          AnnotationDisplayCustomizationFrame.this.currentTypeName, style);
      enableButtons(false);
      AnnotationDisplayCustomizationFrame.this.repaint();
    }

  }

  private class CancelButtonHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      Style style = AnnotationDisplayCustomizationFrame.this.styleMap
          .get(AnnotationDisplayCustomizationFrame.this.currentTypeName);
      if (style == null) {
        style = AnnotationDisplayCustomizationFrame.this.styleMap.get(CAS.TYPE_NAME_ANNOTATION);
      }
      // assert(style != null);
      AnnotationDisplayCustomizationFrame.this.fgColor = StyleConstants.getForeground(style);
      AnnotationDisplayCustomizationFrame.this.fgIcon
          .setColor(AnnotationDisplayCustomizationFrame.this.fgColor);
      AnnotationDisplayCustomizationFrame.this.bgColor = StyleConstants.getBackground(style);
      AnnotationDisplayCustomizationFrame.this.bgIcon
          .setColor(AnnotationDisplayCustomizationFrame.this.bgColor);
      setCurrentStyle(style);
      setTextPane();
      enableButtons(false);
      AnnotationDisplayCustomizationFrame.this.repaint();
    }

  }

  private static final int FG = 0;

  private static final int BG = 1;

  private JSplitPane splitPane;

  private Color fgColor;

  private Color bgColor;

  private ColorIcon fgIcon;

  private ColorIcon bgIcon;

  private JTextPane textPane;

  private static final String defaultStyleName = "defaultUnannotStyle";

  private static final String currentStyleName = "currentStyle";

  private Style currentStyle;

  private String currentTypeName;

  private JButton acceptButton;

  private JButton cancelButton;

  private Map<String, Style> styleMap;

  private JTree tree;

  /**
   * @throws java.awt.HeadlessException
   */
  public AnnotationDisplayCustomizationFrame() {
    super();
  }

  /**
   * @param arg0
   */
  public AnnotationDisplayCustomizationFrame(GraphicsConfiguration arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   * @throws java.awt.HeadlessException
   */
  public AnnotationDisplayCustomizationFrame(String arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   */
  public AnnotationDisplayCustomizationFrame(String arg0, GraphicsConfiguration arg1) {
    super(arg0, arg1);
  }

  private void setCurrentStyle(Style style) {
    // Copy style.
    this.currentStyle = this.textPane.addStyle(currentStyleName, style);
    StyleConstants.setForeground(this.currentStyle, StyleConstants.getForeground(style));
    StyleConstants.setBackground(this.currentStyle, StyleConstants.getBackground(style));
  }

  private void enableButtons(boolean flag) {
    this.acceptButton.setEnabled(flag);
    this.cancelButton.setEnabled(flag);
  }

  public void init(Map<String, Style> styleMap1, CAS cas) {
    this.styleMap = styleMap1;
    this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    this.setContentPane(this.splitPane);
    this.tree = new JTree(createTreeModel(cas.getTypeSystem()));
    this.tree.addTreeSelectionListener(new TypeTreeSelectionListener());
    JScrollPane treeScrollPane = new JScrollPane(this.tree);
    this.splitPane.setLeftComponent(treeScrollPane);
    this.splitPane.setRightComponent(createCustomizationPanel(CAS.TYPE_NAME_ANNOTATION));
  }

  private JPanel createCustomizationPanel(String typeName) {
    this.currentTypeName = typeName;
    String defaultAnnotStyleName = CAS.TYPE_NAME_ANNOTATION;
    Style defaultAnnotStyle = this.styleMap.get(defaultAnnotStyleName);
    GridLayout layout = new GridLayout(0, 1);
    JPanel topPanel = new JPanel(layout);
    this.textPane = new JTextPane();
    Style style = this.styleMap.get(typeName);
    if (style == null) {
      style = defaultAnnotStyle;
    }
    Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    this.textPane.addStyle(defaultStyleName, defaultStyle);
    setCurrentStyle(style);
    this.fgColor = StyleConstants.getForeground(this.currentStyle);
    this.bgColor = StyleConstants.getBackground(this.currentStyle);
    this.fgIcon = new ColorIcon(this.fgColor);
    this.bgIcon = new ColorIcon(this.bgColor);
    topPanel.add(createColorPanel("Foreground: ", this.fgIcon, FG));
    topPanel.add(createColorPanel("Background: ", this.bgIcon, BG));
    setTextPane();
    topPanel.add(this.textPane);
    JPanel buttonPanel = new JPanel();
    createButtonPanel(buttonPanel);
    topPanel.add(buttonPanel);
    return topPanel;
  }

  private void setCustomizationPanel(String typeName) {
    this.currentTypeName = typeName;
    Style defaultAnnotStyle = this.styleMap.get(CAS.TYPE_NAME_ANNOTATION);
    Style style = this.styleMap.get(typeName);
    if (style == null) {
      style = defaultAnnotStyle;
    }
    // Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(
    // StyleContext.DEFAULT_STYLE);
    setCurrentStyle(style);
    this.fgColor = StyleConstants.getForeground(this.currentStyle);
    this.bgColor = StyleConstants.getBackground(this.currentStyle);
    this.fgIcon.setColor(this.fgColor);
    this.bgIcon.setColor(this.bgColor);
    setTextPane();
    enableButtons(false);
    this.repaint();
  }

  private void createButtonPanel(JPanel buttonPanel) {
    this.acceptButton = new JButton("OK");
    this.acceptButton.addActionListener(new AcceptButtonHandler());
    buttonPanel.add(this.acceptButton);
    this.cancelButton = new JButton("Cancel");
    this.cancelButton.addActionListener(new CancelButtonHandler());
    buttonPanel.add(this.cancelButton);
    enableButtons(false);
  }

  private void setTextPane() {
    Style defaultStyle = this.textPane.getStyle(defaultStyleName);
    // Style style = textPane.getStyle(typeName);
    // if (style == null || defaultStyle == null) {
    // System.out.println("Style is null.");
    // }
    Document doc = this.textPane.getDocument();
    try {
      doc.remove(0, doc.getLength());
      doc.insertString(doc.getLength(), "This is what an ", defaultStyle);
      doc.insertString(doc.getLength(), "annotation", this.currentStyle);
      doc.insertString(doc.getLength(), " of type ", defaultStyle);
      doc.insertString(doc.getLength(), this.currentTypeName, this.currentStyle);
      doc.insertString(doc.getLength(), " will look like.", defaultStyle);
    } catch (BadLocationException e) {
      // assert(false);
    }
    this.textPane.repaint();
  }

  private JPanel createColorPanel(String text, ColorIcon icon, int buttonType) {
    JPanel colorPanel = new JPanel();
    JLabel label = new JLabel(text);
    colorPanel.add(label);
    label = new JLabel(icon);
    colorPanel.add(label);
    JButton button = new JButton("Customize");
    if (buttonType == FG) {
      button.addActionListener(new CustomizeFgButtonHandler());
    } else {
      button.addActionListener(new CustomizeBgButtonHandler());
    }
    colorPanel.add(button);
    return colorPanel;
  }

  private TreeModel createTreeModel(TypeSystem ts) {
    String typeName = CAS.TYPE_NAME_ANNOTATION;
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(typeName);
    // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
    org.apache.uima.cas.Type type = ts.getType(typeName);
    addChildren(node, type, ts);
    DefaultTreeModel treeModel = new DefaultTreeModel(node);
    return treeModel;
  }

  private static void addChildren(DefaultMutableTreeNode node, org.apache.uima.cas.Type type,
          TypeSystem ts) {
    // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
    List<org.apache.uima.cas.Type> dtrs = ts.getDirectSubtypes(type);
    DefaultMutableTreeNode dtrNode;
    org.apache.uima.cas.Type dtrType;
    for (int i = 0; i < dtrs.size(); i++) {
      dtrType = dtrs.get(i);
      dtrNode = new DefaultMutableTreeNode(dtrType.getName());
      node.add(dtrNode);
      addChildren(dtrNode, dtrType, ts);
    }
  }
}
