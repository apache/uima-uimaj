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

import junit.framework.TestCase;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.TypeSystemImpl;

/**
 * Test various kinds of inheritance issues 
 * involving result specifications and type system
 */
public class ResultSpecWithTypeSystemTest extends TestCase {
  
  private static class TofLs {
    TypeOrFeature tof;
    String[] langs;
  }
  
  // languages
  private static final String EN = "en";
  private static final String X  = "x-unspecified";
  private static final String EN_US = "en-us";
  private static final String PTBR = "pt-br";
  private static final String I = "I";  // split designator, not a language
	
	// types
  
  private static final TypeSystemMgr ts = new TypeSystemImpl();
  private static final Type t1 = ts.addType("T1", ts.getTopType());
  private static final Type t2 = ts.addType("T2", t1);
  private static final Type t3 = ts.addType("T3", t2);
  private static final Type t4 = ts.addType("T4", t1);  // doesn't inherit from t2
  private static final Feature f1 = ts.addFeature("F1", t1, t1);
  private static final Feature f2 = ts.addFeature("F2", t2, t3);
  private static final Feature f3 = ts.addFeature("F3", t3, t3);
  private static final Feature f4 = ts.addFeature("F4", t4, t4);
  static {ts.commit();};
  
  // TypeOrFeature instances
  private static TypeOrFeature makeTof(String name, boolean isType, boolean allFeats) {
    TypeOrFeature r = new TypeOrFeature_impl();
    r.setName(name);
    r.setType(isType);
    r.setAllAnnotatorFeatures(allFeats);
    return r;
  }
  
  private static final TypeOrFeature tofT1allFeat = makeTof("T1", true, true);
  private static final TypeOrFeature tofT1        = makeTof("T1", true, false);
  private static final TypeOrFeature tofT2allFeat = makeTof("T2", true, true);
  private static final TypeOrFeature tofT2        = makeTof("T2", true, false);
  private static final TypeOrFeature tofT3allFeat = makeTof("T3", true, true);
  private static final TypeOrFeature tofT3        = makeTof("T3", true, false);
  private static final TypeOrFeature tofT4allFeat = makeTof("T4", true, true);
  private static final TypeOrFeature tofT4        = makeTof("T4", true, false);
  
  
  private static final TypeOrFeature tofF1        = makeTof("T1:F1", false, false);
  private static final TypeOrFeature tofF2        = makeTof("T2:F2", false, false);
  private static final TypeOrFeature tofF3        = makeTof("T3:F3", false, false);
  private static final TypeOrFeature tofF4        = makeTof("T4:F4", false, false);
  private static final TypeOrFeature tofT2F1      = makeTof("T2:F1", false, false);  // feature spec'd at subtype, but exists in supertype
  private static final TypeOrFeature tofT4F1      = makeTof("T4:F1", false, false);

  static enum K { // test kind
    Contains,
    NotContain,
  }
  
  // no languages
  // check type inheritance
  public void testTypeInheritance() {
    check(tofT1allFeat, K.Contains, t1);
    check(tofT1, K.Contains, t1);
    check(tofT1, K.Contains, t2);
    check(tofT1, K.Contains, t3);
    check(tofT2, K.Contains, t3);
    check(tofT2, K.NotContain, t4);
    check(tofT1, K.Contains, t4);
  }
  
  // no languages
  // check feat inheritance  
  public void testFeatInheritance() {
    check(tofT1allFeat, K.Contains, f1);
    check(tofT1, K.NotContain, f1);
    check(tofF1, K.Contains, f1);
    check(tofT2, K.NotContain, f1);
    check(tofT2allFeat, K.Contains, f2);
    check(tofT2, K.NotContain, f2);
    check(tofT1allFeat, K.NotContain, f2);  // because allFeat on T1 doesn't include F2 which is only introduced on T2
    check(tofT2F1, K.NotContain, f1);         // feature spec'd for subtype
    check(tofT2F1, K.Contains, "T2:F1");
    check(tofT2F1, K.Contains, "T3:F1");      // oops, features not inheriting
    check(tofT1allFeat, K.NotContain, f4);  // because allFeat on T1 doesn't include F4 which is only introduced on T4
    check(tofT2allFeat, K.NotContain, f4);
    check(tofT1, K.NotContain, f4);
  }
  
  // languages
  // check type inheritance
  public void testTypeInheritanceL() {
    check(tofT1allFeat, X, K.Contains, t1, X);
    check(tofT1allFeat, EN, K.NotContain, t1, X);
    check(tofT1allFeat, EN, K.Contains, t1, EN);
    check(tofT1allFeat, EN, K.Contains, t1, EN_US);
    check(tofT1allFeat, EN_US, K.NotContain, t1, EN);
    check(tofT1, EN, K.Contains, t2, EN);
    check(tofT1, EN, K.Contains, t2, EN_US);

    TofLs[] tofls = aT(tofT1allFeat, X, tofT2, X);
    check(tofls, K.NotContain, f2);    // bad

  }
  
  public void testFeatInheritanceL() {
    check(tofT1allFeat, X, K.Contains, f1, X);
    check(tofT1allFeat, EN, K.NotContain, f1, X);
    check(tofT1allFeat, EN, K.Contains, f1, EN);
    check(tofT1allFeat, EN, K.Contains, f1, EN_US);

    TofLs[] tofls =aT(tofT1allFeat, X, tofT2, EN);
    check(tofls, K.NotContain, f2, X);
    check(tofls, K.NotContain, f2, EN);
    check(tofls, K.NotContain, f2, EN_US);

    tofls =aT(tofT1allFeat, X, tofF2, EN);
    check(tofls, K.NotContain, f2, X);
    check(tofls, K.Contains, f2, EN);
    check(tofls, K.Contains, f2, EN_US);

    tofls = aT(tofT1allFeat, EN, tofT2allFeat, X);
    check(tofls, K.Contains, f2, X);   
    check(tofls, K.Contains, f2, EN);
    check(tofls, K.Contains, f2, EN_US);
    
    tofls = aT(tofT2allFeat, EN, tofF1, X);
    check(tofls, K.Contains, f1, X);
    check(tofls, K.Contains, f1, EN);
    check(tofls, K.Contains, f1, EN_US);
    
    tofls = aT(tofT1allFeat, EN_US, tofT2, EN);
    check(tofls, K.NotContain, f2, X);
    check(tofls, K.NotContain, f2, EN);  //broken
    check(tofls, K.NotContain, f2, EN_US);

    tofls = aT(tofT1, X, tofT2, EN_US);
    check(tofls, K.NotContain, f2, X);
    check(tofls, K.NotContain, f2, EN);
    check(tofls, K.NotContain, f2, EN_US);

    tofls = aT(tofF1, X, tofT2, EN_US);
    check(tofls, K.NotContain, f2, X);
    check(tofls, K.NotContain, f2, EN);
    check(tofls, K.NotContain, f2, EN_US);

    tofls = aT(tofF1, EN, tofF2, EN_US);
    check(tofls, K.NotContain, "T2:F1", X);  
    check(tofls, K.Contains, "T2:F1", EN);  
    check(tofls, K.Contains, "T2:F1", EN_US);  
    check(tofls, K.NotContain, f2, EN);  //broken
    check(tofls, K.Contains, f2, EN_US);
    
    

  }
  
  void check(TypeOrFeature tof, K testKind, Object t) {
    check(aT(tof, X), testKind, t, X);
  }
  
  void check(TofLs[] tofls, K testKind, Object t) {
    check(tofls, testKind, t, X);
  }
  
  void check(TypeOrFeature tof, String l1, K testKind, Object t, String l) {
    check(aT(tof, l1), testKind, t, l);
  }
  
  void check(TofLs[] tofls, K testKind, Object candidate, String lang) {
    String candidateName = (candidate instanceof Type) ? ((Type)candidate).getName() :
                           (candidate instanceof Feature) ? ((Feature)candidate).getName() :
                           (String) candidate;
    check(tofls, testKind, candidateName, lang);
  }
  
  void check(TofLs[] tofLss, K testKind, String candidateName, String lang) {
    boolean isType = -1 == candidateName.indexOf(TypeSystem.FEATURE_SEPARATOR);
    ResultSpecification_impl rs = new ResultSpecification_impl();
    rs.setTypeSystem(ts);
    for (TofLs tofLs : tofLss) {
      rs.addResultTypeOrFeature(tofLs.tof, tofLs.langs);
    }
      
    switch (testKind) {
    case Contains :
      assertTrue(isType ? rs.containsType(candidateName, lang) : rs.containsFeature(candidateName, lang));
      break;
    case NotContain :
      assertFalse(isType ? rs.containsType(candidateName, lang) : rs.containsFeature(candidateName, lang));
      break;
    }
  
    
  }
  
  /**
   * Compose sets of { tof, lang, tof2, lang2, ...} into one object
   * Also handle langs:  {tof, aL{lang1, lang2), ...)
   * @param tofls
   * @return
   */
  TofLs[] aT(Object... tofls) {
    TofLs[] r = new TofLs[tofls.length / 2];
    int j = 0;
    for (int i = 0; i < tofls.length; i = i + 2) {
      r[j] = new TofLs();
      r[j].tof = (TypeOrFeature)tofls[i];
      Object ls = tofls[i+1];
      r[j++].langs = (ls instanceof String) ? aL(ls) : (String[]) ls;
    }
    return r;
  }
  
  String[] aL(Object... langs) {
    String[] r = new String[langs.length];
    System.arraycopy(langs, 0, r, 0, langs.length);
    return r;
  }
}
