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

import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_BEGIN;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_END;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_LONG;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.cas.FSIndex.BAG_INDEX;
import static org.apache.uima.cas.FSIndex.SET_INDEX;
import static org.apache.uima.cas.FSIndex.SORTED_INDEX;
import static org.apache.uima.cas.TypeSystem.FEATURE_SEPARATOR;
import static org.apache.uima.cas.admin.FSIndexComparator.REVERSE_STANDARD_COMPARE;
import static org.apache.uima.cas.admin.FSIndexComparator.STANDARD_COMPARE;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;

import junit.framework.TestCase;

public class CASTestSetup implements AnnotatorInitializer {

  // Type system constants.
  public static final String TOKEN_TYPE = "Token";

  public static final String PHRASE_TYPE = "Phrase";

  public static final String TOKEN_TYPE_FEAT = "type";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "TokenType";

  public static final String WORD_TYPE = "Word";

  public static final String ARRAYFSWITHSUBTYPE_TYPE = "ArrayFsWithSubtype";

  public static final String ARRAYFSWITHSUBTYPE_TYPE_FEAT = "subArrayOfAnnot";

  public static final String ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q = ARRAYFSWITHSUBTYPE_TYPE
          + TypeSystem.FEATURE_SEPARATOR + ARRAYFSWITHSUBTYPE_TYPE_FEAT;

  public static final String SEP_TYPE = "Separator";

  public static final String EOS_TYPE = "EndOfSentence";

  public static final String SENT_TYPE = "Sentence";

  // public static final String INT_ARRAY_SUB = "IntArraySub";
  public static final String INT_SUB_NAME = "intArrayName";

  public static final String LEMMA_FEAT = "lemma";

  public static final String LEMMA_FEAT_Q = TOKEN_TYPE + FEATURE_SEPARATOR + LEMMA_FEAT;

  public static final String SENT_LEN_FEAT = "sentenceLength";

  public static final String SENT_LEN_FEAT_Q = SENT_TYPE + FEATURE_SEPARATOR + SENT_LEN_FEAT;

  public static final String TOKEN_FLOAT_FEAT = "tokenFloatFeat";
  public static final String TOKEN_DOUBLE_FEAT = "tokenDoubleFeat";
  public static final String TOKEN_LONG_FEAT = "tokenLongFeat";

  public static final String TOKEN_FLOAT_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_FLOAT_FEAT;
  public static final String TOKEN_DOUBLE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_DOUBLE_FEAT;
  public static final String TOKEN_LONG_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_LONG_FEAT;

  public static final String LEMMA_LIST_FEAT = "lemmaList";

  public static final String LEMMA_LIST_FEAT_Q = TOKEN_TYPE + FEATURE_SEPARATOR + LEMMA_LIST_FEAT;

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
  }

  /**
   * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(TypeSystemMgr)
   */

  //@formatter:off
  /* Types:
   * TOP
   *   token_type_type 
   *     Word_type
   *     Sep_type
   *     EOS_type
   *   ArrayFSwithSubtype
   *   Annotation
   *     Sentence [SEN_LEN_FEAT(int)
   *       Phrase (subtype of sentence)
   *     Token  TOKEN_TYPE [TOKEN_TYPE_FEAT(TOKEN_TYPE_TYPE), TOKEN_FLOAT_FEAT, LEMMA_FEAT(string), LEMMA_LIST_FEAT[stringArray]
   *   String
   *     Group1
   *     Group2
   *   Lang_pair [LANG1(Group1), LANG2(Group2), DESCR_FEAT(string)  
   */
  //@formatter:on
  @Override
  public void initTypeSystem(TypeSystemMgr tsm) {
    // Add new types and features.
    Type stringType = tsm.getType(TYPE_NAME_STRING);

    Type topType = tsm.getTopType();
    Type annotType = tsm.getType(TYPE_NAME_ANNOTATION);
    // assert(annotType != null);

    Type sentType = tsm.addType(SENT_TYPE, annotType);
    tsm.addFeature(SENT_LEN_FEAT, sentType, tsm.getType(CAS.TYPE_NAME_INTEGER));

    tsm.addType(PHRASE_TYPE, sentType);

    Type tokenTypeType = tsm.addType(TOKEN_TYPE_TYPE, topType);

    Type tokenType = tsm.addType(TOKEN_TYPE, annotType);
    tsm.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    tsm.addFeature(TOKEN_FLOAT_FEAT, tokenType, tsm.getType(TYPE_NAME_FLOAT));
    tsm.addFeature(TOKEN_DOUBLE_FEAT, tokenType, tsm.getType(TYPE_NAME_DOUBLE));
    tsm.addFeature(TOKEN_LONG_FEAT, tokenType, tsm.getType(TYPE_NAME_LONG));
    tsm.addFeature(LEMMA_FEAT, tokenType, stringType);
    tsm.addFeature(LEMMA_LIST_FEAT, tokenType, tsm.getType(TYPE_NAME_STRING_ARRAY));

    tsm.addType(WORD_TYPE, tokenTypeType);
    Type arrayFsWithSubtypeType = tsm.addType(ARRAYFSWITHSUBTYPE_TYPE, topType);
    tsm.addFeature(ARRAYFSWITHSUBTYPE_TYPE_FEAT, arrayFsWithSubtypeType,
            tsm.getArrayType(annotType));

    tsm.addType(SEP_TYPE, tokenTypeType);

    tsm.addType(EOS_TYPE, tokenTypeType);
    // Add a type that inherits from IntArray.
    // tsm.addType(INT_ARRAY_SUB, tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY));
    // tsm.addFeature(
    // INT_SUB_NAME,
    // tsm.getType(INT_ARRAY_SUB),
    // tsm.getType(CAS.TYPE_NAME_STRING));
    Type group1 = tsm.addStringSubtype(GROUP_1, GROUP_1_LANGUAGES);
    Type group2 = tsm.addStringSubtype(GROUP_2, GROUP_2_LANGUAGES);
    Type langPair = tsm.addType(LANG_PAIR, topType);
    tsm.addFeature(LANG1, langPair, group1);
    tsm.addFeature(LANG2, langPair, group2);
    tsm.addFeature(DESCR_FEAT, langPair, stringType);

    assertThatExceptionOfType(CASAdminException.class) //
            .isThrownBy(() -> tsm.addType("some.new.Name", group1)) //
            .extracting(CASAdminException::getMessageKey) //
            .isEqualTo(CASAdminException.TYPE_IS_INH_FINAL);

    assertThatExceptionOfType(CASAdminException.class) //
            .isThrownBy(() -> tsm.addFeature("some.new.Name", group1, stringType)) //
            .extracting(CASAdminException::getMessageKey) //
            .isEqualTo(CASAdminException.TYPE_IS_FEATURE_FINAL);

    // add IntegerArray[] type before commit for testArrayTypes in TypeSystemTest
    Type intArrayType = tsm.getType(TYPE_NAME_INTEGER_ARRAY);
    Type arrayOfIntArray = tsm.getArrayType(intArrayType);
  }

  private FSIndexComparator makeComp(FSIndexRepositoryMgr irm, TypeSystem ts) {
    FSIndexComparator comp = irm.createComparator();
    Type annotation = ts.getType(TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(FEATURE_BASE_NAME_BEGIN), STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(FEATURE_BASE_NAME_END), REVERSE_STANDARD_COMPARE);
    return comp;
  }

  @Override
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
    irm.createIndex(comp, ANNOT_BAG_INDEX, BAG_INDEX);
    irm.createIndex(comp, ANNOT_SET_INDEX, SET_INDEX);
    irm.createIndex(comp, ANNOT_SORT_INDEX, SORTED_INDEX);
    irm.createIndex(compNoTypeOrder, ANNOT_SET_INDEX_NO_TYPEORDER, SET_INDEX);
  }
}
