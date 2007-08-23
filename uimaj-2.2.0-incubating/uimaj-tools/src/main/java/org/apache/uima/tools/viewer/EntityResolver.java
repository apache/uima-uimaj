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
package org.apache.uima.tools.viewer;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * Pluggable interface that supports Entity View mode in the CasAnnotationViewer. Users implement
 * this interface with logic that for their particular type system can determine which Entity an
 * Annotation refers to.
 * <p>
 * In the viewer, all annotations whose Entity objects are <code>equal</code> will be
 * displayed in the same color, and the Entity's canonical form will be shown in the legend.
 */
public interface EntityResolver {
  
  /**
   * Returns the <code>Entity</code> to which an annotation refers.
     * Returns the canonical form String for an annotation.
     * <p>
     * For two annotations that refer to the same Entity, this should return <code>Entity</code>
     * objects that are <code>equal</code>.
     * <p>
     * If the annotation does not represent an entity at all, <code>null</code> should be returned.
     * 
     * @param aAnnotation the annotation to resolve
     * 
     * @return the Entity to which the annotation refers, null if the annotation does not represent an
     */
  Entity getEntity(Annotation aAnnotation);
  
  /**
   * Object representing an Entity.  Annotations whose <code>Entity</code> objects are <code>equal</code>
   * will be displayed in the same color in the viewer.  
   * <p>
   * This means that either the <code>EntityResolver</code>
   * must return the identical (<code>==</code>) <code>Entity</code> object for annotations belonging to the
   * same entity, or your <code>Entity</code> objects must implement {@link Object#equals(Object)} and
   * {@link  Object#hashCode()}.  
   *
   */
  public interface Entity {
    /**
     * Returns the canonical form String for an Entity.  This string will be displayed in the legend.
     * 
     * @return the canonical form of the entity
     */
    String getCanonicalForm();
  }
}
