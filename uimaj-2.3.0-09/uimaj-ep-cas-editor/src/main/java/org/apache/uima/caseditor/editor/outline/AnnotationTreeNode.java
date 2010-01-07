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

import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

/**
 * TODO: add javadoc here
 */
public class AnnotationTreeNode implements IAdaptable {
  private AnnotationTreeNode mParent;

  private final AnnotationTreeNodeList mChildren;

  private final AnnotationFS mAnnotation;

  private final ICasDocument mDocument;

  AnnotationTreeNode(ICasDocument document, AnnotationFS annotation) {
    Assert.isNotNull(document);
    mDocument = document;

    Assert.isNotNull(annotation);
    mAnnotation = annotation;

    mChildren = new AnnotationTreeNodeList(mDocument);
  }

  AnnotationTreeNode getParent() {
    return mParent;
  }

  List<AnnotationTreeNode> getChildren() {
    return mChildren.getElements();
  }

  AnnotationFS getAnnotation() {
    return mAnnotation;
  }

  /**
   * Checks if the given node is completly contained by the current node instance.
   *
   * @param node
   * @return true if completly contained otherwise false
   */
  boolean isChild(AnnotationTreeNode node) {
    return getAnnotation().getBegin() <= node.getAnnotation().getBegin()
            && getAnnotation().getEnd() >= node.getAnnotation().getEnd();
  }

  void addChild(AnnotationTreeNode node) {
    node.mParent = this;

    mChildren.add(node);

    mChildren.buildTree();
  }

  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    // TODO:
    // use ModelFeatureStructure
    // create a AdapterFactory which just calls the
    // ModelFeatureStructureAdpaterFactory

    if (AnnotationFS.class.equals(adapter) || FeatureStructure.class.equals(adapter)) {
      return getAnnotation();
    }
    else {
      return null;
    }
  }

  @Override
  public int hashCode() {
    
//    final int PRIME = 31;
//    int result = 1;
//    result = PRIME * result + (mAnnotation == null ? 0 : mAnnotation.hashCode());
//    result = PRIME * result + (mChildren == null ? 0 : mChildren.hashCode());
//    result = PRIME * result + (mParent == null ? 0 : mParent.hashCode());
    
    return mAnnotation.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    
    if (this == obj) {
      return true;
    }
    else if (obj instanceof AnnotationTreeNode) {
      AnnotationTreeNode other = (AnnotationTreeNode) obj;
      
      return other.getAnnotation().equals(mAnnotation);
    }
    else {
      return false;
    }
  }
}
