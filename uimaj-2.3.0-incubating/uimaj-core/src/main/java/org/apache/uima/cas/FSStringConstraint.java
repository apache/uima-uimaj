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
 * Interface for a String constraint. A String constraint supports equality testing to a given
 * string. After creating this constraint, use the {@link #equals(String)} method to specify the
 * string that the constraint uses in its testing.
 * <p>
 * To use the constraint, invoke its {@link #match(String)} method, passing the value to test. You
 * can also embed this test with a path specification, using the
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)} method,
 * and use it to test feature structures, or combine it with other tests using the
 * {@link org.apache.uima.cas.ConstraintFactory#and(FSMatchConstraint, FSMatchConstraint)} and
 * {@link org.apache.uima.cas.ConstraintFactory#or(FSMatchConstraint, FSMatchConstraint)} methods.
 */
public interface FSStringConstraint extends FSConstraint {

  /**
   * String value of matched FS must match input String.
   * 
   * @param s
   *          The string that the matched FS must equal.
   */
  void equals(String s);

  /**
   * Check if String matches defined constraints.
   * 
   * @param s
   *          The String to be checked.
   * @return <code>true</code> iff the String satisfies the constraints.
   */
  boolean match(String s);

}
