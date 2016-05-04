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

package org.apache.uima.jcas.test;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;

public class CASTestSetup implements AnnotatorInitializer {

  // selectors for generating various bad setups to force error checking
  public final int bad;

  public static final int BAD_MISSING_TYPE_IN_CAS = 1;

  public static final int BAD_MISSING_FEATURE_IN_CAS = 2;

  public static final int BAD_CHANGED_FEATURE_TYPE = 3;

  // Type system constants.
  public static final String TOKEN_TYPE = "x.y.z.Token";

  public static final String SUBTOK_TYPE = "SubToken";

  public static final String TOKEN_TYPE_FEAT = "ttype";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "x.y.z.TokenType";

  public static final String WORD_TYPE = "x.y.z.Word";

  public static final String SEP_TYPE = "x.y.z.Separator";

  public static final String EOS_TYPE = "x.y.z.EndOfSentence";

  public static final String SENT_TYPE = "x.y.z.Sentence";

  // public static final String INT_ARRAY_SUB = "IntArraySub";
  public static final String INT_SUB_NAME = "x.y.z.intArrayName";

  public static final String LEMMA_FEAT = "lemma";

  public static final String LEMMA_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR + LEMMA_FEAT;

  public static final String SENT_LEN_FEAT = "sentenceLength";

  public static final String SENT_LEN_FEAT_Q = SENT_TYPE + TypeSystem.FEATURE_SEPARATOR
          + SENT_LEN_FEAT;

  public static final String TOKEN_FLOAT_FEAT = "tokenFloatFeat";

  public static final String TOKEN_FLOAT_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_FLOAT_FEAT;

  public static final String LEMMA_LIST_FEAT = "lemmaList";

  public static final String LEMMA_LIST_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + LEMMA_LIST_FEAT;

  public static final String LANG_PAIR = "org.apache.lang.LanguagePair";

  public static final String LANG1 = "lang1";

  public static final String LANG2 = "lang2";

  public static final String DESCR_FEAT = "description";

  public static final String GROUP_1 = "org.apache.lang.Group1";

  public static final String GROUP_2 = "org.apache.lang.Group2";

  public static final String[] GROUP_1_LANGUAGES = { "Chinese", "Japanese", "Korean", "English",
      "French", "German", "Italian", "Spanish", "Portuguese" };

  public static final String[] GROUP_2_LANGUAGES = { "Arabic", "Czech", "Danish", "Dutch",
      "Finnish", "Greek", "Hebrew", "Hungarian", "Norwegian", "Polish", "Portuguese", "Russian",
      "Turkish" };

  // Index name constants.
  public static final String ANNOT_SET_INDEX = "Annotation Set Index";

  public static final String ANNOT_BAG_INDEX = "Annotation Bag Index";

  /**
   * Constructor for CASTestSetup.
   */
  public CASTestSetup() {
    super();
    bad = 0;
  }

  public CASTestSetup(int bad) {
    super();
    this.bad = bad;
  }

  /**
   * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(TypeSystemMgr)
   */
  public void initTypeSystem(TypeSystemMgr tsm) {
    // Add new types and features.
    Type topType = tsm.getTopType();
    Type annotType = tsm.getType(CAS.TYPE_NAME_ANNOTATION);
    Type typeArrayInt = tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    Type typeArrayRef = tsm.getType(CAS.TYPE_NAME_FS_ARRAY);
    Type typeArrayFloat = tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY);
    Type typeArrayLong = tsm.getType(CAS.TYPE_NAME_LONG_ARRAY);
    Type typeArrayDouble = tsm.getType(CAS.TYPE_NAME_DOUBLE_ARRAY);
    Type typeArrayString = tsm.getType(CAS.TYPE_NAME_STRING_ARRAY);
    Type typeInteger = tsm.getType(CAS.TYPE_NAME_INTEGER);
    Type typeFloat = tsm.getType(CAS.TYPE_NAME_FLOAT);
    Type typeDouble = tsm.getType(CAS.TYPE_NAME_DOUBLE);
    Type typeLong = tsm.getType(CAS.TYPE_NAME_LONG);
    Type typeString = tsm.getType(CAS.TYPE_NAME_STRING);
    Type typeRef = tsm.getType(CAS.TYPE_NAME_TOP);
    // assert(annotType != null);
    Type sentType = tsm.addType(SENT_TYPE, annotType);
    Type tokenType = tsm.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsm.addType(TOKEN_TYPE_TYPE, topType);
    tsm.addType(WORD_TYPE, tokenTypeType);
    tsm.addType(SEP_TYPE, tokenTypeType);
    tsm.addType(EOS_TYPE, tokenTypeType);
    tsm.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    tsm.addFeature(TOKEN_FLOAT_FEAT, tokenType, typeFloat);
    // Add a type that inherits from IntArray.
    // tsm.addType(INT_ARRAY_SUB, tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY));
    // tsm.addFeature(
    // INT_SUB_NAME,
    // tsm.getType(INT_ARRAY_SUB),
    // tsm.getType(CAS.TYPE_NAME_STRING));
    tsm.addFeature(LEMMA_FEAT, tokenType, typeString);
    tsm.addFeature(SENT_LEN_FEAT, sentType, typeInteger);
    tsm.addFeature(LEMMA_LIST_FEAT, tokenType, typeArrayString);

    // add a type that inherits from Token which has no JCas model
    Type subTok = tsm.addType(SUBTOK_TYPE, tokenType);

    Type group1 = tsm.addStringSubtype(GROUP_1, GROUP_1_LANGUAGES);
    Type group2 = tsm.addStringSubtype(GROUP_2, GROUP_2_LANGUAGES);
    Type langPair = tsm.addType(LANG_PAIR, topType);
    tsm.addFeature(LANG1, langPair, group1);
    tsm.addFeature(LANG2, langPair, group2);
    tsm.addFeature(DESCR_FEAT, langPair, typeString);

    // types for testing every variant in JCas

    Type typeRoot = tsm.addType("aa.Root", topType);
    tsm.addFeature("arrayInt", typeRoot, typeArrayInt);
    tsm.addFeature("arrayRef", typeRoot, typeArrayRef);
    tsm.addFeature("arrayFloat", typeRoot, typeArrayFloat);
    tsm.addFeature("arrayLong", typeRoot, typeArrayLong);
    tsm.addFeature("arrayDouble", typeRoot, typeArrayDouble);
    tsm.addFeature("arrayString", typeRoot, typeArrayString);
    tsm.addFeature("plainInt", typeRoot, typeInteger);
    tsm.addFeature("plainFloat", typeRoot, typeFloat);
    tsm.addFeature("plainDouble", typeRoot, typeDouble);
    tsm.addFeature("plainLong", typeRoot, typeLong);
    tsm.addFeature("plainString", typeRoot, typeString);
    tsm.addFeature("plainRef", typeRoot, typeRef);

    if (bad != BAD_MISSING_TYPE_IN_CAS)
      tsm.addType("aa.MissingInCas", topType);

    Type typeMissingFeat = tsm.addType("aa.MissingFeatureInCas", topType);
    tsm.addFeature("haveThisOne", typeMissingFeat, typeInteger);
    if (bad != BAD_MISSING_FEATURE_IN_CAS)
      tsm.addFeature("missingThisOne", typeMissingFeat, typeFloat);

    tsm.addFeature("changedFType", typeMissingFeat, (bad != BAD_CHANGED_FEATURE_TYPE) ? typeString
            : typeFloat);

    Type abstractType = tsm.addType("aa.AbstractType", topType);
    tsm.addFeature("abstractInt", abstractType, typeInteger);

    Type concreteType = tsm.addType("aa.ConcreteType", abstractType);
    tsm.addFeature("concreteString", concreteType, typeString);

    /*
     * String subtypes cannot be inherited from. String subtypes can't have features added - they're
     * all added in the creation call via an array of allowed strings.
     */
    boolean exc = false;
    try {
      tsm.addType("some.new.Name", group1);
    } catch (CASAdminException e) {
      TestCase.assertTrue(e.getError() == CASAdminException.TYPE_IS_INH_FINAL);
      exc = true;
    }
    TestCase.assertTrue(exc);
    exc = false;
    try {
      tsm.addFeature("some.new.Name", group1, typeString);
    } catch (CASAdminException e) {
      TestCase.assertTrue(e.getError() == CASAdminException.TYPE_IS_FEATURE_FINAL);
      exc = true;
    }
    TestCase.assertTrue(exc);
  }

  public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    FSIndexComparator comp = irm.createComparator();
    Type annotation = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
            FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END),
            FSIndexComparator.REVERSE_STANDARD_COMPARE);
    irm.createIndex(comp, ANNOT_BAG_INDEX, FSIndex.BAG_INDEX);
    irm.createIndex(comp, ANNOT_SET_INDEX, FSIndex.SET_INDEX);

    comp = irm.createComparator();
    comp.setType(ts.getType("uima.cas.TOP"));
    irm.createIndex(comp, "all", FSIndex.BAG_INDEX);

  }
}
