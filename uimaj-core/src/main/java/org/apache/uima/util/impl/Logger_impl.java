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

package org.apache.uima.util.impl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * UIMA Logging interface implementation without using an logging toolkit Logger names are not used
 * 
 * The call getInstance() returns a common shared instance. The call getInstance(String) ignores its
 * argument but returns a new instance of this logger class.
 * 
 * Each instance of this logger class can have a level set via the setAPI call - that is the only
 * configuration possible. If not set, the level is INFO.
 *
 */
public class Logger_impl extends Logger_common_impl {
  /**
   * default PrintStream to which the log messages are printed. Defaults to <code>System.out</code>.
   */
  private static final PrintStream defaultOut = System.out;

  /**
   * PrintStream which the object is used to log the messages, is by default set to defaultOut
   */
  private PrintStream mOut;

  /**
   * message level to be logged
   */
  private Level configLevel = Level.INFO;

  private String loggerName;

  /**
   * default logger instance
   */
  private static final Logger_impl defaultLogger = new Logger_impl(null);

  /**
   * creates a new Logger object and set <code>System.out</code> as default output
   */
  private Logger_impl(Class<?> component) {
    super(component);
    // set default Output
    mOut = defaultOut;
    loggerName = (null == component) ? "" : component.getName();
  }

  private Logger_impl(Logger_impl l, int limit) {
    super(l, limit);
    mOut = l.mOut;
    loggerName = l.loggerName;
  }

  /**
   * creates a new Logger instance for the specified source class
   * 
   * @param component
   *          current source class
   * 
   * @return Logger - returns the Logger object for the specified class
   */
  public static synchronized Logger getInstance(Class<?> component) {
    return new Logger_impl(component);
  }

  /**
   * creates a new Logger instance for the specified source class
   * 
   * @return Logger - returns a new Logger object
   */
  public static synchronized Logger getInstance() {
    return defaultLogger;
  }

  @Override
  public Logger_impl getLimitedLogger(int aLimit) {
    if (aLimit == Integer.MAX_VALUE || aLimit == limit_common) {
      return this;
    }
    return new Logger_impl(this, aLimit);
  }

  /**
   * @deprecated use external configuration possibility
   * 
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
   */
  @Override
  @Deprecated(since = "2.3.1")
  public void setOutputStream(OutputStream out) {
    if (out == null || out instanceof PrintStream) {
      mOut = (PrintStream) out;
    } else {
      mOut = new PrintStream(out);
    }
  }

  /**
   * @deprecated use external configuration possibility
   * 
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.PrintStream)
   */
  @Override
  @Deprecated(since = "2.3.1")
  public void setOutputStream(PrintStream out) {
    mOut = out;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  @Override
  public boolean isLoggable(Level level) {
    return configLevel.isGreaterOrEqual(level);
  }

  @Override
  public boolean isLoggable(Level level, Marker marker) {
    return configLevel.isGreaterOrEqual(level);
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    log(m, aFqcn, level, MessageFormat.format(message, args), thrown);
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Throwable thrown) {
    if (mOut != null) {
      mOut.print(new Date());
      mOut.print(": " + level.toString() + ": ");
      mOut.println(message);
      if (null != thrown) {
        thrown.printStackTrace(mOut);
      }
    }
  }

  @Override
  public void log2(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    if (mOut != null) {
      mOut.print(new Date());
      mOut.print(": " + level.toString() + ": ");
      // this version of MessageFormatter handles {} style
      mOut.println(MessageFormatter.format(message, args).getMessage());
      if (null != thrown) {
        thrown.printStackTrace(mOut);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
   */
  @Override
  public void setLevel(Level level) {
    // set new config level
    configLevel = level;
  }

  @Override
  public String getName() {
    return loggerName;
  }

  @Override
  public boolean isDebugEnabled() {
    return isLoggable(Level.FINE);
  }

  @Override
  public boolean isDebugEnabled(Marker arg0) {
    return isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return isLoggable(Level.SEVERE);
  }

  @Override
  public boolean isErrorEnabled(Marker arg0) {
    return isErrorEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return isLoggable(Level.INFO) || isLoggable(Level.CONFIG);
  }

  @Override
  public boolean isInfoEnabled(Marker arg0) {
    return isInfoEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return isLoggable(Level.FINER) || isLoggable(Level.FINEST);
  }

  @Override
  public boolean isTraceEnabled(Marker arg0) {
    return isTraceEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return isLoggable(Level.WARNING);
  }

  @Override
  public boolean isWarnEnabled(Marker arg0) {
    return isWarnEnabled();
  }

}
