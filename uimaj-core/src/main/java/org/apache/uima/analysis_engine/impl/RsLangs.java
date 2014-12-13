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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.cas.text.Language;

/**
 * A set of languages, each represented by a canonical string object
 * The set is stored without any subsumed elements
 * 
 * Instances of this class are shareable
 * Duplicate-on-update strategy
 *   Requires that all update operations to it return the
 *     possibly new RsLangs object, and that calls are always of the form
 *       rsLangInstance = rsLangInstance.[some-update-operation]
 *   Requires that all copy operations set the shared bit:
 *     copiedInstance = origInstance.setShared(); 
 *  
 * A instance marked isShared == true is immutable
 *   Updates cause duplication.
 *   
 * Users store x-unspecified as null for the rsLangs instance
 *   Because of this, users use static methods, passing in as the first argument,
 *   the value of rsLangs, and getting an updated value of rsLangs.
 *     This allows the passed-in value to be null.
 *  
 * Languages kept in canonical form:
 *   duplicates removed
 *   subsumed languages removed
 *   language strings mapped to unique strings (allowing == comparisons)
 * Languages kept in array list, to allow for expansion
 *   Languages not removed, only added (for a given tof)
 */
public class RsLangs {
  
  private ArrayList<String> languages; // set of languages; null means x-unspecified
  private boolean isShared = false;    // support copy on update for languages
  
  private RsLangs() {}
  
  // for instance used to represent x-unspec inside compiled forms, where null cant be used
  static RsLangs createSharableEmpty() {
    RsLangs rsl = new RsLangs();
    rsl.setShared();
    return rsl;
  }
  
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RsLangs [languages=");
    if (languages != null) {
      for (String l : languages) {
        builder.append(l).append(',');
      }
    }
    builder.append(", isShared=");
    builder.append(isShared);
    builder.append(']');
    return builder.toString();
  }

  static RsLangs createOrNull(String[] languages) {
    return replaceAll(null, languages);
  }
 
  void setShared() {
    isShared = true;
  }
  
  static RsLangs createOrNull(RsLangs rsl) {
    if (null == rsl || rsl.languages == null) {
      return null;
    }
    rsl.setShared();
    return rsl;
  }
  
  // make a copy when needed
  private RsLangs(RsLangs original) {
    languages = (null == original.languages) ? null : new ArrayList<String>(original.languages);
  }
  
  static boolean isEmpty(RsLangs rsl) {
    return rsl == null || rsl.languages == null || rsl.languages.size() == 0;
  }
  
  /**
   * 
   * @param rsl may be null (means x-unspec, subsumes all)
   * @param lang
   * @return true if rsl subsumes lang
   */
  static boolean subsumes(RsLangs rsl, String lang) {
    return subsumesCanonical(rsl, RsLang.getCanonicalLanguageString(lang));
  }
  /**
   * 
   * @param lang
   * @return true if any of the rsLangs subsumes the param lang
   */

//  @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_PARAMETER_STRING_WITH_EQ")
  static boolean subsumesCanonical(RsLangs rsl, String lang) {
    if (null == rsl || null == rsl.languages) {  // don't test for size() == 0 - that's used by replace to indicate empty, not x-unspec
      return true;  // x-unspecified subsumes all
    }
    if (null == lang || lang == Language.UNSPECIFIED_LANGUAGE) {
      return false;  // x-unspec not subsumed by anything (other than x-unspec)
    }
    String baseLang = getBaseLanguage(lang);
    for (String rsLang : rsl.languages) {
      if (subsumesCanonical(rsLang, lang, baseLang)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * 
   * @param rsl assumed to be not null, not x-unspec
   * @param lang assumed to be not null, not x-unspec
   * @param baseLang
   * @return true if any of the rsl languages is equal to the lang or the base lang
   */
  private static boolean subsumesCanonical(RsLangs rsl, String lang, String baseLang) {
    for (String rsLang : rsl.languages) {
      if (subsumesCanonical(rsLang, lang, baseLang)) {
        return true;
      }
    }
    return false;
  }
  
//  @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_PARAMETER_STRING_WITH_EQ")  
  private static boolean subsumesCanonical(String containingLang, String langToTest, String langToTestBase) {
    return containingLang == langToTest || containingLang == langToTestBase; 
  }

  /**
   * 
   * @param language (must not be null)
   * @return the same == language or the base form of the language
   */
  private static String getBaseLanguage(String language) {
    String baseLanguage = language;
    int index = language.indexOf(Language.CANONICAL_LANG_SEPARATOR);
    if (index > -1) {
      baseLanguage = RsLang.getCanonicalLanguageString(language.substring(0, index));
    }
    return baseLanguage;
  }

  /**
   * 
   * @param rsl could be null meaning current is x-unspecified
   * @param langs null means x-unspecified
   * @return null (meaning x-unspecified, or an instance of RsLangs
   */
  static RsLangs replaceAll(RsLangs rsl, String[] langs) {
    if (rsl == null || rsl.languages == null) {
      if (langs == null || (langs.length == 0)) { // UIMA-2212
        return null;
      }
      if (rsl == null || rsl.isShared) {
        rsl = new RsLangs();
      }
      rsl.languages = new ArrayList<String>(1);  // special form means empty, not x-unspec
    }
    return addAll(rsl, langs);
  }
  
  static RsLangs addAll(RsLangs rsl, String[] langs) {
    if (null == langs || 
        null == rsl || null == rsl.languages) {  // because x-unspec subsumes all
      return rsl;
    } else {
      for (String lang : langs) {
        rsl = add(rsl, lang);      
      }
      return rsl;
    }    
  }
    
  static RsLangs addAll(RsLangs rsl, RsLangs rsLangs) {
    if (null == rsLangs || null == rsLangs.languages ||
        null == rsl || null == rsl.languages) {  // because x-unspec subsumes all
      return rsl;
    }
    for (String lang : rsLangs.languages) {
      rsl = add(rsl, lang);      
    }
    return rsl;
  }

  /**
   * add language unless it's subsumed by existing one
   * remove any languages the newly added one subsumes
   * store x-unspec as null
   * @param rsl - is not null and has non-null languages array (may be empty)
   * @param lang - may be null or x-unspec
   */
  static RsLangs add(RsLangs rsl, String lang) {
    lang = RsLang.getCanonicalLanguageString(lang);
    if (lang == Language.UNSPECIFIED_LANGUAGE) {
      return null;
    }
    String baseLang = getBaseLanguage(lang);
    if (!subsumesCanonical(rsl, lang, baseLang)) {
      if (rsl.isShared) {
        rsl = new RsLangs(rsl);
      }
      rsl.removeSubsumedLanguages(lang, baseLang);  // remove subsumed lang, but leave as empty list if all removed
      rsl.languages.add(lang);
    }
    return rsl;
  }
  
  /**
   * Remove languages that are subsumed by the argument
   * If all removed, keep as empty list
   * @param canonicalLang
   */
  private void removeSubsumedLanguages(String canonicalLang, String baseLang) {
    for (Iterator<String> it = languages.iterator(); it.hasNext();) {
      if (subsumesCanonical(it.next(), canonicalLang, baseLang)) {
        it.remove();
      }
    } 
  }
  
  /**
   * 
   * @param other
   * @return null for empty intersection (null doesn't mean x-unspecified here)
   */
  RsLangs intersect(RsLangs other) {
    if (null == other) {
      return null;
    }
        
    if (null == this.languages) { // means x-unspecified, so return the other
      return other;
    }
    if (null == other.languages) { // means x-unspecified, so return the first
      return this;
    }
    
    RsLangs r = new RsLangs();
    r.languages = new ArrayList<String>(1);  // creates an empty, not null arraylist
    
    for (String lang : this.languages) {
      if (subsumesCanonical(other, lang)) {
        r = add(r, lang);                      // add langs in other that are subsumed by this
      }      
    }
    for (String lang : other.languages) {
      if (subsumesCanonical(this, lang)) {               // add langs in this that are subsumed by other 
        r = add(r, lang);
      }
    }
    if (r.languages.size() == 0) {
      return null;
    }
    return r;
  }
  
  static String[] toArray(RsLangs rsl) {    
    return (isEmpty(rsl)) ? null : rsl.languages.toArray(new String[rsl.languages.size()]);
  }
  
  /**
   * Must return the same hashcode regardless of the value of isShared, and
   * treating the values as a set
   */
  @Override
  public int hashCode() {
    int result = 31;
    for (String lang : languages) {
      result += lang.hashCode();  // non-standard, gives same answer regardless of order 
    }
    return result;
  }

  /**
   * This must return true ignoring the value of isShared, and
   * treating the lists as a set
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RsLangs other = (RsLangs) obj;
    if (languages == null) {
      if (other.languages != null) {
        return false;
      }
    } else {
      if (languages.size() != other.languages.size()) {
        return false;
      }
      for (String lang : languages) {
        if (!other.languages.contains(lang)) {
          return false;
        }
      }
    }
    return true;
  }
  
//  /**
//   * also canonicalizes the language strings
//   * @param languages
//   * @return
//   */
//  private void canonicalizeRemoveDupsAndSubsumptions(String[] languages) {
//    if (null == languages || languages.length == 0) {
//      this.languages = null;
//      return;
//    }
//    
//    add
//    
//    // have 2 or more languages
//  outer:
//    for (int i = 0; i < languages.size(); i++) {
//      String later = Language.normalize(languages.get(i));
//      if (null == later || later.equals(Language.UNSPECIFIED_LANGUAGE)) {
//        return null;
//      }
//      // compare against all earlier ones
//      for (int j = 0; j < i; j++) {
//        String earlier = languages.get(j);
//        String earlierBase = getBaseLanguage(earlier);
//        if (earlier.equals(later)) {
//          languages.remove(i--);
//          continue outer;
//        }
//        if (earlierBase.equals(later)) {   // later subsumes earlier
//          languages.set(i, later);          
//          languages.remove(i--);
//          // recursion: handle multiple cases:
//          //   replacing earlier with more general later could have it now 
//          //   subsume others in between earlier and later... so need to rescan
//          return removeDupsAndSubsumptions(languages);
//        }
//        if (earlier.equals(getBaseLanguage(later))) {  // earlier subsumes later
//          languages.remove(i--);
//          continue outer;          
//        }
//      }
//    }
//    return languages;
//  }  

}
