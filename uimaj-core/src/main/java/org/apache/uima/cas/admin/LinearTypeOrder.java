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
package org.apache.uima.cas.admin;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

/**
 * Linear order on types.
 */
public interface LinearTypeOrder {

  /**
   * Compare two types.
   * 
   * @param t1
   *          type to compare
   * @param t2
   *          type to compare
   * @return <code>true</code> iff <code>t1</code> is less than <code>t2</code> in this order.
   */
  boolean lessThan(Type t1, Type t2);

  /**
   * Compare two types.
   * 
   * @param t1
   *          type to compare
   * @param t2
   *          type to compare
   * @return <code>true</code> iff <code>t1</code> is less than <code>t2</code> in this order.
   */
  boolean lessThan(int t1, int t2);

  /**
   * Compare two Feature Structure's types
   * 
   * @param fs1
   *          first Feature Structure
   * @param fs2
   *          second Feature Structure
   * @return same as compare functions: -1 if fs1's type &lt; fs2's type, etc.
   */
  int compare(FeatureStructure fs1, FeatureStructure fs2);

  /**
   * @return The type order as array of type codes in ascending order.
   */
  int[] getOrder();

  /**
   * @return true if there is no type order defined for this pipeline
   */
  boolean isEmptyTypeOrder();
}
