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

import java.lang.reflect.Field;

import junit.framework.TestCase;

public class OptimizeStringsTest extends TestCase {

  // modern Javas do this in various ways, can't depend on internal impl
//  private static Field STRING_OFFSET;
//  static {
//    try {
//   // Found that IBM Java 8 version 8.0.4.2 returns a value for the field "offset", but it's 0
//      // and there doesn't seem to be an offset field in that class.
//      if (System.getProperty("java.version").startsWith("1.8")) {
//        STRING_OFFSET = null;
//      } else {
//        STRING_OFFSET =  String.class.getDeclaredField("offset");
//        STRING_OFFSET.setAccessible(true);
//      }
//    } catch (SecurityException e) {
//      throw new RuntimeException(e);
//    } catch (NoSuchFieldException e) {
//      System.err.println("OptimizeStringsTest could not find the String offset field, skipping that part of the test.");
//      STRING_OFFSET = null;
//    }
//  }
    
  public static int getStringOffset(String s) {
    return -1;  // disabled
//    try {
//      if (STRING_OFFSET != null) {
//        return STRING_OFFSET.getInt(s);
//      }
//      return -1;
//    } catch (IllegalArgumentException e) {
//      throw new RuntimeException(e);
//    } catch (IllegalAccessException e) {
//      throw new RuntimeException(e);
//    }
  }
   
  public void testOpStr() {
    OptimizeStrings os = new OptimizeStrings(true, 6);
    os.add("a0");
    os.add("b0");
    os.add("c0");
    os.add("d0");
    os.add("e0");
    os.add("f0");
    os.optimize();
    String a = os.getString("a0");
    String b = os.getString("b0");
    String c = os.getString("c0");
    String d = os.getString("d0");
    String e = os.getString("e0");
    String f = os.getString("f0");
    assertEquals(a, "a0");
    assertEquals(b, "b0");
    assertEquals(c, "c0");
    assertEquals(d, "d0");
    assertEquals(e, "e0");
    assertEquals(f, "f0");
    checkOffset(f, 0);
    checkOffset(e, 2);
    checkOffset(d, 4);
    checkOffset(c, 0);
    checkOffset(b, 2);
    checkOffset(a, 4);
    
 }
  
  private void checkOffset(String s, int offset) {
    int so = getStringOffset(s);
    if (so >= 0) {
      assertEquals(so, offset);
    }
  }
  
  public void testSort() {
    OptimizeStrings os = new OptimizeStrings(true, 5);
    
    os.add("abc");
    os.add("abcde");
    os.add(new String[] {"defgh", "fg"});
    
    os.optimize();
    String abc = os.getString("abc");
    String fg = os.getString("fg");
    String abcde = os.getString("abcde");
    String defgh = os.getString("defgh");
    
    
    assertEquals("abc", abc);
    assertEquals("abcde", abcde);
    assertEquals("fg", fg);
    assertEquals(os.getCommonStrings()[0], "fg");
    assertEquals(os.getCommonStrings()[1], "defgh");
    assertEquals(os.getCommonStrings()[2], "abcde");
    assertEquals(os.getSavedCharsExact() + os.getSavedCharsSubstr(), 3);
    checkOffset(fg, 0);
    checkOffset(defgh, 0);
    checkOffset(abc, 0);
    checkOffset(abcde, 0);   
  }
 
  public void testSort2() {
    OptimizeStrings os = new OptimizeStrings(true, 7);
    
    os.add("abc");
    os.add("abcde");
    os.add(new String[] {"defgh", "fg"});
    
    os.optimize();
    String abc = os.getString("abc");
    String fg = os.getString("fg");
    String abcde = os.getString("abcde");
    String defgh = os.getString("defgh");
    
    
    assertEquals("abc", abc);
    assertEquals("abcde", abcde);
    assertEquals("fg", fg);
    assertEquals(os.getCommonStrings()[0], "fgdefgh");
    assertEquals(os.getCommonStrings()[1], "abcde");
    assertEquals(os.getSavedCharsExact() + os.getSavedCharsSubstr(), 3);
    checkOffset(fg, 0);
    checkOffset(defgh, 2);
    checkOffset(abc, 0);
    checkOffset(abcde, 0);   
  }
  
  public void testSort3() {
    OptimizeStrings os = new OptimizeStrings(true);
    
    os.add("abc");
    os.add("abcde");
    os.add(new String[] {"defgh", "fg"});
    
    os.optimize();
    String abc = os.getString("abc");
    String fg = os.getString("fg");
    String abcde = os.getString("abcde");
    String defgh = os.getString("defgh");
    
    
    assertEquals("abc", abc);
    assertEquals("abcde", abcde);
    assertEquals("fg", fg);
    assertEquals(os.getCommonStrings()[0], "fgdefghabcde");
    assertEquals(os.getSavedCharsExact() + os.getSavedCharsSubstr(), 3);
    checkOffset(fg, 0);
    checkOffset(defgh, 2);
    checkOffset(abc, 7);
    checkOffset(abcde, 7);   
  }
}
