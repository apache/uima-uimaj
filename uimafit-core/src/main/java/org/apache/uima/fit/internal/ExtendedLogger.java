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
package org.apache.uima.fit.internal;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.impl.Logger_common_impl;
import org.slf4j.Marker;

/**
 * INTERNAL API - Wrapper for the UIMA {@link Logger} offering a more convenient API similar to that
 * of the Apache Commons Logging interface {@link org.apache.commons.logging.Log Log} or to that of
 * Log4J's {@code Category} and SLF4J's {@code Logger}, using the names {@code error}, {@code warn},
 * {@code info}, {@code debug} and {@code trace} and mapping these to UIMA logging levels.
 * 
 */
public class ExtendedLogger extends Logger_common_impl {

  private final UimaContext context;

  public ExtendedLogger(final UimaContext aContext) {
    super(null);
    context = aContext;
  }

  @Override
  @Deprecated
  public void log(String aMessage) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        logger.log(aMessage);
      }
    }
  }

  @Override
  @Deprecated
  public void log(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        logger.log(aResourceBundleName, aMessageKey, aArguments);
      }
    }
  }

  @Override
  @Deprecated
  public void logException(Exception aException) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        logger.logException(aException);
      }
    }
  }

  @Override
  @Deprecated
  public void setOutputStream(PrintStream aStream) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().setOutputStream(aStream);
      }
    }
  }

  @Override
  @Deprecated
  public void setOutputStream(OutputStream aStream) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().setOutputStream(aStream);
      }
    }
  }

  @Override
  public void log(Level level, String aMessage) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        logger.log(getClass().getName(), level, aMessage, null);
      }
    }
  }

  @Override
  public void log(Level level, String aMessage, Object param1) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = MessageFormat.format(aMessage, new Object[] { param1 });
        logger.log(getClass().getName(), level, result, null);
      }
    }
  }

  @Override
  public void log(Level level, String aMessage, Object[] params) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = MessageFormat.format(aMessage, params);
        logger.log(getClass().getName(), level, result, null);
      }
    }
  }

  @Override
  public void log(Level level, String aMessage, Throwable thrown) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        logger.log(getClass().getName(), level, aMessage, thrown);
      }
    }
  }

  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = I18nUtil.localizeMessage(bundleName, msgKey, null,
                getExtensionClassLoader());
        logger.log(getClass().getName(), level, result, null);
      }
    }
  }

  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object param1) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = I18nUtil.localizeMessage(bundleName, msgKey, new Object[] { param1 },
                getExtensionClassLoader());
        logger.log(getClass().getName(), level, result, null);
      }
    }
  }

  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Object[] params) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = I18nUtil.localizeMessage(bundleName, msgKey, params,
                getExtensionClassLoader());
        logger.log(getClass().getName(), level, result, null);
      }
    }
  }

  @Override
  public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
          String msgKey, Throwable thrown) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null && logger.isLoggable(level)) {
        String result = I18nUtil.localizeMessage(bundleName, msgKey, null,
                getExtensionClassLoader());
        logger.log(getClass().getName(), level, result, thrown);
      }
    }
  }

  @Override
  public void log(String wrapperFQCN, Level level, String message, Throwable thrown) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(wrapperFQCN, level, message, thrown);
      }
    }
  }

  @Override
  public boolean isLoggable(Level level) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(level);
      }
    }
    return false;
  }

  @Override
  public void setLevel(Level level) {
    context.getLogger().setLevel(level);
  }

  @Override
  public void setResourceManager(ResourceManager resourceManager) {
    context.getLogger().setResourceManager(resourceManager);
  }

  @Override
  public String rb(String bundleName, String msgKey, Object... parameters) {
    return I18nUtil.localizeMessage(bundleName, msgKey, parameters, getExtensionClassLoader());
  }
  
  /**
   * Logs a message at {@link Level#FINE}.
   * 
   * @param paramObject
   *          a message.
   */
  public void debug(Object paramObject) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger()
                .log(getClass().getName(), Level.FINE, String.valueOf(paramObject), null);
      }
    }
  }

  /**
   * Logs a message at {@link Level#FINE}.
   * 
   * @param paramObject
   *          a message.
   * @param paramThrowable
   *          a cause.
   */
  public void debug(Object paramObject, Throwable paramThrowable) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.FINE, String.valueOf(paramObject),
                paramThrowable);
      }
    }
  }

  /**
   * Logs a message at {@link Level#SEVERE}.
   * 
   * @param paramObject
   *          a message.
   */
  public void error(Object paramObject) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.SEVERE, String.valueOf(paramObject),
                null);
      }
    }
  }

  /**
   * Logs a message at {@link Level#SEVERE}.
   * 
   * @param paramObject
   *          a message.
   * @param paramThrowable
   *          a cause.
   */
  public void error(Object paramObject, Throwable paramThrowable) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.SEVERE, String.valueOf(paramObject),
                paramThrowable);
      }
    }
  }

  /**
   * Logs a message at {@link Level#INFO}.
   * 
   * @param paramObject
   *          a message.
   */
  public void info(Object paramObject) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.INFO, String.valueOf(paramObject), null);
      }
    }
  }

  /**
   * Logs a message at {@link Level#INFO}.
   * 
   * @param paramObject
   *          a message.
   * @param paramThrowable
   *          a cause.
   */
  public void info(Object paramObject, Throwable paramThrowable) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.INFO, String.valueOf(paramObject),
                paramThrowable);
      }
    }
  }

  @Override
  public boolean isDebugEnabled() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(Level.FINE);
      }
    }
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(Level.SEVERE);
      }
    }
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(Level.INFO);
      }
    }
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(Level.FINER);
      }
    }
    return false;
  }

  @Override
  public boolean isWarnEnabled() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(Level.WARNING);
      }
    }
    return false;
  }

  /**
   * Logs a message at {@link Level#FINER}.
   * 
   * @param paramObject
   *          a message.
   */
  public void trace(Object paramObject) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.FINER, String.valueOf(paramObject),
                null);
      }
    }
  }

  /**
   * Logs a message at {@link Level#FINER}.
   * 
   * @param paramObject
   *          a message.
   * @param paramThrowable
   *          a cause.
   */
  public void trace(Object paramObject, Throwable paramThrowable) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.FINER, String.valueOf(paramObject),
                paramThrowable);
      }
    }
  }

  /**
   * Logs a message at {@link Level#WARNING}.
   * 
   * @param paramObject
   *          a message.
   */
  public void warn(Object paramObject) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.WARNING, String.valueOf(paramObject),
                null);
      }
    }
  }

  /**
   * Logs a message at {@link Level#WARNING}.
   * 
   * @param paramObject
   *          a message.
   * @param paramThrowable
   *          a cause.
   */
  public void warn(Object paramObject, Throwable paramThrowable) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        context.getLogger().log(getClass().getName(), Level.WARNING, String.valueOf(paramObject),
                paramThrowable);
      }
    }
  }
  
  /**
   * Gets the extension ClassLoader to used to locate the message digests. If this returns null,
   * then message digests will be searched for using this.class.getClassLoader().
   */
  private ClassLoader getExtensionClassLoader() {
    if (context instanceof UimaContextAdmin) {
      ResourceManager resMgr = ((UimaContextAdmin) context).getResourceManager();
      if (resMgr != null) {
        return resMgr.getExtensionClassLoader();
      }
      else {
        return null;
      }
    }
    return null;
  }

  @Override
  public boolean isLoggable(Level level, Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isLoggable(level, marker);
      }
    }
    return false;
  }

  @Override
  public String getName() {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().getName();
      }
    }
    return "";
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isTraceEnabled(marker);
      }
    }
    return false;
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isDebugEnabled(marker);
      }
    }
    return false;
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isInfoEnabled(marker);
      }
    }
    return false;
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isWarnEnabled(marker);
      }
    }
    return false;
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        return context.getLogger().isErrorEnabled(marker);
      }
    }
    return false;
  }

  @Override
  public void log(Marker m, String aFqcn, Level level, String message, Object[] args,
          Throwable thrown) {
    if (context != null) {
      Logger logger = context.getLogger();
      if (logger != null) {
        switch(level.toInteger()) {
          case Level.SEVERE_INT: 
            logger.error(m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.WARNING_INT: 
            logger.warn(m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.INFO_INT: 
            logger.info(m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.CONFIG_INT:
            logger.info((m == null) ? Logger.UIMA_MARKER_CONFIG : m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.FINE_INT: 
            logger.debug(m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.FINER_INT: 
            logger.trace(m, MessageFormat.format(message, args), thrown); 
            break;
          case Level.FINEST_INT:
            logger.trace((m == null) ? Logger.UIMA_MARKER_FINEST : m, MessageFormat.format(message, args), thrown); 
            break;
          default: Misc.internalError();
        } // end of switch
      } // end of if
    } // end of if  
  } // end of method
}
