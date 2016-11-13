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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.uima.cas.FeatureStructure;

// TODO: Auto-generated Javadoc
/**
 * TODO: add javadoc here.
 */
public abstract class AbstractDocumentListener implements ICasDocumentListener {

  /* (non-Javadoc)
   * @see org.apache.uima.caseditor.editor.ICasDocumentListener#added(java.util.Collection)
   */
  @Override
  public void added(Collection<FeatureStructure> newFeatureStructure) {
  }
  
  /**
   * Forwards the call.
   *
   * @param newAnnotation the new annotation
   */
  @Override
  public void added(FeatureStructure newAnnotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(newAnnotation);

    added(structures);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.caseditor.editor.ICasDocumentListener#removed(java.util.Collection)
   */
  @Override
  public void removed(Collection<FeatureStructure> deletedFeatureStructure) {
  }
  
  /**
   * Forwards the call.
   *
   * @param deletedAnnotation the deleted annotation
   */
  @Override
  public void removed(FeatureStructure deletedAnnotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(deletedAnnotation);

    removed(structures);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.caseditor.editor.ICasDocumentListener#updated(java.util.Collection)
   */
  @Override
  public void updated(Collection<FeatureStructure> featureStructure) {
  }
  
  /**
   * Forwards the call.
   *
   * @param annotation the annotation
   */
  @Override
  public void updated(FeatureStructure annotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(annotation);

    updated(structures);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.caseditor.editor.ICasDocumentListener#changed()
   */
  @Override
  public void changed() {
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.caseditor.editor.ICasDocumentListener#viewChanged(java.lang.String, java.lang.String)
   */
  @Override
  public void viewChanged(String oldViewName, String newViewName) {
  }
}