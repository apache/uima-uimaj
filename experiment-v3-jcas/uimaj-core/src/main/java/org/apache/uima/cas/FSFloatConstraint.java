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
 * Interface for a float constraint. A float constraint contains 0 or more tests, the results of
 * which are "anded" together. To set a test, use any of the methods on this class (except "match").
 * <p>
 * To use the constraint, invoke its {@link #match(float)} method, passing the value to test. You
 * can also embed this test with a path specification, using the
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)} method,
 * and use it to test feature structures, or combine it with other tests using the
 * {@link org.apache.uima.cas.ConstraintFactory#and(FSMatchConstraint, FSMatchConstraint)} and
 * {@link org.apache.uima.cas.ConstraintFactory#or(FSMatchConstraint, FSMatchConstraint)} methods.
 */
public interface FSFloatConstraint extends FSConstraint {

  /**
   * Require float value to be equal <code>f</code>.
   * 
   * @param f
   *          Matched value must be equal to this.
   */
  void eq(float f);

  /**
   * Require float value to be less than <code>f</code>.
   * 
   * @param f
   *          Matched value must be less than this.
   */
  void lt(float f);

  /**
   * Require float value to be less than or equal to <code>f</code>.
   * 
   * @param f
   *          Matched value must be less than or equal to this.
   */
  void leq(float f);

  /**
   * Require float value to be greater than <code>f</code>.
   * 
   * @param f
   *          Matched value must be greater than this.
   */
  void gt(float f);

  /**
   * Require float value to be greater than or equal to <code>f</code>.
   * 
   * @param f
   *          Matched value must be greater than or equal to this.
   */
  void geq(float f);

  /**
   * Check if float matches defined constraints.
   * 
   * @param f
   *          The float to be checked.
   * @return <code>true</code> iff the float satisfies the constraints.
   */
  boolean match(float f);

}
