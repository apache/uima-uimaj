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
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;


public class IndexSerializationTest extends TestCase {

  // Index name constants.
  public static final String ANNOT_SET_INDEX = "Annotation Set Index";

  public static final String ANNOT_BAG_INDEX = "Annotation Bag Index";

  public static final String TOKEN_TYPE = "Token";

  public static final String TOKEN_TYPE_FEAT = "type";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "TokenType";

  public static final String WORD_TYPE = "Word";

  public static final String SEP_TYPE = "Separator";

  public static final String EOS_TYPE = "EndOfSentence";

  public static final String SENT_TYPE = "Sentence";

  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type wordType;

  private Type separatorType;

  private Type eosType;

  private Type tokenType;

  private Feature tokenTypeFeature;

  private Type sentenceType;

  private Feature startFeature;

  private Feature endFeature;

  public IndexSerializationTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    casMgr = initCAS();
    cas = (CASImpl)casMgr;

    TypeSystem ts = cas.getTypeSystem();
    wordType = ts.getType(WORD_TYPE);
    // assert(wordType != null);
    separatorType = ts.getType(SEP_TYPE);
    eosType = ts.getType(EOS_TYPE);
    tokenType = ts.getType(TOKEN_TYPE);
    tokenTypeFeature = ts.getFeatureByFullName(TOKEN_TYPE_FEAT_Q);
    startFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    endFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    sentenceType = ts.getType(SENT_TYPE);
    annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(annotationType != null);
  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    annotationType = null;
    wordType = null;
    separatorType = null;
    eosType = null;
    tokenType = null;
    tokenTypeFeature = null;
    sentenceType = null;
    startFeature = null;
    endFeature = null;
  }
  
  // Initialize the first CAS.
  private static CASMgr initCAS() {
    // // Create a CASMgr. Ensures existence of AnnotationFS type.
    // CASMgr tcas = CASFactory.createCAS();
    CASMgr casMgr = CASFactory.createCAS();
    try {
      CasCreationUtils.setupTypeSystem(casMgr, (TypeSystemDescription) null);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
    }
    // Create a writable type system.
    TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
    // Add new types and features.
    Type topType = tsa.getTopType();
    Type annotType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    // assert(annotType != null);
    tsa.addType(SENT_TYPE, annotType);
    Type tokenType = tsa.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsa.addType(TOKEN_TYPE_TYPE, topType);
    tsa.addType(WORD_TYPE, tokenTypeType);
    tsa.addType(SEP_TYPE, tokenTypeType);
    tsa.addType(EOS_TYPE, tokenTypeType);
    tsa.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    // Commit the type system.
    ((CASImpl) casMgr).commitTypeSystem();
    // assert(tsa.isCommitted());
    // // Create the CAS indexes.
    // tcas.initCASIndexes();
    // Create the Base indexes.
    try {
      casMgr.initCASIndexes();
    } catch (CASException e) {
      e.printStackTrace();
    }

    FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
    FSIndexComparator comp = irm.createComparator();
    Type annotation = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
            FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END),
            FSIndexComparator.REVERSE_STANDARD_COMPARE);
    irm.createIndex(comp, ANNOT_BAG_INDEX, FSIndex.BAG_INDEX);
    irm.createIndex(comp, ANNOT_SET_INDEX, FSIndex.SET_INDEX);

    // Commit the index repository.
    irm.commit();
    // assert(cas.getIndexRepositoryMgr().isCommitted());

    // Create the default text Sofa and return CAS view
    return (CASMgr) casMgr.getCAS().getCurrentView();
  }

  /**
   * Test driver.
   */
  public void testMain() throws Exception {

    for (int i = 0; i < 10; i++) {
      cas.getIndexRepository().addFS(cas.createAnnotation(annotationType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
    }
    for (int i = 19; i >= 10; i--) {
      cas.getIndexRepository().addFS(cas.createAnnotation(annotationType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
      cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
    }

    AnnotationFS searchAnnot = cas.createAnnotation(annotationType, 0, 1);
    assertTrue(cas.getAnnotationIndex().find(searchAnnot) != null);
    assertTrue(cas.getIndexRepository().getIndex(ANNOT_SET_INDEX).find(searchAnnot) != null);
    // find() does not produce useful results on bag indexes, since the comparator
    // is not defined.
    // assertTrue(cas.getIndexRepository().getIndex(ANNOT_BAG_INDEX).find(searchAnnot) != null);

    searchAnnot.setIntValue(endFeature, 4);
    assertTrue(cas.getAnnotationIndex().find(searchAnnot) == null);
    assertTrue(cas.getIndexRepository().getIndex(ANNOT_SET_INDEX).find(searchAnnot) == null);
    assertTrue(cas.getIndexRepository().getIndex(ANNOT_BAG_INDEX).find(searchAnnot) == null);

    final int ordSize = cas.getAnnotationIndex().size();
    final int setSize = cas.getIndexRepository().getIndex(ANNOT_SET_INDEX).size();
    final int bagSize = cas.getIndexRepository().getIndex(ANNOT_BAG_INDEX).size();
    // System.out.println("Before serialization\n");
    // System.out.println("Size of ordered index: " + ordSize);
    // System.out.println("Size of set index: " + setSize);
    // System.out.println("Size of bag index: " + bagSize);

    CASCompleteSerializer cs;
    cs = Serialization.serializeCASComplete(casMgr);
    // casMgr = CASFactory.createCAS();
    CASMgr realCasMgr = CASFactory.createCAS();
    ((CASImpl) realCasMgr).commitTypeSystem();
    Serialization.deserializeCASComplete(cs, realCasMgr);
    cas = ((CASImpl) realCasMgr).getCurrentView();
    casMgr = (CASMgr) cas;

    // System.out.println("After serialization\n");
    FSIndex<? extends FeatureStructure> index = cas.getAnnotationIndex();
    assertTrue(index != null);
    assertTrue(index.getIndexingStrategy() == FSIndex.SORTED_INDEX);
    // System.out.println("Size of ordered index: " + index.size());
    assertTrue(index.size() == ordSize);

    index = cas.getIndexRepository().getIndex(ANNOT_BAG_INDEX);
    assertTrue(index != null);
    assertTrue(index.getIndexingStrategy() == FSIndex.BAG_INDEX);
    // System.out.println("Size of bag index: " + index.size());
    assertTrue(index.size() == bagSize);

    index = cas.getIndexRepository().getIndex(ANNOT_SET_INDEX);
    assertTrue(index != null);
    assertTrue(index.getIndexingStrategy() == FSIndex.SET_INDEX);
    // System.out.println("Size of set index: " + index.size());
    // System.out.println("Should be: " + setSize);
    assertTrue(index.size() == setSize);

  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(IndexSerializationTest.class);
  }

}
