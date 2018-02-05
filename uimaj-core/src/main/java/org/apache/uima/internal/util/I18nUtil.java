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
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Internationaliation utilities.
 * 
 */
public class I18nUtil {
  
  /**
   * Cache for bundle lookup
   *   otherwise, there are multiple lookups in a call-stack-climbing class loader
   *   
   */
  
  static class Bid {
    final String bundleName;
    final Locale locale;
    final ClassLoader loader;
    final ClassLoader [] loaders;
    public Bid(String bundleName, Locale locale, ClassLoader loader, ClassLoader[] loaders) {
      super();
      this.bundleName = bundleName;
      this.locale = locale;
      this.loader = (loaders != null) ? null : loader;
      this.loaders = loaders;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((bundleName == null) ? 0 : bundleName.hashCode());
      result = prime * result + ((loader == null) ? 0 : loader.hashCode());
      result = prime * result +  ((loaders == null) ? 0 : Arrays.hashCode(loaders));
      result = prime * result + ((locale == null) ? 0 : locale.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Bid other = (Bid) obj;
      if (bundleName == null) {
        if (other.bundleName != null)
          return false;
      } else if (!bundleName.equals(other.bundleName))
        return false;
      if (loader == null) {
        if (other.loader != null)
          return false;
      } else if (!loader.equals(other.loader))
        return false;
      if (locale == null) {
        if (other.locale != null)
          return false;
      } else if ( locale != other.locale)
        return false;
      if (loaders == null) {
        if (other.loaders != null)
          return false;
      } else if (!Arrays.equals(loaders, other.loaders))
        return false;
      return true;
    }
    
  }
  
  private static final ThreadLocal<Map<Bid, ResourceBundle>> b_cache = 
      ThreadLocal.withInitial(() -> new HashMap<>());
  
  
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
        // get the constant, thread-safe, stack-climbing class loader
        aLoader = MsgLocalizationClassLoader.getMsgLocalizationClassLoader();        
      }
      
      final boolean is_stack_climbing_loader = aLoader == MsgLocalizationClassLoader.getMsgLocalizationClassLoader();

      // locate the resource bundle for this exception's messages
      String message;
      if (aResourceBundleName == null) {
        message = "Null ResourceBundle, key = \"" + aMessageKey + "\"";
      } else {
        ClassLoader[] cls = is_stack_climbing_loader ? Misc.getCallingClass_classLoaders() : null;
        final ClassLoader final_aLoader = aLoader;
        Bid cache_key = new Bid(aResourceBundleName, aLocale, aLoader, cls);        
        ResourceBundle bundle =  b_cache.get().computeIfAbsent(cache_key,
            (bid) -> 
              ResourceBundle.getBundle(aResourceBundleName, aLocale, final_aLoader));
        message = bundle.getString(aMessageKey);
      }
      // if arguments exist, use MessageFormat to include them
      if (aArguments != null && aArguments.length > 0) {
        MessageFormat fmt = new MessageFormat(message);
        fmt.setLocale(aLocale);
        return fmt.format(aArguments);
      } else
        return message;
    } catch (Exception e) {
      return "MESSAGE LOCALIZATION FAILED: The key " + aMessageKey + " may be missing in the properties file " + e.getMessage();
    }
  }

  public static void setTccl(ClassLoader tccl) {
    MsgLocalizationClassLoader.CallClimbingClassLoader.original_thread_context_class_loader.set(tccl);
  }
  
  public static void removeTccl() {
    MsgLocalizationClassLoader.CallClimbingClassLoader.original_thread_context_class_loader.remove();
  }
    
}
