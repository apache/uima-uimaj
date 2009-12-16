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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;


public class TypeSystemReinitTest extends TestCase {
  public void testReinitCASCompleteSerializer() throws Exception {
    try {
      AnalysisEngineDescription aed = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));
      TypeSystemDescription tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(getClass().getResource("/org/apache/uima/examples/SourceDocumentInformation.xml")));

      List<MetaDataObject> l = new ArrayList<MetaDataObject>();
      l.add(aed);
      l.add(tsd);
      CAS cas1 = CasCreationUtils.createCas(l);
      cas1.setDocumentText("foo");
      CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr) cas1);

      CAS tcas2 = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
      CASImpl cas2 = ((CASImpl) tcas2).getBaseCAS();
      tcas2.setDocumentText("bar");

      // reinit
      //  This uses cas2 which only has a base type system to start, 
      //    and loads it from a complete serialization which has other new types
      cas2.reinit(ser);
      CAS tcas3 = cas2.getCurrentView();

      assertTrue(tcas2 == tcas3);
      assertNotNull(cas1.getTypeSystem().getType("NamedEntity"));
      assertNotNull(tcas3.getTypeSystem().getType("NamedEntity"));

      FeatureStructure fs = tcas3.createFS(tcas3.getTypeSystem().getType("NamedEntity"));
      tcas3.getIndexRepository().addFS(fs);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  public void testReinitCASCompleteSerializerWithArrays() throws Exception {
    try {
      AnalysisEngineDescription aed = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("ExampleTae/arrayTypeSerialization.xml")));
     
      CAS cas1 = CasCreationUtils.createCas(aed);
      cas1.setDocumentText("foo");
      CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr) cas1);

      CAS tcas2 = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
      CASImpl cas2 = ((CASImpl) tcas2).getBaseCAS();
      tcas2.setDocumentText("bar");

      // reinit
      //  This uses cas2 which only has a base type system to start, 
      //    and loads it from a complete serialization which has other new types
      cas2.reinit(ser);
      CAS tcas3 = cas2.getCurrentView();

      assertTrue(tcas2 == tcas3);
      assertNotNull(cas1.getTypeSystem().getType("Test.ArrayType"));
      assertNotNull(tcas3.getTypeSystem().getType("Test.ArrayType"));
      
      TypeSystemImpl ts = (TypeSystemImpl)cas2.getTypeSystem();
      Type arrayType = ts.getType("Test.ArrayType");
      Feature arrayFeat = arrayType.getFeatureByBaseName("arrayFeature");
      TypeImpl featRange = (TypeImpl)(arrayFeat.getRange());
     
      assertTrue(ts.ll_isArrayType(featRange.getCode()));
      assertFalse(arrayFeat.isMultipleReferencesAllowed());
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
