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

package org.apache.uima.caseditor.ui.corpusview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Base class for actions which dependent on {@link IResource} selections.
 */
public abstract class ResourceAction extends BaseSelectionListenerAction {

  protected ResourceAction(String text) {
    super(text);
  }

  /**
   * Retrieves all selected items which are not {@link IResource}s.
   *
   * @return selected item which are not a {@link IResource}s
   */
  protected List<Object> getSelectedNonResources() {
    List<Object> nonResources = new ArrayList<Object>();

    for (Iterator<?> it = getStructuredSelection().iterator(); it.hasNext(); ) {

      Object selection = it.next();

      if (selection instanceof IAdaptable) {
        IAdaptable adapter = (IAdaptable) selection;

        IResource resource = (IResource) adapter.getAdapter(IResource.class);

        if (resource == null) {
          nonResources.add(selection);
        }
      }
      else if (!(selection instanceof IResource)) {
        nonResources.add(selection);
      }
    }

    return nonResources;
  }

  /**
   * Retrieves all selected {@link IResource} items.
   *
   * @return all selected {@link IResource} items
   */
  protected List<IResource> getSelectedResources() {

    List<IResource> resources = new ArrayList<IResource>();

    for (Iterator<?> it = getStructuredSelection().iterator(); it.hasNext(); ) {
      Object selection = it.next();

      if (selection instanceof IResource) {
        resources.add((IResource) selection);
      } else if (selection instanceof IAdaptable) {
        IAdaptable adapter = (IAdaptable) selection;

        IResource resource = (IResource) adapter.getAdapter(IResource.class);

        if (resource != null) {
          resources.add(resource);
        }
      }
    }

    return resources;
  }

  protected boolean resourceIsType(IResource resource, int resourceMask) {
    return (resource.getType() & resourceMask) != 0;
  }

  protected boolean selectionIsOfType(int resourceMask) {
    if (getSelectedNonResources().size() > 0) {
      return false;
    }

    for (Object element : getSelectedResources()) {
      IResource next = (IResource) element;
      if (!resourceIsType(next, resourceMask)) {
        return false;
      }
    }
    return true;
  }
}
