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


import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.util.Primitives;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

public final class FeatureValue implements IAdaptable {
  private FeatureStructure mStructure;

  private Feature mFeature;

  /**
   * Initializes a new instance.
   *
   * @param feature
   * @param value
   */
  public FeatureValue(AnnotationDocument document, FeatureStructure structure, Feature feature) {
    Assert.isNotNull(document);
    // TODO: Remove document parameter ? Not needed anymore!
    
    Assert.isNotNull(feature);
    mFeature = feature;

    Assert.isNotNull(feature);
    mStructure = structure;
  }

  public FeatureStructure getFeatureStructure() {
    return mStructure;
  }

  public Object getValue() {
    if (mFeature.getRange().isPrimitive()) {
      return Primitives.getPrimitiv(mStructure, mFeature);
    }

    return mStructure.getFeatureValue(mFeature);
  }

  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (AnnotationFS.class.equals(adapter)) {
      if (getValue() instanceof AnnotationFS) {
        return getValue();
      }
    }

    if (FeatureStructure.class.equals(adapter)) {
      if (getValue() instanceof FeatureStructure) {
        return getValue();
      }
    }

    return null;
  }

  @Override
  public boolean equals(Object object) {
    boolean result = false;

    if (this == object) {
      result = true;
    } else if (object != null && object instanceof FeatureValue) {
      FeatureValue valueToCompare = (FeatureValue) object;

      result = valueToCompare.mStructure.equals(mStructure)
              && valueToCompare.mFeature.equals(mFeature);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return mStructure.hashCode() | mFeature.hashCode();
  }

  /**
   * Retrieves the {@link Feature}.
   *
   * @return the {@link Feature}
   */
  public Feature getFeature() {
    return mFeature;
  }
}
