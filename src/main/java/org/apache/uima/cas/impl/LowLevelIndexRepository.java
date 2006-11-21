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

package org.apache.uima.cas.impl;

/**
 * Low-level index repository access. Provides access to low-level indexes.
 * 
 * <p>
 * Use
 * {@link org.apache.uima.cas.impl.LowLevelCAS#ll_getIndexRepository() LowLevelCAS.ll_getIndexRepository()}
 * to gain access to an object of this type.
 * 
 */
public interface LowLevelIndexRepository {
  /**
   * Get an index by its name.
   * 
   * @param indexName
   *          The name of the index.
   * @return The corresponding index, or <code>null</code> if no such index exists.
   */
  LowLevelIndex ll_getIndex(String indexName);

  /**
   * Get an index by a name and type. The type must be a subtype of the index's type. Note that
   * there is no special checked version of this method, the type parameters are always checked.
   * 
   * @param indexName
   *          The name of the index.
   * @param typeCode
   *          The code of the desired subtype.
   * @return The corresponding index, or <code>null</code> if no such index exists.
   * @exception LowLevelException
   *              If the type code argument is not a valid type code.
   */
  LowLevelIndex ll_getIndex(String indexName, int typeCode);

  /**
   * Add a FS reference to all appropriate indexes in the repository.
   * 
   * @param fsRef
   *          The FS reference to be added to the repository. If <code>fsRef</code> is not a valid
   *          FS reference, the subsequent behavior of the system is undefined.
   */
  void ll_addFS(int fsRef);

  /**
   * Add a FS reference to all appropriate indexes in the repository.
   * 
   * @param fsRef
   *          The FS reference to be added to the repository. If <code>fsRef</code> is not a valid
   *          FS reference, the subsequent behavior of the system is undefined.
   * @param doChecks
   *          Check if the FS reference argument is a valid reference.
   */
  void ll_addFS(int fsRef, boolean doChecks);

  /**
   * Remove a FS reference from all indexes in the repository. Note that this only removes the
   * reference from the index repository, it does not free memory on the heap.
   * 
   * @param fsRef
   *          The FS reference to be removed from the indexes.
   */
  void ll_removeFS(int fsRef);
}
