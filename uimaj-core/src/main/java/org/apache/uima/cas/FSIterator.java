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

import org.apache.uima.cas.impl.LowLevelIndex;
import org.apache.uima.cas.impl.LowLevelIterator;
import org.apache.uima.cas.impl.TypeSystemImpl;

/**
 * Iterator over feature structures.
 * 
 * <p>
 * This iterator interface extends {@link java.util.ListIterator} which, in turn, extends
 * {@link java.util.Iterator}. It supports all the methods of those APIs except nextIndex,
 * previousIndex, set, and add. remove meaning is changed to mean remove the item obtained by a
 * get() from all the indexes in this view. If finer control, including reverse iteration, is
 * needed, see below.
 * 
 * <p>
 * Note: do not use the APIs described below *together* with the standard Java iterator methods
 * <code>next()</code> and <code>hasNext()</code>. On any given iterator, use either the one or the
 * other, but not both together. Otherwise, <code>next/hasNext</code> may exhibit incorrect
 * behavior.
 * 
 * <p>
 * The <code>FSIterator</code> interface introduces the methods {@link #get()},
 * {@link #moveToNext()}, {@link #moveToPrevious()} methods. With these methods, retrieving the
 * current element (<code>get</code>) is a separate operation from moving the iterator
 * (<code>moveToNext</code> and <code>moveToPrevious</code>. This makes the user's code less
 * compact, but allows for finer control.
 * 
 * <p>
 * Specifically the <code>get</code> method is defined to return the same element that a call to
 * <code>next()</code> would return, but does not advance the iterator.
 * 
 * <p>
 * If the iterator's underlying UIMA Indexes are modified, the iterator continues as if it doesn't
 * see these modifications. Three operations cause the iterator to "see" any modifications:
 * moveToFirst, moveToLast, and moveTo(featureStructure).
 * 
 * <p>
 * If the iterator is moved past the boundaries of the collection, the behavior of subsequent calls
 * to {@link FSIterator#moveToNext() moveToNext()} or {@link FSIterator#moveToPrevious()
 * moveToPrevious()} is undefined. For example, if a previously valid iterator is invalidated by a
 * call to {@link FSIterator#moveToNext() moveToNext()}, a subsequent call to
 * {@link FSIterator#moveToPrevious() moveToPrevious()} is not guaranteed to set the iterator back
 * to the last element in the collection. Always use {@link FSIterator#moveToLast() moveToLast()} in
 * such cases.
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
  default T get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return getNvc();
  }

  /**
   * Get the structure the iterator is pointing at. Throws various unchecked exceptions, if the
   * iterator is not valid
   * 
   * @return The structure the iterator is pointing at.
   */
  T getNvc();

  /**
   * Advance the iterator. This may invalidate the iterator.
   */
  default void moveToNext() {
    if (!isValid()) {
      return;
    }
    moveToNextNvc();
  }

  /**
   * version of moveToNext which bypasses the isValid check - call only if you've just done this
   * check yourself
   */
  void moveToNextNvc();

  /**
   * Move the iterator one element back. This may invalidate the iterator.
   * 
   * @exception ConcurrentModificationException
   *              if the underlying indexes being iterated over were modified
   */
  default void moveToPrevious() {
    if (!isValid()) {
      return;
    }
    moveToPreviousNvc();
  }

  /**
   * version of moveToPrevious which bypasses the isValid check - call only if you've just done this
   * check yourself
   */
  void moveToPreviousNvc();

  /**
   * Move the iterator to the first element. The iterator will be valid iff the underlying
   * collection is non-empty. Allowed even if the underlying indexes being iterated over were
   * modified.
   */
  void moveToFirst();

  /**
   * Move the iterator to the last element. The iterator will be valid iff the underlying collection
   * is non-empty. Allowed even if the underlying indexes being iterated over were modified.
   */
  void moveToLast();

  /**
   * Move the iterator to the first Feature Structure that matches the <code>fs</code>. First means
   * the earliest one occurring in the index, in case multiple FSs matching the fs are in the index.
   * If no such feature structure exists in the underlying collection, and the iterator is over a
   * sorted index, set the iterator to the "insertion point" for <code>fs</code>, i.e., to a point
   * where the current feature structure compares greater than <code>fs</code>, and the previous one
   * compares less than <code>fs</code>, using this sorted index's comparator.
   * <p>
   * If the fs is greater than all of the entries in the index, the moveTo cannot set the iterator
   * to an insertion point where the current feature structure is greater than fs, so it marks the
   * iterator "invalid".
   * 
   * <p>
   * If the underlying index is a set or bag index, or an unordered form of iteration is configured
   * (for example using the <code>select</code> API, no ordering is present, and the moveTo
   * operation moves to a matching item, if one exists. The match is done using the index's
   * comparator. If none exist, the index is left if possible in some valid (but non-matching)
   * position.
   * 
   * <p>
   * When the iterator is over a sorted index whose keys include the typeOrder key, this can cause
   * unexpected operation, depending on type priorities. For example, consider the Annotation Index,
   * which includes this key. If there are many indexed instances of the type "Foo" with the same
   * begin and end, and a moveTo operation is specified using an Annotation instance with the same
   * begin and end, then the Foo elements might or might not be seen going forwards, depending on
   * the relative type priorities of "Foo" and "Annotation".
   * 
   * <p>
   * If you are not making use of typeOrdering, the "select" APIs can create iterators which will
   * ignore the typeOrdering key when doing the moveTo operation, which will result in all the
   * instances of type "Foo" being seen going forwards, independent of the type priorities. See the
   * select documentation in the version 3 users guide.
   * 
   * @param fs
   *          The feature structure the iterator that supplies the comparison information. It
   *          doesn't need to be in the index; it is just being used as a comparison template. It
   *          can be a supertype of T as long as it can supply the keys needed. A typical example is
   *          a subtype of Annotation, and using an annotation instance to specify the begin / end.
   */
  void moveTo(FeatureStructure fs);

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
    LowLevelIndex<T> idx = ((LowLevelIterator<T>) this).ll_getIndex();
    return (null == idx) // happens with a low level empty index, maybe wrapped by others
            ? TypeSystemImpl.staticTsi.getTopType()
            : idx.getType();
  }

  /*****************************************************
   * DEFAULT implementations of Iterator interface in terms of FSIterator methods
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
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return nextNvc();
  }

  default T nextNvc() {
    T result = getNvc();
    moveToNextNvc();
    return result;
  }

  @Override
  default boolean hasPrevious() {
    return isValid();
  }

  @Override
  default T previous() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return previousNvc();
  }

  default T previousNvc() {
    moveToPreviousNvc();
    return getNvc();
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
   * Don't use this directly, use select()... spliterator instead where possible. Otherwise, insure
   * the FSIterator instance can support sized/subsized.
   * 
   * @return a split iterator for this iterator, which has the following characteristics DISTINCT,
   *         SIZED, SUBSIZED
   * 
   */
  default Spliterator<T> spliterator() {
    return Spliterators.spliterator(this,
            ((LowLevelIterator<T>) this).ll_indexSizeMaybeNotCurrent(),

            Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.SUBSIZED);
  }

  /**
   * @return a Stream consisting of the items being iterated over by this iterator, starting from
   *         the current position.
   */
  default Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Removes from all the indexes associated with this view, the "current" Feature Structure (the
   * one that would be returned by a "get()" operation).
   *
   * @throws NoSuchElementException
   *           if the iterator is invalid.
   */
  @Override
  default void remove() {
    ((LowLevelIterator<T>) this).ll_remove();
  }

  /**
   * return the size of the collection being iterated over, if available. Because the iterator can
   * move forwards and backwards, the size is the total size that the iterator would iterate over,
   * starting at the first element thru the last element. This may be inefficient to compute.
   * 
   * @return the size of the collection being iterated over.
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#size()
   */
  default int size() {
    FSIterator<T> it = copy();
    it.moveToFirst();
    int count = 0;
    while (it.isValid()) {
      count++;
      it.nextNvc();
    }
    return count;
  }

}
