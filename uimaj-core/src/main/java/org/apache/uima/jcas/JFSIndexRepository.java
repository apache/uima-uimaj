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
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

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
   * @param <T> the Java class corresponding to the top most type defined by this index
   * @return The index with the name <code>label</code>, or <code>null</code> if no such index
   *         is defined.
   */
  <T extends TOP> FSIndex<T> getIndex(String label);

  /**
   * Retrieve an index according to a label and a type. The type is used to narrow down the index of
   * a more general type to a more specific one.
   * 
   * @param label
   *          The name of the index
   * @param type
   *          A subtype of the type of the index, written as Foo.type
   * @param <T> the Java class corresponding to the type
   * @return The specified index, or <code>null</code> if an index with that name doesn't exist,
   *         or it exists but <code>type</code> is not a subtype of the index's type.
   */
  <T extends TOP> FSIndex<T> getIndex(String label, int type);

  /**
   * Get the standard annotation index.
   * 
   * @return The standard annotation index.
   */
  AnnotationIndex<Annotation> getAnnotationIndex();

  /**
   * Get the standard annotation index restricted to a specific annotation type.
   * 
   * @param type
   *          The annotation type the index is restricted to, written as Foo.type
   * @param <T> the Java class corresponding to type
   * @return The standard annotation index, restricted to <code>type</code>.
   */
  <T extends Annotation> AnnotationIndex<T> getAnnotationIndex(int type);

  /**
   * Get all labels for all indexes.
   * 
   * @return All labels.
   */
  Iterator<String> getLabels();

  /**
   * Get all indexes in this repository.
   * @return All indexes.
   */
  Iterator<FSIndex<TOP>> getIndexes();

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
   * Generics:  The returned Java cover class may not be a JCas one.
   * 
   * @param aType
   *          The type
   * @param <T> the Java Class corresponding to aType
   * @return An iterator that returns all indexed FeatureStructures of type <code>aType</code>,
   *         in no particular order.
   */
  <T extends TOP> FSIterator<T> getAllIndexedFS(Type aType);

  /**
   * Gets an iterator over all indexed FeatureStructures of the specified Type (and any of its
   * subtypes).
   * <p>
   * Limitation: If there are no sorted or bag indexes defined for this type, but there is more than
   * one set index defined, then this method will only return the contents of one of these set
   * indexes (chosen arbitrarily).
   * 
   * Generics:  The returned Java cover class may not be a JCas one.
   * 
   * @param aType
   *          The type obtained by doing MyJCasClass.type
   * @param <T> the Java Class corresponding to aType
   * @return An iterator that returns all indexed FeatureStructures of type <code>aType</code>,
   *         in no particular order.
   */
  <T extends TOP> FSIterator<T> getAllIndexedFS(int aType);

}
