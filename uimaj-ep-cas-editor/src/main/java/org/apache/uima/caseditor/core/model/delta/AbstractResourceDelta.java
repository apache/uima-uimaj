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

package org.apache.uima.caseditor.core.model.delta;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Abstract base class, so clients must not implement all methods.
 */
public abstract class AbstractResourceDelta implements IResourceDelta {
  
  /**
   * Dummy method, does nothing.
   */
  public void accept(IResourceDeltaVisitor visitor) throws CoreException {
    // not implemented
  }

  /**
   * Dummy method, does nothing.
   */
  public void accept(IResourceDeltaVisitor visitor, boolean includePhantoms) throws CoreException {
    // not implemented
  }

  /**
   * Dummy method, does nothing.
   */
  public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
    // not implemented
  }

  /**
   * Dummy method, does nothing.
   */
  public IResourceDelta findMember(IPath path) {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  public IResourceDelta[] getAffectedChildren() {
    return new IResourceDelta[] {};
  }

  /**
   * Dummy method, does nothing.
   */
  public IResourceDelta[] getAffectedChildren(int kindMask) {
    return new IResourceDelta[] {};
  }

  /**
   * Dummy method, does nothing.
   */
  public IResourceDelta[] getAffectedChildren(int kindMask, int memberFlags) {
    return new IResourceDelta[] {};
  }

  /**
   * Dummy method, does nothing.
   */
  public int getFlags() {
    return 0;
  }

  /**
   * Dummy method, does nothing.
   */
  public IPath getFullPath() {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  public int getKind() {
    return 0;
  }

  /**
   * Dummy method, does nothing.
   */
  public IMarkerDelta[] getMarkerDeltas() {
    return new IMarkerDelta[] {};
  }

  /**
   * Dummy method, does nothing.
   */
  public IPath getMovedFromPath() {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  public IPath getMovedToPath() {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  public IPath getProjectRelativePath() {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  public IResource getResource() {
    return null;
  }

  /**
   * Dummy method, does nothing.
   */
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return null;
  }
}
