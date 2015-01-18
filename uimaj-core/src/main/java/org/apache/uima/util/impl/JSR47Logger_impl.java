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

import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.UIMALogFormatter;
import org.apache.uima.internal.util.UIMAStreamHandler;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * UIMA Logging interface implementation for Java Logging Toolkit JSR-47 (JDK 1.4)
 * 
 */
public class JSR47Logger_impl implements Logger {
  private static final String EXCEPTION_MESSAGE = "Exception occurred";

  /**
   * logger object from the underlying JSR-47 logging framework
   */
  private java.util.logging.Logger logger = null;

  /**
   * ResourceManager whose extension ClassLoader will be used to locate the message digests. Null
   * will cause the ClassLoader to default to this.class.getClassLoader().
   */
  private ResourceManager mResourceManager = null;

  /**
   * create a new LogWrapper class for the specified source class
   * 
   * @param component
   *          specified source class
   */
  private JSR47Logger_impl(Class<?> component) {
    super();

    if (component != null) {
      // create new JSR47 logger for this LogWrapper object
      logger = java.util.logging.Logger.getLogger(component.getName());
    } else // if class not set, return "org.apache.uima" logger
    {
      logger = java.util.logging.Logger.getLogger("org.apache.uima");
    }
  }

  /**
   * create a new LogWrapper object with the default logger from the JSR-47 logging framework
   */
  private JSR47Logger_impl() {
    this(null);
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

  /**
   * Creates a new JSR47Logger instance with the default JSR-47 framework logger
   * 
   * @return Logger returns the JSR47Logger object with the default JSR-47 framework logger
   */
  public static synchronized Logger getInstance() {
    return new JSR47Logger_impl();
  }

  /**
   * Logs a message with level INFO.
   * 
   * @deprecated use new function with log level
   * 
   * @param aMessage
   *          the message to be logged
   */
  @Deprecated
  public void log(String aMessage) {
    if (isLoggable(Level.INFO)) {
      if (aMessage == null || aMessage.equals(""))
        return;

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      logger.logp(java.util.logging.Level.INFO, sourceInfo[0], sourceInfo[1], aMessage);
    }
  }

  /**
   * Logs a message with a message key and the level INFO
   * 
   * @deprecated use new function with log level
   * 
   * @see org.apache.uima.util.Logger#log(java.lang.String, java.lang.String, java.lang.Object[])
   */
  @Deprecated
  public void log(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    if (isLoggable(Level.INFO)) {
      if (aMessageKey == null || aMessageKey.equals(""))
        return;

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      logger.logp(java.util.logging.Level.INFO, sourceInfo[0], sourceInfo[1], I18nUtil
              .localizeMessage(aResourceBundleName, aMessageKey, aArguments,
                      getExtensionClassLoader()));
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
  @Deprecated
  public void logException(Exception aException) {
    if (isLoggable(Level.INFO)) {
      if (aException == null)
        return;

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      // log exception
      logger.logp(java.util.logging.Level.INFO, sourceInfo[0], sourceInfo[1], EXCEPTION_MESSAGE,
              aException);
    }
  }

  /**
   * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
   * 
   * @deprecated use external configuration possibility
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
   */
  public boolean isLoggable(Level level) {
    // get corresponding JSR-47 level
    java.util.logging.Level jsr47Level = getJSR47Level(level);

    return logger.isLoggable(jsr47Level);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
   */
  public void setLevel(Level level) {
    // get corresponding JSR-47 level
    java.util.logging.Level jsr47Level = getJSR47Level(level);

    logger.setLevel(jsr47Level);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level, java.lang.String)
   */
  public void log(Level level, String aMessage) {
    if (isLoggable(level)) {
      if (aMessage == null || aMessage.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], aMessage);
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
      if (aMessage == null || aMessage.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], MessageFormat.format(aMessage,
              new Object[] { param1 }));
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
      if (aMessage == null || aMessage.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      String[] sourceInfo = getStackTraceInfo(null, new Throwable());

      logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], MessageFormat.format(aMessage, params));
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
      if (aMessage != null && !aMessage.equals("")) {
        // get corresponding JSR-47 level
        java.util.logging.Level jsr47Level = getJSR47Level(level);

        String[] sourceInfo = getStackTraceInfo(null, new Throwable());

        logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], aMessage, thrown);
      }

      if (thrown != null && (aMessage == null || aMessage.equals(""))) {
        // get corresponding JSR-47 level
        java.util.logging.Level jsr47Level = getJSR47Level(level);

        String[] sourceInfo = getStackTraceInfo(null, new Throwable());

        // log exception
        logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], EXCEPTION_MESSAGE, thrown);
      }
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
      if (msgKey == null || msgKey.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      logger.logp(jsr47Level, sourceClass, sourceMethod, I18nUtil.localizeMessage(bundleName,
              msgKey, new Object[] { param1 }, getExtensionClassLoader()));
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
      if (msgKey == null || msgKey.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      logger.logp(jsr47Level, sourceClass, sourceMethod, I18nUtil.localizeMessage(bundleName,
              msgKey, params, getExtensionClassLoader()));
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
      if (msgKey != null && !msgKey.equals("")) {
        // get corresponding JSR-47 level
        java.util.logging.Level jsr47Level = getJSR47Level(level);

        logger.logp(jsr47Level, sourceClass, sourceMethod, I18nUtil.localizeMessage(bundleName,
                msgKey, null, getExtensionClassLoader()), thrown);
      }

      if (thrown != null && (msgKey == null || msgKey.equals(""))) {
        // get corresponding JSR-47 level
        java.util.logging.Level jsr47Level = getJSR47Level(level);

        // log exception
        logger.logp(jsr47Level, sourceClass, sourceMethod, EXCEPTION_MESSAGE, thrown);
      }
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
      if (msgKey == null || msgKey.equals(""))
        return;

      // get corresponding JSR-47 level
      java.util.logging.Level jsr47Level = getJSR47Level(level);

      logger.logp(jsr47Level, sourceClass, sourceMethod, I18nUtil.localizeMessage(bundleName,
              msgKey, null, getExtensionClassLoader()));
    }
  }

  public void log(String wrapperFQCN, Level level, String message, Throwable thrown) {
    // get corresponding JSR-47 level
    java.util.logging.Level jsr47Level = getJSR47Level(level);
    String[] sourceInfo = getStackTraceInfo(wrapperFQCN, new Throwable());

    // log exception
    logger.logp(jsr47Level, sourceInfo[0], sourceInfo[1], message, thrown);
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

  /**
   * JSR-47 level mapping to UIMA level mapping.
   * 
   * SEVERE (highest value) -%gt; SEVERE WARNING -%gt; WARNING INFO -%gt; INFO CONFIG -%gt; CONFIG FINE -%gt; FINE
   * FINER -%gt; FINER FINEST (lowest value) -%gt; FINEST OFF -%gt; OFF ALL -%gt; ALL
   * 
   * @param level
   *          uima level
   * 
   * @return Level - corresponding JSR47 level
   */
  private java.util.logging.Level getJSR47Level(Level level) {
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
        return java.util.logging.Level.INFO;
      case org.apache.uima.util.Level.CONFIG_INT:
        return java.util.logging.Level.CONFIG;
      case org.apache.uima.util.Level.FINE_INT:
        return java.util.logging.Level.FINE;
      case org.apache.uima.util.Level.FINER_INT:
        return java.util.logging.Level.FINER;
      case org.apache.uima.util.Level.FINEST_INT:
        return java.util.logging.Level.FINEST;
      default: // for all other cases return Level.ALL
        return java.util.logging.Level.ALL;
    }
  }

  /**
   * returns the method name and the line number if available
   * 
   * @param thrown
   *          the thrown
   * 
   * @return String[] - fist element is the source class, second element is the method name with
   *         linenumber if available
   */
  private String[] getStackTraceInfo(String wrapperFQCN, Throwable thrown) {
    StackTraceElement[] stackTraceElement = thrown.getStackTrace();

    String sourceMethod = "";
    String sourceClass = "";
    int lineNumber = 0;
    
    try {
      int index = 0;
      if (wrapperFQCN != null) {
        boolean found = false;
        while (index < stackTraceElement.length) {
          if (wrapperFQCN.equals(stackTraceElement[index].getClassName())) {
            found = true;
            break;
          }
          index++;
        }
        if (!found) {
          index = 0;
        }
      }
      index++;
      
      lineNumber = stackTraceElement[index].getLineNumber();
      sourceMethod = stackTraceElement[index].getMethodName();
      sourceClass = stackTraceElement[index].getClassName();
    } catch (Exception ex) {
      // do nothing, use the initialized string members
    }

    if (lineNumber > 0) {
      StringBuffer buffer = new StringBuffer(25);
      buffer.append(sourceMethod);
      buffer.append('(');
      buffer.append(lineNumber);
      buffer.append(')');
      sourceMethod = buffer.toString();
    }

    return new String[] { sourceClass, sourceMethod };
  }
}
