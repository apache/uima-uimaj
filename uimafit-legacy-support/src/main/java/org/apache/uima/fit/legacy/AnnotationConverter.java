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
package org.apache.uima.fit.legacy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

/**
 * Annotation converters for legacy uimaFIT annotations to Apache uimaFIT annotations.
 *
 * @param <L> legacy annotation type.
 * @param <M> modern annotation type.
 */
public interface AnnotationConverter<L extends Annotation,M extends Annotation> {
  /**
   * Convert the given legacy annotation to its modern counterpart.
   * 
   * @param aAnnotation a legacy annotation.
   * @return the modern annotation.
   */
  M convert(Class<?> aContext, L aAnnotation);

  M convert(AccessibleObject aContext, L aAnnotation);

  Class<M> getModernType();
  
  Class<L> getLegacyType();
}
