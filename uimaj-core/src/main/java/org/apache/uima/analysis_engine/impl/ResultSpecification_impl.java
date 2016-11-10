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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.Language;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link ResultSpecification}.
 * 
 * Notes on the implementation
 * 
 * Result Specifications (result specs, rs) are closely tied to capability specifications.
 * 
 * They consist of instances of
 *   TypeOrFeatures and associated languages for which they are set.
 *   
 * This impl supports removing previously added types and features
 * for particular languages.
 * 
 * There are two forms of the data kept:
 *   The data as it was provided to set the items in the result spec
 *     This form is used when removing previously added things
 *     
 *   The data after a type system has been provided, expanded to cover
 *     the various implied settings, due to either
 *       all Features flag on a type or
 *       the type/subtype hierarchy in the type system
 *         
 *   TypesOrFeatures are:
 *     typeXXX:FeatureYYY - specifying a particular feature of a type
 *       (Corner case: typeXXX:FeatureYYY doesn't imply there's a
 *                     typeXXX allFeat nor a
 *                     typeXXX w/o allFeat.
 *                     
 *     typeXXX with allFeatures - a shorthand for specifying
 *       typeXXX and  
 *       typeXXX:FeatureYYY for all features YYY defined for typeXXX
 *         (Corner case: excludes features ZZZ defined only in subtype of typeXXX)
 *     typeXXX without allFeatures (w/o allFeat) - specifies a type, but says nothing about the features
 *       This is specifiable in the XML.  It means:
 *         The type is produced/needed but there's no information about the features that
 *           are to be produced or used      
 * 
 *       containsType typeXXX  
 *         returns true if typeXXX is in the RS, with or without the allFeats flag
 *         returns false if only features involving typeXXX are specified
 *         
 *  Intersection is done on fully expanded representations.     
 * 
 * There are two kinds of inheritance used
 *   Assuming there's a type system (which must be present when intersection is used), there's type/subtype
 *     This means that if a resultSpec is set for typeXXX, then the containsType(typeYYY) 
 *     returns true if typeYYY is a subtype of typeXXX.
 *     This also needs to work for typeXXX:featZZZ; containsFeature(typeYYY:featZZZ)
 *     returns true if type YYY is a subtype of typeXXX.
 *     
 *   Languages have a 3 level hierarchy:
 *     x-unspecified - the same as no language being specified.
 *       If the resultSpec contains typeXXX for language x-unspecified,
 *       containsType(typeXXX, languageLLL) returns true, for any languageLLL
 *     a "base" language, without a '-', e.g. "en"
 *     a sub-language, with one or more '-', e.g., "en-us"
 *     
 *     The rules for matching languages only handle these three levels of inheritance.
 *       (Corner case: 3 or more level language hierarchy are treated as 3 level hierarchies 
 *        eg. zh-Hant-HK (Traditional Chinese as used in Hong Kong)
 *        See http://www.w3.org/International/articles/language-tags/Overview.en.php )
 * 
 * Design considerations and assumptions
 *   Many instances of this class are made, sometimes via cloning.
 *   Most uses only use types, not type:features
 *   Most don't use languages
 *   A small subset of the possible types and type:features is specified explicitly
 *   Sometimes types and/or features are deleted. (language capability flow deletes types and/or features)
 * 
 * Types and Features are kept on a per-language basis.  Language can include a special value,
 * x-unspecified, which "matches" any other language.
 * 
 * Set operations among different result specs:
 *   Union: done in aggregates over result-specs derived from input capabilities of delegates
 *   Intersection: done for primitive components, over result-spec derived from output capability of the primitive
 *   remove: one type or feature (used by language capability flow)
 *     (Corner cases
 *        removing typeXXX doesn't remove typeXXX:featureYYY
 *        removing typeXXX allFeat doesn't remove typeXXX w/o allFeat (may have different languages)
 *        removing typeXXX w/o allFeat doesn't remove typeXXX allFeat 
 *        
 * The compiled version is used in containsType, containsFeature testing, and is used when
 * computing intersection.
 */

public final class ResultSpecification_impl extends MetaDataObject_impl implements
        ResultSpecification {

  private static final long serialVersionUID = 8516517600467270594L;

  /**
   * main language separator e.g 'en' and 'en-US'
   */  
    
  private static final String[] ARRAY_X_UNSPEC = new String[]{Language.UNSPECIFIED_LANGUAGE};
  
  /**
   * form used in hash table of compilied version to represent x-unspecified
   * (can't use null - that means entry not in table)
   */
  private static final RsLangs compiledXunspecified = RsLangs.createSharableEmpty();  // a distinct object
  
  /**
   * used for empty type subsumption lists in subtype iterator
   */
  public static final List<Type> EMPTY_TYPE_LIST = new ArrayList<Type>(0);
  
  /**
   * For this Result-specification, the collection of language-sets
   * Uncompiled format
   */
  private final RsTypesMap rsTypesMap;
  
  /**
   * The type system used to compute the subtypes and allAnnotatorFeatures of types
   */
  private TypeSystem mTypeSystem = null;

  // compiled forms
  private boolean needsCompilation = true;
  private final Map<String, RsLangs> rsCompiled;

  public ResultSpecification_impl() {
    rsTypesMap = new RsTypesMap();
    rsCompiled = new HashMap<String, RsLangs>();
  }

  /**
   * Constructor specifying the type system
   *   this should always be used in preference to the 0 argument version
   *   if the type system is available.  Otherwise, the type system *must*
   *   be set via a method call prior to querying the result spec, with the
   *   one exception of the method getResultTypesAndFeaturesWithoutCompiling
   * @param aTypeSystem -
   */
  public ResultSpecification_impl(TypeSystem aTypeSystem) {
    this();
    mTypeSystem = aTypeSystem;
  }

  /**
   * copies the result spec passed in so that updates to it
   *   don't affect the original
   * @param original
   */
  private ResultSpecification_impl(ResultSpecification_impl original) {
    mTypeSystem = original.mTypeSystem;    // not cloned
    rsTypesMap = new RsTypesMap(original.rsTypesMap);
    needsCompilation = original.needsCompilation;
    rsCompiled = new HashMap<String, RsLangs>(original.rsCompiled);     
    for (Map.Entry<String, RsLangs> e : rsCompiled.entrySet()) {
      e.getValue().setShared();
    }
  }
      
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures()
   */
  public TypeOrFeature[] getResultTypesAndFeatures() {
    return getResultTypesAndFeatures(true, null);
  }    
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#getResultTypesAndFeatures(java.lang.String)
   * May contain near-duplicates - same type, but with different settings of allannotatorfeatures
   *   (only if they have different languages)
   */
  public TypeOrFeature[] getResultTypesAndFeatures(String language) {
    return getResultTypesAndFeatures(false, language);
  }
  
  private TypeOrFeature[] getResultTypesAndFeatures(boolean skipLanguageFilter, String language) {
    List<TypeOrFeature> r = new ArrayList<TypeOrFeature>();
    if (rsTypesMap.nbrOfTypes() == 0 && !needsCompilation) {
      // being called on results of intersection
      // probably by a test case, not a normal call
      // attempt to construct a plausible representation
      reconstructRsTypesFromCompiled();
    }
    for (RsType t : rsTypesMap) {
      if (t.isAllFeatures && (skipLanguageFilter || RsLangs.subsumes(t.languagesAllFeat, language))) {
        r.add(createTypeOrFeature(t.typeName, true, true));
      }
      if (t.isSpecified && (skipLanguageFilter || RsLangs.subsumes(t.languagesNotAllFeat, language))) {
        if (!(t.isAllFeatures && t.languagesAllFeat.equals(t.languagesNotAllFeat)))  // don't make a duplicate
        r.add(createTypeOrFeature(t.typeName, true, false));
      }
      if (t.features != null) {
        for (RsFeat f : t.features) {
          if (skipLanguageFilter || f.subsumes(language))
            r.add(createTypeOrFeature(t.typeName, f.shortFeatName));
        }
      }
    }
    return r.toArray(new TypeOrFeature[r.size()]);
  }

  private void reconstructRsTypesFromCompiled() {
    // First, recompute basic rsTypes and rsFeatures hooked to types
    for (Entry<String, RsLangs> e : rsCompiled.entrySet()) {
      String tofName = e.getKey();
      int b = tofName.indexOf(TypeSystem.FEATURE_SEPARATOR);
      if (b == -1) {
        rsTypesMap.add(tofName, false, e.getValue(), false);  
      } else {
        String typeName = tofName.substring(0, b);
        String featName = tofName.substring(b+1);
        rsTypesMap.add(typeName, featName, e.getValue(), false);
      }
    }
    
    // Second merge 
    //   if the types features all have the same lang and are all the features,
    //      set the allFeats flag, and merge in the langs
    for (RsType t : rsTypesMap) {
      if (t.hasAllFeaturesExplicitly(mTypeSystem) && t.allFeaturesHaveSameLangs()) {
        t.isAllFeatures = true;
        RsLangs l = t.features.features.get(0).languages;
        if (l != null && RsLangs.isEmpty(l)) {
          l = null;
        }
        if (l != null) {
          if (t.languagesAllFeat == null) {
            t.languagesAllFeat = RsLangs.createOrNull(l);
          } else {  // merge in langs l
            t.languagesAllFeat = RsLangs.addAll(t.languagesAllFeat, l);
          }
        }        
        t.features = null;
      }
      if (t.isSpecified && t.isAllFeatures && equalsOrBothNull(t.languagesAllFeat, t.languagesNotAllFeat)) {
        t.isSpecified = false;
        t.languagesNotAllFeat = null;
      }
    }    
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures) {
    setResultTypesAndFeatures(aTypesAndFeatures, ARRAY_X_UNSPEC);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#setResultTypesAndFeatures(org.apache.uima.analysis_engine.TypeOrFeature[],
   *      java.lang.String[])
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures, String[] aLanguageIDs) {
       
    for (TypeOrFeature tof : aTypesAndFeatures) {
      addResultTof(tof, aLanguageIDs, true);
    }
  }
    
  private void addResultTof(TypeOrFeature tof, String[] langs, boolean replace) {
    String name = tof.getName();
    String typeName = null;
    String shortFeatName = null;
    int i = name.indexOf(TypeSystem.FEATURE_SEPARATOR);
    if (i < 0) {
      typeName = name;
      rsTypesMap.add(typeName, tof.isAllAnnotatorFeatures(), langs, replace);
    } else {
      typeName = name.substring(0, i);
      shortFeatName = name.substring(i+1);
      rsTypesMap.add(typeName, shortFeatName, langs, replace);
    }
    setCompileNeeded();
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature) {
    addResultTypeOrFeature(aTypeOrFeature, ARRAY_X_UNSPEC);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature,
   *      java.lang.String[])
   *      
   * Note: Javadoc makes assumption that there's one tof per type, but this design allows 2 (one with allAnnotatorFeatures set or not).
   */
  public void addResultTypeOrFeature(TypeOrFeature tof, String[] languages) {
    addResultTof(tof, languages, true); 
 }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean)
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures) {
    addResultType(aTypeName, aAllAnnotatorFeatures, ARRAY_X_UNSPEC);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultType(java.lang.String,
   *      boolean, java.lang.String[])
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures, String[] aLanguageIDs) {
    rsTypesMap.add(aTypeName, aAllAnnotatorFeatures, aLanguageIDs, false);
    setCompileNeeded();
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String)
   */
  public void addResultFeature(String aFullFeatureName) {
    addResultFeature(aFullFeatureName, ARRAY_X_UNSPEC);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#addResultFeature(java.lang.String,
   *      java.lang.String[])
   */
  public void addResultFeature(String aFullFeatureName, String[] aLanguageIDs) {
    String typeName = null;
    String shortFeatName = null;
    int i = aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR);
    typeName = aFullFeatureName.substring(0, i);
    shortFeatName = aFullFeatureName.substring(i+1);
    rsTypesMap.add(typeName, shortFeatName, aLanguageIDs, false);
    setCompileNeeded();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#compile(org.apache.uima.cas.TypeSystem)
   * @deprecated no longer needed, remove call to this
   */
  @Deprecated
  public void compile(TypeSystem aTypeSystem) {
    setTypeSystem(aTypeSystem);
    compile();
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
  
  private TypeOrFeature createTypeOrFeature(String typeName, String featureName) {
    return createTypeOrFeature(typeName + TypeSystem.FEATURE_SEPARATOR + featureName, false, false);
  }
  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String)
   */
  public boolean containsType(String aTypeName) {
    return containsType(aTypeName, Language.UNSPECIFIED_LANGUAGE);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsType(java.lang.String,java.lang.String)
   * method:
   *   
   *   for each type (with all-feat, without all-feat):
   *     for each type, and supertypes 
   *       check if one of the resultSpec languages subsumes the given language.
   *         if so, return true
   *   return false;
   *   
   *   But: cache this: key = int[2]: type#, langi#, value = true/false
   *   
   */
  
  // TODO check cache, normalize language
  public boolean containsType(String aTypeName, String aLanguage) {
    if (aTypeName.indexOf(TypeSystem.FEATURE_SEPARATOR) != -1) {
      return false; // check against someone passing a feature name here
    }
    compileIfNeeded();
    return hasLanguage(rsCompiled.get(aTypeName), aLanguage);
  }

  
  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String)
   */
  public boolean containsFeature(String aFullFeatureName) {
    return containsFeature(aFullFeatureName, Language.UNSPECIFIED_LANGUAGE);
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#containsFeature(java.lang.String,java.lang.String)
   */

  public boolean containsFeature(String aFullFeatureName, String aLanguage) {
    int i = aFullFeatureName.indexOf(TypeSystem.FEATURE_SEPARATOR);
    if (i == -1)
      return false; // check against someone passing a type name here
    compileIfNeeded();
    boolean found = hasLanguage(rsCompiled.get(aFullFeatureName), aLanguage);
    if (found) {
      return true;
    }
    // this next bit is to keep the behavior in the case where the type system isn't specified, 
    // the same.
    RsType t = rsTypesMap.getRsType(aFullFeatureName.substring(0, i)); // look for just the type name
    if (null != t && t.isAllFeatures && RsLangs.subsumes(t.languagesAllFeat, aLanguage)) {
      return true;
    }
    return false;
  }

  /**
   * 
   * @param rsLangs
   * @param language
   * @return true if the rsLangs subsumes the language 
   */
  private static boolean hasLanguage(RsLangs rsLangs, String language) {
    language = Language.normalize(language);
    // rsLangs == null means there was no entry in the 
    //   rsCompiled map for this type
    //   It does NOT mean x-unspecified
    return (rsLangs == null) ? false : (RsLangs.subsumes(rsLangs, language));
  }


  /**
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("resultSpecification", "",
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
      
      for (TypeOrFeature tof : tofs) {
        String typeName = tof.getName();
        if (!tof.isType()) {
          int i = typeName.indexOf(TypeSystem.FEATURE_SEPARATOR);
          String shortFeatName = typeName.substring(i+1);
          typeName = typeName.substring(0, i);
          rsTypesMap.add(typeName, shortFeatName, capability.getLanguagesSupported(), false);
        } else {
          rsTypesMap.add(typeName, tof.isAllAnnotatorFeatures(), capability.getLanguagesSupported(), false);
        }
      }
    }
    setCompileNeeded();
  }

  /**
   * @see org.apache.uima.analysis_engine.ResultSpecification#removeTypeOrFeature(org.apache.uima.analysis_engine.TypeOrFeature)
   * This removes the type or feature for all languages.
   * Beware: there are two possible ToFs one with allFeatures set or not (if they have different languages).
   */
  public void removeTypeOrFeature(TypeOrFeature tof) {
    String name = tof.getName();
    if (tof.isType()) {
      rsTypesMap.remove(name);
    } else {
      int i = name.indexOf(TypeSystem.FEATURE_SEPARATOR);
      rsTypesMap.remove(name.substring(0, i), name.substring(i+1));
    }
    setCompileNeeded();
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
    mTypeSystem = ts;
    setCompileNeeded();
  }
  
  public TypeSystem getTypeSystem() {
    return mTypeSystem;
  }
  
  @SuppressWarnings("unchecked")
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("org.apache.uima.analysis_engine.impl.ResultSpecification_impl:\n");
    sb.append("  needsCompilation = ").append(needsCompilation).append('\n');
//    sb.append("lang2int = ").append(lang2int).append("\n");
//    sb.append("name2tof_langs = ").append(name2tof_langs).append("\n");
//    sb.append("withSubtypesName2tof_langs = ").append(withSubtypesName2tof_langs).append("\n");
    sb.append("\nrsTofLangs:\n");
    if (needsCompilation) {
      sb.append(rsTypesMap);
    } else {
      Object [] sorted = rsCompiled.entrySet().toArray();
      Arrays.sort(sorted, new Comparator<Object>() {
        public int compare(Object object1, Object object2) {
          return ((Entry<String, RsLangs>)object1).getKey().
          compareTo(((Entry<String, RsLangs>)object2).getKey());
        }
      });
      for (Object o : sorted) {
        Entry<String, RsLangs> e = (Entry<String, RsLangs>) o;
        String k = e.getKey();
        k = k + "        ".substring(k.length()%8);
        sb.append(" key: ").append(k).append("  value: ").append(e.getValue()).append('\n');
      }
    }
    sb.append("\n\nmTypeSystem = ").append(mTypeSystem).append('\n');
    return sb.toString();
  }

  private void compileIfNeeded() {
    if (needsCompilation) {
      needsCompilation = false;
      compile();
    }
  }
  
  private void setCompileNeeded() {
    needsCompilation = true;
    rsCompiled.clear(); 
  }
  
  /**
   * create a fully expanded version of this result spec
   */
  
  private void compile() {
    for (RsType rst : rsTypesMap) {
      if (rst.isSpecified) {
        addCompiledFormForTypeAndItsSubtypes(rst, rst.languagesNotAllFeat);
      }
      if (rst.isAllFeatures) {
        addCompiledFormForTypeAndItsSubtypes(rst, rst.languagesAllFeat);
        
        for (Feature f : rst.getAllAppropriateFeatures(mTypeSystem)) {
          addCompiledFormForFeatureAndItsSubtypes(rst, f.getShortName(), rst.languagesAllFeat);          
        }
      }
      if (rst.features != null) {
        for (RsFeat rsf : rst.features) {
          addCompiledFormForFeatureAndItsSubtypes(rst, rsf.shortFeatName, rsf.languages);
        }
      }   
    }
  }
  
  private void addCompiledFormForTypeAndItsSubtypes(RsType rst, RsLangs langs) {
    addCompiledFormEntry(rst.typeName, langs);
    for (String subtypeName : subtypeNames(rst.typeName)) {
      addCompiledFormEntry(subtypeName, langs);
    }
  }
  
  /**
   * Note: the string typeXXX:featYYY may not be in the type system.
   *   For instance, if featYYY is introduced in type Foo, we could have a spec of
   *     FooSubtype:featYYY; this string could be unique to the result spec
   * @param rst
   * @param shortFeatName
   * @param langs
   */
  private void addCompiledFormForFeatureAndItsSubtypes(RsType rst, String shortFeatName, RsLangs langs) {
    addCompiledFormEntry(RsFullFeatNames.getFullFeatName(rst.typeName, shortFeatName), langs);
    for (String subtypeName : subtypeNames(rst.typeName)) {
      addCompiledFormEntry(RsFullFeatNames.getFullFeatName(subtypeName, shortFeatName), langs);  
    }
  }
      
  /**
   * Adds languages to a type or feature
   * @param tofName
   * @param languagesToAdd
   */
  private void addCompiledFormEntry(String tofName, RsLangs languagesToAdd) {
    if (languagesToAdd == null) {
      languagesToAdd = compiledXunspecified;
    }
    RsLangs rsLangs = rsCompiled.get(tofName);
    if (null == rsLangs) {
      if (languagesToAdd != compiledXunspecified) {
        languagesToAdd.setShared();
      }
      rsCompiled.put(tofName, languagesToAdd);
      return;
    }
    RsLangs.addAll(rsLangs, languagesToAdd);
  }
  
  private Iterable<String> subtypeNames(final String typeName) {
    final TypeSystemImpl ts = (TypeSystemImpl) mTypeSystem;
    return new Iterable<String>() {

      public Iterator<String> iterator() {
        return new Iterator<String>() {
          Type t = (null == ts) ? null : ts.getType(typeName);         
          List<Type> subtypes = (null == ts) ? EMPTY_TYPE_LIST 
                              : (null == t ) ? EMPTY_TYPE_LIST
                              : ts.getProperlySubsumedTypes(t);
          int  i = 0;

          public boolean hasNext() {
            return i < subtypes.size();
          }

          public String next() {
            return subtypes.get(i++).getName();
          }

          public void remove() {throw new UnsupportedOperationException();}
          
        };
      }
    };
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
   */

  
  ResultSpecification_impl intersect(ResultSpecification_impl rsOther) {
    
    ResultSpecification_impl r = new ResultSpecification_impl();
    r.setTypeSystem(rsOther.mTypeSystem);
    
    r.compileIfNeeded();
    rsOther.compileIfNeeded();
    compileIfNeeded();
    
    /**
     * Iterate over other 
     */
    for (Iterator<Entry<String, RsLangs>> it = rsOther.rsCompiled.entrySet().iterator(); it.hasNext();) {
      Entry<String, RsLangs> e = it.next();
      String tofName = e.getKey();
      RsLangs otherRsLangs = e.getValue(); 
      
      /**
       * Get corresponding languages from this side
       */
      RsLangs thisRsLangs = rsCompiled.get(tofName);
      if (null == thisRsLangs) {
        continue;    // null does NOT mean x-unspecified, it means tof is not present in compiled map at all
      }
      
      /**
       * Intersect languages, with subsumption
       */
      RsLangs intersectRsLangs = thisRsLangs.intersect(otherRsLangs);      
      if (intersectRsLangs != null) {
        r.addCompiledFormEntry(tofName, intersectRsLangs);
      }
    }
    return r;
  }
  
  
  
  private boolean compiledFormEquals(ResultSpecification_impl other) {
    compileIfNeeded();
    other.compileIfNeeded();
    return rsCompiled.equals(other.rsCompiled);  // compares two maps, returns true if have same entries
  }

  @Override
  public boolean equals(Object aObj) {
    if (!(aObj instanceof ResultSpecification_impl)) {
      return false;
    }
    return compiledFormEquals((ResultSpecification_impl)aObj);
  }
  
  
  
  static boolean equalsOrBothNull(Object x, Object y) {
    if (null == x && null == y) {
      return true;
    }
    if (null != x && x.equals(y)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("HashCode not implemented for ResultSpecification_impl");
  }

}
