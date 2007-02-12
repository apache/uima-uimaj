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

package org.apache.uima.tools.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.tools.images.Images;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * A GUI that displays annotation results in a Swing tree viewer. This class extends {@link JPanel}
 * and so can be reused within any Swing application.
 * 
 */
public class CasTreeViewer extends JPanel {

  private static final long serialVersionUID = -674412767134245565L;

  /**
   * Creates a CAS Tree Viewer.
   * 
   * @param aCAS
   *          the CAS containing the annotations to be displayed in the tree viewer GUI
   */
  public CasTreeViewer(CAS aCAS) throws CASException {
    super();

    // build tree from annotations in CAS
    TreeNode root = buildTree(aCAS);

    // create a JSplitPane
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setResizeWeight(0.66);
    this.add(splitPane);

    // build JTree and add to left side of JSplitPane
    tree = new JTree(root);
    splitPane.setLeftComponent(new JScrollPane(tree));

    // add a JPanel with a JLabel (annotation type name), JTextArea (covered text),
    // and JTable (features and their values) to the right side of JSplitPane
    rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    annotationTypeLabel = new JLabel("Annotation Type: ");
    rightPanel.add(annotationTypeLabel);
    String[] columnNames = { "Feature", "Value" };
    featureTable = new JTable(new DefaultTableModel(columnNames, 1));

    rightPanel.add(new JScrollPane(featureTable));
    annotationTextPane = new JTextPane();
    rightPanel.add(new JScrollPane(annotationTextPane));
    splitPane.setRightComponent(rightPanel);

    // add an event handler to catch tree selection changes and update the table
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent aEvent) {
        TreePath selPath = tree.getSelectionPath();
        if (selPath != null) {
          DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath
                  .getLastPathComponent();
          Object userObj = selectedNode.getUserObject();
          if (userObj instanceof AnnotationTreeNodeObject) {
            AnnotationFS annotation = ((AnnotationTreeNodeObject) userObj).getAnnotation();
            refreshAnnotationData(annotation);
          }
        }
      }

    });
  }

  /**
   * Called when the user selects a new node in the JTree. Refreshes the right pane to display
   * information about the selected annotation.
   * 
   * @param aAnnotation
   *          the annotation that was selected in the JTree
   */
  private void refreshAnnotationData(AnnotationFS aAnnotation) {
    // Set Annotation Type Label
    String typeName = aAnnotation.getType().getName();
    annotationTypeLabel.setText("Annotation Type: " + typeName);

    // Add annotation's covered text to the text area
    annotationTextPane.setText(aAnnotation.getCoveredText());
    annotationTextPane.setSelectionStart(0);
    annotationTextPane.setSelectionEnd(0);

    // add annotation's features and their values to the JTable
    DefaultTableModel tableModel = (DefaultTableModel) featureTable.getModel();
    // remove old info
    while (tableModel.getRowCount() > 0) {
      tableModel.removeRow(0);
    }

    // add new feature info
    List aFeatures = aAnnotation.getType().getFeatures();
    Iterator iter = aFeatures.iterator();
    while (iter.hasNext()) {
      Feature feat = (Feature) iter.next();
      String featName = feat.getName();
      // how we get feature value depends on feature's range type)
      String rangeTypeName = feat.getRange().getName();
      if (CAS.TYPE_NAME_STRING.equals(rangeTypeName)) {
        String strVal = aAnnotation.getStringValue(feat);
        if (strVal != null && strVal.length() > 64) {
          strVal = strVal.substring(0, 64) + "...";
        }
        tableModel.addRow(new Object[] { featName, strVal });
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        int intVal = aAnnotation.getIntValue(feat);
        tableModel.addRow(new Object[] { featName, new Integer(intVal) });
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        float floatVal = aAnnotation.getFloatValue(feat);
        tableModel.addRow(new Object[] { featName, new Float(floatVal) });
      } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
        StringArrayFS arrayFS = (StringArrayFS) aAnnotation.getFeatureValue(feat);
        StringBuffer displayVal = new StringBuffer();
        if (arrayFS == null) {
          displayVal.append("null");
        } else {
          displayVal.append('[');
          String[] vals = arrayFS.toArray();
          for (int i = 0; i < vals.length - 1; i++) {
            displayVal.append(vals[i]);
            displayVal.append(',');
          }
          if (vals.length > 0) {
            displayVal.append(vals[vals.length - 1]);
          }
          displayVal.append(']');
        }
        tableModel.addRow(new Object[] { featName, displayVal });
      } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
        IntArrayFS arrayFS = (IntArrayFS) aAnnotation.getFeatureValue(feat);
        StringBuffer displayVal = new StringBuffer();
        if (arrayFS == null) {
          displayVal.append("null");
        } else {
          displayVal.append('[');
          int[] vals = arrayFS.toArray();
          for (int i = 0; i < vals.length - 1; i++) {
            displayVal.append(vals[i]);
            displayVal.append(',');
          }
          if (vals.length > 0) {
            displayVal.append(vals[vals.length - 1]);
          }
          displayVal.append(']');
        }
        tableModel.addRow(new Object[] { featName, displayVal });
      } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
        FloatArrayFS arrayFS = (FloatArrayFS) aAnnotation.getFeatureValue(feat);
        StringBuffer displayVal = new StringBuffer();
        if (arrayFS == null) {
          displayVal.append("null");
        } else {
          displayVal.append('[');
          float[] vals = arrayFS.toArray();
          for (int i = 0; i < vals.length - 1; i++) {
            displayVal.append(vals[i]);
            displayVal.append(',');
          }
          if (vals.length > 0) {
            displayVal.append(vals[vals.length - 1]);
          }
          displayVal.append(']');
        }
        tableModel.addRow(new Object[] { featName, displayVal });
      }
    }
  }

  /**
   * Builds a tree from a CAS.
   * 
   * @param aRootNode
   *          an existing root node for the tree
   * @param aCAS
   *          CAS from which annotations will be extracted
   */
  private TreeNode buildTree(CAS aCAS) throws CASException {
    // get iterator over all annotations
    FSIterator iterator = aCAS.getAnnotationIndex().iterator();

    // create artifical root node encompassing entire document
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Document");
    // add children to this node
    _buildTree(root, iterator, 0, aCAS.getDocumentText().length());

    return root;
  }

  /**
   * Recursive method called by {@link buildTree(DefaultMutableTreeNode,FSIterator)}.
   * 
   * @param aParentNode
   *          root node of tree to be built
   * @param aIterator
   *          iterator over all annotation in CAS
   * @param aStartPos
   *          text position at which to begin processing
   * @param aEndPos
   *          text position at which to end processing
   */
  private void _buildTree(DefaultMutableTreeNode aParentNode, FSIterator aIterator, int aStartPos,
          int aEndPos) {
    while (aIterator.isValid()) {
      AnnotationFS curAnnot = (AnnotationFS) aIterator.get();
      int curAnnotStart = curAnnot.getBegin();
      int curAnnotEnd = curAnnot.getEnd();
      if (curAnnotEnd <= aEndPos) {
        // move iterator to next annotation
        aIterator.moveToNext();

        if (curAnnotStart < curAnnotEnd) // account for bug in JTalent
        {
          // add this annotation as a child of aParentNode
          DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new AnnotationTreeNodeObject(
                  curAnnot));
          aParentNode.add(newNode);
          // recursively add children to this node
          _buildTree(newNode, aIterator, curAnnotStart, curAnnotEnd);
        }
      } else
        break;
    }
  }

  /**
   * Main program. Runs a TAE and displays the resulting annotations in the tree viewer.
   * 
   * @param args
   *          Command-line arguments - two are reguired: the path to the TAE descriptor and a file
   *          to be analyzed.
   */
  public static void main(String[] args) {
    AnalysisEngine ae = null;
    try {
      File taeDescriptor = null;
      File inputFile = null;

      // Read and validate command line arguments
      boolean validArgs = false;
      if (args.length == 2) {
        taeDescriptor = new File(args[0]);
        inputFile = new File(args[1]);
        validArgs = taeDescriptor.exists() && !taeDescriptor.isDirectory() && inputFile.exists()
                && !inputFile.isDirectory();
      }
      if (!validArgs) {
        printUsageMessage();
      } else {
        // get Resource Specifier from XML file or TEAR
        XMLInputSource in = new XMLInputSource(taeDescriptor);
        ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

        // create Text Analysis Engine and CAS here
        ae = UIMAFramework.produceAnalysisEngine(specifier);
        CAS CAS = ae.newCAS();

        // read document from file
        
        String document = FileUtils.file2String(inputFile);
        
        CAS.setDocumentText(getText(document).trim());

        // analyze
        ae.process(CAS);

        // set GUI look and feel
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
          // I don't think this should ever happen, but if it does just print error and continue
          // with defalt look and feel
          System.err.println("Could not set look and feel: " + e.getMessage());
        }
        // create Frame for standlone app
        JFrame frame = new JFrame();
        frame.setTitle("Annotation Tree Viewer");

        // Set frame icon image
        try {
          frame.setIconImage(Images.getImage(Images.MICROSCOPE));
        } catch (IOException e) {
          System.err.println("Image could not be loaded: " + e.getMessage());
        }
        frame.getContentPane().setBackground(Color.WHITE);

        frame.getContentPane().setLayout(new BorderLayout());
        // add banner
        JLabel banner = new JLabel(Images.getImageIcon(Images.BANNER));
        frame.getContentPane().add(banner, BorderLayout.NORTH);

        // create tree viewer component and add to Frame
        CasTreeViewer treeViewer = new CasTreeViewer(CAS);
        frame.getContentPane().add(treeViewer, BorderLayout.CENTER);

        // show Frame
        frame.setSize(800, 600);
        frame.pack();
        frame.show();
        frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            System.exit(0);
          }
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  /**
   * Gets text to be processed by the TAE. If the document contains XML tags named TEXT like this:
   * <code>&lt;TEXT%gt;Process this text.&lt;/TEXT%gt;</code>, then only the text within those
   * tags is returned. Otherwise the whole document is returned.
   * 
   * @param aFile
   *          file to process
   * @param aTAE
   *          Text Analysis Engine that will process the file
   */
  static private String getText(String text) {
    int start = text.indexOf("<TEXT>");
    int end = text.indexOf("</TEXT>");
    if (start != -1 && end != -1) {
      return text.substring(start + 6, end);
    } else {
      return text;
    }
  }

  /**
   * Prints usage message.
   */
  private static void printUsageMessage() {
    System.err.println("Usage: UimaFrameworkTreeViewer "
            + "<TAE descriptor or TEAR file name> <input file>");
  }

  /**
   * @see java.awt.Component#setSize(Dimension)
   */
  public void setSize(Dimension d) {
    super.setSize(d);
    Insets insets = getInsets();
    Dimension paneSize = new Dimension(d.width - insets.left - insets.right, d.height - insets.top
            - insets.bottom);

    splitPane.setPreferredSize(paneSize);
    splitPane.setSize(paneSize);
  }

  // GUI components
  private JSplitPane splitPane;

  private JTree tree;

  private JPanel rightPanel;

  private JLabel annotationTypeLabel;

  private JTextPane annotationTextPane;

  private JTable featureTable;

  /**
   * Inner class containing data for a node in the tree.
   */
  static class AnnotationTreeNodeObject {
    public AnnotationTreeNodeObject(AnnotationFS aAnnotation) {
      mAnnotation = aAnnotation;
      mCaption = aAnnotation.getCoveredText();
      if (mCaption.length() > 64)
        mCaption = mCaption.substring(0, 64) + "...";

    }

    public AnnotationFS getAnnotation() {
      return mAnnotation;
    }

    public String toString() {
      return mCaption;
    }

    private AnnotationFS mAnnotation;

    private String mCaption;
  }

}
