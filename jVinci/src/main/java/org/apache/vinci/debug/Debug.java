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

package org.apache.vinci.debug;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Miscellaneous debugging functionality such as error logging, assertion checking and exception
 * reporting. All output produced by this class goes to debugStream which is configurable. This
 * class is thread safe.
 * 
 * This is JDK1.3 legacy as this functionality is now provided natively by Java 1.4.
 */
public class Debug {
  static private volatile boolean log_messages = true;

  static private volatile boolean log_exceptions = true;

  static private boolean output_thread_name = false;

  static private PrintStream debugStream = System.err;

  static private DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
          DateFormat.SHORT, Locale.ENGLISH);

  static private Date now = new Date();

  /**
   * This utility class not meant to be instantiated.
   */
  private Debug() {
  }

  /**
   * Set the debugging stream. If no debug stream is explicitly set, then System.err is used.
   * 
   * @param w
   *          The stream where debug output will be directed.
   */
  synchronized static public void setDebuggingStream(PrintStream w) {
    debugStream = w;
  }

  /**
   * Get the debugging stream.
   * 
   * @return The stream currently used for debug output.
   */
  synchronized static public PrintStream getDebuggingStream() {
    return debugStream;
  }

  /**
   * Set whether debugging messages will be logged to the debug stream. Default is true.
   * 
   * @param on
   *          Whether or not printed debug messages will go to the log.
   */
  synchronized static public void setLogMessages(boolean on) {
    log_messages = on;
    if (on) {
      Debug.p("vinci.debug.Debug", "Message logging turned ON.");
    }
  }

  /**
   * Determine if message logging is enabled.
   * 
   * @return true if message logging is enabled
   */
  synchronized static public boolean getLogMessages() {
    return log_messages;
  }

  /**
   * Turn on/off the logging of reported exceptions. Default is on.
   * 
   * @param on
   *          Whtehr or not reported exceptions will be directed to the log.
   */
  synchronized static public void setLogExceptions(boolean on) {
    log_exceptions = on;
    Debug.p("vinci.debug.Debug", "Exception logging turned " + ((on) ? "ON" : "OFF"));
  }

  /**
   * Determine if exception logging is enabled.
   * 
   * @return true if message logging is enabled
   */
  synchronized static public boolean getLogExceptions() {
    return log_messages;
  }

  /**
   * Turn on/off the reporting of thread name in the debug log. Default is off.
   * 
   * @param on
   *          Whether or not the thread name will appear in the output.
   */
  synchronized static public void setThreadNameOutput(boolean on) {
    output_thread_name = on;
  }

  /**
   * Report the exception to the debug log. Usually used to record exceptions that are unexpected,
   * yet do not indicate fatal conditions (e.g. they are recoverable).
   * 
   * @param e
   *          The exception to report.
   * @param message
   *          Additional information to report along with the exception.
   * 
   * @pre e != null
   */
  synchronized static public void reportException(Throwable e, String message) {
    if (log_exceptions) {
      printDebuggingMessage("====================================");
      debugStream.println("(WARNING) Unexpected exception: " + message);
      e.printStackTrace(debugStream);
      debugStream.flush();
    }
  }

  /**
   * Report the Exception (or Throwable) to the debug log. Usually used to record exceptions that
   * are unexpected, yet do not indicate fatal conditions (e.g. they are recoverable).
   * 
   * @param e
   *          The exception to report.
   * 
   * @pre e != null
   */
  synchronized static public void reportException(Throwable e) {
    if (log_exceptions) {
      printDebuggingMessage("====================================");
      debugStream.println("(WARNING) Unexpected exception: ");
      e.printStackTrace(debugStream);
      debugStream.flush();
    }
  }

  /**
   * Print the provided message to the debug log (if message logging is on). Does not automatically
   * flush the message to the log. Call flush() if you need to ensure the message is immediately
   * flushed.
   * 
   * @param message
   *          The message to report.
   * @return The string provided as an argument (to support log chaining).
   */
  synchronized static public String printDebuggingMessage(String message) {
    if (log_messages) {
      now.setTime(System.currentTimeMillis());
      if (output_thread_name) {
        debugStream.println("[" + formatter.format(now) + " | " + Thread.currentThread().getName()
                + "] " + message);
      } else {
        debugStream.println("[" + formatter.format(now) + "] " + message);
      }
    }
    return message;
  }

  /**
   * Print the provided message to the debug log (if message logging is on). Does not automatically
   * flush the message to the log. Call flush() if you need to ensure the message is immediately
   * flushed.
   * 
   * @param location
   *          A string indicating which part of code is generating the message.
   * @param message
   *          The message to log.
   */
  synchronized static public void printDebuggingMessage(String location, String message) {
    if (log_messages) {
      now.setTime(System.currentTimeMillis());
      if (output_thread_name) {
        debugStream.println("[" + formatter.format(now) + " | " + Thread.currentThread().getName()
                + "](" + location + ") " + message);
      } else {
        debugStream.println("[" + formatter.format(now) + "](" + location + ") " + message);
      }
    }
  }

  /**
   * Same function as {@link #printDebuggingMessage(String)} but easier to type.
   */
  static public String p(String message) {
    return printDebuggingMessage(message);
  }

  /**
   * Same function as {@link #printDebuggingMessage(String,String)} but easier to type.
   */
  static public void p(String location, String message) {
    printDebuggingMessage(location, message);
  }

  /**
   * Check the provided assertion. Used to be named "assert" but renamed to "Assert" to avoid name
   * conflict with the native JDK 1.4 assert function.
   * 
   * @param check
   *          The result of the assertion check, which should be false if it fails.
   * @exception AssertionFailedException
   *              thrown if the method parameter is false.
   */
  static public void Assert(boolean check) throws AssertionFailedException {
    if (!check) {
      throw new AssertionFailedException("no message");
    }
  }

  /**
   * Check the provided assertion. Used to be named "assert" but renamed to "Assert" to avoid name
   * conflict with the native JDK 1.4 assert function.
   * 
   * @param check
   *          Whether or not to throw the exception.
   * @param message
   *          A message to include in the thrown exception.
   * @exception AssertionFailedException
   *              thrown if the condition evaluates to false.
   */
  static public void Assert(boolean check, String message) throws AssertionFailedException {
    if (!check) {
      throw new AssertionFailedException(message);
    }
  }

  /**
   * Report a fatal exception to the error stream. This is package protected because the way to
   * handle fatal errors is to create and throw a FatalException, which takes care of calling this
   * method for you.
   * 
   * @param e
   *          The exception which will be logged as a fatal error.
   */
  synchronized static void reportFatalException(Throwable e) {
    if (log_exceptions) {
      printDebuggingMessage("====================================");
      debugStream.println("(FATAL ERROR) Unexpected exception: ");
      e.printStackTrace(debugStream);
      debugStream.flush();
    }
  }

  /**
   * Make sure any messages are flushed to the stream. Printing debug messages does not flush the
   * stream automatically, though reporting exceptions does.
   */
  synchronized static public void flush() {
    debugStream.flush();
  }

} // class
