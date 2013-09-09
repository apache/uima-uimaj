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

package org.apache.uima.tools.stylemap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.util.gui.ImageButton;
import org.apache.uima.tools.util.htmlview.AnnotationViewGenerator;

/**
 * A GUI for editing style maps for the annotation viewer.
 * <p>
 * A style map is an XML document that describes how each kind of output from a Text Analysis Engine
 * is to be displayed in the annotation viewer. Each output type that the user wants to be displayed
 * will have an entry in the style map. Each entry in the style map contains:
 * <ul>
 * <li>The CSS style used to display annotations of that type (for example "background:blue;
 * color:white;" to display white text on a blue background.</li>
 * <li>A label used to identify the annotations in the annotation viewer</li>
 * </ul>
 * <p>
 * To invoke the editor, call the {@link #launchEditor(AnalysisEngineMetaData, String, CAS)} method.
 * 
 * 
 * 
 */
public class StyleMapEditor extends JDialog implements ActionListener {
  private static final long serialVersionUID = -7774771368169207250L;

  private boolean buttonPress = false;

  private boolean populated;

  private AnnotationFeaturesViewer annotationFeaturesViewer;

  private ImageButton addTableEntryButton;

  private StyleMapTableModel tableModel;

  private StyleMapTable annotationsTable;

  private ArrayList styleList;

  private ImageButton removeTableRowButton;

  private ImageButton moveRowUpButton;

  private ImageButton moveRowDownButton;

  private JButton okButton = new JButton("Save");

  private JButton cancelButton = new JButton("Cancel");

  private JButton resetButton = new JButton("Reset");

  private JDialog styleMapEditorDialog;

  private HashMap colorNameMap;

  private AnalysisEngineMetaData analysisEngineMetaData;

  private AnalysisEngineDescription ae;

  private TableGUIMediator med;

  // data for data model
  Object[][] data;

  /**
   * 
   * Creates a new `Editor.
   */
  public StyleMapEditor(final JFrame aOwner, CAS cas) {
    super(aOwner, "Style Map Editor", true);
    populated = false; // table not yet loaded
    styleList = new ArrayList();
    // Save this in class member variable for use by inner-classes:
    styleMapEditorDialog = this;

    getContentPane().setLayout(new BorderLayout());

    JPanel annotationFeaturesPanel = new JPanel();
    annotationFeaturesPanel.setLayout(new BorderLayout());

    annotationFeaturesViewer = new AnnotationFeaturesViewer();
    annotationFeaturesPanel.add(annotationFeaturesViewer, BorderLayout.CENTER);

    JPanel iconPanel = new JPanel();

    addTableEntryButton = new ImageButton(Images.FORWARD);
    addTableEntryButton.setToolTipText("Create stylemap table entry");
    addTableEntryButton.addActionListener(this);

    iconPanel.add(addTableEntryButton);

    annotationFeaturesPanel.add(iconPanel, BorderLayout.EAST);

    JPanel tablePanel = new JPanel();
    tablePanel.setLayout(new BorderLayout());

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
            annotationFeaturesPanel, tablePanel);
    Dimension screenDimension = getToolkit().getScreenSize();
    splitPane.setDividerLocation((int) (screenDimension.width * 0.28));

    okButton.setToolTipText("Save stylemap and exit");
    cancelButton.setToolTipText("Exit without saving");
    resetButton.setToolTipText("Reset stylemap to that auto-generated from metadata");

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.add(okButton);
    buttonsPanel.add(cancelButton);
    buttonsPanel.add(resetButton);

    getContentPane().add(splitPane, BorderLayout.CENTER);
    getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

    // creating a jtable to hold the styles

    tableModel = new StyleMapTableModel(StyleConstants.columnNames);
    med = new TableGUIMediator();
    annotationsTable = new StyleMapTable(tableModel, annotationFeaturesViewer, this, med);
    med.setTable(annotationsTable);

    annotationsTable.setDefaultRenderer(Color.class, new ColorRenderer(annotationsTable));
    setUpColorEditor(annotationsTable);
    annotationsTable.setDefaultEditor(String.class, new LabelCellEditor());

    final JScrollPane scrollPane = new JScrollPane();
    scrollPane.getViewport().add(annotationsTable, null);

    tablePanel.add(scrollPane, BorderLayout.CENTER);

    JPanel tableButtonsPanel = new JPanel();
    moveRowUpButton = new ImageButton(Images.UP);
    moveRowUpButton.setToolTipText("Move Row Up");
    moveRowUpButton.addActionListener(this);
    tableButtonsPanel.add(moveRowUpButton);

    moveRowDownButton = new ImageButton(Images.DOWN);
    moveRowDownButton.setToolTipText("Move Row Down");
    moveRowDownButton.addActionListener(this);
    tableButtonsPanel.add(moveRowDownButton);

    removeTableRowButton = new ImageButton(Images.ROW_DELETE);
    removeTableRowButton.setToolTipText("Delete Row");
    removeTableRowButton.addActionListener(this);
    tableButtonsPanel.add(removeTableRowButton);

    // pass all these to the mediator which turns them on and off
    med.setButtons(moveRowUpButton, moveRowDownButton, removeTableRowButton);
    tablePanel.add(tableButtonsPanel, BorderLayout.NORTH);

    MyCellRenderer cellRenderer = new MyCellRenderer();

    TableColumn tc = annotationsTable.getColumnModel().getColumn(0);
    tc.setCellRenderer(cellRenderer);

    TableColumn labelTableColumn = annotationsTable.getColumnModel().getColumn(
            StyleConstants.LABEL_COLUMN);
    TableColumn typeNameTableColumn = annotationsTable.getColumnModel().getColumn(
            StyleConstants.TYPE_NAME_COLUMN);
    // TableColumn featureValueTableColumn =
    // annotationsTable.getColumnModel().getColumn(StyleConstants.FEATURE_VALUE_COLUMN);

    labelTableColumn.setCellRenderer(cellRenderer);
    typeNameTableColumn.setCellRenderer(cellRenderer);
    // featureValueTableColumn.setCellRenderer(cellRenderer);

    // Style Edit OK Button event handle
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        Vector vn = new Vector();
        for (int row = 0; row < styleList.size(); row++) {
          StyleMapEntry e = (StyleMapEntry) styleList.get(row);
          e.setAnnotationTypeName("" + tableModel.getValueAt(row, StyleConstants.TYPE_NAME_COLUMN));
          e.setLabel("" + tableModel.getValueAt(row, StyleConstants.LABEL_COLUMN));
          // e.setFeatureValue( "" + tableModel.getValueAt(row,
          // StyleConstants.FEATURE_VALUE_COLUMN));
          vn.add(e.getLabel());
        }

        buttonPress = true;
        Vector noDups = new Vector();
        Object obj = null;
        for (Iterator itr = vn.iterator(); itr.hasNext();) {
          obj = itr.next();
          if ((obj != null) && !obj.equals("")) {
            if (noDups.contains(obj)) {
              JOptionPane.showMessageDialog(StyleMapEditor.this, ("Duplicate Label :  " + obj
                      + "\n" + "Change the Label and click OK!"), "Duplicate Values",
                      JOptionPane.PLAIN_MESSAGE);
              buttonPress = false;
              break;
            } else {
              noDups.addElement(obj);
            }
          }
        }

        if (buttonPress)
          styleMapEditorDialog.setVisible(false);
      }
    });

    // Style Edit Exit Button Event Handling
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        buttonPress = false;
        styleMapEditorDialog.setVisible(false);
      }
    });

    resetButton.addActionListener(this);

    pack();
  }

  public Dimension getPreferredSize() {
    Dimension screenSize = getToolkit().getScreenSize();
    Dimension d = super.getPreferredSize();
    return new Dimension(screenSize.width, d.height);

    // return getToolkit().getScreenSize();
  }

  /**
   * @deprecated use {@link #setAnalysisEngine(AnalysisEngineDescription)} instead.
   */
  @Deprecated
  public void setTextAnalysisEngine(org.apache.uima.analysis_engine.TaeDescription tae) {
    this.ae = tae;
  }

  public void setAnalysisEngine(AnalysisEngineDescription ae) {
    this.ae = ae;
  }

  /**
   * Displays the StyleMapEditor GUI and allows the user to edit a style map. When the user has
   * finished, the new style map is returned.
   * 
   * @param aAnalysisEngineMetaData
   *          Metadata for the AnalysisEngine whose style map is to be edited. This contains the
   *          AE's capabilities and type system definition, which are needed by the editor.
   * @param aStyleMapXml
   *          An existing style map XML document that will be loaded into the editor. This is
   *          optional, if null is passed in, a default style map will be automatically generated
   *          from the AE metadata.
   * 
   * @return a new style map XML document. If the user cancels, null is returned.
   */
  public String launchEditor(AnalysisEngineMetaData aAnalysisEngineMetaData, String aStyleMapXml,
          CAS cas) {
    analysisEngineMetaData = aAnalysisEngineMetaData;
    // create an ArrayList of style entries used by the GUI
    ArrayList styleList = createStyleList(aAnalysisEngineMetaData, aStyleMapXml);
    // display the GUI and allow user to interact with it (modifying the styleList)
    if (launchGUI(styleList, cas)) {
      // user clicked OK, so produce a new style map XML doc from the style list
      return generateStyleMap(styleList);
    } else {
      // user cancelled; return null
      return null;
    }
  }

  /**
   * Creates a List of StyleMapEntry objects from the given AnalysisEngineMetaData and style map
   * XML.
   * 
   * @param aAnalysisEngineMetaData
   *          Metadata for the AnalysisEngine whose style map is being edited.
   * @param aStyleMapXml
   *          An existing style map XML document. This is optional, if null is passed in, a default
   *          style map will be automatically generated.
   * 
   * @return an ArrayList containing one {@link StyleMapEntry} object for each output type declared
   *         in <code>aTaeMetaData</code>.
   */
  public ArrayList createStyleList(AnalysisEngineMetaData aAnalysisEngineMetaData,
          String aStyleMapXml) {
    styleList = new ArrayList();

    // Parse the style map XML and create StyleMapEntry elements appropriately.
    // Note: we need to support the HTML color names as well as their hex codes;
    // we could do that by hardcoding a hashmap with color names as keys and
    // the corresponding Color objects as values.

    // If stylemap XML is null, auto-generate it from metadata capabilities:
    if (aStyleMapXml == null)
      aStyleMapXml = AnnotationViewGenerator.autoGenerateStyleMap(aAnalysisEngineMetaData);

    return parseStyleList(aStyleMapXml);
  }

  public ArrayList parseStyleList(String aStyleMapXml) {
    StyleMapXmlParser smxp = new StyleMapXmlParser(aStyleMapXml);
    // System.out.println(aStyleMapXml );
    Vector annotType_SME = smxp.annotType;
    Vector styleLabel_SME = smxp.styleLabel;
    Vector featureValue_SME = smxp.featureValue;
    Vector styleColor_SME = smxp.styleColor;

    ColorParser cp = new ColorParser();
    colorNameMap = cp.getColorNameMap();
    for (int i = 0; i < annotType_SME.size(); i++) {
      String typeName = ((String) annotType_SME.elementAt(i));
      String labelString = ((String) styleLabel_SME.elementAt(i));
      String featureValue = ((String) featureValue_SME.elementAt(i));

      String styleColor = styleColor_SME.elementAt(i).toString();
      StyleMapEntry e = cp.parseAndAssignColors(typeName, featureValue, labelString, styleColor);

      styleList.add(e);
    }

    return styleList;
  }

  public void actionPerformed(ActionEvent evt) {
    JComponent source = (JComponent) evt.getSource();

    if (source == moveRowUpButton) {
      int selectedRow = annotationsTable.getSelectedRow();
      if (selectedRow == -1) {
        JOptionPane.showMessageDialog(source, "Select table row", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
      } else if (selectedRow == 0) {
        Toolkit.getDefaultToolkit().beep();
      } else {
        tableModel.moveRowUp(selectedRow);
        ListSelectionModel lsm = annotationsTable.getSelectionModel();
        int newSelectedRow = selectedRow - 1;
        lsm.setSelectionInterval(newSelectedRow, newSelectedRow);

        StyleMapEntry e = (StyleMapEntry) styleList.get(selectedRow);
        styleList.remove(selectedRow);
        styleList.add(newSelectedRow, e);
      }
    } else if (source == moveRowDownButton) {
      int selectedRow = annotationsTable.getSelectedRow();
      if (selectedRow == -1) {
        JOptionPane.showMessageDialog(source, "Select table row", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
      } else if (selectedRow == (tableModel.getRowCount() - 1)) {
        Toolkit.getDefaultToolkit().beep();
      } else {
        tableModel.moveRowDown(selectedRow);
        ListSelectionModel lsm = annotationsTable.getSelectionModel();
        int newSelectedRow = selectedRow + 1;
        lsm.setSelectionInterval(newSelectedRow, newSelectedRow);

        StyleMapEntry e = (StyleMapEntry) styleList.get(selectedRow);
        styleList.remove(selectedRow);
        styleList.add(newSelectedRow, e);
      }
    } else if (source == addTableEntryButton) {
      String typeName = annotationFeaturesViewer.getSelection();
      if (typeName == null) {
        JOptionPane.showMessageDialog(source,
                "You must first select an annotation type or feature", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
      }

      addRow(typeName);
    } else if (source == removeTableRowButton) {
      int row = annotationsTable.getSelectedRow();
      if (row >= 0) {
        String message = "Are you sure you want to remove "
                + tableModel.getValueAt(row, StyleConstants.LABEL_COLUMN);
        int rv = JOptionPane.showConfirmDialog(removeTableRowButton, message, "Remove Table Row",
                JOptionPane.YES_NO_OPTION);
        if (rv == JOptionPane.YES_OPTION) {
          styleList.remove(row);
          styleList.trimToSize();
          tableModel.removeRow(row);

          ListSelectionModel lsm = annotationsTable.getSelectionModel();
          int newSelectedRow = (row == styleList.size() ? (row - 1) : row);
          lsm.setSelectionInterval(newSelectedRow, newSelectedRow);
        }
      } else {
        JOptionPane.showMessageDialog(source, "You must first select a table row to remove",
                "Error", JOptionPane.ERROR_MESSAGE);
      }
    } else if (source == resetButton) {
      styleList = createStyleList(analysisEngineMetaData, null);
      data = new Object[styleList.size()][StyleConstants.NR_TABLE_COLUMNS];
      for (int x = 0; x < styleList.size(); x++) {
        StyleMapEntry e = (StyleMapEntry) styleList.get(x);

        data[x][0] = "";
        data[x][StyleConstants.LABEL_COLUMN] = e.getLabel().trim();
        data[x][StyleConstants.TYPE_NAME_COLUMN] = e.getAnnotationTypeName();
        // data[x][StyleConstants.FEATURE_VALUE_COLUMN] = e.getFeatureValue();
        data[x][StyleConstants.BG_COLUMN] = e.getBackground();
        data[x][StyleConstants.FG_COLUMN] = e.getForeground();
        data[x][StyleConstants.CHECK_COLUMN] = Boolean.valueOf(e.getChecked());
        data[x][StyleConstants.HIDDEN_COLUMN] = Boolean.valueOf(e.getHidden());
      }

      tableModel.set(data);
      tableModel.fireTableDataChanged();
    }
  }

  protected StyleMapTable getAnnotationsTable() {
    return this.annotationsTable;
  }

  /**
   * Displays the Style Map Editor GUI and allows the user to interact with it.
   * 
   * @param aStyleList
   *          an ArrayList containing the style map entries to be edited. When the user modifies a
   *          setting in the GUI, the elements of this List will be updated.
   * 
   * @return true if the user exits the dialog by clicking the OK button, false if the user has
   *         clicked the Cancel button.
   */
  private boolean launchGUI(ArrayList aStyleList, CAS cas) {
    if (!populated) {
      // populate and display GUI here, then wait for user to click OK or Cancel
      styleList = aStyleList;
      data = new Object[aStyleList.size()][StyleConstants.NR_TABLE_COLUMNS];
      int maxColumnWidths[] = new int[StyleConstants.NR_TABLE_COLUMNS];

      maxColumnWidths[0] = 16;
      FontMetrics fm = annotationsTable.getFontMetrics(annotationsTable.getFont());
      // maxColumnWidths[StyleConstants.FEATURE_VALUE_COLUMN] =
      // fm.stringWidth(StyleConstants.columnNames[StyleConstants.FEATURE_VALUE_COLUMN]);
      maxColumnWidths[StyleConstants.BG_COLUMN] = fm
              .stringWidth(StyleConstants.columnNames[StyleConstants.BG_COLUMN]);
      maxColumnWidths[StyleConstants.FG_COLUMN] = fm
              .stringWidth(StyleConstants.columnNames[StyleConstants.FG_COLUMN]);
      maxColumnWidths[StyleConstants.CHECK_COLUMN] = 60;
      maxColumnWidths[StyleConstants.HIDDEN_COLUMN] = 60;

      for (int x = 0; x < aStyleList.size(); x++) {
        StyleMapEntry e = (StyleMapEntry) aStyleList.get(x);

        data[x][0] = "";
        data[x][StyleConstants.LABEL_COLUMN] = e.getLabel().trim();
        data[x][StyleConstants.TYPE_NAME_COLUMN] = e.getAnnotationTypeName();
        // data[x][StyleConstants.FEATURE_VALUE_COLUMN] = e.getFeatureValue();
        data[x][StyleConstants.BG_COLUMN] = e.getBackground();
        data[x][StyleConstants.FG_COLUMN] = e.getForeground();
        data[x][StyleConstants.CHECK_COLUMN] = Boolean.valueOf(e.getChecked());
        data[x][StyleConstants.HIDDEN_COLUMN] = Boolean.valueOf(e.getHidden());

        // Calculate adequate column widths:
        int typeNameWidth = fm.stringWidth(e.getAnnotationTypeName());
        if (typeNameWidth > maxColumnWidths[StyleConstants.TYPE_NAME_COLUMN])
          maxColumnWidths[StyleConstants.TYPE_NAME_COLUMN] = typeNameWidth;
        int labelWidth = fm.stringWidth(e.getLabel().trim());
        if (labelWidth > maxColumnWidths[StyleConstants.LABEL_COLUMN])
          maxColumnWidths[StyleConstants.LABEL_COLUMN] = labelWidth;
      }

      tableModel.set(data);
      tableModel.fireTableDataChanged();

      annotationFeaturesViewer.populate(ae, analysisEngineMetaData, cas);
      annotationFeaturesViewer.addTreeSelectionListener(new AnTreeListener(med));
      med.setEntryButton(addTableEntryButton);

      // Establish sensible column widths based upon data:
      TableColumn column = null;
      for (int i = 0; i < StyleConstants.NR_TABLE_COLUMNS; i++) {
        column = annotationsTable.getColumnModel().getColumn(i);
        column.setPreferredWidth(maxColumnWidths[i] + 4);
      }

      annotationsTable.getTableHeader().repaint();
      populated = true; // only load table once
    }
    styleMapEditorDialog.pack();
    styleMapEditorDialog.setVisible(true);
    return buttonPress;
  }

  private void setUpColorEditor(JTable table) {
    final JTable tbl = table;
    // First, set up the button that brings up the dialog.
    final JButton button = new JButton("") {
      private static final long serialVersionUID = 3955120051470642157L;

      public void setText(String s) {
        // Button never shows text -- only color.
      }
    };
    button.setBackground(Color.white);
    button.setBorderPainted(false);
    button.setMargin(new Insets(0, 0, 0, 0));

    // Now create an editor to encapsulate the button, and
    // set it up as the editor for all Color cells.
    final ColorEditor colorEditor = new ColorEditor(button);
    table.setDefaultEditor(Color.class, colorEditor);

    // Set up the dialog that the button brings up.
    final JColorChooser colorChooser = new JColorChooser();

    // AbstractColorChooserPanel panels[] = colorChooser.getChooserPanels();

    // The following has the effect of removing the RGB panel and making the HSB panel the default:

    // AbstractColorChooserPanel preferredPanels[] = new AbstractColorChooserPanel[2];
    // preferredPanels[0] = panels[1];
    // preferredPanels[1] = panels[0];
    // colorChooser.setChooserPanels(preferredPanels);

    ActionListener okListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        colorEditor.currentColor = colorChooser.getColor();
        Color newColor = colorEditor.currentColor;
        StyleMapEntry entry = (StyleMapEntry) styleList.get(tbl.getSelectedRow());
        int column = tbl.getSelectedColumn();
        if (column == StyleConstants.BG_COLUMN)
          entry.setBackground(newColor);
        else
          entry.setForeground(newColor);

        tbl.repaint();
      }
    };
    final JDialog dialog = JColorChooser.createDialog(button, "Pick a Color", true, colorChooser,
            okListener, null);

    // Here's the code that brings up the dialog.
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        button.setBackground(colorEditor.currentColor);
        colorChooser.setColor(colorEditor.currentColor);
        // Without the following line, the dialog comes up
        // in the middle of the screen.
        dialog.setLocationRelativeTo(button);
        dialog.show();
      }
    });
  }

  /**
   * Generates a style map XML document from the style list.
   * 
   * @param aStyleList
   *          An ArrayList containing the style map entries to be written to XML.
   * 
   * @return A style map XML document representing the information in <code>aStyleList</code>.
   */
  private String generateStyleMap(ArrayList aStyleList) {
    String newStyleMap = null;

    try {
      StringBuffer newStyle = new StringBuffer();

      try {
        newStyle.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        newStyle.append("<styleMap>\n");

        Iterator smt = styleList.iterator();

        for (int row = 0; smt.hasNext(); row++) {
          StyleMapEntry e = (StyleMapEntry) smt.next();
          newStyle.append("<rule>\n");
          newStyle.append("<pattern>");
          newStyle.append(e.getPattern());
          newStyle.append("</pattern>\n");
          newStyle.append("<label>");
          String label = e.getLabel();
          if (label != null) {
            if ((label != null) && !label.equals(""))
              newStyle.append(label);
            else
              newStyle.append(e.getAnnotationTypeName());
          } else
            newStyle.append(e.getAnnotationTypeName());

          newStyle.append("</label>\n");
          newStyle.append("<style>");

          String foregroundColor = "#"
                  + Integer.toHexString(e.getForeground().getRGB()).substring(2);
          String backgroundColor = "#"
                  + Integer.toHexString(e.getBackground().getRGB()).substring(2);

          if (colorNameMap.containsKey(foregroundColor)) {
            newStyle.append("color:" + colorNameMap.get(foregroundColor) + ";");
          } else {
            newStyle.append("color:" + foregroundColor + ";");
          }

          if (colorNameMap.containsKey(backgroundColor)) {
            newStyle.append("background:" + colorNameMap.get(backgroundColor) + ";");
          } else {
            newStyle.append("background:" + backgroundColor + ";");
          }
          // add in checked and hidden
          Boolean ck = (Boolean) tableModel.getValueAt(row, StyleConstants.CHECK_COLUMN);
          String ckString = ck.toString();

          Boolean hid = (Boolean) tableModel.getValueAt(row, StyleConstants.HIDDEN_COLUMN);
          String hidString = hid.toString();
          // this prevents hidden from being checked,
          // becasue that is not a meaningful combination
          if (hidString.equals("true")) {
            ckString = "false";
          }
          newStyle.append("checked:" + ckString + ";");
          newStyle.append("hidden:" + hidString + ";");

          newStyle.append("</style>\n");
          newStyle.append("</rule>\n");
        }

        newStyle.append("</styleMap>\n");
      } catch (Exception e) {
        System.out.println(e);
      }

      newStyleMap = newStyle.toString();
    } catch (Throwable t) {

    }

    return newStyleMap;
  }

  public void addRow(String typeName) {

    // Check for duplicate annotation types that are not feature values:

    for (int i = 0; i < styleList.size(); i++) {
      StyleMapEntry e = (StyleMapEntry) styleList.get(i);
      if (typeName.equals(e.getAnnotationTypeName()) && typeName.indexOf(":") == -1) {
        JOptionPane.showMessageDialog(StyleMapEditor.this, "Duplicate Annotation Type", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
      }
    }

    StyleMapEntry styleMapEntry = new StyleMapEntry();

    styleMapEntry.setAnnotationTypeName(typeName);
    styleMapEntry.setLabel(typeName);
    styleMapEntry.setFeatureValue("");
    styleMapEntry.setForeground(Color.black);
    styleMapEntry.setBackground(Color.white);

    styleList.add(styleMapEntry);

    // Create a Vector of values to be held in table data model:
    Vector rowVector = new Vector();
    rowVector.addElement("");
    rowVector.addElement(typeName);
    rowVector.addElement(typeName);
    rowVector.addElement(Color.white);
    rowVector.addElement(Color.black);
    rowVector.addElement(Boolean.TRUE);
    rowVector.addElement(Boolean.FALSE);

    tableModel.addRow(rowVector);
  }

}
