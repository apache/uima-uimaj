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


import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.util.AnnotationComparator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Sorts <code>AnnotationFS</code>s for a Viewer. This implementation is based on
 * <code>AnnotationComparator</code>.
 *
 * @see AnnotationComparator
 */
class OutlineTableSorter extends ViewerSorter {
  private AnnotationComparator mComperator = new AnnotationComparator();

  /**
   * Uses <code>AnnotationComparator</code> to compare the both objects.
   *
   * @return int the return value is if aObject < bObject negative number, if aObject == bObject 0,
   *         aObject > bObject a positive number or if both objects have different types 1.
   *
   * @see ViewerSorter
   */
  @Override
  public int compare(Viewer viewer, Object aObject, Object bObject) {
    int result = 1;

    if (aObject instanceof IAdaptable && bObject instanceof IAdaptable) {
      AnnotationFS aAnnotation = (AnnotationFS) ((IAdaptable) aObject)
              .getAdapter(AnnotationFS.class);

      AnnotationFS bAnnotation = (AnnotationFS) ((IAdaptable) bObject)
              .getAdapter(AnnotationFS.class);

      if (aAnnotation != null && bAnnotation != null)
    	  result = mComperator.compare(aAnnotation, bAnnotation);
    }

    return result;
  }
}