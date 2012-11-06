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

  private static Field STRING_OFFSET;
  static {
    try {
      STRING_OFFSET = String.class.getDeclaredField("offset");
      STRING_OFFSET.setAccessible(true);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
    
  public static int getStringOffset(String s) {
    try {
      return STRING_OFFSET.getInt(s);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
   
  public void testOpStr() {
    OptimizeStrings os = new OptimizeStrings(true, 4);
    os.add("a");
    os.add("b");
    os.add("c");
    os.add("d");
    os.add("e");
    os.add("f");
    os.optimize();
    String a = os.getString("a");
    String b = os.getString("b");
    String c = os.getString("c");
    String d = os.getString("d");
    String e = os.getString("e");
    String f = os.getString("f");
    assertEquals(a, "a");
    assertEquals(b, "b");
    assertEquals(c, "c");
    assertEquals(d, "d");
    assertEquals(e, "e");
    assertEquals(f, "f");
    assertEquals(getStringOffset(f), 0);
    assertEquals(getStringOffset(e), 1);
    assertEquals(getStringOffset(d), 2);
    assertEquals(getStringOffset(c), 3);
    assertEquals(getStringOffset(b), 0);
    assertEquals(getStringOffset(a), 1);
    
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
    assertEquals(getStringOffset(fg), 0);
    assertEquals(getStringOffset(defgh), 0);
    assertEquals(getStringOffset(abc), 0);
    assertEquals(getStringOffset(abcde), 0);   
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
    assertEquals(getStringOffset(fg), 0);
    assertEquals(getStringOffset(defgh), 2);
    assertEquals(getStringOffset(abc), 0);
    assertEquals(getStringOffset(abcde), 0);   
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
    assertEquals(getStringOffset(fg), 0);
    assertEquals(getStringOffset(defgh), 2);
    assertEquals(getStringOffset(abc), 7);
    assertEquals(getStringOffset(abcde), 7);   
  }
}
