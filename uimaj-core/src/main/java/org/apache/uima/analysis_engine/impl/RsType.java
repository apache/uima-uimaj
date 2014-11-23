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

import static org.apache.uima.analysis_engine.impl.ResultSpecification_impl.equalsOrBothNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * Holds types and/or features with language specs
 * 
 * These are expected to be sparse with respect to the complete type system
 * 
 */

public class RsType {
    
  public final static List<Feature> EMPTY_FEATURE_LIST = new ArrayList<Feature>(0);
  
  final String typeName;
  boolean isAllFeatures = false;
  boolean isSpecified = false;  // true if type is specified by itself, without a feature
  RsLangs languagesAllFeat = null;     // languages for this type w/ allFeat   null means x-unspec
  RsLangs languagesNotAllFeat = null;  // languages for this type w/o allFeat  null means x-unspec
  RsFeats features = null; 
  
  RsType(String name) {
    typeName = name;
  }
  
  RsType(RsType original) {
    typeName = original.typeName;
    isAllFeatures = original.isAllFeatures;
    isSpecified = original.isSpecified;
    languagesAllFeat = RsLangs.createOrNull(original.languagesAllFeat);
    languagesNotAllFeat = RsLangs.createOrNull(original.languagesNotAllFeat);
    features = (original.features == null) ? null : new RsFeats(original.features);
  }
    
  /**
   * 
   * @param shortFeatName
   * @param lang
   * @return true if lang subsumed by langs of the feature 
   *                 or of the type with all-feats specified
   */
  boolean subsumesLanguageInFeat(String shortFeatName, String lang) {
    if (isAllFeatures && RsLangs.subsumes(languagesAllFeat, lang)) {
      return true;
    }
    RsFeat f = getFeat(shortFeatName);
    if (null == f) {
      return false;
    }
    return RsLangs.subsumes(f.languages, lang);
  }
  
  RsFeat getFeat(String shortFeatName) {
    if (null == features) {
      return null;
    }
    return features.get(shortFeatName);
  }
   
  /**
   * returns the Features for a type in a result spec 
   * @param ts The type system, may be null
   * @return list of features for a type in a result spec
   */
  List<Feature> getAllAppropriateFeatures(final TypeSystem ts) {
    if (null == ts) {
      return EMPTY_FEATURE_LIST;
    }
    Type t = ts.getType(typeName);
    return (null == t) ? EMPTY_FEATURE_LIST : t.getFeatures();
  }
  
  boolean hasAllFeaturesExplicitly(TypeSystem ts) {
//    if (features == null || features.features == null || features.features.size() == 0 || ts == null) {
//      return false;
//    }
    List<Feature> all = getAllAppropriateFeatures(ts);
    if (all.size() == 0) {
      if (features == null || features.features == null || features.features.size() == 0 || ts == null) {
        return true;
      }
      return false;
    }
    int fz = (features == null || features.features == null) ? 0 : features.features.size();
    if (fz == all.size()) {
      for (Feature f : all) {
        if (!features.contains(typeName, f.getShortName())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  boolean allFeaturesHaveSameLangs() {
    if (features == null) {
      return false;
    }
    int fz = features.size();
    if (fz == 0) { 
      return false;
    }
    if (fz == 1) {
      return true;
    }
    List<RsFeat> rsf = features.features;
    RsLangs l = rsf.get(0).languages;
    
    for (int i = 1; i < fz; i++) {
      RsLangs fl = rsf.get(i).languages;
      if (!equalsOrBothNull(l, fl)) {
        return false;
      }
    }
    return true;
  }
}

