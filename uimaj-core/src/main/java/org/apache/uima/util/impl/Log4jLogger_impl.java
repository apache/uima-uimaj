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

import java.lang.reflect.Field;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.slf4j.Marker;

/**
 * UIMA Logging interface implementation for Log4j
 * 
 * This version is for Log4j version 2, from Apache
 * 
 * Built using version 2.8
 */
public class Log4jLogger_impl extends Logger_common_impl {

  final static private Object[] zeroLengthArray = new Object[0];
  /**
   * <p>
   * Markers that are for marking levels not supported by log4j.
   * <p>
   * These are log4j class versions of the slf4j markers.
   */
  final static private org.apache.logging.log4j.Marker LOG4J_CONFIG = m(UIMA_MARKER_CONFIG);
  final static private org.apache.logging.log4j.Marker LOG4J_FINEST = m(UIMA_MARKER_FINEST);

  /**
   * Filters for use in setLevel calls, for levels that need marker filtering.
   * <p>
   * Filters return NEUTRAL unless it's for the associated level. For associated level (e.g., INFO
   * or TRACE), they return ACCEPT if the marker is present DENY otherwise
   */

  static private org.apache.logging.log4j.core.filter.AbstractFilter makeFilter(
          final org.apache.logging.log4j.Level tLevel, org.apache.logging.log4j.Marker tMarker) {
    return new org.apache.logging.log4j.core.filter.AbstractFilter() {

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.apache.logging.log4j.core.filter.AbstractFilter#filter(org.apache.logging.log4j.core.
       * LogEvent)
       */
      @Override
      public Result filter(LogEvent event) {
        if (event.getLevel() == tLevel) {
          return (event.getMarker() != tMarker) ? Result.DENY : Result.ACCEPT;
        }
        return Result.NEUTRAL;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.apache.logging.log4j.core.filter.AbstractFilter#filter(org.apache.logging.log4j.core.
       * Logger, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker,
       * org.apache.logging.log4j.message.Message, java.lang.Throwable)
       */
      @Override
      public Result filter(org.apache.logging.log4j.core.Logger logger,
              org.apache.logging.log4j.Level level, org.apache.logging.log4j.Marker marker,
              Message msg, Throwable t) {
        if (level == tLevel) {
          return (marker != tMarker) ? Result.DENY : Result.ACCEPT;
        }
        return Result.NEUTRAL;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.apache.logging.log4j.core.filter.AbstractFilter#filter(org.apache.logging.log4j.core.
       * Logger, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker, java.lang.Object,
       * java.lang.Throwable)
       */
      @Override
      public Result filter(org.apache.logging.log4j.core.Logger logger,
              org.apache.logging.log4j.Level level, org.apache.logging.log4j.Marker marker,
              Object msg, Throwable t) {
        if (level == tLevel) {
          return (marker != tMarker) ? Result.DENY : Result.ACCEPT;
        }
        return Result.NEUTRAL;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.apache.logging.log4j.core.filter.AbstractFilter#filter(org.apache.logging.log4j.core.
       * Logger, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker, java.lang.String,
       * java.lang.Object[])
       */
      @Override
      public Result filter(org.apache.logging.log4j.core.Logger logger,
              org.apache.logging.log4j.Level level, org.apache.logging.log4j.Marker marker,
              String msg, Object... params) {
        if (level == tLevel) {
          return (marker != tMarker) ? Result.DENY : Result.ACCEPT;
        }
        return Result.NEUTRAL;
      }

    };
  }

  final static private org.apache.logging.log4j.core.filter.AbstractFilter FILTER_CONFIG = makeFilter(
          org.apache.logging.log4j.Level.INFO, LOG4J_CONFIG);

  final static private org.apache.logging.log4j.core.filter.AbstractFilter FILTER_FINEST = makeFilter(
          org.apache.logging.log4j.Level.TRACE, LOG4J_FINEST);

  /**
   * logger object from the underlying Log4j logging framework The ExtendedLoggerWrapper includes
   * the ability to specify the wrapper class
   */
  final private ExtendedLoggerWrapper logger;

  final private org.apache.logging.log4j.core.Logger coreLogger;

  final private MessageFactory mf;

  /**
   * create a new LogWrapper class for the specified source class
   * 
   * @param component
   *          specified source class
   */
  private Log4jLogger_impl(Class<?> component) {
    super(component);

    coreLogger = (org.apache.logging.log4j.core.Logger) LogManager
            .getLogger((null == component) ? "org.apache.uima" : component.getName());
    mf = coreLogger.getMessageFactory();

    logger = new ExtendedLoggerWrapper((AbstractLogger) coreLogger, coreLogger.getName(), mf);
  }

  private Log4jLogger_impl(Log4jLogger_impl l, int limit) {
    super(l, limit);
    this.logger = l.logger;
    this.coreLogger = l.coreLogger;
    this.mf = l.mf;
  }

  /**
   * Creates a new Log4jLogger instance for the specified source class
   * 
   * @param component
   *          current source class
   * @return Logger returns the JSR47Logger object for the specified class
   */
  public static synchronized Logger getInstance(Class<?> component) {
    return new Log4jLogger_impl(component);
  }

  /**
   * Creates a new Log4jLogger instance with the default Log4j framework logger
   * 
   * @return Logger returns the JSR47Logger object with the default Log4j framework logger
   */
  public static synchronized Logger getInstance() {
    return new Log4jLogger_impl(null);
  }

  @Override
  public Log4jLogger_impl getLimitedLogger(int aLimit) {
    if (aLimit == Integer.MAX_VALUE || aLimit == this.limit_common) {
      return this;
    }
    return new Log4jLogger_impl(this, aLimit);
  }

  /**
   * log4j level mapping to UIMA level mapping. <br>
   * SEVERE (highest value) -&gt; SEVERE <br>
   * WARNING -&gt; WARNING <br>
   * INFO -&gt; INFO <br>
   * CONFIG -&gt; INFO <br>
   * FINE -&gt; DEBUG <br>
   * FINER -&gt; TRACE <br>
   * FINEST (lowest value) -&gt; TRACE <br>
   * OFF -&gt; OFF <br>
   * ALL -&gt; ALL <br>
   * 
   * @param level
   *          uima level
   * @return Level - corresponding log4j 2 level
   */
  static org.apache.logging.log4j.Level getLog4jLevel(Level level) {
    switch (level.toInteger()) {
      case org.apache.uima.util.Level.OFF_INT:
        return org.apache.logging.log4j.Level.OFF;
      case org.apache.uima.util.Level.SEVERE_INT:
        return org.apache.logging.log4j.Level.ERROR;
      case org.apache.uima.util.Level.WARNING_INT:
        return org.apache.logging.log4j.Level.WARN;
      case org.apache.uima.util.Level.INFO_INT:
        return org.apache.logging.log4j.Level.INFO;
      case org.apache.uima.util.Level.CONFIG_INT:
        return org.apache.logging.log4j.Level.INFO;
      case org.apache.uima.util.Level.FINE_INT:
        return org.apache.logging.log4j.Level.DEBUG;
      case org.apache.uima.util.Level.FINER_INT:
        return org.apache.logging.log4j.Level.TRACE;
      case org.apache.uima.util.Level.FINEST_INT:
        return org.apache.logging.log4j.Level.TRACE;
      default: // for all other cases return Level.ALL
        return org.apache.logging.log4j.Level.ALL;
    }
  }

  private static org.apache.logging.log4j.Marker m(Marker m) {
    if (m == null) {
      return null;
    }

    Field markerField = null;
    try {
      markerField = m.getClass().getDeclaredField("marker");
      markerField.setAccessible(true);
      return (org.apache.logging.log4j.Marker) markerField.get(m);
    } catch (Exception e) {
      // Well, best effort...
      return null;
    } finally {
      if (markerField != null) {
        markerField.setAccessible(false);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  @Override
  public boolean isLoggable(Level level) {
    if (level == Level.CONFIG) {
      Result r = filterTest(org.apache.logging.log4j.Level.INFO, LOG4J_CONFIG);
      return r == Result.ACCEPT || (r == Result.NEUTRAL
              && coreLogger.isEnabled(org.apache.logging.log4j.Level.TRACE));
    }
    if (level == Level.FINEST) {
      Result r = filterTest(org.apache.logging.log4j.Level.TRACE, LOG4J_FINEST);
      return r == Result.ACCEPT || (r == Result.NEUTRAL
              && coreLogger.isEnabled(org.apache.logging.log4j.Level.TRACE));

    }
    return coreLogger.isEnabled(getLog4jLevel(level));
  }

  @Override
  public boolean isLoggable(Level level, Marker marker) {
    return coreLogger.isEnabled(getLog4jLevel(level), m(marker));
  }

  // workaround for bug in log4j 2
  // where it skips using filters set via APIs for this test
  /**
   * 
   * @param level
   *          a log4j level that's equal or above (ERROR is highest) the level being tested
   * @param marker
   *          - the marker that needs to be there to allow this
   * @return - the result of running the logger filter test if there is one, else NEUTRAL
   */
  private Result filterTest(org.apache.logging.log4j.Level level,
          org.apache.logging.log4j.Marker marker) {
    Filter filter = coreLogger.get().getFilter();
    if (null != filter) {
      return filter.filter(coreLogger, level, marker, (String) null, (Object[]) null);
    }
    return Result.NEUTRAL;
  }

  /*
   * ONLY FOR TEST CASE USE
   */
  @Override
  public void setLevel(Level level) {
    if (level == Level.CONFIG) {
      // next seems to do nothing...
      // coreLogger.getContext().getConfiguration().getLoggerConfig(coreLogger.getName()).addFilter(FILTER_CONFIG);
      // next also seems to do nothing...
      // ((LoggerContext)LogManager.getContext(false)).getConfiguration().getLoggerConfig(coreLogger.getName()).addFilter(FILTER_CONFIG);
      coreLogger.get().addFilter(FILTER_CONFIG);
    } else {
      // coreLogger.getContext().getConfiguration().getLoggerConfig(coreLogger.getName()).removeFilter(FILTER_CONFIG);
      coreLogger.get().removeFilter(FILTER_CONFIG);
    }

    if (level == Level.FINEST) {
      coreLogger.get().addFilter(FILTER_FINEST);
    } else {
      coreLogger.get().removeFilter(FILTER_FINEST);
    }

    coreLogger.get().setLevel(getLog4jLevel(level));
    coreLogger.getContext().updateLoggers();
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    log(m, aFqcn, level, MessageFormat.format(message, args), thrown);
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Throwable thrown) {
    logger.logIfEnabled(aFqcn, getLog4jLevel(level), m(m), message, thrown);
  }

  @Override
  public void log2(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    if (thrown != null) {
      assert args == null;
      logger.logIfEnabled(aFqcn, getLog4jLevel(level), m(m), message, thrown);
    } else {
      logger.logIfEnabled(aFqcn, getLog4jLevel(level), m(m), message, args);
    }
  }

  // ----------------------

  @Override
  public String getName() {
    return logger.getName();
  }

  // ----------------------

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isDebugEnabled(Marker arg0) {
    return logger.isDebugEnabled(m(arg0));
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public boolean isErrorEnabled(Marker arg0) {
    return logger.isErrorEnabled(m(arg0));
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isInfoEnabled(Marker arg0) {
    return logger.isInfoEnabled(m(arg0));
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isTraceEnabled(Marker arg0) {
    return logger.isTraceEnabled(m(arg0));
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isWarnEnabled(Marker arg0) {
    return logger.isWarnEnabled(m(arg0));
  }

}
