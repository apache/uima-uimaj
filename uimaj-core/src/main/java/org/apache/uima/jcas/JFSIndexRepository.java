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

package org.apache.uima.jcas;

import java.util.Iterator;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;

/**
 * Provides the same function as the FSIndexRepository except that the variants that take a "type"
 * argument take type arguments obtainable easily from the JCas type.
 * 
 */

// * like FSIndexRepository - except
// * - has method variants that take int args as the "type"
// * - has the annotation index
// * - doesn't have the addFS, removeFS methods
public interface JFSIndexRepository {

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
   *          The name of the index
   * @param type
   *          A subtype of the type of the index, written as Foo.type
   * @return The specified index, or <code>null</code> if an index with that name doesn't exist,
   *         or it exists but <code>type</code> is not a subtype of the index's type.
   */
  FSIndex getIndex(String label, int type);

  /**
   * Get the standard annotation index.
   * 
   * @return The standard annotation index.
   */
  FSIndex getAnnotationIndex();

  /**
   * Get the standard annotation index restricted to a specific annotation type.
   * 
   * @param type
   *          The annotation type the index is restricted to, written as Foo.type
   * @return The standard annotation index, restricted to <code>type</code>.
   */
  FSIndex getAnnotationIndex(int type);

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
   * Get the underlying FSIndexRepository associated with this JFSIndexRepository.
   * 
   * @return The associated FSIndexRepository.
   */
  FSIndexRepository getFSIndexRepository();

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

  /**
   * Gets an iterator over all indexed FeatureStructures of the specified Type (and any of its
   * subtypes).
   * <p>
   * Limitation: If there are no sorted or bag indexes defined for this type, but there is more than
   * one set index defined, then this method will only return the contents of one of these set
   * indexes (chosen arbitrarily).
   * 
   * @param aType
   *          The type obtained by doing MyJCasClass.type
   * 
   * @return An iterator that returns all indexed FeatureStructures of type <code>aType</code>,
   *         in no particular order.
   */
  FSIterator getAllIndexedFS(int aType);

}
