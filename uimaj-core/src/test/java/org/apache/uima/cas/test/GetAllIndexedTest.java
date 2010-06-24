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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;


public class GetAllIndexedTest extends TestCase {

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
  
  //  private Type tokenType;

//  private Type sentenceType;

  public GetAllIndexedTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/typePriorityTestCaseDescriptor.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(), descriptorFile.exists());
    
    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spec);
      this.cas = ae.newCAS();
      assertTrue(this.cas != null);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    }

    TypeSystem ts = this.cas.getTypeSystem();
    // assert(wordType != null);
//    this.tokenType = ts.getType(TOKEN_TYPE);
//    this.sentenceType = ts.getType(SENT_TYPE);
    this.annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(this.annotationType != null);
    this.otherAnnotationType = ts.getType(OTHER_ANNOT_TYPE);
    assertTrue(this.otherAnnotationType != null);
    this.annotationBaseType = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertTrue(this.annotationBaseType != null);
  }

  public void tearDown() {
    this.cas = null;
//    this.tokenType = null;
//    this.sentenceType = null;
    this.annotationType = null;
    this.annotationBaseType = null;
    this.otherAnnotationType = null;
  }
  
  private final FSIterator<FeatureStructure> getAllIndexed() {
    return getAllIndexed(this.cas.getTypeSystem().getTopType());
  }

  private final FSIterator<FeatureStructure> getAllIndexed(Type type) {
    return this.cas.getIndexRepository().getAllIndexedFS(type);
  }
  
  private final int getIteratorSize(FSIterator<? extends FeatureStructure> it) {
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }
    return count;
  }

  private final void addFS(FeatureStructure fs) {
    this.cas.getIndexRepository().addFS(fs);
    ++this.fsCount;
    assertTrue(getIteratorSize(getAllIndexed()) == this.fsCount);
  }
  
  private final FeatureStructure createAnnot(int from, int to) {
    return this.cas.createAnnotation(this.annotationType, from, to);
  }
  
  private final void initTest() {
    this.cas.reset();
    this.fsCount = 0;
  }
  
  /**
   * Test driver.
   */
  public void testGetAllIndexed() throws Exception {
    initTest();
    FeatureStructure docAnnotation = this.cas.getDocumentAnnotation();
  	assertNotNull(docAnnotation);
    ++this.fsCount;
    assertTrue(getIteratorSize(getAllIndexed()) == this.fsCount);
  	final FeatureStructure otherAnnotationFS = this.cas.createFS(this.otherAnnotationType);
  	FeatureStructure annotationFS = this.cas.createFS(this.annotationType);
  	final FeatureStructure annotationBaseFS = this.cas.createFS(this.annotationBaseType);
  	addFS(annotationFS);
    addFS(otherAnnotationFS);
    addFS(annotationBaseFS);
    addFS(this.cas.createFS(this.cas.getTypeSystem().getTopType()));
    assertTrue(getIteratorSize(this.cas.getAnnotationIndex().iterator()) == 2);
    addFS(createAnnot(0, 1));
    addFS(createAnnot(1, 2));
    addFS(createAnnot(2, 3));
    addFS(createAnnot(3, 4));
    
    // Iterate backwards, check only that it returns correct number of FSs
    FSIterator<FeatureStructure> it = getAllIndexed();
    int down = this.fsCount;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      --down;
    }
    assertTrue(down == 0);

    // Get all indexed, create copy and iterate in parallel.
    it = getAllIndexed();
    FSIterator<FeatureStructure> copy = it.copy();
    copy.moveToFirst();
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      assertTrue(copy.isValid());
      assertTrue(it.get().equals(copy.get()));
      copy.moveToNext();
    }
    assertFalse(copy.isValid());
    
    // Iterate over all indexed, create a copy at each stage, check that it gets same FS.
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      copy = it.copy();
      assertTrue(it.get().equals(copy.get()));
    }
    copy = it.copy();
    assertFalse(it.isValid());
    assertFalse(copy.isValid());
    
    //test getAllIndexed(Type)
    Type tokenType = this.cas.getTypeSystem().getType(TOKEN_TYPE);
    assertNotNull(tokenType);
    FSIterator<FeatureStructure> tokenIter = this.cas.getIndexRepository().getAllIndexedFS(tokenType);
    assertFalse(tokenIter.hasNext());
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(GetAllIndexedTest.class);
  }

}
