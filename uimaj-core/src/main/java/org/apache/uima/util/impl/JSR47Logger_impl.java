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
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.uima.internal.util.UIMALogFormatter;
import org.apache.uima.internal.util.UIMAStreamHandler;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * UIMA Logging interface implementation for Java Logging Toolkit JSR-47 (JDK 1.4) JUL
 * 
 * Ignores Markers and MDC (not supported in the JUL)
 */
public class JSR47Logger_impl extends Logger_common_impl {

  final static private Object[] zeroLengthArray = new Object[0];

  /**
   * logger object from the underlying JSR-47 logging framework
   */
  final private java.util.logging.Logger logger;

  /**
   * create a new LogWrapper class for the specified source class
   * 
   * @param component
   *          specified source class
   */
  private JSR47Logger_impl(Class<?> component) {
    super(component);

    logger = java.util.logging.Logger
            .getLogger((component != null) ? component.getName() : "org.apache.uima");
  }

  private JSR47Logger_impl(JSR47Logger_impl l, int limit) {
    super(l, limit);
    this.logger = l.logger;
  }

  /**
   * Creates a new JSR47Logger instance for the specified source class
   * 
   * @param component
   *          current source class
   * 
   * @return Logger returns the JSR47Logger object for the specified class
   */
  public static synchronized Logger getInstance(Class<?> component) {
    return new JSR47Logger_impl(component);
  }

  public static synchronized Logger getInstance(JSR47Logger_impl l, int limit) {
    if (limit == Integer.MAX_VALUE) {
      return l;
    }
    return new JSR47Logger_impl(l, limit);
  }

  /**
   * Creates a new JSR47Logger instance with the default JSR-47 framework logger
   * 
   * @return Logger returns the JSR47Logger object with the default JSR-47 framework logger
   */
  public static synchronized JSR47Logger_impl getInstance() {
    return new JSR47Logger_impl(null);
  }

  @Override
  public JSR47Logger_impl getLimitedLogger(int limit) {
    if (limit == Integer.MAX_VALUE || limit == this.limit_common) {
      return this;
    }
    return new JSR47Logger_impl(this, limit);
  }

  /**
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
   * 
   * @deprecated use external configuration possibility
   */
  @Override
  @Deprecated
  public void setOutputStream(OutputStream out) {
    // if OutputStream is null set root logger level to OFF
    if (out == null) {
      LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.OFF);
      return;
    }

    // get root logger handlers - root logger is parent of all loggers
    Handler[] handlers = LogManager.getLogManager().getLogger("").getHandlers();

    // remove all current handlers
    for (int i = 0; i < handlers.length; i++) {
      LogManager.getLogManager().getLogger("").removeHandler(handlers[i]);
    }

    // add new UIMAStreamHandler with the given output stream
    UIMAStreamHandler streamHandler = new UIMAStreamHandler(out, new UIMALogFormatter());
    streamHandler.setLevel(java.util.logging.Level.ALL);
    LogManager.getLogManager().getLogger("").addHandler(streamHandler);
  }

  /**
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.PrintStream)
   * 
   * @deprecated use external configuration possibility
   */
  @Override
  @Deprecated
  public void setOutputStream(PrintStream out) {
    // if PrintStream is null set root logger level to OFF
    if (out == null) {
      LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.OFF);
      return;
    }

    // get root logger handlers - root logger is parent of all loggers
    Handler[] handlers = LogManager.getLogManager().getLogger("").getHandlers();

    // remove all current handlers
    for (int i = 0; i < handlers.length; i++) {
      LogManager.getLogManager().getLogger("").removeHandler(handlers[i]);
    }

    // add new UIMAStreamHandler with the given output stream
    UIMAStreamHandler streamHandler = new UIMAStreamHandler(out, new UIMALogFormatter());
    streamHandler.setLevel(java.util.logging.Level.ALL);
    LogManager.getLogManager().getLogger("").addHandler(streamHandler);
  }

  /**
   * JSR-47 level mapping to UIMA level mapping.
   * 
   * Maps via marker values for UIMA_MARKER_CONFIG and UIMA_MARKER_FINEST
   * 
   * SEVERE (highest value) -%gt; SEVERE<br>
   * WARNING -%gt; WARNING<br>
   * INFO -%gt; INFO <br>
   * CONFIG -%gt; CONFIG <br>
   * FINE -%gt; FINE<br>
   * FINER -%gt; FINER <br>
   * FINEST (lowest value) -%gt; FINEST<br>
   * OFF -%gt; OFF <br>
   * ALL -%gt; ALL<br>
   * 
   * @param level
   *          uima level
   * @param m
   *          the marker
   * 
   * @return Level - corresponding JSR47 level
   */
  public static java.util.logging.Level getJSR47Level(Level level, Marker m) {
    if (null == level) {
      return null;
    }
    switch (level.toInteger()) {
      case org.apache.uima.util.Level.OFF_INT:
        return java.util.logging.Level.OFF;
      case org.apache.uima.util.Level.SEVERE_INT:
        return java.util.logging.Level.SEVERE;
      case org.apache.uima.util.Level.WARNING_INT:
        return java.util.logging.Level.WARNING;
      case org.apache.uima.util.Level.INFO_INT:
        return (m == UIMA_MARKER_CONFIG) ? java.util.logging.Level.CONFIG
                : java.util.logging.Level.INFO;
      case org.apache.uima.util.Level.CONFIG_INT:
        return java.util.logging.Level.CONFIG;
      case org.apache.uima.util.Level.FINE_INT:
        return java.util.logging.Level.FINE;
      case org.apache.uima.util.Level.FINER_INT:
        // could be DEBUG with marker FINEST, DEBUG_INT == FINER_INT
        return (m == UIMA_MARKER_FINEST) ? java.util.logging.Level.FINEST
                : java.util.logging.Level.FINER;
      case org.apache.uima.util.Level.FINEST_INT:
        return java.util.logging.Level.FINEST;

      default: // for all other cases return Level.ALL
        return java.util.logging.Level.ALL;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  @Override
  public boolean isLoggable(Level level) {
    return logger.isLoggable(getJSR47Level(level, null));
  }

  @Override
  public boolean isLoggable(Level level, Marker m) {
    return logger.isLoggable(getJSR47Level(level, m));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
   */
  @Override
  public void setLevel(Level level) {
    logger.setLevel(getJSR47Level(level, null));
  }

  // /**
  // * Log the message at the specified level with the specified throwable if any.
  // * This method creates a LogRecord and fills in caller date before calling
  // * this instance's JDK14 logger.
  // *
  // * See bug report #13 for more details.
  // *
  // * @param level
  // * @param msg
  // * @param t
  // */
  // private void log(String callerFQCN, Level level, String msg, Throwable t) {
  // // millis and thread are filled by the constructor
  // LogRecord record = new LogRecord(level, msg);
  // record.setLoggerName(getName());
  // record.setThrown(t);
  // // Note: parameters in record are not set because SLF4J only
  // // supports a single formatting style
  // fillCallerData(callerFQCN, record);
  // logger.log(record);
  // }

  @Override
  public void log(Marker m, String aFqcn, Level level, String msg, Object[] args,
          Throwable throwable) {
    if (isLoggable(level, m)) {
      log(m, aFqcn, level, MessageFormat.format(msg, args), throwable);
    }
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String msg, Throwable throwable) {
    if (isLoggable(level, m)) {
      LogRecord record = new LogRecord(getJSR47Level(level, m), msg);
      record.setLoggerName(getName());
      record.setThrown(throwable);

      StackTraceElement[] elements = new Throwable().getStackTrace();
      StackTraceElement top = null;

      boolean found = false;

      for (int i = 0; i < elements.length; i++) {
        final String className = elements[i].getClassName();
        if (className.equals(aFqcn)) {
          if (found) {
            continue; // keep going until not found
          } else {
            found = true;
            continue;
          }
        } else {
          if (found) {
            top = elements[i];
            break;
          }
        }
      }

      if (top != null) {
        record.setSourceClassName(top.getClassName());
        record.setSourceMethodName(top.getMethodName() + "(" + top.getLineNumber() + ")");
      }
      logger.log(record);
    }
  }

  @Override
  public void log2(Marker m, String aFqcn, Level level, String msg, Object[] args,
          Throwable throwable) {
    // this version of MessageFormatter does the {} style
    log(m, aFqcn, level, MessageFormatter.format(msg, args).getMessage(), zeroLengthArray,
            throwable);
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isLoggable(java.util.logging.Level.FINE);
  }

  @Override
  public boolean isDebugEnabled(Marker arg0) {
    return isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isLoggable(java.util.logging.Level.SEVERE);
  }

  @Override
  public boolean isErrorEnabled(Marker arg0) {
    return isErrorEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isLoggable(java.util.logging.Level.INFO)
            || logger.isLoggable(java.util.logging.Level.CONFIG);
  }

  @Override
  public boolean isInfoEnabled(Marker arg0) {
    return isInfoEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isLoggable(java.util.logging.Level.FINER)
            || logger.isLoggable(java.util.logging.Level.FINEST);
  }

  @Override
  public boolean isTraceEnabled(Marker arg0) {
    return isTraceEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isLoggable(java.util.logging.Level.WARNING);
  }

  @Override
  public boolean isWarnEnabled(Marker arg0) {
    return isWarnEnabled();
  }

}
