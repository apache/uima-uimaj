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

package org.apache.uima.util;

import java.io.PrintStream;

/**
 * A PrintStream implementation that writes to a UIMA logger. Useful if you have a UIMA component
 * that uses a 3rd-party package that logs to a PrintStream, and you want to redirect the output to
 * the UIMA log.
 * <p>
 * Note that only the output of <code>print</code> and <code>println</code> calls goes to the
 * logger. Calls to the <code>write</code> methods are ignored since they take binary data which
 * cannot be easily redirected to the logger.
 */
public class LoggerPrintStream extends PrintStream {
  Logger logger;

  Level level;

  StringBuffer buf = new StringBuffer();

  public LoggerPrintStream(Logger logger, Level level) {
    super(System.out); // hopefully nothing will actually reach stdout
    this.logger = logger;
    this.level = level;
  }

  @Override
  public void close() {
    flush();
  }

  @Override
  public void flush() {
    if (buf.length() > 0) {
      logger.log(level, buf.toString());
      buf.setLength(0);
    }
  }

  @Override
  public void print(boolean b) {
    print("" + b);
  }

  @Override
  public void print(char c) {
    print("" + c);
  }

  @Override
  public void print(char[] s) {
    print(new String(s));
  }

  @Override
  public void print(double d) {
    print("" + d);
  }

  @Override
  public void print(float f) {
    print("" + f);
  }

  @Override
  public void print(int i) {
    print("" + i);
  }

  @Override
  public void print(long l) {
    print("" + l);
  }

  @Override
  public void print(Object obj) {
    print("" + obj);

  }

  @Override
  public void print(String s) {
    buf.append(s);
  }

  @Override
  public void println() {
    flush();
  }

  @Override
  public void println(boolean x) {
    println("" + x);
  }

  @Override
  public void println(char x) {
    println("" + x);
  }

  @Override
  public void println(char[] x) {
    println(new String(x));
  }

  @Override
  public void println(double x) {
    println("" + x);
  }

  @Override
  public void println(float x) {
    println("" + x);
  }

  @Override
  public void println(int x) {
    println("" + x);
  }

  @Override
  public void println(long x) {
    println("" + x);
  }

  @Override
  public void println(Object x) {
    println("" + x);
  }

  @Override
  public void println(String x) {
    buf.append(x);
    flush();
  }

  @Override
  public void write(byte[] aBuf, int off, int len) {
    // raw bytes not supported
  }

  @Override
  public void write(int b) {
    // raw bytes not supported
  }

}
