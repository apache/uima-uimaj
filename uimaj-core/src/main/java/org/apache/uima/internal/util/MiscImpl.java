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

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class MiscImpl {

  private static final Pattern whitespace = Pattern.compile("\\s");

  public static String replaceWhiteSpace(String s, String replacement) {
    return whitespace.matcher(s).replaceAll(replacement);
  }
   
  /**
   * @param s starting frames above invoker
   * @param n max number of callers to return
   * @return  x called by: y ...
   */
  public static StringBuilder getCallers(final int s, final int n) {
    StackTraceElement[] e = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();
    for (int i = s + 2; i < s + n + 3; i++) {
      if (i >= e.length) break;
      if (i != s + 2) {
        sb.append("  called_by: ");
      }
      sb.append(formatcaller(e[i]));      
    }
    return sb;
  }

  /**
   * @return the name of the caller in the stack
   */
  public static String getCaller() {
    StackTraceElement[] e = Thread.currentThread().getStackTrace();
    return formatcaller(e[4]) + "called by: " + formatcaller(e[5]);
  }
  
  private static String formatcaller(StackTraceElement e) {
    String n = e.getClassName();
    return n.substring(1 + n.lastIndexOf('.')) + "." + e.getMethodName() + "[" + e.getLineNumber() + "]";
  }
  
  public static final String blanks = "                                                     ";
  public static final String dots = "...";
  
  public static String elide(String s, int n) {
    return elide(s, n, true);
  }
  
  public static String elide(String s, int n, boolean pad) {
    if (s == null) {
      s = "null";
    }
    int sl = s.length();
    if (sl <= n) {
      return s + (pad ? blanks.substring(0, n - sl) : "");
    }
    int dl = 1; // (n < 11) ? 1 : (n < 29) ? 2 : 3;  // number of dots
    int ss = (n - dl) / 2;
    int ss2 = ss + (( ss * 2 == (n - dl)) ? 0 : 1);
    return s.substring(0, ss) + dots.substring(0, dl) + s.substring(sl - ss2);
  }  
  
  public static void timeLoops(String title, int iterations, Callable<Object> r) throws Exception {
    long shortest = Long.MAX_VALUE;
    for (int i = 0; i < iterations; i++) {
      long startTime = System.nanoTime();
      r.call();
      long time = (System.nanoTime() - startTime)/ 1000;  // microseconds
      if (time < shortest) {
        shortest = time;
        System.out.format("%s: speed is %,d microseconds on iteration %,d%n", title, shortest, i);
      }
    }
  }

//  public static void main(String[] args) {
//    System.out.println(elide("uninflectedWord", 11));
//  }
}
