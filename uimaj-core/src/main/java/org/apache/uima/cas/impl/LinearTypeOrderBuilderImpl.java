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

package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.internal.util.GraphNode;

/**
 * Implementation of the <code>LinearTypeOrderBuilder</code> interface.
 * 
 * 
 */

public class LinearTypeOrderBuilderImpl implements LinearTypeOrderBuilder {

  /**
   * An implementation of the {@link LinearTypeOrder LinearTypeOrder} interface.
   * 
   * 
   */
  public static class TotalTypeOrder implements LinearTypeOrder {

    // The explicit order. We keep this since we need to return it. It would
    // be awkward and inefficient to compute it from lt.
    // index = orderNumber, value = type-code
    // used by serialization routines
    private final int[] order;

    // index= typeCode, value = order number
    private final short[] typeCodeToOrder;

    private boolean hashCodeComputed = false;

    private int computedHashCode;

    private final boolean isEmptyTypeOrder;

    private TotalTypeOrder(String[] typeList, TypeSystem ts, boolean isEmpty) throws CASException {
      this(encodeTypeList(typeList, ts), ts, isEmpty);
    }

    private static int[] encodeTypeList(String[] typeList, TypeSystem ts) throws CASException {
      int[] a = new int[typeList.length];
      LowLevelTypeSystem llts = (LowLevelTypeSystem) ts;
      for (int i = 0; i < a.length; i++) {
        int t = llts.ll_getCodeForTypeName(typeList[i]);
        if (t == LowLevelTypeSystem.UNKNOWN_TYPE_CODE) {
          throw new CASException(CASException.TYPEORDER_UNKNOWN_TYPE, typeList[i]);
        }
        a[i] = t;
      }
      return a;
    }

    /**
     * The constructor for the total type order, called by the other constructor and also when doing
     * a cas complete deserialization, or just deserializing the type system/index defs Create the
     * order from an array of type codes in ascending order.
     * 
     * @param typeList
     *          the list of ordered types
     * @param ts
     *          the type system
     */
    private TotalTypeOrder(int[] typeList, TypeSystem ts, boolean isEmpty) {
      TypeSystemImpl tsi = (TypeSystemImpl) ts;
      order = typeList;
      final int sz = order.length + tsi.getSmallestType();
      if (sz > 32767) {
        /** Total number of UIMA types, {0}, exceeds the maximum of 32766. **/
        throw new CASAdminException(CASAdminException.TOO_MANY_TYPES, sz - 1);
      }
      typeCodeToOrder = new short[order.length + tsi.getSmallestType()];
      for (int i = 0; i < order.length; i++) {
        typeCodeToOrder[order[i]] = (short) i;
      }
      isEmptyTypeOrder = isEmpty;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.admin.LinearTypeOrder#compare(org.apache.uima.cas.impl.
     * FeatureStructureImplC, org.apache.uima.cas.impl.FeatureStructureImplC)
     */
    @Override
    public int compare(FeatureStructure fs1, FeatureStructure fs2) {
      TypeImpl t1 = ((FeatureStructureImplC) fs1)._getTypeImpl();
      TypeImpl t2 = ((FeatureStructureImplC) fs2)._getTypeImpl();
      if (t1 == t2)
        return 0;
      return Short.compare(typeCodeToOrder[t1.getCode()], typeCodeToOrder[t2.getCode()]);
    }

    // Look-up.
    @Override
    public boolean lessThan(Type t1, Type t2) {
      return lessThan(((TypeImpl) t1).getCode(), ((TypeImpl) t2).getCode());
      // return this.lt[((TypeImpl) t1).getCode()].get(((TypeImpl) t2)
      // .getCode());
    }

    @Override
    public boolean lessThan(int t1, int t2) {
      return typeCodeToOrder[t1] < typeCodeToOrder[t2];
      // return this.lt[t1].get(t2);
    }

    @Override
    public int[] getOrder() {
      return order;
    }

    @Override
    public int hashCode() {
      if (!hashCodeComputed) {
        computedHashCode = Arrays.hashCode(order);
        hashCodeComputed = true;
      }
      return computedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || (getClass() != obj.getClass())) {
        return false;
      }
      TotalTypeOrder other = (TotalTypeOrder) obj;
      if (hashCode() != other.hashCode()) {
        return false;
      }
      if (!Arrays.equals(order, other.order)) {
        return false;
      }
      return true;
    }

    @Override
    public boolean isEmptyTypeOrder() {
      return isEmptyTypeOrder;
    }

  }

  private class Node extends GraphNode {

    private Node(Object o) {
      super(o);
    }

    private void removeAncestor(Node node) {
      predecessors.remove(node);
    }

    private int outRank() {
      return getNbrSucc();
    }

    private int inRank() {
      return getNbrPred();
    }

    private ArrayList<GraphNode> getAllPredecessors() {
      return predecessors;
    }

    private ArrayList<GraphNode> getAllSuccessors() {
      return successors;
    }

    private void removeSuccessor(int i) {
      successors.remove(i);
    }

    private void addAllPredecessors(ArrayList<? extends GraphNode> pred) {
      for (Iterator<? extends GraphNode> it = pred.iterator(); it.hasNext();) {
        Node n = (Node) it.next();
        if (!order.pathFromTo(this, n)) {
          n.connect(this);
        }
        // else {
        // System.out.println("Testing: add predec: found loop from " +
        // this.getElement() + " to " +
        // n.getElement());
        // }
      }
    }

    private void addAllSuccessors(ArrayList<? extends GraphNode> successors1) {
      for (Iterator<? extends GraphNode> it = successors1.iterator(); it.hasNext();) {
        Node n = (Node) it.next();
        if (!order.pathFromTo(n, this)) {
          connect(n);
        }
        // else {
        // System.out.println("Testing: add succ: found loop from " +
        // this.getElement() + " to " +
        // n.getElement());
        // }
      }
    }
  }

  private class Graph {

    // Map type names to graph nodes.
    private final Map<String, Node> nodeMap = new HashMap<>();

    private int size() {
      return nodeMap.size();
    }

    private Node getNode(String name) {
      Node node = nodeMap.get(name);
      if (node == null) {
        node = new Node(name);
        nodeMap.put(name, node);
      }
      return node;
    }

    private Graph copy(Node inRank0nodes) {
      Graph copy = new Graph();
      Iterator<Map.Entry<String, Node>> it = nodeMap.entrySet().iterator();
      Map.Entry<String, Node> entry;
      String key;
      // Copy the nodes.
      while (it.hasNext()) {
        entry = it.next();
        key = entry.getKey();
        copy.nodeMap.put(key, copy.getNode(key)); // getNode makes a new
                                                  // node
      }
      // Set pred's and succ's for nodes.
      it = nodeMap.entrySet().iterator();
      Node origNode, copyNode;
      while (it.hasNext()) {
        entry = it.next();
        key = entry.getKey();
        origNode = entry.getValue();
        copyNode = copy.nodeMap.get(key);
        for (int i = 0; i < origNode.inRank(); i++) {
          key = (String) origNode.getPredecessor(i).getElement();
          copyNode.addPredecessor(copy.getNode(key));
        }
        for (int i = 0; i < origNode.outRank(); i++) {
          key = (String) origNode.getSuccessor(i).getElement();
          copyNode.addSuccessor(copy.getNode(key));
        }
        if (origNode.inRank() == 0) {
          inRank0nodes.addSuccessor(origNode);
        }
      }
      return copy;
    }

    private ArrayList<Node> removeNode(Node node) {
      // Removing a node means removing it from the map (set of nodes) as
      // well
      // as removing outgoing references. Since we only remove nodes with
      // an
      // in-degree of 0, we don't need to worry about in-arcs.
      // Node node = (Node) this.nodeMap.get(name);
      ArrayList<Node> rank0s = new ArrayList<>();
      // if (node == null) {
      // return ;
      // }
      nodeMap.remove(node.getElement());
      final int max = node.outRank();
      for (int i = 0; i < max; i++) {
        Node n = (Node) node.getSuccessor(i);
        n.removeAncestor(node);
        if (n.inRank() == 0) {
          rank0s.add(n);
        }
      }
      return rank0s;
    }

    private boolean pathFromTo(Node n1, Node n2) {
      final HashMap<Node, Node> map = new HashMap<>();
      return pathFromTo(n1, n2, map);
    }

    private boolean pathFromTo(Node n1, Node n2, HashMap<Node, Node> map) {
      if (n1 == n2) {
        return true;
      }
      if (map.containsKey(n1)) {
        return false;
      }
      map.put(n1, n1);
      for (int i = 0; i < n1.outRank(); i++) {
        if (pathFromTo((Node) n1.getSuccessor(i), n2, map)) {
          return true;
        }
      }
      return false;
    }

  }

  private Graph order;

  private TypeSystem ts;

  public LinearTypeOrderBuilderImpl(TypeSystem ts) {
    order = new Graph();
    this.ts = ts;
  }

  /**
   * The constructor for the total type order, called by the other constructor and also when doing a
   * cas complete deserialization, or just deserializing the type system/index defs
   * 
   * @param typeList
   *          -
   * @param ts
   *          -
   * @return -
   */
  public static LinearTypeOrder createTypeOrder(int[] typeList, TypeSystem ts) {
    return new TotalTypeOrder(typeList, ts, false);
  }

  @Override
  public void add(String[] types) throws CASException {
    final int max = types.length - 1;
    boolean rc;
    for (int i = 0; i < max; i++) {
      rc = add(types[i], types[i + 1]);
      if (!rc) {
        throw new CASException(CASException.CYCLE_IN_TYPE_ORDER, types[i], types[i + 1]);
      }
    }
  }

  private boolean add(String s1, String s2) {
    final Node n1 = order.getNode(s1);
    final Node n2 = order.getNode(s2);
    if (order.pathFromTo(n1, n2)) {
      return true;
    }
    if (order.pathFromTo(n2, n1)) {
      return false;
    }
    n1.connect(n2);
    return true;
  }

  private void addInheritanceTypes() {
    List<Type> typesToModify = new ArrayList<>();

    for (Iterator<Type> tsi = ts.getTypeIterator(); tsi.hasNext();) {
      Type bottomType = tsi.next();

      Type type = bottomType;
      Node nIn = null;
      Node nOut = null;
      typesToModify.clear();

      while (true) {
        String typeName = type.getName();
        final Node n = order.getNode(typeName);
        if ((nIn == null) && (n.inRank() != 0)) {
          nIn = n;
        }
        if ((nOut == null) && (n.outRank() != 0)) {
          nOut = n;
        }
        if (((nIn != null) && (nOut != null)) || typeName.equals(CAS.TYPE_NAME_TOP)) {
          break;
        }
        typesToModify.add(type);
        type = ts.getParent(type);
      }
      boolean doIn = true;
      boolean doOut = true;
      for (Iterator<Type> ni = typesToModify.iterator(); ni.hasNext();) {
        type = ni.next();
        String typeName = type.getName();
        final Node n = order.getNode(typeName);
        if (doIn && (nIn != null)) {
          if (n.inRank() == 0) {
            n.addAllPredecessors(nIn.getAllPredecessors());
          } else {
            doIn = false; // when going up the tree, when you find one
                          // filled in, stop
          }
        }
        if (doOut && (nOut != null)) {
          if (n.outRank() == 0) {
            n.addAllSuccessors(nOut.getAllSuccessors());
          } else {
            doOut = false;
          }
        }
      }
    } // for all types
  }

  @Override
  public LinearTypeOrder getOrder() throws CASException {
    int origOrderSize = order.size();
    addInheritanceTypes();
    Node inRank0Nodes = new Node("");
    Graph g = order.copy(inRank0Nodes);

    String[] totalOrder = new String[g.size()];
    // String s;
    for (int i = 0; i < totalOrder.length; i++) {
      Node n = (Node) inRank0Nodes.getSuccessor(0);
      totalOrder[i] = (String) n.getElement();
      inRank0Nodes.removeSuccessor(0);
      ArrayList<Node> newRank0Nodes = g.removeNode(n);
      for (Iterator<Node> it = newRank0Nodes.iterator(); it.hasNext();) {
        inRank0Nodes.addSuccessor(it.next());
      }
    }

    // System.out.println("Printing total order of types:");
    // for (int i = 0; i < totalOrder.length; i++) {
    // System.out.println(" " + totalOrder[i]);
    // }
    return new TotalTypeOrder(totalOrder, ts, origOrderSize == 0);
  }

}
