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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.FileCompare;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;


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

//  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type tokenType;

  private Type sentenceType;

  public TypeOrderTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
//    this.casMgr = initCAS();
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
    this.tokenType = ts.getType(TOKEN_TYPE);
    this.sentenceType = ts.getType(SENT_TYPE);
    this.annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(this.annotationType != null);
  }

  public void tearDown() {
//    this.casMgr = null;
    this.cas = null;
    this.tokenType = null;
    this.sentenceType = null;
    this.annotationType = null;
  }

  // Initialize the first CAS.
  public void testInitCAS() {
    // Create a CASMgr. Ensures existence of AnnotationFS type.
    CASMgr cas1 = CASFactory.createCAS();
    // Create a writable type system.
    TypeSystemMgr tsa = cas1.getTypeSystemMgr();
    // Add new types and features.
    Type topType = tsa.getTopType();
    Type annotType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    // assert(annotType != null);
    tsa.addType(SENT_TYPE, annotType);
    tsa.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsa.addType(TOKEN_TYPE_TYPE, topType);
    tsa.addType(WORD_TYPE, tokenTypeType);
    tsa.addType(SEP_TYPE, tokenTypeType);
    tsa.addType(EOS_TYPE, tokenTypeType);
    // Commit the type system.
    ((CASImpl) cas1).commitTypeSystem();
    // Create the Base indexes.
    try {
      cas1.initCASIndexes();
    } catch (CASException e2) {
      e2.printStackTrace();
      assertTrue(false);
    }

    FSIndexRepositoryMgr irm = cas1.getIndexRepositoryMgr();

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
    try {
      order.add(new String[] { TOKEN_TYPE, SENT_TYPE, CAS.TYPE_NAME_ANNOTATION });
      order.getOrder();
    } catch (CASException e) {
      assertTrue(false);
    }
  }

  /**
   * Test driver.
   */
  public void testMain() throws Exception {

    File refFile = JUnitExtension.getFile("CASTests/CasTypeOrderTestRef.txt");
    Assert.assertNotNull(refFile);
    File outputFile = new File(JUnitExtension.getFile("CASTests"), "CasTypeOderTest_testouput.txt");
    OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(
            outputFile , false), "UTF-8");
    Assert.assertNotNull(fileWriter);   
    
    for (int i = 0; i < 10; i++) {
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    }
    for (int i = 19; i >= 10; i--) {
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    }

    FSIterator<FeatureStructure> it = this.cas.getIndexRepository().getIndex(TYPE_ORDER_INDEX).iterator();

    // it = cas.getAnnotationIndex().iterator();
    AnnotationFS fs;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      fs = (AnnotationFS) it.get();
      fileWriter.write(
       fs.getType().getName()
       + ": "
       + fs.getBegin()
       + " - "
       + fs.getEnd() + "\n");
    }
    
    fileWriter.close();
    //System.out.println(refFile.getAbsolutePath());
    //System.out.println(outputFile.getAbsolutePath());
    Assert.assertTrue("Comparing ref " + refFile.getAbsolutePath() + " and output " + outputFile.getAbsolutePath(), FileCompare.compare(refFile, outputFile));
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(TypeOrderTest.class);
  }

}
