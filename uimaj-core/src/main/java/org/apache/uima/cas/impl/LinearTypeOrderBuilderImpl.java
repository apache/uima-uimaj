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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
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
         * An implementation of the {@link LinearTypeOrder LinearTypeOrder}
         * interface.
         * 
         * 
         */
  private static class TotalTypeOrder implements LinearTypeOrder {

    // The explicit order. We keep this since we need to return it. It would
    // be awkward and inefficient to compute it from lt.
    // index = orderNumber, value = type-code
    // used by serialization routines
    private int[] order;

    // index= typeCode, value = order number
    private int[] typeCodeToOrder;

    private TotalTypeOrder(String[] typeList, TypeSystem ts) throws CASException {
      this(encodeTypeList(typeList, ts), ts);
    }

    private static int[] encodeTypeList(String[] typeList, TypeSystem ts) throws CASException {
      int[] a = new int[typeList.length];
      LowLevelTypeSystem llts = (LowLevelTypeSystem) ts;
      for (int i = 0; i < a.length; i++) {
        int t = llts.ll_getCodeForTypeName(typeList[i]);
        if (t == LowLevelTypeSystem.UNKNOWN_TYPE_CODE) {
          CASException e = new CASException(CASException.TYPEORDER_UNKNOWN_TYPE,
              new String[] { typeList[i] });
          throw e;
        }
        a[i] = t;
      }
      return a;
    }

    // Create the order from an array of type codes in ascending order.
    private TotalTypeOrder(int[] typeList, TypeSystem ts) {
      super();
      TypeSystemImpl tsi = (TypeSystemImpl) ts;
      this.order = typeList;
      this.typeCodeToOrder = new int[this.order.length + tsi.getSmallestType()];
      for (int i = 0; i < this.order.length; i++) {
        this.typeCodeToOrder[this.order[i]] = i;
      }
    }

    // Look-up.
    public boolean lessThan(Type t1, Type t2) {
      return lessThan(((TypeImpl) t1).getCode(), ((TypeImpl) t2).getCode());
      // return this.lt[((TypeImpl) t1).getCode()].get(((TypeImpl) t2)
      // .getCode());
    }

    public boolean lessThan(int t1, int t2) {
      return this.typeCodeToOrder[t1] < this.typeCodeToOrder[t2];

      // return this.lt[t1].get(t2);
    }

    public int[] getOrder() {
      return this.order;
    }

  }

  private class Node extends GraphNode {

    private Node(Object o) {
      super(o);
    }

    private void removeAncestor(Node node) {
      this.predecessors.remove(node);
    }

    private int outRank() {
      return getNbrSucc();
    }

    private int inRank() {
      return getNbrPred();
    }

    private ArrayList<GraphNode> getAllPredecessors() {
      return this.predecessors;
    }

    private ArrayList<GraphNode> getAllSuccessors() {
      return this.successors;
    }

    private void removeSuccessor(int i) {
      this.successors.remove(i);
    }

    private void addAllPredecessors(ArrayList<? extends GraphNode> pred) {
      for (Iterator<? extends GraphNode> it = pred.iterator(); it.hasNext();) {
        Node n = (Node) it.next();
        if (!LinearTypeOrderBuilderImpl.this.order.pathFromTo(this, n)) {
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
        if (!LinearTypeOrderBuilderImpl.this.order.pathFromTo(n, this)) {
          connect(n);
        }
        // else {
        // System.out.println("Testing: add succ: found loop from " +
        // this.getElement() + " to " +
        // n.getElement());
        // }
      }
    }

    public boolean equals(Object o) {
      return (this == o);
    }

    public int hashCode() {
      return super.hashCode();
    }

  }

  private class Graph {

    // Map type names to graph nodes.
    private final HashMap<String, Node> nodeMap = new HashMap<String, Node>();

    private int size() {
      return this.nodeMap.size();
    }

    private Node getNode(String name) {
      Node node = this.nodeMap.get(name);
      if (node == null) {
        node = new Node(name);
        this.nodeMap.put(name, node);
      }
      return node;
    }

    private Graph copy(Node inRank0nodes) {
      Graph copy = new Graph();
      Iterator<Map.Entry<String, Node>> it = this.nodeMap.entrySet().iterator();
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
      it = this.nodeMap.entrySet().iterator();
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
      ArrayList<Node> rank0s = new ArrayList<Node>();
      // if (node == null) {
      // return ;
      // }
      this.nodeMap.remove(node.getElement());
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
      final HashMap<Node, Node> map = new HashMap<Node, Node>();
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
    super();
    this.order = new Graph();
    this.ts = ts;
  }

  public static LinearTypeOrder createTypeOrder(int[] typeList, TypeSystem ts) {
    return new TotalTypeOrder(typeList, ts);
  }

  public void add(String[] types) throws CASException {
    final int max = types.length - 1;
    boolean rc;
    for (int i = 0; i < max; i++) {
      rc = add(types[i], types[i + 1]);
      if (!rc) {
	CASException e = new CASException(CASException.CYCLE_IN_TYPE_ORDER, new String[] {
	    types[i], types[i + 1] });
	throw e;
      }
    }
  }

  private boolean add(String s1, String s2) {
    final Node n1 = this.order.getNode(s1);
    final Node n2 = this.order.getNode(s2);
    if (this.order.pathFromTo(n1, n2)) {
      return true;
    }
    if (this.order.pathFromTo(n2, n1)) {
      return false;
    }
    n1.connect(n2);
    return true;
  }

  private void addInheritanceTypes() {
    List<Type> typesToModify = new ArrayList<Type>();

    for (Iterator<Type> tsi = this.ts.getTypeIterator(); tsi.hasNext();) {
      Type bottomType = tsi.next();

      Type type = bottomType;
      Node nIn = null;
      Node nOut = null;
      typesToModify.clear();

      while (true) {
	String typeName = type.getName();
	final Node n = this.order.getNode(typeName);
	if ((nIn == null) && (n.inRank() != 0)) {
	  nIn = n;
	}
	if ((nOut == null) && (n.outRank() != 0)) {
	  nOut = n;
	}
	if ((nIn != null) && (nOut != null)) {
	  break;
	}
	if (typeName.equals(CAS.TYPE_NAME_TOP)) {
	  break;
	}
	typesToModify.add(type);
	type = this.ts.getParent(type);
      }
      boolean doIn = true;
      boolean doOut = true;
      for (Iterator<Type> ni = typesToModify.iterator(); ni.hasNext();) {
	type = ni.next();
	String typeName = type.getName();
	final Node n = this.order.getNode(typeName);
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

  public LinearTypeOrder getOrder() throws CASException {
    addInheritanceTypes();
    Node inRank0Nodes = new Node("");
    Graph g = this.order.copy(inRank0Nodes);

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
    return new TotalTypeOrder(totalOrder, this.ts);
  }

}
