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

/**
 * Common parts of the Array interfaces.
 */
public interface CommonArrayFS extends FeatureStructure {

  /**
   * Return the size of the array.
   * 
   * @return The size of the array.
   */
  int size(); // Java style. We can also call this getLength().

  /**
   * Creates a new string array and copies this array values into it.
   * 
   * @return A Java array copy of this array.
   */
  String[] toStringArray();

  /**
   * Copy the contents of the array to an external string array.
   * 
   * @param srcOffset
   *                The index of the first element to copy.
   * @param dest
   *                The array to copy to.
   * @param destOffset
   *                Where to start copying into <code>dest</code>.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>srcOffset &lt; 0</code> or
   *                    <code>length &gt; size()</code> or
   *                    <code>destOffset + length &gt; destArray.length</code>.
   */
  void copyToArray(int srcOffset, String[] dest, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException;

  /**
   * Copy the contents of an external string array into this array. The strings
   * are parsed and converted to floats.
   * 
   * @param src
   *                The source array.
   * @param srcOffset
   *                Where to start copying in the source array.
   * @param destOffset
   *                Where to start copying to in the destination array.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    When length conditions are not met.
   * @exception NumberFormatException
   *                    When the input strings do not represent valid floats.
   * @exception UnsupportedOperationException
   *                    When the array is an array of FSs.
   */
  void copyFromArray(String[] src, int srcOffset, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException, NumberFormatException;

}
