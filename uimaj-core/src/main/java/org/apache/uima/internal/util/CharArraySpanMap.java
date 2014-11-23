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

import java.util.ArrayList;

/**
 * A map from subsequences of a character array to objects.
 */
public class CharArraySpanMap {

  private static final class Entry {
    private Entry() {
      super();
      this.start = 0;
      this.length = 0;
      this.value = null;
    }

    private int start;

    private int length;

    private Object value;

  }

  private static final int MIN_ARRAY_SIZE = 1024;

  private static final int MIN_MAP_SIZE = 5003;

  private char[] charArray;

  private int pos;

  // TOOD: It is not possible to create a generic array the array should be replaced by a List
  private ArrayList<Entry>[] map;

  /**
   * Default constructor.
   */
  public CharArraySpanMap() {
    this(MIN_ARRAY_SIZE);
  }

  /**
   * Constructor with initial array size argument.
   * 
   * @param initialArraySize
   *          Initial array size.
   */
  public CharArraySpanMap(int initialArraySize) {
    this(initialArraySize, MIN_MAP_SIZE);
  }

  /**
   * Constructor.
   * 
   * @param initialArraySize
   *          Initial array size.
   * @param initialMapSize
   *          Initial map size.
   */
  @SuppressWarnings("unchecked")
  public CharArraySpanMap(int initialArraySize, int initialMapSize) {
    super();
    if (initialArraySize < MIN_ARRAY_SIZE) {
      initialArraySize = MIN_ARRAY_SIZE;
    }
    if (initialMapSize < MIN_MAP_SIZE) {
      initialMapSize = MIN_MAP_SIZE;
    }
    this.charArray = new char[initialArraySize];
    this.map = new ArrayList[initialMapSize];
    for (int i = 0; i < initialMapSize; i++) {
      this.map[i] = new ArrayList<Entry>();
    }
    this.pos = 0;
  }

  private final int isInList(String s, ArrayList<Entry> entryList) {
    final int listLen = entryList.size();
    final int strLen = s.length();
    Entry entry;
    boolean found = false;
    int i = 0;
    while (i < listLen) {
      entry = entryList.get(i);
      if (strLen != entry.length) {
        ++i;
        continue;
      }
      found = true;
      for (int j = 0; j < strLen; j++) {
        if (s.charAt(j) != this.charArray[j + entry.start]) {
          found = false;
          break;
        }
      }
      if (found) {
        break;
      }
      ++i;
    }
    return (found) ? i : -1;
  }

  private final int isInList(final char[] inputArray, final int start, final int strLen,
          ArrayList<Entry> entryList) {
    final int listLen = entryList.size();
    Entry entry;
    boolean found = false;
    int i = 0, k;
    int max;
    while (i < listLen) {
      entry = entryList.get(i);
      if (strLen != entry.length) {
        ++i;
        continue;
      }
      found = true;
      k = entry.start;
      max = start + strLen;
      for (int count = start; count < max; count++) {
        if (inputArray[count] != this.charArray[k]) {
          found = false;
          break;
        }
        ++k;
      }
      if (found) {
        break;
      }
      ++i;
    }
    return (found) ? i : -1;
  }

  /**
   * Add a key-value pair to the map.
   * 
   * @param s
   *          The key (will be copied).
   * @param value
   *          The value.
   * @pre s != null
   */
  public void put(String s, Object value) {
    final int hashCode = CharArrayString.hashCode(s);
    ArrayList<Entry> list = this.map[hashCode % this.map.length];
    final int listPos = isInList(s, list);
    if (listPos >= 0) {
      Entry entry = list.get(listPos);
      entry.value = value;
      return;
    }
    final int start = this.pos;
    addString(s);
    Entry entry = new Entry();
    entry.start = start;
    entry.length = this.pos - start;
    entry.value = value;
    list.add(entry);
  }

  /**
   * Check if sub-range of character array is a key.
   * 
   * @param characterArray
   *          Array that contains the potential key chars.
   * @param start
   *          Start of sub-range.
   * @param length
   *          Length of sub-range.
   * @return <code>true</code> iff the map contains the key.
   * @pre characterArray != null
   * @pre start &ge; 0
   * @pre length &ge; 0
   * @pre length &le; (characterArray.length - start)
   */
  public final boolean containsKey(char[] characterArray, int start, int length) {
    final int hashCode = CharArrayString.hashCode(characterArray, start, (start + length));
    final ArrayList<Entry> list = this.map[hashCode % this.map.length];
    final int listPos = isInList(characterArray, start, length, list);
    return (listPos >= 0);
  }

  public final Object get(char[] characterArray, int start, int length) {
    final int hashCode = CharArrayString.hashCode(characterArray, start, (start + length));
    final ArrayList<Entry> list = this.map[hashCode % this.map.length];
    final int listPos = isInList(characterArray, start, length, list);
    return (listPos >= 0) ? list.get(listPos).value : null;
  }

  private final void addString(String s) {
    final int strLen = s.length();
    final int newMinLength = this.pos + strLen;
    int newLength = this.charArray.length;
    boolean needToCopy = false;
    while (newLength < newMinLength) {
      newLength += MIN_ARRAY_SIZE;
      needToCopy = true;
    }
    if (needToCopy) {
      char[] newCharArray = new char[newLength];
      System.arraycopy(this.charArray, 0, newCharArray, 0, this.pos);
      this.charArray = newCharArray;
    }
    for (int i = 0; i < strLen; i++) {
      this.charArray[this.pos] = s.charAt(i);
      ++this.pos;
    }
  }

}
