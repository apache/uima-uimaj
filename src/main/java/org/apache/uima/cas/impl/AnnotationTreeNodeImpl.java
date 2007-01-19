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

/**
 * TODO: Create type comment for AnnotationTreeNodeImpl.
 * 
 * 
 */

public class AnnotationTreeNodeImpl implements AnnotationTreeNode {

  private AnnotationFS annot;

  private AnnotationTreeNodeImpl parent;

  private ArrayList dtrs;

  private int pos;

  /**
   * 
   */
  AnnotationTreeNodeImpl() {
    super();
    this.dtrs = new ArrayList();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getParent()
   */
  public AnnotationTreeNode getParent() {
    return this.parent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getChildCount()
   */
  public int getChildCount() {
    return this.dtrs.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getChild(int)
   */
  public AnnotationTreeNode getChild(int i) throws CASRuntimeException {
    try {
      return (AnnotationTreeNode) this.dtrs.get(i);
    } catch (IndexOutOfBoundsException e) {
      throw new CASRuntimeException(CASRuntimeException.CHILD_INDEX_OOB, null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getNextSibling()
   */
  public AnnotationTreeNode getNextSibling() {
    if (this.parent == null) {
      return null;
    }
    try {
      return this.parent.getChild(this.pos + 1);
    } catch (CASRuntimeException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getPreviousSibling()
   */
  public AnnotationTreeNode getPreviousSibling() {
    if (this.parent == null) {
      return null;
    }
    try {
      return this.parent.getChild(this.pos - 1);
    } catch (CASRuntimeException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#getChildren()
   */
  public ArrayList getChildren() {
    return this.dtrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationTreeNode#get()
   */
  public AnnotationFS get() {
    return this.annot;
  }

  // ////////////////////////////////////////////////////////////////////////////
  // Package private APIs for building the tree.

  void set(AnnotationFS annot) {
    this.annot = annot;
  }

  void addChild(AnnotationTreeNodeImpl child) {
    child.pos = this.dtrs.size();
    child.parent = this;
    this.dtrs.add(child);
  }

}
