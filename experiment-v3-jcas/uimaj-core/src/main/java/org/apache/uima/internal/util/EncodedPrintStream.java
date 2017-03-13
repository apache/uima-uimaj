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

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Allow a <code>PrintStream</code> to specify an encoding.
 * 
 * <p>
 * This functionality is provided out of the box by the Java 1.4 version of <code>PrintStream</code>.
 * This class is strictly for backwards compatibility.
 * 
 */
public class EncodedPrintStream extends PrintStream {

  private final String encoding;

  /**
   * Constructor allows specifying a character encoding.
   * 
   * <p>
   * Does not automatically flush the underlying output stream.
   * 
   * @param out
   *          The underlying output stream.
   * @param encoding
   *          String representation of a character encoding.
   * @throws java.io.UnsupportedEncodingException
   *           If the character encoding is not supported by the JVM.
   */
  public EncodedPrintStream(OutputStream out, String encoding) throws UnsupportedEncodingException {
    this(out, false, encoding);
  }

  /**
   * Constructor allows specifying a character encoding.
   * 
   * @param out
   *          The underlying output stream.
   * @param autoFlush
   *          See {@link PrintStream#PrintStream(java.io.OutputStream, boolean) PrintStream()}
   * @param encoding
   *          String representation of a character encoding.
   * @throws java.io.UnsupportedEncodingException
   *           If the character encoding is not supported by the JVM.
   */
  public EncodedPrintStream(OutputStream out, boolean autoFlush, String encoding)
          throws UnsupportedEncodingException {
    super(out, autoFlush);
    this.encoding = encoding;
    // Possibly trigger Unsupported encoding exception.
    "test".getBytes(encoding);
  }

  private final void writeBytes(byte[] bytes) {
    super.write(bytes, 0, bytes.length);
  }

  public void print(char c) {
    try {
      writeBytes((new String(new char[] { c })).getBytes(encoding));
    } catch (UnsupportedEncodingException e) {
    }
  }

  public void print(char[] s) {
    try {
      writeBytes((new String(s)).getBytes(encoding));
    } catch (UnsupportedEncodingException e) {
    }
  }

  public void print(String s) {
    try {
      writeBytes(s.getBytes(encoding));
    } catch (UnsupportedEncodingException e) {
    }
  }

  public void println(char x) {
    print(x);
    println();
  }

  public void println(char[] x) {
    print(x);
    println();
  }

  public void println(String x) {
    print(x);
    println();
  }

}
