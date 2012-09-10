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

import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * UIMA Logging interface implementation without using an logging toolkit
 * 
 */
public class Logger_impl implements Logger {
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

  /**
   * default logger instance
   */
  private static final Logger_impl defaultLogger = new Logger_impl();

  /**
   * ResourceManager whose extension ClassLoader will be used to locate the message digests. Null
   * will cause the ClassLoader to default to this.class.getClassLoader().
   */
  private ResourceManager mResourceManager = null;

  /**
   * creates a new Logger object and set <code>System.out</code> as default output
   */
  private Logger_impl() {
    // set default Output
    mOut = defaultOut;
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
    return new Logger_impl();
  }

  /**
   * creates a new Logger instance for the specified source class
   * 
   * @return Logger - returns a new Logger object
   */
  public static synchronized Logger getInstance() {
    return defaultLogger;
  }

  /**
   * Logs a message with message level INFO
   * 
   * @deprecated use method with log level as parameter
   * 
   * @param aMessage
   *          the message to be logged
   */
  @Deprecated
  public void log(String aMessage) {
    if (isLoggable(Level.INFO) && mOut != null) {
      mOut.print(new Date());
      mOut.print(": " + Level.INFO.toString() + ": ");
      mOut.println(aMessage);
    }
  }

  /**
   * Logs a message with a message key and with the message level INFO
   * 
   * @deprecated use method with log level as parameter
   * 
   * @see org.apache.uima.util.Logger#log(java.lang.String, java.lang.String, java.lang.Object[])
   */
  @Deprecated
  public void log(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    if (isLoggable(Level.INFO)) {
      log(I18nUtil.localizeMessage(aResourceBundleName, aMessageKey, aArguments,
              getExtensionClassLoader()));
    }
  }

  /**
   * Logs an exception with message level INFO
   * 
   * @deprecated use method with log level as parameter
   * 
   * @param aException
   *          the exception to be logged
   */
  @Deprecated
  public void logException(Exception aException) {
    if (isLoggable(Level.INFO) && mOut != null) {
      mOut.print(new Date());
      mOut.print(": " + Level.INFO.toString() + ": ");
      aException.printStackTrace(mOut);
    }
  }

  /**
   * @deprecated use external configuration possibility
   * 
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
   */
  @Deprecated
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
  @Deprecated
  public void setOutputStream(PrintStream out) {
    mOut = out;
  }

  /**
   * Logs an exception.
   * 
   * @param level
   *          message level
   * @param thrown
   *          the throwable
   */
  private void logException(Level level, Throwable thrown) {
    mOut.print(new Date());
    mOut.print(": " + level.toString() + ": ");
    thrown.printStackTrace(mOut);
  }

  /**
   * Logs a message.
   * 
   * @param level
   *          message level
   * @param aMessage
   *          the message
   */
  private void logMessage(Level level, String aMessage) {
    if (mOut != null) {
      mOut.print(new Date());
      mOut.print(": " + level.toString() + ": ");
      mOut.println(aMessage);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  public boolean isLoggable(Level level) {
    return configLevel.isGreaterOrEqual(level);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String)
   */
  public void log(Level level, String aMessage) {
    if (isLoggable(level)) {
      logMessage(level, aMessage);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.Object)
   */
  public void log(Level level, String aMessage, Object param1) {
    if (isLoggable(level)) {
      String result = MessageFormat.format(aMessage, new Object[] { param1 });

      logMessage(level, result);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.Object[])
   */
  public void log(Level level, String aMessage, Object[] params) {
    if (isLoggable(level)) {
      String result = MessageFormat.format(aMessage, params);

      logMessage(level, result);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.Throwable)
   */
  public void log(Level level, String aMessage, Throwable thrown) {
    if (isLoggable(level)) {
      logMessage(level, aMessage);

      logException(level, thrown);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object param1) {
    if (isLoggable(level)) {
      logMessage(level, I18nUtil.localizeMessage(bundleName, msgKey, new Object[] { param1 },
              getExtensionClassLoader()));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object[] params) {
    if (isLoggable(level)) {
      logMessage(level, I18nUtil.localizeMessage(bundleName, msgKey, params,
              getExtensionClassLoader()));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable)
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Throwable thrown) {
    if (isLoggable(level)) {
      logMessage(level, I18nUtil.localizeMessage(bundleName, msgKey, null,
              getExtensionClassLoader()));

      logException(level, thrown);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey) {
    if (isLoggable(level)) {
      logMessage(level, I18nUtil.localizeMessage(bundleName, msgKey, null,
              getExtensionClassLoader()));
    }
  }
  
  public void log(String wrapperFQCN, Level level, String message, Throwable thrown) {
    if (isLoggable(level)) {
      logMessage(level, message);

      if (thrown != null) {
        logException(level, thrown);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
   */
  public void setLevel(Level level) {
    // set new config level
    configLevel = level;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setResourceManager(org.apache.uima.resource.ResourceManager)
   */
  public void setResourceManager(ResourceManager resourceManager) {
    mResourceManager = resourceManager;
  }

  /**
   * Gets the extension ClassLoader to used to locate the message digests. If this returns null,
   * then message digests will be searched for using this.class.getClassLoader().
   */
  private ClassLoader getExtensionClassLoader() {
    if (mResourceManager == null)
      return null;
    else
      return mResourceManager.getExtensionClassLoader();
  }

}
