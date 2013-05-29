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

package org.apache.uima.caseditor.editor;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * The {@link ICasDocument} represents texts with meta information.
 * 
 * A {@link ICasDocument} allows manipulation of its meta information the text must not be changed.
 * 
 * Meta information can be retrieved over the {@link CAS} object.
 * 
 * Note: All changes to meta information should be done with calls to the manipulation methods of
 * the document. If this is not possible, change the {@link CAS} and after the change call the
 * {@link ICasDocument#changed()} method.
 */
public interface ICasDocument {

  /**
   * Adds a given change listener.
   * 
   * @param listener
   */
  void addChangeListener(ICasDocumentListener listener);

  /**
   * Removes the given change listener.
   * 
   * @param listener
   */
  void removeChangeListener(ICasDocumentListener listener);

  /**
   * Retrieves the CAS.
   * 
   * @return the CAS
   */
  CAS getCAS();

  /**
   * Adds a {@link FeatureStructure} to the document.
   * 
   * @param structure
   *          - the {@link FeatureStructure} to add.
   */
  void addFeatureStructure(FeatureStructure structure);

  /**
   * Adds the {@link FeatureStructure}s.
   * 
   * @param structures
   */
  void addFeatureStructures(Collection<? extends FeatureStructure> structures);

  /**
   * Removes an {@link FeatureStructure} from the Document.
   * 
   * @param structure
   *          - the {@link FeatureStructure} to remove.
   */
  void removeFeatureStructure(FeatureStructure structure);

  /**
   * Removes the given {@link FeatureStructure}s.
   * 
   * @param structuresToRemove
   */
  void removeFeatureStructures(Collection<? extends FeatureStructure> structuresToRemove);

  /**
   * Updates the given {@link FeatureStructure}.
   * 
   * @param structure
   */
  void update(FeatureStructure structure);

  /**
   * Updates the given {@link FeatureStructure}s.
   * 
   * @param structures
   */
  void updateFeatureStructure(Collection<? extends FeatureStructure> structures);

  /**
   * The document was changed. Its unknown what changed.
   */
  void changed();

  /**
   * Returns all <code>Annotation</code>s of the given type.
   * 
   * @param type
   *          - type of the requested <code>Annotation</code>s.
   * @return - return all <code>Annotation</code> of the given type or null if no
   *         <code>Annotation</code> of this type exist.
   */
  Collection<AnnotationFS> getAnnotations(Type type);

  /**
   * Switches the view of the underlying CAS to the provided view name.
   * 
   * @param viewName
   */
  void switchView(String viewName);

  /**
   * Retrieves the requested type.
   * 
   * @param type
   * @return the type
   */
  Type getType(String type);

  /**
   * Retrieves the string representation of the applied type system
   * 
   * @return the string representation of the applied type system
   */
  String getTypeSystemText();

}
