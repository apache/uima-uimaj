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

package org.apache.uima.internal.util;

import java.util.ArrayList;

/**
 * Interface for a generic node in a graph.
 * 
 * @see org.apache.uima.internal.util.Graph
 */
public class GraphNode {

  protected Object element;

  protected ArrayList<GraphNode> successors = new ArrayList<GraphNode>();

  protected ArrayList<GraphNode> predecessors = new ArrayList<GraphNode>();

  public GraphNode(Object element) {
    this.element = element;
  }

  /**
   * Get the element contained in the node.
   * 
   * @return The object contained in the node.
   */
  public Object getElement() {
    return this.element;
  }

  /**
   * Set the element in the node.
   * 
   * @param element
   *          The element.
   */
  public void setElement(Object element) {
    this.element = element;
  }

  /**
   * Get the number of successor node.
   * 
   * @return The number of successor nodes.
   */
  public int getNbrSucc() {
    return this.successors.size();
  }

  /**
   * Get a specific successor node. As usual, the count is 0-based.
   * 
   * @param i
   *          The number of the successor to be retrieved.
   * @return The successor node.
   */
  public GraphNode getSuccessor(int i) {
    if (i >= 0 && i < this.successors.size()) {
      return this.successors.get(i);
    }
    throw new UtilError(UtilError.ILLEGAL_SUCCESSOR_INDEX);
  }

  /**
   * Add a new successor node.
   * 
   * @param succ
   *          The node to be added.
   */
  public void addSuccessor(GraphNode succ) {
    this.successors.add(succ);
  }

  /**
   * Get the number of predecessor node.
   * 
   * @return The number of predecessor nodes.
   */
  public int getNbrPred() {
    return this.predecessors.size();
  }

  /**
   * Get a specific predecessor node. As usual, the count is 0-based.
   * 
   * @param i
   *          The number of the predecessor to be retrieved.
   * @return The predecessor node.
   */
  public GraphNode getPredecessor(int i) {
    if (i >= 0 && i < this.predecessors.size()) {
      return this.predecessors.get(i);
    }
    throw new UtilError(UtilError.ILLEGAL_PREDECESSOR_INDEX);
  }

  /**
   * Add a new predecessor node.
   * 
   * @param pred
   *          The node to be added.
   */
  public void addPredecessor(GraphNode pred) {
    this.predecessors.add(pred);
  }

  /**
   * Connect this node to a new node.
   * 
   * @param node
   *          The node to be connected to.
   */
  public void connect(GraphNode node) {
    this.addSuccessor(node);
    node.addPredecessor(this);
  }

}
