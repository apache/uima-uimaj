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

package org.apache.uima.cas.text;

import java.util.ArrayList;

import org.apache.uima.cas.CASRuntimeException;

/**
 * Represents a node in an annotation tree.
 * 
 * 
 */
public interface AnnotationTreeNode<T extends AnnotationFS> {

  /**
   * Get the parent of this node.
   * 
   * @return The parent of this node, or <code>null</code> if this node has no parent (root).
   */
  AnnotationTreeNode<T> getParent();

  /**
   * Get the number of children this node has.
   * 
   * @return The number of children.
   */
  int getChildCount();

  /**
   * Get the i-th child of this node.
   * 
   * @param i
   *          The index of the child.
   * @return The i-th child.
   * @throws CASRuntimeException
   *           If <code>i &lt; 0</code> or <code>i &gt;= getChildCount()</code>.
   */
  AnnotationTreeNode<T> getChild(int i) throws CASRuntimeException;

  /**
   * Get the next sibling (to the right) of this node.
   * 
   * @return The right sibling of this node, or <code>null</code> if no such sibling exists.
   */
  AnnotationTreeNode<T> getNextSibling();

  /**
   * Get the previous sibling (to the left) of this node.
   * 
   * @return The left sibling of this node, or <code>null</code> if no such sibling exists.
   */
  AnnotationTreeNode<T> getPreviousSibling();

  /**
   * Get all children of this node as an ArrayList.
   * 
   * @return An ArrayList of the children.
   */
  ArrayList<AnnotationTreeNode<T>> getChildren();

  /**
   * Return the annotation for this node.
   * 
   * @return The annotation for this node.
   */
  T get();

}
