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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.GregorianCalendar;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * JUnitTestRunner is the start class for the comand line test with JUnit.
 */
public class JUnitTestRunner {
  // this mehtod name must be avaialbe at every TestSuite class
  private static final String SUITE_METHODNAME = "suite";

  // command line parameters
  private static final String ABORT_ON_FAIL = "-abortonfail";

  private static final String TEST_SUITE = "-suite";

  private static final String TEST_CASE = "-testcase";

  private static final String RESULT_OUTPUT = "-resultfile";

  private static final String CONSOLE_OUTPUT = "-consolefile";

  private static final String RESULT_OUTPUT_SHORT = "-rf";

  private static final String CONSOLE_OUTPUT_SHORT = "-cf";

  private static final String TEE_CONSOLE_OUTPUT = "-cpconsole";

  private static final String TEE_RESULT_OUTPUT = "-cpresult";

  // command line argument flags
  private static boolean abortOnFail = false;

  private static boolean testCase = false;

  private static boolean testSuite = false;

  private static boolean teeResult = false;

  private static boolean teeConsole = false;

  private static boolean error = false;

  // command line arguments
  private static String testName = null;

  private static String consoleFileName = null;

  private static String resultFileName = null;

  public static void main(String args[]) {
    JUnitTestRunner jRun = new JUnitTestRunner();
    if (jRun.runTest(args)) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  public boolean runTest(String[] args) {
    // process command line arguments, loop over all arguments
    for (int i = 0; i < args.length; i++) {
      // get current argument
      String currentArg = args[i];

      // if current argument starts with '-' transform to lower case
      if (currentArg.startsWith("-")) {
        currentArg = currentArg.toLowerCase();
      }

      // check abort on failure
      if (currentArg.equals(ABORT_ON_FAIL)) {
        abortOnFail = true;
      }
      // check if testname is a TestSuite
      else if (currentArg.equals(TEST_SUITE)) {
        if ((i + 1) < args.length) {
          testSuite = true;
          testName = args[i + 1];
          i++;
        } else {
          error = true;
        }
      }
      // check if testname is a TestCase
      else if (currentArg.equals(TEST_CASE)) {
        if ((i + 1) < args.length) {
          testCase = true;
          testName = args[i + 1];
          i++;
        } else {
          error = true;
        }
      }
      // check if a result output filename is specified
      else if (currentArg.equals(RESULT_OUTPUT) || currentArg.equals(RESULT_OUTPUT_SHORT)) {
        if ((i + 1) < args.length) {
          resultFileName = args[i + 1];
          i++;
        } else {
          error = true;
        }
      }
      // check if a console output filename is specified
      else if (currentArg.equals(CONSOLE_OUTPUT) || currentArg.equals(CONSOLE_OUTPUT_SHORT)) {
        if ((i + 1) < args.length) {
          consoleFileName = args[i + 1];
          i++;
        } else {
          error = true;
        }
      }
      // check if console output should be dublicated
      else if (currentArg.equals(TEE_CONSOLE_OUTPUT)) {
        teeConsole = true;
      }
      // check if result output should be dublicated
      else if (currentArg.equals(TEE_RESULT_OUTPUT)) {
        teeResult = true;
      }
    }

    // check if commandline was ok
    if (!(testCase || testSuite) || error) {
      System.out
              .println("Usage: JUnitTestRunner [-abortOnFail] [-testcase <FullTestCaseClassname>] "
                      + " [-suite <FullTestSuiteClassname] [-consoleFile <consoleOutputFileName>]"
                      + " [-resultFile <resultOutputFilename>] [-cpConsole] [-cpResult]");
    }

    // create new JUnitTestRunner
    TestRunner aTestRunner = new TestRunner();

    try {
      // create ouput files references
      File resultFile = null;
      File consoleFile = null;

      // initialize standard output stream, use console as standard output
      PrintStream resultWriter = System.out;
      PrintStream consoleWriter = System.out;

      // check if result output filename was specifed
      if (resultFileName != null) {
        // create new result output file
        resultFile = new File(resultFileName);
        resultFile.createNewFile();
        // dublicate PrintStream if neccessary
        if (teeResult == true) {
          PrintStream tempResultWriter = new PrintStream(new FileOutputStream(resultFile, false),
                  true, "UTF-8");
          resultWriter = new TeePrintStream(tempResultWriter, System.out);
        } else {
          resultWriter = new PrintStream(new FileOutputStream(resultFile, false), true, "UTF-8");
        }
      }
      // check if console output filename was specified
      if (consoleFileName != null) {
        // create new console output file
        consoleFile = new File(consoleFileName);
        consoleFile.createNewFile();
        // dublicate PrintStream if neccessary
        if (teeConsole == true) {
          PrintStream tempConsoleWriter = new PrintStream(new FileOutputStream(consoleFile, false),
                  true, "UTF-8");
          consoleWriter = new TeePrintStream(tempConsoleWriter, System.out);
        } else {
          consoleWriter = new PrintStream(new FileOutputStream(consoleFile, false), true, "UTF-8");
        }
        // set new System.out PrintStream
        System.setOut(consoleWriter);
      }

      // set UIMAResult printer mode, if teeResult is true use another output layout
      UIMAResultPrinter printer = new UIMAResultPrinter(resultWriter, abortOnFail, teeResult);
      aTestRunner.setPrinter(printer);

      // get testClass for the given testname
      Class testClass = Class.forName(testName);

      // create new JUnit TestSuite
      TestSuite suite = new TestSuite();
      suite.setName(testName.substring((testName.lastIndexOf(".") + 1), testName.length()));

      // add suite to current suite
      if (testSuite) {
        Method suiteMethod = testClass.getMethod(SUITE_METHODNAME, new Class[0]);
        Test test = (Test) suiteMethod.invoke(null, new Class[0]); // static method
        suite.addTest(test);
      }
      // add testcase to current suite
      if (testCase) {
        suite.addTestSuite(testClass);

      }

      // print header
      resultWriter
              .println("################################################################################################");
      resultWriter.println("# TestSuite: " + suite.getName());
      resultWriter.println("# Testcases: " + suite.countTestCases());
      resultWriter.println("# Test candidate: " + testName);
      resultWriter.println("# Abort on error: " + abortOnFail);
      resultWriter.println("# Result output filename: " + resultFileName);
      resultWriter.println("# Console output filename: " + consoleFileName);
      resultWriter.println("# Test start directory: " + System.getProperty("user.dir"));
      resultWriter.println("# Test starter: " + System.getProperty("user.name"));
      resultWriter.println("# OS: " + System.getProperty("os.name") + " "
              + System.getProperty("os.version"));
      resultWriter.println("# Command line run: " + System.getProperty("isCommandLine", "false"));

      // get current date and time
      DateFormat dateTime;
      GregorianCalendar cal = new GregorianCalendar();
      StringBuffer time = new StringBuffer(42);
      time.append("# Test started at: ");
      dateTime = DateFormat.getDateInstance();
      time.append(dateTime.format(cal.getTime()));
      dateTime = DateFormat.getTimeInstance();
      time.append("  ");
      time.append(dateTime.format(cal.getTime()));
      resultWriter.println(time.toString());

      resultWriter.println("# Test runs with java version: " + System.getProperty("java.version"));
      resultWriter.println("# JUnitTestRunner version: 1.2");
      resultWriter
              .println("################################################################################################");

      // run current test
      TestResult results = aTestRunner.doRun(suite);

      // check test result
      if (results.wasSuccessful() == false) {
        return false;
      }
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return false;
    }
  }
}
