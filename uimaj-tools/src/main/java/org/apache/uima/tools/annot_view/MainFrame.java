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

package org.apache.uima.tools.annot_view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonCas;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TCASImpl;
import org.apache.uima.cas.impl.TypeSystem2Xml;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.internal.util.FileUtils;
import org.apache.uima.internal.util.TimeSpan;
import org.apache.uima.internal.util.Timer;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.images.Images;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/**
 * Class comment for MainFrame.java goes here.
 * 
 * 
 */
public class MainFrame extends JFrame {

  private static final long serialVersionUID = -1357410768440678886L;

  private class FocusIRAction extends AbstractAction implements Action {

    private static final long serialVersionUID = -8128067676842119411L;

    public void actionPerformed(ActionEvent arg0) {
      // Only available in 1.4.
      // MainFrame.this.indexTree.requestFocusInWindow();
      MainFrame.this.indexTree.requestFocus();
    }

  }

  private class FocusFSAction extends AbstractAction implements Action {

    private static final long serialVersionUID = -8330075846211434833L;

    public void actionPerformed(ActionEvent arg0) {
      // Only available in 1.4.
      // MainFrame.this.fsTree.requestFocusInWindow();
      MainFrame.this.fsTree.requestFocus();
    }

  }

  private class FocusTextAction extends AbstractAction implements Action {

    private static final long serialVersionUID = -4867535661038776033L;

    public void actionPerformed(ActionEvent arg0) {
      // Only available in 1.4.
      // MainFrame.this.textArea.requestFocusInWindow();
      MainFrame.this.textArea.requestFocus();
    }

  }

  private class TextChangedListener implements DocumentListener {

    public void changedUpdate(DocumentEvent arg0) {
      // Do nothing.
    }

    public void insertUpdate(DocumentEvent arg0) {
      removeUpdate(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
     */
    public void removeUpdate(DocumentEvent arg0) {
      if (!MainFrame.this.isDirty) {
        MainFrame.this.isDirty = true;
        setTitle();
        if (MainFrame.this.cas != null) {
          setStatusbarMessage("Text changed, CAS removed.");
        }
        resetTrees();
      }
    }

  }

  private class TextFocusHandler implements FocusListener {

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
      MainFrame.this.textArea.getCaret().setVisible(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
      // Does nothing
    }

  }

  private static class TreeFocusHandler implements FocusListener {

    private JTree tree;

    private TreeFocusHandler(JTree tree) {
      super();
      this.tree = tree;
    }

    public void focusGained(FocusEvent arg0) {
      TreePath selPath = this.tree.getSelectionPath();
      if (selPath == null) {
        selPath = new TreePath(new Object[] { this.tree.getModel().getRoot() });
        this.tree.setSelectionPath(selPath);
      }
    }

    public void focusLost(FocusEvent arg0) {
      // Do nothing.
    }

  }

  private class PopupHandler implements ActionListener {

    private final int node;

    private PopupHandler(int n) {
      super();
      this.node = n;
    }

    public void actionPerformed(ActionEvent e) {
      FSTreeModel treeModel = (FSTreeModel) MainFrame.this.fsTree.getModel();
      TreePath path = treeModel.pathToNode(this.node);
      MainFrame.this.fsTree.setSelectionPath(path);
      MainFrame.this.fsTree.scrollPathToVisible(path);
    }

  }

  private class CaretChangeHandler implements CaretListener {

    public void caretUpdate(CaretEvent ce) {
      final int dot = ce.getDot();
      final int mark = ce.getMark();
      setCaretStatus(dot, mark);
      if (dot == mark) {
        MainFrame.this.cutAction.setEnabled(false);
        MainFrame.this.copyAction.setEnabled(false);
      } else {
        MainFrame.this.cutAction.setEnabled(true);
        MainFrame.this.copyAction.setEnabled(true);
      }
    }

  }

  private class PopupListener extends MouseAdapter {

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        showTextPopup(e.getX(), e.getY());
      }
    }
  }

  private class TextContextMenuAction extends AbstractAction {

    private static final long serialVersionUID = -5518456467913617514L;

    public void actionPerformed(ActionEvent arg0) {
      Point caretPos = MainFrame.this.textArea.getCaret().getMagicCaretPosition();
      if (caretPos == null) {
        // No idea why this is needed. Bug in JTextArea, or my poor
        // understanding of the magics of carets. The point is null when
        // the
        // text area is first focused.
        showTextPopup(0, 0);
      } else {
        showTextPopup(caretPos.x, caretPos.y);
      }
    }

  }

  private class IndexPopupListener extends MouseAdapter {

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) MainFrame.this.indexTree
                .getLastSelectedPathComponent();
        if (node == null) {
          return;
        }
        Object userObject = node.getUserObject();
        String annotTitle = null;
        boolean isAnnotation = true;
        if (userObject instanceof IndexTreeNode) {
          IndexTreeNode iNode = (IndexTreeNode) userObject;
          if (!iNode.getName().equals(TCAS.STD_ANNOTATION_INDEX)) {
            isAnnotation = false;
          }
          annotTitle = iNode.getType().getName();
        } else if (userObject instanceof TypeTreeNode) {
          TypeTreeNode tNode = (TypeTreeNode) userObject;
          if (!tNode.getLabel().equals(TCAS.STD_ANNOTATION_INDEX)) {
            isAnnotation = false;
          }
          annotTitle = tNode.getType().getName();
        } else {
          isAnnotation = false;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = null;
        if (isAnnotation) {
          item = new JMenuItem("Show annotations: " + annotTitle);
          item.addActionListener(new ShowAnnotatedTextHandler());
        } else {
          item = new JMenuItem("No annotations selected");
        }
        menu.add(item);
        menu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  // private class IndexPopupHandler implements ActionListener {
  //
  // public void actionPerformed(ActionEvent event) {
  // DefaultMutableTreeNode node =
  // (DefaultMutableTreeNode) indexTree.getLastSelectedPathComponent();
  // if (node == null) {
  // return;
  // }
  // Object userObject = node.getUserObject();
  // String label = null;
  // Type type = null;
  // if (userObject instanceof IndexTreeNode) {
  // IndexTreeNode indexNode = (IndexTreeNode) userObject;
  // label = indexNode.getName();
  // type = indexNode.getType();
  // } else if (userObject instanceof TypeTreeNode) {
  // TypeTreeNode typeNode = (TypeTreeNode) userObject;
  // label = typeNode.getLabel();
  // type = typeNode.getType();
  // } else {
  // return;
  // }
  // indexLabel = label;
  // isAnnotIndex = label.equals(TCAS.STD_ANNOTATION_INDEX);
  // index = cas.getIndexRepository().getIndex(label, type);
  // String title = indexLabel + " [" + index.getType().getName() + "]";
  // MultiAnnotViewerFrame f = new MultiAnnotViewerFrame(title);
  // f.addWindowListener(new CloseAnnotationViewHandler());
  // FSIterator it = index.iterator();
  // final String text = cas.getDocumentText();
  // System.out.println("Creating extents.");
  // AnnotationExtent[] extents =
  // MultiMarkup.createAnnotationMarkups(it, text.length(), styleMap);
  // System.out.println("Initializing text frame.");
  // f.init(text, extents, getDimension(MainFrame.annotViewSizePref));
  // System.out.println("Done.");
  // }
  //
  // }

  private class MainFrameClosing extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      try {
        setStatusbarMessage("Saving preferences.");
        saveProgramPreferences();
        if (MainFrame.this.ae != null) {
          MainFrame.this.ae.destroy();
        }
      } catch (IOException ioe) {
        handleException(ioe);
      }
      System.exit(0);
    }

  }

  /**
   * Change the display of the FSTree if a type in an index is selected.
   */
  private class IndexTreeSelectionListener implements TreeSelectionListener {

    /**
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    public void valueChanged(TreeSelectionEvent arg0) {
      // System.out.println("Tree selection value changed");
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) MainFrame.this.indexTree
              .getLastSelectedPathComponent();
      if (node == null) {
        return;
      }
      Object userObject = node.getUserObject();
      String label = null;
      Type type = null;
      if (userObject instanceof IndexTreeNode) {
        IndexTreeNode indexNode = (IndexTreeNode) userObject;
        label = indexNode.getName();
        type = indexNode.getType();
      } else if (userObject instanceof TypeTreeNode) {
        TypeTreeNode typeNode = (TypeTreeNode) userObject;
        label = typeNode.getLabel();
        type = typeNode.getType();
      } else {
        return;
      }
      MainFrame.this.indexLabel = label;
      MainFrame.this.isAnnotIndex = label.equals(TCAS.STD_ANNOTATION_INDEX);
      MainFrame.this.index = MainFrame.this.cas.getIndexRepository().getIndex(label, type);
      updateFSTree(label, MainFrame.this.index);
      MainFrame.this.allAnnotViewerItem.setEnabled(((TCASImpl) MainFrame.this.cas)
              .isAnnotationType(type));
      MainFrame.this.textArea.getCaret().setVisible(true);
    }

  }

  private class FSTreeSelectionListener implements TreeSelectionListener {

    /**
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    public void valueChanged(TreeSelectionEvent event) {
      // System.out.println("");
      FSTreeNode protoNode = (FSTreeNode) MainFrame.this.fsTree.getLastSelectedPathComponent();
      if (!(protoNode instanceof FSNode)) {
        return;
      }
      FSNode node = (FSNode) protoNode;
      if (node == null) {
        return;
      }
      // Remeber start of current selection.
      final int currentSelStart = MainFrame.this.textArea.getSelectionStart();
      if (node.isAnnotation()) {
        if (null != MainFrame.this.cas.getDocumentText()) {
          MainFrame.this.textArea.setSelectionStart(node.getStart());
          MainFrame.this.textArea.setSelectionEnd(node.getEnd());
          // System.out.println(
          // "Setting selection from " + node.getStart() + " to " +
          // node.getEnd());
          MainFrame.this.textArea.getCaret().setSelectionVisible(true);
        }
      } else {
        MainFrame.this.textArea.setSelectionEnd(currentSelStart);
      }

    }

  }

  private class FileOpenEventHandler implements ActionListener {

    private FileOpenEventHandler() {
      super();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Open text file");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      if (MainFrame.this.fileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.fileOpenDir);
      }
      int rc = fileChooser.showOpenDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        MainFrame.this.textFile = fileChooser.getSelectedFile();
        if (MainFrame.this.textFile.exists() && MainFrame.this.textFile.isFile()) {
          MainFrame.this.fileOpenDir = MainFrame.this.textFile.getParentFile();
        }
        Timer time = new Timer();
        time.start();
        loadFile();
        time.stop();
        resetTrees();
        MainFrame.this.fileSaveItem.setEnabled(true);
        MainFrame.this.undoMgr.discardAllEdits();
        setFileStatusMessage();
        setStatusbarMessage("Done loading text file " + MainFrame.this.textFile.getName() + " in "
                + time.getTimeSpan() + ".");
      }
    }
  }

  private class NewTextEventHandler implements ActionListener {

    private NewTextEventHandler() {
      super();
    }

    public void actionPerformed(ActionEvent event) {
      MainFrame.this.textFile = null;
      MainFrame.this.textArea.setText("");
      if (MainFrame.this.isDirty) {
        MainFrame.this.isDirty = false;
      }
      setTitle();
      resetTrees();
      MainFrame.this.fileSaveItem.setEnabled(false);
      MainFrame.this.undoMgr.discardAllEdits();
      setFileStatusMessage();
      setStatusbarMessage("Text area cleared.");
    }

  }

  private class LoadRecentTextFileEventHandler implements ActionListener {

    private final String fileName;

    private LoadRecentTextFileEventHandler(String fileName) {
      super();
      this.fileName = fileName;
    }

    public void actionPerformed(ActionEvent e) {
      loadTextFile(new File(this.fileName));
    }

  }

  private class LoadRecentDescFileEventHandler implements ActionListener {

    private final String fileName;

    private LoadRecentDescFileEventHandler(String fileName) {
      super();
      this.fileName = fileName;
    }

    public void actionPerformed(ActionEvent e) {
      loadAEDescriptor(new File(this.fileName));
    }

  }

  private class FileSaveAsEventHandler implements ActionListener {

    private FileSaveAsEventHandler() {
      super();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save file as...");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
      if (MainFrame.this.fileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.fileOpenDir);
      }
      int rc = fileChooser.showSaveDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File tmp = MainFrame.this.textFile;
        MainFrame.this.textFile = fileChooser.getSelectedFile();
        boolean fileSaved = saveFile();
        if (fileSaved) {
          MainFrame.this.isDirty = false;
          setTitle();
          MainFrame.this.fileSaveItem.setEnabled(true);
          setFileStatusMessage();
          setStatusbarMessage("Text file " + MainFrame.this.textFile.getName() + " saved.");
        } else {
          MainFrame.this.textFile = tmp;
        }
      }
    }

  }

  private class FileSaveEventHandler implements ActionListener {

    private FileSaveEventHandler() {
      super();
    }

    public void actionPerformed(ActionEvent event) {
      saveFile();
      setStatusbarMessage("Text file " + MainFrame.this.textFile.getName() + " saved.");
    }

  }

  private class XCASFileOpenEventHandler implements ActionListener {

    private XCASFileOpenEventHandler() {
      super();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Open XCAS file");
      if (MainFrame.this.xcasFileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.xcasFileOpenDir);
      }
      int rc = fileChooser.showOpenDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File xcasFile = fileChooser.getSelectedFile();
        if (xcasFile.exists() && xcasFile.isFile()) {
          try {
            MainFrame.this.xcasFileOpenDir = xcasFile.getParentFile();
            Timer time = new Timer();
            time.start();
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            XCASDeserializer xcasDeserializer = new XCASDeserializer(MainFrame.this.cas
                    .getTypeSystem());
            MainFrame.this.cas.reset();
            parser.parse(xcasFile, xcasDeserializer.getXCASHandler(MainFrame.this.cas));
            time.stop();
            // Populate sofa combo box with the names of all text
            // Sofas in the CAS
            MainFrame.this.disableSofaListener = true;
            String currentView = (String) MainFrame.this.sofaSelectionComboBox.getSelectedItem();
            MainFrame.this.sofaSelectionComboBox.removeAllItems();
            MainFrame.this.sofaSelectionComboBox.addItem(CommonCas.NAME_DEFAULT_SOFA);
            Iterator sofas = ((CASImpl) MainFrame.this.cas).getBaseCAS().getSofaIterator();
            Feature sofaIdFeat = MainFrame.this.cas.getTypeSystem().getFeatureByFullName(
        	CommonCas.FEATURE_FULL_NAME_SOFAID);
            boolean nonDefaultSofaFound = false;
            while (sofas.hasNext()) {
              SofaFS sofa = (SofaFS) sofas.next();
              String sofaId = sofa.getStringValue(sofaIdFeat);
              if (!CommonCas.NAME_DEFAULT_SOFA.equals(sofaId)) {
                MainFrame.this.sofaSelectionComboBox.addItem(sofaId);
                nonDefaultSofaFound = true;
              }
            }
            // reuse last selected view if found in new CAS
            int newIndex = 0;
            String newView = CommonCas.NAME_DEFAULT_SOFA;
            for (int i = 0; i < MainFrame.this.sofaSelectionComboBox.getItemCount(); i++) {
              if (currentView.equals(MainFrame.this.sofaSelectionComboBox.getItemAt(i))) {
                newIndex = i;
                newView = currentView;
                break;
              }
            }
            // make sofa selector visible if any text sofa other
            // than the default was found
            MainFrame.this.sofaSelectionPanel.setVisible(nonDefaultSofaFound);
            MainFrame.this.cas = MainFrame.this.cas.getView(newView);
            MainFrame.this.disableSofaListener = false;

            MainFrame.this.sofaSelectionComboBox.setSelectedIndex(newIndex);
            String text = MainFrame.this.cas.getDocumentText();
            if (text == null) {
              text = MainFrame.this.cas.getSofaDataURI();
              if (text != null) {
                text = "SofaURI = " + text;
              } else {
                if (null != MainFrame.this.cas.getSofaDataArray()) {
                  text = "Sofa array with mime type = "
                          + MainFrame.this.cas.getSofa().getSofaMime();
                }
              }
            }
            MainFrame.this.textArea.setText(text);
            if (text == null) {
              MainFrame.this.textArea.repaint();
            }

            MainFrame.this.setTitle("XCAS");
            MainFrame.this.updateIndexTree(true);
            MainFrame.this.runOnCasMenuItem.setEnabled(true);
            setStatusbarMessage("Done loading XCAS file in " + time.getTimeSpan() + ".");
          } catch (Exception e) {
            e.printStackTrace();
            handleException(e);
          }
        }
      }
    }

  }

  private class TypeSystemFileOpenEventHandler implements ActionListener {

    private TypeSystemFileOpenEventHandler() {
      super();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Open Type System File");
      if (MainFrame.this.xcasFileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.xcasFileOpenDir);
      }
      int rc = fileChooser.showOpenDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File tsFile = fileChooser.getSelectedFile();
        if (tsFile.exists() && tsFile.isFile()) {
          try {
            MainFrame.this.xcasFileOpenDir = tsFile.getParentFile();
            Timer time = new Timer();
            time.start();
            Object descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(tsFile));
            // instantiate TCAS to get type system. Also build style
            // map file if there is none.
            TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
            tsDesc.resolveImports();
            if (MainFrame.this.ae != null) {
              MainFrame.this.ae.destroy();
              MainFrame.this.ae = null;
            }
            MainFrame.this.cas = CasCreationUtils
                    .createCas(tsDesc, null, new FsIndexDescription[0]);
            MainFrame.this.runOnCasMenuItem.setEnabled(false);
            MainFrame.this.reRunMenu.setEnabled(false);
            MainFrame.this.textArea.setText("");
            MainFrame.this.resetTrees();
            MainFrame.this.tsViewerItem.setEnabled(true);
            MainFrame.this.xcasReadItem.setEnabled(true);
            time.stop();
            setStatusbarMessage("Done loading type system file in " + time.getTimeSpan() + ".");
          } catch (Exception e) {
            e.printStackTrace();
            handleException(e);
          }
        }
      }
    }

  }

  private class ColorPrefsOpenHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Load color preferences file");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      if (MainFrame.this.colorSettingsDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.colorSettingsDir);
      }
      int rc = fileChooser.showOpenDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if (file.exists() && file.isFile()) {
          MainFrame.this.colorSettingsDir = file.getParentFile();
          MainFrame.this.colorSettingFile = file;
          try {
            loadColorPreferences(MainFrame.this.colorSettingFile);
          } catch (IOException e) {
            handleException(e);
            // e.printStackTrace();
            // JOptionPane.showMessageDialog(
            // MainFrame.this,
            // e.getMessage(),
            // "I/O Error",
            // JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    }

  }

  private class ColorPrefsSaveHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save color preferences");
      if (MainFrame.this.colorSettingsDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.colorSettingsDir);
      }
      int rc = fileChooser.showSaveDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File prefFile = fileChooser.getSelectedFile();
        MainFrame.this.colorSettingsDir = prefFile.getParentFile();
        try {
          saveColorPreferences(prefFile);
        } catch (IOException e) {
          handleException(e);
        }
      }
    }

  }

  private class XCASSaveHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save XCAS file");
      if (MainFrame.this.xcasFileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.xcasFileOpenDir);
      }
      int rc = fileChooser.showSaveDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File xcasFile = fileChooser.getSelectedFile();
        MainFrame.this.xcasFileOpenDir = xcasFile.getParentFile();
        try {
          long time = System.currentTimeMillis();
          OutputStream outStream = new BufferedOutputStream(new FileOutputStream(xcasFile));
          XMLSerializer xmlSerializer = new XMLSerializer(outStream);
          XCASSerializer xcasSerializer = new XCASSerializer(MainFrame.this.cas.getTypeSystem());
          xcasSerializer.serialize(MainFrame.this.cas, xmlSerializer.getContentHandler());
          outStream.close();
          time = System.currentTimeMillis() - time;
          System.out.println("Time taken: " + new TimeSpan(time));
        } catch (IOException e) {
          handleException(e);
        } catch (SAXException e) {
          handleException(e);
        }
      }
    }

  }

  private class XCASSaveTSHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save type system file");
      if (MainFrame.this.xcasFileOpenDir != null) {
        fileChooser.setCurrentDirectory(MainFrame.this.xcasFileOpenDir);
      }
      int rc = fileChooser.showSaveDialog(MainFrame.this);
      if (rc == JFileChooser.APPROVE_OPTION) {
        File tsFile = fileChooser.getSelectedFile();
        MainFrame.this.xcasFileOpenDir = tsFile.getParentFile();
        try {
          OutputStream outStream = new BufferedOutputStream(new FileOutputStream(tsFile));
          TypeSystem2Xml.typeSystem2Xml(MainFrame.this.cas.getTypeSystem(), outStream);
          outStream.close();
        } catch (IOException e) {
          handleException(e);
        } catch (SAXException e) {
          handleException(e);
        }
      }
    }

  }

  private class SystemExitHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      try {
        saveProgramPreferences();
      } catch (IOException e) {
        handleException(e);
      }
      System.exit(0);
    }

  }

  private class AnnotatorOpenEventHandler implements ActionListener {

    private MainFrame frame;

    private AnnotatorOpenEventHandler(MainFrame frame) {
      super();
      this.frame = frame;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      try {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load AE specifier file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (MainFrame.this.annotOpenDir != null) {
          fileChooser.setCurrentDirectory(MainFrame.this.annotOpenDir);
        }
        int rc = fileChooser.showOpenDialog(this.frame);
        if (rc == JFileChooser.APPROVE_OPTION) {
          MainFrame.this.aeDescriptorFile = fileChooser.getSelectedFile();
          loadAEDescriptor(MainFrame.this.aeDescriptorFile);
        }
        MainFrame.this.allAnnotViewerItem.setEnabled(false);
      } finally {
        resetCursor();
      }
    }

  }

  private class SofaSelectionListener implements ItemListener {

    public void itemStateChanged(ItemEvent e) {
      if (MainFrame.this.disableSofaListener) {
        return;
      }
      if (e.getSource() == MainFrame.this.sofaSelectionComboBox) {
        // a new sofa was selected. Switch to that view and update
        // display
        String sofaId = (String) e.getItem();
        MainFrame.this.cas = MainFrame.this.cas.getView(sofaId);
        String text = MainFrame.this.cas.getDocumentText();
        if (text == null) {
          text = MainFrame.this.cas.getSofaDataURI();
          if (text != null) {
            text = "SofaURI = " + text;
          } else {
            if (null != MainFrame.this.cas.getSofaDataArray()) {
              text = "Sofa array with mime type = " + MainFrame.this.cas.getSofa().getSofaMime();
            }
          }
        }
        MainFrame.this.textArea.setText(text);
        if (text == null) {
          MainFrame.this.textArea.repaint();
        }
        MainFrame.this.updateIndexTree(true);
      }
    }
  }

  public void loadAEDescriptor(File descriptorFile) {
    setWaitCursor();
    if (descriptorFile.exists() && descriptorFile.isFile()) {
      this.annotOpenDir = descriptorFile.getParentFile();
    }
    Timer time = new Timer();
    time.start();
    boolean success = false;
    try {
      success = setupAE(descriptorFile);
    } catch (Exception e) {
      handleException(e);
    } catch (NoClassDefFoundError e) {
      // We don't want to catch all errors, but some are ok.
      handleException(e);
    }
    time.stop();
    addRecentDescFile(descriptorFile);
    if (!success) {
      setStatusbarMessage("Failed to load AE specifier: " + descriptorFile.getName());
      this.reRunMenu.setText("Run AE");
      setAEStatusMessage();
      resetCursor();
      return;
    }
    if (this.ae != null) {
      String annotName = this.ae.getAnalysisEngineMetaData().getName();
      this.reRunMenu.setText("Run " + annotName);
      this.reRunMenu.setEnabled(true);
      this.runOnCasMenuItem.setText("Run " + annotName + " on CAS");
      setAEStatusMessage();
      setStatusbarMessage("Done loading AE " + annotName + " in " + time.getTimeSpan() + ".");
    }
    resetCursor();
  }

  private class AnnotatorRerunEventHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      runAE(true);
    }
  }

  private class AnnotatorRunOnCasEventHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      runAE(false);
    }
  }

  public void runAE(boolean doCasReset) {
    setStatusbarMessage("Running Annotator.");
    Timer timer = new Timer();
    timer.start();
    if (this.ae == null) {
      JOptionPane.showMessageDialog(this, "No AE loaded.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    internalRunAE(doCasReset);
    timer.stop();
    setStatusbarMessage("Done running AE " + this.ae.getAnalysisEngineMetaData().getName() + " in "
            + timer.getTimeSpan() + ".");
    updateIndexTree(true);
    this.allAnnotViewerItem.setEnabled(false);
    this.isDirty = false;
    this.runOnCasMenuItem.setEnabled(true);
  }

  // Get the size of the window so we can save it for later use.
  private class CloseTypeSystemHandler extends WindowAdapter implements WindowListener {

    public void windowClosing(WindowEvent event) {
      JComponent tsContentPane = (JComponent) ((JFrame) event.getComponent()).getContentPane();
      final int x = tsContentPane.getWidth();
      final int y = tsContentPane.getHeight();
      MainFrame.this.prefs.setProperty(tsWindowSizePref + widthSuffix, Integer.toString(x));
      MainFrame.this.prefs.setProperty(tsWindowSizePref + heightSuffix, Integer.toString(y));
    }

  }

  private class CloseAnnotationViewHandler extends WindowAdapter implements WindowListener {

    public void windowClosing(WindowEvent event) {
      JComponent tsContentPane = (JComponent) ((JFrame) event.getComponent()).getContentPane();
      final int x = tsContentPane.getWidth();
      final int y = tsContentPane.getHeight();
      MainFrame.this.prefs.setProperty(annotViewSizePref + widthSuffix, Integer.toString(x));
      MainFrame.this.prefs.setProperty(annotViewSizePref + heightSuffix, Integer.toString(y));
    }

  }

  private class CloseLogViewHandler extends WindowAdapter implements WindowListener {

    public void windowClosing(WindowEvent event) {
      JComponent contentPane = (JComponent) ((JFrame) event.getComponent()).getContentPane();
      final int x = contentPane.getWidth();
      final int y = contentPane.getHeight();
      MainFrame.this.prefs.setProperty(logViewSizePref + widthSuffix, Integer.toString(x));
      MainFrame.this.prefs.setProperty(logViewSizePref + heightSuffix, Integer.toString(y));
    }

  }

  private class ShowTypesystemHandler implements ActionListener {

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
      if (MainFrame.this.cas == null) {
        return;
      }
      org.apache.uima.tools.annot_view.ts_editor.MainFrame tsFrame = new org.apache.uima.tools.annot_view.ts_editor.MainFrame();
      tsFrame.addWindowListener(new CloseTypeSystemHandler());
      JComponent tsContentPane = (JComponent) tsFrame.getContentPane();
      MainFrame.this.setPreferredSize(tsContentPane, tsWindowSizePref);
      tsFrame.setTypeSystem(MainFrame.this.cas.getTypeSystem());
      tsFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      tsFrame.pack();
      tsFrame.setVisible(true);
    }

  }

  private class SetDataPathHandler implements ActionListener {

    public void actionPerformed(ActionEvent arg0) {
      String result = (String) JOptionPane.showInputDialog(MainFrame.this, "Specify the data path",
              "Set data path", JOptionPane.PLAIN_MESSAGE, null, null, MainFrame.this.dataPathName);

      if (result != null) {
        MainFrame.this.setDataPath(result);
      }
    }

  }

  public void setDataPath(String dataPath) {
    this.dataPathName = dataPath;
  }

  private class SetCodePageHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
      MainFrame.this.codePage = item.getText();
    }

  }

  private class RemoveCodePageHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String cp = ((JMenuItem) e.getSource()).getText();
      if (MainFrame.this.codePage.equals(cp)) {
        MainFrame.this.codePage = null;
      }
      MainFrame.this.codePages.remove(cp);
      resetCPMenu();
    }

  }

  private class SetLanguageHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
      MainFrame.this.language = item.getText();
    }

  }

  private class RemoveLanguageHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String lang = ((JMenuItem) e.getSource()).getText();
      if (MainFrame.this.language.equals(lang)) {
        MainFrame.this.language = null;
      }
      MainFrame.this.languages.remove(lang);
      resetLangMenu();
    }

  }

  private class RestoreLangDefaultsHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      MainFrame.this.language = null;
      MainFrame.this.languagePrefsList = null;
      createLanguages();
      resetLangMenu();
    }

  }

  private class RestoreCPDefaultsHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      MainFrame.this.codePage = null;
      MainFrame.this.codePagePrefsList = null;
      createCodePages();
      resetCPMenu();
    }

  }

  private class ShowAnnotatedTextHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      String title = MainFrame.this.indexLabel + " - " + MainFrame.this.index.getType().getName();
      MultiAnnotViewerFrame f = new MultiAnnotViewerFrame(title);
      f.addWindowListener(new CloseAnnotationViewHandler());
      FSIterator it = MainFrame.this.index.iterator();
      final String text = MainFrame.this.cas.getDocumentText();
      System.out.println("Creating extents.");
      AnnotationExtent[] extents = MultiMarkup.createAnnotationMarkups(it, text.length(),
              MainFrame.this.styleMap);
      System.out.println("Initializing text frame.");
      f.init(text, extents, getDimension(MainFrame.annotViewSizePref));
      System.out.println("Done.");
    }

  }

  private class ShowAnnotationCustomizerHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      AnnotationDisplayCustomizationFrame acd = new AnnotationDisplayCustomizationFrame(
              "Customize Annotation Display");
      acd.init(MainFrame.this.styleMap, MainFrame.this.cas);
      acd.pack();
      acd.setVisible(true);
    }

  }

  private class AddCPHandler implements ActionListener {

    public void actionPerformed(ActionEvent arg0) {
      String input = JOptionPane.showInputDialog(MainFrame.this, "Add new code page option");
      if (input != null && input.length() > 0) {
        addCodePage(input);
      }
    }

  }

  private class AddLangHandler implements ActionListener {

    public void actionPerformed(ActionEvent arg0) {
      String input = JOptionPane.showInputDialog(MainFrame.this, "Add new language");
      if (input != null && input.length() > 0) {
        addLanguage(input);
      }
    }

  }

  private class AboutHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String javaVersion = System.getProperty("java.version");
      String javaVendor = System.getProperty("java.vendor");
      javaVendor = (javaVendor == null) ? "<Unknown>" : javaVendor;
      String versionInfo = null;
      if (javaVersion == null) {
        versionInfo = "Running on an old version of Java";
      } else {
        versionInfo = "Running Java " + javaVersion + " from " + javaVendor;
      }
      String msg = "CVD (CAS Visual Debugger)\n" + "Apache UIMA Version "
              + UIMAFramework.getVersionString() + "\n"
              + "Copyright 2006 The Apache Software Foundation\n" + versionInfo + "\n";
      Icon icon = Images.getImageIcon(Images.MICROSCOPE);
      if (icon == null) {
        JOptionPane.showMessageDialog(MainFrame.this, msg, "About CVD",
                JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(MainFrame.this, msg, "About CVD",
                JOptionPane.INFORMATION_MESSAGE, icon);
      }
    }

  }

  private static class WindowClosingMouseListener implements MouseListener {

    private JWindow window;

    private WindowClosingMouseListener(JWindow window) {
      this.window = window;
    }

    public void mouseClicked(MouseEvent e) {
      this.window.dispose();
    }

    public void mousePressed(MouseEvent e) {
      // does nothing
    }

    public void mouseReleased(MouseEvent e) {
      // does nothing
    }

    public void mouseEntered(MouseEvent e) {
      // does nothing
    }

    public void mouseExited(MouseEvent e) {
      // does nothing
    }

  }

  private class AboutUimaHandler implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      JWindow window = new JWindow();
      window.addMouseListener(new WindowClosingMouseListener(window));
      JLabel splashLabel = new JLabel(Images.getImageIcon(Images.SPLASH));
      splashLabel.setBorder(null);
      // JPanel panel = new JPanel(new BorderLayout());
      JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      panel.add(splashLabel, BorderLayout.NORTH);
      panel.setBorder(null);
      window.setContentPane(panel);
      window.pack();
      window.setLocationRelativeTo(MainFrame.this);
      window.setVisible(true);
    }

  }

  private class ManualHandler implements ActionListener {

    class Hyperactive implements HyperlinkListener {

      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          JEditorPane pane = (JEditorPane) e.getSource();
          if (e instanceof HTMLFrameHyperlinkEvent) {
            HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            doc.processHTMLFrameHyperlinkEvent(evt);
          } else {
            try {
              pane.setPage(e.getURL());
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        }
      }
    }

    public void actionPerformed(ActionEvent event) {
      try {
        String manFileName = "tools/tools.html";
        JFrame manFrame = new JFrame("CVD Manual");
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(new Hyperactive());
        URL manURL = ClassLoader.getSystemResource(manFileName);
        if (manURL == null) {
          String msg = "Can't find manual. The manual is loaded via the classpath,\n" +
          		"so make sure the manual folder is in the classpath.";
          JOptionPane.showMessageDialog(MainFrame.this, msg, "Error loading manual",
                  JOptionPane.ERROR_MESSAGE);
          return;
        }
        editorPane.setPage(manURL);
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(700, 800));
        manFrame.setContentPane(scrollPane);
        manFrame.pack();
        manFrame.setVisible(true);
        URL cvdLinkUrl = new URL(manURL.toString() + "#ugr.tools.cvd");
        HyperlinkEvent e = new HyperlinkEvent(editorPane, HyperlinkEvent.EventType.ACTIVATED, cvdLinkUrl);
        editorPane.fireHyperlinkUpdate(e);
      } catch (Exception e) {
        handleException(e);
      }
    }

  }

  private class HelpHandler implements ActionListener {

    public void actionPerformed(ActionEvent event) {
      String msg = "There is currently no online help."
              + "\nPlease find documentation on CVD and UIMA"
              + "\nin the doc directory of the UIMA installation";
      JOptionPane.showMessageDialog(MainFrame.this, msg, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

  }

  private class UndoMgr extends UndoManager implements ActionListener {

    private static final long serialVersionUID = 7677701629555379146L;

    public void actionPerformed(ActionEvent arg0) {
      undo();
      if (!canUndo()) {
        MainFrame.this.undoItem.setEnabled(false);
      }
    }

    public synchronized boolean addEdit(UndoableEdit arg0) {
      MainFrame.this.undoItem.setEnabled(true);
      return super.addEdit(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.undo.UndoManager#discardAllEdits()
     */
    public synchronized void discardAllEdits() {
      super.discardAllEdits();
      MainFrame.this.undoItem.setEnabled(false);
    }

  }
  
  private static final String loggerPropertiesFileName = "org/apache/uima/tools/annot_view/Logger.properties";

  private static final String defaultText =
  // "Load a text file and/or edit text here.";
  "Load or edit text here.";

  private static final String titleText = "CAS Visual Debugger (CVD)";

  static final String htmlGrayColor = "<font color=#808080>";

  private static final String indexReposRootLabel = "<html><b>CAS Index Repository</b></html>";

  private static final String noIndexReposLabel = "<html><b>" + htmlGrayColor
          + "CAS Index Repository</b></html>";

  // private static final String noIndexReposLabel = indexReposRootLabel;

  // The content areas.
  protected JTextArea textArea;

  private JTree indexTree;

  private JTree fsTree;

  private JPanel statusPanel;

  private JTextField statusBar;

  private JTextField fileStatus;

  private JTextField aeStatus;

  private JTextField caretStatus;

  private Border textTitleBorder;

  // Dirty flag for the editor.
  private boolean isDirty;

  // The scroll panels.
  private JScrollPane textScrollPane;

  private JScrollPane indexTreeScrollPane;

  private JScrollPane fsTreeScrollPane;

  // Menus
  protected JMenu fileMenu = null;

  private JMenuItem fileSaveItem = null;

  private JMenu editMenu;

  private JMenuItem undoItem;

  private UndoMgr undoMgr;

  private Action cutAction;

  private Action copyAction;

  private JMenuItem allAnnotViewerItem;

  private JMenuItem acdItem;

  private JMenuItem tsViewerItem;

  private JMenuItem reRunMenu;

  private JMenuItem runOnCasMenuItem;

  private JPopupMenu textPopup;

  private JMenuItem xcasReadItem;

  private JMenuItem xcasWriteItem;

  private JMenuItem typeSystemWriteItem;

  private JMenuItem typeSystemReadItem;

  private JMenu recentTextFileMenu;

  private JMenu recentDescFileMenu;

  // Code page support
  private String codePagePrefsList = null;

  private ArrayList codePages = null;

  private String codePage = null;

  private JMenu cpMenu;

  private ButtonGroup cpButtons;

  private static final String defaultCodePages = "US-ASCII,ISO-8859-1,UTF-8,UTF-16BE,UTF-16LE,UTF-16";

  // Language support
  private String languagePrefsList = null;

  // private String defaultLanguagePref = null;
  private ArrayList languages = null;

  private JMenu langMenu;

  private ButtonGroup langButtons;

  private static final String LANGUAGE_DEFAULT = "en";

  private String language;

  private static final String defaultLanguages = "de,en,fr,ja,ko-kr,pt-br,zh-cn,zh-tw,x-unspecified";

  protected File textFile = null;

  private File fileOpenDir = null;

  private File annotOpenDir = null;

  private File xcasFileOpenDir = null;

  private File colorSettingsDir = null;

  // Selected index
  private String indexLabel = null;

  private FSIndex index = null;

  private boolean isAnnotIndex = false;

  // private ArrayList runConfigs;

  protected CAS cas = null;

  private File aeDescriptorFile = null;

  private AnalysisEngine ae = null;

  private File logFile = null;

  private PrintStream logStream = null;

  private Logger log = null;

  private File colorSettingFile;

  private static final Color selectionColor = Color.orange;

  private Properties prefs;

  private static final String textDirPref = "dir.open.text";

  private static final String aeDirPref = "dir.open.tae";

  private static final String xcasDirPref = "dir.open.xcas";

  private static final String textSizePref = "textArea.size";

  private static final String indexTreeSizePref = "indexTree.size";

  private static final String fsTreeSizePref = "fsTree.size";

  private static final String tsWindowSizePref = "tsWindow.size";

  private static final String annotViewSizePref = "annotViewWindow.size";

  private static final String logViewSizePref = "logViewWindow.size";

  private static final String widthSuffix = ".width";

  private static final String heightSuffix = ".height";

  private static final String colorFilePref = "colors.file";

  private static final String colorDirPref = "colors.dir";

  private static final String cpCurrentPref = "cp.selected";

  private static final String cpListPref = "cp.list";

  private static final String langCurrentPref = "lang.selected";

  private static final String langListPref = "lang.list";

  private static final String textFileListPref = "file.text.list";

  private static final String descFileListPref = "file.desc.list";

  private static final String dataPathPref = "datapath";

  private static final Dimension textDimensionDefault = new Dimension(500, 400);

  private static final Dimension fsTreeDimensionDefault = new Dimension(200, 200);

  private static final Dimension logFileDimensionDefault = new Dimension(500, 600);

  public static final String DEFAULT_STYLE_NAME = "defaultStyle";

  private HashMap styleMap = new HashMap();

  // For recently used text and descriptor files.
  private ArrayList textFileNameList = new ArrayList();

  private int numRecentTextFiles = 0;

  private int nextToReplaceTextFile = 0;

  private ArrayList descFileNameList = new ArrayList();

  private int numRecentDescFiles = 0;

  private int nextToReplaceDescFile = 0;

  private static final int maxRecentSize = 8;

  // For cursor handling (busy cursor). Is there a better way?
  private ArrayList cursorOwningComponents = new ArrayList();

  private ArrayList cursorCache = null;

  private String dataPathName;

  private JComboBox sofaSelectionComboBox;

  private JPanel sofaSelectionPanel;

  private boolean disableSofaListener = false;

  /**
   * Constructor for MainFrame.
   * 
   * @throws HeadlessException
   */
  public MainFrame() {
    super();
    init();
  }

  /**
   * Constructor for MainFrame.
   * 
   * @param arg0
   */
  public MainFrame(GraphicsConfiguration arg0) {
    super(arg0);
    init();
  }

  /**
   * Constructor for MainFrame.
   * 
   * @param arg0
   * @throws HeadlessException
   */
  public MainFrame(String arg0) {
    super(arg0);
    init();
  }

  /**
   * Constructor for MainFrame.
   * 
   * @param arg0
   * @param arg1
   */
  public MainFrame(String arg0, GraphicsConfiguration arg1) {
    super(arg0, arg1);
    init();
  }

  protected void handleException(Throwable e) {
    StringBuffer msg = new StringBuffer();
    handleException(e, msg);
  }

  protected void handleException(Throwable e, StringBuffer msg) {
    if (e.getMessage() == null) {
      msg.append(e.getClass().getName());
    } else {
      msg.append(e.getMessage());
    }
    if (this.log != null) {
      if (e instanceof java.lang.Exception) {
        this.log.log(Level.SEVERE, ((Exception) e).getLocalizedMessage(), e);
      } else {
        e.printStackTrace(this.logStream);
      }
      msg.append("\nMore detailed information is in the log file.");
    }
    boolean hasAsserts = false;
    // assert(hasAsserts = true);
    if (hasAsserts) {
      e.printStackTrace();
    }
    JOptionPane.showMessageDialog(this, msg.toString(), "Exception", JOptionPane.ERROR_MESSAGE);

  }

  private void showError(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  protected void loadFile() {
    try {
      if (this.textFile.exists() && this.textFile.canRead()) {
        String text = null;
        if (this.codePage == null) {
          text = FileUtils.file2String(this.textFile);
        } else {
          text = FileUtils.file2String(this.textFile, this.codePage);
        }
        setTextNoTitle(text);
        setTitle();
        addRecentTextFile(this.textFile);
      } else {
        handleException(new IOException("File does not exist or is not readable: "
                + this.textFile.getAbsolutePath()));
      }
      // Add the loaded file to the recently used files list.
    } catch (UnsupportedEncodingException e) {
      StringBuffer msg = new StringBuffer("Unsupported text encoding (code page): ");
      handleException(e, msg);
    } catch (Exception e) {
      handleException(e);
    }
  }

  private static final int getMnemonic(int i) {
    switch (i) {
      case 1:
        return KeyEvent.VK_1;
      case 2:
        return KeyEvent.VK_2;
      case 3:
        return KeyEvent.VK_3;
      case 4:
        return KeyEvent.VK_4;
      case 5:
        return KeyEvent.VK_5;
      case 6:
        return KeyEvent.VK_6;
      case 7:
        return KeyEvent.VK_7;
      case 8:
        return KeyEvent.VK_8;
      case 9:
        return KeyEvent.VK_9;
      default:
        return KeyEvent.VK_0;
    }
  }

  private final JMenuItem createRecentTextFileItem(int num, File file) {
    String fileShortName = file.getName();
    JMenuItem item = new JMenuItem(num + " " + fileShortName, getMnemonic(num));
    item.addActionListener(new LoadRecentTextFileEventHandler(file.getAbsolutePath()));
    item.setToolTipText(file.getAbsolutePath());
    return item;
  }

  private void addRecentTextFile(File file) {
    if (this.textFileNameList.contains(file.getAbsolutePath())) {
      return;
    }
    if (this.numRecentTextFiles < maxRecentSize) {
      ++this.numRecentTextFiles;
      ++this.nextToReplaceTextFile;
      this.recentTextFileMenu.add(createRecentTextFileItem(this.numRecentTextFiles, file));
      this.textFileNameList.add(file.getAbsolutePath());
    } else {
      if (this.nextToReplaceTextFile >= maxRecentSize) {
        this.nextToReplaceTextFile = 0;
      }
      this.textFileNameList.set(this.nextToReplaceTextFile, file.getAbsolutePath());
      JMenuItem item = createRecentTextFileItem(this.nextToReplaceTextFile + 1, file);
      this.recentTextFileMenu.remove(this.nextToReplaceTextFile);
      this.recentTextFileMenu.insert(item, this.nextToReplaceTextFile);
      ++this.nextToReplaceTextFile;
    }
  }

  private final JMenuItem createRecentDescFileItem(int num, File file) {
    String fileShortName = file.getName();
    JMenuItem item = new JMenuItem(num + " " + fileShortName, getMnemonic(num));
    item.addActionListener(new LoadRecentDescFileEventHandler(file.getAbsolutePath()));
    item.setToolTipText(file.getAbsolutePath());
    return item;
  }

  private void addRecentDescFile(File file) {
    if (this.descFileNameList.contains(file.getAbsolutePath())) {
      return;
    }
    if (this.numRecentDescFiles < maxRecentSize) {
      ++this.numRecentDescFiles;
      ++this.nextToReplaceDescFile;
      this.recentDescFileMenu.add(createRecentDescFileItem(this.numRecentDescFiles, file));
      this.descFileNameList.add(file.getAbsolutePath());
    } else {
      if (this.nextToReplaceDescFile >= maxRecentSize) {
        this.nextToReplaceDescFile = 0;
      }
      this.descFileNameList.set(this.nextToReplaceDescFile, file.getAbsolutePath());
      JMenuItem item = createRecentDescFileItem(this.nextToReplaceDescFile + 1, file);
      this.recentDescFileMenu.remove(this.nextToReplaceDescFile);
      this.recentDescFileMenu.insert(item, this.nextToReplaceDescFile);
      ++this.nextToReplaceDescFile;
    }
  }

  /**
   * Set the text to be analyzed.
   * 
   * @param text
   *          The text.
   */
  public void setText(String text) {
    this.textFile = null;
    setTextNoTitle(text);
    setTitle();
  }

  /**
   * Load a text file.
   * 
   * @param textFile1
   *          The text file.
   */
  public void loadTextFile(File textFile1) {
    this.textFile = textFile1;
    loadFile();
  }

  // Set the text.
  private void setTextNoTitle(String text) {
    this.textArea.setText(text);
    this.textArea.getCaret().setDot(0);
    this.isDirty = false;
  }

  private void setTitle() {
    StringBuffer buf = new StringBuffer();
    buf.append(titleText);
    if (this.textFile != null) {
      buf.append(": \"");
      buf.append(this.textFile.getAbsolutePath());
      buf.append("*");
      buf.append("\"");
    }
    this.setTitle(buf.toString());
  }

  private boolean saveFile() {
    if (this.textFile.exists() && !this.textFile.canWrite()) {
      showError("File is not writable: " + this.textFile.getAbsolutePath());
      return false;
    }
    final String text = this.textArea.getText();
    FileOutputStream fileOutStream = null;
    try {
      fileOutStream = new FileOutputStream(this.textFile);
    } catch (FileNotFoundException e) {
      handleException(e);
      return false;
    }
    Writer writer = null;
    if (this.codePage == null) {
      writer = new OutputStreamWriter(fileOutStream);
    } else {
      try {
        writer = new OutputStreamWriter(fileOutStream, this.codePage);
      } catch (UnsupportedEncodingException e) {
        handleException(e);
        return false;
      }
    }
    try {
      writer.write(text);
      writer.close();
      this.isDirty = false;
      setTitle();
    } catch (IOException e) {
      handleException(e);
      return false;
    }
    return true;
  }

  private void createTextArea() {
    try {
      this.textArea = new JTextArea();
      this.addCursorOwningComponent(this.textArea);
      Border emptyBorder = BorderFactory.createEmptyBorder(2, 4, 2, 2);
      Border grayLineBordre = BorderFactory.createLineBorder(Color.gray, 1);
      this.textArea.setBorder(BorderFactory.createCompoundBorder(grayLineBordre, emptyBorder));
      this.textArea.setSelectionColor(selectionColor);
      this.textArea.setEditable(true);
      this.textArea.setLineWrap(true);
      this.textArea.setWrapStyleWord(true);
      this.textArea.setText(defaultText);
      this.textArea.addMouseListener(new PopupListener());
      // textArea.setFocusable(true);
      this.textArea.addFocusListener(new TextFocusHandler());
      this.textArea.getDocument().addDocumentListener(new TextChangedListener());
      this.textArea.addCaretListener(new CaretChangeHandler());
      this.undoMgr = new UndoMgr();
      this.textArea.getDocument().addUndoableEditListener(this.undoMgr);
    } catch (Exception e) {
      handleException(e);
    }
  }

  private void populateEditMenu() {
    this.undoItem = new JMenuItem("Undo");
    this.undoItem.addActionListener(this.undoMgr);
    this.undoItem.setEnabled(false);
    this.undoItem.setMnemonic(KeyEvent.VK_U);
    this.undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
    this.editMenu.add(this.undoItem);
    this.editMenu.addSeparator();
    HashMap actionMap = createEditActionMap();
    // Cut
    this.cutAction = (Action) actionMap.get(DefaultEditorKit.cutAction);
    this.cutAction.putValue(Action.NAME, "Cut");
    this.cutAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
    this.cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
            InputEvent.CTRL_MASK));
    this.cutAction.setEnabled(false);
    this.editMenu.add(this.cutAction);
    // Copy
    this.copyAction = (Action) actionMap.get(DefaultEditorKit.copyAction);
    this.copyAction.putValue(Action.NAME, "Copy");
    this.copyAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
    this.copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
            InputEvent.CTRL_MASK));
    this.copyAction.setEnabled(false);
    this.editMenu.add(this.copyAction);
    // Paste
    Action pasteAction = (Action) actionMap.get(DefaultEditorKit.pasteAction);
    pasteAction.putValue(Action.NAME, "Paste");
    pasteAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
    pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
            InputEvent.CTRL_MASK));
    this.editMenu.add(pasteAction);
  }

  private HashMap createEditActionMap() {
    HashMap map = new HashMap();
    Action[] ar = this.textArea.getActions();
    for (int i = 0; i < ar.length; i++) {
      Action a = ar[i];
      map.put(a.getValue(Action.NAME), a);
    }
    return map;
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    createFileMenu();
    menuBar.add(this.fileMenu);
    menuBar.add(createEditMenu());
    menuBar.add(createRunMenu());
    menuBar.add(createToolsMenu());
    menuBar.add(createHelpMenu());
    return menuBar;
  }

  private JMenu createEditMenu() {
    this.editMenu = new JMenu("Edit");
    this.editMenu.setMnemonic(KeyEvent.VK_E);
    populateEditMenu();
    return this.editMenu;
  }

  private JMenu createHelpMenu() {
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    JMenuItem manualItem = new JMenuItem("Manual", KeyEvent.VK_M);
    manualItem.addActionListener(new ManualHandler());
    helpMenu.add(manualItem);
    JMenuItem helpInfoItem = new JMenuItem("Help", KeyEvent.VK_H);
    helpInfoItem.addActionListener(new HelpHandler());
    helpMenu.add(helpInfoItem);
    helpMenu.addSeparator();
    JMenuItem aboutItem = new JMenuItem("About CVD", KeyEvent.VK_A);
    aboutItem.addActionListener(new AboutHandler());
    helpMenu.add(aboutItem);
    JMenuItem aboutUimaItem = new JMenuItem("About UIMA", KeyEvent.VK_U);
    aboutUimaItem.addActionListener(new AboutUimaHandler());
    helpMenu.add(aboutUimaItem);
    return helpMenu;
  }

  private void createFileMenu() {
    this.fileMenu = new JMenu("File");
    JMenuItem newTextItem = new JMenuItem("New Text...", KeyEvent.VK_N);
    newTextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    newTextItem.addActionListener(new NewTextEventHandler());
    this.fileMenu.add(newTextItem);
    this.fileMenu.setMnemonic(KeyEvent.VK_F);
    JMenuItem fileOpen = new JMenuItem("Open Text File", KeyEvent.VK_O);
    fileOpen.addActionListener(new FileOpenEventHandler());
    fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
    this.fileMenu.add(fileOpen);
    this.fileSaveItem = new JMenuItem("Save Text File", KeyEvent.VK_S);
    this.fileSaveItem.setEnabled(false);
    this.fileSaveItem.addActionListener(new FileSaveEventHandler());
    this.fileSaveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    this.fileMenu.add(this.fileSaveItem);
    JMenuItem fileSaveAsItem = new JMenuItem("Save Text As...", KeyEvent.VK_A);
    fileSaveAsItem.addActionListener(new FileSaveAsEventHandler());
    this.fileMenu.add(fileSaveAsItem);
    createCPMenu();
    this.cpMenu.setMnemonic(KeyEvent.VK_P);
    this.fileMenu.add(this.cpMenu);
    this.fileMenu.addSeparator();
    this.recentTextFileMenu = new JMenu("Recently used ...");
    this.recentTextFileMenu.setMnemonic(KeyEvent.VK_U);
    this.fileMenu.add(this.recentTextFileMenu);
    this.fileMenu.addSeparator();
    JMenuItem colorPrefsOpenItem = new JMenuItem("Load Color Settings", KeyEvent.VK_L);
    colorPrefsOpenItem.addActionListener(new ColorPrefsOpenHandler());
    this.fileMenu.add(colorPrefsOpenItem);
    JMenuItem colorPrefsSaveItem = new JMenuItem("Save Color Settings", KeyEvent.VK_C);
    colorPrefsSaveItem.addActionListener(new ColorPrefsSaveHandler());
    this.fileMenu.add(colorPrefsSaveItem);
    this.fileMenu.addSeparator();
    this.typeSystemReadItem = new JMenuItem("Read Type System File");
    this.typeSystemReadItem.setEnabled(true);
    this.typeSystemReadItem.addActionListener(new TypeSystemFileOpenEventHandler());
    this.fileMenu.add(this.typeSystemReadItem);
    this.typeSystemWriteItem = new JMenuItem("Write Type System File");
    this.typeSystemWriteItem.setEnabled(false);
    this.typeSystemWriteItem.addActionListener(new XCASSaveTSHandler());
    this.fileMenu.add(this.typeSystemWriteItem);
    this.fileMenu.addSeparator();
    this.xcasWriteItem = new JMenuItem("Write XCAS File", KeyEvent.VK_W);
    this.xcasWriteItem.setEnabled(false);
    this.xcasWriteItem.addActionListener(new XCASSaveHandler());
    this.fileMenu.add(this.xcasWriteItem);
    this.xcasReadItem = new JMenuItem("Read XCAS File", KeyEvent.VK_R);
    this.xcasReadItem.addActionListener(new XCASFileOpenEventHandler());
    this.xcasReadItem.setEnabled(false);
    this.fileMenu.add(this.xcasReadItem);
    this.fileMenu.addSeparator();
    JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
    exit.addActionListener(new SystemExitHandler());
    this.fileMenu.add(exit);
  }

  private final void addCursorOwningComponent(Component comp) {
    this.cursorOwningComponents.add(comp);
  }

  private final void setWaitCursor() {
    this.setEnabled(false);
    this.cursorCache = new ArrayList();
    for (int i = 0; i < this.cursorOwningComponents.size(); i++) {
      Component comp = (Component) this.cursorOwningComponents.get(i);
      this.cursorCache.add(comp.getCursor());
      comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
  }

  private final void resetCursor() {
    if (this.cursorCache == null) {
      return;
    }
    // assert(this.cursorCache != null);
    // assert(this.cursorCache.size() ==
    // this.cursorOwningComponents.size());
    for (int i = 0; i < this.cursorOwningComponents.size(); i++) {
      Component comp = (Component) this.cursorOwningComponents.get(i);
      comp.setCursor((Cursor) this.cursorCache.get(i));
    }
    this.setEnabled(true);
  }

  private void addCodePage(String codePage1) {
    this.codePage = codePage1;
    if (!this.codePages.contains(codePage1)) {
      this.codePages.add(codePage1);
    }
    resetCPMenu();
  }

  private void createCodePages() {
    this.codePages = new ArrayList();
    if (this.codePagePrefsList == null) {
      this.codePagePrefsList = defaultCodePages;
    }
    // Add the system code page and use it as default. This is super klunky.
    String sysCodePage = null;
    try {
      File tmpFile = File.createTempFile("uima", "tmp");
      FileOutputStream fos = new FileOutputStream(tmpFile);
      OutputStreamWriter writer = new OutputStreamWriter(fos);
      sysCodePage = writer.getEncoding();
      writer.close();
    } catch (IOException e) {
      // do nothing
    }
    if (sysCodePage != null) {
      this.codePages.add(sysCodePage);
      if (this.codePage == null) {
        this.codePage = sysCodePage;
      }
    }
    StringTokenizer tok = new StringTokenizer(this.codePagePrefsList, ",");
    String cp;
    while (tok.hasMoreTokens()) {
      cp = tok.nextToken();
      if (!this.codePages.contains(cp)) {
        this.codePages.add(cp);
      }
    }
  }

  private void resetTrees() {
    updateIndexTree(false);
  }

  private void createCPMenu() {
    createCodePages();
    this.cpMenu = new JMenu("Code Page");
    resetCPMenu();
  }

  private void resetCPMenu() {
    this.cpMenu.removeAll();
    this.cpButtons = new ButtonGroup();
    JRadioButtonMenuItem item;
    String cp;
    for (int i = 0; i < this.codePages.size(); i++) {
      cp = (String) this.codePages.get(i);
      item = new JRadioButtonMenuItem(cp);
      if (cp.equals(this.codePage)) {
        item.setSelected(true);
      }
      item.addActionListener(new SetCodePageHandler());
      this.cpButtons.add(item);
      this.cpMenu.add(item);
    }
    this.cpMenu.addSeparator();
    JMenuItem addCPItem = new JMenuItem("Add code page");
    addCPItem.addActionListener(new AddCPHandler());
    this.cpMenu.add(addCPItem);
    JMenu removeMenu = new JMenu("Remove code page");
    for (int i = 0; i < this.codePages.size(); i++) {
      JMenuItem rmItem = new JMenuItem((String) this.codePages.get(i));
      rmItem.addActionListener(new RemoveCodePageHandler());
      removeMenu.add(rmItem);
    }
    this.cpMenu.add(removeMenu);
    JMenuItem restoreDefaultsItem = new JMenuItem("Restore defaults");
    restoreDefaultsItem.addActionListener(new RestoreCPDefaultsHandler());
    this.cpMenu.add(restoreDefaultsItem);
  }

  private void addLanguage(String language1) {
    this.language = language1;
    if (!this.languages.contains(language1)) {
      this.languages.add(language1);
    }
    resetLangMenu();
  }

  private void createLangMenu() {
    createLanguages();
    this.langMenu = new JMenu("Language");
    resetLangMenu();
  }

  private void resetLangMenu() {
    this.langMenu.removeAll();
    this.langButtons = new ButtonGroup();
    JRadioButtonMenuItem item;
    String lang;
    for (int i = 0; i < this.languages.size(); i++) {
      lang = (String) this.languages.get(i);
      item = new JRadioButtonMenuItem(lang);
      if (lang.equals(this.language)) {
        item.setSelected(true);
      }
      item.addActionListener(new SetLanguageHandler());
      this.langButtons.add(item);
      this.langMenu.add(item);
    }
    this.langMenu.addSeparator();
    JMenuItem addLangItem = new JMenuItem("Add language");
    addLangItem.addActionListener(new AddLangHandler());
    this.langMenu.add(addLangItem);
    JMenu removeMenu = new JMenu("Remove language");
    for (int i = 0; i < this.languages.size(); i++) {
      JMenuItem rmItem = new JMenuItem((String) this.languages.get(i));
      rmItem.addActionListener(new RemoveLanguageHandler());
      removeMenu.add(rmItem);
    }
    this.langMenu.add(removeMenu);
    JMenuItem restoreDefaultsItem = new JMenuItem("Restore defaults");
    restoreDefaultsItem.addActionListener(new RestoreLangDefaultsHandler());
    this.langMenu.add(restoreDefaultsItem);
  }

  private void createLanguages() {
    this.languages = new ArrayList();
    if (this.languagePrefsList == null) {
      this.languagePrefsList = defaultLanguages;
    }
    if (this.language == null) {
      this.language = LANGUAGE_DEFAULT;
    }
    StringTokenizer tok = new StringTokenizer(this.languagePrefsList, ",");
    String lang;
    while (tok.hasMoreTokens()) {
      lang = tok.nextToken();
      if (!this.languages.contains(lang)) {
        this.languages.add(lang);
      }
    }
  }

  private JMenu createRunMenu() {
    JMenu runMenu = new JMenu("Run");
    runMenu.setMnemonic(KeyEvent.VK_R);
    JMenuItem runMenuItem = new JMenuItem("Load AE", KeyEvent.VK_L);
    runMenuItem.addActionListener(new AnnotatorOpenEventHandler(this));
    runMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
    runMenu.add(runMenuItem);
    this.reRunMenu = new JMenuItem("Run AE", KeyEvent.VK_R);
    this.reRunMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
    runMenu.add(this.reRunMenu);
    this.reRunMenu.addActionListener(new AnnotatorRerunEventHandler());
    this.reRunMenu.setEnabled(false);
    this.runOnCasMenuItem = new JMenuItem("Run AE on CAS", KeyEvent.VK_Y);
    this.runOnCasMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
            ActionEvent.CTRL_MASK));
    runMenu.add(this.runOnCasMenuItem);
    this.runOnCasMenuItem.addActionListener(new AnnotatorRunOnCasEventHandler());
    this.runOnCasMenuItem.setEnabled(false);
    runMenu.addSeparator();
    this.recentDescFileMenu = new JMenu("Recently used ...");
    this.recentDescFileMenu.setMnemonic(KeyEvent.VK_U);
    runMenu.add(this.recentDescFileMenu);
    runMenu.addSeparator();
    createLangMenu();
    runMenu.add(this.langMenu);
    this.langMenu.setMnemonic(KeyEvent.VK_L);
    // runMenu.addSeparator();
    JMenuItem dataPathItem = new JMenuItem("Set data path");
    dataPathItem.addActionListener(new SetDataPathHandler());
    dataPathItem.setMnemonic(KeyEvent.VK_S);
    runMenu.addSeparator();
    runMenu.add(dataPathItem);
    return runMenu;
  }

  private JMenu createToolsMenu() {
    JMenu toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic(KeyEvent.VK_T);
    this.tsViewerItem = new JMenuItem("View Typesystem", KeyEvent.VK_T);
    this.tsViewerItem.addActionListener(new ShowTypesystemHandler());
    this.tsViewerItem.setEnabled(false);
    toolsMenu.add(this.tsViewerItem);
    this.allAnnotViewerItem = new JMenuItem("Show Selected Annotations", KeyEvent.VK_A);
    this.allAnnotViewerItem.addActionListener(new ShowAnnotatedTextHandler());
    toolsMenu.add(this.allAnnotViewerItem);
    this.allAnnotViewerItem.setEnabled(false);
    this.acdItem = new JMenuItem("Customize Annotation Display", KeyEvent.VK_C);
    toolsMenu.add(this.acdItem);
    this.acdItem.setEnabled(false);
    this.acdItem.addActionListener(new ShowAnnotationCustomizerHandler());
    JMenuItem logViewer = new JMenuItem("View Log File", KeyEvent.VK_L);
    logViewer.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent event) {
        if (MainFrame.this.logStream != null) {
          MainFrame.this.logStream.flush();
        }
        LogFileViewer viewer = new LogFileViewer("Log file: "
                + MainFrame.this.logFile.getAbsolutePath());
        viewer.addWindowListener(new CloseLogViewHandler());
        Dimension dim = getDimension(logViewSizePref);
        if (dim == null) {
          dim = logFileDimensionDefault;
        }
        viewer.init(MainFrame.this.logFile, dim);
      }
    });
    toolsMenu.add(logViewer);
    return toolsMenu;
  }

  private void createStatusBar() {
    this.statusPanel = new JPanel();
    // statusPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
    this.statusPanel.setLayout(new BoxLayout(this.statusPanel, BoxLayout.X_AXIS));
    this.statusBar = new JTextField();
    Border innerCompound = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(
            BevelBorder.LOWERED, Color.lightGray, Color.darkGray), BorderFactory.createEmptyBorder(
            0, 3, 0, 3));
    Border leftCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(
            0, 0, 0, 1), innerCompound);
    Border middleCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory
            .createEmptyBorder(0, 1, 0, 1), innerCompound);
    Border rightCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory
            .createEmptyBorder(0, 1, 0, 0), innerCompound);
    // statusBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    this.statusBar.setBorder(leftCompoundBorder);
    this.statusBar.setEditable(false);
    this.statusBar.setBackground(this.getBackground());
    this.statusBar.setText("Starting up.");
    this.statusBar.setToolTipText("Status Bar");
    this.statusPanel.add(this.statusBar);
    this.fileStatus = new JTextField();
    // fileStatus.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    this.fileStatus.setBorder(rightCompoundBorder);
    this.fileStatus.setMaximumSize(new Dimension(500, 25));
    this.fileStatus.setEditable(false);
    // fileStatus.setBackground(new Color(204, 204, 255));
    // fileStatus.setBackground(new Color(164, 147, 255));
    // statusPanel.add(fileStatus);
    this.aeStatus = new JTextField();
    this.aeStatus.setBorder(middleCompoundBorder);
    this.aeStatus.setMaximumSize(new Dimension(500, 25));
    this.aeStatus.setEditable(false);
    this.statusPanel.add(this.aeStatus);
    this.caretStatus = new JTextField();
    this.caretStatus.setBorder(rightCompoundBorder);
    this.caretStatus.setMaximumSize(new Dimension(500, 25));
    // caretStatus.setBackground(new Color(204, 255, 204));
    this.caretStatus.setEditable(false);
    this.caretStatus.setToolTipText("Position of cursor or extent of selection");
    this.statusPanel.add(this.caretStatus);
    setCaretStatus(0, 0);
    // setFileStatusMessage();
    setAEStatusMessage();
  }

  private void setCaretStatus(final int dot, final int mark) {
    if (dot == mark) {
      this.caretStatus.setText("Cursor: " + dot);
    } else {
      int from, to;
      if (dot < mark) {
        from = dot;
        to = mark;
      } else {
        from = mark;
        to = dot;
      }
      this.caretStatus.setText("Selection: " + from + " - " + to);
    }
    this.statusPanel.revalidate();
  }

  private void setFileStatusMessage() {
    // if (this.textFile == null)
    // {
    // fileStatus.setText("(No Text File Loaded)");
    // fileStatus.setToolTipText("No text file loaded.");
    // } else
    // {
    // fileStatus.setText(textFile.getName());
    // fileStatus.setToolTipText(
    // "Currently loaded text file: " + textFile.getAbsolutePath());
    // }
    // statusPanel.revalidate();
    Border textBorder;
    if (this.textFile == null) {
      textBorder = BorderFactory.createTitledBorder(this.textTitleBorder, "New Text Buffer");
      // textBorder.setTitleJustification(TitledBorder.ABOVE_TOP);
    } else {
      textBorder = BorderFactory.createTitledBorder(this.textTitleBorder, this.textFile
              .getAbsolutePath());
    }
    this.textScrollPane.setBorder(textBorder);
    this.textScrollPane.revalidate();
  }

  private void setAEStatusMessage() {
    if (this.ae == null || this.aeDescriptorFile == null) {
      this.aeStatus.setText("(No AE Loaded)");
      this.aeStatus.setToolTipText("No AE descriptor loaded.");
    } else {
      this.aeStatus.setText(this.aeDescriptorFile.getName());
      this.aeStatus.setToolTipText("<html>Currently loaded AE descriptor file:<br>"
              + this.aeDescriptorFile.getAbsolutePath() + "</html>");
    }
    this.statusPanel.revalidate();
  }

  private void setStatusbarMessage(String message) {
    // Date date = new Date();
    Calendar calendar = Calendar.getInstance();
    int time;
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    time = calendar.get(Calendar.HOUR_OF_DAY);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append(":");
    time = calendar.get(Calendar.MINUTE);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append(":");
    time = calendar.get(Calendar.SECOND);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append("]  ");
    buf.append(message);
    this.statusBar.setText(buf.toString());
    this.statusPanel.revalidate();
  }

  private void initializeLogging() {
    File homeDir = new File(System.getProperty("user.home"));
    LogManager logManager = LogManager.getLogManager();
    try {
      logManager.readConfiguration(ClassLoader.getSystemResourceAsStream(loggerPropertiesFileName));
    } catch (SecurityException e) {
      handleException(e);
      return;
    } catch (IOException e) {
      handleException(e);
      return;
    }
    this.logFile = new File(homeDir, "uima.log");
    this.log = UIMAFramework.getLogger();
  }
  
  private void init() {
    initializeLogging();
    this.addCursorOwningComponent(this);
    this.addWindowListener(new MainFrameClosing());
    // runConfigs = new ArrayList();
    createTextArea();
    this.setTitle(titleText);
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
    Border grayLineBorder = BorderFactory.createLineBorder(Color.gray, 1);
    splitPane.setBorder(emptyBorder);
    final int dividerSize = 5;
    splitPane.setDividerSize(dividerSize);
    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.add(splitPane, BorderLayout.CENTER);
    contentPane.setBorder(emptyBorder);
    createStatusBar();
    contentPane.add(this.statusPanel, BorderLayout.SOUTH);
    contentPane.setOpaque(true);

    JLabel bannerLabel = new JLabel(Images.getImageIcon(Images.BANNER));
    bannerLabel.setAlignmentX(LEFT_ALIGNMENT);
    contentPane.setAlignmentX(LEFT_ALIGNMENT);
    JPanel mainPane = new JPanel();
    mainPane.setBackground(Color.WHITE);
    // mainPane.setLayout(new BorderLayout());
    mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
    mainPane.add(bannerLabel);
    mainPane.add(contentPane);
    // this.setContentPane(contentPane);
    this.setContentPane(mainPane);
    initIRTree();
    this.indexTree.addMouseListener(new IndexPopupListener());

    // add combobox to select the view
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    this.sofaSelectionPanel = new JPanel();
    this.sofaSelectionComboBox = new JComboBox();
    this.sofaSelectionComboBox.addItem(CommonCas.NAME_DEFAULT_SOFA);
    this.sofaSelectionPanel.add(new JLabel("Select View:"));
    this.sofaSelectionPanel.add(this.sofaSelectionComboBox);
    leftPanel.add(this.sofaSelectionPanel, BorderLayout.NORTH);
    this.sofaSelectionPanel.setVisible(false);
    this.sofaSelectionComboBox.addItemListener(new SofaSelectionListener());
    this.sofaSelectionComboBox
            .setToolTipText("This CAS has multiple Views. Select the View to display.");

    JSplitPane treePairPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    treePairPane.setDividerSize(dividerSize);
    this.indexTreeScrollPane = new JScrollPane(this.indexTree);
    this.indexTreeScrollPane.setBorder(grayLineBorder);
    leftPanel.add(treePairPane);
    splitPane.setLeftComponent(leftPanel);
    // splitPane.setLeftComponent(treePairPane);
    treePairPane.setBorder(BorderFactory.createTitledBorder(grayLineBorder, "Analysis Results"));
    treePairPane.setLeftComponent(this.indexTreeScrollPane);
    initFSTree();
    this.fsTreeScrollPane = new JScrollPane(this.fsTree);
    this.fsTreeScrollPane.setBorder(grayLineBorder);
    treePairPane.setRightComponent(this.fsTreeScrollPane);
    // TitledBorder analysisResultBorder =
    // BorderFactory.createTitledBorder(emptyBorder, "Analysis Results");
    // treePairPane.setBorder(analysisResultBorder);
    this.textScrollPane = new JScrollPane(this.textArea);
    this.textScrollPane.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
    // JPanel textAreaPanel = new JPanel();
    this.textTitleBorder = grayLineBorder;
    setFileStatusMessage();
    splitPane.setRightComponent(this.textScrollPane);
    try {
      loadProgramPreferences();
    } catch (IOException e) {
      handleException(e);
    }
    // Create menus after loading preferences to get code pages.
    this.setJMenuBar(createMenuBar());
    Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    defaultStyle = StyleContext.getDefaultStyleContext().addStyle("defaultAnnot", defaultStyle);
    StyleConstants.setBackground(defaultStyle, selectionColor);
    this.styleMap.put(TCAS.TYPE_NAME_ANNOTATION, defaultStyle);
    this.textPopup = new JPopupMenu();
    this.fsTree.addFocusListener(new TreeFocusHandler(this.fsTree));
    this.indexTree.addFocusListener(new TreeFocusHandler(this.indexTree));
    initKeyMap();
    // Does not work in Java 1.3
    // initFocusTraversalPolicy();
    initFileLists();
    setStatusbarMessage("Ready.");
  }

  private final void initFileLists() {
    int numFiles = this.textFileNameList.size();
    int max = (numFiles < maxRecentSize) ? numFiles : maxRecentSize;
    for (int i = 0; i < max; i++) {
      this.recentTextFileMenu.add(createRecentTextFileItem(i + 1, new File(
              (String) this.textFileNameList.get(i))));
    }
    numFiles = this.descFileNameList.size();
    max = (numFiles < maxRecentSize) ? numFiles : maxRecentSize;
    for (int i = 0; i < max; i++) {
      this.recentDescFileMenu.add(createRecentDescFileItem(i + 1, new File(
              (String) this.descFileNameList.get(i))));
    }
  }

  protected boolean setupAE(File aeFile) {
    try {
      ResourceManager rsrcMgr = null;
      if (this.dataPathName != null) {
        try {
          rsrcMgr = UIMAFramework.newDefaultResourceManager();
          rsrcMgr.setDataPath(this.dataPathName);
        } catch (MalformedURLException e) {
          StringBuffer msg = new StringBuffer();
          msg.append("Error setting data path in AE,\n");
          msg.append("data path contains invalid URL or file descriptor.\n");
          msg.append("You can still run the AE if it doesn't rely on the data path.\n");
          msg.append("Please correct the data path in the \"Run->Set data path\" menu.\n");
          handleException(e, msg);
        }
      }

      // Destroy old AE.
      if (this.ae != null) {
        this.acdItem.setEnabled(false);
        this.xcasWriteItem.setEnabled(false);
        this.tsViewerItem.setEnabled(false);
        this.reRunMenu.setEnabled(false);
        this.runOnCasMenuItem.setEnabled(false);
        this.ae.destroy();
        this.ae = null;
      }

      // get Resource Specifier from XML file
      XMLInputSource in = new XMLInputSource(aeFile);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // for debugging, output the Resource Specifier
      // System.out.println(specifier);

      // create Analysis Engine here
      if (rsrcMgr == null) {
        this.ae = UIMAFramework.produceAnalysisEngine(specifier);
      } else {
        this.ae = UIMAFramework.produceAnalysisEngine(specifier, rsrcMgr, null);
      }
      this.cas = this.ae.newTCAS();
      this.acdItem.setEnabled(true);
      this.tsViewerItem.setEnabled(true);
      this.xcasWriteItem.setEnabled(true);
      this.xcasReadItem.setEnabled(true);
      this.reRunMenu.setEnabled(true);
      this.typeSystemWriteItem.setEnabled(true);

      // reset sofa combo box with just the initial view
      this.disableSofaListener = true;
      this.sofaSelectionComboBox.removeAllItems();
      this.sofaSelectionComboBox.addItem(CommonCas.NAME_DEFAULT_SOFA);
      this.sofaSelectionPanel.setVisible(false);
      this.disableSofaListener = false;
//      MainFrame.this.textArea.setText(null);
//      MainFrame.this.textArea.repaint();
      MainFrame.this.updateIndexTree(true);
    } catch (Exception e) {
      handleException(e);
      return false;
    }
    return true;
  }

  protected void internalRunAE(boolean doCasReset) {
    try {
      if (doCasReset) {
        // Change to Initial view
        this.cas = this.cas.getView(CommonCas.NAME_DEFAULT_SOFA);
        this.cas.reset();
        setLanguage();
        this.cas.setDocumentText(this.textArea.getText());
        this.disableSofaListener = true;
        this.sofaSelectionComboBox.setSelectedIndex(0);
      }
      this.ae.process(this.cas);
      // Update sofacombobox here
      this.disableSofaListener = true;
      int currentViewID = this.sofaSelectionComboBox.getSelectedIndex();
      this.sofaSelectionComboBox.removeAllItems();
      this.sofaSelectionComboBox.addItem(CommonCas.NAME_DEFAULT_SOFA);
      Iterator sofas = ((CASImpl) MainFrame.this.cas).getBaseCAS().getSofaIterator();
      Feature sofaIdFeat = MainFrame.this.cas.getTypeSystem().getFeatureByFullName(
	  CommonCas.FEATURE_FULL_NAME_SOFAID);
      boolean nonDefaultSofaFound = false;
      while (sofas.hasNext()) {
        SofaFS sofa = (SofaFS) sofas.next();
        String sofaId = sofa.getStringValue(sofaIdFeat);
        if (!CommonCas.NAME_DEFAULT_SOFA.equals(sofaId)) {
          this.sofaSelectionComboBox.addItem(sofaId);
          nonDefaultSofaFound = true;
        }
      }
      this.disableSofaListener = false;
      this.sofaSelectionComboBox.setSelectedIndex(currentViewID);
      // make sofa selector visible if any text sofa other than the
      // default was found
      this.sofaSelectionPanel.setVisible(nonDefaultSofaFound);
    } catch (Exception e) {
      handleException(e);
    }
  }

  private void setLanguage() {
    if (this.language != null) {
      Feature langFeat = this.cas.getTypeSystem().getFeatureByFullName(
              TCAS.FEATURE_FULL_NAME_LANGUAGE);
      AnnotationFS doc = this.cas.getDocumentAnnotation();
      if (doc != null) {
        doc.setStringValue(langFeat, this.language);
      }
    }
  }

  // private void resetIRTree() {
  // DefaultMutableTreeNode root =
  // (DefaultMutableTreeNode) this.indexTree.getModel().getRoot();
  // root.setUserObject(noIndexReposLabel);
  // root.removeAllChildren();
  // }

  private void initIRTree() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(noIndexReposLabel);
    DefaultTreeModel model = new DefaultTreeModel(root);
    this.indexTree = new JTree(model);
    this.indexTree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
    // Only one node can be selected at any one time.
    this.indexTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    this.indexTree.addTreeSelectionListener(new IndexTreeSelectionListener());
    // No icons.
    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    // cellRenderer.setIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    this.indexTree.setCellRenderer(cellRenderer);
  }

  private void initFSTree() {
    FSTreeModel treeModel = new FSTreeModel();
    this.fsTree = new JTree(treeModel);
    this.fsTree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
    this.fsTree.setLargeModel(true);
    // Only one node can be selected at any one time.
    this.fsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    this.fsTree.addTreeSelectionListener(new FSTreeSelectionListener());
    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    // cellRenderer.setIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    this.fsTree.setCellRenderer(cellRenderer);
  }

  private void deleteFSTree() {
    ((FSTreeModel) this.fsTree.getModel()).reset();
  }

  protected void updateIndexTree(boolean useCAS) {
    deleteFSTree();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.indexTree.getModel().getRoot();
    if (useCAS) {
      root.setUserObject(indexReposRootLabel);
    } else {
      root.setUserObject(noIndexReposLabel);
    }
    root.removeAllChildren();
    if (this.cas != null && useCAS) {
      FSIndexRepository ir = this.cas.getIndexRepository();
      Iterator it = ir.getLabels();
      while (it.hasNext()) {
        String label = (String) it.next();
        FSIndex index1 = ir.getIndex(label);
        IndexTreeNode nodeObj = new IndexTreeNode(label, index1.getType(), index1.size());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeObj);
        root.add(node);
        node.add(createTypeTree(index1.getType(), this.cas.getTypeSystem(), label, ir));
      }
    }
    DefaultTreeModel model = (DefaultTreeModel) this.indexTree.getModel();
    // 1.3 workaround
    TreeModelListener[] listeners = org.apache.uima.tools.annot_view.ts_editor.MainFrame
            .getTreeModelListeners(model);
    // TreeModelListener[] listeners = model.getTreeModelListeners();
    // System.out.println("Number of tree model listeners: " +
    // listeners.length);
    Object[] path = new Object[1];
    path[0] = root;
    TreeModelEvent event = new TreeModelEvent(root, path);
    for (int i = 0; i < listeners.length; i++) {
      listeners[i].treeStructureChanged(event);
    }
  }

  private void updateFSTree(String indexName, FSIndex index1) {
    FSTreeModel treeModel = (FSTreeModel) this.fsTree.getModel();
    treeModel.update(indexName, index1, this.cas);
  }

  private ArrayList getAnnotationsAtPos(int pos, ArrayList annots) {
    ArrayList res = new ArrayList();
    FSNode annot;
    final int max = annots.size();
    for (int i = 0; i < max; i++) {
      annot = (FSNode) annots.get(i);
      if (annot.getStart() > pos) {
        break;
      }
      if (annot.getEnd() >= pos) {
        res.add(annot);
      }
    }
    return res;
  }

  private DefaultMutableTreeNode createTypeTree(Type type, TypeSystem ts, String label,
          FSIndexRepository ir) {
    int size = ir.getIndex(label, type).size();
    TypeTreeNode typeNode = new TypeTreeNode(type, label, size);
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(typeNode);
    List types = ts.getDirectSubtypes(type);
    final int max = types.size();
    for (int i = 0; i < max; i++) {
      DefaultMutableTreeNode child = createTypeTree((Type) types.get(i), ts, label, ir);
      node.add(child);
    }
    return node;
  }

  private void loadProgramPreferences() throws IOException {
    File home = new File(System.getProperty("user.home"));
    File prefFile = new File(home, "annotViewer.pref");
    if (prefFile.exists() && prefFile.isFile() && prefFile.canRead()) {
      FileInputStream in = new FileInputStream(prefFile);
      this.prefs = new Properties();
      this.prefs.load(in);
      String fileOpenDirName = this.prefs.getProperty(textDirPref);
      if (fileOpenDirName != null) {
        this.fileOpenDir = new File(fileOpenDirName);
      }
      String aeOpenDirName = this.prefs.getProperty(aeDirPref);
      if (aeOpenDirName != null) {
        this.annotOpenDir = new File(aeOpenDirName);
      }
      String xcasOpenDirName = this.prefs.getProperty(xcasDirPref);
      if (xcasOpenDirName != null) {
        this.xcasFileOpenDir = new File(xcasOpenDirName);
      }
      String colorFileName = this.prefs.getProperty(colorFilePref);
      if (colorFileName != null) {
        this.colorSettingFile = new File(colorFileName);
        try {
          loadColorPreferences(this.colorSettingFile);
        } catch (IOException e) {
          handleException(e);
        }
      }
      String colorDirName = this.prefs.getProperty(colorDirPref);
      if (colorDirName != null) {
        this.colorSettingsDir = new File(colorDirName);
      }
      this.codePage = this.prefs.getProperty(cpCurrentPref);
      this.codePagePrefsList = this.prefs.getProperty(cpListPref);
      this.language = this.prefs.getProperty(langCurrentPref);
      this.languagePrefsList = this.prefs.getProperty(langListPref);
      this.dataPathName = this.prefs.getProperty(dataPathPref);
    }
    if (this.prefs == null) {
      this.textScrollPane.setPreferredSize(textDimensionDefault);
      this.fsTree.setPreferredSize(fsTreeDimensionDefault);
    } else {
      setPreferredSize(this.textScrollPane, textSizePref);
      setPreferredSize(this.indexTreeScrollPane, indexTreeSizePref);
      setPreferredSize(this.fsTreeScrollPane, fsTreeSizePref);
    }
    if (this.prefs != null) {
      ArrayList list = stringToArrayList(this.prefs.getProperty(textFileListPref, ""));
      for (int i = 0; i < list.size(); i++) {
        this.textFileNameList.add(list.get(i));
        ++this.numRecentTextFiles;
      }
      list = stringToArrayList(this.prefs.getProperty(descFileListPref, ""));
      for (int i = 0; i < list.size(); i++) {
        this.descFileNameList.add(list.get(i));
        ++this.numRecentDescFiles;
      }
    }
    // System.out.println("Home dir: " + System.getProperty("user.home"));
    if (this.prefs == null) {
      this.prefs = new Properties();
    }
  }

  private void setPreferredSize(JComponent comp, String propPrefix) {
    // assert(comp != null);
    comp.setPreferredSize(getDimension(propPrefix));
  }

  private Dimension getDimension(String propPrefix) {
    if (this.prefs == null) {
      return null;
    }
    final String width = this.prefs.getProperty(propPrefix + widthSuffix);
    final String height = this.prefs.getProperty(propPrefix + heightSuffix);
    if ((height == null) || (width == null)) {
      return null;
    }
    double x = 0.0;
    double y = 0.0;
    try {
      x = Double.parseDouble(width);
      y = Double.parseDouble(height);
    } catch (NumberFormatException e) {
      handleException(e);
      return null;
    }
    Dimension d = new Dimension();
    d.setSize(x, y);
    return d;
  }

  private static final String arrayListToString(ArrayList list) {
    if (list.size() < 1) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    buf.append(list.get(0).toString());
    for (int i = 1; i < list.size(); i++) {
      buf.append(',');
      buf.append(list.get(i).toString());
    }
    return buf.toString();
  }

  private static final ArrayList stringToArrayList(String s) {
    ArrayList list = new ArrayList();
    if (s.length() > 0) {
      StringTokenizer tok = new StringTokenizer(s, ",");
      while (tok.hasMoreTokens()) {
        list.add(tok.nextToken());
      }
    }
    return list;
  }

  public void saveProgramPreferences() throws IOException {
    // File open dialog preferences.
    File home = new File(System.getProperty("user.home"));
    File prefFile = new File(home, "annotViewer.pref");
    if (this.prefs == null) {
      this.prefs = new Properties();
    }
    if (this.fileOpenDir != null) {
      this.prefs.setProperty(textDirPref, this.fileOpenDir.getAbsolutePath());
    }
    if (this.annotOpenDir != null) {
      this.prefs.setProperty(aeDirPref, this.annotOpenDir.getAbsolutePath());
    }
    if (this.xcasFileOpenDir != null) {
      this.prefs.setProperty(xcasDirPref, this.xcasFileOpenDir.getAbsolutePath());
    }
    // Window size preferences.
    Dimension d = this.textScrollPane.getSize();
    this.prefs.setProperty(textSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.prefs.setProperty(textSizePref + heightSuffix, Double.toString(d.getHeight()));
    d = this.indexTreeScrollPane.getSize();
    this.prefs.setProperty(indexTreeSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.prefs.setProperty(indexTreeSizePref + heightSuffix, Double.toString(d.getHeight()));
    d = this.fsTreeScrollPane.getSize();
    this.prefs.setProperty(fsTreeSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.prefs.setProperty(fsTreeSizePref + heightSuffix, Double.toString(d.getHeight()));
    if (this.dataPathName != null) {
      this.prefs.setProperty(dataPathPref, this.dataPathName);
    }
    if (this.colorSettingFile != null) {
      this.prefs.setProperty(colorFilePref, this.colorSettingFile.getAbsolutePath());
    }
    if (this.colorSettingsDir != null) {
      this.prefs.setProperty(colorDirPref, this.colorSettingsDir.getAbsolutePath());
    }
    if (this.codePage != null) {
      this.prefs.setProperty(cpCurrentPref, this.codePage);
    }
    if (this.codePages != null && this.codePages.size() > 0) {
      StringBuffer buf = new StringBuffer();
      buf.append((String) this.codePages.get(0));
      for (int i = 1; i < this.codePages.size(); i++) {
        buf.append(",");
        buf.append((String) this.codePages.get(i));
      }
      this.prefs.setProperty(cpListPref, buf.toString());
    }
    if (this.language != null) {
      this.prefs.setProperty(langCurrentPref, this.language);
    }
    if (this.languages != null && this.languages.size() > 0) {
      StringBuffer buf = new StringBuffer();
      buf.append((String) this.languages.get(0));
      for (int i = 1; i < this.languages.size(); i++) {
        buf.append(",");
        buf.append((String) this.languages.get(i));
      }
      this.prefs.setProperty(langListPref, buf.toString());
    }
    this.prefs.setProperty(textFileListPref, arrayListToString(this.textFileNameList));
    this.prefs.setProperty(descFileListPref, arrayListToString(this.descFileNameList));
    // Write out preferences to file.
    FileOutputStream out = new FileOutputStream(prefFile);
    this.prefs.store(out, "Automatically generated preferences file for Annotation Viewer");
  }

  private void saveColorPreferences(File file) throws IOException {
    Properties prefs1 = new Properties();
    Iterator it = this.styleMap.keySet().iterator();
    String type;
    Style style;
    Color fg, bg;
    while (it.hasNext()) {
      type = (String) it.next();
      style = (Style) this.styleMap.get(type);
      fg = StyleConstants.getForeground(style);
      bg = StyleConstants.getBackground(style);
      prefs1.setProperty(type, Integer.toString(fg.getRGB()) + "+" + Integer.toString(bg.getRGB()));
    }
    FileOutputStream out = new FileOutputStream(file);
    prefs1.store(out, "Color preferences for annotation viewer.");
  }

  private void loadColorPreferences(File file) throws IOException {
    Style parent = (Style) this.styleMap.get(TCAS.TYPE_NAME_ANNOTATION);
    StyleContext sc = StyleContext.getDefaultStyleContext();
    Properties prefs1 = new Properties();
    FileInputStream in = new FileInputStream(file);
    prefs1.load(in);
    String typeName, value;
    Style style;
    Color color;
    int pos;
    Iterator it = prefs1.keySet().iterator();
    while (it.hasNext()) {
      typeName = (String) it.next();
      value = prefs1.getProperty(typeName);
      style = sc.addStyle(typeName, parent);
      pos = value.indexOf('+');
      if (pos <= 0) {
        continue;
      }
      // Set foreground.
      color = new Color(Integer.parseInt(value.substring(0, pos)));
      StyleConstants.setForeground(style, color);
      // Set background.
      color = new Color(Integer.parseInt(value.substring(pos + 1, value.length())));
      StyleConstants.setBackground(style, color);
      this.styleMap.put(typeName, style);
    }
  }

  private void initKeyMap() {
    // Create a key map for focussing the index repository tree panel.
    Action focusIRAction = new FocusIRAction();
    String focusIRActionName = "focusIRAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK), focusIRActionName);
    getRootPane().getActionMap().put(focusIRActionName, focusIRAction);
    // Create a key map for focussing the FS tree panel.
    Action focusFSAction = new FocusFSAction();
    String focusFSActionName = "focusFSAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), focusFSActionName);
    getRootPane().getActionMap().put(focusFSActionName, focusFSAction);
    // Create a key map for focussing the text area.
    Action focusTextAction = new FocusTextAction();
    String focusTextActionName = "focusTextAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK), focusTextActionName);
    getRootPane().getActionMap().put(focusTextActionName, focusTextAction);
    // Create a key map for bringing up the text area context menu.
    Action textContextAction = new TextContextMenuAction();
    String textContextActionName = "textContextAction";
    this.textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_MASK), textContextActionName);
    this.textArea.getActionMap().put(textContextActionName, textContextAction);
  }

  private void showTextPopup(int x, int y) {
    final int pos = this.textArea.getCaretPosition();
    this.textPopup.removeAll();
    JMenuItem item = new JMenuItem("Position: " + pos);
    item.setEnabled(false);
    this.textPopup.add(item);
    FSNode posAnnot;
    if (this.isAnnotIndex) {
      ArrayList annots = ((FSTreeModel) this.fsTree.getModel()).getFSs();
      ArrayList selAnnots = getAnnotationsAtPos(pos, annots);
      for (int i = 0; i < selAnnots.size(); i++) {
        posAnnot = (FSNode) selAnnots.get(i);
        item = new JMenuItem("[" + posAnnot.getArrayPos() + "] = " + posAnnot.getType().getName());
        item.addActionListener(new PopupHandler(posAnnot.getArrayPos()));
        this.textPopup.add(item);
      }
    }
    this.textPopup.show(this.textArea, x, y);
  }

  // private void initFocusTraversalPolicy() {
  //    
  // FocusTraversalPolicy ftp = new FocusTraversalPolicy() {
  //
  // public Component getDefaultComponent(Container arg0)
  // {
  // return indexTree;
  // }
  //
  // public Component getFirstComponent(Container arg0)
  // {
  // return indexTree;
  // }
  //
  // public Component getLastComponent(Container arg0)
  // {
  // return fsTree;
  // }
  //
  // public Component getComponentAfter(Container arg0, Component comp)
  // {
  // if (comp == textArea) {
  // return fsTree;
  // } else if (comp == fsTree) {
  // return indexTree;
  // } else {
  // return textArea;
  // }
  // }
  //
  // public Component getComponentBefore(Container arg0, Component comp)
  // {
  // if (comp == textArea) {
  // return indexTree;
  // } else if (comp == fsTree) {
  // return textArea;
  // } else {
  // return fsTree;
  // }
  // }
  //      
  // };
  //    
  // this.setFocusTraversalPolicy(ftp);
  // }

}
