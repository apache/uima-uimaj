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
package org.apache.uima.fit.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.uima.fit.factory.FsIndexFactory;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;

/**
 * @see FsIndexKeyDescription
 * @see FsIndexFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FsIndexKey {
  /**
   * @see FsIndexKeyDescription#STANDARD_COMPARE
   */
  public static final int STANDARD_COMPARE = FsIndexKeyDescription.STANDARD_COMPARE;

  /**
   * @see FsIndexKeyDescription#REVERSE_STANDARD_COMPARE
   */
  public static final int REVERSE_STANDARD_COMPARE = FsIndexKeyDescription.REVERSE_STANDARD_COMPARE;

  /**
   * @see FsIndexKeyDescription#getFeatureName()
   * 
   * @return the name of this key's Feature
   */
  String featureName();

  /**
   * @see FsIndexKeyDescription#getComparator()
   * 
   * @return this key's comparator
   */
  int comparator() default STANDARD_COMPARE;
}
