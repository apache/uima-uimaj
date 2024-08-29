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
package org.apache.uima.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CasIOUtilsAlwaysHoldOnTest {
  private static String oldHoldOntoFss;

  @BeforeAll
  static void setUp() throws Exception {
    // Must set this to true, otherwise the test will not fail. Setting it to true will cause
    // FSes which are not in any index to still be serialized out. When reading this data back,
    // UIMA will find the non-indexed DocumentAnnotation and add it back without checking whether
    // is was actually indexed or not.
    oldHoldOntoFss = System.setProperty(CASImpl.ALWAYS_HOLD_ONTO_FSS, "true");
  }

  @AfterAll
  static void tearDown() throws Exception {
     System.setProperty(CASImpl.ALWAYS_HOLD_ONTO_FSS, oldHoldOntoFss);
  }

  @Test
  void thatDocumentAnnotationIsNotResurrected() throws Exception {
    var customDocAnnoTypeName = "org.apache.uima.testing.CustomDocumentAnnotation";

    var tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    tsd.addType(customDocAnnoTypeName, "", CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    var cas = CasCreationUtils.createCas(tsd, null, null);

    // Initialize the default document annotation
    // ... then immediately remove it from the indexes.
    FeatureStructure da = cas.getDocumentAnnotation();

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName())
            .containsExactly(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    cas.removeFsFromIndexes(da);

    // Now add a new document annotation of our custom type
    var cda = cas.createFS(cas.getTypeSystem().getType(customDocAnnoTypeName));
    cas.addFsToIndexes(cda);

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()).containsExactly(customDocAnnoTypeName);

    // Serialize to a buffer
    var bos = new ByteArrayOutputStream();
    CasIOUtils.save(cas, bos, SerialFormat.SERIALIZED_TSI);

    // Deserialize from the buffer
    var bis = new ByteArrayInputStream(bos.toByteArray());
    CasIOUtils.load(bis, cas);

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()).containsExactly(customDocAnnoTypeName);
  }
}
