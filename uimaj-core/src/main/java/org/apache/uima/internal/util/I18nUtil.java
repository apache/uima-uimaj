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

package org.apache.uima.internal.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internationaliation utilities.
 * 
 */
public class I18nUtil {
  /**
   * Localize a message to the default Locale.
   * 
   * @param aResourceBundleName
   *          base name of resource bundle
   * @param aMessageKey
   *          key of message to localize
   * @param aArguments
   *          arguments to message (may be null if none)
   * 
   * @return localized message. If an exception occurs, returns "MESSAGE LOCALIZATION FAILED:"
   *         followed by the exception message.
   */
  public static String localizeMessage(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
    return localizeMessage(aResourceBundleName, Locale.getDefault(), aMessageKey, aArguments, null);
  }

  /**
   * Localize a message to the default Locale.
   * 
   * @param aResourceBundleName
   *          base name of resource bundle
   * @param aMessageKey
   *          key of message to localize
   * @param aArguments
   *          arguments to message (may be null if none)
   * @param aLoader
   *          ClassLoader to use to load the resource bundle. If null, the ClassLoader that loased
   *          <code>I18nUtil</code> is used.
   * 
   * @return localized message. If an exception occurs, returns "MESSAGE LOCALIZATION FAILED:"
   *         followed by the exception message.
   */
  public static String localizeMessage(String aResourceBundleName, String aMessageKey,
          Object[] aArguments, ClassLoader aLoader) {
    return localizeMessage(aResourceBundleName, Locale.getDefault(), aMessageKey, aArguments,
            aLoader);
  }

  /**
   * Localize a message to a specified Locale.
   * 
   * @param aResourceBundleName
   *          base name of resource bundle
   * @param aLocale
   *          locale to which to localize
   * @param aMessageKey
   *          key of message to localize
   * @param aArguments
   *          arguments to message (may be null if none)
   * 
   * @return localized message. If an exception occurs, returns "MESSAGE LOCALIZATION FAILED:"
   *         followed by the exception message.
   */
  public static String localizeMessage(String aResourceBundleName, Locale aLocale,
          String aMessageKey, Object[] aArguments) {
    return localizeMessage(aResourceBundleName, aLocale, aMessageKey, aArguments, null);
  }

  /**
   * Localize a message to a specified Locale.
   * 
   * @param aResourceBundleName
   *          base name of resource bundle
   * @param aLocale
   *          locale to which to localize
   * @param aMessageKey
   *          key of message to localize
   * @param aArguments
   *          arguments to message (may be null if none)
   * @param aLoader
   *          ClassLoader to use to load the resource bundle. If null, the ClassLoader that loased
   *          <code>I18nUtil</code> is used.
   * 
   * @return localized message. If an exception occurs, returns "MESSAGE LOCALIZATION FAILED:"
   *         followed by the exception message.
   */
  public static String localizeMessage(String aResourceBundleName, Locale aLocale,
          String aMessageKey, Object[] aArguments, ClassLoader aLoader) {
    try {
      if (aLoader == null) {
        aLoader = MsgLocalizationClassLoader.getMsgLocalizationClassLoader();        
      }
      // locate the resource bundle for this exception's messages
      ResourceBundle bundle =  ResourceBundle.getBundle(aResourceBundleName, aLocale, aLoader);
      String message = bundle.getString(aMessageKey);
      // if arguments exist, use MessageFormat to include them
      if (aArguments != null && aArguments.length > 0) {
        MessageFormat fmt = new MessageFormat(message);
        fmt.setLocale(aLocale);
        return fmt.format(aArguments);
      } else
        return message;
    } catch (Exception e) {
      return "MESSAGE LOCALIZATION FAILED: " + e.getMessage();
    }
  }

  public static void setTccl(ClassLoader tccl) {
    MsgLocalizationClassLoader.CallClimbingClassLoader.originalTccl.set(tccl);
  }
  
  public static void removeTccl() {
    MsgLocalizationClassLoader.CallClimbingClassLoader.originalTccl.remove();
  }
    
}
