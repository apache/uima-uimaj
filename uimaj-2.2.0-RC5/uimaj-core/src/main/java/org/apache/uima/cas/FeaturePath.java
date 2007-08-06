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

package org.apache.uima.cas;

/**
 * Interface for a feature path. A feature path is a sequence of {@link org.apache.uima.cas.Feature}s
 * that are used with constraint tests to specify how to get the value to test, starting from a
 * given feature structure.
 * 
 * To use:
 * <ul>
 * <li>create an empty feature path using {@link org.apache.uima.cas.CAS#createFeaturePath()} </li>
 * <li>add one or more features to it, representing the chain of features to follow to get to the
 * value to test, using {@link #addFeature(Feature)} </li>
 * <li>hook up this feature path with a particular constraint test, using
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)}. </li>
 * </ul>
 */
public interface FeaturePath {

  /**
   * Get length of path.
   * 
   * @return An integer <code>&gt;= 0</code>.
   */
  int size();

  /**
   * Get feature at position.
   * 
   * @param i
   *          The position in the path (starting at <code>0</code>).
   * @return The feature, or <code>null</code> if there is no such feature.
   */
  Feature getFeature(int i);

  /**
   * Add a new feature at the end of the path.
   * 
   * @param feat
   *          The feature to be added.
   */
  void addFeature(Feature feat);

}
