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

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class StringSubtypeTest extends TestCase {

  private static final String specifier = "./CASTests/desc/StringSubtypeTest.xml";

  private static final String definedValue1 = "aa";

  private static final String definedValue2 = "bb";

  private static final String definedValue3 = "cc";

  private static final String undefinedValue = "dd";

  private static final String annotationTypeName = "org.apache.uima.cas.test.StringSubtypeAnnotation";

  private static final String stringSetFeatureName = "stringSetFeature";

  private JCas jcas;

  private AnalysisEngine ae;

  public static class Annotator extends JCasAnnotator_ImplBase {

    public void process(JCas aJCas) {
      // Does nothing, not used in this test.
    }

  }

  public StringSubtypeTest(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    super.setUp();
    File specifierFile = JUnitExtension.getFile(specifier);
    XMLInputSource in = new XMLInputSource(specifierFile);
    ResourceSpecifier resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    this.ae = UIMAFramework.produceAnalysisEngine(resourceSpecifier);
    this.jcas = this.ae.newJCas();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.ae.destroy();
    this.jcas = null;
  }

  public void testJcas() {
    StringSubtypeAnnotation annot = new StringSubtypeAnnotation(this.jcas);
    annot.setStringSetFeature(definedValue1);
    annot.setStringSetFeature(definedValue2);
    annot.setStringSetFeature(definedValue3);
    boolean exCaught = false;
    try {
      annot.setStringSetFeature(undefinedValue);
    } catch (CASRuntimeException e) {
      exCaught = true;
    }
    assertTrue(exCaught);
  }

  public void testLowLevelCas() {
    LowLevelCAS cas = this.jcas.getLowLevelCas();
    LowLevelTypeSystem ts = cas.ll_getTypeSystem();
    final int annotType = ts.ll_getCodeForTypeName(annotationTypeName);
    final int addr = cas.ll_createFS(annotType);
    final int stringSetFeat = ts.ll_getCodeForFeatureName(annotationTypeName
	+ TypeSystem.FEATURE_SEPARATOR + stringSetFeatureName);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue1);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue2);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue3);
    // next should be ok https://issues.apache.org/jira/browse/UIMA-1839
    cas.ll_setStringValue(addr, stringSetFeat, null); 
    boolean exCaught = false;
    try {
      cas.ll_setStringValue(addr, stringSetFeat, undefinedValue);
    } catch (CASRuntimeException e) {
      exCaught = true;
    }
    assertTrue(exCaught);
  }

  public void testCas() {
    CAS cas = this.jcas.getCas();
    TypeSystem ts = cas.getTypeSystem();
    Type annotType = ts.getType(annotationTypeName);
    FeatureStructure fs = cas.createFS(annotType);
    Feature stringSetFeat = ts.getFeatureByFullName(annotationTypeName
	+ TypeSystem.FEATURE_SEPARATOR + stringSetFeatureName);
    fs.setStringValue(stringSetFeat, definedValue1);
    fs.setStringValue(stringSetFeat, definedValue2);
    fs.setStringValue(stringSetFeat, definedValue3);
    // next should be ok https://issues.apache.org/jira/browse/UIMA-1839
    fs.setStringValue(stringSetFeat, null);
    boolean exCaught = false;
    try {
      fs.setStringValue(stringSetFeat, undefinedValue);
    } catch (CASRuntimeException e) {
      exCaught = true;
    }
    assertTrue(exCaught);
  }

}
