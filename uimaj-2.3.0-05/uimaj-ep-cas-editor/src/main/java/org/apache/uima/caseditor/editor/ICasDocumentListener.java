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

import org.apache.uima.cas.FeatureStructure;

/**
 * This interface is used to notifies clients about {@link FeatureStructure} changes.
 */
public interface ICasDocumentListener {
  /**
   * This method is called if an {@link FeatureStructure} is added.
   *
   * @param newFeatureStructure -
   *          the added {@link FeatureStructure}.
   */
  void added(FeatureStructure newFeatureStructure);

  /**
   * This method is called if multiple <code>FeatureStructure</code>s are added.
   *
   * @param newFeatureStructure -
   *          the added {@link FeatureStructure}s.
   */
  void added(Collection<FeatureStructure> newFeatureStructure);

  /**
   * This method is called if an {@link FeatureStructure} is removed.
   *
   * @param deletedFeatureStructure -
   *          the removed {@link FeatureStructure}.
   */
  void removed(FeatureStructure deletedFeatureStructure);

  /**
   * This method is called if multiple {@link FeatureStructure}s are removed.
   *
   * @param deletedFeatureStructure -
   *          the removed <code>Annotation</code>s.
   */
  void removed(Collection<FeatureStructure> deletedFeatureStructure);

  /**
   * This method is called if the {@link FeatureStructure} changed.
   *
   * @param featureStructure
   */
  void updated(FeatureStructure featureStructure);

  /**
   * This method is called if the {@link FeatureStructure}s changed.
   *
   * @param featureStructure
   */
  void updated(Collection<FeatureStructure> featureStructure);

  /**
   * This method is called if {@link FeatureStructure}s in the
   * document are changed.
   *
   * Note: The text can not be changed
   */
  void changed();
}