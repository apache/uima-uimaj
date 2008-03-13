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

import java.util.Iterator;

/**
 * Repository of indexes over feature structures. Use this interface to access previously defined
 * indexes.
 */
public interface FSIndexRepository {

  /**
   * Retrieve an index according to a label.
   * 
   * @param label
   *          The name of the index.
   * @return The index with the name <code>label</code>, or <code>null</code> if no such index
   *         is defined.
   */
  FSIndex getIndex(String label);

  /**
   * Retrieve an index according to a label and a type. The type is used to narrow down the index of
   * a more general type to a more specific one.
   * 
   * @param label
   *          The name of the index.
   * @param type
   *          A subtype of the type of the index.
   * @return The specified, or <code>null</code> if an index with that name doesn't exist.
   * @exception CASRuntimeException When <code>type</code> is not a subtype of the index's type.
   */
  FSIndex getIndex(String label, Type type) throws CASRuntimeException;

  /**
   * Get all labels for all indexes.
   * 
   * @return All labels.
   */
  Iterator getLabels();

  /**
   * Get all indexes in this repository.
   * 
   * @return All indexes.
   */
  Iterator getIndexes();

  /**
   * Add a feature structure to all appropriate indexes in the repository. If no indexes exist for
   * the type of FS that you are adding, then a bag (unsorted) index will be automatically created.
   * <p>
   * <b>Important</b>: after you have called <code>addFS()</code> on a FS, do not change the
   * values of any features used for indexing. If you do, the index will become corrupted and may be
   * unusable. If you need to change an index feature value, first call
   * {@link #removeFS(FeatureStructure) removeFS()} on the FS, change the feature values, then call
   * <code>addFS()</code> again.
   * 
   * @param fs
   *          The FS to be added.
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  void addFS(FeatureStructure fs);

  /**
   * Remove a feature structure from all indexes in the repository.
   * 
   * @param fs
   *          The FS to be removed.
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  void removeFS(FeatureStructure fs);

  /**
   * Gets an iterator over all indexed FeatureStructures of the specified Type (and any of its
   * subtypes).
   * <p>
   * Limitation: If there are no sorted or bag indexes defined for this type, but there is more than
   * one set index defined, then this method will only return the contents of one of these set
   * indexes (chosen arbitrarily).
   * 
   * @param aType
   *          The type
   * 
   * @return An iterator that returns all indexed FeatureStructures of type <code>aType</code>,
   *         in no particular order.
   */
  FSIterator getAllIndexedFS(Type aType);
}
