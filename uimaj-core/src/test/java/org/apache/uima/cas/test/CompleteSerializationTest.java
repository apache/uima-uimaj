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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CasCompare;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelException;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.CasCreationUtils;

import junit.framework.TestCase;

/**
 * CAS complete serialization test class
 * 
 */
public class CompleteSerializationTest extends TestCase {

  /**
   * Constructor for CASTest.
   * 
   * @param arg0
   */
  public CompleteSerializationTest(String arg0) {
    super(arg0);
  }

  public void testSerialization() throws Exception {
    try {
      CASMgr cas = null;
      try {
        cas = (CASMgr) CASInitializer.initCas(new CASTestSetup(), null);
      } catch (Exception e) {
        assertTrue(false);
      }
      ((CAS) cas).setDocumentText("Create the sofa for the inital view");
      assertTrue(((CASImpl) cas).isBackwardCompatibleCas());
      CASCompleteSerializer ser = Serialization.serializeCASComplete(cas);

      // deserialize into a new CAS with a type system that only contains the builtins
      CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);

      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(((CASImpl) newCas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
      assertEquals("Create the sofa for the inital view", newCas.getDocumentText());

      // make sure JCas can be created
      newCas.getJCas();

      // deserialize into newCas a second time (OF bug found 7/7/2006)
      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(cas.getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
      boolean caught = false;
      try {
      ((LowLevelCAS)newCas).ll_enableV2IdRefs();
      } catch (IllegalStateException e) {
        caught = true;
      }
      assertTrue(caught);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    

  }

  public void testSerializationV2IdRefs() throws Exception {
    try (AutoCloseableNoException a = LowLevelCAS.ll_defaultV2IdRefs()){
      CAS cas = null;
      JCas jcas = null;
      
      try {
        cas =  CASInitializer.initCas(new CASTestSetup(), null);
        jcas = cas.getJCas();
      } catch (Exception e) {
        assertTrue(false);
      }
      cas.setDocumentText("Create the sofa for the inital view");
      assertTrue(((CASImpl) cas).isBackwardCompatibleCas());
      
      Type t1 = cas.getTypeSystem().getType(CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE);
      Feature feat1 = t1.getFeatureByBaseName(CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE_FEAT);


      FSArray<FeatureStructure> fsa1 = new FSArray<>(jcas, 1);
      FeatureStructure f1 = cas.createFS(t1);
      f1.setFeatureValue(feat1, fsa1);

      Annotation myAnnot = new Annotation(jcas);
      
      fsa1.set(0, myAnnot);

      myAnnot = new Annotation(jcas);
      int id = myAnnot._id();
      myAnnot = new Annotation(jcas);
      int id2 = myAnnot._id();
      assertEquals(4, id2 - id);  // type code, sofa, begin, end
            
      CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr)cas);

      // deserialize into a new CAS with a type system that only contains the builtins
      CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);

      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }      
      LowLevelCAS ll = newCas.getLowLevelCAS();
      boolean caught = false;
      try {
      assertEquals(id, ll.ll_getFSForRef(id)._id());
      } catch (LowLevelException e ) {
        caught = true;
      }
      assertFalse(caught);
      
      CasCompare cc = new CasCompare((CASImpl)cas,  (CASImpl)newCas);
      cc.compareIds(true);
      assertTrue(cc.compareCASes());
      
      Serialization.deserializeCASComplete(ser, (CASImpl) newCas);     
      assertEquals(id, ll.ll_getFSForRef(id)._id());
      assertEquals(id2, ll.ll_getFSForRef(id2)._id());        
      
      assertEquals(id2, ll.ll_getFSForRef(id2)._id());        
      assertTrue(((CASImpl) newCas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
      assertEquals("Create the sofa for the inital view", newCas.getDocumentText());

      // make sure JCas can be created
      newCas.getJCas();

      // deserialize into newCas a second time (OF bug found 7/7/2006)
      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(((CASMgr)cas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
