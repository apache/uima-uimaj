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
import java.text.NumberFormat;
import java.util.Enumeration;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;
import junit.textui.ResultPrinter;

/**
 * UIMAResultPrinter is a ResultPrinter extension for the JUnit framework.
 */
public class UIMAResultPrinter extends ResultPrinter implements TestListener {
  // print stream for the output
  private PrintStream fWriter;

  // current column in line
  private int fColumn = 0;

  // test case counter
  private int testCounter;

  // name of the current test class
  private String currentTestClass;

  // success status of the current test method
  private boolean currentTestSuccess;

  // abort execution if an error occurs
  private boolean abortOnFail;

  // dublicated output stream, change output layout
  private boolean teeOutputStream;

  public UIMAResultPrinter(PrintStream writer, boolean abortOnFail, boolean teeOutputStream) {
    // init class members
    super(writer);
    this.fWriter = writer;
    this.testCounter = 0;
    this.currentTestClass = null;
    this.currentTestSuccess = true;
    this.abortOnFail = abortOnFail;
    this.teeOutputStream = teeOutputStream;
  }

  /**
   * @see junit.textui.ResultPrinter#printHeader(long)
   */
  protected void printHeader(long runTime) {
    getWriter().println();
    getWriter().println();
    getWriter().println("Time: " + elapsedTimeAsString(runTime));
  }

  /**
   * @see junit.textui.ResultPrinter#printErrors(junit.framework.TestResult)
   */
  protected void printErrors(TestResult result) {
    printDefects(result.errors(), result.errorCount(), "error");
  }

  /**
   * @see junit.textui.ResultPrinter#printFailures(junit.framework.TestResult)
   */
  protected void printFailures(TestResult result) {
    printDefects(result.failures(), result.failureCount(), "failure");
  }

  /**
   * @see junit.textui.ResultPrinter#printDefects(java.util.Enumeration, int, java.lang.String)
   */
  protected void printDefects(Enumeration booBoos, int count, String type) {
    if (count == 0)
      return;
    if (count == 1)
      getWriter().println("There was " + count + " " + type + ":");
    else
      getWriter().println("There were " + count + " " + type + "s:");
    for (int i = 1; booBoos.hasMoreElements(); i++) {
      printDefect((TestFailure) booBoos.nextElement(), i);
    }
  }

  /**
   * @see junit.textui.ResultPrinter#printDefect(junit.framework.TestFailure, int)
   */
  public void printDefect(TestFailure booBoo, int count) { // only public for testing purposes
    printDefectHeader(booBoo, count);
    printDefectTrace(booBoo);
  }

  /**
   * @see junit.textui.ResultPrinter#printDefectHeader(junit.framework.TestFailure, int)
   */
  protected void printDefectHeader(TestFailure booBoo, int count) {
    // I feel like making this a println, then adding a line giving the throwable a chance to print
    // something
    // before we get to the stack trace.
    getWriter().print(count + ") " + booBoo.failedTest());
  }

  /**
   * @see junit.textui.ResultPrinter#printDefectTrace(junit.framework.TestFailure)
   */
  protected void printDefectTrace(TestFailure booBoo) {
    getWriter().print(BaseTestRunner.getFilteredTrace(booBoo.trace()));
  }

  /**
   * @see junit.textui.ResultPrinter#printFooter(junit.framework.TestResult)
   */
  protected void printFooter(TestResult result) {
    if (result.wasSuccessful()) {
      getWriter().println();
      getWriter().print("OK");
      getWriter().println(
              " (" + result.runCount() + " test" + (result.runCount() == 1 ? "" : "s") + ")");

    } else {
      getWriter().println();
      getWriter().println("FAILURES!!!");
      getWriter().println(
              "Tests run: " + result.runCount() + ",  Failures: " + result.failureCount()
                      + ",  Errors: " + result.errorCount());
    }
    getWriter().println();
  }

  /**
   * Returns the formatted string of the elapsed time. Duplicated from BaseTestRunner. Fix it.
   */
  protected String elapsedTimeAsString(long runTime) {
    return NumberFormat.getInstance().format((double) runTime / 1000);
  }

  /**
   * @see junit.textui.ResultPrinter#getWriter()
   */
  public PrintStream getWriter() {
    return this.fWriter;
  }

  /**
   * @see junit.framework.TestListener#addError(Test, Throwable)
   */
  public void addError(Test test, Throwable t) {
    getWriter().print("error");
    this.currentTestSuccess = false;

    if (this.abortOnFail) {
      getWriter().println();
      getWriter().println();
      getWriter().println("Stop executing testcases...");
      getWriter().println("Print Stacktrace: ");
      getWriter().println();
      StackTraceElement[] stackTrace = t.getStackTrace();
      for (int i = 0; i < stackTrace.length; i++) {
        getWriter().println(stackTrace[i].toString());
      }

      throw new RuntimeException("Abort on error");
    }
  }

  /**
   * @see junit.framework.TestListener#addFailure(Test, AssertionFailedError)
   */
  public void addFailure(Test test, AssertionFailedError t) {
    getWriter().print("failure");
    this.currentTestSuccess = false;
    if (this.abortOnFail) {
      getWriter().println();
      getWriter().println();
      getWriter().println("Stop executing testcases...");
      getWriter().println("Print Stacktrace: ");
      getWriter().println();
      StackTraceElement[] stackTrace = t.getStackTrace();
      for (int i = 0; i < stackTrace.length; i++) {
        getWriter().println(stackTrace[i].toString());
      }

      throw new RuntimeException("Abort on failure");
    }
  }

  /**
   * @see junit.framework.TestListener#endTest(Test)
   */
  public void endTest(Test test) {
    if (this.currentTestSuccess == false)
      this.currentTestSuccess = true;
    else
      getWriter().print("ok");
  }

  /**
   * @see junit.framework.TestListener#startTest(Test)
   */
  public void startTest(Test test) {
    this.testCounter++;
    String name = test.toString();
    String tempCurrentTestClass = name.substring(name.indexOf('(') + 1, name.lastIndexOf(')'));
    String testName = name.substring(0, name.indexOf('('));

    if (!tempCurrentTestClass.equals(this.currentTestClass)) {
      this.currentTestClass = tempCurrentTestClass;
      getWriter().println();
      getWriter().println();
      getWriter().print(this.currentTestClass);
      getWriter().println();
      for (int i = 0; i < this.currentTestClass.length(); i++)
        getWriter().print("=");
    }

    getWriter().println();
    getWriter().print(this.testCounter + ":  " + testName + ": ");
    if (this.fColumn++ >= 40 || this.teeOutputStream) {
      getWriter().println();
      this.fColumn = 0;
    }
  }
}
