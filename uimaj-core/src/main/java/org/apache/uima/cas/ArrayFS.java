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

import org.apache.uima.jcas.cas.TOP;

/**
 * Feature structure array interface. To create a FS array object, use
 * {@link org.apache.uima.cas.CAS#createArrayFS(int length)} or
 * new FSArray(aJCas, length)
 */
public interface ArrayFS<E extends FeatureStructure> extends CommonArrayFS<E> {

  /**
   * Get the i-th feature structure from the array.
   * @param i index
   * @param <U> The class of the item being obtained by the get
   * @return The i-th feature structure.
   * @exception ArrayIndexOutOfBoundsException
   *              If the index is out of bounds.
   */
  <U extends FeatureStructure> U get(int i) throws ArrayIndexOutOfBoundsException;

  /**
   * Set the i-th value.
   * 
   * @param i
   *          The index.
   * @param fs
   *          The value.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>i</code> is out of bounds.
   */
  void set(int i, E fs) throws ArrayIndexOutOfBoundsException;

  /**
   * Copy the contents of the array from <code>start</code> to <code>end</code> to the
   * destination <code>destArray</code> with destination offset <code>destOffset</code>.
   * 
   * @param srcOffset
   *          The index of the first element to copy.
   * @param dest
   *          The array to copy to.
   * @param destOffset
   *          Where to start copying into <code>dest</code>.
   * @param length
   *          The number of elements to copy.
   * @param <U> the type of the array element
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>srcOffset &lt; 0</code> or <code>length &gt; size()</code> or
   *              <code>destOffset + length &gt; destArray.length</code>.
   */
  <U extends FeatureStructure> void copyToArray(int srcOffset, U[] dest, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException;

  /**
   * Copy the contents of an external array into this array.
   * 
   * @param src
   *          The source array.
   * @param srcOffset
   *          Where to start copying in the source array.
   * @param destOffset
   *          Where to start copying to in the destination array.
   * @param length
   *          The number of elements to copy.
   * @param <T> the class of the array being copied into
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>srcOffset &lt; 0</code> or <code>length &gt; size()</code> or
   *              <code>destOffset + length &gt; destArray.length</code>.
   */
  <T extends FeatureStructure> void copyFromArray(T[] src, int srcOffset, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException;

  /**
   * Creates a new array the this array is copied to.
   * Return type is FeatureStructure to be backwards compatible with V2
   * @return A Java array copy of this FS array.
   */
  FeatureStructure[] toArray();
  
  /**
   * Populates an existing array from this FS Array.
   * @param a the existing array
   * @param <T> the type of the element
   * @return the populated array
   */
  <T extends TOP> T[] toArray(T[] a);

}
