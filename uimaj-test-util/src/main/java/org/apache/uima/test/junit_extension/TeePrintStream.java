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

import java.io.PrintStream;

/**
 * TeePrintStream duplicates a PrintStream.
 * 
 */
public class TeePrintStream extends PrintStream {
  PrintStream original;

  PrintStream teeStream;

  /**
   * Initialize the two print streams.
   * 
   * @param original
   *          original stream
   * @param teeStream
   *          dublicated stream
   */
  public TeePrintStream(PrintStream original, PrintStream teeStream) {
    super(original);
    this.original = original;
    this.teeStream = teeStream;
  }

  /**
   * @see java.io.PrintStream#checkError()
   */
  public boolean checkError() {
    if (this.original.checkError() && this.teeStream.checkError())
      return true;
    else
      return false;
  }

  /**
   * @see java.io.OutputStream#close()
   */
  public void close() {
    this.original.close();
    this.teeStream.close();
  }

  /**
   * @see java.io.OutputStream#flush()
   */
  public void flush() {
    this.original.flush();
    this.teeStream.flush();
  }

  /**
   * @see java.io.PrintStream#print(boolean)
   */
  public void print(boolean b) {
    this.original.print(b);
    this.teeStream.print(b);
  }

  /**
   * @see java.io.PrintStream#print(char)
   */
  public void print(char c) {
    this.original.print(c);
    this.teeStream.print(c);
  }

  /**
   * @see java.io.PrintStream#print(char[])
   */
  public void print(char[] s) {
    this.original.print(s);
    this.teeStream.print(s);
  }

  /**
   * @see java.io.PrintStream#print(double)
   */
  public void print(double d) {
    this.original.print(d);
    this.teeStream.print(d);
  }

  /**
   * @see java.io.PrintStream#print(float)
   */
  public void print(float f) {
    this.original.print(f);
    this.teeStream.print(f);
  }

  /**
   * @see java.io.PrintStream#print(int)
   */
  public void print(int i) {
    this.original.print(i);
    this.teeStream.print(i);
  }

  /**
   * @see java.io.PrintStream#print(long)
   */
  public void print(long l) {
    this.original.print(l);
    this.teeStream.print(l);
  }

  /**
   * @see java.io.PrintStream#print(java.lang.Object)
   */
  public void print(Object obj) {
    this.original.print(obj);
    this.teeStream.print(obj);
  }

  /**
   * @see java.io.PrintStream#print(java.lang.String)
   */
  public void print(String s) {
    this.original.print(s);
    this.teeStream.print(s);
  }

  /**
   * @see java.io.PrintStream#println()
   */
  public void println() {
    this.original.println();
    this.teeStream.println();
  }

  /**
   * @see java.io.PrintStream#println(boolean)
   */
  public void println(boolean x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(char)
   */
  public void println(char x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(char[])
   */
  public void println(char[] x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(double)
   */
  public void println(double x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(float)
   */
  public void println(float x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(int)
   */
  public void println(int x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(long)
   */
  public void println(long x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(java.lang.Object)
   */
  public void println(Object x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(java.lang.String)
   */
  public void println(String x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.OutputStream#write(byte[], int, int)
   */
  public void write(byte[] buf, int off, int len) {
    this.original.write(buf, off, len);
    this.teeStream.write(buf, off, len);
  }

  /**
   * @see java.io.OutputStream#write(int)
   */
  public void write(int b) {
    this.original.write(b);
    this.teeStream.write(b);
  }
}
