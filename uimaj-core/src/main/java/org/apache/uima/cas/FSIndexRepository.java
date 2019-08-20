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

import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.LowLevelIndex;
import org.apache.uima.jcas.cas.TOP;

/**
 * Repository of indexes over feature structures. Use this interface to access previously defined
 * indexes.
 * 
 * Generics: T is the associated Java cover class for the top type defined for this index name.
 * If JCas is being used, it is that JCas class.  Otherwise it is the standard non-JCas Java cover class.
 * 
 */
public interface FSIndexRepository {

  /**
   * Retrieve an index according to a label.
   * 
   * Generics: T is the associated Java cover class for the top type defined for this index name.
   * 
   * @param label
   *          The name of the index.
   * @param <T> the Java class associated with the top-most type of the index
   * @return The index with the name <code>label</code>, or <code>null</code> if no such index
   *         is defined.
   */
  <T extends FeatureStructure> FSIndex<T> getIndex(String label);

  /**
   * Retrieve an index according to a label and a type. The type is used to narrow down the index of
   * a more general type to a more specific one.
   * 
   * Generics: T is the associated Java cover class for the type.
   * 
   * @param label
   *          The name of the index.
   * @param type
   *          A subtype of the type of the index.
   * @param <T> The Java class associated with the type
   * @return The specified, or <code>null</code> if an index with that name doesn't exist.
   * @exception CASRuntimeException When <code>type</code> is not a subtype of the index's type.
   */
  <T extends FeatureStructure> FSIndex<T> getIndex(String label, Type type) throws CASRuntimeException;
  
  /**
   * Get all labels for all indexes.
   * 
   * @return All labels.
   */
  Iterator<String> getLabels();

  /**
   * Get all indexes in this repository.
   * 
   * @param <T> the generic type of the FeatureStructures
   * @return All indexes.
   */
  <T extends FeatureStructure> Iterator<FSIndex<T>> getIndexes();

  /**
   * Get all indexes in this repository as low level indexes
   * 
   * @param <T> the generic type of the FeatureStructures
   * @return All indexes.
   */
  <T extends FeatureStructure> Iterator<LowLevelIndex<T>> ll_getIndexes();
  
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
   * @param <T> the generic type of the FeatureStructure
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  <T extends FeatureStructure> void addFS(T fs);

  /**
   * Remove a feature structure from all indexes in the repository.
   * 
   * @param fs
   *          The FS to be removed.  The fs must be the exact FS to remove, not just one
   *          which compares "equal" using the index's comparator.
   * @param <T> the generic type of the FeatureStructure
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  <T extends FeatureStructure> void removeFS(T fs);

  /**
   * Remove all instances of type, including all subtypes from all indexes in the repository view.
   * @param type the type to remove
   * @exception NullPointerException if the <code>type</code> parameter is <code>null</code>.
  */
  void removeAllIncludingSubtypes(Type type);
  
  /**
   * Remove all instances of type, including all subtypes from all indexes in the repository view.
   * @param clazz the JCas class of the type to remove.  To remove all use TOP.class
   * @param <T> the type to remove
   * @exception NullPointerException if the <code>clazz</code> parameter is <code>null</code>.
  */
  default <T extends TOP> void removeAllIncludingSubtypes(Class<T> clazz) {
    removeAllIncludingSubtypes(((FSIndexRepositoryImpl)this).getCasImpl().getJCasImpl().getCasType(clazz));
  }
  
  /**
   * Remove all instances of just this type, excluding subtypes, from all indexes in the repository view.
   * @param type the type to remove
   * @exception NullPointerException if the <code>type</code> parameter is <code>null</code>.
  */
  void removeAllExcludingSubtypes(Type type);

  /**
   * Remove all instances of just this type, excluding subtypes, from all indexes in the repository view.
   * @param clazz the JCas Class of the type to remove
   * @param <T> the type to remove
   * @exception NullPointerException if the <code>type</code> parameter is <code>null</code>.
  */
  default <T extends TOP> void removeAllExcludingSubtypes(Class<T> clazz) {
    removeAllExcludingSubtypes(((FSIndexRepositoryImpl)this).getCasImpl().getJCasImpl().getCasType(clazz));
  }

  /**
   * Gets an iterator over all indexed (in this View) FeatureStructures of the specified Type (and any of its
   * subtypes).  The elements are returned in arbitrary order.
   *
   * Generics: T is the Java class for aType.
   * @param aType
   *          The type
   * @param <T> The Java class associated with aType
   * @return An iterator that returns all indexed FeatureStructures of type <code>aType</code>
   *         and its subtypes, in no particular order.
   */
  <T extends FeatureStructure> FSIterator<T> getAllIndexedFS(Type aType);
  
  /**
   * Gets an FSIterator over all indexed (in this view) FeatureStructures of the specified Type (and any of its
   * subtypes).  The elements are returned in arbitrary order.
   *
   * Generics: T is the Java class for aType.
   * @param clazz
   *          The JCas class corresponding to the type
   * @param <T> The Java class associated with aType
   * @return An iterator that returns all indexed FeatureStructures of the specified type
   *         and its subtypes, in no particular order.
   */
  default <T extends FeatureStructure> FSIterator<T> getAllIndexedFS(Class<T> clazz) {
    return getAllIndexedFS(((FSIndexRepositoryImpl)this).getCasImpl().getCasType((Class<TOP>) clazz));
  }
  
  /**
   * Returns an unmodifiable collection of all the FSs of this type and its subtypes, 
   * that are indexed in this view, in an arbitrary order.  
   * Subsequent modifications to the indexes do not affect this collection.
   * @param type the type of Feature Structures to include (including subtypes)
   * @param <T> The Java class associated with type
   * @return an unmodifiable, unordered collection of all indexed (in this view) Feature Structures
   *         of the specified type (including subtypes)
   */
  public <T extends TOP> Collection<T> getIndexedFSs(Type type);
  
  /**
   * Returns an unmodifiable collection of all the FSs of this type and its subtypes, 
   * that are indexed in this view, in an arbitrary order.  
   * Subsequent modifications to the indexes do not affect this collection.
   * @param clazz
   *          The JCas class corresponding to the type
   * @param <T> The Java class associated with type
   * @return an unmodifiable, unordered collection of all indexed (in this view) Feature Structures
   *         of the specified type (including subtypes)
   */
  public <T extends TOP> Collection<T> getIndexedFSs(Class<T> clazz);
 
  /**
   * Returns an unmodifiable collection of all of the FSs
   * that are indexed in this view, in an arbitrary order.  
   * Subsequent modifications to the indexes do not affect this collection.
   * @param <T> The Java class associated with type
   * @return an unmodifiable, unordered collection of all indexed (in this view) Feature Structures
   *         of the specified type (including subtypes)
   */
  public <T extends TOP> Collection<T> getIndexedFSs();

}
