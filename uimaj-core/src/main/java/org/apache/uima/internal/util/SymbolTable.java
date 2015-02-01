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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The SymbolTable class provides a generic symbol table. A symbol table is a bijective mapping
 * between strings and integers. Symbol numbering starts at a point defined by the user, which is 0
 * by default. A SymbolTable provides quick access to symbol numbers (code points) through a hash
 * table. The reverse map is facilitated through a separate vector of the symbols. This is memory
 * intensive, but much faster than searching through the hashtable.
 */
public class SymbolTable {

  // The default starting point
  private static final int DEFAULT_START = 0;

  // The actual starting point
  private int start;

  final private HashMap<String, Integer> symbol2intMap; // String -> Integer

  final private ArrayList<String> int2symbolMap; // switch from Vector to avoid sync contention 
  
  /**
   * Use this constructor if you need your symbol numbering to start at a different point than 0.
   * 
   * @param start
   *          The code point of the first symbol added to the table. Subsequent symbols will have
   *          larger code points.
   */
  public SymbolTable(int start) {
    this.symbol2intMap = new HashMap<String, Integer>();
    this.int2symbolMap = new ArrayList<String>();
    this.start = start;
  }

  /** Default constructor */
  public SymbolTable() {
    this(DEFAULT_START);
  }

  /**
   * Init the symbol table from an array of strings, where code points correspond to array
   * positions. Warning: if the array contains duplicate strings, the numbering will be off!
   * 
   * @param names
   *          The String array containing the symbols.
   */
  public SymbolTable(String[] names) {
    // Start numbering at 0.
    this(0);
    for (int i = 0; i < names.length; i++) {
      this.set(names[i]);
    }
  }

  public boolean contains(String symbol) {
    return (this.getStart() <= this.get(symbol));
  }

  /**
   * Get the starting number of the symbol table.
   * 
   * @return The start of the table.
   */
  public int getStart() {
    return this.start;
  }

  /**
   * Perform a deep copy.
   * 
   * @return A copy of <code>this</code>.
   */
  public SymbolTable copy() {
    // not efficient, but no internal callers Feb 2914 scan
    SymbolTable copy = new SymbolTable(this.start);
    int max = this.int2symbolMap.size();
    for (int i = 0; i < max; i++) {
      copy.set(this.int2symbolMap.get(i));
    }
    return copy;
  }

  // Utility function to convert from relative addressing (external)
  // to absolute addressing (internal).
  private final int rel2abs(int i) {
    return (i - this.start);
  }

  // Utility function to convert from absolute addressing (internal)
  // to relative addressing (external).
  private final int abs2rel(int i) {
    return (i + this.start);
  }

  /**
   * Create new symbol in table. If the symbol already exists, only the symbol's number is returned.
   * Use get() if you need to find out if the symbol is already in the table.
   * 
   * @param symbol
   *          the input string to be put in the table.
   * @return the symbol's number.
   */
  public int set(String symbol) {
    if (this.symbol2intMap.containsKey(symbol)) {
      return this.symbol2intMap.get(symbol).intValue();
    }
    int rel;
    int abs;
    synchronized (this) { // synchronize write access to internal data
      // structures
      abs = this.symbol2intMap.size();
      rel = abs2rel(abs);
      // System.out.println("Adding symbol " + symbol + " at pos: " + i);
      this.symbol2intMap.put(symbol, Integer.valueOf(rel));
      int2symbolMap.add(symbol);
    }
    return rel;
  }

  /**
   * Get value of symbol in table.
   * 
   * @param symbol
   *          the input string.
   * @return the symbol's number, if the symbol is in the table, and <code>start-1</code>, else
   *         (where <code>start</code> is the code point of the first symbol).
   */
  public int get(String symbol) {
    Integer i = symbol2intMap.get(symbol);
    return (i == null) ? start - 1 : i;
  }

  /**
   * Find the symbol corresponding to a value.
   * 
   * @param i
   *          the number that we want to get the string value for.
   * @return the symbol corresponding to the input number, if it exists, and null, else.
   */
  public String getSymbol(int i) {
    int abs = rel2abs(i);
    if (abs < 0 || abs >= this.int2symbolMap.size()) {
      // System.out.println("Out of bounds error in SymbolTable object");
      return null;
    }
    return int2symbolMap.get(abs);
  }

  /**
   * Returns number of symbols in table.
   * 
   * @return The number of symbols in the table.
   */
  public int size() {
    return this.int2symbolMap.size();
  }

  /**
   * Returns the string representation of the internal hash table.
   * 
   * @return A string representing the symbol table.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("{ ");
    final int max = getStart() + size();
    for (int i = getStart(); i < max; i++) {
      buf.append(i + ":" + getSymbol(i));
      if ((i + 1) < max) {
        buf.append(", ");
      }
    }
    buf.append(" }");
    return buf.toString();
  }

  /**
   * Read a file line by line, and create an entry for each line. Empty lines are ignored. Lines
   * containing only whitespace are not.
   * 
   * @param filename
   *          The name of the file to be read in.
   * @return The SymbolTable created from the file.
   * @exception IOException
   *              Errors reading in the file.
   */
  public static SymbolTable readFromFile(String filename) throws IOException {
    File stf = new File(filename);
    FileReader fr = null;
    BufferedReader in = null;
    try {
      fr = new FileReader(stf);
      in = new BufferedReader(fr);
      SymbolTable st = new SymbolTable();
      String word;
      while ((word = in.readLine()) != null) {
        if (!word.equals("")) {
          st.set(word);
        }
      }
      return st;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

}
