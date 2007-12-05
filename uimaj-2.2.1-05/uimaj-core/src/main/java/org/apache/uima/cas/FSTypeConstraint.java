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
 * Interface for type constraint. Tests if a feature structure is a subtype of a given type (or
 * types).
 * 
 * <p>
 * You can add as many types to this constraint as you like. If you add more than one type, they
 * will be interpreted disjunctively; i.e., the constraint will match any feature structure whose
 * type is subsumed by one of the types of the constraint.
 * 
 * <p>
 * these constraints may be embedded in a path, or combined using and/or operators.
 * <p>
 * To use the constraint, invoke its {@link #match(FeatureStructure)} method, passing the feature
 * structure to test.
 * <p>
 * You can also use these constraints to construct a
 * {@link org.apache.uima.cas.CAS#createFilteredIterator(FSIterator, FSMatchConstraint)}.
 * 
 */
public interface FSTypeConstraint extends FSMatchConstraint {

  /**
   * Add a new type to this type constraint. This method can be called more than once. Multiple
   * types will be interpreted disjunctively.
   * 
   * @param type
   *          A type that should be permitted by this constraint.
   */
  void add(Type type);

  /**
   * Add a new type to this type constraint. This method can be called more than once. Multiple
   * types will be interpreted disjunctively.
   * 
   * @param type
   *          A fully qualified type name that should be permitted by this constraint.
   */
  void add(String type);

}
