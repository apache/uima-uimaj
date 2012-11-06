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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulate string storage for the CAS.
 * 
 */
final class StringHeap {
  
  private static final int leastStringCode = 1;

  private List<String> stringList;

  StringHeap() {
    super();
    initMemory();
  }

  // Initialize internal datastructures.  This used to be a lot more complicated when we had the
  // character heap option.  
  private final void initMemory() {
    this.stringList = new ArrayList<String>();
    this.stringList.add(null);
  }

  /** Deserialize from a binary serialized CAS
   * 
   * @param shdh Serialization helper datastructure.
   */
  final void reinit(StringHeapDeserializationHelper shdh, boolean delta) {
  	if (!delta) {
        initMemory();
  	}
    // Simply iterate over the ref heap and add one string after another.  The references come out
    // right because they are defined by the positions on the ref heap.
    int stringOffset;
    int stringLength;
    String charHeapInString = new String(shdh.charHeap); // UIMA-2460
    Map<String, String> reuseStrings = new HashMap<String, String>(
        Math.min(8, 
                 (shdh.refHeap.length / StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) 
                 / 2));
    for (int i = StringHeapDeserializationHelper.FIRST_CELL_REF; i < shdh.refHeap.length; i += StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) {
      stringOffset = shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET];
      stringLength = shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET];
      String s = charHeapInString.substring(stringOffset, stringOffset + stringLength);
      String reuse = reuseStrings.get(s);
      if (reuse == null) {
        reuseStrings.put(s, s);
      }
      this.stringList.add(reuse != null ? reuse : s);  
    }
  }

  /**
   * Create serialization helper datastructure.
   * @return Serialization helper that can be interpreted easier by serialization code.
   */
  StringHeapDeserializationHelper serialize() {
    return serialize(1);  
  }
  
  StringHeapDeserializationHelper serialize(int startPos) {
    StringHeapDeserializationHelper shdh = new StringHeapDeserializationHelper();
	// Ref heap is 3 times the size of the string list.
	shdh.refHeap = new int[(this.stringList.size() - startPos + 1)
			* StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE];
	shdh.refHeapPos = shdh.refHeap.length;
	// Compute required size of character heap.
	int charHeapSize = 0;   
	for (int i = startPos; i < this.stringList.size(); i++) {
		String s = this.stringList.get(i);
		if (s != null) {
			charHeapSize += s.length();
		}
	}
	shdh.charHeap = new char[charHeapSize];
	shdh.charHeapPos = shdh.charHeap.length;

	int charCount = 0;
	// Now write out the actual data
	int r = 1;
	for (int i = startPos; i < this.stringList.size(); i++) {
		String s = this.stringList.get(i);
		int refHeapOffset = r
				* StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE;
		shdh.refHeap[refHeapOffset
				+ StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET] = charCount;
		shdh.refHeap[refHeapOffset
				+ StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET] = s
				.length();
		System.arraycopy(s.toCharArray(), 0, shdh.charHeap, charCount, s
				.length());
		charCount += s.length();
		r++;
	}
	assert (charCount == shdh.charHeap.length);
	return shdh;
  }

  // Reset the string heap (called on CAS reset).
  final void reset() {
    initMemory();
  }

  // Get a string value
  String getStringForCode(int stringCode) {
    if (stringCode == LowLevelCAS.NULL_FS_REF) {
      return null;
    }
    return this.stringList.get(stringCode);
  }

  // Who uses this?
  int copyCharsToBuffer(int stringCode, char[] buffer, int start) {
    final String str = this.stringList.get(stringCode);
    final int len = str.length();
    final int requestedMax = start + len;
    // Check that the buffer is long enough to copy the whole string. If it isn't long enough, we
    // copy up to buffer.length - start characters.
    final int max = (buffer.length < requestedMax) ? (buffer.length - start) : len;
    for (int i = 0; i < max; i++) {
      buffer[start + i] = str.charAt(i);
    }
    return len;
  }

  /**
   * Add a string.
   * @param s The string.
   * @return The positional code of the added string.
   */
  int addString(String s) {
    if (s == null) {
      return LowLevelCAS.NULL_FS_REF;
    }
    final int addr = this.stringList.size();
    this.stringList.add(s);
    return addr;
  }

  // Not sure what this is supposed to do.  Passes unit tests like this.
  int cloneStringReference(int stringCode) {
    return stringCode;
  }

  // Who uses this?
  int addCharBuffer(char[] buffer, int start, int length) {
    String s = new String(buffer, start, length);
    return this.addString(s);
  }


  final int getCharArrayLength(int stringCode) {
    return this.stringList.get(stringCode).length();
  }

  final int getLeastStringCode() {
    return leastStringCode;
  }

  final int getLargestStringCode() {
    return this.stringList.size() - 1;
  }
  
  final int getSize() {
	  return this.stringList.size();
  }
  
}
