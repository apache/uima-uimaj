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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

/**
 * The internal implementation of the nlp model delta interface.
 * 
 * TODO: Add a filter to avoid node with Kind.NOTHING.
 */
public final class NlpModelDeltaImpl implements INlpElementDelta {
  private IResourceDelta mResourceDelta;

  private Collection<INlpElementDelta> mChildren = new LinkedList<INlpElementDelta>();

  private INlpElement mNlpElement;

  /**
   * The kind of the event. After an project was opened the old closed project (closed project does
   * not have a natures) must be removed. Then the newly created NlpProject will be added. For this
   * add the kind must be changed from changed to added. To have control about the kind the status
   * is duplicated here.
   */
  private Kind mKind;

  private NlpModelDeltaImpl mParent;

  /**
   * Initializes a new instance.
   * 
   * @param parent
   * @param resourceDelta
   */
  public NlpModelDeltaImpl(NlpModelDeltaImpl parent, IResourceDelta resourceDelta) {
    mParent = parent;

    mResourceDelta = resourceDelta;

    mKind = getKind(resourceDelta);

    IResourceDelta deltas[] = mResourceDelta.getAffectedChildren();

    for (IResourceDelta element : deltas) {
      if (element.getFlags() != IResourceDelta.MARKERS) {
        mChildren.add(new NlpModelDeltaImpl(this, element));
      }
    }
  }

  /**
   * Sets the nlp element Note: do not call this method from outside code
   * 
   * @param element
   */
  public void setNlpElement(INlpElement element) {
    mNlpElement = element;
  }

  /**
   * Retrieves the parent element or null if this is the root delta.
   * 
   * @return parent element or null
   */
  public INlpElementDelta getParent() {
    return mParent;
  }

  /**
   * Adds a child to the current element instance.
   * 
   * @param child
   */
  public void addChild(NlpModelDeltaImpl child) {
    mChildren.add(child);
  }

  /**
   * Accepts the given visitor.
   */
  public void accept(INlpModelDeltaVisitor visitor) {
    boolean wantsToVisitChilds = visitor.visit(this);

    if (wantsToVisitChilds) {
      for (INlpElementDelta delta : getAffectedChildren()) {
        delta.accept(visitor);
      }
    }
  }

  /**
   * Retrieves the affected children.
   */
  public INlpElementDelta[] getAffectedChildren() {
    return mChildren.toArray(new INlpElementDelta[mChildren.size()]);
  }

  /**
   * Checks if the current instance is a nlp element.
   */
  public boolean isNlpElement() {
    return mNlpElement != null;
  }

  /**
   * Retrieves the nlp element.
   */
  public INlpElement getNlpElement() {
    return mNlpElement;
  }

  /**
   * Retrieves the resource.
   */
  public IResource getResource() {
    return mResourceDelta.getResource();
  }

  /**
   * Retrieves the kind
   */
  public Kind getKind() {
    return mKind;
  }

  /**
   * Sets the new kind. This method should only be called from the event handlers and not by any
   * client.
   * 
   * @param kind
   */
  public void setKind(Kind kind) {
    mKind = kind;
  }

  /**
   * Retrieves the original flags of the {@link IResourceDelta} object.
   */
  public int getFlags() {
    return mResourceDelta.getFlags();
  }

  /**
   * Retrieves the resource delta.
   * 
   * @return the resource delta
   */
  public IResourceDelta getResourceDelta() {
    return mResourceDelta;
  }

  public IPath getMovedFromPath() {
    return mResourceDelta.getMovedFromPath();
  }

  public IPath getMovedToPath() {
    return mResourceDelta.getMovedToPath();
  }

  /**
   * Retrieves a human-readable string of the current instance.
   */
  @Override
  public String toString() {
    return (isNlpElement() ? "Nlp: " : "") + mResourceDelta.toString();
  }

  private static Kind getKind(IResourceDelta delta) {
    Kind kind;

    if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.ADDED_PHANTOM)) != 0) {
      kind = Kind.ADDED;
    } else if ((delta.getKind() & (IResourceDelta.REMOVED | IResourceDelta.REMOVED_PHANTOM)) != 0) {
      kind = Kind.REMOVED;
    } else if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
      kind = Kind.CHANGED;
    } else {
      kind = null;
    }

    return kind;
  }
}
