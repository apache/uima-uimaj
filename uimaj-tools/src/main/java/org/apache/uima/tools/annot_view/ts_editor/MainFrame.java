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

package org.apache.uima.tools.annot_view.ts_editor;

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

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.text.TCAS;

/**
 * Insert comment for enclosing_type here.
 * 
 * 
 */
public class MainFrame extends JFrame {

    private class TypeTreeSelectionListener implements TreeSelectionListener {
        /**
         * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
         */
        public void valueChanged(TreeSelectionEvent event) {
            // System.out.println("");
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) MainFrame.this.typeTree
                    .getLastSelectedPathComponent();
            if (node == null) {
                return;
            }
            Type t = (Type) node.getUserObject();
            if (t != null) {
                MainFrame.this.selectedType = t;
                updateFeatureTable();
            }
        }
    }

    TypeSystem ts = null;

    private Type selectedType;

    private JTable featureTable = null;

    private JTree typeTree = null;

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
     * @param gc
     */
    public MainFrame(GraphicsConfiguration gc) {
        super(gc);
        init();
    }

    /**
     * Constructor for MainFrame.
     * 
     * @param title
     * @throws HeadlessException
     */
    public MainFrame(String title) {
        super(title);
        init();
    }

    /**
     * Constructor for MainFrame.
     * 
     * @param title
     * @param gc
     */
    public MainFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        init();
    }

    private void init() {
        // Set the title.
        this.setTitle("Type System Editor");
        JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.setContentPane(contentPane);

        // Set up the type tree. Use simple DefaultTreeModel.
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                "<html><b>No CAS!</b></html>");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        this.typeTree = new JTree(treeModel);
        this.typeTree.addTreeSelectionListener(new TypeTreeSelectionListener());
        TreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
        treeSelectionModel
                .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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

    public void showAnnotFeats() {
        // Debug...
        this.selectedType = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
        updateFeatureTable();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem showInheritedFeatsItem = new JMenuItem(
                "Show Inherited Features");
        optionsMenu.add(showInheritedFeatsItem);
        menuBar.add(optionsMenu);
        return menuBar;
    }

    private void updateFeatureTable() {
        if (this.selectedType == null) {
            return;
        }
        ((FeatureTableModel) this.featureTable.getModel())
                .setType(this.selectedType);
    }

    private DefaultMutableTreeNode createTypeTree(Type type) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(type);
        List types = this.ts.getDirectSubtypes(type);
        final int max = types.size();
        for (int i = 0; i < max; i++) {
            DefaultMutableTreeNode child = createTypeTree((Type) types.get(i));
            node.add(child);
        }
        return node;
    }

    private void updateTypeTree() {
        if (this.ts == null) {
            return;
        }
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) this.typeTree
                .getModel().getRoot();
        rootNode.removeAllChildren();
        Type top = this.ts.getTopType();
        rootNode.setUserObject(top);
        List types = this.ts.getDirectSubtypes(top);
        for (int i = 0; i < types.size(); i++) {
            rootNode.add(createTypeTree((Type) types.get(i)));
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

    public static TreeModelListener[] getTreeModelListeners(
            DefaultTreeModel model) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Class tmlClass = null;
        try {
            tmlClass = cl.loadClass("javax.swing.event.TreeModelListener");
        } catch (ClassNotFoundException e) {
            return new TreeModelListener[0];
        }
        EventListener[] eventListeners = model.getListeners(tmlClass);
        TreeModelListener[] modelListeners = new TreeModelListener[eventListeners.length];
        for (int i = 0; i < modelListeners.length; i++) {
            modelListeners[i] = (TreeModelListener) eventListeners[i];
        }
        return modelListeners;
    }

    public void setTypeSystem(TypeSystem ts) {
        this.ts = ts;
        updateTypeTree();
    }

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
            frame.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
