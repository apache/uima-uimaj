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

package org.apache.uima.test.junit_extension;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileCompare class provides a several methods, which compare two files or input streams.
 * Most methods are static.
 * 
 * It has a facility to incorporate a regex ignore-differences filter
 * 
 */
public class FileCompare {

  /**
   * TODO Currently only tags containing word characters [a-zA-Z_0-9] are recognised.
   */
  // match "<" followed by 1 or more word_chars followed by ">" followed by "</" followed by
  //   (presumably) the same 1 or more word_chars followed by ">"
  private static final String EMPTY_TAG_REGEX = "(<([\\w]+)>[\\s]*</[\\w]+>)";

  private static Pattern emptyTagPattern = Pattern.compile(EMPTY_TAG_REGEX);
  
  // matches cr if it is followed by a new-line, will be repl with just a new line
  private static Pattern crnlPattern = Pattern.compile("\\r(?=\\n)");

  /**
   * compares two files and return true if the files have the same content.
   * 
   * @param filename1
   *          filename of the first file
   * @param filename2
   *          filename of the second file
   * @return - true if the files have the same content
   * 
   * @throws IOException -
   */
  public static boolean compare(String filename1, String filename2) throws IOException {
    InputStream file1 = null;
    InputStream file2 = null;

    try {
      // create file input stream of the two bytes
      file1 = new FileInputStream(filename1);
      file2 = new FileInputStream(filename2);

      return compare(file1, file2);

    } finally {
      file1.close();
      file2.close();
    }
  }

  /**
   * compares two files and return true if the files have the same content.
   * 
   * @param file1
   *          first file
   * @param file2
   *          second file
   * @return - true if the files have the same content
   * 
   * @throws IOException -
   */
  public static boolean compare(File file1, File file2) throws IOException {
    InputStream inputStream1 = null;
    InputStream inputStream2 = null;

    try {
      // create file input stream of the two bytes
      inputStream1 = new FileInputStream(file1);
      inputStream2 = new FileInputStream(file2);

      return compare(inputStream1, inputStream2);

    } finally {
      inputStream1.close();
      inputStream2.close();
    }
  }

  
  /**
   * compares two files and return true if the files have the same content.
   * 
   * @param filename1
   *          filename of the first file
   * @param in
   *          an input Sream
   * 
   * @return - true if the content is the same
   * 
   * @throws IOException -
   */
  public static boolean compare(String filename1, InputStream in) throws IOException {
    InputStream file1 = null;

    try {
      // create file input stream of the two bytes
      file1 = new FileInputStream(filename1);

      return compare(file1, in);

    } finally {
      file1.close();
    }
  }

  public static boolean compare(InputStream in1, InputStream in2) throws IOException {
    int in1byte, in2byte;

    final int byteBufferSize = 10000;

    in1 = new BufferedInputStream(in1, byteBufferSize);
    in2 = new BufferedInputStream(in2, byteBufferSize);

    in1byte = 0;
    while (in1byte != -1) {
      // read one byte from file1
      in1byte = in1.read();

      // check if byte is whitespace or blank
      if ((!(Character.isWhitespace((char) in1byte))) && (in1byte != ' ') && (in1byte != '\n')
              && (in1byte != '\r')) {
        // read one byte form file2
        in2byte = in2.read();

        // read bytes until byte is no whitespace or blank
        while ((Character.isWhitespace((char) in2byte)) || (in2byte == ' ') || (in2byte == '\n')
                || (in2byte == '\r')) {
          // if byte is whitespace or blank read next byte
          in2byte = in2.read();
        }

        // check if byte from file1 and file2 are the same
        if (in1byte != in2byte) {
          return false; // file content of the two files are not the same
        }
      }
    }
    
    // eof on in1
    in2byte = in2.read();
    while (in2byte != -1) {
      // read bytes until byte is no whitespace or blank
      while ((Character.isWhitespace((char) in2byte)) || (in2byte == ' ') || (in2byte == '\n')
              || (in2byte == '\r')) {
        // if byte is whitespace or blank read next byte
        in2byte = in2.read();
      }
      if (in2byte != -1) {
        return false;  // in2 had more non-whitespace chars after in1 end of file
      }
    }
    return true;
  }
  
  /**
   * Compare 2 strings, ignoring whitespace characters
   * @param in1 -
   * @param in2 -
   * @return -
   */
  public static boolean compareStrings(String in1, String in2) {
    char c1, c2;

    int i1 = 0;
    int i2 = 0;
    
    while (i1 < in1.length()) {
      
      c1 = in1.charAt(i1 ++);

      // check if char is whitespace, and skip it
      if (Character.isWhitespace(c1)) {
        continue;
      }
      
      while (true) {
        if (i2 >= in2.length()) {
          return false;  // ran off the end of string 2
        }

        c2 = in2.charAt(i2 ++);
        if (!Character.isWhitespace(c2)) {
          break;
        }
      }
      
      if (c1 != c2) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares two XML files and returns true, if both have the same content. Different notations for
   * empty tags are considered equal.
   * 
   * @param filename1
   *          Filename of the first XML file.
   * @param filename2
   *          Filename of the second XML file.
   * @return -
   * @throws IOException -
   */
  public static boolean compareXML(String filename1, String filename2) throws IOException {
    File file1 = null;
    File file2 = null;

    String s1 = null;
    String s2 = null;

    ByteArrayInputStream bais1 = null;
    ByteArrayInputStream bais2 = null;

    try {
      file1 = new File(filename1);
      file2 = new File(filename2);

      // read files into strings
      s1 = file2String(file1);
      s2 = file2String(file2);

      // replace empty tags with short notation
      s1 = shortenEmptyTags(s1, filename1);
      s2 = shortenEmptyTags(s2, filename2);

      // create input streams from resulting XML strings
      bais1 = new ByteArrayInputStream(s1.getBytes());
      bais2 = new ByteArrayInputStream(s2.getBytes());

      // compare the two XML strings
      return compare(bais1, bais2);

    } finally {
      bais1.close();
      bais2.close();
    }
  }

  /**
   * Compares two files and returns true, if both have the same content, after
   * filtering using the supplied Pattern.  
   * In addition, 
   *   \r\n is normalized to \n,
   *   multiple spaces and tabs are normalized to a single space 
   * 
   * @param filename1
   *          Filename of the first XML file.
   * @param filename2
   *          Filename of the second XML file.
   * @param pattern 
   *          an instance of Pattern which matches all substrings which should be filtered out of the match
   * @return true, if both have the same content, after filtering using the supplied Pattern.
   * @throws IOException -
   */
  public static boolean compareWithFilter(String filename1, String filename2, Pattern pattern)
      throws IOException {
    File file1 = null;
    File file2 = null;

    String s1 = null;
    String s2 = null;

    file1 = new File(filename1);
    file2 = new File(filename2);

    // read files into strings
    s1 = file2String(file1);
    s2 = file2String(file2);
 
    return compareStringsWithFilter(s1, s2, pattern);
  }  

  // match at least 2 spaces
  private static final Pattern multipleWhiteSpace = Pattern.compile("[ \\t]{2,}");
  
  // match nl space nl
  private static final Pattern emptyLinePattern = Pattern.compile("(?m)^ $");
  
  // match 2 or more nl's in a row
  private static final Pattern multipleNlPattern = Pattern.compile("\\n{2,}");
  
  /**
   * Compare 2 strings, showing where they differ in output to system.out, after
   * doing filtering:
   *   normalize cr nl to nl
   *   normalize &lt;xmltag:&gt;   &lt;/xmltag&gt;  to &lt;xmltag/&gt;
   *   normalize by applying supplied Pattern and deleting anything it matches
   *   normalize by converting all 2 or more spaces/tabs to just 1 space
   * @param s1 -
   * @param s2 -
   * @param pattern -
   * @return -
   */

  public static boolean compareStringsWithFilter(String s1, String s2, Pattern pattern) {
    // apply cr + nl --> nl
    s1 = crnlPattern.matcher(s1).replaceAll("");
    s2 = crnlPattern.matcher(s2).replaceAll("");

    // apply empty xml tag conversion
    s1 = emptyTagPattern.matcher(s1).replaceAll("<$2/>");
    s2 = emptyTagPattern.matcher(s2).replaceAll("<$2/>");

    // apply filter
    s1 = pattern.matcher(s1).replaceAll("");
    s2 = pattern.matcher(s2).replaceAll("");

    // ignore different white space outside of strings
    s1 = multipleWhiteSpace.matcher(s1).replaceAll(" ");
    s2 = multipleWhiteSpace.matcher(s2).replaceAll(" ");

    // apply nl + spaces + nl -> nl nl
    s1 = emptyLinePattern.matcher(s1).replaceAll("");
    s2 = emptyLinePattern.matcher(s2).replaceAll("");
    
    // apply nl nl -> nl
    
    s1 = multipleNlPattern.matcher(s1).replaceAll("\n");
    s2 = multipleNlPattern.matcher(s2).replaceAll("\n");
    
    // apply get rid of trailing nl
    
    s1 = removeTrailingNl(s1);
    s2 = removeTrailingNl(s2);

    return compareStringsWithMsg(s1, s2);
  }
  
  private static String removeTrailingNl(String s) {
    int i = s.length() - 1;
    if (i >= 0 && s.charAt(i) == '\n') {
      return s.substring(0, i);
    }
    return s;
  }
  
  /**
   * Compare two strings, give message indicating where they miscompare, including
   *   approx 10 chars before and after the first miscompare, for context
   * @param s1  first string to compare
   * @param s2  second string to compare
   * @return  true if strings have the same charactersS
   */
  public static boolean compareStringsWithMsg(String s1, String s2) {

    final int maxI = Math.min(s1.length(), s2.length());
    for (int i = 0; i < maxI; i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
        System.out.println("Error: strings differ starting at char: " + i);
        System.out.println("Error:   string 1 = " + s1.substring(Math.max(0, i-100), Math.min(s1.length(), i+100)));
        System.out.println("Error:   string 2 = " + s2.substring(Math.max(0, i-100), Math.min(s2.length(), i+100)));
        return false;
      }
    }
    
    
    
    
    if (s1.length() != s2.length()) {
      System.out.println("Error: strings are different length");
      System.out.println("  s1 length = " + s1.length() + "; s2 length = " + s2.length());
      return false;
    } 
    return true;
  }
  
  /**
   * Helper method that replaces empty XML tags in long notation with the corresponding short form.
   * 
   * @param xml
   *          The XML file where the empty tags are to be replaced as string.
   * @param filename -
   * @return The XML file with short empty tags as string.
   */
  private static String shortenEmptyTags(String xml, String filename) {
    Matcher matcher = emptyTagPattern.matcher(xml);

    StringBuffer result = new StringBuffer();
    StringBuffer sb = null;
    String replacement = null;
    boolean replaced = false;

    // find and replace
    while (matcher.find()) {
      sb = new StringBuffer();
      sb.append("<").append(matcher.group(2)).append("/>");
      replacement = sb.toString();
      matcher.appendReplacement(result, replacement);
      replaced = true;
    }
    matcher.appendTail(result);

    // notify that files have been changed in memory
    if (replaced) {
      System.out.println("In file \"" + filename
              + "\" empty tags have been transformed from long to short notation in memory!");
    }

    return result.toString();
  }

  /**
   * Read the contents of a file into a string, using the default platform encoding.
   * 
   * @param file
   *          The file to be read in.
   * @return String The contents of the file.
   * @throws IOException
   *           Various I/O errors. '
   * 
   * TODO: This is duplicated from org.apache.uima.internal.util.FileUtils in the uimaj-core
   * package. We can't have a compile dependency on uimaj-core since that introduces a cycle. Not
   * sure what the best way of handling this is.
   */
  public static String file2String(File file) throws IOException {
    return reader2String(
            new FileReader(file),
            (int) file.length());   
  }
  
  /**
   * Read a bufferedReader into a string, using the default platform encoding.
   * 
   * @param reader to be read in
   * @param bufSize - size of stream, in bytes.  Size in chars is &le; size in bytes, because
   * chars take 1 or more bytes to encode.
   * @return String The contents of the stream.
   * @throws IOException
   *           Various I/O errors.
   *           
   * TODO: This is duplicated from org.apache.uima.internal.util.FileUtils in the uimaj-core
   * package. We can't have a compile dependency on uimaj-core since that introduces a cycle. Not
   * sure what the best way of handling this is.
   */
  public static String reader2String(Reader reader, int bufSize) throws IOException {
    char[] buf = new char[bufSize];
    int read_so_far = 0;
    try {
      while (read_so_far < bufSize) {
        int count = reader.read(buf, read_so_far, bufSize - read_so_far);
        if (0 > count) {
          break;
        }
        read_so_far += count;
      }
    } finally {
      reader.close();
    }
    return new String(buf, 0, read_so_far);
  }

}
