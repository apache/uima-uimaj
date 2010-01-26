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

package org.apache.uima.caseditor.core.model;

import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;

/**
 * The abstract base class of all nlp element implementations.
 */
public abstract class AbstractNlpElement extends PlatformObject implements INlpElement {
  
  /**
   * Checks if the current elements resource equals the given resource, if so it returns the current
   * element, otherwise null.
   */
  public INlpElement findMember(IResource resource) {
    if (getResource().equals(resource)) {
      return this;
    }

    return null;
  }

  /**
   * Retrieves the {@link IResource} of the current instance.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    Object result;

    if (adapter.equals(IResource.class)) {
      result = getResource();
    } else {
      result = super.getAdapter(adapter);
    }

    return result;
  }

  /**
   * Checks if the given resource equals the current elements resource, if so it returns the parent
   * of the current element, otherwise null.
   * 
   * @throws CoreException
   */
  public INlpElement getParent(IResource resource) throws CoreException {
    INlpElement result;

    if (getResource().equals(resource)) {
      return getParent();
    } else {
      result = null;
    }

    return result;
  }

  /**
   * Adds resources after the element was initialized.
   * 
   * @param delta
   *          TODO
   * @param resource
   *          the added resource
   * 
   * @throws CoreException
   */
  abstract void addResource(INlpElementDelta delta, IResource resource) throws CoreException;

  /**
   * Removes resources after the element was initialized.
   * 
   * @param delta
   *          TODO
   * @param resource
   *          the removed resource
   * 
   * @throws CoreException
   */
  abstract void removeResource(INlpElementDelta delta, IResource resource) throws CoreException;

  /**
   * Changed resource after the element was initialized.
   * 
   * @param resource
   */
  @SuppressWarnings("all")
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
  }

  /**
   * Retrieves the human-readable name.
   */
  @Override
  public String toString() {
    return getResource().toString();
  }
}
