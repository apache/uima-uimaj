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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * This class manupulates the output of the system. Methode output like System.out.println("blah")
 * may be send to something else than the console.
 */
public class ManageOutputDevice {

  private static PrintStream sysOutPS;

  private static PrintStream sysErrPS;
  static {
    sysOutPS = System.out;
    sysErrPS = System.err;
  }

  /**
   * sets the <code>System.out</code> to a file based <code>java.io.PrintStream</code>
   * 
   * @param descriptor
   *          a full qualified filename, see {@link java.io.File#File(String pathname)}
   * @throws FileNotFoundException -
   */
  public static void setSysOutToFile(String descriptor) throws FileNotFoundException {
    File f = new File(descriptor);
    PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)),
            true);
    System.setOut(printStream);
  }

  /**
   * sets the <code>System.out</code> to the value, which was set, when the class was loaded by
   * the VM.
   */
  public static void setSysOutToDefault() {
    System.setOut(sysOutPS);
  }

  /**
   * sets the <code>System.out</code> to a virtual <code>java.io.ByteArrayOutputStream</code>
   */
  public static void setSysOutToNirvana() {
    System.setOut(new PrintStream(new ByteArrayOutputStream()));
  }

  /**
   * sets the <code>System.err</code> to a file based <code>java.io.PrintStream</code>
   * 
   * @param descriptor -
   * @throws FileNotFoundException -
   */
  public static void setSysErrToFile(String descriptor) throws FileNotFoundException {
    File f = new File(descriptor);
    PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)),
            true);
    System.setErr(printStream);
  }

  /**
   * sets the <code>System.err</code> to the value, which was set, when this class was loaded by
   * the VM.
   */
  public static void setSysErrToDefault() {
    System.setErr(sysErrPS);
  }

  /**
   * sets the <code>System.err</code> to a virtual <code>java.io.ByteArrayOutputStream</code>
   */
  public static void setSysErrToNirvana() {
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
  }

  /**
   * sets the <code>System.err</code> and <code>System.out</code> to a virtual
   * <code>java.io.ByteArrayOutputStream</code>
   */
  public static void setAllSystemOutputToNirvana() {
    setSysErrToNirvana();
    setSysOutToNirvana();
  }

  /**
   * sets the <code>System.err</code> and <code>System.out</code> to their values, which were
   * set, when this class was loaded.
   */
  public static void setAllSystemOutputToDefault() {
    setSysErrToDefault();
    setSysOutToDefault();
  }
}
