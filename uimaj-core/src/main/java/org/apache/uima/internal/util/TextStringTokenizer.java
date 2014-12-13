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

import java.util.Arrays;

/**
 * An implementation of a text tokenizer for whitespace separated natural language text.
 * 
 * <p>
 * The tokenizer knows about four different character classes: regular word characters, whitespace
 * characters, sentence delimiters and separator characters. Tokens can consist of
 * <ul>
 * 
 * <li>sequences of word characters and sentence delimiters where the last character is a word
 * character,</li>
 * 
 * <li>sentence delimiter characters (if they do not precede a word character),</li>
 * 
 * <li>sequences of whitespace characters,</li>
 * 
 * <li>and individual separator characters.</li>
 * </ul>
 * 
 * <p>
 * The character classes are completely user definable. By default, whitespace characters are the
 * Unicode whitespace characters. All other characters are word characters. The two separator
 * classes are empty by default. The different classes may have non-empty intersections. When
 * determining the class of a character, the user defined classes are considered in the following
 * order: end-of-sentence delimiter before other separators before whitespace before word
 * characters. That is, if a character is defined to be both a separator and a whitespace character,
 * it will be considered to be a separator.
 * 
 * <p>
 * By default, the tokenizer will return all tokens, including whitespace. That is, appending the
 * sequence of tokens will recover the original input text. This behavior can be changed so that
 * whitespace and/or separator tokens are skipped.
 * 
 * <p>
 * A tokenizer provides a standard iterator interface similar to
 * {@link java.util.StringTokenizer StringTokenizer}. The validity of the iterator can be queried
 * with <code>hasNext()</code>, and the next token can be queried with <code>nextToken()</code>.
 * In addition, <code>getNextTokenType()</code> returns the type of the token as an integer. NB
 * that you need to call <code>getNextTokenType()</code> before calling <code>nextToken()</code>,
 * since calling <code>nextToken()</code> will advance the iterator.
 * 
 * 
 * @version $Id: TextStringTokenizer.java,v 1.6 2003/04/07 14:50:11 goetz Exp $
 */
public class TextStringTokenizer {

  /** Sentence delimiter character/word type. */
  public static final int EOS = 0;

  /** Separator character/word type. */
  public static final int SEP = 1;

  /** Whitespace character/word type. */
  public static final int WSP = 2;

  /** Word character/word type. */
  public static final int WCH = 3;

  private final String text;

  private final int end; // Points to the last character.

  private int pos;

  private char[] eosDels = new char[0];

  private char[] separators = new char[0];

  private char[] whitespace = new char[0];

  private char[] wordChars = new char[0];

  private int nextTokenStart;

  private int nextTokenEnd;

  private int nextTokenType;

  private boolean nextComputed = false;

  private boolean showWhitespace = true;

  private boolean showSeparators = true;

  /**
   * Construct a tokenizer from a Java string.
   * 
   * @param string
   *          The string to tokenize.
   * @pre string != null
   */
  public TextStringTokenizer(String string) {
    // assert(string != null);
    this.text = string;
    this.pos = 0;
    this.end = string.length() - 1;
    setToNext();
  }

  /**
   * Set the flag for showing whitespace tokens.
   * 
   * @param b
   *          The whitespace flag.
   */
  public void setShowWhitespace(boolean b) {
    this.showWhitespace = b;
  }

  /**
   * Set the flag for showing separator tokens.
   * 
   * @param b
   *          The flag.
   */
  public void setShowSeparators(boolean b) {
    this.showSeparators = b;
  }

  /**
   * Set the set of sentence delimiters.
   * 
   * @param chars
   *          A string containing EOS chars.
   */
  public void setEndOfSentenceChars(String chars) {
    if (chars == null) {
      makeSortedList("");
    } else {
      this.eosDels = makeSortedList(chars);
    }
  }

  /**
   * Add to the set of sentence delimiters.
   * 
   * @param chars
   *          A string containing EOS chars.
   */
  public void addToEndOfSentenceChars(String chars) {
    if (chars == null) {
      return;
    }
    this.eosDels = addToSortedList(chars, this.eosDels);
  }

  /**
   * Set the set of separator characters.
   * 
   * @param chars
   *          The separator chars.
   */
  public void setSeparators(String chars) {
    if (chars == null) {
      chars = "";
    }
    this.separators = makeSortedList(chars);
  }

  /**
   * Add to the set of separator characters.
   * 
   * @param chars
   *          Separator chars.
   */
  public void addSeparators(String chars) {
    if (chars == null) {
      return;
    }
    this.separators = addToSortedList(chars, this.separators);
  }

  /**
   * Set the set of whitespace characters (in addition to the Unicode whitespace chars).
   * 
   * @param chars
   *          Whitespace chars.
   */
  public void setWhitespaceChars(String chars) {
    if (chars == null) {
      chars = "";
    }
    this.whitespace = makeSortedList(chars);
  }

  /**
   * Add to the set of whitespace characters.
   * 
   * @param chars
   *          Whitespace chars.
   */
  public void addWhitespaceChars(String chars) {
    if (chars == null) {
      return;
    }
    this.whitespace = addToSortedList(chars, this.whitespace);
  }

  /**
   * Set the set of word characters.
   * 
   * @param chars
   *          Word chars.
   */
  public void setWordChars(String chars) {
    if (chars == null) {
      chars = "";
    }
    this.wordChars = makeSortedList(chars);
  }

  /**
   * Add to the set of word characters.
   * 
   * @param chars
   *          Word chars.
   */
  public void addWordChars(String chars) {
    if (chars == null) {
      return;
    }
    this.wordChars = addToSortedList(chars, this.wordChars);
  }

  /**
   * Get the type of the token returned by the next call to <code>nextToken()</code>.
   * 
   * @return The token type, or <code>-1</code> if there is no next token.
   */
  public int getTokenType() {
    if (this.nextComputed) {
      return this.nextTokenType;
    }
    return -1;
  }

  /**
   * Return <code>true</code> iff there is a next token.
   * 
   * @return <code>true</code> iff there is a next token.
   */
  public boolean isValid() {
    return this.nextComputed;
  }

  /**
   * Reset the tokenizer at any time.
   */
  public void setToFirst() {
    this.pos = 0;
    setToNext();
  }

  /**
   * Return the next token.
   * 
   * @return The next token.
   */
  public String getToken() {
    if (!this.nextComputed) {
      return null;
    }
    return this.text.substring(this.nextTokenStart, this.nextTokenEnd);
  }

  /**
   * Get the start of the token.
   * 
   * @return The start of the token.
   */
  public int getTokenStart() {
    if (!this.nextComputed) {
      return -1;
    }
    return this.nextTokenStart;
  }

  /**
   * Get the end of the token.
   * 
   * @return The token end.
   */
  public int getTokenEnd() {
    if (!this.nextComputed) {
      return -1;
    }
    return this.nextTokenEnd;
  }

  /**
   * Compute the next token.
   */
  public void setToNext() {
    if (this.pos > this.end) {
      this.nextComputed = false;
      return;
    }
    this.nextTokenStart = this.pos;
    int charType = getCharType(this.text.charAt(this.pos));
    switch (charType) {
      case EOS: {
        ++this.pos;
        this.nextTokenType = EOS;
        break;
      }
      case SEP: {
        ++this.pos;
        if (!this.showSeparators) {
          setToNext();
          return;
        }
        this.nextTokenType = SEP;
        break;
      }
      case WSP: {
        ++this.pos;
        while (this.pos <= this.end && getCharType(this.text.charAt(this.pos)) == WSP) {
          ++this.pos;
        }
        if (!this.showWhitespace) {
          setToNext();
          return;
        }
        this.nextTokenType = WSP;
        break;
      }
      case WCH: {
        ++this.pos;
        this.nextTokenType = WCH;
        if (this.pos <= this.end) {
          charType = getCharType(this.text.charAt(this.pos));
        } else {
          break;
        }
        while (this.pos < this.end && (charType == WCH || charType == EOS)) {
          ++this.pos;
          // If the type of the _current_ character is EOS, check what
          // the type of the _next_ character is. If this is the last
          // char in the buffer, we treat it as an EOS char. If the
          // next character exists and is not a word character, then
          // the EOS character will be treated as a separate token,
          // and we break. We need to reset the position, since we're
          // already pointing at the next char at this time. If, on
          // the other hand, the current character is a WCH, keep on
          // looping.
          if (charType == EOS) { // Current char is EOS
            if (this.pos >= this.end) { // If current char is last
              // char...
              --this.pos; // ...reset position...
              break; // ...and break.
            }
            charType = getCharType(this.text.charAt(this.pos));
            // Get type of next char
            if (charType != WCH) { // If next char is not WCH...
              --this.pos; // ...reset position...
              break; // ...and break out of loop.
            }
          }
          charType = getCharType(this.text.charAt(this.pos));
          // Get type of next char and keep on going.
        }
        break;
      }
      default: { // ???
        return;
      }
    }
    this.nextTokenEnd = this.pos;
    this.nextComputed = true;
    return;
  }

  /**
   * Get the type of an individual character.
   * @param c -
   * @return The char type.
   */
  public int getCharType(char c) {
    // First, check user-defined lists in the order end-of-sentence
    // delimiter, separator character, whitespace and finally regular
    // character that can be part of a word.
    if (Arrays.binarySearch(this.eosDels, c) >= 0) {
      return EOS;
    }
    if (Arrays.binarySearch(this.separators, c) >= 0) {
      return SEP;
    }
    if (Arrays.binarySearch(this.whitespace, c) >= 0) {
      return WSP;
    }
    if (Arrays.binarySearch(this.wordChars, c) >= 0) {
      return WCH;
    }

    // If we get here, we check if it's Unicode whitespace.
    // Otherwise, we consider it a word character.
    if (Character.isWhitespace(c)) {
      return WSP;
    }
    return WCH;
  }

  /**
   * Add the characters in <code>s</code> to the sorted array of characters in <code>list</code>,
   * returning a new, sorted array.
   */
  private static final char[] addToSortedList(String s, char[] list) {
    char[] newList = new char[list.length + s.length()];
    System.arraycopy(list, 0, newList, 0, list.length);
    s.getChars(0, s.length(), newList, list.length);
    Arrays.sort(newList);
    return newList;
  }

  private static final char[] makeSortedList(String s) {
    char[] newList = new char[s.length()];
    s.getChars(0, s.length(), newList, 0);
    Arrays.sort(newList);
    return newList;
  }

  /**
   * Test driver.
   */
  // public static void main(String[] args) {
  //
  // if (args.length != 1) {
  // System.out.println(
  // "Usage: java org.apache.uima.text.TextTokenizer <TextFile>");
  // System.exit(1);
  // }
  // try {
  // BufferedReader br =
  // new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
  // StringBuffer buf = new StringBuffer();
  // String line;
  // while ((line = br.readLine()) != null) {
  // buf.append(line + "\n");
  // }
  //
  // FileWriter fw = new FileWriter(args[0] + ".out");
  //
  // TextStringTokenizer tokenizer = new TextStringTokenizer(buf.toString());
  // tokenizer.setSeparators("/-*&@");
  // tokenizer.addWhitespaceChars(",");
  // tokenizer.setEndOfSentenceChars(".!?");
  // tokenizer.setShowWhitespace(false);
  // int tokenType;
  // int wordCounter = 0;
  // int sepCounter = 0;
  // int endOfSentenceCounter = 0;
  // long time = System.currentTimeMillis();
  // while (tokenizer.isValid()) {
  // tokenType = tokenizer.getTokenType();
  // switch (tokenType) {
  // case EOS :
  // {
  // ++endOfSentenceCounter;
  // break;
  // }
  // case SEP :
  // {
  // ++sepCounter;
  // break;
  // }
  // case WSP :
  // {
  // break;
  // }
  // case WCH :
  // {
  // ++wordCounter;
  // if ((wordCounter % 100000) == 0) {
  // System.out.println("Number of words tokenized: " + wordCounter);
  // }
  // break;
  // }
  // default :
  // {
  // System.out.println(
  // "Something went wrong, fire up that debugger!");
  // return;
  // }
  // }
  // fw.write(tokenizer.getToken() + "\n");
  // tokenizer.setToNext();
  // // System.out.println("Token: " + tokenizer.nextToken());
  // }
  // time = System.currentTimeMillis() - time;
  // System.out.println("Number of words: " + wordCounter);
  // System.out.println("Time used: " + new TimeSpan(time));
  // fw.close();
  // } catch (IOException e) {
  // e.printStackTrace();
  // System.exit(1);
  // }
  // }
}
