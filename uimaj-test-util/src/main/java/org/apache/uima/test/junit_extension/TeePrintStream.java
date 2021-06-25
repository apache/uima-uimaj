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
  @Override
  public boolean checkError() {
    if (this.original.checkError() && this.teeStream.checkError())
      return true;
    else
      return false;
  }

  /**
   * @see java.io.OutputStream#close()
   */
  @Override
  public void close() {
    this.original.close();
    this.teeStream.close();
  }

  /**
   * @see java.io.OutputStream#flush()
   */
  @Override
  public void flush() {
    this.original.flush();
    this.teeStream.flush();
  }

  /**
   * @see java.io.PrintStream#print(boolean)
   */
  @Override
  public void print(boolean b) {
    this.original.print(b);
    this.teeStream.print(b);
  }

  /**
   * @see java.io.PrintStream#print(char)
   */
  @Override
  public void print(char c) {
    this.original.print(c);
    this.teeStream.print(c);
  }

  /**
   * @see java.io.PrintStream#print(char[])
   */
  @Override
  public void print(char[] s) {
    this.original.print(s);
    this.teeStream.print(s);
  }

  /**
   * @see java.io.PrintStream#print(double)
   */
  @Override
  public void print(double d) {
    this.original.print(d);
    this.teeStream.print(d);
  }

  /**
   * @see java.io.PrintStream#print(float)
   */
  @Override
  public void print(float f) {
    this.original.print(f);
    this.teeStream.print(f);
  }

  /**
   * @see java.io.PrintStream#print(int)
   */
  @Override
  public void print(int i) {
    this.original.print(i);
    this.teeStream.print(i);
  }

  /**
   * @see java.io.PrintStream#print(long)
   */
  @Override
  public void print(long l) {
    this.original.print(l);
    this.teeStream.print(l);
  }

  /**
   * @see java.io.PrintStream#print(java.lang.Object)
   */
  @Override
  public void print(Object obj) {
    this.original.print(obj);
    this.teeStream.print(obj);
  }

  /**
   * @see java.io.PrintStream#print(java.lang.String)
   */
  @Override
  public void print(String s) {
    this.original.print(s);
    this.teeStream.print(s);
  }

  /**
   * @see java.io.PrintStream#println()
   */
  @Override
  public void println() {
    this.original.println();
    this.teeStream.println();
  }

  /**
   * @see java.io.PrintStream#println(boolean)
   */
  @Override
  public void println(boolean x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(char)
   */
  @Override
  public void println(char x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(char[])
   */
  @Override
  public void println(char[] x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(double)
   */
  @Override
  public void println(double x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(float)
   */
  @Override
  public void println(float x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(int)
   */
  @Override
  public void println(int x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(long)
   */
  @Override
  public void println(long x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(java.lang.Object)
   */
  @Override
  public void println(Object x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.PrintStream#println(java.lang.String)
   */
  @Override
  public void println(String x) {
    this.original.println(x);
    this.teeStream.println(x);
  }

  /**
   * @see java.io.OutputStream#write(byte[], int, int)
   */
  @Override
  public void write(byte[] buf, int off, int len) {
    this.original.write(buf, off, len);
    this.teeStream.write(buf, off, len);
  }

  /**
   * @see java.io.OutputStream#write(int)
   */
  @Override
  public void write(int b) {
    this.original.write(b);
    this.teeStream.write(b);
  }
}
