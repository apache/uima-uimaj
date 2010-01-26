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

package org.apache.uima.collection.impl.cpm.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class CpmLocalizedMessage {

  public static String getLocalizedMessage(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
    if (aMessageKey == null)
      return null;

    try {
      // locate the resource bundle for this exception's messages
      ResourceBundle bundle = ResourceBundle.getBundle(aResourceBundleName, Locale.getDefault());
      // retrieve the message from the resource bundle
      String message = bundle.getString(aMessageKey);
      // if arguments exist, use MessageFormat to include them
      if (aArguments != null && aArguments.length > 0) {
        MessageFormat fmt = new MessageFormat(message);
        fmt.setLocale(Locale.getDefault());
        return fmt.format(aArguments);
      } else
        return message;
    } catch (Exception e) {
      return "EXCEPTION MESSAGE LOCALIZATION FAILED: " + e.toString();
    }

  }
}
