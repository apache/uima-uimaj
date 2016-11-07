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

package org.apache.uima;

import java.util.Locale;

import org.apache.uima.internal.util.I18nUtil;

/**
 * Like InternationalizedException, but is an interface with default methods.
 * 
 * This common set of default implementations are intended to be added to 
 * sets of messages collected into exception classes, which implement this,
 * and separately extend one of the 3 superclasses:
 *   - Exception (for checked exceptions)
 *   - RuntimeException (for unchecked exceptions)
 *   - SaxException (for exceptions within XML parsing code
 *   
 */
public interface I18nExceptionI {
  
    String getResourceBundleName();   // like getMessageCatalog?
    String getMessageKey();
    Object[] getArguments();
    Throwable getCause();
    
    /** 
     * Due to the fact that superclass definitions override any
     * default methods, these next must be in the class definitions
     * as they override other supertype methods.
     * @return -
     */
    String getMessage();
    String getLocalizedMessage();

//    /**
//     * @return The message of the exception. Useful for including the text in another exception.
//     */
//    default String getMessage() {
//      return getLocalizedMessage(Locale.ENGLISH);
//    }
//    
//    /**
//     * Gets the localized detail message for this exception. This uses the
//     * default Locale for this JVM. A Locale may be specified using
//     * {@link #getLocalizedMessage(Locale)}.
//     * 
//     * @return this exception's detail message, localized for the default Locale.
//     */
//    default String getLocalizedMessage() {
//      return getLocalizedMessage(Locale.getDefault());
//    }
    
    /**
     * Gets the localized detail message for this exception using the specified
     * <code>Locale</code>.
     * 
     * @param aLocale
     *           the locale to use for localizing the message
     * 
     * @return this exception's detail message, localized for the specified
     *         <code>Locale</code>.
     */
    default String getLocalizedMessage(Locale aLocale) {
       // check for null message
       if (getMessageKey() == null)
          return null;

       return I18nUtil.localizeMessage(getResourceBundleName(), aLocale, getMessageKey(), getArguments());
    }
    
    /**
     * Checks if this exception, or any of its root causes, has a particular UIMA
     * message key. This allows checking for particular error condition in test cases
     * 
     * @param messageKey
     *           to search for in the exception chain
     * @return true if this exception or any of its root causes has a particular UIMA message key.
     */
    default boolean hasMessageKey(String messageKey) {
       if (messageKey.equals(this.getMessageKey())) {
          return true;
       }
       Throwable cause = getCause();
       if (cause != null && cause instanceof I18nExceptionI) {
          return ((I18nExceptionI) cause).hasMessageKey(messageKey);
       }
       return false;
    }
}
