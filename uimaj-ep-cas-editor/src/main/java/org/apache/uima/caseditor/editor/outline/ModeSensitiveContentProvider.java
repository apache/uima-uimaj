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

import java.util.Collection;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

/**
 * This <code>OutlineContentProvider</code> synchronizes the <code>AnnotationFS</code>s with the
 * <code>TableViewer</code>.
 */
class ModeSensitiveContentProvider extends OutlineContentProviderBase {

  /** The m annotation node list. */
  private AnnotationTreeNodeList mAnnotationNodeList;

  /**
   * Instantiates a new mode sensitive content provider.
   *
   * @param editor
   *          the editor
   * @param viewer
   *          the viewer
   */
  protected ModeSensitiveContentProvider(AnnotationEditor editor, TreeViewer viewer) {
    super(editor, viewer);
    this.viewer = viewer;
  }

  /**
   * Adds the added annotations to the viewer.
   *
   * @param annotations
   *          the annotations
   */
  @Override
  public void addedAnnotation(Collection<AnnotationFS> annotations) {
    for (AnnotationFS annotation : annotations) {
      if (!annotation.getType().getName().equals(mEditor.getAnnotationMode().getName())) {
        return;
      }

      final AnnotationTreeNode annotationNode = new AnnotationTreeNode(mEditor.getDocument(),
              annotation);

      mAnnotationNodeList.add(annotationNode);
      // mAnnotationNodeList.buildTree();

      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          viewer.add(
                  annotationNode.getParent() != null ? annotationNode.getParent() : mInputDocument,
                  annotationNode);
        }
      });
    }
  }

  /**
   * Removes the removed annotations from the viewer.
   *
   * @param deletedAnnotations
   *          the deleted annotations
   */
  @Override
  public void removedAnnotation(Collection<AnnotationFS> deletedAnnotations) {
    // TODO: what happens if someone removes an annoation which
    // is not an element of this list e.g in the featruestructure view ?
    final AnnotationTreeNode[] items = new AnnotationTreeNode[deletedAnnotations.size()];

    int i = 0;
    for (AnnotationFS annotation : deletedAnnotations) {
      // TODO: maybe it is a problem if the parent is not correctly set!
      items[i] = new AnnotationTreeNode(mEditor.getDocument(), annotation);
      mAnnotationNodeList.remove(items[i]);
      i++;
    }

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        viewer.remove(items);
      }
    });
  }

  @Override
  public void viewChanged(String oldViewName, String newViewName) {
    changed();
  }

  @Override
  public void changed() {
    Collection<AnnotationFS> annotations = mEditor.getDocument()
            .getAnnotations(mEditor.getAnnotationMode());

    mAnnotationNodeList = annotations != null
            ? new AnnotationTreeNodeList(mEditor.getDocument(), annotations)
            : null;

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        viewer.refresh();
      }
    });
  }

  /**
   * Retrieves all children of the NlpModel. That are the NlpProjects and {@link IProject}s.
   *
   * @param inputElement
   *          the input element
   *
   * @return the nlp-projects and non-nlp projects
   */
  @Override
  public Object[] getElements(Object inputElement) {
    if (mAnnotationNodeList == null) {
      return new Object[0];
    }

    return mAnnotationNodeList.getElements().toArray();
  }

  @Override
  public Object getParent(Object element) {
    AnnotationTreeNode node = (AnnotationTreeNode) element;
    return node.getParent();
  }

  @Override
  public boolean hasChildren(Object element) {
    AnnotationTreeNode node = (AnnotationTreeNode) element;
    return node.getChildren().size() > 0;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    AnnotationTreeNode node = (AnnotationTreeNode) parentElement;
    return node.getChildren().toArray();
  }
}
