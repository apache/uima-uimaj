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
import java.util.Collection;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.AbstractAnnotationDocumentListener;
import org.apache.uima.caseditor.editor.AnnotationDocument;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

abstract class OutlineContentProviderBase extends AbstractAnnotationDocumentListener
		implements ITreeContentProvider {
	
	protected AnnotationEditor mEditor;
	  
	protected ICasDocument mInputDocument;

    protected TreeViewer viewer;

    protected OutlineContentProviderBase(AnnotationEditor editor, TreeViewer viewer) {
    	this.viewer = viewer;
    	this.mEditor = editor;
    }
    
    /**
     * not implemented
     */
    public void dispose() {
      // currently not implemented
    }

	/**
	 * Gets called if the viewer input was changed. In this case, this only happens once if the
	 * {@link AnnotationOutline} is initialized.
	 *
	 * @param viewer
	 * @param oldInput
	 * @param newInput
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	  if (oldInput != null) {
	    ((AnnotationDocument) oldInput).removeChangeListener(this);
	  }
	
	  if (newInput != null) {
	    ((AnnotationDocument) newInput).addChangeListener(this);
	
	    mInputDocument = (ICasDocument) newInput;
	    
	    changed();
	  }
	}

	/**
	 * Updates the given annotation in the viewer.
	 *
	 * @param annotations
	 */
	@Override
	protected void updatedAnnotation(Collection<AnnotationFS> featureStructres) {
	  Collection<AnnotationFS> annotations = new ArrayList<AnnotationFS>(featureStructres.size());
	
	  for (FeatureStructure structure : featureStructres) {
	    if (structure instanceof AnnotationFS) {
	      annotations.add((AnnotationFS) structure);
	    }
	  }
	
	  final Object[] items = new Object[annotations.size()];
	
	  int i = 0;
	  for (AnnotationFS annotation : annotations) {
	    items[i++] = new AnnotationTreeNode(mEditor.getDocument(), annotation);
	  }
	
	  Display.getDefault().syncExec(new Runnable() {
	    public void run() {
	    	viewer.update(items, null);
	    }
	  });
	}
}