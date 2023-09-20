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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationTreeNode;

public class AnnotationTreeNodeImpl<T extends AnnotationFS> implements AnnotationTreeNode<T> {

  private T annot;

  private AnnotationTreeNodeImpl<T> parent;

  private ArrayList<AnnotationTreeNode<T>> dtrs;

  private int pos;

  AnnotationTreeNodeImpl() {
    dtrs = new ArrayList<>();
  }

  @Override
  public AnnotationTreeNode<T> getParent() {
    return parent;
  }

  @Override
  public int getChildCount() {
    return dtrs.size();
  }

  @Override
  public AnnotationTreeNode<T> getChild(int i) throws CASRuntimeException {
    try {
      return dtrs.get(i);
    } catch (IndexOutOfBoundsException e) {
      throw new CASRuntimeException(CASRuntimeException.CHILD_INDEX_OOB, null);
    }
  }

  @Override
  public AnnotationTreeNode<T> getNextSibling() {
    if (parent == null) {
      return null;
    }
    try {
      return parent.getChild(pos + 1);
    } catch (CASRuntimeException e) {
      return null;
    }
  }

  @Override
  public AnnotationTreeNode<T> getPreviousSibling() {
    if (parent == null) {
      return null;
    }
    try {
      return parent.getChild(pos - 1);
    } catch (CASRuntimeException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getChildren()
   */
  @Override
  public ArrayList<AnnotationTreeNode<T>> getChildren() {
    return dtrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#get()
   */
  @Override
  public T get() {
    return annot;
  }

  // ////////////////////////////////////////////////////////////////////////////
  // Package private APIs for building the tree.

  void set(T annot) {
    this.annot = annot;
  }

  void addChild(AnnotationTreeNodeImpl<T> child) {
    child.pos = dtrs.size();
    child.parent = this;
    dtrs.add(child);
  }

}
