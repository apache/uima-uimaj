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

import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

/**
 * A element delta represents changes in the state of a element tree between two discrete points in
 * time.
 * 
 * @see IResourceDelta
 */
public interface INlpElementDelta {

  /**
   * Accepts the given visitor. The only kinds of resource deltas visited are <code>ADDED</code>,
   * <code>REMOVED</code>, and <code>CHANGED</code>.
   * 
   * @param visotor
   */
  void accept(INlpModelDeltaVisitor visotor);

  /**
   * Returns resource deltas for all children of this resource which were added, removed, or
   * changed. Returns an empty array if there are no affected children.
   * 
   * @return - children or empty array
   */
  public INlpElementDelta[] getAffectedChildren();

  /**
   * Returns true if the given element is an nlp element.
   * 
   * @return true if an nlp element otherwise false
   */
  boolean isNlpElement();

  /**
   * Retrieves the nlp element.
   * 
   * @return the nlp element or if non null.
   */
  INlpElement getNlpElement();

  /**
   * Retrieves the resource belonging to this delta.
   * 
   * @return the resource
   */
  IResource getResource();

  /**
   * Retrieves the kind.
   * 
   * @return the kind
   */
  Kind getKind();

  /**
   * Retrieves the flags.
   * 
   * @return the flags
   */
  int getFlags();

  /**
   * Retrieves the moved to path.
   * 
   * @return
   */
  IPath getMovedToPath();

  /**
   * Retrieves the moved from path.
   * 
   * @return
   */
  IPath getMovedFromPath();
}
