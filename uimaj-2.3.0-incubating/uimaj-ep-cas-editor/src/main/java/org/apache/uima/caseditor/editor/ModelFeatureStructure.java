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
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

/**
 * TODO:
 * set feature value to null
 * delete feature value structure
 * create feature value structure
 */
public class ModelFeatureStructure implements IAdaptable {
  private ICasDocument mDocument;

  private FeatureStructure mFeatureStructre;

  /**
   * Initializes a new instance.
   *
   * @param document
   * @param featureStructre
   */
  public ModelFeatureStructure(ICasDocument document, FeatureStructure featureStructre) {
    mDocument = document;
    mFeatureStructre = featureStructre;
  }

  public ICasDocument getDocument() {
    return mDocument;
  }

  public FeatureStructure getStructre() {
    return mFeatureStructre;
  }

  public void setFeatureNull(Feature feature) {
    mFeatureStructre.setFeatureValue(feature, null);
  }

  public void deleteFeatureValue(Feature feature) {
    // get value and call remove
  }

  public void createFeatureValue(Feature feature) {
    // create, add and link
  }

  public void createFeatureValueArray(Feature feature, int size) {
    // create add and link
  }


  public static List<ModelFeatureStructure> create(ICasDocument document,
          List<AnnotationFS> annotations) {
    List<ModelFeatureStructure> structres = new ArrayList<ModelFeatureStructure>(annotations.size());

    for (AnnotationFS annotation : annotations) {
      structres.add(new ModelFeatureStructure(document, annotation));
    }

    return structres;
  }

  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (FeatureStructure.class.equals(adapter)) {
      return getStructre();
    } else if (AnnotationFS.class.equals(adapter) && getStructre() instanceof AnnotationFS) {
      return getStructre();
    } else {
      return Platform.getAdapterManager().getAdapter(this, adapter);
    }
  }

  public void update() {
    mDocument.update(mFeatureStructre);
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    }
    else if (obj instanceof ModelFeatureStructure) {
      ModelFeatureStructure foreignFS = (ModelFeatureStructure) obj;

      return mFeatureStructre.equals(foreignFS.mFeatureStructre);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mFeatureStructre.hashCode();
  }
}