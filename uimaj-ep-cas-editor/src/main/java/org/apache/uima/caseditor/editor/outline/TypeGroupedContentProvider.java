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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

/**
 * The content provider for the type grouped annotation outline.
 * 
 * TODO:
 * Make it sensitive to show annotations menu from the editor ...
 * Who sends selection events which are not from the UI thread ?
 */
public class TypeGroupedContentProvider extends OutlineContentProviderBase {

	// map of AnnotationTypeTreeNode
	private Map<String, AnnotationTypeTreeNode> nameAnnotationTypeNodeMap = 
			new HashMap<String, AnnotationTypeTreeNode>();

	TypeGroupedContentProvider(AnnotationEditor editor, TreeViewer viewer) {
		super(editor, viewer);
	}
	
	@Override
	protected void addedAnnotation(Collection<AnnotationFS> annotations) {

		for (AnnotationFS annotation : annotations) {
			String name = annotation.getType().getName();

			AnnotationTypeTreeNode typeNode = nameAnnotationTypeNodeMap
					.get(name);

			AnnotationTreeNode annotationNode = new AnnotationTreeNode(mInputDocument, annotation); 
			typeNode.add(annotationNode);
			
			viewer.add(typeNode, annotationNode);
		}
	}

	@Override
	protected void removedAnnotation(Collection<AnnotationFS> annotations) {

	  Set<AnnotationTreeNode> removeAnnotations = new HashSet<AnnotationTreeNode>();
	  
		for (AnnotationFS annotation : annotations) {
			String name = annotation.getType().getName();

			AnnotationTypeTreeNode typeNode = nameAnnotationTypeNodeMap.get(name);
			
			if (typeNode != null) {
  			AnnotationTreeNode annotationNode = new AnnotationTreeNode(mInputDocument, annotation); 
  			typeNode.remove(annotationNode);
  			
  			removeAnnotations.add(annotationNode);
			}
			else {
			  CasEditorPlugin.logError("Unmapped annotation type!");
			}
		}
		
		viewer.remove(removeAnnotations.toArray());
	}

	public Object[] getElements(Object inputElement) {
	
		return nameAnnotationTypeNodeMap.values().toArray();
	}

	public Object getParent(Object element) {
	
		if (element instanceof AnnotationTreeNode) {
			AnnotationTreeNode annotation = (AnnotationTreeNode) element;
		
			Type type = annotation.getAnnotation().getType();
		
			if (type != null) {
				return nameAnnotationTypeNodeMap.get(type.getName());
			}
			else {
				// Can happen when a changed request was triggered after
				// a CAS.reset(), in this case it is not possible to find a 
				// parent for the element and null should be returned.
				return null;
			}
		}
		else if (element instanceof AnnotationTypeTreeNode) {
			// The head type elements to not have a parent
			return null;
		}
		else {
			CasEditorPlugin.logError("TypeGroupedContentProvider: Unexpected type " + element.getClass().getName());
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		if (element instanceof AnnotationTypeTreeNode) {
			AnnotationTypeTreeNode treeNode = (AnnotationTypeTreeNode) element;
			
			return treeNode.getAnnotations().length > 0;
		} else {
			return false;
		}
	}

	public Object[] getChildren(Object parentElement) {

		AnnotationTypeTreeNode typeNode = (AnnotationTypeTreeNode) parentElement;

		return typeNode.getAnnotations();
	}

	public void viewChanged(String oldViewName, String newViewName) {
		changed();
	}
	
	public void changed() {
		nameAnnotationTypeNodeMap.clear();
		
		TypeSystem typeSystem = mInputDocument.getCAS().getTypeSystem();
		
		List<Type> types = typeSystem.getProperlySubsumedTypes(
				typeSystem.getType(CAS.TYPE_NAME_ANNOTATION));
		
		types.add(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION));
		
		for (Type type : types) {
			
			AnnotationTypeTreeNode typeNode = new AnnotationTypeTreeNode(type);
			
			nameAnnotationTypeNodeMap.put(type.getName(), typeNode);
			
			CAS cas = mInputDocument.getCAS();
			
			AnnotationIndex<AnnotationFS> index = cas.getAnnotationIndex(type);
			
			for (FSIterator<AnnotationFS> it = index.iterator(); it.hasNext(); ) {
				AnnotationFS annotation = it.next();
				
				if (annotation.getType().equals(type)) {
				  typeNode.add(new AnnotationTreeNode(mInputDocument, annotation));
				}
			}
		}

      Display.getDefault().syncExec(new Runnable() {
        public void run() {
        	viewer.refresh();
        }
      });
	}
}
