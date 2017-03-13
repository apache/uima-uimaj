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

import org.apache.uima.cas.CASException;

/**
 * Defines a pre-order on types. This pre-order is later embedded in a total order an can be used in
 * index comparators.
 * 
 * 
 */
public interface LinearTypeOrderBuilder {

  /**
   * Add pairs types[i] &lt; types[i+1], for each i &lt; (types.length-1), to the partial sort
   * order. This method can be called as often as desired. It will throw an exception if the pairs
   * could not be successfully added to the relation. A pair can not be added if the resulting
   * relation is no longer a partial order. If you need to know exactly which pair fails, always
   * call add() with a two-element array.
   * 
   * @param types types to add
   * @exception CASException
   *              When adding pairs would make order inconsistent.
   */
  void add(String[] types) throws CASException;

  /**
   * Return a total order of the type names added earlier that is consistent with the pre-order
   * defined through calls to add().
   * 
   * @return An array of Strings in ascending order.
   * @throws CASException if any error
   */
  LinearTypeOrder getOrder() throws CASException;

}
