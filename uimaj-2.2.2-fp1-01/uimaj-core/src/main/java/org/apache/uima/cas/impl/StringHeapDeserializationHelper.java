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

/**
 * Support for legacy string heap format.  Used only for (de)serialization.
 */
public class StringHeapDeserializationHelper {

  // Number of cells in string ref heap: 1 for start of string, 1 for length of string, and 1 if
  // String is in string list, not on heap.
  protected static final int REF_HEAP_CELL_SIZE = 3;

  // First position in a cell: where string starts on heap
  protected static final int CHAR_HEAP_POINTER_OFFSET = 0;

  // Second position in a cell: how long string on heap is
  protected static final int CHAR_HEAP_STRLEN_OFFSET = 1;

  // Third position in a cell: if the string is a real Java string, the position of that string
  // in the string list.  This is not used for serialization and kept here only for documentation
  // purposes.
  protected static final int STRING_LIST_ADDR_OFFSET = 2;

  // Start pos so that first returned string code is 1.
  protected static final int FIRST_CELL_REF = 3;

  protected int refHeapPos = FIRST_CELL_REF;

  // The 3-ints-per-cell reference heap
  protected int[] refHeap;

  // Current position in the character heap, meaningless
  protected int charHeapPos = 0;

  // Heap with the actual character data
  protected char[] charHeap;

  public StringHeapDeserializationHelper() {
    super();
  }

}
