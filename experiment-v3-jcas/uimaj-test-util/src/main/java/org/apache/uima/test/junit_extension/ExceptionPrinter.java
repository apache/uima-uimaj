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

/**
 * ExceptionPrinter print out an exception with the place where the exception occurs
 * 
 */
public class ExceptionPrinter {
  /**
   * Print out exception stack trace and the place where the exception was thrown.
   * 
   * @param stackTrace
   *          a exception stack trace
   * @param message
   *          exception message
   */
  public static void printException(StackTraceElement[] stackTrace, String message) {
    Throwable ex = new Throwable();
    StackTraceElement[] stackTraceElement = ex.getStackTrace();

    System.out.println();
    System.out.println("Exception occurred:");
    /*
     * System.out.println(stackTraceElement[1].getClassName()+ "." +
     * stackTraceElement[1].getMethodName() + "(" + stackTraceElement[1].getFileName() + ")");
     */
    System.out.println("Test file: " + stackTraceElement[1].getFileName());
    System.out.println("Test class: " + stackTraceElement[1].getClassName());
    System.out.println("Test method: " + stackTraceElement[1].getMethodName() + "()");

    System.out.println();
    System.out.println("Exception output:");
    System.out.println(message);
    // print stack trace
    for (int i = 0; i < stackTrace.length; i++) {
      System.out.println(stackTrace[i].toString());
    }
  }

  /**
   * Print out the exception message and where the exception was thrown.
   * 
   * @param message
   *          exception message
   */
  public static void printException(String message) {
    Throwable ex = new Throwable();
    StackTraceElement[] stackTraceElement = ex.getStackTrace();

    System.out.println();
    System.out.println("Exception occurred:");
    System.out.println("Test file: " + stackTraceElement[1].getFileName());
    System.out.println("Test class: " + stackTraceElement[1].getClassName());
    System.out.println("Test method: " + stackTraceElement[1].getMethodName() + "()");
    System.out.println();
    System.out.println("Exception output:");
    // print exception message
    System.out.println(message);
  }

  /**
   * Print out exception stack trace and the place where the exception was thrown.
   * 
   * @param ex
   *          exception
   */
  public static void printException(Exception ex) {
    printException(ex.getStackTrace(), ex.getMessage());
  }

}
