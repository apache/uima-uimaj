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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileCompare class provides a method, which compares two files.
 * 
 */
public class FileCompare {

  /**
   * TODO Currently only tags containing word characters [a-zA-Z_0-9] are recognised.
   */
  private static final String EMPTY_TAG_REGEX = "(<([\\w]+)>[\\s]*</[\\w]+>)";

  private static Pattern emptyTagPattern = Pattern.compile(EMPTY_TAG_REGEX);

  /**
   * compares two files and return true if the files have the same content.
   * 
   * @param filename1
   *          filename of the first file
   * @param filename2
   *          filename of the second file
   * @return - true if the files have the same content
   * 
   * @throws IOException
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
   * @throws IOException
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
   * @throws IOException
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
   * @return
   * @throws IOException
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
   * Helper method that replaces empty XML tags in long notation with the corresponding short form.
   * 
   * @param xml
   *          The XML file where the empty tags are to be replaced as string.
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
    // Read the file into a string using a char buffer.
    char[] buf = new char[10000];
    int charsRead;
    BufferedReader reader = new BufferedReader(new FileReader(file));
    StringBuffer strbuf = new StringBuffer();
    while ((charsRead = reader.read(buf)) >= 0) {
      strbuf.append(buf, 0, charsRead);
    }
    reader.close();
    final String text = strbuf.toString();
    return text;
  }
}
