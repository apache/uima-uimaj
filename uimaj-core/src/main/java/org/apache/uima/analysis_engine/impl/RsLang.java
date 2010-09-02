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
package org.apache.uima.analysis_engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.text.Language;

/**
 * Class used to canonicalize language string
 */
public class RsLang {

 /**
   * global set for canonical language strings
   */
  private static final Map<String, String> canonicalLanguageStrings = new HashMap<String, String>();
 
  /**
   * 
   * @param language
   * @return x-unspecified if lang is null or a canonical version of the lang string
   */
  static String getCanonicalLanguageString(String language) {
    if (language == null || language.equals(Language.UNSPECIFIED_LANGUAGE)) {  // represents x-unspecified
      return Language.UNSPECIFIED_LANGUAGE;
    }
    synchronized(canonicalLanguageStrings) {
      String cl = canonicalLanguageStrings.get(language);
      if (cl == null) {
        // make new string based on trimmed chars if needed, in case holding on to big string
        language = new String(language);
        canonicalLanguageStrings.put(language, language);
        return language;
      }
      return cl;
    }
  }
}
