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
package org.apache.uima.cas.text;

public class Language {

  public static final String UNSPECIFIED_LANGUAGE = "x-unspecified";

  // TODO: finish implementing this class

  public static final Language ZH = new Language("zh");

  public static final Language ZH_CN = new Language("zh-cn");

  public static final Language ZH_TW = new Language("zh-tw");

  public static final char CANONICAL_LANG_SEPARATOR = '-';

  private String lang;

  private String langPart = null;

  private String territoryPart = null;

  public Language(String language) {
    lang = normalize(language);
    parseLanguage();
  }

  public static final String normalize(String lang) {
    if (lang == null) {
      return UNSPECIFIED_LANGUAGE;
    }
    lang = lang.toLowerCase();
    lang = lang.replace('_', CANONICAL_LANG_SEPARATOR);
    return lang;
  }

  public String getLanguagePart() {
    return langPart;
  }

  public String getTerritoryPart() {
    return territoryPart;
  }

  public String getFullLanguage() {
    return lang;
  }

  private final void parseLanguage() {
    int pos = lang.indexOf(CANONICAL_LANG_SEPARATOR);
    if (pos >= 0) {
      langPart = lang.substring(0, pos);
    } else {
      langPart = lang;
      return;
    }
    ++pos;
    if (pos < lang.length()) {
      territoryPart = lang.substring(pos);
    }
  }

  @Override
  public String toString() {
    return "Full language string: " + getFullLanguage() + ", language part: " + getLanguagePart()
            + ", territory part: " + getTerritoryPart();
  }

  public static void main(String[] args) {
    System.out.println(Language.ZH);
    System.out.println(Language.ZH_CN);
    System.out.println(new Language("en_US_NY"));
  }
}
