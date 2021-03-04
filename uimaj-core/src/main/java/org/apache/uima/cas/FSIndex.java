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
import java.util.stream.Stream;

import org.apache.uima.cas.impl.LowLevelIndex;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.cas.TOP;

/**
 * Feature structure index access interface.
 * 
 * <p>
 * Notice that each feature structure index uses its own ordering relation that will usually be
 * different from index to index. In particular, equality with respect to the index ordering will in
 * general not imply object identity for the feature structures.
 * 
 * <p>
 * We currently support three different kinds of indexes: sorted, set and bag indexes. The default
 * index is a sorted index. In a sorted index, FSs that are committed (added to the indexes) 
 * are added, unless they are duplicates of already existing FSs in the index.  
 * Multiple different instances of FSs which compare equal may all be in the index, individually.
 * 
 * <p>  
 * The index is sorted in the sense that iterators will
 * output FSs in sorted order according to the comparator for that index. The order of FSs that are
 * equal with respect to the comparator is arbitrary but fixed. That is, if you iterate over the same index
 * several times, you will see the same relative order of FSs every time. We also guarantee that
 * reverse iterators will produce exactly the reverse sequence of forward iteration.
 * 
 * <p>
 * A set index will contain no duplicates of the same type, where a duplicate is defined by the
 * indexing comparator. That is, if you commit two feature structures of the same type that are
 * equal with respect to the indexing comparator, only the first one will be entered into the index. Note that
 * you can still have duplicates with respect to the indexing order if they are of a different type;
 * different types are never "equal". A set index is not in any particular sort order.
 * 
 * <p>
 * A bag index finally simply stores everything, without any guaranteed order. 
 * Operations that depend on comparison, such as find(FeatureStructure fs) or
 * FSIterator.moveTo(FeatureStructure fs) only compare for equality using FeatureStructure identity,
 * since no ordering relationship is possible with bag indexes.
 * 
 * <p>
 * Indexes have a top-most type; let's call it T.  They store only instances of that type or its subtypes.
 * A given index definition specifies this top-most type.  The APIs for obtaining an index
 * include the ability to specify, in addition, a type T2 which is a subtype of T; indexes obtained 
 * using this will have only instances of type T2 or its subtypes.
 * 
 * @param <T> the topmost type in this Index
 */
public interface FSIndex<T extends FeatureStructure> extends Collection<T> {

  /**
   * Indexing strategy: sorted index. A sorted index contains all elements, including duplicates.
   * Iterators over sorted indexes will return elements in sorted order.
   */
  public static final int SORTED_INDEX = 0;

  /**
   * Indexing strategy: set index. A set index contains no duplicates of the same type, where a
   * duplicate is defined by the indexing comparator. A set index is not guaranteed to be sorted.
   */
  public static final int SET_INDEX = 1;

  /**
   * Indexing strategy: bag index. A bag index contains all elements, in no particular order.
   */
  public static final int BAG_INDEX = 2;
  
  /**
   * Special indexes used by the framework to implement
   * {@link FSIndexRepository#getAllIndexedFS(Type)}.  Not user-definable.
   */
  public static final int DEFAULT_BAG_INDEX = 3;
  
//  /**
//   * Special indexes used by the framework to implement flattened indexes
//   * NOT CURRENTLY IN USE
//   * Not user-definable
//   */
//  public static final int FLAT_INDEX = 4;
  
  /**
   * Return the number of feature structures in this index.
   * 
   * @return The number of FSs in this index.
   */
  int size();

  /**
   * Return the type of feature structures this index contains.
   * 
   * @return The type of feature structures in this index.
   */
  Type getType();

  /**
   * <p>Check if the index contains an element equal to the given feature structure 
   * according to the comparators defined for this index.  For bag indexes (which
   * have no comparators), the equality test means the identical feature structure.
   * Note that this is in general not the same as feature structure identity.</p>
   * 
   * <p>The element is used as a template, and may be a supertype of the type of the index,
   * as long as the keys specified for this index can be accessed.
   * 
   * @param fs A Feature Structure used a template to match for equality with the
   *           FSs in the index.
   * @return <code>true</code> if the index contains such an element.
   */
  boolean contains(FeatureStructure fs);

  /**
   * Find an entry in the index "equal to" the given feature structure according to the comparators specified
   * for this index.  Note that this is in general not the same as feature structure identity.  For BAG
   * indexes, it is identity, for others it means the found feature structure compares 
   * equal with the parameter in terms of the defined comparators for the index.
   * 
   * If there are multiple different FSs in the index which compare equal with the 
   * given feature structure, an arbitrary one is returned. This differs from the moveTo(fs)
   * operation which guarantees to move to the first feature structure occurring in the index
   * in this case.
   * 
   * @param fs A Feature Structure used a template to match with the Feature Structures in the index.
   *           It must have the keys needed to do the compare as specified for the index that it's in.
   * @return A FS equal to the template argument, or <code>null</code> if no such FS exists.
   * @see FSIterator#moveTo(FeatureStructure)
   */
  T find(FeatureStructure fs);

  /**
   * Compare two feature structures according to the ordering relation of the index. If the input
   * feature structures are not of the type of the index or a supertype, the result is undefined.
   * 
   * Because the indexes compare might use only features defined in supertypes, the arguments being 
   * compared could be supertypes of the indexed type.
   * 
   * @param fs1 the first  Feature Structure to compare
   * @param fs2 the second Feature Structure to compare
   * @return <code>-1</code> if <code>fs1 &lt; fs2</code>; <code>0</code> if
   *         <code>fs1 = fs2</code>; <code>1</code> else.
   */
  int compare(FeatureStructure fs1, FeatureStructure fs2);

  /**
   * Return an iterator over the index. The position of the iterator will be set such that the
   * feature structure returned by a call to the iterator's {@link FSIterator#get() get()} method is
   * greater than or equal to <code>fs</code>, and any previous FS is less than <code>FS</code>
   * (the iterator is positioned at the earliest of equal values).
   * If no such position exists, the iterator will be invalid.
   * 
   * @param fs
   *          A feature structure template (may be a supertype of T) having keys used in the index compare function,
   *          specifying where to initially position the iterator.
   * @return An iterator positioned at <code>fs</code>, if it exists; else, an invalid iterator.
   */
  default FSIterator<T> iterator(FeatureStructure fs) {
    FSIterator<T> it = iterator();
    if (fs != null) {
      it.moveTo(fs);
    }
    return it;
  }

  /**
   * Return an iterator over the index. The position of the iterator will be set to 
   * return the first item in the index.
   * If the index is empty, the iterator position will be marked as invalid.
   * 
   * @return An FSIterator positioned at the beginning, or an invalid iterator.
   */
  @Override
  FSIterator<T> iterator();
  
  /**
   * Return the indexing strategy.
   * 
   * @return One of <code>SORTED_INDEX</code>, <code>BAG_INDEX</code> or <code>SET_INDEX</code>.
   */
  int getIndexingStrategy();

  /**
   * Creates a shared copy of this FSIndex configured to produce snapshot iterators
   * that don't throw ConcurrentModificationExceptions.
   * 
   * @return a light-weight copy of this FSIndex, configured such that
   * any iterator created using it will be a snapshot iterator - one where 
   * a snapshot is made of the state of the index at the time the iterator
   * is created, and where subsequent modifications to the underlying index
   * are allowed, but don't affect the iterator (which iterates over the
   * read-only snapshot).  Iterators produced with this won't throw
   * ConcurrentModificationExceptions.
   */
  FSIndex<T> withSnapshotIterators();

  /**
    * @return a newly created selection object for accessing feature structures
   */
 SelectFSs<T> select();

  /**
   * @param type specifies the type (and subtypes of that type) to access
   * @param <N> the Type of the elements being accessed
   * @return a newly created selection object for accessing feature structures of that type and its subtypes
   */
  <N extends T> SelectFSs<N> select(Type type);
  
  /**
   * @param clazz a JCas class corresponding to the type (and subtypes of that type) to access
   * @param <N> the Type of the elements being accessed
   * @return a newly created selection object for accessing feature structures of that type and its subtypes
   */
  <N extends T> SelectFSs<N> select(Class<N> clazz);
  
  /**
   * @param jcasType the "type" field from the JCas class corresponding to the type (and subtypes of that type) to access
   * @param <N> the Type of the elements being accessed
   * @return a newly created selection object for accessing feature structures of that type and its subtypes
   */
  <N extends T> SelectFSs<N> select(int jcasType);
  
  /**
   * @param fullyQualifiedTypeName the string name of the type to access
   * @param <N> the Type of the elements being accessed
   * @return a newly created selection object for accessing feature structures of that type and its subtypes
   */
  <N extends T> SelectFSs<N> select(String fullyQualifiedTypeName);
  
  /**
   * @return a Stream over all the elements in the index (including subtypes)
   */
  default Stream<T> stream() {
    return this.select();
  }
  
  /**
   * @param clazz - the subtype
   * @param <U> the subtype
   * @return an instance of this index specialized to a subtype
   */
  default <U extends T> FSIndex<U> subType(Class<? extends TOP> clazz) {
    return ((LowLevelIndex<T>)this).getSubIndex(clazz);
  }
  
  /**
   * @param type - the subtype
   * @param <U> the subtype
   * @return an instance of this index specialized to a subtype
   */
  default <U extends T> FSIndex<U> subType(Type type) {
    return ((LowLevelIndex<T>)this).getSubIndex(type);
  }
  
}
