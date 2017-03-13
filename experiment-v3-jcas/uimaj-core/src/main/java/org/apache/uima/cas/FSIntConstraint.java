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
 * Interface for an integer constraint. A integer constraint contains 0 or more tests, the results
 * of which are "anded" together. To set a test, use any of the methods on this class (except
 * "match").
 * <p>
 * To use the constraint, invoke its {@link #match(int)} method, passing the value to test. You can
 * also embed this test with a path specification, using the
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)} method,
 * and use it to test feature structures, or combine it with other tests using the
 * {@link org.apache.uima.cas.ConstraintFactory#and(FSMatchConstraint, FSMatchConstraint)} and
 * {@link org.apache.uima.cas.ConstraintFactory#or(FSMatchConstraint, FSMatchConstraint)} methods.
 */
public interface FSIntConstraint extends FSConstraint {

  /**
   * Require int value to be equal <code>i</code>.
   * 
   * @param i
   *          Matched value must be equal to this.
   */
  void eq(int i);

  /**
   * Require int value to be less than <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than this.
   */
  void lt(int i);

  /**
   * Require int value to be less than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than or equal to this.
   */
  void leq(int i);

  /**
   * Require int value to be greater than <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than this.
   */
  void gt(int i);

  /**
   * Require int value to be greater than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than or equal to this.
   */
  void geq(int i);

  /**
   * Check if integer matches defined constraints.
   * 
   * @param i
   *          The int to be checked.
   * @return <code>true</code> iff the int satisfies the constraints.
   */
  boolean match(int i);

}
