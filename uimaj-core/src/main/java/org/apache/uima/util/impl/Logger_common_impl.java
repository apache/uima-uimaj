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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.slf4j.Marker;

/**
 * UIMA Logging interface common implementation Specific loggers extend this class
 * <p>
 * Logging "location" information:
 * <ul>
 * <li>This is the Classname / Methodname / and maybe line number where the logging statement
 * is</li>
 * <li>is passed in on the logrb calls, but is not needed by modern loggers.</li>
 * <li>In V3, passed in value is ignored; loggers get what they need as configured.</li>
 * <li>In Java 9 this will be efficient</li>
 * </ul>
 * 
 * <p>
 * Limiting or throttling loggers: This is normally done using logger configuration. For cases where
 * UIMA is running as an embedded library, sometimes Annotators log excessivly, and users do not
 * have access to the logging configuration. But they do have access to APIs which create the UIMA
 * pipelines.
 * <p>
 * V3 supports an additional param, AnalysisEngine.PARAM_THROTTLE_EXCESSIVE_ANNOTATOR_LOGGING which
 * if set, specifies a limit of the number of log messages issued by Annotator code.
 * <p>
 * This requires:
 * <ul>
 * <li>marking loggers if they are Annotator loggers (e.g., their associated "class" used in setting
 * the name of the logger, is assignable to AnalysisComponent_ImplBase, which includes: Annotators,
 * CasMultipliers, and UimacppAnalysisComponents.</li>
 * <li>When setting up a logger in the UimaContext logger code (via setLogger), checking if the
 * logger is an Annotator logger, and if so, setting the limit on it from the parameter associated
 * with the UIMA context.</li>
 * </ul>
 * 
 * <p>
 * The loggers with a limit are cloned for the particular pipeline (represented by the root UIMA
 * context), so that setting the limit only affects one pipeline.
 * 
 * <p>
 * The common part of logging does:
 * <ul>
 * <li>optional throttling</li>
 * <li>the UIMA specific resource bundle message conversions</li>
 * <li>the conversion of variants of log methods to standard ones</li>
 * </ul>
 * 
 */
public abstract class Logger_common_impl implements Logger {
  protected static final String EXCEPTION_MESSAGE = "Exception occurred";
  protected static final String[] EMPTY_STACK_TRACE_INFO = new String[] { null, null };

  protected final String fqcn = this.getClass().getName(); // the subclass name
  protected final String fqcnCmn = Logger_common_impl.class.getName(); // this class

  // for throttling misbehaving Annotator Loggers
  private int SEVERE_COUNT = 0;
  private int WARNING_COUNT = 0;
  private int INFO_COUNT = 0;
  private int CONFIG_COUNT = 0;
  private int FINE_COUNT = 0;
  private int FINER_COUNT = 0;
  private int FINEST_COUNT = 0;

  protected final int limit_common;
  private final boolean isLimited; // master switch tested first
  private final AtomicInteger dontSetResourceManagerCount = new AtomicInteger();

  /**
   * ResourceManager whose extension ClassLoader will be used to locate the message digests. Null
   * will cause the ClassLoader to default to this.class.getClassLoader().
   * 
   * @Deprecated When a logger is used within UIMA, the resource manager is picked up from the
   *             {@link UimaContextHolder} and if none is available, then the class loader set on
   *             the {@link Thread#getContextClassLoader()} is used. Thus, setting a resource
   *             manager for loading message localizations should not be required. Setting a
   *             resource manager anyway can lead to resource being registered in the resource
   *             manager to not be garbage collected in a timely manner. Also, the logger is shared
   *             globally and in a multi-threaded/multi-classloader scenario, it is likely that
   *             different threads overwrite each others logger resource manager making it likely
   *             that in any given thread the wrong resource manager is used by the logger.
   */
  private ResourceManager mResourceManager = null;
  private boolean isAnnotatorLogger;

  protected Logger_common_impl(Class<?> component) {
    limit_common = Integer.MAX_VALUE;
    isLimited = false;
  }

  /**
   * Copy constructor for limited loggers
   * 
   * @param lci
   *          the original logger to copy
   * @param limit
   *          the limit
   */
  protected Logger_common_impl(Logger_common_impl lci, int limit) {
    limit_common = limit;
    isLimited = true;
    isAnnotatorLogger = true;
    mResourceManager = lci.mResourceManager;
  }

  /*********************************************
   * Abstract methods not in UIMA Logger interface that must be implemented by subclasses
   *********************************************/

  /**
   * The main log call implemented by subclasses
   * 
   * @param m
   *          the marker
   * @param aFqcn
   *          the fully qualified class name of the top-most logging class used to filter the stack
   *          trace to get the caller class / method info
   * @param level
   *          the UIMA level
   * @param message
   *          -
   * @param args
   *          - arguments to be substituted into the message
   * @param throwable
   *          - can be null
   */
  public abstract void log(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable throwable);

  /**
   * The version of the main log call implemented by subclasses that uses {}, not {n} as the
   * substitutable syntax.
   * 
   * This syntax is used by log4j, slf4j, and others. But not used by uimaj logger basic syntax, or
   * Java Util Logger.
   *
   * This version is called by all new logging statments that don't need to be backwards compatible.
   * e.g. logger.info, logger.error, logger.warn, etc.
   * 
   * @param m
   *          the marker
   * @param aFqcn
   *          the fully qualified class name of the top-most logging class used to filter the stack
   *          trace to get the caller class / method info
   * @param level
   *          the UIMA level
   * @param message
   *          -
   * @param args
   *          - arguments to be substituted into the message
   * @param throwable
   *          - can be null
   */
  public abstract void log2(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable throwable);

  /**
   * The version of the main log call implemented by subclasses that skips the substitution because
   * it already was done by rb()
   * 
   * @param m
   *          the marker
   * @param aFqcn
   *          the fully qualified class name of the top-most logging class used to filter the stack
   *          trace to get the caller class / method info
   * @param level
   *          the UIMA level
   * @param message
   *          -
   * @param throwable
   *          - can be null
   */
  public abstract void log(Marker m, String aFqcn, Level level, String message,
          Throwable throwable);

  /**
   * @param level
   *          the Uima Level
   * @return the Marker to use
   */
  public static Marker getMarkerForLevel(Level level) {
    switch (level.toInteger()) {
      case Level.CONFIG_INT:
        return UIMA_MARKER_CONFIG;
      case Level.FINEST_INT:
        return UIMA_MARKER_FINEST;
      default:
        return null;
    }
  }

  /**
   * Convert a standard UIMA call for wrapped loggers
   * 
   * @param aFqcn
   *          - fully qualified class name of highest level of logging impl. The class / method
   *          above this in the stack trace is used for identifying where the logging call
   *          originated from.
   * @param level
   *          the uima Level
   * @param message
   *          the message
   * @param thrown
   *          may be null
   */
  @Override
  public void log(String aFqcn, Level level, String message, Throwable thrown) {
    // log(getMarkerForLevel(level), aFqcn, level, message, null, thrown);
    log(getMarkerForLevel(level), aFqcn, level, message, thrown);
  }

  /**
   * 
   * @param level
   *          -
   * @return true if not limited
   */
  private boolean isNotLimited(Level level) {
    if (!isLimited) {
      return true;
    }
    switch (level.toInteger()) {
      case Level.SEVERE_INT:
        if (SEVERE_COUNT >= limit_common) {
          return false;
        }
        SEVERE_COUNT++;
        return true;
      case Level.WARNING_INT:
        if (WARNING_COUNT >= limit_common) {
          return false;
        }
        WARNING_COUNT++;
        return true;
      case Level.INFO_INT:
        if (INFO_COUNT >= limit_common) {
          return false;
        }
        INFO_COUNT++;
        return true;
      case Level.CONFIG_INT:
        if (CONFIG_COUNT >= limit_common) {
          return false;
        }
        CONFIG_COUNT++;
        return true;
      case Level.FINE_INT:
        if (FINE_COUNT >= limit_common) {
          return false;
        }
        FINE_COUNT++;
        return true;
      case Level.FINER_INT:
        if (FINER_COUNT >= limit_common) {
          return false;
        }
        FINER_COUNT++;
        return true;
      case Level.FINEST_INT:
        if (FINEST_COUNT >= limit_common) {
          return false;
        }
        FINEST_COUNT++;
        return true;
    }
    Misc.internalError();
    return false;
  }

  /**
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
   * 
   * @deprecated use external configuration possibility
   */
  @Override
  @Deprecated
  public void setOutputStream(OutputStream out) {
    throw new UnsupportedOperationException();
  }

  /**
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.PrintStream)
   * 
   * @deprecated use external configuration possibility
   */
  @Override
  @Deprecated
  public void setOutputStream(PrintStream out) {
    throw new UnsupportedOperationException();
  }

  /**
   * Logs a message with level INFO.
   * 
   * @deprecated use new function with log level
   * 
   * @param aMessage
   *          the message to be logged
   */
  @Override
  @Deprecated
  public void log(String aMessage) {
    if (isLoggable(Level.INFO) && !isEmpty(aMessage) && isNotLimited(Level.INFO)) {
      log(fqcnCmn, Level.INFO, aMessage, null);
    }
  }

  /**
   * Logs a message with a message key and the level INFO
   * 
   * @deprecated use new function with log level
   * 
   * @see org.apache.uima.util.Logger#log(java.lang.String, java.lang.String, java.lang.Object[])
   */
  @Override
  @Deprecated
  public void log(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    if (isLoggable(Level.INFO) && !isEmpty(aMessageKey) && isNotLimited(Level.INFO)) {
      log(fqcnCmn, Level.INFO, rb(aResourceBundleName, aMessageKey, aArguments), null);
    }
  }

  /**
   * Logs an exception with level INFO
   * 
   * @deprecated use new function with log level
   * 
   * @param aException
   *          the exception to be logged
   */
  @Override
  @Deprecated
  public void logException(Exception aException) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO) && aException != null) {
      log(fqcnCmn, Level.INFO, EXCEPTION_MESSAGE, aException);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String)
   */
  @Override
  public void log(Level level, String aMessage) {
    if (isLoggable(level) && !isEmpty(aMessage) && isNotLimited(level)) {
      log(fqcnCmn, level, aMessage, null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   * java.lang.Object)
   */
  @Override
  public void log(Level level, String aMessage, Object param1) {
    if (isLoggable(level) && !isEmpty(aMessage) && isNotLimited(level)) {
      log(fqcnCmn, level, MessageFormat.format(aMessage, new Object[] { param1 }), null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   * java.lang.Object[])
   */
  @Override
  public void log(Level level, String aMessage, Object[] params) {
    if (isLoggable(level) && !isEmpty(aMessage) && isNotLimited(level)) {
      log(fqcnCmn, level, MessageFormat.format(aMessage, params), null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   * java.lang.Throwable)
   */
  @Override
  public void log(Level level, String aMessage, Throwable thrown) {
    if (isLoggable(level) && isNotLimited(level)) {
      log(fqcnCmn, level, (aMessage != null && !aMessage.equals("")) ? aMessage : EXCEPTION_MESSAGE,
              thrown);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
   */
  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object param1) {
    if (isLoggable(level) && !isEmpty(msgKey) && isNotLimited(level)) {
      log(fqcnCmn, level, rb(bundleName, msgKey, param1), null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
   */
  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object[] params) {
    if (isLoggable(level) && !isEmpty(msgKey) && isNotLimited(level)) {
      log(fqcnCmn, level, rb(bundleName, msgKey, params), null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable)
   */
  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Throwable thrown) {
    if (isLoggable(level) && isNotLimited(level)) {
      if (thrown == null && isEmpty(msgKey)) {
        return;
      }
      log(fqcnCmn, level,
              (msgKey != null && !msgKey.equals("")) ? rb(bundleName, msgKey) : EXCEPTION_MESSAGE,
              thrown);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey) {
    if (isLoggable(level) && !isEmpty(msgKey) && isNotLimited(level)) {
      log(fqcnCmn, level, rb(bundleName, msgKey), null);
    }
  }

  @Deprecated
  @Override
  public void setResourceManager(ResourceManager resourceManager) {
    mResourceManager = resourceManager;
    Misc.decreasingWithTrace(dontSetResourceManagerCount,
            "Setting a resouce manager on a logger can lead to memory leaks and to the inability of locating message localizations in multi-classloader scenaros.",
            UIMAFramework.getLogger());
  }

  /**
   * Gets the extension ClassLoader to used to locate the message digests. If this returns null,
   * then message digests will be searched for using this.class.getClassLoader().
   */
  private ClassLoader getExtensionClassLoader() {
    if (mResourceManager != null) {
      return mResourceManager.getExtensionClassLoader();
    }

    UimaContext context = UimaContextHolder.getContext();
    if (context != null) {
      return ((UimaContextAdmin) context).getResourceManager().getExtensionClassLoader();
    }

    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (contextClassLoader != null) {
      return contextClassLoader;
    }

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#rb(String, String, Object...)
   */
  @Override
  public String rb(String bundleName, String msgKey, Object... parameters) {
    return I18nUtil.localizeMessage(bundleName, msgKey, parameters, getExtensionClassLoader());
  }

  protected boolean isEmpty(String v) {
    return (v == null || v.equals(""));
  }

  @Override
  public boolean isAnnotatorLogger() {
    return isAnnotatorLogger;
  }

  public void setAnnotatorLogger(boolean v) {
    isAnnotatorLogger = v;
  }

  private Object[] suppliersToArray(Supplier<?>[] suppliers) {
    Object[] r = new Object[suppliers.length];
    int i = 0;
    for (Supplier<?> s : suppliers) {
      r[i++] = s.get();
    }
    return r;
  }

  /************************************************
   * Convert standard call varieties
   ************************************************/

  @Override
  public void debug(String arg0) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, arg0, null, null);
    }
  }

  @Override
  public void debug(String arg0, Object arg1) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, arg0, new Object[] { arg1 }, null);
    }
  }

  @Override
  public void debug(String arg0, Object... arg1) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, arg0, arg1, null);
    }
  }

  @Override
  public void debug(String arg0, Throwable arg1) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, arg0, null, arg1);
    }
  }

  @Override
  public void debug(Marker arg0, String arg1) {
    if (isLoggable(Level.DEBUG, arg0) && isNotLimited(Level.DEBUG)) {
      log2(arg0, fqcnCmn, Level.DEBUG, arg1, null, null);
    }
  }

  @Override
  public void debug(String arg0, Object arg1, Object arg2) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, arg0, new Object[] { arg1, arg2 }, null);
    }
  }

  @Override
  public void debug(Marker arg0, String arg1, Object arg2) {
    if (isLoggable(Level.DEBUG, arg0) && isNotLimited(Level.DEBUG)) {
      log2(arg0, fqcnCmn, Level.DEBUG, arg1, new Object[] { arg2 }, null);
    }
  }

  @Override
  public void debug(Marker arg0, String arg1, Object... arg2) {
    if (isLoggable(Level.DEBUG, arg0) && isNotLimited(Level.DEBUG)) {
      log2(arg0, fqcnCmn, Level.DEBUG, arg1, arg2, null);
    }
  }

  @Override
  public void debug(Marker arg0, String arg1, Throwable arg2) {
    if (isLoggable(Level.DEBUG, arg0) && isNotLimited(Level.DEBUG)) {
      log2(arg0, fqcnCmn, Level.DEBUG, arg1, null, arg2);
    }
  }

  @Override
  public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
    if (isLoggable(Level.DEBUG, arg0) && isNotLimited(Level.DEBUG)) {
      log2(arg0, fqcnCmn, Level.DEBUG, arg1, new Object[] { arg2, arg3 }, null);
    }
  }

  // methods from log4j 2 using Java 8 suppliers

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void debug(Supplier<String> msgSupplier) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   * @param throwable
   *          the exception to log
   */
  @Override
  public void debug(Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, msgSupplier.get(), null, throwable);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void debug(Marker marker, String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.DEBUG, marker) && isNotLimited(Level.DEBUG)) {
      log2(marker, fqcnCmn, Level.DEBUG, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void debug(String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.DEBUG) && isNotLimited(Level.DEBUG)) {
      log2(null, fqcnCmn, Level.DEBUG, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void debug(Marker marker, Supplier<String> msgSupplier) {
    if (isLoggable(Level.DEBUG, marker) && isNotLimited(Level.DEBUG)) {
      log2(marker, fqcnCmn, Level.DEBUG, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void debug(Marker marker, Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.DEBUG, marker) && isNotLimited(Level.DEBUG)) {
      log2(marker, fqcnCmn, Level.DEBUG, msgSupplier.get(), null, throwable);
    }
  }

  // ---------------------- ERROR

  @Override
  public void error(String arg0) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, arg0, null, null);
    }
  }

  @Override
  public void error(String arg0, Object arg1) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, arg0, new Object[] { arg1 }, null);
    }
  }

  @Override
  public void error(String arg0, Object... arg1) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, arg0, arg1, null);
    }
  }

  @Override
  public void error(String arg0, Throwable arg1) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, arg0, null, arg1);
    }
  }

  @Override
  public void error(Marker arg0, String arg1) {
    if (isLoggable(Level.ERROR, arg0) && isNotLimited(Level.ERROR)) {
      log2(arg0, fqcnCmn, Level.ERROR, arg1, null, null);
    }
  }

  @Override
  public void error(String arg0, Object arg1, Object arg2) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, arg0, new Object[] { arg1, arg2 }, null);
    }
  }

  @Override
  public void error(Marker arg0, String arg1, Object arg2) {
    if (isLoggable(Level.ERROR, arg0) && isNotLimited(Level.ERROR)) {
      log2(arg0, fqcnCmn, Level.ERROR, arg1, new Object[] { arg2 }, null);
    }
  }

  @Override
  public void error(Marker arg0, String arg1, Object... arg2) {
    if (isLoggable(Level.ERROR, arg0) && isNotLimited(Level.ERROR)) {
      log2(arg0, fqcnCmn, Level.ERROR, arg1, arg2, null);
    }
  }

  @Override
  public void error(Marker arg0, String arg1, Throwable arg2) {
    if (isLoggable(Level.ERROR, arg0) && isNotLimited(Level.ERROR)) {
      log2(arg0, fqcnCmn, Level.ERROR, arg1, null, arg2);
    }
  }

  @Override
  public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
    if (isLoggable(Level.ERROR, arg0) && isNotLimited(Level.ERROR)) {
      log2(arg0, fqcnCmn, Level.ERROR, arg1, new Object[] { arg2, arg3 }, null);
    }
  }

  // methods from log4j 2 using Java 8 suppliers

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void error(Supplier<String> msgSupplier) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   * @param throwable
   *          the exception to log
   */
  @Override
  public void error(Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, msgSupplier.get(), null, throwable);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void error(Marker marker, String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.ERROR, marker) && isNotLimited(Level.ERROR)) {
      log2(marker, fqcnCmn, Level.ERROR, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void error(String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.ERROR) && isNotLimited(Level.ERROR)) {
      log2(null, fqcnCmn, Level.ERROR, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void error(Marker marker, Supplier<String> msgSupplier) {
    if (isLoggable(Level.ERROR, marker) && isNotLimited(Level.ERROR)) {
      log2(marker, fqcnCmn, Level.ERROR, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void error(Marker marker, Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.ERROR, marker) && isNotLimited(Level.ERROR)) {
      log2(marker, fqcnCmn, Level.ERROR, msgSupplier.get(), null, throwable);
    }
  }

  // ---------------------- INFO

  @Override
  public void info(String arg0) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, arg0, null, null);
    }
  }

  @Override
  public void info(String arg0, Object arg1) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, arg0, new Object[] { arg1 }, null);
    }
  }

  @Override
  public void info(String arg0, Object... arg1) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, arg0, arg1, null);
    }
  }

  @Override
  public void info(String arg0, Throwable arg1) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, arg0, null, arg1);
    }
  }

  @Override
  public void info(Marker arg0, String arg1) {
    if (isLoggable(Level.INFO, arg0) && isNotLimited(Level.INFO)) {
      log2(arg0, fqcnCmn, Level.INFO, arg1, null, null);
    }
  }

  @Override
  public void info(String arg0, Object arg1, Object arg2) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, arg0, new Object[] { arg1, arg2 }, null);
    }
  }

  @Override
  public void info(Marker arg0, String arg1, Object arg2) {
    if (isLoggable(Level.INFO, arg0) && isNotLimited(Level.INFO)) {
      log2(arg0, fqcnCmn, Level.INFO, arg1, new Object[] { arg2 }, null);
    }
  }

  @Override
  public void info(Marker arg0, String arg1, Object... arg2) {
    if (isLoggable(Level.INFO, arg0) && isNotLimited(Level.INFO)) {
      log2(arg0, fqcnCmn, Level.INFO, arg1, arg2, null);
    }
  }

  @Override
  public void info(Marker arg0, String arg1, Throwable arg2) {
    if (isLoggable(Level.INFO, arg0) && isNotLimited(Level.INFO)) {
      log2(arg0, fqcnCmn, Level.INFO, arg1, null, arg2);
    }
  }

  @Override
  public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
    if (isLoggable(Level.INFO, arg0) && isNotLimited(Level.INFO)) {
      log2(arg0, fqcnCmn, Level.INFO, arg1, new Object[] { arg2, arg3 }, null);
    }
  }

  // methods from log4j 2 using Java 8 suppliers

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void info(Supplier<String> msgSupplier) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   * @param throwable
   *          the exception to log
   */
  @Override
  public void info(Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, msgSupplier.get(), null, throwable);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void info(Marker marker, String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.INFO, marker) && isNotLimited(Level.INFO)) {
      log2(marker, fqcnCmn, Level.INFO, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  public void info(String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.INFO) && isNotLimited(Level.INFO)) {
      log2(null, fqcnCmn, Level.INFO, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void info(Marker marker, Supplier<String> msgSupplier) {
    if (isLoggable(Level.INFO, marker) && isNotLimited(Level.INFO)) {
      log2(marker, fqcnCmn, Level.INFO, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void info(Marker marker, Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.INFO, marker) && isNotLimited(Level.INFO)) {
      log2(marker, fqcnCmn, Level.INFO, msgSupplier.get(), null, throwable);
    }
  }

  // ---------------------- TRACE

  @Override
  public void trace(String arg0) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, arg0, null, null);
    }
  }

  @Override
  public void trace(String arg0, Object arg1) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, arg0, new Object[] { arg1 }, null);
    }
  }

  @Override
  public void trace(String arg0, Object... arg1) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, arg0, arg1, null);
    }
  }

  @Override
  public void trace(String arg0, Throwable arg1) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, arg0, null, arg1);
    }
  }

  @Override
  public void trace(Marker arg0, String arg1) {
    if (isLoggable(Level.TRACE, arg0) && isNotLimited(Level.TRACE)) {
      log2(arg0, fqcnCmn, Level.TRACE, arg1, null, null);
    }
  }

  @Override
  public void trace(String arg0, Object arg1, Object arg2) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, arg0, new Object[] { arg1, arg2 }, null);
    }
  }

  @Override
  public void trace(Marker arg0, String arg1, Object arg2) {
    if (isLoggable(Level.TRACE, arg0) && isNotLimited(Level.TRACE)) {
      log2(arg0, fqcnCmn, Level.TRACE, arg1, new Object[] { arg2 }, null);
    }
  }

  @Override
  public void trace(Marker arg0, String arg1, Object... arg2) {
    if (isLoggable(Level.TRACE, arg0) && isNotLimited(Level.TRACE)) {
      log2(arg0, fqcnCmn, Level.TRACE, arg1, arg2, null);
    }
  }

  @Override
  public void trace(Marker arg0, String arg1, Throwable arg2) {
    if (isLoggable(Level.TRACE, arg0) && isNotLimited(Level.TRACE)) {
      log2(arg0, fqcnCmn, Level.TRACE, arg1, null, arg2);
    }
  }

  @Override
  public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
    if (isLoggable(Level.TRACE, arg0) && isNotLimited(Level.TRACE)) {
      log2(arg0, fqcnCmn, Level.TRACE, arg1, new Object[] { arg2, arg3 }, null);
    }
  }

  // methods from log4j 2 using Java 8 suppliers

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void trace(Supplier<String> msgSupplier) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   * @param throwable
   *          the exception to log
   */
  @Override
  public void trace(Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, msgSupplier.get(), null, throwable);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void trace(Marker marker, String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.TRACE, marker) && isNotLimited(Level.TRACE)) {
      log2(marker, fqcnCmn, Level.TRACE, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void trace(String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.TRACE) && isNotLimited(Level.TRACE)) {
      log2(null, fqcnCmn, Level.TRACE, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void trace(Marker marker, Supplier<String> msgSupplier) {
    if (isLoggable(Level.TRACE, marker) && isNotLimited(Level.TRACE)) {
      log2(marker, fqcnCmn, Level.TRACE, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void trace(Marker marker, Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.TRACE, marker) && isNotLimited(Level.TRACE)) {
      log2(marker, fqcnCmn, Level.TRACE, msgSupplier.get(), null, throwable);
    }
  }

  // ---------------------- WARN
  @Override
  public void warn(String arg0) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, arg0, null, null);
    }
  }

  @Override
  public void warn(String arg0, Object arg1) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, arg0, new Object[] { arg1 }, null);
    }
  }

  @Override
  public void warn(String arg0, Object... arg1) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, arg0, arg1, null);
    }
  }

  @Override
  public void warn(String arg0, Throwable arg1) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, arg0, null, arg1);
    }
  }

  @Override
  public void warn(Marker arg0, String arg1) {
    if (isLoggable(Level.WARNING, arg0) && isNotLimited(Level.WARNING)) {
      log2(arg0, fqcnCmn, Level.WARNING, arg1, null, null);
    }
  }

  @Override
  public void warn(String arg0, Object arg1, Object arg2) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, arg0, new Object[] { arg1, arg2 }, null);
    }
  }

  @Override
  public void warn(Marker arg0, String arg1, Object arg2) {
    if (isLoggable(Level.WARNING, arg0) && isNotLimited(Level.WARNING)) {
      log2(arg0, fqcnCmn, Level.WARNING, arg1, new Object[] { arg2 }, null);
    }
  }

  @Override
  public void warn(Marker arg0, String arg1, Object... arg2) {
    if (isLoggable(Level.WARNING, arg0) && isNotLimited(Level.WARNING)) {
      log2(arg0, fqcnCmn, Level.WARNING, arg1, arg2, null);
    }
  }

  @Override
  public void warn(Marker arg0, String arg1, Throwable arg2) {
    if (isLoggable(Level.WARNING, arg0) && isNotLimited(Level.WARNING)) {
      log2(arg0, fqcnCmn, Level.WARNING, arg1, null, arg2);
    }
  }

  @Override
  public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
    if (isLoggable(Level.WARNING, arg0) && isNotLimited(Level.WARNING)) {
      log2(arg0, fqcnCmn, Level.WARNING, arg1, new Object[] { arg2, arg3 }, null);
    }
  }

  // methods from log4j 2 using Java 8 suppliers

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void warn(Supplier<String> msgSupplier) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   * @param throwable
   *          the exception to log
   */
  @Override
  public void warn(Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.WARNING) && isNotLimited(Level.WARNING)) {
      log2(null, fqcnCmn, Level.WARNING, msgSupplier.get(), null, throwable);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void warn(Marker marker, String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.WARNING, marker) && isNotLimited(Level.WARNING)) {
      log2(marker, fqcnCmn, Level.WARNING, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param message
   *          the message to log
   * @param paramSuppliers
   *          An array of functions, which when called, produce the desired log message parameters.
   */
  @Override
  public void warn(String message, Supplier<?>... paramSuppliers) {
    if (isLoggable(Level.WARN) && isNotLimited(Level.WARN)) {
      log2(null, fqcnCmn, Level.WARN, message, suppliersToArray(paramSuppliers), null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void warn(Marker marker, Supplier<String> msgSupplier) {
    if (isLoggable(Level.WARNING, marker) && isNotLimited(Level.WARNING)) {
      log2(marker, fqcnCmn, Level.WARNING, msgSupplier.get(), null, null);
    }
  }

  /**
   * @param marker
   *          the marker data specific to this log statement
   * @param msgSupplier
   *          A function, which when called, produces the desired log message
   */
  @Override
  public void warn(Marker marker, Supplier<String> msgSupplier, Throwable throwable) {
    if (isLoggable(Level.WARNING, marker) && isNotLimited(Level.WARNING)) {
      log2(marker, fqcnCmn, Level.WARNING, msgSupplier.get(), null, throwable);
    }
  }

}
