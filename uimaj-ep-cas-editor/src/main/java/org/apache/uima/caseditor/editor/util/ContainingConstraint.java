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

package org.apache.uima.caseditor.editor.util;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;


/**
 * Filters out all annotations which are not contained by one of the provided
 * annotations.
 */
public class ContainingConstraint implements FSMatchConstraint {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1;

  /** The m containing annotations. */
  private Collection<AnnotationFS> mContainingAnnotations = new LinkedList<>();

  /**
   * Adds an annotation in which the an other annotations must be contained to match this
   * constraint.
   *
   * @param containingAnnotation the containing annotation
   */
  public void add(AnnotationFS containingAnnotation) {
    mContainingAnnotations.add(containingAnnotation);
  }

  /**
   * Checks if the given FeatureStructure is inside the a containing annotation.
   *
   * @param featureStructure the feature structure
   * @return true, if successful
   */
  @Override
  public boolean match(FeatureStructure featureStructure) {
    boolean result = false;

    if (featureStructure instanceof AnnotationFS) {
      AnnotationFS annotation = (AnnotationFS) featureStructure;

      for (AnnotationFS containingAnnotation : mContainingAnnotations) {
        if (isContaining(annotation, containingAnnotation)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Checks if is containing.
   *
   * @param annotation the annotation
   * @param containing the containing
   * @return true, if is containing
   */
  private boolean isContaining(AnnotationFS annotation, AnnotationFS containing) {
    return containing.getBegin() <= annotation.getBegin()
        && containing.getEnd() >= annotation.getEnd();
  }
}
