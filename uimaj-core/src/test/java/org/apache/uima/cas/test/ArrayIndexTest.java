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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Class must be public because it is also a annotator class!
@SuppressWarnings("java:S5786")
public class ArrayIndexTest implements TextAnnotator {

  private static final String idxId = "ArrayIndex";

  private AnalysisEngine ae = null;

  @BeforeEach
  void setUp() throws Exception {
    // Start up TAE
    var input = new XMLInputSource(JUnitExtension.getFile("CASTests/desc/ArrayIndexTest.xml"));
    var desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);
    ae = UIMAFramework.produceAnalysisEngine(desc);

  }

  @Test
  void testArrayIndex() throws Exception {
    var cas = ae.newCAS();
    var ir = cas.getIndexRepository();
    var ts = cas.getTypeSystem();
    var annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    var annotArrayType = ts.getArrayType(annotationType);

    assertEquals(countIndexMembers(ir.getIndex(idxId)), 0);
    assertEquals(countIndexMembers(ir.getIndex(idxId, ts.getType(CAS.TYPE_NAME_FS_ARRAY))), 0);
    assertNull(ir.getIndex(idxId, annotArrayType));
  }

  private static int countIndexMembers(FSIndex<? extends FeatureStructure> idx) {
    var it = idx.iterator();
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }
    return count;
  }

  @AfterEach
  public void tearDown() throws Exception {
    ae.destroy();
  }

  @Override
  public void process(CAS aCAS, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    // Do nothing.
  }

  @Deprecated(since = "3.6.0")
  @Override
  public void initialize(AnnotatorContext aContext)
          throws AnnotatorInitializationException, AnnotatorConfigurationException {
    // do nothing
  }

  @Override
  public void typeSystemInit(TypeSystem aTypeSystem)
          throws AnnotatorInitializationException, AnnotatorConfigurationException {
    // do nothing
  }

  @Override
  public void reconfigure()
          throws AnnotatorConfigurationException, AnnotatorInitializationException {
    // do nothing
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
