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

package org.apache.uima.pear.generate;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 
 * Utility class which helps managing messages
 * 
 * 
 * 
 */
public class PearExportMessages {

  private static final String RESOURCE_BUNDLE = "org.apache.uima.pear.generate.messages";//$NON-NLS-1$

  private static ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);

  private PearExportMessages() {
    // prevent instantiation of class
  }

  /**
   * Returns the formatted message for the given key in the resource bundle.
   * 
   * @param key
   *          the resource name
   * @param args
   *          the message arguments
   * @return the string
   */
  public static String format(String key, Object[] args) {
    return MessageFormat.format(getString(key), args);
  }

  /**
   * Returns the resource object with the given key in the resource bundle. If there isn't any value
   * under the given key, the key is returned.
   * 
   * @param key
   *          the resource name
   * @return the string
   */
  public static String getString(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
