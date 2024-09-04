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
package org.apache.uima;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;

import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.util.impl.Constants;

/**
 * a List API that returns ints instead of T
 * 
 * @deprecated To become package private in {@code org.apache.uima.jcas.cas} or possibly removed.
 *             Should also be renamed.
 * @forRemoval 4.0.0
 */
@Deprecated(since = "3.6.0")
public interface List_of_ints extends Iterable<Integer> {

  int size();

  default boolean isEmpty() {
    return size() == 0;
  };

  default boolean contains(int i) {
    return indexOf(i) != -1;
  }

  int[] toArray();

  /**
   * Avoid copying, return the original array, if start/end offsets not in use
   * 
   * @return -
   */
  int[] toArrayMinCopy();

  boolean add(int i);

  boolean remove(int i);

  void clear();

  int get(int index);

  int set(int index, int element);

  void add(int index, int element);

  int removeAtIndex(int index);

  int indexOf(int i);

  int lastIndexOf(int i);

  List_of_ints subList(int fromIndex, int toIndex);

  @Override
  OfInt iterator();

  IntListIterator intListIterator();

  void copyFromArray(int[] src, int srcPos, int destPos, int length);

  void copyToArray(int srcPos, int[] dest, int destPos, int length);

  void sort();

  static List_of_ints EMPTY_LIST() {
    return new List_of_ints() {

      @Override
      public int[] toArray() {
        return Constants.EMPTY_INT_ARRAY;
      }

      @Override
      public int[] toArrayMinCopy() {
        return Constants.EMPTY_INT_ARRAY;
      }

      @Override
      public List_of_ints subList(int fromIndex, int toIndex) {
        throw new IndexOutOfBoundsException();
      }

      @Override
      public int size() {
        return 0;
      }

      @Override
      public int set(int index, int element) {
        throw new IndexOutOfBoundsException();
      }

      @Override
      public int removeAtIndex(int index) {
        throw new IndexOutOfBoundsException();
      }

      @Override
      public boolean remove(int i) {
        return false;
      }

      @Override
      public int lastIndexOf(int i) {
        return -1;
      }

      @Override
      public int indexOf(int i) {
        return -1;
      }

      @Override
      public int get(int index) {
        throw new IndexOutOfBoundsException();
      }

      @Override
      public boolean contains(int i) {
        return false;
      }

      @Override
      public void clear() {
      }

      @Override
      public void add(int index, int element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean add(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public OfInt iterator() {
        return new OfInt() {
          @Override
          public boolean hasNext() {
            return false;
          }

          @Override
          public Integer next() {
            throw new NoSuchElementException();
          }

          @Override
          public int nextInt() {
            throw new NoSuchElementException();
          }
        };
      }

      @Override
      public IntListIterator intListIterator() {
        return new IntListIterator() {
          @Override
          public boolean hasNext() {
            return false;
          }

          @Override
          public int nextNvc() {
            throw new IllegalStateException();
          }

          @Override
          public boolean hasPrevious() {
            return false;
          }

          @Override
          public int previousNvc() {
            throw new IllegalStateException();
          }

          @Override
          public void moveToStart() {
          }

          @Override
          public void moveToEnd() {
          }
        };
      }

      @Override
      public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void sort() {
      };
    };
  }

  static List_of_ints newInstance(int[] ia) {
    return newInstance(ia, 0, ia.length);
  }

  static List_of_ints newInstance(final int[] ia, final int start, final int end) {
    return new List_of_ints() {

      @Override
      public int size() {
        return end - start;
      }

      @Override
      public int[] toArray() {
        return Arrays.copyOfRange(ia, start, end);
      }

      @Override
      public int[] toArrayMinCopy() {
        return (start == 0 && end == size()) ? ia : toArray();
      }

      @Override
      public boolean add(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int get(int index) {
        return ia[start + index];
      }

      @Override
      public int set(int index, int element) {
        int r = get(start + index);
        ia[start + index] = element;
        return r;
      }

      @Override
      public void add(int index, int element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int removeAtIndex(int index) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int indexOf(int e) {
        for (int i = start; i < end; i++) {
          if (e == ia[i]) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public int lastIndexOf(int e) {
        for (int i = end - 1; i >= start; i--) {
          if (e == ia[i]) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public List_of_ints subList(int fromIndex, int toIndex) {
        return List_of_ints.newInstance(ia, start + fromIndex, start + toIndex);
      }

      @Override
      public OfInt iterator() {
        return new OfInt() {
          int pos = 0;

          @Override
          public boolean hasNext() {
            return pos < ia.length;
          }

          @Override
          public Integer next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return ia[pos++];
          }

          @Override
          public int nextInt() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return ia[pos++];
          }

        };
      }

      @Override
      public IntListIterator intListIterator() {
        return new IntListIterator() {

          private int pos = 0;

          @Override
          public boolean hasNext() {
            return pos >= 0 && pos < size();
          }

          @Override
          public int nextNvc() {
            return get(pos++);
          }

          @Override
          public boolean hasPrevious() {
            return pos > 0 && pos < size();
          }

          @Override
          public int previousNvc() {
            return get(--pos);
          }

          @Override
          public void moveToStart() {
            pos = 0;
          }

          @Override
          public void moveToEnd() {
            pos = size() - 1;
          }

        };
      }

      @Override
      public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
        System.arraycopy(src, srcPos, ia, start + destPos, length);
      }

      @Override
      public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
        System.arraycopy(ia, start + srcPos, dest, destPos, length);
      }

      @Override
      public void sort() {
        Arrays.sort(ia, start, end);
      }
    };
  }
}
