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

import java.text.MessageFormat;

import org.apache.uima.internal.util.Misc;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

/**
 * UIMA Logging interface implementation for SLF4j
 * 
 * This design gets a logger in static initialization, in order to see what the back end is. If it
 * is JUL or Log4j, it sets flags so that subsequent calls to getInstance gets those UIMA logger
 * impls, not this one, in order to slightly reduce indirection at run time.
 * 
 */
public class Slf4jLogger_impl extends Logger_common_impl {

  public static final String DEFAULT_JUL = "uima.use_jul_as_default_uima_logger";
  public static final boolean IS_DEFAULT_JUL = Misc.getNoValueSystemProperty(DEFAULT_JUL);

  static final boolean isJul;
  static final boolean isLog4j;

  static {
    Class<?> staticLoggerBinderClass = null;
    try {
      staticLoggerBinderClass = Class.forName("org.slf4j.impl.StaticLoggerBinder");
    } catch (Exception e) {
      // empty on purpose, if class not present, no back end logger, and staticLoggerBinderClass is
      // left as null
    }

    if (null == staticLoggerBinderClass) {
      if (IS_DEFAULT_JUL) {
        isJul = true;
        isLog4j = false;
      } else {
        isJul = false;
        isLog4j = false;
      }
    } else {
      // have some backend binding
      boolean tb;
      org.slf4j.Logger tempLogger = org.slf4j.LoggerFactory.getLogger("org.apache.uima");
      try { // for jdk14 impl
        Class<?> clazz = Class.forName("org.slf4j.impl.JDK14LoggerAdapter");
        tb = clazz != null && clazz.isAssignableFrom(tempLogger.getClass());
      } catch (ClassNotFoundException e1) {
        tb = false;
      }
      isJul = tb;

      tb = false;
      if (!isJul) {
        try { // for log4j 2 impl
          Class<?> clazz = Class.forName("org.apache.logging.slf4j.Log4jLogger");
          tb = null != clazz && clazz.isAssignableFrom(tempLogger.getClass());
        } catch (ClassNotFoundException e1) {
          tb = false;
        }
      }
      isLog4j = tb;
    }
  }

  /**
   * logger object from the underlying Slf4j logging framework
   */
  private final org.slf4j.Logger logger;
  private final boolean isLocationCapable; // the slf4j simple logger is not

  /**
   * create a new LogWrapper class for the specified source class
   * 
   * @param component
   *          specified source class
   */
  Slf4jLogger_impl(Class<?> component) {
    super(component);
    final String loggerName = (component != null) ? component.getName() : "org.apache.uima";

    logger = org.slf4j.LoggerFactory.getLogger(loggerName);
    isLocationCapable = logger instanceof org.slf4j.spi.LocationAwareLogger;
  }

  private Slf4jLogger_impl(Slf4jLogger_impl l, int limit) {
    super(l, limit);
    logger = l.logger;
    isLocationCapable = logger instanceof org.slf4j.spi.LocationAwareLogger;
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
    if (isJul) {
      return JSR47Logger_impl.getInstance(component);
    }
    if (isLog4j) {
      return Log4jLogger_impl.getInstance(component);
    }
    return new Slf4jLogger_impl(component);
  }

  /**
   * creates a new Logger instance using default name "org.apache.uima"
   * 
   * @return Logger - returns the Logger object for the specified class
   */
  public static synchronized Logger getInstance() {
    return getInstance(null);
  }

  @Override
  public Slf4jLogger_impl getLimitedLogger(int aLimit) {
    if (aLimit == Integer.MAX_VALUE || aLimit == limit_common) {
      return this;
    }
    return new Slf4jLogger_impl(this, aLimit);
  }

  public static int getSlf4jLevel(Level level) {
    switch (level.toInteger()) {
      case Level.SEVERE_INT:
        return LocationAwareLogger.ERROR_INT;
      case Level.WARNING_INT:
        return LocationAwareLogger.WARN_INT;
      case Level.INFO_INT:
        return LocationAwareLogger.INFO_INT;
      case Level.CONFIG_INT:
        return LocationAwareLogger.INFO_INT;
      case Level.FINE_INT:
        return LocationAwareLogger.DEBUG_INT;
      case Level.FINER_INT:
        return LocationAwareLogger.TRACE_INT;
      case Level.FINEST_INT:
        return LocationAwareLogger.TRACE_INT;
    }
    Misc.internalError();
    return LocationAwareLogger.ERROR_INT; // ignored, just here for compile error avoidance
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  @Override
  public boolean isLoggable(Level level) {
    switch (level.toInteger()) {
      case org.apache.uima.util.Level.OFF_INT:
        return false;
      case org.apache.uima.util.Level.SEVERE_INT:
        return logger.isErrorEnabled();
      case org.apache.uima.util.Level.WARNING_INT:
        return logger.isWarnEnabled();
      case org.apache.uima.util.Level.INFO_INT:
        return logger.isInfoEnabled();
      case org.apache.uima.util.Level.CONFIG_INT:
        return logger.isDebugEnabled(UIMA_MARKER_CONFIG);
      case org.apache.uima.util.Level.FINE_INT:
        return logger.isDebugEnabled();
      case org.apache.uima.util.Level.FINER_INT:
        return logger.isTraceEnabled();
      case org.apache.uima.util.Level.FINEST_INT:
        return logger.isTraceEnabled(UIMA_MARKER_FINEST);
      default: // for Level.ALL return false, that's what jul logger does
        return false;
    }
  }

  @Override
  public boolean isLoggable(Level level, Marker marker) {
    switch (level.toInteger()) {
      case org.apache.uima.util.Level.OFF_INT:
        return false;
      case org.apache.uima.util.Level.SEVERE_INT:
        return logger.isErrorEnabled(marker);
      case org.apache.uima.util.Level.WARNING_INT:
        return logger.isWarnEnabled(marker);
      case org.apache.uima.util.Level.INFO_INT:
        return logger.isInfoEnabled(marker);
      case org.apache.uima.util.Level.CONFIG_INT:
        return logger.isInfoEnabled(marker);
      case org.apache.uima.util.Level.FINE_INT:
        return logger.isDebugEnabled(marker);
      case org.apache.uima.util.Level.FINER_INT:
        return logger.isTraceEnabled(marker);
      case org.apache.uima.util.Level.FINEST_INT:
        return logger.isTraceEnabled(marker);
      default: // for Level.ALL return false, that's what jul logger does
        return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
   */
  @Override
  public void setLevel(Level level) {
    // allow nop operation
  }

  // does the uima-logger style of message formatting
  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    log(m, aFqcn, level, MessageFormat.format(message, args), thrown);
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String msg_with_params, Throwable thrown) {
    m = (m == null) ? getMarkerForLevel(level) : m;

    if (isLocationCapable) { // slf4j simple logger is not
      ((org.slf4j.spi.LocationAwareLogger) logger).log(m, aFqcn, getSlf4jLevel(level),
              msg_with_params, null, thrown);
    } else {
      switch (level.toInteger()) {
        case Level.SEVERE_INT:
          // all of these calls to MessageFormat are to the java.text.MessageFormat
          // to do {n} style format substitution
          logger.error(m, msg_with_params, thrown);
          break;
        case Level.WARNING_INT:
          logger.warn(m, msg_with_params, thrown);
          break;
        case Level.INFO_INT:
          logger.info(m, msg_with_params, thrown);
          break;
        case Level.CONFIG_INT:
          logger.info(m, msg_with_params, thrown);
          break;
        case Level.FINE_INT:
          logger.debug(m, msg_with_params, thrown);
          break;
        case Level.FINER_INT:
          logger.trace(m, msg_with_params, thrown);
          break;
        case Level.FINEST_INT:
          logger.trace(m, msg_with_params, thrown);
          break;
        default:
          Misc.internalError();
      }
    }

  }

  // does the slf4j style of message formatting
  @Override
  public void log2(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    m = (m == null) ? getMarkerForLevel(level) : m;

    if (isLocationCapable) { // slf4j simple logger is not
      // Work around LOG4J2-3177 by pulling a throwable from the args and providing it as thrown
      // to the logger
      Throwable actualThrown = thrown;
      Object[] actualArgs = args;
      if (actualThrown == null && args != null && args.length > 0
              && args[args.length - 1] instanceof Throwable) {
        actualThrown = (Throwable) args[args.length - 1];
        actualArgs = new Object[args.length - 1];
        System.arraycopy(args, 0, actualArgs, 0, actualArgs.length);
      }

      ((org.slf4j.spi.LocationAwareLogger) logger).log(m, aFqcn, getSlf4jLevel(level), message,
              actualArgs, actualThrown);
    } else {
      if (thrown != null) {
        Object[] args1 = (args == null) ? new Object[1] : new Object[args.length + 1];
        if (args != null) {
          System.arraycopy(args, 0, args1, 0, args.length);
        }
        args1[args1.length - 1] = thrown;
        args = args1;
      }
      switch (level.toInteger()) {
        case Level.SEVERE_INT:
          logger.error(m, message, args);
          break;
        case Level.WARNING_INT:
          logger.warn(m, message, args);
          break;
        case Level.INFO_INT:
          logger.info(m, message, args);
          break;
        case Level.CONFIG_INT:
          logger.info(m, message, args);
          break;
        case Level.FINE_INT:
          logger.debug(m, message, args);
          break;
        case Level.FINER_INT:
          logger.trace(m, message, args);
          break;
        case Level.FINEST_INT:
          logger.trace(m, message, args);
          break;
        default:
          Misc.internalError();
      }
    }
  }

  /**
   * @return the logger name
   * @see org.slf4j.Logger#getName()
   */
  @Override
  public String getName() {
    return logger.getName();
  }

  /**
   * @return -
   * @see org.slf4j.Logger#isTraceEnabled()
   */
  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  /**
   * @param marker
   *          -
   * @return true if trace is enabled for this marker
   * @see org.slf4j.Logger#isTraceEnabled(org.slf4j.Marker)
   */
  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  /**
   * @return -
   * @see org.slf4j.Logger#isDebugEnabled()
   */
  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  /**
   * @param marker
   *          -
   * @return true if is enabled for this marker
   * @see org.slf4j.Logger#isDebugEnabled(org.slf4j.Marker)
   */
  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  /**
   * @return -
   * @see org.slf4j.Logger#isInfoEnabled()
   */
  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  /**
   * @param marker
   *          -
   * @return true if is enabled for this marker
   * @see org.slf4j.Logger#isInfoEnabled(org.slf4j.Marker)
   */
  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  /**
   * @return -
   * @see org.slf4j.Logger#isWarnEnabled()
   */
  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  /**
   * @param marker
   *          -
   * @return true if is enabled for this marker
   * @see org.slf4j.Logger#isWarnEnabled(org.slf4j.Marker)
   */
  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  /**
   * @return -
   * @see org.slf4j.Logger#isErrorEnabled()
   */
  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  /**
   * @param marker
   *          -
   * @return true if is enabled for this marker
   * @see org.slf4j.Logger#isErrorEnabled(org.slf4j.Marker)
   */
  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

}
