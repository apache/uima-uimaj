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

package org.apache.uima.tttypesystem;

/**
 * Copy of functionality of TAF TokenProperties class with additional token property for n-gram
 * tokens.
 * 
 */
public class TokenProperties {

  // token property bit mask
  public static final int TAF_TOKEN_PROP_LEADING_UPPER = 1;

  public static final int TAF_TOKEN_PROP_TRAILING_UPPER = 2;

  public static final int TAF_TOKEN_PROP_LOWER = 4;

  public static final int TAF_TOKEN_PROP_NUMERIC = 8;

  public static final int TAF_TOKEN_PROP_SPECIAL = 16;

  public static final int TOKEN_PROP_NGRAM = 32;

  // current property bit mask
  private int bits = 0;

  /**
   * standard constructor
   */
  public TokenProperties() {
    super();
  }

  /**
   * initialize the current property bit mask
   * 
   * @param bits
   *          bit mask
   */
  public TokenProperties(int bits) {
    this();
    this.bits = bits;
  }

  /**
   * returns the current property bit mask
   * 
   * @return int - return current bit mask
   */
  public int getInt() {
    return this.bits;
  }

  /**
   * initialize the current property bit mask
   * 
   * @param bits
   *          bit mask
   */
  public void setInt(int bits) {
    this.bits = bits;
  }

  // / true if the first char in the token is upper case
  public boolean hasLeadingUpper() {
    return (this.bits & TAF_TOKEN_PROP_LEADING_UPPER) != 0;
  }

  // / sets the <TT>hasLeadingUpper()</TT> property to <TT>bSetOn</TT>
  public void setLeadingUpper(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TAF_TOKEN_PROP_LEADING_UPPER;
    } else {
      this.bits &= ~TAF_TOKEN_PROP_LEADING_UPPER;
    }
  }

  // / true if some char after the first char in the token is upper case
  public boolean hasTrailingUpper() {
    return (this.bits & TAF_TOKEN_PROP_TRAILING_UPPER) != 0;
  }

  // / sets the <TT>hasTrailingUpper()</TT> property to <TT>bSetOn</TT>
  public void setTrailingUpper(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TAF_TOKEN_PROP_TRAILING_UPPER;
    } else {
      this.bits &= ~TAF_TOKEN_PROP_TRAILING_UPPER;
    }
  }

  // / true if the token has upper case chars (leading or trailing)
  public boolean hasUpper() {
    return (hasLeadingUpper() || hasTrailingUpper());
  }

  // / true if the token has lower case chars
  public boolean hasLower() {
    return (this.bits & TAF_TOKEN_PROP_LOWER) != 0;
  }

  // / sets the <TT>hasLower()</TT> property to <TT>bSetOn</TT>
  public void setLower(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TAF_TOKEN_PROP_LOWER;
    } else {
      this.bits &= ~TAF_TOKEN_PROP_LOWER;
    }
  }

  // / true if the token has numeric chars
  public boolean hasNumeric() {
    return (this.bits & TAF_TOKEN_PROP_NUMERIC) != 0;
  }

  // / sets the <TT>hasNumeric()</TT> property to <TT>bSetOn</TT>
  public void setNumeric(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TAF_TOKEN_PROP_NUMERIC;
    } else {
      this.bits &= ~TAF_TOKEN_PROP_NUMERIC;
    }
  }

  // / true if the token has special chars (e.g. hyphen, period etc.)
  public boolean hasSpecial() {
    return (this.bits & TAF_TOKEN_PROP_SPECIAL) != 0;
  }

  // / sets the <TT>hasSpecial()</TT> property to <TT>bSetOn</TT>
  public void setSpecial(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TAF_TOKEN_PROP_SPECIAL;
    } else {
      this.bits &= ~TAF_TOKEN_PROP_SPECIAL;
    }
  }

  // / true if the token is a n-gram token
  public boolean hasNgram() {
    return (this.bits & TOKEN_PROP_NGRAM) != 0;
  }

  // / sets the <TT>hasNgram()</TT> property to <TT>bSetOn</TT>
  public void setNgram(boolean bSetOn) {
    if (bSetOn) {
      this.bits |= TOKEN_PROP_NGRAM;
    } else {
      this.bits &= ~TOKEN_PROP_NGRAM;
    }
  }

  /* @} */
  /** @name Miscellaneous */
  /* @{ */
  // / true if not hasSpecial() and not hasNumeric()
  public boolean isPlainWord() {
    return !hasSpecial() && !hasNumeric();
  }

  // / true if only hasUpper()
  public boolean isAllUppercaseWord() {
    return (this.bits & ~(TAF_TOKEN_PROP_LEADING_UPPER | TAF_TOKEN_PROP_TRAILING_UPPER)) == 0;
  }

  // / true if only hasLower()
  public boolean isAllLowercaseWord() {
    return (this.bits & ~TAF_TOKEN_PROP_LOWER) == 0;
  }

  /**
   * Check if string starts with uppercase letter and continues with lowercase letters.
   * 
   * @return <code>true</code> if the string is non-empty, the first character is a uppercase
   *         letter, and does not contain specials, numerics or trailing uppers.
   */
  public boolean isInitialUppercaseWord() {
    return ((this.bits & TAF_TOKEN_PROP_LEADING_UPPER) != 0)
                    && ((this.bits & (TAF_TOKEN_PROP_NUMERIC | TAF_TOKEN_PROP_SPECIAL | TAF_TOKEN_PROP_TRAILING_UPPER)) == 0);
  }

  /**
   * true if hasNumeric() && !(hasLower() || hasUpper()) Note: this might have decimal point and
   * sign
   */
  public boolean isPlainNumber() {
    return ((this.bits & TAF_TOKEN_PROP_NUMERIC) != 0)
                    && ((this.bits & (TAF_TOKEN_PROP_LEADING_UPPER | TAF_TOKEN_PROP_LOWER | TAF_TOKEN_PROP_TRAILING_UPPER)) == 0);
  }

  /**
   * Contains only numbers, size > 0.
   */
  public boolean isPureNumber() {
    return this.bits == TAF_TOKEN_PROP_NUMERIC;
  }

  /**
   * Contains only specials, size > 0.
   */
  public boolean isPureSpecial() {
    return this.bits == TAF_TOKEN_PROP_SPECIAL;
  }

  /**
   * Contains only n-gram size > 0.
   */
  public boolean isPureNgram() {
    return this.bits == TOKEN_PROP_NGRAM;
  }

  // / Resets all bits in *this, and returns *this
  public void reset() {
    this.bits = 0;
  }

  // some quick check macros solving the task for characters < 128 inline
  final private static boolean CHECK_U_UPPER(char c) {
    return ((c >= (char) 65 && c <= (char) 90) || Character.isUpperCase(c));
  }

  final private static boolean CHECK_U_LOWER(char c) {
    return ((c >= (char) 97 && c <= (char) 122) || Character.isLowerCase(c));
  }

  final private static boolean CHECK_U_DIGIT(char c) {
    return ((c >= (char) 48 && c <= (char) 57) || Character.isDigit(c));
  }

  // / Resets all bits and reinitializes from the string
  public void initFromString(String s) {
    initFromCharArray(s.toCharArray(), 0, s.length());
  }

  // / Resets all bits and reinitializes from the char array
  public void initFromCharArray(char[] chars, int begin, int len) {
    this.bits = 0;

    int end = begin + len;
    int current = begin;

    if (current >= end) {
      return;
    }
    char currentChar = chars[current];
    if (CHECK_U_LOWER(currentChar)) {
      setLower(true);
    } else if (CHECK_U_UPPER(currentChar)) {
      setLeadingUpper(true);
    } else if (CHECK_U_DIGIT(currentChar)) {
      setNumeric(true);
    } else {
      setSpecial(true);
    }
    ++current;
    while (current < end) {
      currentChar = chars[current];
      if (CHECK_U_LOWER(currentChar)) {
        setLower(true);
      } else if (CHECK_U_UPPER(currentChar)) {
        setTrailingUpper(true);
      } else if (CHECK_U_DIGIT(currentChar)) {
        setNumeric(true);
      } else {
        setSpecial(true);
      }
      ++current;
    }
  }

}
