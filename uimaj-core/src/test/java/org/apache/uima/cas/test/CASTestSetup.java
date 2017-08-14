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

package org.apache.uima.cas.test;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;

public class CASTestSetup  implements AnnotatorInitializer {

  // Type system constants.
  public static final String TOKEN_TYPE = "Token";

  public static final String TOKEN_TYPE_FEAT = "type";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "TokenType";

  public static final String WORD_TYPE = "Word";
  
  public static final String ARRAYFSWITHSUBTYPE_TYPE = "ArrayFsWithSubtype";
  
  public static final String ARRAYFSWITHSUBTYPE_TYPE_FEAT = "subArrayOfAnnot";
  
  public static final String ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q = ARRAYFSWITHSUBTYPE_TYPE + TypeSystem.FEATURE_SEPARATOR
          + ARRAYFSWITHSUBTYPE_TYPE_FEAT;

  public static final String SEP_TYPE = "Separator";

  public static final String EOS_TYPE = "EndOfSentence";

  public static final String SENT_TYPE = "Sentence";

  // public static final String INT_ARRAY_SUB = "IntArraySub";
  public static final String INT_SUB_NAME = "intArrayName";

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
  
  public static final String ANNOT_SET_INDEX_NO_TYPEORDER = "Annotation Set Index No Type Order";

  public static final String ANNOT_BAG_INDEX = "Annotation Bag Index";

  public static final String ANNOT_SORT_INDEX = "Annotation Sort Index";

  /**
   * Constructor for CASTestSetup.
   */
  public CASTestSetup() {
    super();
  }

  /**
   * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(TypeSystemMgr)
   */
  
  /* Types:
   * TOP
   *   Token  TOKEN_TYPE
   *     Word
   *     Separator
   *     EndOfSentence
   *   ArrayFSwithSubtype
   *   Annotation
   *     Sentence
   *   
   */
  public void initTypeSystem(TypeSystemMgr tsm) {
    // Add new types and features.
    Type topType = tsm.getTopType();
    Type annotType = tsm.getType(CAS.TYPE_NAME_ANNOTATION);
    // assert(annotType != null);
    tsm.addType(SENT_TYPE, annotType);
    Type tokenType = tsm.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsm.addType(TOKEN_TYPE_TYPE, topType);
    tsm.addType(WORD_TYPE, tokenTypeType);
    Type arrayFsWithSubtypeType = tsm.addType(ARRAYFSWITHSUBTYPE_TYPE, topType);
    Type arrayOfAnnot = tsm.getArrayType(annotType);
    tsm.addFeature(ARRAYFSWITHSUBTYPE_TYPE_FEAT, arrayFsWithSubtypeType, arrayOfAnnot);
    tsm.addType(SEP_TYPE, tokenTypeType);
    tsm.addType(EOS_TYPE, tokenTypeType);
    tsm.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    tsm.addFeature(TOKEN_FLOAT_FEAT, tokenType, tsm.getType(CAS.TYPE_NAME_FLOAT));
    // Add a type that inherits from IntArray.
    // tsm.addType(INT_ARRAY_SUB, tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY));
    // tsm.addFeature(
    // INT_SUB_NAME,
    // tsm.getType(INT_ARRAY_SUB),
    // tsm.getType(CAS.TYPE_NAME_STRING));
    tsm.addFeature(LEMMA_FEAT, tokenType, tsm.getType(CAS.TYPE_NAME_STRING));
    tsm.addFeature(SENT_LEN_FEAT, tsm.getType(SENT_TYPE), tsm.getType(CAS.TYPE_NAME_INTEGER));
    tsm.addFeature(LEMMA_LIST_FEAT, tsm.getType(TOKEN_TYPE), tsm
            .getType(CAS.TYPE_NAME_STRING_ARRAY));
    Type group1 = tsm.addStringSubtype(GROUP_1, GROUP_1_LANGUAGES);
    Type group2 = tsm.addStringSubtype(GROUP_2, GROUP_2_LANGUAGES);
    Type langPair = tsm.addType(LANG_PAIR, topType);
    tsm.addFeature(LANG1, langPair, group1);
    tsm.addFeature(LANG2, langPair, group2);
    Type stringType = tsm.getType(CAS.TYPE_NAME_STRING);
    tsm.addFeature(DESCR_FEAT, langPair, stringType);
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
      tsm.addFeature("some.new.Name", group1, stringType);
    } catch (CASAdminException e) {
      TestCase.assertTrue(e.getError() == CASAdminException.TYPE_IS_FEATURE_FINAL);
      exc = true;
    }
    TestCase.assertTrue(exc);
  }

  private FSIndexComparator makeComp(FSIndexRepositoryMgr irm, TypeSystem ts) {
    FSIndexComparator comp = irm.createComparator();
    Type annotation = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
            FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END),
            FSIndexComparator.REVERSE_STANDARD_COMPARE);
    return comp;
  }
  
  public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    FSIndexComparator compNoTypeOrder = makeComp(irm, ts);
    FSIndexComparator comp = makeComp(irm, ts);
    LinearTypeOrderBuilder tob = irm.createTypeSortOrder();
    try {
      tob.add(new String[] { CAS.TYPE_NAME_ANNOTATION, SENT_TYPE, TOKEN_TYPE });
      comp.addKey(tob.getOrder(), FSIndexComparator.STANDARD_COMPARE);
    } catch (CASException e) {
      TestCase.assertTrue(false);
    }
    
    
    irm.createIndex(comp, ANNOT_BAG_INDEX, FSIndex.BAG_INDEX);
    irm.createIndex(comp, ANNOT_SET_INDEX, FSIndex.SET_INDEX);
    irm.createIndex(comp, ANNOT_SORT_INDEX, FSIndex.SORTED_INDEX);
    irm.createIndex(compNoTypeOrder, ANNOT_SET_INDEX_NO_TYPEORDER, FSIndex.SET_INDEX);
    

  }
}
