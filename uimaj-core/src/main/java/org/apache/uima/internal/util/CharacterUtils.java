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
 * Collection of utilities for character handling. Contains utilities for semi-automatically
 * creating lexer rules.
 */
public class CharacterUtils {

  /**
   * Represents character range.
   */
  private static class CharRange {
    private char start;

    private char end;
  }

  /**
   * Constructor for CharacterUtils.
   */
  public CharacterUtils() {
    super();
  }

  private static final boolean isType(char c, int[] types) {
    final int charType = Character.getType(c);
    final int max = types.length;
    for (int i = 0; i < max; i++) {
      if (charType == types[i]) {
        return true;
      }
    }
    return false;
  }

  private static ArrayList<CharRange> getCharacterRanges(int[] charSpecs) {
    final ArrayList<CharRange> ranges = new ArrayList<CharRange>();
    CharRange range;
    // Max value needs special case since characters wrap.
    for (char c = Character.MIN_VALUE; c <= Character.MAX_VALUE; c++) {
      if (isType(c, charSpecs)) {
        range = new CharRange();
        range.start = c;
        range.end = c;
        if (c == Character.MAX_VALUE) {
          break;
        }
        ++c;
        while (c <= Character.MAX_VALUE && isType(c, charSpecs)) {
          range.end = c;
          if (c == Character.MAX_VALUE) {
            break;
          }
          ++c;
        }
        ranges.add(range);
        // System.out.println(
        // "Adding range: "
        // + toUnicodeChar(range.start)
        // + " - "
        // + toUnicodeChar(range.end));
      }
      if (c == Character.MAX_VALUE) {
        break;
      }
    }
    return ranges;
  }

  /**
   * Create a hex representation of the UTF-16 encoding of a Java char. This is the representation
   * that's understood by Java when reading source code.
   * 
   * @param c
   *          The char to be encoded.
   * @return String Hex representation of character. For example, the result of encoding
   *         <code>'A'</code> would be <code>"\u0041"</code>.
   */
  public static String toUnicodeChar(char c) {
    String prefix = "\\u";
    String code = Integer.toHexString(c);
    switch (code.length()) {
      case 1: {
        return prefix + "000" + code;
      }
      case 2: {
        return prefix + "00" + code;
      }
      case 3: {
        return prefix + "0" + code;
      }
      default: {
        return prefix + code;
      }
    }
  }

  /**
   * Create a hex representation of the UTF-16 encoding of a Java char. This is the representation
   * that's understood by the JavaCC lexer.
   * 
   * @param c
   *          The char to be encoded.
   * @return String Hex representation of character. For example, the result of encoding
   *         <code>'A'</code> would be <code>"0x0041"</code>.
   */
  public static String toHexString(char c) {
    String prefix = "0x";
    String code = Integer.toHexString(c);
    switch (code.length()) {
      case 1: {
        return prefix + "000" + code;
      }
      case 2: {
        return prefix + "00" + code;
      }
      case 3: {
        return prefix + "0" + code;
      }
      default: {
        return prefix + code;
      }
    }
  }

  /**
   * Generate an ArrayList of CharRanges for what Java considers to be a letter. I use this as input
   * to Unicode agnostic lexers like ANTLR.
   * 
   * @return ArrayList A list of character ranges.
   */
  public static ArrayList<CharRange> getLetterRange() {
    int[] types = new int[] { Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
        Character.TITLECASE_LETTER, Character.MODIFIER_LETTER, Character.OTHER_LETTER };
    return getCharacterRanges(types);
  }

  /**
   * Generate an ArrayList of CharRanges for what Java considers to be a digit. I use this as input
   * to Unicode agnostic lexers like ANTLR.
   * 
   * @return ArrayList A list of character ranges.
   */
  public static ArrayList<CharRange> getDigitRange() {
    int[] types = new int[] { Character.DECIMAL_DIGIT_NUMBER };
    return getCharacterRanges(types);
  }

  public static void printAntlrLexRule(String name, ArrayList<CharRange> charRanges) {
    CharRange range;
    System.out.print(name + " : ");
    StringBuffer spaceBuffer = new StringBuffer();
    StringUtils.printSpaces(name.length(), spaceBuffer);
    String spaces = spaceBuffer.toString();
    for (int i = 0; i < charRanges.size(); i++) {
      if (i != 0) {
        System.out.print("\n" + spaces + " | ");
      }
      range = charRanges.get(i);
      if (range.start == range.end) {
        System.out.print(" '" + toUnicodeChar(range.start) + "'");
      } else {
        System.out.print(" '" + toUnicodeChar(range.start) + "' .. '" + toUnicodeChar(range.end)
                + "' ");
      }
    }
    System.out.println("\n" + spaces + " ;");
  }

  public static void printJavaCCLexRule(String name, ArrayList<CharRange> charRanges) {
    CharRange range;
    System.out.print(name + " = ");
    StringBuffer spaceBuffer = new StringBuffer();
    StringUtils.printSpaces(name.length(), spaceBuffer);
    String spaces = spaceBuffer.toString();
    for (int i = 0; i < charRanges.size(); i++) {
      if (i != 0) {
        System.out.print("\n" + spaces + " | ");
      }
      range = charRanges.get(i);
      if (range.start == range.end) {
        System.out.print(toHexString(range.start));
      } else {
        System.out.print("[" + toHexString(range.start) + ".." + toHexString(range.end) + "]");
      }
    }
    System.out.println("\n" + spaces + " ;");
  }

  public static void main(String[] args) {
    ArrayList<CharRange> letters = getDigitRange();
    // ArrayList letters = getLetterRange();
    // getCharacterRanges(new int[] { Character.UPPERCASE_LETTER });
    printJavaCCLexRule("udigit", letters);
  }

}
