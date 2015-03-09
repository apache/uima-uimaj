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

/**
 * Utilities used by some of the IntX classes.
 */
public final class IntArrayUtils {

  private static final int default_growth_factor = 2;

  private static final int default_multiplication_limit = 1024 * 1024 * 16;

  public static final int[] ensure_size(int[] array, int req) {
    return ensure_size(array, req, default_growth_factor, default_multiplication_limit);
  }

  // done this way to allow more inlining
  public static final int[] ensure_size(final int[] array, final int req, final int growth_factor,
          final int multiplication_limit) {
    if (array.length < req) {
      return expand_size(array, req, growth_factor, multiplication_limit);
    }
    return array;
  }

  private static final int[] expand_size(final int[] array, final int req, final int growth_factor,
          final int multiplication_limit) {
    if (array.length == 0)
      return new int[req];
    int new_array_size = array.length;
    
    while (new_array_size < req) {
      if (new_array_size < multiplication_limit) {
        new_array_size = new_array_size * growth_factor;
      } else {
        new_array_size = new_array_size + multiplication_limit;
      }
    }
    final int[] new_array = new int[new_array_size];
    System.arraycopy(array, 0, new_array, 0, array.length);
    return new_array;
  }
  
  public static final boolean[] ensure_size(boolean[] array, int req, int growth_factor,
          int multiplication_limit) {
    if (array.length < req) {
      int new_array_size;
      if (array.length > 0) {
        new_array_size = array.length;
      } else {
        return new boolean[req];
      }
      while (new_array_size < req) {
        if (new_array_size < multiplication_limit) {
          new_array_size = new_array_size * growth_factor;
        } else {
          new_array_size = new_array_size + multiplication_limit;
        }
      }
      boolean[] new_array = new boolean[new_array_size];
      System.arraycopy(array, 0, new_array, 0, array.length);
      array = new_array;
    }
    return array;
  }

  public static final char[] ensure_size(char[] array, int req, int growth_factor,
          int multiplication_limit) {
    if (array.length < req) {
      int new_array_size;
      if (array.length > 0) {
        new_array_size = array.length;
      } else {
        return new char[req];
      }
      while (new_array_size < req) {
        if (new_array_size < multiplication_limit) {
          new_array_size = new_array_size * growth_factor;
        } else {
          new_array_size = new_array_size + multiplication_limit;
        }
      }
      char[] new_array = new char[new_array_size];
      System.arraycopy(array, 0, new_array, 0, array.length);
      array = new_array;
    }
    return array;
  }

  /**
   * Binary search on a span of a sorted integer array. If array is not sorted, results are
   * unpredictable. If you want to search the whole array, use the version in
   * {@link java.util.Arrays java.util.Arrays} instead; it's probably faster.
   * 
   * @param array
   *          The input array.
   * @param ele
   *          The int we're looking for.
   * @param start
   *          Start looking at this position, where <code>0 &le;=
   *    start &le;= end &le;= array.length</code>.
   * @param end
   *          Look up to this point (non-inclusive).
   * @return The position of <code>ele</code>, if found; <code>-insertPos-1</code>, if not.
   *         <code>insertPos</code> is the position where <code>ele</code> would be inserted.
   *         Note that the return value is <code>&gt;= start</code> iff <code>ele</code> was
   *         found; see {@link java.util.Arrays java.util.Arrays}.
   */
  public static final int binarySearch(int[] array, int ele, int start, int end) {
    --end; // Make end a legal value.
    int i; // Current position
    int current; // Current value
    while (start <= end) {
      i = (int)(((long)start + end) / 2);
      current = array[i];
      if (ele == current) {
        return i;
      }
      if (start == end) {
        if (ele < current) {
          return (-i) - 1;
        }
        // el > current
        return (-i) - 2; // (-(i+1))-1
      }
      if (ele < current) {
        end = i - 1;
      } else { // el > current
        start = i + 1;
      }
    }
    // This means that the input span is empty.
    return (-start) - 1;
  }

  /**
   * Find an int in an (unsorted) array.
   * 
   * @param x
   *          The int to find.
   * @param a
   *          The array.
   * @return The position (first occurence) where <code>x</code> was found; <code>-1</code> if
   *         not found.
   */
  public static final int find(int x, int[] a) {
    final int max = a.length;
    for (int i = 0; i < max; i++) {
      if (a[i] == x) {
        return i;
      }
    }
    return -1;
  }

}
