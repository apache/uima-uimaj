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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

/**
 * Interface for creating comparators, which in turn are used to create indexes.
 * 
 * 
 */
public interface FSIndexComparator extends Comparable<FSIndexComparator> {

  static final int FEATURE_KEY = 0;

  static final int TYPE_ORDER_KEY = 1;

  /**
   * Compare key1 of fs1 with key2 of fs2 so that the order of fs1 and fs2 is the same as that of
   * key1 and key2 in the standard order for that kind of key. For integer and float values, this is
   * the standard linear order, and for strings it is lexicographic order.
   */
  static final int STANDARD_COMPARE = 0;

  /**
   * Compare key1 of fs1 with key2 of fs2 so that the order of fs1 and fs2 is the reverse as that of
   * key1 and key2 (with respect to the standard order of that key).
   */
  static final int REVERSE_STANDARD_COMPARE = 1;

  /**
   * Set the type of this comparator. Note that you can use this method more than once, in case you
   * need to generate indexes that are identical except for the type.
   * 
   * @param type
   *          The type of the index.
   */
  void setType(Type type);

  /**
   * Get the type of this comparator.
   * 
   * @return The type of the comparator.
   */
  Type getType();

  /**
   * Add a new key.
   * 
   * @param feat
   *          The key feature.
   * @param compareKey
   *          The way to compare the key values.
   * @return The number of the key.
   */
  int addKey(Feature feat, int compareKey);

  int addKey(LinearTypeOrder typeOrder, int compareKey);

  /**
   * Return the number of keys.
   * 
   * @return the number of keys.
   */
  int getNumberOfKeys();

  int getKeyType(int key);

  /**
   * Get the feature for this key.
   * 
   * @param key
   *          The number of the key.
   * @return The corresponding feature, if it exists; <code>null</code>, else.
   */
  Feature getKeyFeature(int key);

  /**
   * Get the comparator for this key.
   * 
   * @param key
   *          The number of the key.
   * @return The corresponding comparator, if it exists; <code>-1</code>, else.
   */
  int getKeyComparator(int key);

  /**
   * Test for equality against another <code>FSIndexComparator</code>
   *  <code>true</code> iff the
   * comparators have the same keys and comparators.
   */
  boolean equals(Object o);

  /**
   * Validate that this comparator is valid with respect to the type system. Note that all types and
   * features used to define this comparator must come from the same type system. Note that this
   * method only returns true or false. It doesn't tell you what's actually wrong. Maybe we need to
   * change that?
   * 
   * @return <code>true</code> iff all key features are appropriate for the type of this
   *         comparator.
   */
  boolean isValid();

}
