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

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.cas.text.TCASException;

/**
 * @author Thilo Goetz
 */
public class TypeOrderTest extends TestCase {

  // Index name constants.
  public static final String ANNOT_SET_INDEX = "Annotation Set Index";

  public static final String ANNOT_BAG_INDEX = "Annotation Bag Index";

  public static final String TYPE_ORDER_INDEX = "TypeOrderIndex";

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

  private TCAS cas;

  private Type annotationType;

  private Type wordType;

  private Type separatorType;

  private Type eosType;

  private Type tokenType;

  private Feature tokenTypeFeature;

  private Type sentenceType;

  private Feature startFeature;

  private Feature endFeature;

  public TypeOrderTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    casMgr = initCAS();
    cas = casMgr.getCAS().getTCAS();

    TypeSystem ts = cas.getTypeSystem();
    wordType = ts.getType(WORD_TYPE);
    // assert(wordType != null);
    separatorType = ts.getType(SEP_TYPE);
    eosType = ts.getType(EOS_TYPE);
    tokenType = ts.getType(TOKEN_TYPE);
    tokenTypeFeature = ts.getFeatureByFullName(TOKEN_TYPE_FEAT_Q);
    startFeature = ts.getFeatureByFullName(TCAS.FEATURE_FULL_NAME_BEGIN);
    endFeature = ts.getFeatureByFullName(TCAS.FEATURE_FULL_NAME_END);
    sentenceType = ts.getType(SENT_TYPE);
    annotationType = ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    assertTrue(annotationType != null);
  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    wordType = null;
    separatorType = null;
    eosType = null;
    tokenType = null;
    tokenTypeFeature = null;
    startFeature = null;
    endFeature = null;
    sentenceType = null;
    annotationType = null;
  }

  // Initialize the first CAS.
  private static CASMgr initCAS() throws TCASException {
    // Create a TCASMgr. Ensures existence of AnnotationFS type.
    CASMgr cas = CASFactory.createCAS();
    // Create a writable type system.
    TypeSystemMgr tsa = cas.getTypeSystemMgr();
    // Add new types and features.
    Type topType = tsa.getTopType();
    Type annotType = tsa.getType(TCAS.TYPE_NAME_ANNOTATION);
    // assert(annotType != null);
    tsa.addType(SENT_TYPE, annotType);
    Type tokenType = tsa.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsa.addType(TOKEN_TYPE_TYPE, topType);
    tsa.addType(WORD_TYPE, tokenTypeType);
    tsa.addType(SEP_TYPE, tokenTypeType);
    tsa.addType(EOS_TYPE, tokenTypeType);
    tsa.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    // Commit the type system.
    ((CASImpl) cas).commitTypeSystem();
    // assert(tsa.isCommitted());
    // Create the TCAS indexes.
    // tcas.initTCASIndexes();
    // Create the Base indexes.
    try {
      cas.initCASIndexes();
    } catch (CASException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
      assertTrue(false);
    }

    FSIndexRepositoryMgr irm = cas.getIndexRepositoryMgr();
    FSIndexComparator comp = irm.createComparator();
    Type annotation = tsa.getType(TCAS.TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_BEGIN),
                    FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_END),
                    FSIndexComparator.REVERSE_STANDARD_COMPARE);
    irm.createIndex(comp, ANNOT_BAG_INDEX, FSIndex.BAG_INDEX);
    irm.createIndex(comp, ANNOT_SET_INDEX, FSIndex.SET_INDEX);

    // Check that appropriate exception is thrown on unknown types.
    LinearTypeOrderBuilder order = irm.createTypeSortOrder();
    boolean excCaught = false;
    try {
      order.add(new String[] { "foo", "bar" });
      order.getOrder();
    } catch (NullPointerException e) {
      assertTrue(false);
    } catch (CASException e) {
      excCaught = true;
    }
    assertTrue(excCaught);

    // Create an alternative annotation index using a type sort order.
    order = irm.createTypeSortOrder();
    LinearTypeOrder lo = null;
    try {
      order.add(new String[] { TOKEN_TYPE, SENT_TYPE, TCAS.TYPE_NAME_ANNOTATION });
      lo = order.getOrder();
    } catch (CASException e) {
      assertTrue(false);
    }
    assertTrue(!lo.lessThan(tokenType, tokenType));
    assertTrue(lo.lessThan(tokenType, annotType));
    assertTrue(!lo.lessThan(annotType, tokenType));
    comp = irm.createComparator();
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_BEGIN),
                    FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_END),
                    FSIndexComparator.REVERSE_STANDARD_COMPARE);
    try {
      comp.addKey(order.getOrder(), FSIndexComparator.STANDARD_COMPARE);
    } catch (CASException e1) {
      assertTrue(false);
    }
    irm.createIndex(comp, TYPE_ORDER_INDEX);

    // Commit the index repository.
    cas.getIndexRepositoryMgr().commit();
    // assert(cas.getIndexRepositoryMgr().isCommitted());
    return cas;
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

    FSIterator it = cas.getIndexRepository().getIndex(TYPE_ORDER_INDEX).iterator();

    // it = cas.getAnnotationIndex().iterator();
    AnnotationFS fs;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      fs = (AnnotationFS) it.get();
      // TODO: shouldn't this assert something rather than printing?
      // System.out.println(
      // fs.getType().getName()
      // + ": "
      // + fs.getBegin()
      // + " - "
      // + fs.getEnd());
    }

  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(TypeOrderTest.class);
  }

}
