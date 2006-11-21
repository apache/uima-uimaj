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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementaion of {@link ResultSpecification}.
 * 
 * 
 */
public class ResultSpecification_impl extends MetaDataObject_impl implements ResultSpecification {

  private static final long serialVersionUID = 8516517600467270594L;

  static final String UNSPECIFIED_LANGUAGE = "x-unspecified";

  /**
   * main language separator e.g 'en' and 'en-US'
   */
  private static final char LANGUAGE_SEPARATOR = '-';

  /**
   * Map from TypeOrFeature objects to HashSets that include the language codes (Strings) for which
   * that type or feature should be produced.
   */
  private Map mTypesAndFeatures = new HashMap();

  /**
   * Map from String type or feature names to HashSets that include the language codes (Strings) for
   * which that type or feature should be produced. This is populated by the compile() method, and
   * includes subtypes as well as the individual feature names for types that have
   * allAnnotatorFeatures=true.
   */
  private Map mCompiledNameToLanguageMap = new HashMap();

  /**
   * Default language set to use if nothing else is specified
   */
  private HashSet mDefaultLanguage = new HashSet();
  
  /**
   * constructor init the default languge set with the language x-unspecified
   */
  public ResultSpecification_impl() {
    mDefaultLanguage.add(UNSPECIFIED_LANGUAGE);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures()
   */
  public TypeOrFeature[] getResultTypesAndFeatures() {
    TypeOrFeature[] arr = new TypeOrFeature[mTypesAndFeatures.size()];
    mTypesAndFeatures.keySet().toArray(arr);
    return arr;
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures(java.lang.String)
   */
  public TypeOrFeature[] getResultTypesAndFeatures(String language) {
    // get language without country if applicable
    String baseLanguage = null;
    int index = language.indexOf(LANGUAGE_SEPARATOR);
    if (index > -1) {
      baseLanguage = language.substring(0, index);
    }

    // holds the found ToFs for the specified language
    Vector vec = new Vector();

    Iterator it = mTypesAndFeatures.keySet().iterator();
    while (it.hasNext()) {
      // get current key
      TypeOrFeature tof = (TypeOrFeature) it.next();
      // get value for the current key
      Set values = (HashSet) mTypesAndFeatures.get(tof);
      if (values.contains(language) || values.contains(UNSPECIFIED_LANGUAGE)
                      || values.contains(baseLanguage)) {
        // add tof to the TypeOfFeature array
        vec.add(tof);
      }
    }

    // create array for return
    TypeOrFeature[] arr = new TypeOrFeature[vec.size()];

    // convert vector to TypeOrFeature[]
    vec.toArray(arr);

    return arr;
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures) {
    mTypesAndFeatures.clear();
    for (int i = 0; i < aTypesAndFeatures.length; i++) {
      mTypesAndFeatures.put(aTypesAndFeatures[i], mDefaultLanguage);
    }
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[],
   *      java.lang.String[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures, String[] aLanguageIDs) {
    if (aLanguageIDs != null) {
      // create HashSet for the aLanguageIDs with the initial capacity of the aLanguageIDs size
      HashSet languagesSupported = new HashSet(aLanguageIDs.length);
      // add all supported languages to at HashSet
      for (int x = 0; x < aLanguageIDs.length; x++) {
        // add current language to the HashSet
        languagesSupported.add(aLanguageIDs[x]);
      }

      mTypesAndFeatures.clear();
      for (int i = 0; i < aTypesAndFeatures.length; i++) {
        mTypesAndFeatures.put(aTypesAndFeatures[i], languagesSupported);
      }
      // revert to uncompiled state
      mCompiledNameToLanguageMap.clear();
    } else {
      // if aLangugeIDs is null set ToFs for the default language
      setResultTypesAndFeatures(aTypesAndFeatures);
    }

  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature) {
    mTypesAndFeatures.put(aTypeOrFeature, mDefaultLanguage);
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature,
   *      java.lang.String[])
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature, String[] aLanguageIDs) {
    if (aLanguageIDs != null) {
      // create HashSet for the aLanguageIDs with the initial capacity of the aLanguageIDs size
      HashSet languagesSupported = new HashSet(aLanguageIDs.length);
      // add all supported languages to at HashSet
      for (int x = 0; x < aLanguageIDs.length; x++) {
        // add current language to the HashSet
        languagesSupported.add(aLanguageIDs[x]);
      }

      mTypesAndFeatures.put(aTypeOrFeature, languagesSupported);
      // revert to uncompiled state
      mCompiledNameToLanguageMap.clear();
    } else {
      // if aLangugeIDs is null add ToF with the default language
      addResultTypeOrFeature(aTypeOrFeature);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean)
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures) {
    TypeOrFeature t = new TypeOrFeature_impl();
    t.setType(true);
    t.setName(aTypeName);
    t.setAllAnnotatorFeatures(aAllAnnotatorFeatures);
    mTypesAndFeatures.put(t, mDefaultLanguage);
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean, java.lang.String[])
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures, String[] aLanguageIDs) {
    if (aLanguageIDs != null) {
      // create HashSet for the aLanguageIDs with the initial capacity of the aLanguageIDs size
      HashSet languagesSupported = new HashSet(aLanguageIDs.length);
      // add all supported languages to at HashSet
      for (int x = 0; x < aLanguageIDs.length; x++) {
        // add current language to the HashSet
        languagesSupported.add(aLanguageIDs[x]);
      }

      TypeOrFeature t = new TypeOrFeature_impl();
      t.setType(true);
      t.setName(aTypeName);
      t.setAllAnnotatorFeatures(aAllAnnotatorFeatures);
      HashSet existingLanguages = (HashSet) mTypesAndFeatures.get(t);
      if (existingLanguages != null) {
        existingLanguages.addAll(languagesSupported);
      } else {
        mTypesAndFeatures.put(t, languagesSupported);
      }
      // revert to uncompiled state
      mCompiledNameToLanguageMap.clear();
    } else {
      // if aLangugeIDs is null add type with the default language
      addResultType(aTypeName, aAllAnnotatorFeatures);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String)
   */
  public void addResultFeature(String aFullFeatureName) {
    TypeOrFeature f = new TypeOrFeature_impl();
    f.setType(false);
    f.setName(aFullFeatureName);
    mTypesAndFeatures.put(f, mDefaultLanguage);
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String,
   *      java.lang.String[])
   */
  public void addResultFeature(String aFullFeatureName, String[] aLanguageIDs) {
    if (aLanguageIDs != null) {
      // create HashSet for the aLanguageIDs with the initial capacity of the aLanguageIDs size
      HashSet languagesSupported = new HashSet(aLanguageIDs.length);
      // add all supported languages to at HashSet
      for (int x = 0; x < aLanguageIDs.length; x++) {
        // add current language to the HashSet
        languagesSupported.add(aLanguageIDs[x]);
      }

      TypeOrFeature f = new TypeOrFeature_impl();
      f.setType(false);
      f.setName(aFullFeatureName);
      HashSet existingLanguages = (HashSet) mTypesAndFeatures.get(f);
      if (existingLanguages != null) {
        existingLanguages.addAll(languagesSupported);
      } else {
        mTypesAndFeatures.put(f, languagesSupported);
      }
      // revert to uncompiled state
      mCompiledNameToLanguageMap.clear();
    } else {
      // if aLangugeIDs is null add type with the default language
      addResultFeature(aFullFeatureName);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#compile(org.apache.uima.cas.TypeSystem)
   */
  public void compile(TypeSystem aTypeSystem) {
    mCompiledNameToLanguageMap.clear();
    Iterator it = mTypesAndFeatures.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry elem = (Map.Entry) it.next();
      TypeOrFeature tof = (TypeOrFeature) elem.getKey();
      if (tof.isType()) {
        Type t = aTypeSystem.getType(tof.getName());
        if (t != null) {
          addTypeRecursive(t, aTypeSystem, (HashSet) elem.getValue(), tof.isAllAnnotatorFeatures());
        }
      } else // feature
      {
        mCompiledNameToLanguageMap.put(tof.getName(), elem.getValue());
      }
    }
    // TODO: process the set of intersections
  }

  /**
   * @param t
   */
  private void addTypeRecursive(Type type, TypeSystem typeSystem, HashSet languages,
                  boolean allFeatures) {
    mCompiledNameToLanguageMap.put(type.getName(), languages);
    if (allFeatures) {
      List features = type.getFeatures();
      Iterator featIt = features.iterator();
      while (featIt.hasNext()) {
        Feature f = (Feature) featIt.next();
        mCompiledNameToLanguageMap.put(f.getName(), languages);
      }
    }
    // recurse on subtypes
    List subtypes = typeSystem.getDirectSubtypes(type);
    Iterator typeIt = subtypes.iterator();
    while (typeIt.hasNext()) {
      Type subtype = (Type) typeIt.next();
      addTypeRecursive(subtype, typeSystem, languages, allFeatures);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String)
   */
  public boolean containsType(String aTypeName) {
    if (aTypeName.indexOf(TypeSystem.FEATURE_SEPARATOR) != -1)
      return false; // check against someone passing a feature name here
    // if compile() has been called can be done by a hash lookup
    if (!mCompiledNameToLanguageMap.isEmpty()) {
      return mCompiledNameToLanguageMap.containsKey(aTypeName);
    } else {
      // brute force search
      Iterator it = mTypesAndFeatures.keySet().iterator();
      while (it.hasNext()) {
        TypeOrFeature elem = (TypeOrFeature) it.next();
        if (elem.isType() && aTypeName.equals(elem.getName())) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String,java.lang.String)
   */
  public boolean containsType(String aTypeName, String language) {
    if (language == null) {
      return containsType(aTypeName);
    }
    if (aTypeName.indexOf(TypeSystem.FEATURE_SEPARATOR) != -1)
      return false; // check against someone passing a feature name here

    // get language without country if applicable
    String baseLanguage = null;
    int index = language.indexOf(LANGUAGE_SEPARATOR);
    if (index > -1) {
      baseLanguage = language.substring(0, index);
    }

    boolean found = false;

    if (!mCompiledNameToLanguageMap.isEmpty()) {
      // if compile() has been called can be done by a hash lookup
      HashSet languages = (HashSet) mCompiledNameToLanguageMap.get(aTypeName);
      if (languages != null) {
        // check if tof is supported for the current language
        if (UNSPECIFIED_LANGUAGE.equals(language) || languages.contains(language)
                        || languages.contains(UNSPECIFIED_LANGUAGE)
                        || languages.contains(baseLanguage)) {
          // mark item found
          found = true;
        }
      }
    } else {
      // brute force search
      Iterator it = mTypesAndFeatures.keySet().iterator();
      while (it.hasNext()) {
        TypeOrFeature elem = (TypeOrFeature) it.next();
        if (elem.isType() && aTypeName.equals(elem.getName())) {
          HashSet languages = (HashSet) mTypesAndFeatures.get(elem);
          if (languages != null) {
            // check if tof is supported for the current language
            if (UNSPECIFIED_LANGUAGE.equals(language) || languages.contains(language)
                            || languages.contains(UNSPECIFIED_LANGUAGE)
                            || languages.contains(baseLanguage)) {
              // mark item found
              return true;
            }
          }
        }
      }
    }

    return found;
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String)
   */
  public boolean containsFeature(String aFullFeatureName) {
    if (aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR) == -1)
      return false; // check against someone passing a type name here

    // if compile() has been called can be done by a hash lookup
    if (!mCompiledNameToLanguageMap.isEmpty()) {
      return mCompiledNameToLanguageMap.containsKey(aFullFeatureName);
    } else {
      // brute force search
      // (also need to consider Types with allAnnotatorFeatures = true)
      String typeName = "";
      int typeSeparatorIndex = aFullFeatureName.indexOf(':');
      if (typeSeparatorIndex > 0) {
        typeName = aFullFeatureName.substring(0, typeSeparatorIndex);
      }

      Iterator it = mTypesAndFeatures.keySet().iterator();
      while (it.hasNext()) {
        TypeOrFeature elem = (TypeOrFeature) it.next();
        if ((!elem.isType() && aFullFeatureName.equals(elem.getName()))
                        || (elem.isType() && elem.isAllAnnotatorFeatures() && typeName.equals(elem
                                        .getName()))) {
          return true;
        }
      }

      return false;
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String,java.lang.String)
   */
  public boolean containsFeature(String aFullFeatureName, String language) {
    if (language == null) {
      return containsFeature(aFullFeatureName);
    }
    if (aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR) == -1)
      return false; // check against someone passing a type name here

    // get language without country if applicable
    String baseLanguage = null;
    int index = language.indexOf(LANGUAGE_SEPARATOR);
    if (index > -1) {
      baseLanguage = language.substring(0, index);
    }

    boolean found = false;

    // if compile() has been called can be done by a hash lookup
    if (!mCompiledNameToLanguageMap.isEmpty()) {
      HashSet languages = (HashSet) mCompiledNameToLanguageMap.get(aFullFeatureName);
      if (languages != null) {
        // check if tof is supported for the current language
        if (UNSPECIFIED_LANGUAGE.equals(language) || languages.contains(language)
                        || languages.contains(UNSPECIFIED_LANGUAGE)
                        || languages.contains(baseLanguage)) {
          // mark item found
          found = true;
        }
      }
    } else {
      // brute force search
      // (also need to consider Types with allAnnotatorFeatures = true)
      String typeName = "";
      int typeSeparatorIndex = aFullFeatureName.indexOf(':');
      if (typeSeparatorIndex > 0) {
        typeName = aFullFeatureName.substring(0, typeSeparatorIndex);
      }

      Iterator it = mTypesAndFeatures.keySet().iterator();
      while (it.hasNext()) {
        TypeOrFeature elem = (TypeOrFeature) it.next();
        if ((!elem.isType() && aFullFeatureName.equals(elem.getName()))
                        || (elem.isType() && elem.isAllAnnotatorFeatures() && typeName.equals(elem
                                        .getName()))) {
          HashSet languages = (HashSet) mTypesAndFeatures.get(elem);
          if (languages != null) {
            // check if tof is supported for the current language
            if (UNSPECIFIED_LANGUAGE.equals(language) || languages.contains(language)
                            || languages.contains(UNSPECIFIED_LANGUAGE)
                            || languages.contains(baseLanguage)) {
              // mark item found
              return true;
            }
          }
        }
      }
    }

    return found;
  }

  /**
   * @see org.apache.uima.resource.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("resultSpecification", null,
                    new PropertyXmlInfo[] { new PropertyXmlInfo("resultTypesAndFeatures", null) });
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addCapabilities(org.apache.uima.resource.metadata.Capability[])
   */
  public void addCapabilities(Capability[] capabilities) {
    addCapabilities(capabilities, true);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addCapabilities(org.apache.uima.resource.metadata.Capability[],
   *      boolean)
   */
  public void addCapabilities(Capability[] capabilities, boolean outputs) {
    if (capabilities != null) {
      for (int i = 0; i < capabilities.length; i++) {
        TypeOrFeature[] tofs = outputs ? capabilities[i].getOutputs() : capabilities[i].getInputs();

        // get supported languages
        String[] supportedLanguagesArr = capabilities[i].getLanguagesSupported();

        HashSet supportedLanguages = null;
        if (supportedLanguagesArr != null && supportedLanguagesArr.length > 0) {
          // create new HashSet with the initial capacity of the supportedLanguageArr
          supportedLanguages = new HashSet(supportedLanguagesArr.length);
          // add all supported languages to at HashSet
          for (int x = 0; x < supportedLanguagesArr.length; x++) {
            // add current language to the HashSet
            supportedLanguages.add(supportedLanguagesArr[x]);
          }
        } else {
          // if no languages are set, set default language
          supportedLanguages = mDefaultLanguage;
        }

        for (int y = 0; y < tofs.length; y++) {
          mTypesAndFeatures.put(tofs[y], supportedLanguages);
        }

      }
    }
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#removeTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   */
  public void removeTypeOrFeature(TypeOrFeature aTypeOrFeature) {
    // reomve Type or Feature from the
    mTypesAndFeatures.remove(aTypeOrFeature);
    // revert to uncompiled state
    mCompiledNameToLanguageMap.clear();
  }

  /**
   * returns a clone of the <code>ResultSpecification</code> object.
   * 
   * @return Object copy of the current object
   */
  public Object clone() {
    // create new result specification
    // NOTE: cannot use super.clone here, since we do not want to execute
    // the MetaDataObject_impl logic. TODO: rethink whether this should be
    // a subclass of MetaDataOBject_impl.
    ResultSpecification_impl newResultSpec = new ResultSpecification_impl();

    // clone HashMaps
    newResultSpec.mTypesAndFeatures = new HashMap(this.mTypesAndFeatures);
    newResultSpec.mDefaultLanguage = new HashSet(this.mDefaultLanguage);

    return newResultSpec;
  }
}
