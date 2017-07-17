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

import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.impl.LowLevelIterator;

/**
 * Iterator over feature structures.
 * 
 * <p>
 * This iterator interface extends {@link java.util.Iterator}, and supports the
 * standard <code>hasNext</code> and <code>next</code> methods. If finer control, including
 * reverse iteration, is needed, see below.
 * 
 * <p>Note: do not use the APIs described below *together* with the standard Java iterator methods
 * <code>next()</code> and <code>hasNext()</code>.  On any given iterator, use either the one or the
 * other, but not both together.  Otherwise, <code>next/hasNext</code> may exhibit incorrect
 * behavior.
 * 
 * <p>
 * The <code>FSIterator</code> interface introduces the methods {@link #get()},
 * {@link #moveToNext()}, {@link #moveToPrevious()} methods. With these methods, retrieving the
 * current element (<code>get</code>) is a separate operation from moving the iterator (<code>moveToNext</code>
 * and <code>moveToPrevious</code>. This makes the user's code less compact, but allows for finer
 * control.
 * 
 * <p>
 * Specifically the <code>get</code> method is defined to return the same element that a call to
 * <code>next()</code> would return, but does not advance the iterator.
 * 
 * <p>
 * Implementations of this interface are not required to be fail-fast. That is, if the iterator's
 * collection is modified, the effects on the iterator are in general undefined. Some collections
 * may handle this more gracefully than others, but in general, concurrent modification of the
 * collection you're iterating over is a bad idea.
 * 
 * <p>
 * If the iterator is moved past the boundaries of the collection, the behavior of subsequent calls
 * to {@link FSIterator#moveToNext() moveToNext()} or
 * {@link FSIterator#moveToPrevious() moveToPrevious()} is undefined. For example, if a previously
 * valid iterator is invalidated by a call to {@link FSIterator#moveToNext() moveToNext()}, a
 * subsequent call to {@link FSIterator#moveToPrevious() moveToPrevious()} is not guaranteed to set
 * the iterator back to the last element in the collection. Always use
 * {@link FSIterator#moveToLast() moveToLast()} in such cases.
 * 
 * 
 * 
 */
public interface FSIterator<T extends FeatureStructure> extends ListIterator<T> {

  /**
   * Check if this iterator is valid.
   * 
   * @return <code>true</code> if the iterator is valid.
   */
  boolean isValid();

  /**
   * Get the structure the iterator is pointing at.
   * 
   * @return The structure the iterator is pointing at.
   * @exception NoSuchElementException
   *              If the iterator is not valid.
   */
  T get() throws NoSuchElementException;

  /**
   * Get the structure the iterator is pointing at.
   * Throws various unchecked exceptions, if the iterator is not valid
   * @return The structure the iterator is pointing at.
   */
  default T getNvc() {
    return get();
  }
  
  /**
   * Advance the iterator. This may invalidate the iterator.
   * @exception ConcurrentModificationException if the underlying indexes being iterated over were modified
   */
  void moveToNext();

  /**
   * version of moveToNext which bypasses the isValid check - call only if you've just done this check yourself
   */
  default void moveToNextNvc() {
    moveToNext();
  }
  
  /**
   * Move the iterator one element back. This may invalidate the iterator.
   * @exception ConcurrentModificationException if the underlying indexes being iterated over were modified
   */
  void moveToPrevious();

  /**
   * version of moveToPrevious which bypasses the isValid check - call only if you've just done this check yourself
   */
  default void moveToPreviousNvc() {
    moveToPrevious();
  }

  /**
   * Move the iterator to the first element. The iterator will be valid iff the underlying
   * collection is non-empty.  Allowed even if the underlying indexes being iterated over were modified.
   */
  void moveToFirst();

  /**
   * Move the iterator to the last element. The iterator will be valid iff the underlying collection
   * is non-empty.  Allowed even if the underlying indexes being iterated over were modified.
   */
  void moveToLast();

  /**
   * Move the iterator to the first Feature Structure that is equal to <code>fs</code>. 
   * First means the earliest one occurring in the index, in case multiple FSs that are "equal" to fs
   * are in the index.  If no
   * such feature structure exists in the underlying collection, set the iterator to the "insertion
   * point" for <code>fs</code>, i.e., to a point where the current feature structure is greater
   * than <code>fs</code>, and the previous one is less than <code>fs</code>.
   * <p>
   * If the fs is greater than all of the entries in the index, the moveTo cannot set the iterator to an insertion point
   * where the current feature structure is greater than fs, so it marks the iterator "invalid".
   * <p>
   * If the underlying index is a bag index, no ordering is present, and the moveTo operation moves to the
   * fs which is the same identical fs as the key. If no such fs is in the index, the iterator is marked 
   * invalid.
   * 
   * @param fs
   *          The feature structure the iterator that supplies the 
   *          comparison information.  It can be a supertype of T as long as it can supply the keys needed.
   *          A typical example is a subtype of Annotation, and using an annotation instance to specify 
   *          the begin / end.
   */
   void  moveTo(FeatureStructure fs);

  /**
   * Copy this iterator.
   * 
   * @return A copy of this iterator, pointing at the same element.
   */
  FSIterator<T> copy();
  
  /**
   * @return the type this iterator is over
   */
  default Type getType() {
    return ((LowLevelIterator<T>)this).ll_getIndex().getType();
  }

  /*****************************************************
   * DEFAULT implementations of Iterator interface
   * in terms of FSIterator methods
   *****************************************************/
  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  default boolean hasNext() {
    return isValid();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  default T next() {
    T result = get();
    moveToNext();
    return result;
  }
  
  default T nextNvc() {
    T result = getNvc();
    moveToNextNvc();
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  default void remove() {
    throw new UnsupportedOperationException();
  } 
  
  @Override
  default boolean hasPrevious() {
    return isValid();
  }

  @Override
  default T previous() {
    T result = get();
    moveToPrevious();
    return result;

  }

  @Override
  default int nextIndex() {
    throw new UnsupportedOperationException();
  }

  @Override
  default int previousIndex() {
    throw new UnsupportedOperationException();
  }

  @Override
  default void set(T e) {
    throw new UnsupportedOperationException();
    
  }

  @Override
  default void add(T e) {
    throw new UnsupportedOperationException();
    
  }
  
  /**
   * Don't use this directly, use select()... spliterator instead where possible.
   * Otherwise, insure the FSIterator instance can support sized/subsized.
   * @return a split iterator for this iterator, which has the following characteristics
   *   DISTINCT, SIZED, SUBSIZED
   *   
   */
  default Spliterator<T> spliterator() {
    return Spliterators.spliterator(
        this, 
        ((LowLevelIterator<T>)this).ll_indexSize(), 

        Spliterator.DISTINCT |
        Spliterator.SIZED    |
        Spliterator.SUBSIZED);
  }
  
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(),  false);
  }

  
}
