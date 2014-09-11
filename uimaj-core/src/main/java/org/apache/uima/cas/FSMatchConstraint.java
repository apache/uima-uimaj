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

import java.io.Serializable;

/**
 * Interface for feature structure matching constraints. These constraints are created as the result
 * of combining primitive constraints with a "path" that specifies how to find the value to test,
 * starting with a feature structure instance, and specifying which feature (or possibly a chain of
 * features) to use as the value, using the
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)} method.
 * These constraints are also produced by "anding" and "oring" together other instances of these
 * constraints.
 * <p>
 * To use the constraint, invoke its {@link #match(FeatureStructure)} method, passing the feature
 * structure to test.
 * <p>
 * You can also use these constraints to construct a
 * {@link org.apache.uima.cas.CAS#createFilteredIterator(FSIterator, FSMatchConstraint)}.
 */
public interface FSMatchConstraint extends FSConstraint, Serializable {

  /**
   * Match against feature structures.
   * 
   * @param fs
   *          The feature structure we want to match.
   * @return -
   */
  boolean match(FeatureStructure fs);

}
