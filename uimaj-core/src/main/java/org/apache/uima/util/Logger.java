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

package org.apache.uima.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.resource.ResourceManager;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A <code>Logger</code> is a component used to log messages. This interface defines the standard
 * way for UIMA components to produce log output.
 * <p>
 * In the UIMA SDK, this interface is implemented using the Java logger as a back end.
 * <p>The back end may be changed to Apache Log4j 2  by specifying
 * <code>-Dorg.apache.uima.logger.class=org.apache.uima.util.impl.Log4jLogger_impl</code>
 * and including the log4j 2 JARs in the classpath.
 * <p>  
 * If you
 * want to configure the logger, for example to specify the location of the log file and the logging
 * level, you use whatever back end logger implementation specifies, or use that logger's APIs.
 * should use the standard Java logger properties or the java.util.logging APIs. See
 * the section "Specifying the Logging Configuration" in the Annotator and Analysis Engine
 * Developer's Guide chapter of the UIMA documentation for more information.
 * <p>
 * Version 3 augments this API with methods to do UIMA-Resource-bundle-based internationalization, 
 * separate from logging, so the String result can be used with various back end loggers.
 * <p>
 * Version 3 augments isLoggable to include the Marker.
 *
 *  
 */
public interface Logger extends org.slf4j.Logger {

  // standard markers
  static final Marker UIMA_MARKER_CONFIG = MarkerFactory.getMarker("org.apache.uima.config");
  static final Marker UIMA_MARKER_FINEST = MarkerFactory.getMarker("org.apache.uima.finest");

  /**
   * Logs a message.
   * 
   * @deprecated use new function with log level
   * 
   * @param aMessage
   *          the message to be logged with message level INFO
   */
  @Deprecated
  public void log(String aMessage);

  /**
   * Logs an internationalized message.
   * 
   * @deprecated use new function with log level
   * 
   * @param aResourceBundleName
   *          base name of resource bundle
   * @param aMessageKey
   *          key of message to localize with message level INFO
   * @param aArguments
   *          arguments to message (may be null if none)
   */
  @Deprecated
public void log(String aResourceBundleName, String aMessageKey, Object[] aArguments);

  /**
   * Logs an exception
   * 
   * @deprecated use new function with log level
   * 
   * @param aException
   *          the exception to be logged with message level INFO
   */
  @Deprecated
public void logException(Exception aException);

  /**
   * Sets the output stream to which log messages will go. Setting the output stream to
   * <code>null</code> will disable the logger.
   * 
   * @deprecated use external configuration possibility
   * 
   * @param aStream
   *          <code>PrintStream</code> to which log messages will be printed
   */
  @Deprecated
public void setOutputStream(PrintStream aStream);

  /**
   * Sets the output stream to which log messages will go. Setting the output stream to
   * <code>null</code> will disable the logger.
   * 
   * @deprecated use external configuration possibility
   * 
   * @param aStream
   *          <code>OutputStream</code> to which log messages will be printed
   */
  @Deprecated
public void setOutputStream(OutputStream aStream);

  /**
   * Logs a message.
   * 
   * @param level
   *          message level
   * @param aMessage
   *          the message to be logged
   */
  public void log(Level level, String aMessage);

  /**
   * Logs a message with one parameter
   * 
   * @param level
   *          message level
   * @param aMessage
   *          the message to be logged
   * @param param1
   *          message parameter
   */
  public void log(Level level, String aMessage, Object param1);

  /**
   * Logs a message with an arbitrary number of parameters
   * 
   * @param level
   *          message level
   * @param aMessage
   *          the message to be logged
   * @param params
   *          message parameter array
   */
  public void log(Level level, String aMessage, Object[] params);

  /**
   * Logs a message and a throwable object
   * 
   * @param level
   *          message level
   * @param aMessage
   *          the message to be logged
   * @param thrown
   *          throwable object
   */
  public void log(Level level, String aMessage, Throwable thrown);

  /**
   * Logs a message with a message key. The real message is extracted from a resource bundle.
   * 
   * @param level
   *          message level
   * @param sourceClass
   *          source class name
   * @param sourceMethod
   *          source method name
   * @param bundleName
   *          resource bundle
   * @param msgKey
   *          message key
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey);

  /**
   * Logs a message with a message key and one parameter. The real message is extracted from a
   * resource bundle.
   * 
   * @param level
   *          message level
   * @param sourceClass
   *          source class name
   * @param sourceMethod
   *          source method name
   * @param bundleName
   *          resource bundle
   * @param msgKey
   *          message key
   * @param param1
   *          message parameter
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object param1);

  /**
   * Logs a message with a message key and an arbitrary number of parameters. The real message is
   * extracted from a resource bundle.
   * 
   * @param level
   *          message level
   * @param sourceClass
   *          source class name
   * @param sourceMethod
   *          source method name
   * @param bundleName
   *          resource bundle
   * @param msgKey
   *          message key
   * @param params
   *          message parameter array with an arbitrary number of parameters
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object[] params);

  /**
   * Logs a message with a message key and a throwable object. The real message is extracted from a
   * resource bundle.
   * 
   * @param level
   *          message level
   * @param sourceClass
   *          source class name
   * @param sourceMethod
   *          source method name
   * @param bundleName
   *          resource bundle
   * @param msgKey
   *          message key
   * @param thrown
   *          throwable object
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Throwable thrown);

  /**
   * Generic logging method intended for logging wrappers.
   * 
   * @param wrapperFQCN
   *          fully qualified class name of the wrapper
   * @param level
   *          message level
   * @param message
   *          message
   * @param thrown
   *          throwable object
   */
  public void log(String wrapperFQCN, Level level, String message, Throwable thrown);
  
  /**
   * Checks if the argument level is greater or equal to the specified level
   * 
   * @param level
   *          message level
   * 
   * @return boolean - true if the argument level is greater or equal to the specified level
   */
  public boolean isLoggable(Level level);
  
  /**
   * Checks if this logger is enabled for this level and this marker
   * @param level the level to test
   * @param marker null or the marker to test
   * @return true if the level is greater or equal to the specified level and the marker matches
   */
  public boolean isLoggable(Level level, Marker marker);

  /**
   * Sets the level of messages that will be logged by this logger. Note that if you call
   * <code>UIMAFramework.getLogger().setLevel(level)</code>, this will only change the logging
   * level for messages produced by the UIMA framework. It will NOT change the logging level for
   * messages produced by annotators. To change the logging level for an annotator, use
   * <code>UIMAFramework.getLogger(YourAnnotatorClass.class).setLevel(level)</code>.
   * <p>
   * If you need more flexibility it configuring the logger, consider using the standard Java logger
   * properties file or the java.util.logging APIs.
   * 
   * @param level
   *          message level
   */
  public void setLevel(Level level);

  /**
   * Sets the ResourceManager to use for message localization. This method is intended for use by
   * the framework, not by user code.
   * 
   * @param resourceManager
   *          A resource manager instance whose extension ClassLoader (if any) will be used for
   *          message localization by this logger.
   */
  public void setResourceManager(ResourceManager resourceManager);

  /**
   * Get an internationalized message from a resource bundle by key name, substituting the parameters.
   * This should be called via a Supplier to avoid computing this until needed
   * @param resourceBundle -
   * @param key -
   * @param params -
   * @return the internationalized message
   */
  public String rb(String resourceBundle, String key, Object... params);
  
  default String rb_ue(String key, Object... params) {
    return rb(UIMAException.STANDARD_MESSAGE_CATALOG, key, params);
  }
  
  /**
   * This is true if the name of the logger corresponds to a class which implements
   * AnalysisComponent_ImplBase, which includes basic Annotators, plus Cas Multipliers
   * and CPP components.
   * @return true if this logger is an Annotator logger.
   */
  public boolean isAnnotatorLogger();

  /**
   * @param limit the limit
   * @return a copy of the logger with the throttling limit set, or the same logger if no change
   */
  default Logger getLimitedLogger(int limit) {
    return this;
  }
  
  /* added Supplier APIs */
  
  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  void debug(Supplier<String> msgSupplier);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void debug(Supplier<String> msgSupplier, Throwable throwable);
  
  /**
   * @param marker the marker data specific to this log statement
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void debug(Marker marker, String message, Supplier<?>... paramSuppliers);

  /**
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void debug(String message, Supplier<?>... paramSuppliers);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  public void debug(Marker marker, Supplier<String> msgSupplier);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void debug(Marker marker, Supplier<String> msgSupplier, Throwable throwable);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  void error(Supplier<String> msgSupplier);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void error(Supplier<String> msgSupplier, Throwable throwable);
  
  /**
   * @param marker the marker data specific to this log statement
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void error(Marker marker, String message, Supplier<?>... paramSuppliers);

  /**
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void error(String message, Supplier<?>... paramSuppliers);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  public void error(Marker marker, Supplier<String> msgSupplier);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void error(Marker marker, Supplier<String> msgSupplier, Throwable throwable);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  void info(Supplier<String> msgSupplier);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void info(Supplier<String> msgSupplier, Throwable throwable);
  
  /**
   * @param marker the marker data specific to this log statement
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void info(Marker marker, String message, Supplier<?>... paramSuppliers);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  public void info(Marker marker, Supplier<String> msgSupplier);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void info(Marker marker, Supplier<String> msgSupplier, Throwable throwable);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  void trace(Supplier<String> msgSupplier);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void trace(Supplier<String> msgSupplier, Throwable throwable);
  
  /**
   * @param marker the marker data specific to this log statement
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void trace(Marker marker, String message, Supplier<?>... paramSuppliers);

  /**
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void trace(String message, Supplier<?>... paramSuppliers);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  public void trace(Marker marker, Supplier<String> msgSupplier);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void trace(Marker marker, Supplier<String> msgSupplier, Throwable throwable);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  void warn(Supplier<String> msgSupplier);

  /**
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void warn(Supplier<String> msgSupplier, Throwable throwable);
  
  /**
   * @param marker the marker data specific to this log statement
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void warn(Marker marker, String message, Supplier<?>... paramSuppliers);

  /**
   * @param message the message to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
   */
  public void warn(String message, Supplier<?>... paramSuppliers);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   */
  public void warn(Marker marker, Supplier<String> msgSupplier);

  /**
   * @param marker the marker data specific to this log statement
   * @param msgSupplier A function, which when called, produces the desired log message
   * @param throwable the exception to log
   */
  public void warn(Marker marker, Supplier<String> msgSupplier, Throwable throwable);

}
