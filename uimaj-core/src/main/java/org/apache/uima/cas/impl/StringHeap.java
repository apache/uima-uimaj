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

package org.apache.uima.cas.impl;

import org.apache.uima.internal.util.IntArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Encapsulate string storage for the CAS.
 * 
 */
final class StringHeap {
  private static final int DEFAULT_REF_HEAP_BASE_SIZE = 5000;

  private static final int DEFAULT_REF_HEAP_MULT_LIMIT = DEFAULT_REF_HEAP_BASE_SIZE * 1024;

  private static final int DEFAULT_STRING_HEAP_BASE_SIZE = 20000;

  private static final int DEFAULT_STRING_HEAP_MULT_LIMIT = DEFAULT_STRING_HEAP_BASE_SIZE * 1024;

  private static final int MIN_REF_HEAP_BASE_SIZE = 1024;

  private static final int MIN_STR_HEAP_BASE_SIZE = 1024 * 32;

  protected static final int REF_HEAP_CELL_SIZE = 3;

  protected static final int CHAR_HEAP_POINTER_OFFSET = 0;

  protected static final int CHAR_HEAP_STRLEN_OFFSET = 1;

  protected static final int STRING_LIST_ADDR_OFFSET = 2;

  private static final int GROWTH_FACTOR = 4;

  private static final int NULL = 0;

  // Start pos so that first returned string code is 1.
  protected static final int FIRST_CELL_REF = 3;

  private final int refHeapBaseSize;

  private final int refHeapMultLimit;

  private final int strHeapBaseSize;

  private final int strHeapMultLimit;

  protected int refHeapPos = FIRST_CELL_REF;

  protected int[] refHeap;

  protected ArrayList stringList;

  protected int charHeapPos = 0;

  protected char[] stringHeap;

  StringHeap() {
    this(DEFAULT_REF_HEAP_BASE_SIZE, DEFAULT_REF_HEAP_MULT_LIMIT, DEFAULT_STRING_HEAP_BASE_SIZE,
                    DEFAULT_STRING_HEAP_MULT_LIMIT);
  }

  StringHeap(int refHeapBaseSize, int refHeapMultLimit, int strHeapBaseSize, int strHeapMultLimit) {
    super();
    if (refHeapBaseSize < MIN_REF_HEAP_BASE_SIZE) {
      this.refHeapBaseSize = MIN_REF_HEAP_BASE_SIZE;
    } else {
      this.refHeapBaseSize = refHeapBaseSize;
    }
    if (refHeapMultLimit < DEFAULT_REF_HEAP_MULT_LIMIT) {
      this.refHeapMultLimit = DEFAULT_REF_HEAP_MULT_LIMIT;
    } else {
      this.refHeapMultLimit = refHeapMultLimit;
    }
    if (strHeapBaseSize < MIN_STR_HEAP_BASE_SIZE) {
      this.strHeapBaseSize = MIN_STR_HEAP_BASE_SIZE;
    } else {
      this.strHeapBaseSize = strHeapBaseSize;
    }
    if (strHeapMultLimit < DEFAULT_STRING_HEAP_MULT_LIMIT) {
      this.strHeapMultLimit = DEFAULT_STRING_HEAP_MULT_LIMIT;
    } else {
      this.strHeapMultLimit = strHeapMultLimit;
    }
    initMemory();
  }

  private static final int cas2refHeapPointer(int i) {
    return i * REF_HEAP_CELL_SIZE;
  }

  private static final int refHeap2casPointer(int i) {
    return i / REF_HEAP_CELL_SIZE;
  }

  private final void initMemory() {
    this.refHeap = new int[this.refHeapBaseSize];
    this.stringHeap = new char[this.strHeapBaseSize];
    this.stringList = new ArrayList();
    this.stringList.add(null);
  }

  final void reset() {
    this.reset(false);
  }

  final void reset(boolean doFullReset) {
    if (doFullReset) {
      this.initMemory();
    } else {
      Arrays.fill(this.refHeap, 0, this.refHeapPos, 0);
      this.stringList = new ArrayList();
      this.stringList.add(null);
    }
    this.refHeapPos = FIRST_CELL_REF;
    this.charHeapPos = 0;
  }

  // Getters

  String getStringForCode(int stringCode) {
    if (stringCode == NULL) {
      return null;
    }
    final int strInfoRef = cas2refHeapPointer(stringCode);
    final int internalStringCode = this.refHeap[strInfoRef + STRING_LIST_ADDR_OFFSET];
    if (internalStringCode != NULL) {
      return (String) this.stringList.get(internalStringCode);
    }
    final int strOffset = this.refHeap[strInfoRef + CHAR_HEAP_POINTER_OFFSET];
    final int strLen = this.refHeap[strInfoRef + CHAR_HEAP_STRLEN_OFFSET];
    return new String(this.stringHeap, strOffset, strLen);
  }

  int copyCharsToBuffer(int stringCode, char[] buffer, int start) {
    final int strInfoRef = cas2refHeapPointer(stringCode);
    final int strCode = this.refHeap[strInfoRef + STRING_LIST_ADDR_OFFSET];
    int strOffset = this.refHeap[strInfoRef + CHAR_HEAP_POINTER_OFFSET];
    final int bufMax = buffer.length - start;
    if (strCode == 0) {
      final int strLen = this.refHeap[strInfoRef + CHAR_HEAP_STRLEN_OFFSET];
      final int max = (strLen < bufMax) ? strLen : bufMax;
      // Warning: using start and strOffset as counters (premature
      // optimization ;)
      for (int i = 0; i < max; i++) {
        buffer[start] = this.stringHeap[strOffset];
        ++start;
        ++strOffset;
      }
      return strLen;
    }
    final int internalStringCode = this.refHeap[strInfoRef + STRING_LIST_ADDR_OFFSET];
    final String str = (String) this.stringList.get(internalStringCode);
    final int len = str.length();
    final int max = (len < bufMax) ? len : bufMax;
    for (int i = 0; i < max; i++) {
      buffer[start + i] = str.charAt(i);
    }
    return len;
  }

  // Setters

  int addString(String s) {
    // Get and remember pointer at next free refHeap cell.
    final int cellRef = this.refHeapPos;
    // Increment pos and ensure sufficient space.
    this.refHeapPos += REF_HEAP_CELL_SIZE;
    ensureRefHeapSize();
    // Get a new string ref.
    final int stringRef = this.stringList.size();
    // Set the string list reference to the code point of the string being
    // added.
    this.refHeap[cellRef + STRING_LIST_ADDR_OFFSET] = stringRef;
    if (s != null) {
      this.refHeap[cellRef + CHAR_HEAP_STRLEN_OFFSET] = s.length();
    }
    // Add the string to the list, at the position corresponding to
    // stringRef.
    this.stringList.add(s);
    // Return cas version of refHeap ref.
    return refHeap2casPointer(cellRef);
  }

  int cloneStringReference(int stringCode) {
    // Set the string list reference to the code point of the string being
    // added.
    final int strInfoRef = cas2refHeapPointer(stringCode);
    int strRef = this.refHeap[strInfoRef + STRING_LIST_ADDR_OFFSET];
    // get the length of the reference string
    int strLen = this.refHeap[strInfoRef + CHAR_HEAP_STRLEN_OFFSET];
    // Get and remember pointer at next free refHeap cell.
    final int cellRef = this.refHeapPos;
    // Increment pos and ensure sufficient space.
    this.refHeapPos += REF_HEAP_CELL_SIZE;
    ensureRefHeapSize();
    // Set the string list reference to the code point of the string being
    // reference
    this.refHeap[cellRef + STRING_LIST_ADDR_OFFSET] = strRef;
    this.refHeap[cellRef + CHAR_HEAP_STRLEN_OFFSET] = strLen;
    // Return cas version of refHeap ref.
    return refHeap2casPointer(cellRef);
  }

  private final void ensureRefHeapSize() {
    this.refHeap = IntArrayUtils.ensure_size(this.refHeap, this.refHeapPos, GROWTH_FACTOR,
                    this.refHeapMultLimit);
  }

  int addCharBuffer(char[] buffer, int start, int length) {
    // Get and remember pointer at next free refHeap cell.
    final int cellRef = this.refHeapPos;
    // Increment pos and ensure sufficient space.
    this.refHeapPos += REF_HEAP_CELL_SIZE;
    ensureRefHeapSize();
    final int charBufStart = this.charHeapPos;
    // Create the cell.
    this.refHeap[cellRef + CHAR_HEAP_POINTER_OFFSET] = charBufStart;
    this.refHeap[cellRef + CHAR_HEAP_STRLEN_OFFSET] = length;
    this.charHeapPos += length;
    ensureCharHeapSize();
    System.arraycopy(buffer, start, this.stringHeap, charBufStart, length);
    return refHeap2casPointer(cellRef);
  }

  private void ensureCharHeapSize() {
    this.stringHeap = IntArrayUtils.ensure_size(this.stringHeap, this.charHeapPos, GROWTH_FACTOR,
                    this.strHeapMultLimit);
  }

  // Informational

  final boolean isStringCode(int stringCode) {
    final int cellRef = cas2refHeapPointer(stringCode);
    return isValidRef(cellRef);
  }

  private final boolean isValidRef(final int ref) {
    return (ref >= FIRST_CELL_REF && ref < this.refHeapPos);
  }

  final boolean isJavaString(int stringCode) {
    final int cellRef = cas2refHeapPointer(stringCode);
    if (!isValidRef(cellRef)) {
      return false;
    }
    return (this.refHeap[cellRef + STRING_LIST_ADDR_OFFSET] != NULL);
  }

  final boolean isCharArray(int stringCode) {
    final int cellRef = cas2refHeapPointer(stringCode);
    if (!isValidRef(cellRef)) {
      return false;
    }
    return (this.refHeap[cellRef + CHAR_HEAP_POINTER_OFFSET] != NULL);
  }

  final int getCharArrayLength(int stringCode) {
    final int cellRef = cas2refHeapPointer(stringCode);
    return this.refHeap[cellRef + CHAR_HEAP_STRLEN_OFFSET];
  }

  final int getLeastStringCode() {
    return refHeap2casPointer(FIRST_CELL_REF);
  }

  final int getLargestStringCode() {
    return refHeap2casPointer(this.refHeapPos) - 1;
  }

}
