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

package org.apache.uima.caseditor.editor.outline;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.Type;
import org.eclipse.core.runtime.IAdaptable;

/**
 * The {@link AnnotationTreeNode} is used to group annotations by their type. Only the
 * {@link TypeGroupedContentProvider} creates {@link AnnotationTreeNode} objects.
 */
class AnnotationTypeTreeNode implements IAdaptable {

  /** The annotation type. */
  private Type type;

  /** The annotations. */
  private List<AnnotationTreeNode> annotations = new ArrayList<>();

  /**
   * Instantiates a new annotation type tree node.
   *
   * @param type
   *          the type
   */
  public AnnotationTypeTreeNode(Type type) {
    this.type = type;
  }

  /**
   * Adds the.
   *
   * @param annotation
   *          the annotation
   */
  public void add(AnnotationTreeNode annotation) {
    annotations.add(annotation);
  }

  /**
   * Gets the annotations.
   *
   * @return the annotations
   */
  public AnnotationTreeNode[] getAnnotations() {
    return annotations.toArray(new AnnotationTreeNode[annotations.size()]);
  }

  /**
   * Removes the.
   *
   * @param annotation
   *          the annotation
   */
  public void remove(AnnotationTreeNode annotation) {
    annotations.remove(annotation);
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {

    if (Type.class.equals(adapter)) {
      return type;
    }

    return null;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof AnnotationTypeTreeNode) {
      AnnotationTypeTreeNode otherTypeNode = (AnnotationTypeTreeNode) obj;

      return type.equals(otherTypeNode.type);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return type.getShortName() + " #chhildren = " + annotations.size();
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public Object getType() {
    return type;
  }
}
