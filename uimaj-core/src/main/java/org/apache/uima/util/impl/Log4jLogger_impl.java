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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * UIMA Logging interface implementation for Log4j
 * 
 * This version is for Log4j version 2, from Apache
 * Built using version 2.8
 * 
 */
public class Log4jLogger_impl implements Logger {

  private static final String SOURCE_CLASS = "source_class";
  
  private static final String SOURCE_METHOD = "source_method";
  
  private static final String EXCEPTION_MESSAGE = "Exception occurred";

  static final String FQCN = Log4jLogger_impl.class.getName();
   /**
    * logger object from the underlying Log4j logging framework
    * The ExtendedLoggerWrapper includes the ability to specify the wrapper class
    */
  final private ExtendedLoggerWrapper                logger;

  final private org.apache.logging.log4j.core.Logger coreLogger;

  final private MessageFactory                       mf;

  /**
   * ResourceManager whose extension ClassLoader will be used to locate the
   * message digests. Null will cause the ClassLoader to default to
   * this.class.getClassLoader().
   */
  private ResourceManager mResourceManager = null;

   /**
    * create a new LogWrapper class for the specified source class
    * 
    * @param component
    *           specified source class
    */
   private Log4jLogger_impl(Class<?> component) {
      super();

      coreLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger( (null == component) 
                                       ?  "org.apache.uima"
                                       : component.getClass().getName());
      mf = coreLogger.getMessageFactory();
      
      logger = new ExtendedLoggerWrapper((AbstractLogger) coreLogger, coreLogger.getName(), mf);
   }

   /**
    * create a new LogWrapper object with the default logger from the Log4j
    * logging framework
    */
   private Log4jLogger_impl() {
      this(null);
   }

   /**
    * Creates a new Log4jLogger instance for the specified source class
    * 
    * @param component
    *           current source class
    * @return Logger returns the JSR47Logger object for the specified class
    */
   public static synchronized Logger getInstance(Class<?> component) {
      return new Log4jLogger_impl(component);
   }

   /**
    * Creates a new Log4jLogger instance with the default Log4j framework logger
    * 
    * @return Logger returns the JSR47Logger object with the default Log4j
    *         framework logger
    */
   public static synchronized Logger getInstance() {
      return new Log4jLogger_impl();
   }

   private boolean empty(String v) {
     return (v == null || v.equals(""));
   }
   /**
    * Convert bundle + message key + parameters to message
    * @param bundleName -
    * @param msgKey -
    * @param params -
    * @return the message
    */
   public String rb(String bundleName, String msgKey, Object... params) {
     return I18nUtil.localizeMessage(bundleName, msgKey, params, getExtensionClassLoader());
   }
   
   private void logMsg(Level level, Message m, Throwable th) {
     logger.logMessage(FQCN, getLog4jLevel(level), null, m, th);
   }
   
   private void logMsg(Level level, String m, Throwable th) {
     logMsg(level, mf.newMessage(m), th);
   }
   
   private void ssc(String sourceClass, String sourceMethod) {
     final Map<String, String> ctx = ThreadContext.getContext();
     
     ctx.put(SOURCE_CLASS, Misc.null2str(sourceClass));
     ctx.put(SOURCE_METHOD,  sourceMethod);
   }
   
   /**
    * Logs a message with level INFO.
    * Uses this logger, not one associated with the calling class (if any).
    * 
    * @deprecated use new function with log level
    * @param aMessage
    *           the message to be logged
    */
   @Deprecated
  public void log(String aMessage) {
      if (isLoggable(Level.INFO)) {
         if (empty(aMessage)) return;

         logMsg(Level.INFO, aMessage, null);
      }
   }

   /**
    * Logs a message with a message key and the level INFO
    * 
    * @deprecated use new function with log level
    * @see org.apache.uima.util.Logger#log(java.lang.String, java.lang.String,
    *      java.lang.Object[])
    */
   @Deprecated
   public void log(String aResourceBundleName, String aMessageKey,
         Object[] aArguments) {
      if (isLoggable(Level.INFO)) {
         if (empty(aMessageKey)) return;
         
         logMsg(Level.INFO, rb(aResourceBundleName, aMessageKey, aArguments), null);
      }
   }

   /**
    * Logs an exception with level INFO
    * 
    * @deprecated use new function with log level
    * @param aException
    *           the exception to be logged
    */
   @Deprecated
   public void logException(Exception aException) {
      if (isLoggable(Level.INFO)) {
         if (aException == null)
            return;
         logMsg(Level.INFO, EXCEPTION_MESSAGE, aException);
      }
   }

   /**
    * @see org.apache.uima.util.Logger#setOutputStream(java.io.OutputStream)
    * @deprecated use external configuration possibility
    */
   @Deprecated
   public void setOutputStream(OutputStream out) {
      throw new UnsupportedOperationException(
            "Method setOutputStream(OutputStream out) not supported");
   }

   /**
    * @see org.apache.uima.util.Logger#setOutputStream(java.io.PrintStream)
    * @deprecated use external configuration possibility
    */
   @Deprecated
  public void setOutputStream(PrintStream out) {
      throw new UnsupportedOperationException(
            "Method setOutputStream(PrintStream out) not supported");
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#isLoggable(org.apache.uima.util.Level)
    */
   public boolean isLoggable(Level level) {
     return logger.isEnabled(getLog4jLevel(level));
//      org.apache.logging.log4j.Level log4jLevel = getLog4jLevel(level);

//      return logger.isEnabled(log4jLevel);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#setLevel(org.apache.uima.util.Level)
    */
   public void setLevel(Level level) {
     
     coreLogger.get().setLevel(getLog4jLevel(level));
     coreLogger.getContext().updateLoggers();
//     Configurator.setLevel(null, getLog4jLevel(level));
//      // get corresponding Log4j level
//      org.apache.logging.log4j.Level log4jLevel = getLog4jLevel(level);
//
//      logger.setLevel(log4jLevel);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level,
    *      java.lang.String)
    */
   public void log(Level level, String aMessage) {
      if (isLoggable(level)) {
         if (empty(aMessage)) return;

         logMsg(level, aMessage, null);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.Object)
    */
   public void log(Level level, String aMessage, Object param1) {
     log(level, aMessage, new Object[] {param1});
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.Object[])
    */
   public void log(Level level, String aMessage, Object[] params) {
      if (isLoggable(level)) {
         if (empty(aMessage)) return;
         
         logMsg(level,MessageFormat.format(aMessage, params), null);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#log(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.Throwable)
    */
   public void log(Level level, String aMessage, Throwable thrown) {
      if (isLoggable(level)) {

        logMsg(level, aMessage, thrown);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.String, java.lang.String,
    *      java.lang.String, java.lang.Object)
    */
   public void logrb(Level level, String sourceClass, String sourceMethod,
         String bundleName, String msgKey, Object param1) {
      if (isLoggable(level)) {
         if (empty(msgKey)) return;
         
         ssc(sourceClass, sourceMethod);
         logMsg(level, rb(bundleName, msgKey, param1), null);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.String, java.lang.String,
    *      java.lang.String, java.lang.Object[])
    */
   public void logrb(Level level, String sourceClass, String sourceMethod,
         String bundleName, String msgKey, Object[] params) {
      if (isLoggable(level)) {
         if (empty(msgKey)) return;
         
         ssc(sourceClass, sourceMethod);
         logMsg(level, rb(bundleName, msgKey, params), null);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.String, java.lang.String,
    *      java.lang.String, java.lang.Throwable)
    */
   public void logrb(Level level, String sourceClass, String sourceMethod,
         String bundleName, String msgKey, Throwable thrown) {
      if (isLoggable(level)) {
        if (empty(msgKey) && null == thrown) return;
        
        String msg = empty(msgKey) 
                       ? EXCEPTION_MESSAGE
                       : rb(bundleName, msgKey);
        ssc(sourceClass, sourceMethod);
        logMsg(level, msg, thrown);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.util.Logger#logrb(org.apache.uima.util.Level,
    *      java.lang.String, java.lang.String, java.lang.String,
    *      java.lang.String)
    */
   public void logrb(Level level, String sourceClass, String sourceMethod,
         String bundleName, String msgKey) {
      if (isLoggable(level)) {

         if (empty(msgKey)) return;

         ssc(sourceClass, sourceMethod);
         logMsg(level, rb(bundleName, msgKey), null);
      }
   }

   public void log(String wrapperFQCN, Level level, String message, Throwable thrown) {
     logger.logMessage(wrapperFQCN, getLog4jLevel(level), null, mf.newMessage(message), thrown);
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
    * Gets the extension ClassLoader to used to locate the message digests. If
    * this returns null, then message digests will be searched for using
    * this.class.getClassLoader().
    */
   private ClassLoader getExtensionClassLoader() {
      if (mResourceManager == null)
         return null;
      else
         return mResourceManager.getExtensionClassLoader();
   }

   /**
    * log4j level mapping to UIMA level mapping. SEVERE (highest value) -&gt;
    * SEVERE WARNING -&gt; WARNING INFO -&gt; INFO CONFIG -&gt; CONFIG FINE -&gt; FINE FINER -&gt;
    * FINER FINEST (lowest value) -&gt; FINEST OFF -&gt; OFF ALL -&gt; ALL
    * 
    * @param level
    *           uima level
    * @return Level - corresponding JSR47 level
    */
   private org.apache.logging.log4j.Level getLog4jLevel(Level level) {
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
         return org.apache.logging.log4j.Level.ALL;
      case org.apache.uima.util.Level.FINEST_INT:
         return org.apache.logging.log4j.Level.ALL;
      default: // for all other cases return Level.ALL
         return org.apache.logging.log4j.Level.ALL;
      }
   }

//   /**
//    * returns the method name and the line number if available
//    * 
//    * @param thrown
//    *           the thrown
//    * @return String[] - fist element is the source class, second element is the
//    *         method name with linenumber if available
//    */
//   private String[] getStackTraceInfo(Throwable thrown) {
//      StackTraceElement[] stackTraceElement = thrown.getStackTrace();
//
//      String sourceMethod = "";
//      String sourceClass = "";
//      int lineNumber = 0;
//      try {
//         lineNumber = stackTraceElement[1].getLineNumber();
//         sourceMethod = stackTraceElement[1].getMethodName();
//         sourceClass = stackTraceElement[1].getClassName();
//      } catch (Exception ex) {
//         // do nothing, use the initialized string members
//      }
//
//      if (lineNumber > 0) {
//         StringBuffer buffer = new StringBuffer(25);
//         buffer.append(sourceMethod);
//         buffer.append('(');
//         buffer.append(lineNumber);
//         buffer.append(')');
//         sourceMethod = buffer.toString();
//      }
//
//      return new String[] { sourceClass, sourceMethod };
//   }
}
