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

package org.apache.uima.pear.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The <code>StringUtil</code> class provides utility methods for working with strings.
 * 
 */

public class StringUtil {

  /**
   * Appends properties specified in a given 'plus' object to a given 'source' object. If the
   * 'override' flag is <code>true</code>, the 'plus' properties will override the 'source'
   * properties with the same name, otherwise the 'source' properties will stay.
   * 
   * @param source
   *          The given 'source' properties object.
   * @param plus
   *          The given 'plus' properties object.
   * @param override
   *          If this flag is <code>true</code>, the 'plus' properties will override the 'source'
   *          properties with the same name, otherwise the 'source' properties will stay.
   * @return The properties object, containing both the given 'source' properties and the given
   *         'plus' properties.
   */
  public static Properties appendProperties(Properties source, Properties plus, boolean override) {
    Properties result = new Properties();
    // copy 'source' to 'result'
    result.putAll(source);
    // append 'plus' to 'result', checking same names
    Enumeration<?> plusNames = plus.propertyNames();
    while (plusNames.hasMoreElements()) {
      String name = (String) plusNames.nextElement();
      String value = plus.getProperty(name);
      if (result.getProperty(name) == null || override)
        result.setProperty(name, value);
    }
    return result;
  }

  /**
   * @return The number of words in a given string.
   * @param text
   *          The given string.
   */
  public static int countWords(String text) {
    int wordCounter = (text.trim().length() > 0) ? 1 : 0;
    for (int i = 0; i < text.length(); i++) {
      char prevChar = (i > 0) ? text.charAt(i - 1) : '\0';
      char currChar = text.charAt(i);
      if (!Character.isWhitespace(currChar)) {
        if (Character.isWhitespace(prevChar))
          wordCounter++;
      }
    }
    return wordCounter;
  }

  /**
   * Return the content of the stack trace for a given <code>Throwable</code> object.
   * 
   * @param error
   *          The given <code>Throwable</code> object.
   * @return The content of the stack trace for the given error.
   */
  public static String errorStackTraceContent(Throwable error) {
    StringWriter sWriter = new StringWriter();
    PrintWriter pWriter = new PrintWriter(sWriter);
    error.printStackTrace(pWriter);
    return sWriter.toString();
  }

  /**
   * @return Text extracted from a given markup string.
   * @param mString
   *          The given markup string.
   */
  public static String extractTextFromMarkup(String mString) {
    StringBuffer buffer = new StringBuffer();
    boolean mSection = false;
    for (int i = 0; i < mString.length(); i++) {
      char ch = mString.charAt(i);
      if (ch == '<')
        mSection = true;
      else if (mSection) {
        if (ch == '>')
          mSection = false;
      } else
        buffer.append(ch);
    }
    return buffer.toString();
  }

  /**
   * Returns a plain name (without package name) of a given Java <code>Class</code>.
   * 
   * @param aClass
   *          The given Java <code>Class</code>.
   * @return The plain name (without package name) of the given Java <code>Class</code>.
   */
  public static String getPlainClassName(Class<?> aClass) {
    int index = aClass.getName().lastIndexOf('.');
    return (index > 0) ? aClass.getName().substring(index + 1) : aClass.getName();
  }

  /**
   * @return <code>true</code>, if all characters in a given string are lowercase letters,
   *         <code>false</code> otherwise.
   * @param string
   *          The given string.
   */
  public static boolean isLowerCase(String string) {
    for (int i = 0; i < string.length(); i++) {
      if (Character.isUpperCase(string.charAt(i)))
        return false;
    }
    return true;
  }

  /**
   * @return <code>true</code>, if all characters in a given string are uppercase letters,
   *         <code>false</code> otherwise.
   * @param string
   *          The given string.
   */
  public static boolean isUpperCase(String string) {
    for (int i = 0; i < string.length(); i++) {
      if (Character.isLowerCase(string.charAt(i)))
        return false;
    }
    return true;
  }

  /**
   * @return If the given string argument occurs as a substring, ignoring case, within the given
   *         string object, then the index of the first character of the first such substring is
   *         returned; if it does not occur as a substring, <code>-1</code> is returned.
   * @param mainStr
   *          The given string object.
   * @param argStr
   *          The given string argument.
   */
  public static int indexOfIgnoreCase(String mainStr, String argStr) {
    String source = mainStr.toLowerCase();
    String pattern = argStr.toLowerCase();
    int index = source.indexOf(pattern);
    return index;
  }

  /**
   * @return If the given string argument occurs as a substring, ignoring case, within the given
   *         string object at a starting index no smaller than <code>fromIndex</code>, then the
   *         index of the first character of the first such substring is returned; if it does not
   *         occur as a substring starting at <code>fromIndex</code> or beyond, <code>-1</code>
   *         is returned.
   * @param mainStr
   *          The given string object.
   * @param argStr
   *          The given string argument.
   * @param fromIndex
   *          The index to start the search from.
   */
  public static int indexOfIgnoreCase(String mainStr, String argStr, int fromIndex) {
    String source = mainStr.toLowerCase();
    String pattern = argStr.toLowerCase();
    int index = source.indexOf(pattern, fromIndex);
    return index;
  }

  /**
   * Converts a given input string to another string that can be used as a 'replacement' string in
   * the <code>String::replaceAll(String regex, String replacement)</code> method. <br>
   * Characters to be escaped are: "\ $".
   * 
   * @param string
   *          The given input string.
   * @return The string that can be used as a 'replacement' string in the
   *         <code>String::replaceAll(String regex, String replacement)</code> method.
   */
  public static String toRegExpReplacement(String string) {
    // replace '\' with '\\'
    String replacement = string.replaceAll("\\\\", "\\\\\\\\");
    // replace '$' with '\$'
    replacement = replacement.replaceAll("\\$", "\\\\\\$");
    return replacement;
  }

  /**
   * Converts a given input string to another string that can be used in all 'regular expression'
   * methods. <br>
   * Characters to be escaped are: "\ . $ ^ { [ ( | ) * + ?".
   * 
   * @param string
   *          The given input string.
   * @return The string that can be used in 'regular expression' methods.
   */
  public static String toRegExpString(String string) {
    final char[] chars2escape = { '.', '$', '^', '{', '[', '(', '|', ')', '*', '+', '?' };
    // replace '\' with '\\'
    String regExString = string.replaceAll("\\\\", "\\\\\\\\");
    StringBuffer escBuff = new StringBuffer();
    StringBuffer repBuff = new StringBuffer();
    for (int i = 0; i < chars2escape.length; i++) {
      escBuff.setLength(0);
      repBuff.setLength(0);
      escBuff.append('\\');
      escBuff.append(chars2escape[i]);
      repBuff.append("\\\\");
      repBuff.append(escBuff);
      regExString = regExString.replaceAll(escBuff.toString(), repBuff.toString());
    }
    return regExString;
  }
}
