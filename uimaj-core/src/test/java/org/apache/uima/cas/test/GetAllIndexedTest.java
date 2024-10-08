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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetAllIndexedTest {

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

  public static final String OTHER_ANNOT_TYPE = "OtherAnnotation";

  private CAS cas;

  private Type annotationType;

  private Type otherAnnotationType;

  private Type annotationBaseType;

  // Count the number of FSs created in a test case.
  private int fsCount = 0;

  // private Type tokenType;

  // private Type sentenceType;

  @BeforeEach
  public void setUp() throws Exception {
    File descriptorFile = JUnitExtension
            .getFile("CASTests/desc/typePriorityTestCaseDescriptor.xml");
    assertThat(descriptorFile).as("Descriptor must exist: " + descriptorFile.getAbsolutePath())
            .exists();

    XMLParser parser = UIMAFramework.getXMLParser();
    ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spec);
    cas = ae.newCAS();
    assertThat(cas).isNotNull();

    TypeSystem ts = cas.getTypeSystem();
    // assert(wordType != null);
    // this.tokenType = ts.getType(TOKEN_TYPE);
    // this.sentenceType = ts.getType(SENT_TYPE);
    annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertThat(annotationType).isNotNull();
    otherAnnotationType = ts.getType(OTHER_ANNOT_TYPE);
    assertThat(otherAnnotationType).isNotNull();
    annotationBaseType = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertThat(annotationBaseType).isNotNull();
  }

  @AfterEach
  void tearDown() {
    cas = null;
    // this.tokenType = null;
    // this.sentenceType = null;
    annotationType = null;
    annotationBaseType = null;
    otherAnnotationType = null;
  }

  private final FSIterator<FeatureStructure> getAllIndexed() {
    return getAllIndexed(cas.getTypeSystem().getTopType());
  }

  private final FSIterator<FeatureStructure> getAllIndexed(Type type) {
    return cas.getIndexRepository().getAllIndexedFS(type);
  }

  private final int getIteratorSize(FSIterator<? extends FeatureStructure> it) {
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }
    return count;
  }

  private final void addFS(FeatureStructure fs) {
    cas.getIndexRepository().addFS(fs);
    ++fsCount;
    assertThat(fsCount).isEqualTo(getIteratorSize(getAllIndexed()));
  }

  private final FeatureStructure createAnnot(int from, int to) {
    return cas.createAnnotation(annotationType, from, to);
  }

  private final void initTest() {
    cas.reset();
    fsCount = 0;
  }

  /**
   * Test driver.
   */
  @Test
  void testGetAllIndexed() throws Exception {
    initTest();
    FeatureStructure docAnnotation = cas.getDocumentAnnotation();
    assertThat(docAnnotation).isNotNull();
    ++fsCount;
    assertThat(fsCount).isEqualTo(getIteratorSize(getAllIndexed()));
    final FeatureStructure otherAnnotationFS = cas.createFS(otherAnnotationType);
    FeatureStructure annotationFS = cas.createFS(annotationType);
    final FeatureStructure annotationBaseFS = cas.createFS(annotationBaseType);
    addFS(annotationFS);
    addFS(otherAnnotationFS);
    addFS(annotationBaseFS);
    addFS(cas.createFS(cas.getTypeSystem().getTopType()));
    assertThat(getIteratorSize(cas.getAnnotationIndex().iterator())).isEqualTo(2);
    addFS(createAnnot(0, 1));
    addFS(createAnnot(1, 2));
    addFS(createAnnot(2, 3));
    addFS(createAnnot(3, 4));

    // Iterate backwards, check only that it returns correct number of FSs
    FSIterator<FeatureStructure> it = getAllIndexed();
    int down = fsCount;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      --down;
    }
    assertThat(down).isZero();

    // Get all indexed, create copy and iterate in parallel.
    it = getAllIndexed();
    FSIterator<FeatureStructure> copy = it.copy();
    copy.moveToFirst();
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      assertThat(copy.isValid()).isTrue();
      assertThat(copy.get()).isEqualTo(it.get());
      copy.moveToNext();
    }
    assertThat(copy.isValid()).isFalse();

    // Iterate over all indexed, create a copy at each stage, check that it gets same FS.
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      copy = it.copy();
      assertThat(copy.get()).isEqualTo(it.get());
    }
    copy = it.copy();
    assertThat(it.isValid()).isFalse();
    assertThat(copy.isValid()).isFalse();

    // test getAllIndexed(Type)
    Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
    assertThat(tokenType).isNotNull();
    FSIterator<FeatureStructure> tokenIter = cas.getIndexRepository().getAllIndexedFS(tokenType);
    assertThat(tokenIter.hasNext()).isFalse();
    Iterator<TOP> tokenIter2 = cas.getIndexedFSs(tokenType).iterator();
    assertThat(tokenIter2.hasNext()).isFalse();
  }
}
