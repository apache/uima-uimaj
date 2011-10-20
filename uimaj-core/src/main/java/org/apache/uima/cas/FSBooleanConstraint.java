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
 * Interface for a boolean constraint. A boolean constraint contains a single condition, either true
 * or false. To set the condition, use 
 * <p>
 * To use the constraint, invoke its {@link #match(boolean)} method, passing the value to test. You
 * can also embed this test with a path specification, using the
 * {@link org.apache.uima.cas.ConstraintFactory#embedConstraint(FeaturePath, FSConstraint)} method,
 * and use it to test feature structures, or combine it with other tests using the
 * {@link org.apache.uima.cas.ConstraintFactory#and(FSMatchConstraint, FSMatchConstraint)} and
 * {@link org.apache.uima.cas.ConstraintFactory#or(FSMatchConstraint, FSMatchConstraint)} methods.

 */
public interface FSBooleanConstraint extends FSConstraint {

  /**
   * Set the constraint.
   * @param condition The condition that needs to be matched to satisfy the constraint.
   */
  public void eq(boolean condition);
  
  /**
   * Check the condition.
   * @param condition Value to compare with the condition.
   * @return True if value and constraint are equal, false else.
   */
  public boolean match(boolean condition);
}
