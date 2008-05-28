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

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;

/**
 * Insert comment for enclosing_type here.
 * 
 * 
 */
public class FSTreeModel implements TreeModel {

  private FSTreeNode root;

  private CASImpl cas;

  private List<TreeModelListener> treeModelListeners = new ArrayList<TreeModelListener>();

  private List<FSNode> fss;

  private static final String defaultRootString = "<html><b>" + MainFrame.htmlGrayColor
          + "FS List - no selection</b></html>";

  private String rootString = defaultRootString;

  /**
   * Constructor for FSTreeModel.
   */
  public FSTreeModel() {
    super();
    this.root = new FSNode(this, FSNode.DISPLAY_NODE, 0, null);
    this.root.setChildren(new ArrayList<FSTreeNode>());
  }

  public void update(String indexName, FSIndex index, CAS cas1) {
    // this.indexName = indexName;
    this.cas = (CASImpl) cas1;
    final int size = index.size();
    this.rootString = "<html><font color=green>" + indexName + "</font> - <font color=blue>"
            + index.getType().getName() + "</font> [" + size + "]</html>";
    this.root = new FSNode(this, FSNode.DISPLAY_NODE, 0, null);
    this.fss = new ArrayList<FSNode>();
    FSIterator it = index.iterator();
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      FeatureStructure fs = it.get();
      this.fss.add(new FSNode(this, getNodeType(fs.getType()), fs.hashCode(), count));
      ++count;
    }
    List<FSTreeNode> kids = createArrayChildren(0, size, this.fss, this);
    this.root.setChildren(kids);
    Object[] path = new Object[1];
    path[0] = this.root;
    TreeModelEvent event = new TreeModelEvent(this.root, path);
    for (int i = 0; i < this.treeModelListeners.size(); i++) {
      this.treeModelListeners.get(i).treeStructureChanged(event);
    }
  }

  public void reset() {
    this.root.removeAllChildren();
    this.rootString = defaultRootString;
    Object[] path = new Object[1];
    path[0] = this.root;
    TreeModelEvent event = new TreeModelEvent(this.root, path);
    for (int i = 0; i < this.treeModelListeners.size(); i++) {
      this.treeModelListeners.get(i).treeStructureChanged(event);
    }

  }

  public List<FSNode> getFSs() {
    return this.fss;
  }

  /**
   * @see javax.swing.tree.TreeModel#getRoot()
   */
  public Object getRoot() {
    return this.root;
  }

  /**
   * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
   */
  public Object getChild(Object parent, int index) {
    FSTreeNode node = (FSTreeNode) parent;
    node.initChildren();
    return node.getChildren().get(index);
  }

  int getNodeType(int addr, Feature feat) {
    Type domType = feat.getRange();
    if (this.cas.isStringType(domType)) {
      return FSNode.STRING_FS;
    } else if (this.cas.isIntType(domType)) {
      return FSNode.INT_FS;
    } else if (this.cas.isFloatType(domType)) {
      return FSNode.FLOAT_FS;
    } else if (this.cas.isByteType(domType)) {
      return FSNode.BYTE_FS;
    } else if (this.cas.isBooleanType(domType)) {
      return FSNode.BOOL_FS;
    } else if (this.cas.isShortType(domType)) {
      return FSNode.SHORT_FS;
    } else if (this.cas.isLongType(domType)) {
      return FSNode.LONG_FS;
    } else if (this.cas.isDoubleType(domType)) {
      return FSNode.DOUBLE_FS;
    } else if (this.cas.isAbstractArrayType(domType)) {
      final int featAddr = this.cas.getFeatureValue(addr, ((FeatureImpl) feat).getCode());
      if (this.cas.isArrayType(this.cas.getTypeSystemImpl()
              .ll_getTypeForCode(this.cas.getHeapValue(featAddr)))) {
        return FSNode.ARRAY_FS;
      }
    }
    return FSNode.STD_FS;
  }

  int getNodeType(Type type) {
    if (this.cas.isStringType(type)) {
      return FSNode.STRING_FS;
    } else if (this.cas.isIntType(type)) {
      return FSNode.INT_FS;
    } else if (this.cas.isFloatType(type)) {
      return FSNode.FLOAT_FS;
    } else if (this.cas.isArrayType(type)) {
      return FSNode.ARRAY_FS;
    } else if (this.cas.isByteArrayType(type)) {
      return FSNode.BYTE_FS;
    } else if (this.cas.isBooleanArrayType(type)) {
      return FSNode.BOOL_FS;
    } else if (this.cas.isShortArrayType(type)) {
      return FSNode.SHORT_FS;
    } else if (this.cas.isLongArrayType(type)) {
      return FSNode.LONG_FS;
    } else if (this.cas.isDoubleArrayType(type)) {
      return FSNode.DOUBLE_FS;
    }
    return FSNode.STD_FS;
  }

  /**
   * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
   */
  public int getChildCount(Object parent) {
    return ((FSTreeNode) parent).getChildCount();
  }

  /**
   * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
   */
  public boolean isLeaf(Object node) {
    return (getChildCount(node) == 0);
  }

  /**
   * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath,
   *      java.lang.Object)
   */
  public void valueForPathChanged(TreePath path, Object newValue) {
    // Does nothing.
  }

  /**
   * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
   */
  public int getIndexOfChild(Object parent, Object child) {
    FSTreeNode node = (FSTreeNode) parent;
    node.initChildren();
    return node.getChildren().indexOf(child);
  }

  /**
   * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
   */
  public void addTreeModelListener(TreeModelListener arg0) {
    this.treeModelListeners.add(arg0);
  }

  /**
   * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
   */
  public void removeTreeModelListener(TreeModelListener arg0) {
    this.treeModelListeners.remove(arg0);
  }

  /**
   * @return CASImpl
   */
  CASImpl getCas() {
    return this.cas;
  }

  /**
   * @return String
   */
  String getRootString() {
    return this.rootString;
  }

  static List<FSTreeNode> createArrayChildren(int start, int end, List<FSNode> array, FSTreeModel model) {
    ArrayList<FSTreeNode> kids = new ArrayList<FSTreeNode>();
    final int size = end - start;
    if (size <= ArrayNode.CUTOFF) {
      kids.ensureCapacity(size);
      for (int i = start; i < end; i++) {
        kids.add(array.get(i));
      }
    } else {
      final int deg = ArrayNode.degree(size);
      final int divisor = (int) Math.pow(ArrayNode.MULT, deg);
      final int buckets = size / divisor;
      final int rem = size % divisor;
      int start_i, end_i;
      for (int i = 0; i < buckets; i++) {
        start_i = (start + (i * divisor));
        end_i = start_i + divisor;
        ArrayNode node = new ArrayNode(start_i, end_i - 1);
        List<FSTreeNode> grandkids = createArrayChildren(start_i, end_i, array, model);
        node.setChildren(grandkids);
        kids.add(node);
      }
      if (rem > 0) {
        final int start_rem = start + (buckets * divisor);
        if (rem <= ArrayNode.CUTOFF) {
          for (int i = start_rem; i < end; i++) {
            kids.add(array.get(i));
          }
        } else {
          ArrayNode node = new ArrayNode(start_rem, end - 1);
          node.setChildren(createArrayChildren(start_rem, end, array, model));
          kids.add(node);
        }
      }
    }
    return kids;
  }

  public TreePath pathToNode(int fsNum) {
    List<FSTreeNode> p = new ArrayList<FSTreeNode>();
    p.add(this.root);
    getPathToNode(fsNum, this.root.getChildren(), p);
    TreePath path = new TreePath(p.toArray());
    return path;
  }

  private void getPathToNode(int n, List<FSTreeNode> dtrs, List<FSTreeNode> path) {
    // Do a linear search. The branching factor is small, so this should not
    // be a problem.
    FSTreeNode node;
    ArrayNode arrayNode;
    FSNode fsNode;
    for (int i = 0; i < dtrs.size(); i++) {
      node = dtrs.get(i);
      if (node instanceof ArrayNode) {
        arrayNode = (ArrayNode) node;
        if (arrayNode.getEnd() >= n) {
          path.add(node);
          getPathToNode(n, node.getChildren(), path);
          return;
        }
      } else {
        fsNode = (FSNode) node;
        if (fsNode.getArrayPos() == n) {
          path.add(node);
          return;
        }
      }
    }
  }

}
