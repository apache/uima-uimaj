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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeImpl_string;
import org.apache.uima.cas.impl.TypeSystemConstants;
import org.apache.uima.jcas.cas.TOP;


/**
 * Swing Tree Model for Feature Structures.
 */

public class FSTreeModel implements TreeModel {

  /** The root. */
  private FSTreeNode root;

  /** The cas. */
  private CASImpl cas;

  /** The tree model listeners. */
  private List<TreeModelListener> treeModelListeners = new ArrayList<>();

  /** The fss. */
  private List<FSNode> fss;

  /** The Constant defaultRootString. */
  private static final String defaultRootString = "<html><b>" + MainFrame.htmlGrayColor
          + "FS List - no selection</b></html>";

  /** The root string. */
  private String rootString = defaultRootString;

  /**
   * Constructor for FSTreeModel.
   */
  public FSTreeModel() {
    this.root = new FSNode(this, FSNode.DISPLAY_NODE, null, 0, null);
    this.root.setChildren(new ArrayList<>());
  }

  /**
   * Update.
   *
   * @param indexName the index name
   * @param index the index
   * @param cas1 the cas 1
   */
  public void update(String indexName, FSIndex index, CAS cas1) {
    // this.indexName = indexName;
    this.cas = (CASImpl) cas1;
    final int size = index.size();
    this.rootString = "<html><font color=green>" + indexName + "</font> - <font color=blue>"
            + index.getType().getName() + "</font> [" + size + "]</html>";
    this.root = new FSNode(this, FSNode.DISPLAY_NODE, null, 0, null);
    this.fss = new ArrayList<>();
    FSIterator<TOP> it = index.iterator();
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      TOP fs = it.get();
      this.fss.add(new FSNode(this, getNodeType(fs.getType()), fs, fs._id(), count));
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

  /**
   * Reset.
   */
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

  /**
   * Gets the f ss.
   *
   * @return the f ss
   */
  public List<FSNode> getFSs() {
    return this.fss;
  }

  /**
   * Gets the root.
   *
   * @return the root
   * @see javax.swing.tree.TreeModel#getRoot()
   */
  @Override
  public Object getRoot() {
    return this.root;
  }

  /**
   * Gets the child.
   *
   * @param parent the parent
   * @param index the index
   * @return the child
   * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
   */
  @Override
  public Object getChild(Object parent, int index) {
    FSTreeNode node = (FSTreeNode) parent;
    node.initChildren();
    return node.getChildren().get(index);
  }

//  int getNodeType(int addr, Feature feat) {
//    Type domType = feat.getRange();
//    if (this.cas.isStringType(domType)) {
//      return FSNode.STRING_FS;
//    } else if (this.cas.isIntType(domType)) {
//      return FSNode.INT_FS;
//    } else if (this.cas.isFloatType(domType)) {
//      return FSNode.FLOAT_FS;
//    } else if (this.cas.isByteType(domType)) {
//      return FSNode.BYTE_FS;
//    } else if (this.cas.isBooleanType(domType)) {
//      return FSNode.BOOL_FS;
//    } else if (this.cas.isShortType(domType)) {
//      return FSNode.SHORT_FS;
//    } else if (this.cas.isLongType(domType)) {
//      return FSNode.LONG_FS;
//    } else if (this.cas.isDoubleType(domType)) {
//      return FSNode.DOUBLE_FS;
//    } else if (this.cas.isArrayOfFsType(domType)) {
//      final int featAddr = this.cas.getFeatureValue(addr, ((FeatureImpl) feat).getCode());
//      if (this.cas.isArrayType(this.cas.getTypeSystemImpl()
//              .ll_getTypeForCode(this.cas.getHeapValue(featAddr)))) {
//        return FSNode.ARRAY_FS;
//      }
//    }
//    return FSNode.STD_FS;
//  }

  /**
 * Gets the node type.
 *
 * @param type the type
 * @return the node type
 */
int getNodeType(Type type) {
    if (type instanceof TypeImpl_string) {
      return FSNode.STRING_FS;
    } else {
      switch(((TypeImpl)type).getCode()) {
      case TypeSystemConstants.intTypeCode: return FSNode.INT_FS;
      case TypeSystemConstants.floatTypeCode: return FSNode.FLOAT_FS;
      case TypeSystemConstants.fsArrayTypeCode: return FSNode.ARRAY_FS;
      case TypeSystemConstants.byteArrayTypeCode: return FSNode.BYTE_FS;
      case TypeSystemConstants.booleanArrayTypeCode: return FSNode.BOOL_FS;
      case TypeSystemConstants.shortArrayTypeCode: return FSNode.SHORT_FS;
      case TypeSystemConstants.longArrayTypeCode: return FSNode.LONG_FS;
      case TypeSystemConstants.doubleArrayTypeCode: return FSNode.DOUBLE_FS;
      default: return FSNode.STD_FS;
      }
    }
  }

  /**
   * Gets the child count.
   *
   * @param parent the parent
   * @return the child count
   * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
   */
  @Override
  public int getChildCount(Object parent) {
    return ((FSTreeNode) parent).getChildCount();
  }

  /**
   * Checks if is leaf.
   *
   * @param node the node
   * @return true, if is leaf
   * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
   */
  @Override
  public boolean isLeaf(Object node) {
    return (getChildCount(node) == 0);
  }

  /**
   * Value for path changed.
   *
   * @param path the path
   * @param newValue the new value
   * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath,
   *      java.lang.Object)
   */
  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // Does nothing.
  }

  /**
   * Gets the index of child.
   *
   * @param parent the parent
   * @param child the child
   * @return the index of child
   * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
   */
  @Override
  public int getIndexOfChild(Object parent, Object child) {
    FSTreeNode node = (FSTreeNode) parent;
    node.initChildren();
    return node.getChildren().indexOf(child);
  }

  /**
   * Adds the tree model listener.
   *
   * @param arg0 the arg 0
   * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
   */
  @Override
  public void addTreeModelListener(TreeModelListener arg0) {
    this.treeModelListeners.add(arg0);
  }

  /**
   * Removes the tree model listener.
   *
   * @param arg0 the arg 0
   * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
   */
  @Override
  public void removeTreeModelListener(TreeModelListener arg0) {
    this.treeModelListeners.remove(arg0);
  }

  /**
   * Gets the cas.
   *
   * @return CASImpl
   */
  CASImpl getCas() {
    return this.cas;
  }

  /**
   * Gets the root string.
   *
   * @return String
   */
  String getRootString() {
    return this.rootString;
  }

  /**
   * Creates the array children.
   *
   * @param start the start
   * @param end the end
   * @param array the array
   * @param model the model
   * @return the list
   */
  static List<FSTreeNode> createArrayChildren(int start, int end, List<FSNode> array, FSTreeModel model) {
    ArrayList<FSTreeNode> kids = new ArrayList<>();
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

  /**
   * Path to node.
   *
   * @param fsNum the fs num
   * @return the tree path
   */
  public TreePath pathToNode(int fsNum) {
    List<FSTreeNode> p = new ArrayList<>();
    p.add(this.root);
    getPathToNode(fsNum, this.root.getChildren(), p);
    TreePath path = new TreePath(p.toArray());
    return path;
  }

  /**
   * Gets the path to node.
   *
   * @param n the n
   * @param dtrs the dtrs
   * @param path the path
   * @return the path to node
   */
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
