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

package org.apache.vinci.transport;

import java.util.ArrayList;

/**
 * QueryableFrame is a decorator class that extends Frame. While the Frame decorates its
 * descendents with adder methods for building an XML document, the QueryableFrame further
 * decorates its descendents with getter methods that provide simple "declarative" querying of
 * document values.  This supports the important Vinci convention of declarative value access to
 * support evolving service schemas.
 *
 * Descendents of QueryableFrame need only implement the abstract fget(String key) and
 * fgetFirst(String key) methods.
 * 
 * Typically you will use VinciFrame, an immediate and concrete descendent of QueryableFrame, for
 * most of your code.  QueryableFrame can be extended to simplify implementation of alternate
 * Frame implementations should VinciFrame be inadequate for whatever reason.
 *
 * Note that the getters of QueryableFrame do not search the document recursively. That is, they
 * only search the "top level" tags within the Frame. More sophisticated document search methods
 * should probably be added in orthogonal query processing classes, such as through an
 * XPathProcessor class that understands the QueryableFrame interface.
 */
public abstract class QueryableFrame extends Frame {

  /**
   * This method must be implemented so that it returns ALL values paired with the specified key
   * in ArrayList format. Note that this method searches only the "top level" keys, that is, 
   * keys nested within a sub-frame are not searched by this method.
   *
   * @param key The key identifying the values to retrieve.
   * @return The list of values that are paired with given key. If no such values exist,
   * then an empty list is returned (null is never returned).  
   */
  public abstract ArrayList fget(String key);

  /**
   * This method must be implemented so that it returns only the FIRST value paired with
   * the specified key. Note that this method searches only the "top level" keys, that is, 
   * keys nested within a sub-frame are not searched by this method.
   *
   * @param key The key identifying the value to retrieve.
   * @return The first value associated with the given key, or null if none exist.
   */
  public abstract FrameComponent fgetFirst(String key);

  /**
   * Presuming the data identified by this key is Base64 data, returns the binary array result
   * of Base64 decoding that data.  If there is more than one of the specified key, then
   * the first is used. Returns null if no matching key found.
   * 
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested data is not B64.
   * @exception ClassCastException thrown if the specified frame component is not a leaf.
   * @return The requested value, or null if the specified key does not exist.
   */
  public byte[] fgetBytes(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toBytes();
    }
    return null;
  }

  /**
   * This is a "non-XTalk-1.0" hack to allow retrieving any data that was transported as "true
   * binary" (non Base64 encoded), for example via Frame.faddTrueBinary(String, byte[]) instead
   * of Frame.fadd(String, byte[]).  If there is more than one of the specified key, then
   * the first is used. Returns null if no matching key found.
   *
   * You can also use this method if you simply want to get at leaf data in raw UTF-8 form.
   *
   * @param key The key identifying the value to retrieve.
   * @exception ClassCastException thrown if the specified frame component is not a leaf.
   * @return The requested value, or null if the specified key does not exist.  
   */
  public byte[] fgetTrueBinary(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).getData();
    }
    return null;
  }

  /**
   * Retrieve the value of the specified key as a boolean. Returns true if and only if the value
   * of the key exactly equals the string "true" (TransportConstants.TRUE_VALUE).  If there is
   * more than one of the specified key, then the first is used.
   *
   * @param key The key identifying the value to be retrieved.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return true if the requested key exists and its value is "true", false otherwise.
   */
  public boolean fgetBoolean(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toBoolean();
    }
    return false;
  }

  /**
   * Retrieve the value of the specified key as a float. Throws an exception if the value could
   * not be converted to float. Returns Float.MIN_VALUE if the key does not exist.  If there is
   * more than one of the specified key, then the first is used.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested value could not be
   * converted to a float.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or Float.MIN_VALUE if the specified key does not exist.
   */
  public float fgetFloat(String key) throws LeafCastException {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toFloat();
    } else {
      return Float.MIN_VALUE;
    }
  }

  /**
   * Retrieve the value of the specified key as an array of floats. Assumes the key's value is a
   * space separated list of tokens. If there is more than one of the specified key, then the
   * first is used. Returns null if no matching key is found.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if *any* of the space-separated tokens
   * cannot be converted to float.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or null if the specified key does not exist.
   */
  public float[] fgetFloatArray(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toFloatArray();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a double. Throws an exception if the value could
   * not be converted to double. Returns Double.MIN_VALUE if the key does not exist.  If there
   * is more than one of the specified key, then the first is used.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested value could not be
   * converted to a double.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or Double.MIN_VALUE if the specified key does not exist.
   */
  public double fgetDouble(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toDouble();
    } else {
      return Double.MIN_VALUE;
    }
  }

  /**
   * Retrieve the value of the specified key as an array of doubles. Assumes the key's value is
   * a space separated list of tokens. If there is more than one of the specified key, then the
   * first is used. Returns null if there is no matching key.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if *any* of the space-separated tokens
   * cannot be converted to double.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or null if the specified key does not exist.
   */
  public double[] fgetDoubleArray(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toDoubleArray();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a int. Throws an exception if the value could
   * not be converted to int. Returns Int.MIN_VALUE if the key does not exist.  If there
   * is more than one of the specified key, then the first is used.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested value could not be
   * converted to a int.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or Int.MIN_VALUE if the specified key does not exist.
   */
  public int fgetInt(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toInt();
    } else {
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Retrieve the value of the specified key as an array of ints. Assumes the key's value is a
   * space separated list of tokens. If there is more than one of the specified key, then the
   * first is used. Returns null if there is no matching key.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if *any* of the space-separated tokens
   * cannot be converted to int.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or null if the specified key does not exist.
   */
  public int[] fgetIntArray(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toIntArray();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a long. Throws an exception if the value could
   * not be converted to long. Returns Long.MIN_VALUE if the key does not exist.  If there
   * is more than one of the specified key, then the first is used.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested value could not be
   * converted to a long.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or Long.MIN_VALUE if the specified key does not exist.
   */
  public long fgetLong(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toLong();
    } else {
      return Long.MIN_VALUE;
    }
  }

  /**
   * Retrieve the value of the specified key as an array of longs. Assumes the key's value is a
   * space separated list of tokens. If there is more than one of the specified key, then the
   * first is used. Returns null if there is no matching key.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if *any* of the space-separated tokens
   * cannot be converted to long.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or null if the specified key does not exist.
   */
  public long[] fgetLongArray(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toLongArray();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a String. If there is more than one of the
   * specified key, then the first is used. Returns null if there is no matching key. If
   * the requested value is not a leaf, then the XML of the sub-frame value is returned.
   *
   * @param key The key identifying the value to retrieve.
   * @return The requested value, or null if the specified key does not exist.
   */
  public String fgetString(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return fc.toString();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a String array. If there is more than one of the
   * specified key, then the first is used.  and assumes it's a String. Use at your own risk.
   *
   * @param key The key identifying the value to retrieve.
   * @exception LeafCastException (unchecked) thrown if the requested value is not a properly
   * encoded string array.
   * @exception ClassCastException if the specified value is not a leaf.
   * @return The requested value, or null if the specified key does not exist.  
   */
  public String[] fgetStringArray(String key) {
    FrameComponent fc = fgetFirst(key);
    if (fc != null) {
      return ((FrameLeaf) fc).toStringArray();
    } else {
      return null;
    }
  }

  /**
   * Retrieve the value of the specified key as a QueryableFrame.  If there is more than one of
   * the specified key, then the first is used.
   *
   * @param key The key identifying the value to retrieve.
   * @throws ClassCastException (unchecked) if the value was not of type QueryableFrame.
   * @return The requested value, or null if the specified key does not exist.
   */
  public QueryableFrame fgetFrame(String key) {
    return (QueryableFrame) fgetFirst(key);
  }
}
