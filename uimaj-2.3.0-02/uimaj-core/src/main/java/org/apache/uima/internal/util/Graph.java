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

import java.util.Vector;

/**
 * A simple graph class.
 * 
 * @see org.apache.uima.internal.util.GraphNode
 */
public class Graph {

  protected GraphNode root;

  /** @return The root of the graph. */
  public GraphNode getRoot() {
    return this.root;
  }

  /**
   * Set the root of the graph.
   * 
   * @param root
   *          The root node.
   */
  public void setRoot(GraphNode root) {
    this.root = root;
  }

  protected static int getNodeCode(GraphNode n, Vector<GraphNode> nodes) {
    int max = nodes.size();
    for (int i = 0; i < max; i++) {
      if (n == nodes.get(i)) {
        return i;
      }
    }
    return -1;
  }

  public static void collectNodes(GraphNode n, Vector<GraphNode> nodes) {
    if (getNodeCode(n, nodes) >= 0) {
      return;
    }
    nodes.add(n);
    int max = n.getNbrSucc();
    for (int i = 0; i < max; i++) {
      collectNodes(n.getSuccessor(i), nodes);
    }
  }

}
