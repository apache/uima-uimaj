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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.uima.cas.text.Language;

/**
 * Class used to canonicalize language string
 */
public class RsLang {

 /**
   * global set for canonical language strings
   */
  private static final ConcurrentMap<String, String> canonicalLanguageStrings = new ConcurrentHashMap<String, String>();
 
  /**
   * 
   * @param language
   * @return x-unspecified if lang is null or a canonical version of the lang string
   */
//  @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_STRING_CTOR")
  static String getCanonicalLanguageString(String language) {
    if (language == null || language.equals(Language.UNSPECIFIED_LANGUAGE)) {  // represents x-unspecified
      return Language.UNSPECIFIED_LANGUAGE;
    }
    String cl = canonicalLanguageStrings.get(language), clOther;
    if (cl == null) {
      // make new string based on trimmed chars if needed, in case holding on to big string
      // This strange construct is intended to drop references to big char arrays  
      //   where only a part the big char arrays constitute this string.
      language = new StringBuilder(language.length()).append(language).toString();
      clOther = canonicalLanguageStrings.putIfAbsent(language, language);
      cl = (clOther != null) ? clOther : language;
    }
    return cl;
  }
}
