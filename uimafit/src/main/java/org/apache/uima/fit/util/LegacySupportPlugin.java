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
package org.apache.uima.fit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

import org.apache.uima.resource.ResourceInitializationException;

/**
 * INTERNAL API - Legacy support plug-in API.
 */
public interface LegacySupportPlugin {
  /**
   * Checks if a legacy version of the given modern annotation is present.
   * 
   * @param aObject
   *          an object that might have a legacy annotation.
   * @param aAnnotationClass
   *          the modern annotation type.
   * @return {@code true} if a legacy version of the annotation is present.
   */
  boolean isAnnotationPresent(AccessibleObject aObject, Class<? extends Annotation> aAnnotationClass);

  /**
   * Checks if a legacy version of the given modern annotation is present.
   * 
   * @param aObject
   *          an object that might have a legacy annotation.
   * @param aAnnotationClass
   *          the modern annotation type.
   * @return {@code true} if a legacy version of the annotation is present.
   */
  boolean isAnnotationPresent(Class<?> aObject, Class<? extends Annotation> aAnnotationClass);

  /**
   * Gets the annotation from the given object. Instead of looking for the given modern annotation,
   * this method looks for a legacy version of the annotation, converts it to a modern annotation
   * and returns that.
   * 
   * @param aObject
   *          an object that has a legacy annotation.
   * @param aAnnotationClass
   *          the modern annotation type.
   * @return an instance of the modern annotation filled with the data from the legacy annotation.
   */
  <L extends Annotation, M extends Annotation> M getAnnotation(AccessibleObject aObject,
          Class<M> aAnnotationClass);

  /**
   * Gets the annotation from the given object. Instead of looking for the given modern annotation,
   * this method looks for a legacy version of the annotation, converts it to a modern annotation
   * and returns that.
   * 
   * @param aObject
   *          an object that has a legacy annotation.
   * @param aAnnotationClass
   *          the modern annotation type.
   * @return an instance of the modern annotation filled with the data from the legacy annotation.
   */
  <L extends Annotation, M extends Annotation> M getAnnotation(Class<?> aObject,
          Class<M> aAnnotationClass);
//
//  /**
//   * Get the default value of a property of the annotation. This is used for example to get the
//   * default name of a configuration parameter.
//   * 
//   * @param aObject
//   *          an object that has a legacy annotation.
//   * @param aAnnotationClass
//   *          the modern annotation type.
//   * @param aProperty
//   *          a property of the annotation, e.g. {@code "name"} (see
//   *          {@link ConfigurationParameter#name()})
//   * @return
//   */
//  <M extends Annotation> Object getDefaultValue(AccessibleObject aObject, Class<M> aAnnotationClass,
//          String aProperty);

  /**
   * Get all currently accessible descriptor locations for the given type.
   * 
   * @return an array of locations.
   * @throws ResourceInitializationException
   *           if the locations could not be resolved.
   */
  String[] scanTypeDescriptors(MetaDataType aType) throws ResourceInitializationException;
}
