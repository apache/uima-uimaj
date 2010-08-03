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
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.Language;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementaion of {@link ResultSpecification}.
 * 
 * Notes on the implementation
 * 
 * There are two ways this data is used:  with and without "compiling"
 *   Compiling means: adding subtypes of types and adding all features of a type
 *   Uncompiled form is called ORIGINAL.
 *   
 *   Compiling is deferred - until the first reference to containsType or Feature.
 *   
 * Many instances of this class are made, sometimes via cloning.
 * 
 * Sometimes types and features are deleted - the intent is to do this operation on the
 * uncompiled form, and then "recompile" it.
 * 
 * Types and Features are kept on a per-language basis.  Language can include a special value,
 * x-unspecified, which "matches" any other language.
 * 
 * Language specifications are simplified to eliminate the country part.  All refs to 
 * test if a type or feature is in the result spec for a language uses the simplified language.
 * 
 * Set operations are done to combine, for a particular type or feature, the languages for which it is valid.
 * This is a Union operation
 * Set operations are done to union the input types/features with the output types/features when computing the default
 * result-spec for an aggregate.
 * Set operations are done to intersect the result spec with the output capabilities of a component.
 * 
 * Languages are represented as integers; there is a hash table from the string to the integer, and
 * an array to go from integer to lang string.
 * 
 * A result set of ORIGINALs consists of types/features with associated language sets. 
 */

public final class ResultSpecification_impl extends MetaDataObject_impl implements
        ResultSpecification {

  private static final long serialVersionUID = 8516517600467270594L;

  private static final int UNSPECIFIED_LANGUAGE_INDEX = 0;

  /**
   * main language separator e.g 'en' and 'en-US'
   */
  private static final char LANGUAGE_SEPARATOR = '-';
  
  private class ToF_Languages implements Cloneable {
    public TypeOrFeature tof;
    public BitSet languages;
    
    ToF_Languages(TypeOrFeature aTof, String[] aLanguages) {
      tof = aTof;
      languages = new BitSet();
      for (String lang : aLanguages) {
        languages.set(getLanguageIndex(lang));
      }
    }
    
    ToF_Languages(TypeOrFeature aTof, BitSet aLanguages) {
      tof = aTof;
      languages = aLanguages;
    }
    
    public Object clone() {
      return new ToF_Languages((TypeOrFeature) tof.clone(), (BitSet)languages.clone());
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((languages == null) ? 0 : languages.hashCode());
      result = prime * result + ((tof == null) ? 0 : tof.hashCode());
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
      final ToF_Languages other = (ToF_Languages) obj;
      if (languages == null) {
        if (other.languages != null)
          return false;
      } else if (!languages.equals(other.languages))
        return false;
      if (tof == null) {
        if (other.tof != null)
          return false;
      } else if (!tof.equals(other.tof))
        return false;
      return true;
    }
    
    
  }

  private boolean needsCompilation = true;
  
  private final Map<String, Integer> lang2int; 
  
  
  /**
   * hash map used to map fully qualified type and feature names to associated
   * ToF_Languages instances.  This used for ORIGINAL types and features.
   * 
   * Another hash map is used for compiled types and features - these include
   * the subtypes of the ORIGINAL types.  We keep the originals because the
   * operations of adding and removing types and features are done with respect
   * to the originals, only, and then the other map for compiled types is recomputed.
   * 
   * A case in particular: we need to be able to distinguish which types were
   * originally marked allAnnotatorFeatures, versus those types which were
   * added because they were subtypes.  The corner case happens when a type is both
   * an original and is also an added-via-subtype, where the allAnnotatorFeatures
   * flag of the original is not set but the subtype version is set.
   * 
   */
  private final Map<String, ToF_Languages> name2tof_langs;

  /**
   * hash map used to map fully qualified type and feature names to associated
   * ToF_Languages instances.  This used for COMPILED types and features.
   */

  private final Map<String, ToF_Languages> withSubtypesName2tof_langs;
  
//  /**
//   * Map from TypeOrFeature objects to HashSets that include the language codes (Strings) for which
//   * that type or feature should be produced.
//   */
//  private Map<TypeOrFeature, Set<String>> mTypesAndFeatures = new HashMap<TypeOrFeature, Set<String>>();
//
//  /**
//   * Map from String type or feature names to HashSets that include the language codes (Strings) for
//   * which that type or feature should be produced. This is populated by the compile() method, and
//   * includes subtypes as well as the individual feature names for types that have
//   * allAnnotatorFeatures=true.
//   */
//  private final Map<String, Set<String>> mCompiledNameToLanguageMap = 
//                                   new HashMap<String, Set<String>>();
  
  /**
   * Default language set to use if nothing else is specified
   */
  private static final String[] UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1 = new String[] {Language.UNSPECIFIED_LANGUAGE};

  /**
   * The type system used to compute the subtypes and allAnnotatorFeatures of types
   */
  private TypeSystem mTypeSystem;

  /**
   * constructor:  init the default languge set with the language x-unspecified
   */
  public ResultSpecification_impl() {
    name2tof_langs = new HashMap<String, ToF_Languages>();
    withSubtypesName2tof_langs = new HashMap<String, ToF_Languages>();
    lang2int = new HashMap<String, Integer>();
    lang2int.put(Language.UNSPECIFIED_LANGUAGE, 0); 
  }

  /**
   * Constructor specifying the type system
   *   this should always be used in preference to the 0 argument version
   *   if the type system is available.  Otherwise, the type system *must*
   *   be set via a method call prior to querying the result spec, with the
   *   one exception of the method getResultTypesAndFeaturesWithoutCompiling
   * @param aTypeSystem
   */
  public ResultSpecification_impl(TypeSystem aTypeSystem) {
    this();
    mTypeSystem = aTypeSystem;
  }
  
  private ResultSpecification_impl(ResultSpecification_impl original) {
    name2tof_langs = new HashMap<String, ToF_Languages>(original.name2tof_langs.size());
    withSubtypesName2tof_langs = new HashMap<String, ToF_Languages>(original.withSubtypesName2tof_langs.size());
    
    // don't share this - unless prove there are no multi-tasking interlocks possible
    lang2int = new HashMap<String, Integer>(original.lang2int);
    
    for (Map.Entry<String, ToF_Languages> entry : original.name2tof_langs.entrySet()) {
      ToF_Languages tof_langs = entry.getValue();
      
      // note: TypeOrFeature instances are not cloned, but shared
      //   If they are modified, things may break
      name2tof_langs.put(entry.getKey(), 
          new ToF_Languages(tof_langs.tof, (BitSet)(tof_langs.languages.clone())));
    }
    mTypeSystem = original.mTypeSystem;
  }

  private int getBaseLanguageIndex(String language) {
    return getLanguageIndex(getBaseLanguage(language));
  }
  
  private int getLanguageIndex(String language) {
    Integer r = lang2int.get(language);
    if (null == r) {
      int i = lang2int.size();
      lang2int.put(language, Integer.valueOf(i));
      return i;
    }
    return r.intValue();
  }

  private void compileIfNeeded() {
    if (needsCompilation) {
      compile();
    }
  }
  
  private static String getBaseLanguage(String language) {
    String baseLanguage = language;
    int index = language.indexOf(LANGUAGE_SEPARATOR);
    if (index > -1) {
      baseLanguage = language.substring(0, index);
    }
    return baseLanguage;
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures()
   */
  public TypeOrFeature[] getResultTypesAndFeatures() {
    TypeOrFeature[] arr = new TypeOrFeature[name2tof_langs.size()];
    int i = 0;
    for (ToF_Languages tof_langs : name2tof_langs.values()) {
      arr[i++] = tof_langs.tof;
    }
    return arr;
  }
  
  private Map<String, ToF_Languages> availName2tof_langs() {
    if (needsCompilation) {
      return name2tof_langs;
    }
    return withSubtypesName2tof_langs;
  }

  /**
   * return the set of languages for this type or feature, or null if no such type/feature
   */
  private ToF_Languages getLanguagesForTypeOrFeature(String typeOrFeature) {
    boolean isType = typeOrFeature.indexOf(TypeSystem.FEATURE_SEPARATOR) == -1;
    Map<String, ToF_Languages> tofMap = (isType) ? availName2tof_langs() : name2tof_langs;
    return tofMap.get(typeOrFeature);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures(java.lang.String)
   */
  public TypeOrFeature[] getResultTypesAndFeatures(String language) {
    
    int languageIndex = getLanguageIndex(language);
    int baseLanguageIndex = getBaseLanguageIndex(language);

    // holds the found ToFs for the specified language
    List<TypeOrFeature> foundToF = new ArrayList<TypeOrFeature>();

    for (Map.Entry<String, ToF_Languages> entry : name2tof_langs.entrySet()) {
      if (languageMatches(entry.getValue(), languageIndex, baseLanguageIndex)) {
        foundToF.add(entry.getValue().tof);
      }
    }
    return foundToF.toArray(new TypeOrFeature[foundToF.size()]);
  }

  // private helper functions
  
//  private boolean sameLanguages(String [] s, BitSet b) {
//    if (s.length != b.cardinality()) {
//      return false;
//    }
//    for (String lang : s) {
//      if ( ! b.get(getLanguageIndex(lang))) {
//        return false;
//      }
//    }
//    return true;
//  }
  
  /**
   * change null languages to the unspecified language
   * change a set of languages that includes the unspecified language to
   *   just the unspecified language.  
   *   This is OK when storing things into a result spec, since
   *     the unspecified language will match any query.
   *   This doesn't apply for querying because the queries only 
   *     specify one language, not a set 
   */
  private String [] normalizeLanguages(String [] languages) {   
    if (null == languages) {
      return UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1;
    } else {
      for (String lang : languages) {
        if (lang.equals(Language.UNSPECIFIED_LANGUAGE)) {
          return UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1;
        }
      }
    }
    // normalization is expensive - so do this once as part of parsing capabilities
//    int i = 0;
//    for (String language : languages) {
//      languages[i++] = normalizeLanguage(language);
//    }
    return languages;  
  }
  
//  private String normalizeLanguage(String language) {
//    String result = language.toLowerCase(Locale.ENGLISH);  // language specs are in English locale
//    return result.replace('_', '-');
//  }
  
  private void setNeedsCompilation() {
    needsCompilation = true;
    if (0 != withSubtypesName2tof_langs.size()) {
      withSubtypesName2tof_langs.clear();
    }
  }
  
  private void addTypeOrFeatureInternal(TypeOrFeature tof, String[] languages) {
    languages = normalizeLanguages(languages);
    
    ToF_Languages tof_langs = name2tof_langs.get(tof.getName());
    if (null == tof_langs) {
      name2tof_langs.put(tof.getName(), new ToF_Languages(tof, languages));
      setNeedsCompilation();
      return;
    }
    tof_langs.tof.setAllAnnotatorFeatures(tof.isAllAnnotatorFeatures());
    BitSet langBitSet = tof_langs.languages;
    langBitSet.clear();
    for (String lang : languages) {
      langBitSet.set(getLanguageIndex(lang));
    }
    setNeedsCompilation();
  }
  
  /**
   * Create an entry in this result spec from the type or feature and its languages
   * @param tofLangs
   */
  private void addClonedToF_Languages(ToF_Languages tofLangs, ResultSpecification_impl rs) {
    List<String> languages = new ArrayList<String>();
    BitSet bs = tofLangs.languages;
    for (Map.Entry<String, Integer> si : rs.lang2int.entrySet()) {
      if (bs.get(si.getValue())) {
        languages.add(si.getKey());
      }
    }
    
    ToF_Languages n = new ToF_Languages(
        tofLangs.tof, 
        languages.toArray(new String[languages.size()]));
    name2tof_langs.put(n.tof.getName(), n);
    setNeedsCompilation();
  }

  private TypeOrFeature createTypeOrFeature(String name, boolean isType, boolean aAllAnnotatorFeatures) {
    TypeOrFeature r = new TypeOrFeature_impl();
    r.setType(isType);
    r.setName(name);
    if (isType) {
      r.setAllAnnotatorFeatures(aAllAnnotatorFeatures);
    }
    return r;
  }

  private void addResultTypeOrFeatureAddLanguage(String name, boolean isType, boolean allAnnotatorFeatures, String[] languages) {

    ToF_Languages tof_langs = name2tof_langs.get(name);
    
    if (null == tof_langs) {
      addTypeOrFeatureInternal(createTypeOrFeature(name, isType, allAnnotatorFeatures), languages);
      setNeedsCompilation();
      return;
    }
    
    // tof_langs entry for this name exists, so update it
    addResultTypeOrFeatureAddLanguageCommon(tof_langs, allAnnotatorFeatures, languages);
  } 
  
  private void addResultTypeOrFeatureAddLanguage(TypeOrFeature tof, String[] languages) {

    ToF_Languages tof_langs = name2tof_langs.get(tof.getName());
    
    if (null == tof_langs) {
      addTypeOrFeatureInternal(tof, languages);
      setNeedsCompilation();
      return;
    }
    
    addResultTypeOrFeatureAddLanguageCommon(tof_langs, tof.isAllAnnotatorFeatures(), languages);
  }
  
  private void addResultTypeOrFeatureAddLanguageCommon(
      ToF_Languages tof_langs, boolean allAnnotatorFeatures, String [] languages) {

    // tof_langs entry for this name exists, so update it
    if (allAnnotatorFeatures) {
      if (!tof_langs.tof.isAllAnnotatorFeatures()) {
        tof_langs.tof.setAllAnnotatorFeatures(true);
        setNeedsCompilation();
      }
    }

    // update the languages by adding the new languages passed in
    languages = normalizeLanguages(languages);
    BitSet langBitSet = tof_langs.languages;
    
    // "==" ok here due to normalizeLanguages call above
    if (languages == UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1) {
      if ( ! langBitSet.get(UNSPECIFIED_LANGUAGE_INDEX)) {
        langBitSet.clear();
        langBitSet.set(UNSPECIFIED_LANGUAGE_INDEX);
        setNeedsCompilation();
      }
      return;
    } 
 
    // languages set already exists; add new ones to existing set
    for (String lang : languages) {
      langBitSet.set(getLanguageIndex(lang));
    }
    setNeedsCompilation();
  }
  
  /**
   * version used by compile to add subtypes
   * @param aTypeName
   * @param aAllAnnotatorFeatures
   * @param languages
   */
  private void addResultType(String name, boolean allAnnotatorFeatures, BitSet languages) {
    ToF_Languages tof_langs = withSubtypesName2tof_langs.get(name);
    
    if (null == tof_langs) {
      withSubtypesName2tof_langs.put(
          name, 
          new ToF_Languages(createTypeOrFeature(name, true, allAnnotatorFeatures), (BitSet)languages.clone()));
      return;
    }

    // tof_langs entry for this name exists, so update it
    if (allAnnotatorFeatures) {
      if (!tof_langs.tof.isAllAnnotatorFeatures()) {
        tof_langs.tof.setAllAnnotatorFeatures(true);
      }
    }

    // update the languages by adding the new languages passed in
    tof_langs.languages.or(languages); 
  }

   
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures) {
    setResultTypesAndFeatures(aTypesAndFeatures, UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[],
   *      java.lang.String[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures, String[] aLanguageIDs) {
    name2tof_langs.clear();
    for (TypeOrFeature tof : aTypesAndFeatures) {
      name2tof_langs.put(tof.getName(), new ToF_Languages(tof, normalizeLanguages(aLanguageIDs)));
    }    
    setNeedsCompilation();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature) {
    addTypeOrFeatureInternal(aTypeOrFeature, UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature,
   *      java.lang.String[])
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature, String[] aLanguageIDs) {
    addTypeOrFeatureInternal(aTypeOrFeature, aLanguageIDs);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean)
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures) {
    addTypeOrFeatureInternal(createTypeOrFeature(aTypeName, true, aAllAnnotatorFeatures), UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean, java.lang.String[])
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures, String[] aLanguageIDs) {
    addResultTypeOrFeatureAddLanguage(aTypeName, true, aAllAnnotatorFeatures, aLanguageIDs);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String)
   */
  public void addResultFeature(String aFullFeatureName) {
    addResultFeature(aFullFeatureName, UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String,
   *      java.lang.String[])
   */
  public void addResultFeature(String aFullFeatureName, String[] aLanguageIDs) {
    addResultTypeOrFeatureAddLanguage(aFullFeatureName, false, false, aLanguageIDs);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#compile(org.apache.uima.cas.TypeSystem)
   */
  public void compile(TypeSystem aTypeSystem) {
    setTypeSystem(aTypeSystem);
    compileIfNeeded();
  }
  
//  private static class TypeToCompile {
//    String name;
//    boolean allFeatures;
//    String[] languages;
//    TypeToCompile(String aName, boolean aAllFeatures, String[] aLanguages) {
//      name = aName;
//      allFeatures = aAllFeatures;
//      languages = aLanguages;
//    }
//  }
  
  private void compile() { 
    if (null == mTypeSystem) {
      return;
    }
    
    needsCompilation = false;
    // get set of current type names
    // for each name, get set of implied additional names (allAnnotatorFeatures and subtypes), recursively
    // add with languages
    
    // issue:  can a result spec hold for language 1 types a b c, for language 2 types a b? yes
    //         can it hold for lang 1 type a(allfeats) and for lang 2 type a(not all feat)? no
    
//    Map<String, TypeToCompile> typesToCompile = new HashMap<String, TypeToCompile>(mNameToTofLang.size());
//    for (ToF_Languages tof_langs : mNameToTofLang.values()) {
//      TypeOrFeature tof = tof_langs.tof;
//      if (tof.isType()) {
//        String typeName = tof.getName();
//        typesToCompile.put(typeName, new TypeToCompile(typeName, tof.isAllAnnotatorFeatures(), tof_langs.languages));
//      }
//    }

    for (ToF_Languages tof_langs : name2tof_langs.values()) {
        TypeOrFeature tof = tof_langs.tof;
        
        addResultType(tof.getName(), tof.isAllAnnotatorFeatures(), tof_langs.languages);
        
        if (tof.isType()) {
          compileTypeRecursively(mTypeSystem.getType(tof.getName()), tof.isAllAnnotatorFeatures(), tof_langs.languages);
        }
    }
  }
    
//    mCompiledNameToLanguageMap.clear();
//    for (Map.Entry<TypeOrFeature, Set<String>> elem : mTypesAndFeatures.entrySet()) {
//      TypeOrFeature tof = elem.getKey();
//      if (tof.isType()) {
//        Type t = aTypeSystem.getType(tof.getName());
//        if (t != null) {
//          addTypeRecursive(t, aTypeSystem, elem.getValue(), tof.isAllAnnotatorFeatures());
//        }
//      } else { // feature
//        mCompiledNameToLanguageMap.put(tof.getName(), elem.getValue());
//      }
//    }
//    // TODO: process the set of intersections
//  }

  private void compileTypeRecursively(Type type, boolean allFeatures, BitSet languages) {

    if (null != type) {
//      if (allFeatures) {
//        for (Feature f : (List<Feature>) type.getFeatures()) {
//          addResultFeature(f.getName(), languages); // this add "merges"
//                                                    // langauges with existing
//                                                    // ones
//        }
//      }
      
      for (Type subType : (List<Type>) mTypeSystem.getDirectSubtypes(type)) {
        String subTypeName = subType.getName();
        addResultType(subTypeName, allFeatures, languages);
        compileTypeRecursively(subType, allFeatures, languages);
      }
    }
  }
  
// /**
// * @param t
// */
//  private void addTypeRecursive(Type type, TypeSystem typeSystem, Set<String> languages,
//          boolean allFeatures) {
//    mCompiledNameToLanguageMap.put(type.getName(), languages);
//    if (allFeatures) {
//      for (Feature f : (List<Feature>)type.getFeatures()) {
//        mCompiledNameToLanguageMap.put(f.getName(), languages);
//      }
//    }
//    // recurse on subtypes
//    for (Type subtype : (List<Type>)typeSystem.getDirectSubtypes(type)) {
//      addTypeRecursive(subtype, typeSystem, languages, allFeatures);
//    }
//  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String)
   */
  public boolean containsType(String aTypeName) {
    return containsType(aTypeName, Language.UNSPECIFIED_LANGUAGE);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String,java.lang.String)
   */
  public boolean containsType(String aTypeName, String language) {
    language = Language.normalize(language);

    if (aTypeName.indexOf(TypeSystem.FEATURE_SEPARATOR) != -1)
      return false; // check against someone passing a feature name here
    
    compileIfNeeded();
    return languageMatches(availName2tof_langs().get(aTypeName), language);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String)
   */
  public boolean containsFeature(String aFullFeatureName) {
    return containsFeature(aFullFeatureName, Language.UNSPECIFIED_LANGUAGE);
  }
  
  
//    int typeEndPosition = aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR);
//    if (typeEndPosition == -1)
//      return false; // check against someone passing a type name here
//
//    compileIfNeeded();
//    if (availName2tof_langs().containsKey(aFullFeatureName)) {
//      return true;
//    }
    
    // special code here to return true if the allAnnotatorFeatures flag is set for the type
//    String typeName = aFullFeatureName.substring(0, typeEndPosition);
//    ToF_Languages tof_langs = availName2tof_langs().get(typeName);
//    if (null != tof_langs && tof_langs.tof.isAllAnnotatorFeatures()) {
//      if (null != mTypeSystem) {
//        return null != mTypeSystem.getFeatureByFullName(aFullFeatureName);  // verify feature is there
//      }
//      return true;
//    }
//    return false;
//  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String,java.lang.String)
   */
  public boolean containsFeature(String aFullFeatureName, String language) {
    language = Language.normalize(language);
    int typeEndPosition = aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR);
    if (typeEndPosition == -1)
      return false; // check against someone passing a type name here

    compileIfNeeded();
    ToF_Languages tof_langs = name2tof_langs.get(aFullFeatureName);
    if (languageMatches(tof_langs, language)) {
      return true;
    }
    
    // special code for allAnnotatorFeatures: return true if type name is found and
    // has all annotator features set
    tof_langs = availName2tof_langs().get(aFullFeatureName.substring(0, typeEndPosition));
    if (null != tof_langs && tof_langs.tof.isAllAnnotatorFeatures() && languageMatches(tof_langs, language)) {
      if (null != mTypeSystem) {
        return null != mTypeSystem.getFeatureByFullName(aFullFeatureName);  // verify feature is there
      }
      return true;
    }
    return false;
  }

  /**
   * Languages matches if the query language is xxx-yyy and
   *    result spec languages contains:
   *       x-unspecified
   *       xxx-yyy
   *       xxx  
   *    
   * @param tof_langs
   * @param language
   * @return
   */
  private boolean languageMatches(ToF_Languages tof_langs, String language) {
    if (null == tof_langs) {
      return false;
    }    
    BitSet languages = tof_langs.languages;
    if (languages.get(UNSPECIFIED_LANGUAGE_INDEX) ||
        languages.get(getLanguageIndex(language))) {
      return true;
    }
    String baseLanguage = getBaseLanguage(language);
    return baseLanguage != language &&  // the != means the base language is different from the language
                                        // != is OK here
           languages.get(getLanguageIndex(baseLanguage));
  }

  private boolean languageMatches(ToF_Languages tof_langs, int languageIndex, int baseLanguageIndex) {
    if (null == tof_langs) {
      return false;
    }    
    BitSet languages = tof_langs.languages;
    if (languages.get(UNSPECIFIED_LANGUAGE_INDEX) ||
        languages.get(languageIndex)) {
      return true;
    }
    return baseLanguageIndex != languageIndex && 
           languages.get(baseLanguageIndex);
  }
  
  /**
   * @see org.apache.uima.resource.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
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
    if (null == capabilities) {
      return;
    }
    for (Capability capability : capabilities) {
      TypeOrFeature[] tofs = outputs ? capability.getOutputs() : capability.getInputs();
      String[] supportedLanguages = capability.getLanguagesSupported();
      if (null == supportedLanguages ||
          supportedLanguages.length == 0) {
        supportedLanguages = UNSPECIFIED_LANGUAGE_IN_ARRAY_OF_1;
      }
      for (TypeOrFeature tof : tofs) {
        addResultTypeOrFeatureAddLanguage(tof, supportedLanguages);
      }
    }
    setNeedsCompilation();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#removeTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   */
  public void removeTypeOrFeature(TypeOrFeature aTypeOrFeature) {
    // remove Type or Feature from the
    name2tof_langs.remove(aTypeOrFeature.getName());
    setNeedsCompilation();  // may have removed something which had subtypes
  }

  /**
   * returns a clone of the <code>ResultSpecification</code> object.
   * 
   * @return Object copy of the current object
   */
  @Override
  public Object clone() {
    // create new result specification
    // NOTE: we don't use super.clone here, since for performance reasons
    // we want to do a faster clone that what the general-purpose logic in
    // MetaDataObject_impl does. This class is marked final so that
    // this can't cause a problem if ResultSpecification_impl is subclassed.
    return new ResultSpecification_impl(this);
  }

  public void setTypeSystem(TypeSystem ts) {
    if (mTypeSystem == ts) {
      return;
    }
    mTypeSystem = ts;
    setNeedsCompilation();
  }
  
  public TypeSystem getTypeSystem() {
    return mTypeSystem;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("org.apache.uima.analysis_engine.impl.ResultSpecification_impl:\n);");
    sb.append("needsCompilation = ").append(needsCompilation).append("\n");
    sb.append("lang2int = ").append(lang2int).append("\n");
    sb.append("name2tof_langs = ").append(name2tof_langs).append("\n");
    sb.append("withSubtypesName2tof_langs = ").append(withSubtypesName2tof_langs).append("\n");
    sb.append("mTypeSystem = ").append(mTypeSystem).append("\n");
    return sb.toString();
  }
  
  /**
   * Compute the feature/type + language intersection of two result specs
   * Result-spec 2 is the more-or-less constant spec from the primitive's capability outputs
   *   it can change if the type system changes... causing new 'inheritance"
   *   
   * Language intersection is done on a per-type-or-feature basis:
   *   Each is a set of languages, interpreted as a "Union".
   *     If the set contains x-unspecified - it is taken to mean all languages
   *     if the set contains XX - it is taken to mean the union of all sublanguages XX-yy
   *     
   * package scope
   */

  static ResultSpecification_impl intersect(ResultSpecification rs1in, ResultSpecification_impl rs2in) {
    ResultSpecification_impl rs1 = (ResultSpecification_impl) rs1in;
    ResultSpecification_impl rs2 = (ResultSpecification_impl) rs2in;
    ResultSpecification_impl newRs = new ResultSpecification_impl(rs1.getTypeSystem());
    
    rs1.compileIfNeeded();  // compile to make the next tests for type intersecting work
    rs2.compileIfNeeded();
    
    // iterate over all types and features in this component's result set
    for (Map.Entry<String, ToF_Languages> item : rs2.availName2tof_langs().entrySet()) {
      String rs2tof = item.getKey();
      ToF_Languages rs2Langs = item.getValue();
      // see if in other resultSpec
      ToF_Languages rs1Langs = rs1.getLanguagesForTypeOrFeature(rs2tof);
      if (rs1Langs == null) {
        continue;
      }

      // Type or Feature is in both; intersect the languages
      // if either has language x-unspecified, use the other's language spec.
      if (rs1Langs.languages.get(ResultSpecification_impl.UNSPECIFIED_LANGUAGE_INDEX)) {
        newRs.addClonedToF_Languages(rs2Langs, rs2);
        continue;
      }
      if (rs2Langs.languages.get(ResultSpecification_impl.UNSPECIFIED_LANGUAGE_INDEX)) {
        newRs.addClonedToF_Languages(rs1Langs, rs1);
        continue;
      }

      // Intersect languages - neither has x-unspecified

      List<String> rsltLangs = computeResultLangIntersection(rs1, rs1Langs, rs2, rs2Langs);
 
      if (rsltLangs.size() > 0) {
        newRs.addResultTypeOrFeature(rs2Langs.tof, rsltLangs.toArray(new String[rsltLangs.size()]));
      }
    }
    return newRs;
  }
  
  private static List<String> computeResultLangIntersection(
      ResultSpecification_impl rs1, ToF_Languages rs1Langs,     
      ResultSpecification_impl rs2, ToF_Languages rs2Langs) {

    BitSet rs1bs = rs1Langs.languages;
    BitSet rs2bs = rs2Langs.languages;
    List<String> rsltLangs = new ArrayList<String>();

    // because we don't have a list of languages as "Strings",
    // iterate over all the languages, and skip those not in this
    // type-or-feature
    for (Map.Entry<String, Integer> langIndex2 : rs2.lang2int.entrySet()) {
      if (!rs2bs.get(langIndex2.getValue())) {
        continue;
      }

      // String intersectLang = intersectLanguages(langIndex.getKey(),
      // rs1Langs, rs2Langs);

      String thisLang = langIndex2.getKey();
      if (rs1bs.get(rs1.getLanguageIndex(thisLang))) {
        rsltLangs.add(thisLang);
        continue;
      }

      // thisLang is not in the set of rs1 languages, but it might still be
      // in the intersection, if thisLang is not a base form, and the base
      // form
      // *is* in the set of rs1 languages
      String baseLang = getBaseLanguage(thisLang);
      if (baseLang != thisLang) { // thisLang is not a base form
        if (rs1bs.get(rs1.getLanguageIndex(baseLang))) {
          rsltLangs.add(thisLang);
          continue;
        }
      }
    }
    
    // add in more specific langs in rs1 matching general lang in rs2
   
    // because we don't have a list of languages as "Strings",
    // iterate over all the languages, and skip those not in this
    // type-or-feature
    for (Map.Entry<String, Integer> langIndex1 : rs1.lang2int.entrySet()) {
      if (!rs1bs.get(langIndex1.getValue())) {
        continue;
      }

      String rsLang1 = langIndex1.getKey();
      if (rs2bs.get(rs2.getLanguageIndex(rsLang1))) {
        continue;  // skip this if already would be in intersection
      }
      String baseLang1 = getBaseLanguage(rsLang1);
      if (rsLang1 != baseLang1) {  // rsLang1 is not a base form
        if (rs2bs.get(rs2.getLanguageIndex(baseLang1))) {
          rsltLangs.add(rsLang1);  // add specific lang to intersection
        }
      }
    }
    return rsltLangs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ResultSpecification_impl other = (ResultSpecification_impl) obj;
    if (lang2int == null) {
      if (other.lang2int != null) {
        return false;
      }
    }
    if (mTypeSystem == null) {
      if (other.mTypeSystem != null) {
        return false;
      }
    } else if (mTypeSystem != other.mTypeSystem) {
      return false;
    }
    if (name2tof_langs == null) {
      if (other.name2tof_langs != null) {
        return false;
      }
    } 
    this.compileIfNeeded();
    other.compileIfNeeded();
    
    if (withSubtypesName2tof_langs == null) {
      if (other.withSubtypesName2tof_langs != null) {
        return false;
      }
    }
    
    if (availName2tof_langs().size() != other.availName2tof_langs().size()) {
      return false;
    }
    
    // iterate over all types and features in this 
    for (Map.Entry<String, ToF_Languages> item : availName2tof_langs().entrySet()) {
      String tof = item.getKey();
      ToF_Languages toflangs = item.getValue();
      ToF_Languages otherToflangs = other.availName2tof_langs().get(tof);
      BitSet thisBs = toflangs.languages;
      BitSet otherBs = otherToflangs.languages;
      if (thisBs.cardinality() != otherBs.cardinality()) {
        return false;
      }
      for (Map.Entry<String, Integer>l2ie : lang2int.entrySet()) {
        if (thisBs.get(l2ie.getValue())) {
          if (!otherBs.get(other.lang2int.get(l2ie.getKey()))) {
            return false;
          }
        }
      }
    }
    
    return true;
  }
  
  /**
   * Hash code not implemented
   * @return
   */
  public int hashcode() {
    throw new UnsupportedOperationException();
  }
}
