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

package org.apache.uima.tools.cvd.tsview;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.util.EventListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeSelectionModel;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;


/**
 * Insert comment for enclosing_type here.
 * 
 * 
 */
public class MainFrame extends JFrame {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 5606886216212480040L;

  /**
   * The listener interface for receiving typeTreeSelection events.
   * The class that is interested in processing a typeTreeSelection
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addTypeTreeSelectionListener<code> method. When
   * the typeTreeSelection event occurs, that object's appropriate
   * method is invoked.
   *
   * @see TypeTreeSelectionEvent
   */
  private class TypeTreeSelectionListener implements TreeSelectionListener {
    
    /**
     * Value changed.
     *
     * @param event the event
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    @Override
    public void valueChanged(TreeSelectionEvent event) {
      // System.out.println("");
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) MainFrame.this.typeTree
              .getLastSelectedPathComponent();
      if (node == null) {
        return;
      }
      // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
      org.apache.uima.cas.Type t = (org.apache.uima.cas.Type) node.getUserObject();
      if (t != null) {
        MainFrame.this.selectedType = t;
        updateFeatureTable();
      }
    }
  }

  /** The ts. */
  TypeSystem ts = null;

  /** The selected type. */
  // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
  private org.apache.uima.cas.Type selectedType;

  /** The feature table. */
  private JTable featureTable = null;

  /** The type tree. */
  private JTree typeTree = null;

  /**
   * Constructor for MainFrame.
   * 
   * @throws HeadlessException -
   */
  public MainFrame() {
    super();
    init();
  }

  /**
   * Constructor for MainFrame.
   *
   * @param gc the gc
   */
  public MainFrame(GraphicsConfiguration gc) {
    super(gc);
    init();
  }

  /**
   * Constructor for MainFrame.
   *
   * @param title the title
   * @throws HeadlessException -
   */
  public MainFrame(String title) {
    super(title);
    init();
  }

  /**
   * Constructor for MainFrame.
   *
   * @param title the title
   * @param gc the gc
   */
  public MainFrame(String title, GraphicsConfiguration gc) {
    super(title, gc);
    init();
  }

  /**
   * Inits the.
   */
  private void init() {
    // Set the title.
    this.setTitle("Type System Editor");
    JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    this.setContentPane(contentPane);

    // Set up the type tree. Use simple DefaultTreeModel.
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("<html><b>No CAS!</b></html>");
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    this.typeTree = new JTree(treeModel);
    this.typeTree.addTreeSelectionListener(new TypeTreeSelectionListener());
    TreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
    treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    this.typeTree.setSelectionModel(treeSelectionModel);
    JScrollPane treePane = new JScrollPane(this.typeTree);
    contentPane.setLeftComponent(treePane);
    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    // cellRenderer.setIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    this.typeTree.setCellRenderer(cellRenderer);

    // Set up the feature table.
    this.featureTable = new JTable(new FeatureTableModel());
    JScrollPane featurePane = new JScrollPane(this.featureTable);
    featurePane.getViewport().setBackground(Color.WHITE);
    contentPane.setRightComponent(featurePane);

    this.setJMenuBar(createMenuBar());
  }

  /**
   * Show annot feats.
   */
  public void showAnnotFeats() {
    // Debug...
    this.selectedType = this.ts.getType(CAS.TYPE_NAME_ANNOTATION);
    updateFeatureTable();
  }

  /**
   * Creates the menu bar.
   *
   * @return the j menu bar
   */
  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    JMenu optionsMenu = new JMenu("Options");
    JMenuItem showInheritedFeatsItem = new JMenuItem("Show Inherited Features");
    optionsMenu.add(showInheritedFeatsItem);
    menuBar.add(optionsMenu);
    return menuBar;
  }

  /**
   * Update feature table.
   */
  private void updateFeatureTable() {
    if (this.selectedType == null) {
      return;
    }
    ((FeatureTableModel) this.featureTable.getModel()).setType(this.selectedType);
  }

  /**
   * Creates the type tree.
   *
   * @param type the type
   * @return the default mutable tree node
   */
  private DefaultMutableTreeNode createTypeTree(org.apache.uima.cas.Type type) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(type);
    // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
    // also on method parameter "type"
    List<org.apache.uima.cas.Type> types = this.ts.getDirectSubtypes(type);
    final int max = types.size();
    for (int i = 0; i < max; i++) {
      DefaultMutableTreeNode child = createTypeTree(types.get(i));
      node.add(child);
    }
    return node;
  }

  /**
   * Update type tree.
   */
  private void updateTypeTree() {
    if (this.ts == null) {
      return;
    }
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) this.typeTree.getModel().getRoot();
    rootNode.removeAllChildren();
    // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
    org.apache.uima.cas.Type top = this.ts.getTopType();
    rootNode.setUserObject(top);
    List<org.apache.uima.cas.Type> types = this.ts.getDirectSubtypes(top);
    for (int i = 0; i < types.size(); i++) {
      rootNode.add(createTypeTree(types.get(i)));
    }
    DefaultTreeModel model = (DefaultTreeModel) this.typeTree.getModel();
    // 1.3 compatability hack.
    // TreeModelListener[] listeners = model.getTreeModelListeners();
    TreeModelListener[] listeners = getTreeModelListeners(model);
    // System.out.println("Number of tree model listeners: " +
    // listeners.length);
    Object[] path = new Object[1];
    path[0] = rootNode;
    TreeModelEvent event = new TreeModelEvent(rootNode, path);
    for (int i = 0; i < listeners.length; i++) {
      listeners[i].treeStructureChanged(event);
    }
  }

  /**
   * Gets the tree model listeners.
   *
   * @param model the model
   * @return the tree model listeners
   */
  public static TreeModelListener[] getTreeModelListeners(DefaultTreeModel model) {
    EventListener[] eventListeners = model.getListeners(TreeModelListener.class);
    TreeModelListener[] modelListeners = new TreeModelListener[eventListeners.length];
    for (int i = 0; i < modelListeners.length; i++) {
      modelListeners[i] = (TreeModelListener) eventListeners[i];
    }
    return modelListeners;
  }

  /**
   * Sets the type system.
   *
   * @param ts the new type system
   */
  public void setTypeSystem(TypeSystem ts) {
    this.ts = ts;
    updateTypeTree();
  }

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main(String[] args) {

    try {
      // Set the look&feel
      // UIManager.setLookAndFeel(
      // "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

      MainFrame frame = new MainFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      CASMgr casMgr = CASFactory.createCAS();
      frame.setTypeSystem(casMgr.getTypeSystemMgr());

      // frame.showAnnotFeats();

      frame.pack();
      frame.setVisible(true);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
