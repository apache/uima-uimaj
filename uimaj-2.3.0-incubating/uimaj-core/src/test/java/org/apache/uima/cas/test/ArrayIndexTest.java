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

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class ArrayIndexTest extends TestCase implements TextAnnotator {

  private static final String idxId = "ArrayIndex";

  private AnalysisEngine ae = null;

  protected void setUp() throws Exception {
    super.setUp();
    // Start up TAE
    XMLInputSource input = new XMLInputSource(JUnitExtension
            .getFile("CASTests/desc/ArrayIndexTest.xml"));
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);
    this.ae = UIMAFramework.produceAnalysisEngine(desc);

  }

  public void testArrayIndex() {
    try {
      CAS cas = this.ae.newCAS();
      FSIndexRepository ir = cas.getIndexRepository();
      TypeSystem ts = cas.getTypeSystem();
      Type annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
      Type annotArrayType = ts.getArrayType(annotationType);

      FSIndex<FeatureStructure> arrayIndexAll = ir.getIndex(idxId);
      assertEquals(countIndexMembers(arrayIndexAll), 0);
      FSIndex<FeatureStructure> arrayIndexFSArray = ir.getIndex(idxId, ts.getType(CAS.TYPE_NAME_FS_ARRAY));
      assertEquals(countIndexMembers(arrayIndexFSArray), 0);
      FSIndex<FeatureStructure> arrayIndexAnnotArray = ir.getIndex(idxId, annotArrayType);
      assertNull(arrayIndexAnnotArray);
    } catch (ResourceInitializationException e) {
      assertTrue(false);
    }
  }

  private static final int countIndexMembers(FSIndex<? extends FeatureStructure> idx) {
    FSIterator<? extends FeatureStructure> it = idx.iterator();
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }
    return count;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.ae.destroy();
  }

  public void process(CAS aCAS, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    // Do nothing.
  }

  public void initialize(AnnotatorContext aContext) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    // do nothing
  }

  public void typeSystemInit(TypeSystem aTypeSystem) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    // do nothing
  }

  public void reconfigure() throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    // do nothing
  }

  public void destroy() {
    // do nothing
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(ArrayIndexTest.class);
  }

}
