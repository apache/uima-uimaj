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

import java.util.ArrayList;

import org.apache.uima.cas.impl.ConstraintFactoryImpl;

/**
 * Methods to create {@link org.apache.uima.cas.FSMatchConstraint}s for filtered iterators or other
 * use. A constraint is an object which represents a test or a series of tests joined with "and" or
 * "or". Each test consists of a test predicate plus an optional "path" which specifies how to walk
 * through a chain of references, starting from a feature structure being tested, to reach the value
 * to be tested.
 * <p>
 * Tests include
 * <ul>
 * <li>type subsumption --(satisfied if the CAS feature structure being tested is of a specified
 * type (or is a subtype of that type). </li>
 * <li>value equality</li>
 * <li>for numeric values - range testing</li>
 * </ul>
 * 
 * Constraints can be used by calling their "match" method, passing as an argument the value to
 * test. If the constraint includes the "path", the argument is a feature structure; the path
 * specifies how to reach the value to test, starting with the feature structure. Otherwise, the
 * value to test depends on the constraint; for an {@link org.apache.uima.cas.FSIntConstraint}, for
 * instance, the value would be an integer.
 */
public abstract class ConstraintFactory {

  /**
   * Create a new type constraint. A type constraint contains one or more types to test against. A
   * type constraint must be initialized by adding one or more types to it. The match is true if any
   * of the types are the same or a super type of the feature structure being tested by the
   * constraint.
   * 
   * @return A new type constraint with the type set to the top type.
   */
  public abstract FSTypeConstraint createTypeConstraint();

  /**
   * Create a new int constraint. An int constraint must be initialized after it's created by adding
   * one or more tests to it.
   * 
   * @return A new int constraint, completely unconstrained.
   */
  public abstract FSIntConstraint createIntConstraint();

  /**
   * Create a new float constraint. A float constraint must be initialized after it's created by
   * adding one or more tests to it.
   * 
   * @return A new float constraint, completely unconstrained.
   */
  public abstract FSFloatConstraint createFloatConstraint();

  /**
   * Create a new String constraint. A String constraint must be initialized after it's created by
   * adding one or more tests to it.
   * 
   * @return A new String constraint, completely unconstrained.
   */
  public abstract FSStringConstraint createStringConstraint();
  
  /**
   * Create a new boolean constraint. A boolean constraint must be initialized after it's created by
   * adding one or more tests to it.
   * 
   * @return A new boolean constraint, completely unconstrained.
   */
  public abstract FSBooleanConstraint createBooleanConstraint();

  /**
   * Combine a constraint test with a path from a feature structure instance to the value to be
   * tested. This is called "embedding" a constraint under a path. For example, create an int
   * constraint, and then embed it under some int valued feature, such as the start feature of an
   * annotation.
   * 
   * @param path
   *          The path to embed the constraint under. Create a new path with
   *          {@link CAS#createFeaturePath() CAS.createFeaturePath()}.
   * @param constraint
   *          The constraint to be embedded.
   * @return A new FSMatchConstraint.
   */
  public abstract FSMatchConstraint embedConstraint(FeaturePath path, FSConstraint constraint);

  /**
   * Embed a constraint under a path. For example, create an int constraint, and then embed it under
   * some int valued feature, such as the start feature of an annotation.
   * 
   * @param path
   *          The path to embed the constraint under. This is a list of {@link Feature} names.
   * @param constraint
   *          The constraint to be embedded.
   * @return A new FSMatchConstraint.
   */
  public abstract FSMatchConstraint embedConstraint(ArrayList<String> path, FSConstraint constraint);

  /**
   * Conjoin two constraints.
   * 
   * @param c1
   *          The first conjunct.
   * @param c2
   *          The second conjunct.
   * @return A new FSMatchConstraint, representing the conjunction of <code>c1</code> and
   *         <code>c2</code>.
   */
  public abstract FSMatchConstraint and(FSMatchConstraint c1, FSMatchConstraint c2);

  /**
   * Disjoin two constraints.
   * 
   * @param c1
   *          The first disjunct.
   * @param c2
   *          The second disjunct.
   * @return A new FSMatchConstraint, representing the disjunction of <code>c1</code> and
   *         <code>c2</code>.
   */
  public abstract FSMatchConstraint or(FSMatchConstraint c1, FSMatchConstraint c2);

  /**
   * Create a new constraint factory.
   * 
   * @return A new ConstraintFactory instance.
   */
  public static ConstraintFactory instance() {
    return new ConstraintFactoryImpl();
  }

}
