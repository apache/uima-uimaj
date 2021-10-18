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
package org.apache.uima.internal.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CopyOnWriteIndexPart;

/**
 * implements ObjHashSet partially, for iterator use
 */

public class CopyOnWriteObjHashSet<T extends FeatureStructure> implements CopyOnWriteIndexPart<T> {

  private ObjHashSet<T> ohs;

  private ObjHashSet<T> original;

  private final int original_size;

  public CopyOnWriteObjHashSet(ObjHashSet<T> original) {
    this.ohs = original;
    this.original = original;
    this.original_size = original.size();
  }

  /**
   * Called by index when about to make an update
   */
  @Override
  public void makeReadOnlyCopy() {
    ohs = new ObjHashSet<>(ohs, true); // true - read-only copy
  }

  // ***************************************************
  // These methods to make this class easily usable by *
  // FsIterator_bag *
  // ***************************************************

  /**
   * @param obj
   *          the object to find in the table (if it is there)
   * @return the position of obj in the table, or -1 if not in the table
   */
  public int find(T obj) {
    return ohs.find(obj);
  }

  /**
   * For iterator use
   * 
   * @param index
   *          a magic number returned by the internal find
   * @return the T at that spot, or null if nothing there
   */
  public T get(int index) {
    return ohs.get(index);
  }

  /**
   * advance pos until it points to a non 0 or is 1 past end
   * 
   * @param pos
   *          -
   * @return updated pos
   */
  public int moveToNextFilled(int pos) {
    return ohs.moveToNextFilled(pos);
  }

  /**
   * decrement pos until it points to a non 0 or is -1
   * 
   * @param pos
   *          -
   * @return updated pos
   */
  public int moveToPreviousFilled(int pos) {
    return ohs.moveToPreviousFilled(pos);
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      /**
       * Keep this always pointing to a non-0 entry, or if not valid, outside the range
       */
      protected int curPosition = moveToNextFilled(0);

      @Override
      public final boolean hasNext() {
        return curPosition < getCapacity();
      }

      @Override
      public final T next() {
        if (curPosition >= getCapacity()) {
          throw new NoSuchElementException();
        }
        try {
          T r = get(curPosition);
          curPosition = moveToNextFilled(curPosition + 1);
          return r;
        } catch (ArrayIndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private int moveToPrevious(int position) {
        if (position >= getCapacity()) {
          return -1;
        }
        return moveToPreviousFilled(position - 1);
      }

    };
  }

  /**
   * if the fs is in the set, the iterator should return it. if not, return -1 (makes iterator
   * invalid)
   * 
   * @param fs
   *          position to this fs
   * @return the index if present, otherwise -1;
   */
  public int moveTo(FeatureStructure fs) {
    return ohs.moveTo(fs);
  }

  @Override
  public String toString() {
    return ohs.toString();
  }

  // /**
  // * @see ObjHashSet#getModificationCount()
  // * @return the modification count
  // */
  // public int getModificationCount() {
  // return ohs.getModificationCount();
  // }

  /**
   * @see ObjHashSet#getCapacity()
   * @return the capacity &gt;= size
   */
  public int getCapacity() {
    return ohs.getCapacity();
  }

  /**
   * @see ObjHashSet#size()
   * @return the size
   */
  @Override
  final public int size() {
    return original_size;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#isOriginal(java.lang.Object)
   */
  @Override
  public boolean isOriginal() {
    return ohs == original;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#copyToArray(org.apache.uima.jcas.cas.TOP[],
   * int)
   */
  @Override
  public int copyToArray(T[] target, int startingIndexInTarget) {
    Iterator<T> it = iterator();
    int i = startingIndexInTarget;
    while (it.hasNext()) {
      target[i++] = it.next();
    }
    return i;
  }

}
