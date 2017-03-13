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

import java.lang.reflect.Array;

/**
 * Some utilities for dealing with Arrays.
 * 
 * 
 */
public abstract class ArrayUtils {

  /**
   * Combines two arrays. If the two arrays are of the same type, the resulting array will also be
   * of that type, otherwise it will be an <code>Object[]</code>.
   * 
   * @param aArray1
   *          first non-null array to combine
   * @param aArray2
   *          second non-null array to combine
   * 
   * @return an array consisting of all the elements of <code>aArray1</code> followed by all the
   *         elements of <code>aArray2</code>.
   */
  public static Object[] combine(Object[] aArray1, Object[] aArray2) {
    Object result;
    int length = aArray1.length + aArray2.length;

    // determine the component type for the new array
    Class<?> componentType1 = aArray1.getClass().getComponentType();
    Class<?> componentType2 = aArray2.getClass().getComponentType();
    if (componentType1 == componentType2) {
      result = Array.newInstance(componentType1, length);
    } else {
      result = Array.newInstance(Object.class, length);
    }

    // do the array copy
    System.arraycopy(aArray1, 0, result, 0, aArray1.length);
    System.arraycopy(aArray2, 0, result, aArray1.length, aArray2.length);

    return (Object[]) result;
  }
}
