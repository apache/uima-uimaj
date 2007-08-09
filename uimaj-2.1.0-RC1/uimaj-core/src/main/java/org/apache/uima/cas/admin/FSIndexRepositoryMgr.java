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

import org.apache.uima.cas.FSIndexRepository;

/**
 * Repository of indexes. Indexes are defined via
 * {@link org.apache.uima.cas.admin.FSIndexComparator FSIndexComparator}s.
 * 
 * @see org.apache.uima.cas.FSIndex
 */
public interface FSIndexRepositoryMgr extends FSIndexRepository {

  /**
   * Create a new comparator to define a new index.
   * 
   * @return A new comparator instance.
   */
  public FSIndexComparator createComparator();

  /**
   * Create a new index. Note: if you creata a BAG_INDEX, the comparator will be ignored.
   * 
   * @param comp
   *          The comparator for the new index.
   * @param label
   *          The name of the new index.
   * @param indexingStrategy
   *          The kind of index (sorted, set, bag).
   * @return <code>false</code> iff an index with the same<code>label</code> already exists.
   * @throws CASAdminException
   *           If the repository is locked (after calling {@link #commit() commit()}).
   */
  boolean createIndex(FSIndexComparator comp, String label, int indexingStrategy)
          throws CASAdminException;

  /**
   * Create a new sorted index.
   * 
   * @param comp
   *          The comparator for the new index.
   * @param label
   *          The name of the new index.
   * @return <code>false</code> iff an index with the same<code>label</code> already exists.
   * @throws CASAdminException
   *           If the repository is locked (after calling {@link #commit() commit()}).
   */
  boolean createIndex(FSIndexComparator comp, String label) throws CASAdminException;

  /**
   * Commit this repository instance. No more additions will be allowed.
   */
  void commit();

  /**
   * Check if this instance has been committed.
   * 
   * @return <code>true</code> iff this instance has been committed.
   */
  boolean isCommitted();

  /**
   * Get the default type order builder.
   * 
   * @return The default type order builder.
   */
  LinearTypeOrderBuilder getDefaultOrderBuilder();

  /**
   * Get the default type order.
   * 
   * @return The default type order.
   */
  LinearTypeOrder getDefaultTypeOrder();

  /**
   * Currently not useful.
   * 
   * @return A new type order builder.
   */
  LinearTypeOrderBuilder createTypeSortOrder();

  /**
   * Create a new index comparator for creating an index.
   * 
   * @return A new index comparator.
   */
  // FSIndexComparator createIndexComparator();
}
