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
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.core.model.NlpModel;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

/**
   * This <code>OutlineContentProvider</code> synchronizes the <code>AnnotationFS</code>s with
   * the <code>TableViewer</code>.
   */
  class ModeSensitiveContentProvider extends OutlineContentProviderBase {
	  
    private AnnotationTreeNodeList mAnnotationNodeList;

    private Map<AnnotationFS, AnnotationTreeNode> mParentNodeLookup =
      new HashMap<AnnotationFS, AnnotationTreeNode>();

    protected ModeSensitiveContentProvider(AnnotationEditor editor, TreeViewer viewer) {
    	super(editor, viewer);
    	this.viewer = viewer;
    }

    /**
     * Adds the added annotations to the viewer.
     *
     * @param annotations
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
          public void run() {
        	  viewer.add(annotationNode.getParent() != null ? annotationNode.getParent()
                    : mInputDocument, annotationNode);
          }
        });
      }
    }

    /**
     * Removes the removed annoations from the viewer.
     *
     * @param deletedAnnotations
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
        public void run() {
        	viewer.remove(items);
        }
      });
    }

    public void changed() {

      Collection<AnnotationFS> annotations = mEditor.getDocument().getAnnotations(
              mEditor.getAnnotationMode());

      mAnnotationNodeList = annotations != null ? new AnnotationTreeNodeList(mEditor
              .getDocument(), annotations) : null;

      mParentNodeLookup.clear();

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
        	viewer.refresh();
        }
      });
    }

    /**
	 * Retrieves all children of the {@link NlpModel}. That are the {@link NlpProject}s and
	 * {@link IProject}s.
	 *
	 * @param inputElement
	 *          the {@link NlpModel}
	 *          
	 * @return the nlp-projects and non-nlp projects
	 */
	public Object[] getElements(Object inputElement) {
	  if (mAnnotationNodeList == null) {
	    return new Object[0];
	  }
	
	  return mAnnotationNodeList.getElements().toArray();
	}

	public Object getParent(Object element) {
	  AnnotationTreeNode node = (AnnotationTreeNode) element;
	
	  return node.getParent();
	}

	public boolean hasChildren(Object element) {
	  AnnotationTreeNode node = (AnnotationTreeNode) element;
	
	  return node.getChildren().size() > 0;
	}

	public Object[] getChildren(Object parentElement) {
      AnnotationTreeNode node = (AnnotationTreeNode) parentElement;

      return node.getChildren().toArray();
    }
  }