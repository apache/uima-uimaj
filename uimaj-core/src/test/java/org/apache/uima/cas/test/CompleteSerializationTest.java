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

import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeWithCompression;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasIOUtils.load;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CasCompare;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * CAS complete serialization test class
 */
class CompleteSerializationTest {

  @BeforeAll
  static void setupClass() {
    System.setProperty("uima.enable_strict_type_source_check", "true");
  }

  @AfterAll
  static void tearDownClass() {
    System.getProperties().remove("uima.enable_strict_type_source_check");
  }

  @Test
  void testSerialization() throws Exception {
    CASMgr cas = (CASMgr) CASInitializer.initCas(new CASTestSetup(), null);

    ((CAS) cas).setDocumentText("Create the sofa for the inital view");
    assertThat(((CASImpl) cas).isBackwardCompatibleCas()).isTrue();
    CASCompleteSerializer ser = Serialization.serializeCASComplete(cas);

    // deserialize into a new CAS with a type system that only contains the builtins
    CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);

    Serialization.deserializeCASComplete(ser, (CASImpl) newCas);

    assertThat(((CASImpl) newCas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1)).isNotNull();
    assertThat(((CASImpl) newCas).isBackwardCompatibleCas()).isTrue();
    assertThat(newCas.getDocumentText()).isEqualTo("Create the sofa for the inital view");

    // make sure JCas can be created
    newCas.getJCas();

    // deserialize into newCas a second time (OF bug found 7/7/2006)
    Serialization.deserializeCASComplete(ser, (CASImpl) newCas);

    assertThat(cas.getTypeSystemMgr().getType(CASTestSetup.GROUP_1)).isNotNull();
    assertThat(((CASImpl) newCas).isBackwardCompatibleCas()).isTrue();

    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> ((LowLevelCAS) newCas).ll_enableV2IdRefs());
  }

  @Test
  void testSerializationV2IdRefs() throws Exception {
    try (AutoCloseableNoException a = LowLevelCAS.ll_defaultV2IdRefs()) {
      CAS cas = CASInitializer.initCas(new CASTestSetup(), null);
      JCas jcas = cas.getJCas();

      cas.setDocumentText("Create the sofa for the inital view");
      assertThat(((CASImpl) cas).isBackwardCompatibleCas()).isTrue();

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
      assertThat(id2 - id).isEqualTo(4); // type code, sofa, begin, end

      CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr) cas);

      // deserialize into a new CAS with a type system that only contains the builtins
      CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);

      Serialization.deserializeCASComplete(ser, (CASImpl) newCas);

      LowLevelCAS ll = newCas.getLowLevelCAS();

      assertThat(ll.ll_getFSForRef(id)._id()).isEqualTo(id);

      CasCompare cc = new CasCompare((CASImpl) cas, (CASImpl) newCas);
      cc.compareIds(true);
      assertThat(cc.compareCASes()).isTrue();

      Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      assertThat(ll.ll_getFSForRef(id)._id()).isEqualTo(id);
      assertThat(ll.ll_getFSForRef(id2)._id()).isEqualTo(id2);

      assertThat(ll.ll_getFSForRef(id2)._id()).isEqualTo(id2);
      assertThat(((CASImpl) newCas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1)).isNotNull();
      assertThat(((CASImpl) newCas).isBackwardCompatibleCas()).isTrue();
      assertThat(newCas.getDocumentText()).isEqualTo("Create the sofa for the inital view");

      // make sure JCas can be created
      newCas.getJCas();

      // deserialize into newCas a second time (OF bug found 7/7/2006)
      Serialization.deserializeCASComplete(ser, (CASImpl) newCas);

      assertThat(((CASMgr) cas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1)).isNotNull();
      assertThat(((CASImpl) newCas).isBackwardCompatibleCas()).isTrue();
    }
  }

  @Test
  void thatReplacingTypeSystemInCasWorks() throws Exception {
    String initialViewText = "First view text";
    String secondViewId = "secondView";
    String secondViewText = "Second view text";

    // Construct a CAS with two views
    CAS cas = CasCreationUtils.createCas();
    cas.setDocumentText(initialViewText);
    cas.createView(secondViewId).setDocumentText(secondViewText);

    // Save the CAS data to a buffer
    TypeSystem originalTypeSystem = cas.getTypeSystem();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    serializeWithCompression(cas, buffer, originalTypeSystem);

    // Create a new type system
    TypeSystemDescription newTSD = new TypeSystemDescription_impl();
    newTSD.addType("my.Type", "", CAS.TYPE_NAME_ANNOTATION);

    // Replace the type system in the CAS with the new type system
    CAS tempCas = createCas(newTSD, null, null, null);
    CASCompleteSerializer serializer = serializeCASComplete((CASImpl) tempCas);
    deserializeCASComplete(serializer, (CASImpl) cas);

    // Write the CAS data from the buffer back into the CAS - this throws an exception if the
    // FSIndexRepositories are not properly reset (cf. UIMA-6352)
    load(new ByteArrayInputStream(buffer.toByteArray()), cas, originalTypeSystem);

    CAS secondView = cas.getView(secondViewId);
    assertThat(cas.getDocumentText()).isEqualTo(initialViewText);
    assertThat(secondView).isNotNull();
    assertThat(secondView.getDocumentText()).isEqualTo(secondViewText);
    assertThat(((FSIndexRepositoryImpl) cas.getIndexRepository()).getTypeSystemImpl()) //
            .as("Index repositories in both views use the same type system instance") //
            .isSameAs(
                    ((FSIndexRepositoryImpl) secondView.getIndexRepository()).getTypeSystemImpl());
  }
}
