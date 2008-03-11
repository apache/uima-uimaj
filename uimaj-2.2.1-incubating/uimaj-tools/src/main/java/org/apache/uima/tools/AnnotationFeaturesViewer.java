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

package org.apache.uima.tools;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.resource.metadata.Capability;

/**
 * A tree view of Annotations and associated features.
 * 
 */
public class AnnotationFeaturesViewer extends JPanel implements ActionListener {

  private static final long serialVersionUID = -8028169177689821008L;

  public static final String ROOT = "Root";

  private JScrollPane scrollPane;

  private JTree tree;

  private JButton expandAllButton;

  private JButton collapseAllButton;

  public AnnotationFeaturesViewer() {

    super();

    setLayout(new BorderLayout());

    scrollPane = new JScrollPane();
    // We'll add a tree to this later through a call to populate.

    add(scrollPane, BorderLayout.CENTER);

    JPanel buttonsPanel = new JPanel();

    expandAllButton = new JButton("Expand All");
    expandAllButton.setToolTipText("Expand all tree nodes");
    expandAllButton.addActionListener(this);
    buttonsPanel.add(expandAllButton);

    collapseAllButton = new JButton("Collapse All");
    collapseAllButton.setToolTipText("Collapse all tree nodes");
    collapseAllButton.addActionListener(this);
    buttonsPanel.add(collapseAllButton);

    add(buttonsPanel, BorderLayout.NORTH);
  }

  public void populate(AnalysisEngine analysisEngine, AnalysisEngineMetaData aeMetaData) {
    tree = generateTreeView(analysisEngine, aeMetaData);

    tree.setDragEnabled(true); // To allow drag to stylemap table.
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true); // Displays node expansion glyphs.

    TreeSelectionModel selectionModel = tree.getSelectionModel();
    selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    tree.setCellRenderer(cellRenderer);

    scrollPane.getViewport().add(tree, null);
  }

  private JTree generateTreeView(AnalysisEngine analysisEngine, AnalysisEngineMetaData aeMetaData) {

    DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT);
    // We won't actually see this.

    ArrayList annotationTypes = new ArrayList();
    Capability[] capabilities = aeMetaData.getCapabilities();
    for (int i = 0; i < capabilities.length; i++) {
      TypeOrFeature[] outputs = capabilities[i].getOutputs();

      for (int j = 0; j < outputs.length; j++) {
        if (outputs[j].isType() && !annotationTypes.contains(outputs[j].getName())) {
          annotationTypes.add(outputs[j].getName());
        }
      }
    }

    Iterator it = annotationTypes.iterator();
    while (it.hasNext()) {
      String annotationTypeName = (String) it.next();

      DefaultMutableTreeNode annotationTreeNode = new DefaultMutableTreeNode(annotationTypeName);
      String[] featureNames = analysisEngine.getFeatureNamesForType(annotationTypeName);

      for (int i = 0; i < featureNames.length; i++) {
        DefaultMutableTreeNode featureTreeNode = new DefaultMutableTreeNode(featureNames[i]);
        annotationTreeNode.add(featureTreeNode);
      }

      root.add(annotationTreeNode);
    }

    return new JTree(root);
  }

  public String getSelection() {
    TreePath treePath = tree.getSelectionPath();
    if (treePath != null) {
      String parentPath = treePath.getParentPath().getLastPathComponent().toString();
      String lastPath = treePath.getLastPathComponent().toString();
      if (parentPath.equals(AnnotationFeaturesViewer.ROOT))
        return lastPath;
      else
        return parentPath + ":" + lastPath;
    } else
      return null;
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == expandAllButton) {
      for (int i = 0; i < tree.getRowCount(); i++)
        tree.expandRow(i);
    } else if (source == collapseAllButton) {
      for (int i = 0; i < tree.getRowCount(); i++)
        tree.collapseRow(i);
    }
  }
}
