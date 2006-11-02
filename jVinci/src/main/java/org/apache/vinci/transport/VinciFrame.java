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

import org.apache.vinci.transport.util.TransportableConverter;

/**
 * This is the "default" document class for use with the Vinci client and servable classes.
 * VinciFrame implements a queryable frame from (nested) ArrayList data structures. Search time
 * for a named tag is O(n) in the number of keys at a given depth, which is fine for all but the
 * largest documents.
 *
 * VinciFrame complements the QueryableFrame adders and getters with several setter methods
 * [fset(String, *)] for modifying the values of designated tags.
 */
public class VinciFrame extends QueryableFrame {

  private int                         capacity;
  private int                         size;
  private KeyValuePair[]              elements;

  private static TransportableFactory vinciFrameFactory = new TransportableFactory() {
                                                          public Transportable makeTransportable() {
                                                            return new VinciFrame();
                                                          }
                                                        };

  /**
   * Get a TransportableFactory that creates new VinciFrames.
   */
  static public TransportableFactory getVinciFrameFactory() {
    return vinciFrameFactory;
  }

  /**
   * Create a new empty VinciFrame.
   */
  public VinciFrame() {
    this(10);
  }

  /**
   * Create a VinciFrame that is a (deep) copy of the given transportable.
   * 
   * @pre t != null
   */
  public static VinciFrame toVinciFrame(Transportable t) {
    return (VinciFrame) TransportableConverter.convert(t, getVinciFrameFactory());
  }

  /**
   * Create a new empty VinciFrame with the specified initial capacity.
   *
   * @param initialCapacity the capacity value to be passed on to the internal ArrayList used
   * for holding KeyValuePairs. 
   *
   * @pre initialCapacity >= 0
   */
  public VinciFrame(int initialCapacity) {
    capacity = initialCapacity;
    size = 0;
    elements = new KeyValuePair[capacity];
  }

  /**
   * Returns a ArrayList of all the keys at the top-level of this frame, removing any
   * duplicates.
   *
   * @return A ArrayList of keys.  
   */
  public ArrayList fkeys() {
    ArrayList rval = new ArrayList();
    for (int i = 0; i < size; i++) {
      String key = elements[i].key;
      if (!(rval.contains(key))) {
        rval.add(key);
      }
    }
    return rval;
  }

  /** 
   * Implementation of the abstract fget method defined in QueryableFrame.
   */
  public ArrayList fget(String key) {
    ArrayList return_me = new ArrayList();
    for (int i = 0; i < size; i++) {
      KeyValuePair pair = elements[i];
      if (pair.key.equals(key)) {
        return_me.add(pair.value);
      }
    }
    return return_me;
  }

  /** 
   * Implementation of the abstract fgetFirst method defined in QueryableFrame.
   */
  public FrameComponent fgetFirst(String key) {
    for (int i = 0; i < size; i++) {
      KeyValuePair pair = elements[i];
      if (pair.key.equals(key)) {
        return pair.value;
      }
    }
    return null;
  }

  /**
   * Override the createSubFrame to create a VinciFrame of precise capacity.
   *
   * @pre tag_name != null
   * @pre initialCapacity >= 0
   */
  public Frame createSubFrame(String tag_name, int initialCapacity) {
    return new VinciFrame(initialCapacity);
  }

  /**
   * Convenience method for fetching sub-frames when their type is known to be VinciFrame
   *
   * @param key The key identifying the value to retrieve.
   * @exception ClassCastException (unchecked) if the value was not of type VinciFrame.
   * @return The requested value, or null if the specified key does not exist.
   */
  public VinciFrame fgetVinciFrame(String key) {
    return (VinciFrame) fgetFirst(key);
  }

  /**
   * Change the value associated with first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.
   *
   * @exception NullPointerException if val is null.  
   *
   * @pre key != null
   * @pre val != null
   */
  public VinciFrame fset(String key, String val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.  
   * 
   * @pre key != null
   */
  public VinciFrame fset(String key, long val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added. 
   *
   * @pre key != null
   */
  public VinciFrame fset(String key, boolean val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added. 
   *
   * @pre key != null
   */
  public VinciFrame fset(String key, int val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.
   *
   * @pre key != null
   * @pre val != null
   */
  public VinciFrame fset(String key, int[] val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.
   *
   * @exception NullPointerException if val is null.  
   *
   * @pre key != null
   */
  public VinciFrame fset(String key, Frame val) {
    set(key, val);
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.  
   *
   * @pre key != null
   */
  public VinciFrame fset(String key, double val) {
    set(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added.
   *
   * @exception NullPointerException if bin is null.  
   *
   * @pre key != null
   * @pre bin != null
   */
  public VinciFrame fset(String key, byte[] bin) {
    set(key, new FrameLeaf(bin, true));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added. The warnings associated with faddTrueBinary also
   * apply to this method.
   *
   * @exception NullPointerException if bin is null.  
   *
   * @pre key != null
   * @pre bin != null
   */
  public VinciFrame fsetTrueBinary(String key, byte[] bin) {
    set(key, new FrameLeaf(bin, false));
    return this;
  }

  /**
   * Change the value associated with the first occurence of the given key to val. If the key
   * doesn't exist, then the value is added. Note that there is no suite of methods to change
   * *all* values associated with a given key to some value.
   *
   * @exception NullPointerException if val is null.  
   *
   * @pre key != null
   */
  protected void set(String key, FrameComponent val) {
    if (val != null) {
      for (int i = 0; i < size; i++) {
        KeyValuePair pair = elements[i];
        if (pair.key.equals(key)) {
          pair.value = val;
          return;
        }
      }
      add(key, val);
    } else {
      throw new NullPointerException();
    }
  }

  /**
   * Remove only the first element whose tag name matches the specified key (if any) from the
   * top level of this frame.
   * 
   * @param key The tag name of the element to remove.
   * @return this object (NOT the component dropped).  
   */
  public VinciFrame fdropFirst(String key) {
    for (int i = 0; i < size; i++) {
      KeyValuePair pair = elements[i];
      if (pair.key.equals(key)) {
        System.arraycopy(elements, i + 1, elements, i, size - i - 1);
        size--;
        elements[size] = null;
        break;
      }
    }
    return this;
  }

  /**
   * Remove all elements whose tag name matches the provided key (if any) from the top level of
   * this frame.
   * 
   * @param key The tag name of the elements to remove.
   * @return this object (NOT the component dropped).  
   */
  public VinciFrame fdrop(String key) {
    int shift = 0;
    for (int i = 0; i < size; i++) {
      if (shift != 0) {
        elements[i] = elements[i + shift];
      }
      KeyValuePair pair = elements[i];
      if (pair.key.equals(key)) {
        shift++;
        size--;
        i--;
      }
    }
    for (int i = 0; i < shift; i++) {
      elements[size + i] = null;
    }
    return this;
  }

  /**
   * Reset this frame to an empty state.
   */
  public void freset() {
    if (capacity > 10) {
      capacity = 10;
    }
    elements = new KeyValuePair[capacity];
    size = 0;
  }

  /**
   * Implementation of the abstract Frame method.
   *
   * @pre key != null
   * @pre val != null
   */
  public void add(String key, FrameComponent val) {
    if (val != null) {
      ensureCapacity();
      elements[size++] = new KeyValuePair(key, val);
    }
  }

  protected void ensureCapacity() {
    if (size == capacity) {
      KeyValuePair[] old = elements;
      int newCapacity = (capacity * 3) / 2 + 1;
      elements = new KeyValuePair[newCapacity];
      System.arraycopy(old, 0, elements, 0, size);
      capacity = newCapacity;
    }
  }

  /**
   * Implementation of the abstract Frame method.
   *
   * @pre which < getKeyValuePairCount()
   * @pre which >= 0
   */
  public KeyValuePair getKeyValuePair(int which) {
    return elements[which];
  }

  /**
   * Implementation of the abstract Frame method.
   */
  public int getKeyValuePairCount() {
    return size;
  }

  /**
   * Recursively strip any raw PCDATA fields that are entirely whitespace.
   * 
   * @returns true if there was whitespace to strip.
   * @since 2.1.2a
   */
  public boolean stripWhitespace() {
    boolean returnMe = false;
    int shift = 0;
    for (int i = 0; i < size; i++) {
      if (shift != 0) {
        elements[i] = elements[i + shift];
      }
      KeyValuePair pair = elements[i];
      if (pair.isValueALeaf()) {
        if (pair.key.equals("")) {
          String checkForWhitespace = pair.getValueAsString();
          boolean isAllWhitespace = true;
          for (int j = 0; j < checkForWhitespace.length(); j++) {
            if (!Character.isWhitespace(checkForWhitespace.charAt(j))) {
              isAllWhitespace = false;
              break;
            }
          }
          if (isAllWhitespace) {
            returnMe = true;
            shift++;
            size--;
            i--;
          }
        }
      } else {
        if (((VinciFrame) pair.getValueAsFrame()).stripWhitespace()) {
          returnMe = true;
        }
      }
    }
    for (int i = 0; i < shift; i++) {
      elements[size + i] = null;
    }
    return returnMe;
  }

}
