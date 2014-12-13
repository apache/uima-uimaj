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
package org.apache.uima.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Share common underlying char[] among strings: Optimize sets of strings for
 * efficient storage
 * 
 * Apply it to a set of strings.
 * 
 * Lifecycle: 
 *   1) make an instance of this class 
 *   2) call .add (String or String[]) for all strings in the set 
 *   
 *   or - skip 1 and 2 and pass in a String [] that can be modified (sorted]
 *   
 *   3) call .optimize()
 *    
 *   4) call
 *        .getString(String) or .getStringArray(String[]) - returns new string objects
 *        .updateStringArray(String[]) to replace original Strings 
 *        .getOffset(String) - returns int offset in some common string
 *        .getCommonStringIndex(String)
 *   5) call getCommonStrings to get all the common strings
 *   6) Let the GC collect the instance of this class
 * 
 * The strings are added first to a big list, instead of directly to a
 * stringbuilder in order to improve reuse by sorting for longest strings first
 * 
 * The commonStrings are kept in an array.  There are more than one to support
 * very large amounts, in excess of 2GB.  
 * 
 * Nulls, passed in as strings, are mostly ignored, but handled appropriately.
 * 
 * 
 */
public class OptimizeStrings {
  

  /**
   * splitSize = 100 means 
   * the common string can be as large as 100
   * the common string can hold a string of length 100 
   */
  // not final static for testing
  private int splitSize = Integer.MAX_VALUE - 2;  // avoid boundary issues
  
  private ArrayList<String> inStrings = new ArrayList<String>();
  
  /**
   * A two hop map from strings to offsets is used.
   * "index" (int):
   *    The first hop: goes from string to an int index, incrementing 1 per
   *      unique shared common string segment
   * "offset" (int):     
   * The 2nd hop: goes from the first index to the actual int offset in
   *   the particular associated piece of the common strings.
   * The lastIndexInCommonStringA array holds the last index value
   *   within each of the common string segments
   */
  
  // the value is either
  //   an int index into the offset table which has the offset in the common string
  //   the intindex in the str aux heap for this string, in the deserialized form
  //   These are distinguished by having the aux heap index be negative
  private Map<String, Integer> stringToIndexMap;
  // map from string index its offset in the piece of the common string array
  private int[]             offsets;  
  private String[]          commonStringsA;
  /**
   * holds the last index (counting down in sort order) that is valid for 
   * the corresponding common string segment.
   */
  private int[] lastIndexInCommonStringsA;
  
  private Map<String, String> returnedStrings = new HashMap<String, String>();
  
  private long              savedCharsExact   = 0;
  private long              savedCharsSubstr  = 0;
  private int               nextSeq           = -1;
  
  private final boolean     doMeasurement;
  
  public OptimizeStrings(boolean doMeasurement) {
    this.doMeasurement = doMeasurement;
  }
  
  // for testing, mainly
  public OptimizeStrings(boolean doMeasurement, int splitSize) {
    this.doMeasurement = doMeasurement;
    this.splitSize = splitSize;
  }

  /**
   * The number of characters saved - for statistical reporting only
   * @return the number of characters saved
   */
  public long getSavedCharsExact() {
    return savedCharsExact;
  }
  
  public long getSavedCharsSubstr() {
    return savedCharsSubstr;
  }

  /**
   * The list of common strings
   * @return the list of common strings
   */
  public String[] getCommonStrings() {
    return commonStringsA;
  }

  /**
   * null strings not added
   * 0 length strings added
   * @param s -
   */
  public void add(String s) {
    if (inStrings.size() == (Integer.MAX_VALUE -1)) {
      throw new RuntimeException(String.format("Exceeded size limit, size = %,d%n", inStrings.size()));
    }
    if (null != s) {
      inStrings.add(s);
    }
  }

  public void add(String[] sa) {
    if (null != sa) {
      for (String s : sa) {
        add(s);
      }
    }
  }

  public int getStringIndex(String s) {
    if (null == s) {
      return -1;
    }
    return stringToIndexMap.get(s);
  }
  
  /** 
   * 
   * @param s  must not be null
   * @return a (positive or 0) or negative number.
   * If positive, it is the offset in the common string
   * If negative, -v is the index (starting at 1) that sequentially
   * increases, for each new unique string fetched using this
   * method.
   */
  public int getIndexOrSeqIndex(String s) {
    if (null == s) {
      throw new RuntimeException();
    }
    int v = stringToIndexMap.get(s);
    // v is 0 for ""
    if (v >= 0) {
      stringToIndexMap.put(s, nextSeq--);
    }
    return v;
  }
  
  /**
   * return a string which is made as a substring of the common string
   * 
   * @param s -
   * @return an equal string, made as substring of common string instance
   *         equal results return the same string
   */
  public String getString(String s) {
    if (null == s) {
      return null;
    }
    int i = getStringIndex(s);
    int offset = getOffset(i);
    String r = commonStringsA[getCommonStringIndex(i)].substring(offset, offset + s.length());
    String rs = returnedStrings.get(r);
    if (null != rs) {
      return rs;
    }
    returnedStrings.put(r, r);
    return r;
  }
  
  public long getOffset(String s) {
    if (null == s) {
      return -1;
    }
    return offsets[getStringIndex(s)];
  }
  
  public int getOffset(int i) {
    return offsets[i];
  }
  
  /**
   * @param index an index (not offset) to the sorted strings, 
   * @return the index of the segment it belongs to
   */
  public int getCommonStringIndex(int index) {
    for (int i = 0; i < lastIndexInCommonStringsA.length; i++) {
      if (index >= lastIndexInCommonStringsA[i]) {
        return i;
      }
    }
    throw new IllegalArgumentException("index out of range, must be >= 0, was " + index);
  }

  public String[] getStringArray(String[] sai) {
    if (null == sai) {
      return null;
    }
    String[] sa = new String[sai.length];
    for (int i = 0; i < sai.length; i++) {
      sa[i] = getString(sai[i]);
    }
    return sa;
  }

  public void updateStringArray(String[] sa) {
    if (null == sa) {
      return;
    }
    for (int i = 0; i < sa.length; i++) {
      sa[i] = getString(sa[i]);
    }
  }
  
  /**
   * Fully checking indexof for every new string is prohibitively expensive We
   * do a partial check - only checking if a string is a substring of the
   * previous one
   */
  public void optimize() {
    String[] sa = inStrings.toArray(new String[inStrings.size()]);
    optimizeI(sa); 
    inStrings = new ArrayList<String>();  // release space
  }
     
  private void optimizeI(String[] sortedStrings) {
    savedCharsExact = 0;
    savedCharsSubstr = 0;
    StringBuilder sb = new StringBuilder();
        
    String[] sortedStrings2 = sortStrings(sortedStrings);
    
    int ssLength;
    if (sortedStrings2 != sortedStrings) {
      // in this case, duplicates have already been eliminated
      ssLength = sortedStrings2.length;
      sortedStrings = sortedStrings2;
    } else {
      ssLength = eliminateSortedStringDuplicates(sortedStrings);
    }
    // create Offsets table
    // sorted so longer ones follow shorter subsumed string
    String previous = "";
    int previousOffset = 0;
    offsets = new int[ssLength];
    List<Integer> lastIndexInCommonStrings = new ArrayList<Integer>();
    List<String> commonStrings = new ArrayList<String>();

    for (int i = ssLength - 1; i >= 0; i--) {
      String s = sortedStrings[i];
      int sLength = s.length();
      if (sLength > splitSize) {
        throw new RuntimeException(String.format("String too long, length = %,d, max length allowed is %,d", sLength, splitSize));
      }
      if (previous.startsWith(s)) {
        offsets[i] = previousOffset;
        if (doMeasurement) {  // equals case counted in dupl removal
          savedCharsSubstr += sLength;
        }
      } else {
        if (sb.length() + sLength  > splitSize) {
          commonStrings.add(sb.toString());
          sb = new StringBuilder();
          lastIndexInCommonStrings.add(i + 1);  // previous index because index counting down
        }
        offsets[i] = previousOffset = sb.length();
        sb.append(previous = s);
      }
    }
    commonStrings.add(sb.toString());  // add the last (partial) one
    lastIndexInCommonStrings.add(0);   // the last index 
    
    // convert List<Integer> to int[]
    lastIndexInCommonStringsA = new int[lastIndexInCommonStrings.size()];
    for (int i = 0; i < lastIndexInCommonStrings.size(); i++) {
      lastIndexInCommonStringsA[i] = lastIndexInCommonStrings.get(i);
    }
    
    commonStringsA = commonStrings.toArray(new String[commonStrings.size()]);
    

    // prepare map from original string object to index in sorted arrays and offsets
    // index also used to find common string segment.
    stringToIndexMap = new HashMap<String, Integer>(ssLength);
    for (int i = ssLength - 1; i >= 0; i--) {
      stringToIndexMap.put(sortedStrings[i], i);
    }
  }
  
  /**
   * Scan sorted strings, removing duplicates.
   * Copy refs so that final array has no dups up to new length
   * Return new length, but don't trim array
   * @param sortedStrings
   * @return new length
   */
  private int eliminateSortedStringDuplicates(String[] sortedStrings) {
    if (sortedStrings.length == 0) {
      return 0;
    }
    String prev = sortedStrings[0];
    int to = 1;
    for(int from = 1; from < sortedStrings.length; from++) {
      String s = sortedStrings[from];
      if (s.equals(prev)) {
        if (doMeasurement) {
          savedCharsExact += s.length();
        }
        continue;
      }
      prev = s;
      sortedStrings[to] = s;
      to++;
    }    
    return to;  // to is length, is also where next string would be copied to
  }
  
  private String[] sortStrings(String[] sa) {
    try {
      Arrays.sort(sa);

      // testing
//      Set<String> orderedSet = new TreeSet<String>();
//      for (String s : inStrings) {
//        orderedSet.add(s);
//      }
//      Iterator<String> it = orderedSet.iterator();
//      for (int i = 0; i < sa.length; i++) {
//        String s = it.next();
//        if (sa[i] != s) {
//          throw new RuntimeException(String.format(
//              "Mismatch, old way sort(i = %,d) = %s, new way is %s", i, s));
//        }
//        while ((i < sa.length - 1) && (sa[i + 1].equals(sa[i]))) {
//         i++;
//        }
//      }
      // end of test
      
      
      return sa;
    } catch (StackOverflowError e) {
      // see https://issues.apache.org/jira/browse/UIMA-2515
      // debug/test
//      System.out.println("hit stack overflow");
      Set<String> orderedSet = new TreeSet<String>();
      for (String s : inStrings) {
        if (!orderedSet.add(s)) {
          savedCharsExact += s.length();
        }
      }
      sa = new String[orderedSet.size()]; // has dups removed already
      Iterator<String> it = orderedSet.iterator();
      for (int i = 0; i < sa.length; i++) {
        sa[i] = it.next();
      }
      return sa;
    }
  }
}
