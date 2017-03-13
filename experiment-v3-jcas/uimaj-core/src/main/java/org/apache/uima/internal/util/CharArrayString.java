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

/**
 * An unsafe String class based on a publicly accessible character array. This class aims to provide
 * similar functionality as java.lang.String, but without the overhead of copying at creation time.
 * Consequently, you should only use this class if you have complete control over the underlying
 * memory (i.e., char array).
 * 
 * 
 * @version $Id: CharArrayString.java,v 1.1 2002/09/30 19:09:09 goetz Exp $
 */
public class CharArrayString {

  private final int start;

  private final int len;

  private final int end;

  private final char[] chars;

  private static final int SEED1 = 31415;

  private static final int SEED2 = 27183;

  /**
   * Create a new CharArrayString from a String.
   * 
   * @param string
   *          The input string. The content of this string is copied.
   * @pre string != null
   */
  public CharArrayString(String string) {
    super();
    this.len = string.length();
    this.start = 0;
    this.end = this.len;
    this.chars = new char[this.len];
    string.getChars(0, this.len, this.chars, 0);
  }

  /**
   * Create a new CharArrayString from an array of characters.
   * 
   * @param charArray
   *          The input char array.
   * @pre charArray != null
   */
  public CharArrayString(char[] charArray) {
    this(charArray, 0, charArray.length);
  }

  /**
   * Create a new CharArrayString from an array of characters.
   * 
   * @param charArray
   *          The input char array.
   * @param startPos
   *          The start of the string.
   * @param length
   *          The length of the string.
   * @pre charArray != null
   * @pre startPos &ge; 0
   * @pre startPos &le; charArray.length
   * @pre length &ge; 0
   * @pre length &le; charArray.length - startPos
   */
  public CharArrayString(char[] charArray, int startPos, int length) {
    super();
    this.start = startPos;
    this.len = length;
    this.end = this.start + this.len;
    this.chars = charArray;
  }

  /**
   * Get the length of the string.
   * 
   * @return The length.
   */
  public int length() {
    return this.len;
  }

  /**
   * Get the start position of the string in the internal array.
   * 
   * @return The start position.
   */
  public int getStart() {
    return this.start;
  }

  /**
   * Get the end position of the string in the internal array.
   * 
   * @return The end position.
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * Get the internal character array.
   * 
   * @return The char array.
   */
  public char[] getChars() {
    return this.chars;
  }

  /**
   * Trim this.
   * 
   * @return A trimmed version.
   */
  public CharArrayString trim() {
    int newStart = this.start;
    int newEnd = this.end - 1;
    while (newStart <= newEnd) {
      if (Character.isWhitespace(this.chars[newStart])) {
        ++newStart;
      } else {
        break;
      }
    }
    while (newEnd >= newStart) {
      if (Character.isWhitespace(this.chars[newEnd])) {
        --newEnd;
      } else {
        break;
      }
    }
    ++newEnd;
    if ((this.start != newStart) || (this.end != newEnd)) {
      return new CharArrayString(this.chars, newStart, newEnd - newStart);
    }
    return this;
  }

  /**
   * Returns a substring. The position parameters are interpreted relative to the string represented
   * in this object, not the underlying char array. Note that the substring is NOT a copy, but uses
   * the same underlying char array.
   * 
   * @param startPos
   *          The start of the substring.
   * @param endPos
   *          The end of the substring.
   * @return The corresponding substring.
   * @throws IndexOutOfBoundsException
   *           If the position parameters are not valid string positions.
   */
  public CharArrayString substring(int startPos, int endPos) throws IndexOutOfBoundsException {
    if (startPos >= this.len || startPos > endPos || startPos < 0) {
      throw new IndexOutOfBoundsException();
    }
    return new CharArrayString(this.chars, this.start + startPos, endPos - startPos);
  }

  /**
   * Return a substring starting at a given position.
   * 
   * @param startPos
   *          The start position of the substring.
   * @return A new substring, starting at <code>startPos</code>.
   */
  public CharArrayString substring(int startPos) {
    return this.substring(startPos, this.len);
  }

  /**
   * Find the last occurence of a character.
   * 
   * @param c
   *          The char we're looking for.
   * @return The last position of the character, or <code>-1</code> if the character is not
   *         contained in the string.
   */
  public int lastIndexOf(char c) {
    final int last = this.end - 1;
    for (int pos = last; pos >= this.start; pos--) {
      if (this.chars[pos] == c) {
        return pos - this.start;
      }
    }
    return -1;
  }

  /**
   * Return a string representation.
   * 
   * @return The string version of this CharArrayString.
   */
  public String toString() {
    return new String(this.chars, this.start, this.len);
  }

  /**
   * Check of we end in a give string suffix.
   * 
   * @param string
   *          The string suffix we're looking for.
   * @return <code>true</code> iff <code>string</code> is a suffix of this.
   * @pre string != null
   */
  public boolean endsWith(CharArrayString string) {
    if (string.len > this.len) {
      return false;
    }
    int thisPos = this.end;
    int stringPos = string.end;
    while (stringPos > string.start) {
      --thisPos;
      --stringPos;
      if (this.chars[thisPos] != string.chars[stringPos]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if we end in a given character.
   * 
   * @param c
   *          The character.
   * @return <code>true</code> iff we end in <code>c</code>.
   */
  public boolean endsWith(char c) {
    if (this.len <= 0) {
      return false;
    }
    return (this.chars[this.start + this.len - 1] == c);
  }

  /**
   * Return the character at a given position.
   * 
   * @param pos
   *          The position we're looking for.
   * @return The character at the position.
   */
  public char charAt(int pos) {
    if (pos < this.start || pos >= this.len) {
      throw new IndexOutOfBoundsException();
    }
    return this.chars[pos + this.start];
  }

  /**
   * Find an occurence of a given character after some position.
   * 
   * @param c
   *          The char we're looking for.
   * @param offset
   *          An offset after which we start looking.
   * @return The position, or <code>-1</code> if the char wasn't found.
   * @throws IndexOutOfBoundsException
   *           If <code>offset</code> is less than 0.
   */
  public int indexOf(char c, int offset) throws IndexOutOfBoundsException {
    if (offset < 0) {
      throw new IndexOutOfBoundsException();
    }
    int pos = this.start + offset;
    while (pos < this.end) {
      if (this.chars[pos] == c) {
        return pos - this.start;
      }
      ++pos;
    }
    return -1;
  }

  /**
   * Find the first occurence of a given char.
   * 
   * @param c
   *          The char we're looking for.
   * @return The position of the char, or <code>-1</code> if the char couldn't be found.
   */
  public int indexOf(char c) {
    return this.indexOf(c, 0);
  }

  /**
   * Set the char at a certain position.
   * 
   * @param pos
   *          The position where to set the char.
   * @param c
   *          The char to set.
   * @throws IndexOutOfBoundsException
   *           If <code>pos</code> is out of bounds.
   */
  public void setChar(int pos, char c) throws IndexOutOfBoundsException {
    pos = pos + this.start;
    if (pos < this.start || pos >= this.end) {
      throw new IndexOutOfBoundsException();
    }
    this.chars[pos] = c;
  }

  /**
   * Copy this string.
   * 
   * @return A copy.
   */
  public CharArrayString copy() {
    final char[] newChars = new char[this.len];
    System.arraycopy(this.chars, 0, newChars, 0, this.len);
    return new CharArrayString(newChars, 0, this.len);
  }

  /**
   * Get the hash code for this object.
   * 
   * @return The hash code.
   */
  public int hashCode() {
    return hashCode(this.chars, this.start, this.end);
  }

  /**
   * A static method to compute the hash code for a character range in an array.
   * 
   * @param charArray -
   * @param startPos -
   * @param endPos -
   * @return The hash code.
   * @pre charArray != null
   * @pre startPos &ge; 0
   * @pre endPos &ge; startPos
   * @pre charArray.length &ge; endPos
   */
  public static final int hashCode(char[] charArray, int startPos, int endPos) {
    // Universal hashing (pseudo-random), out of Sedgewick.
    int hash = 0;
    int a = SEED1;
    int b = SEED2;
    for (int i = startPos; i < endPos; i++) {
      hash = a * hash + charArray[i];
      a = a * b;
    }
    return Math.abs(hash);
  }

  /**
   * Compute a hash-code for a string.
   * 
   * @param s
   *          The string to get the hash code for.
   * @return A hash code.
   */
  public static final int hashCode(String s) {
    // Universal hashing (pseudo-random), out of Sedgewick.
    if (s == null) {
      return 0;
    }
    int hash = 0;
    int a = SEED1;
    final int b = SEED2;
    final int strLen = s.length();
    for (int i = 0; i < strLen; i++) {
      hash = a * hash + s.charAt(i);
      a = a * b;
    }
    return Math.abs(hash);
  }

  /**
   * Check for equality with another CharArrayString.
   * 
   * @param o
   *          The other string.
   * @return <code>true</code> iff the two strings are equal.
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (o instanceof CharArrayString) {
      CharArrayString s = (CharArrayString) o;
      if (this.len != s.len) {
        return false;
      }
      int j = s.start;
      for (int i = this.start; i < this.end; i++) {
        if (this.chars[i] != s.chars[j]) {
          return false;
        }
        ++j;
      }
      return true;
    }
    return false;
  }

  // public static void main(String[] args) {
  // CharArrayString s1 = new CharArrayString("");
  // CharArrayString s2 = new CharArrayString(" foo ");
  // CharArrayString s3 = new CharArrayString("foo\r");
  // CharArrayString s4 = new CharArrayString("foo bar");
  // System.out.println(">" + s1 + "< trims to >" + s1.trim() + "<");
  // System.out.println(">" + s2 + "< trims to >" + s2.trim() + "<");
  // System.out.println(">" + s3 + "< trims to >" + s3.trim() + "<");
  // System.out.println(">" + s4 + "< trims to >" + s4.trim() + "<");
  // }

}
