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

package org.apache.uima.cas_data.impl;

import java.lang.reflect.Array;

import org.apache.uima.cas_data.PrimitiveArrayFS;

/**
 * 
 * 
 */
public class PrimitiveArrayFSImpl extends FeatureStructureImpl implements PrimitiveArrayFS {
  
  private static final long serialVersionUID = -2050313181387759103L;

  private Object mArrayObject;

  public PrimitiveArrayFSImpl(String[] aArray) {
    mArrayObject = aArray;
  }

  public PrimitiveArrayFSImpl(int[] aArray) {
    mArrayObject = aArray;
  }

  public PrimitiveArrayFSImpl(float[] aArray) {
    mArrayObject = aArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.PrimitiveArrayFS#size()
   */
  public int size() {
    return Array.getLength(mArrayObject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.PrimitiveArrayFS#toIntArray()
   */
  public int[] toIntArray() {
    if (mArrayObject instanceof int[]) {
      return (int[]) mArrayObject;
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.PrimitiveArrayFS#toFloatArray()
   */
  public float[] toFloatArray() {
    if (mArrayObject instanceof float[]) {
      return (float[]) mArrayObject;
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.PrimitiveArrayFS#toStringArray()
   */
  public String[] toStringArray() {
    if (mArrayObject instanceof String[]) {
      return (String[]) mArrayObject;
    } else {
      // convert int or flota arrays to String arrays
      int size = size();
      String[] strArray = new String[size];
      for (int i = 0; i < size; i++) {
        strArray[i] = Array.get(mArrayObject, i).toString();
      }
      return strArray;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.FeatureValue#get()
   */
  public Object get() {
    return mArrayObject;
  }

  public String toString() {
    String[] strArray = toStringArray();
    StringBuffer buf = new StringBuffer();
    buf.append('\n').append(getType()).append('\n');
    if (getId() != null) {
      buf.append("ID = ").append(getId()).append('\n');
    }
    buf.append('[');
    for (int i = 0; i < strArray.length; i++) {
      buf.append(strArray[i]);
      if (i < strArray.length - 1) {
        buf.append(',');
      }
    }
    buf.append("]\n");
    return buf.toString();
  }

}
