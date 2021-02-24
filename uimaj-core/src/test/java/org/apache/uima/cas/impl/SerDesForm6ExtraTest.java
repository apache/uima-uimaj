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
package org.apache.uima.cas.impl;

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.SerialFormat.COMPRESSED_FILTERED_TSI;
import static org.apache.uima.cas.SerialFormat.XMI;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasIOUtils;
import org.junit.Test;

public class SerDesForm6ExtraTest {
  private final static String TYPE_NAME_ELEMENT = "Element";
  private final static String TYPE_NAME_ARRAY_HOLDER = "ArrayHolder";

  private final static String FEATURE_NAME_ARRAY = "array";

  private final static String NO_DESCRIPTION = "";

  @Test
  public void thatArraySubtypesCanBeDeserialized() throws Exception {

    CAS cas = prepCasWithNegativeDeltaReference();
    
    // Now finally test whether form 6 serialization can handle the Element[] type and the negative
    // delta reference
    byte[] form6Data;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      CasIOUtils.save(cas, bos, COMPRESSED_FILTERED_TSI);
      form6Data = bos.toByteArray();
    }
    
    try (ByteArrayInputStream bis = new ByteArrayInputStream(form6Data)) {
      TypeSystem ts = cas.getTypeSystem();
      Serialization.deserializeCAS(cas, bis, ts, null);
    }
  }

  private CAS prepCasWithNegativeDeltaReference() throws Exception
  {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType(TYPE_NAME_ELEMENT, NO_DESCRIPTION, TYPE_NAME_ANNOTATION);
    TypeDescription arrayHolderTypeDesc = tsd.addType(TYPE_NAME_ARRAY_HOLDER, NO_DESCRIPTION,
        TYPE_NAME_ANNOTATION);
    arrayHolderTypeDesc.addFeature(FEATURE_NAME_ARRAY, NO_DESCRIPTION, TYPE_NAME_FS_ARRAY,
        TYPE_NAME_ELEMENT, null);

    
    CAS cas = createCas(tsd, null, null);
    cas.setDocumentText("This is a test.");

    Type elementType = cas.getTypeSystem().getType(TYPE_NAME_ELEMENT);
    Type holderType = cas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOLDER);

    // BEGIN: Setup CAS so that the reference to element1 in holder1 is encoded as a negative delta
    // during binary serialization
    AnnotationFS element1 = cas.createAnnotation(elementType, 0, cas.getDocumentText().length());
    AnnotationFS element2 = cas.createAnnotation(elementType, 0, cas.getDocumentText().length());
    
    AnnotationFS holder2 = cas.createAnnotation(holderType, 0, 0);
    ArrayFS<FeatureStructure> array2 = cas.createArrayFS(1);
    array2.set(0, element2);
    holder2.setFeatureValue(holderType.getFeatureByBaseName(FEATURE_NAME_ARRAY), array2);
    
    AnnotationFS holder1 = cas.createAnnotation(holderType, 1, 1);
    ArrayFS<FeatureStructure> array1 = cas.createArrayFS(1);
    array1.set(0, element1);
    holder1.setFeatureValue(holderType.getFeatureByBaseName(FEATURE_NAME_ARRAY), array1);

    cas.addFsToIndexes(element1);
    cas.addFsToIndexes(element2);
    cas.addFsToIndexes(holder2);
    cas.addFsToIndexes(holder1);
    // END: Setup CAS so that the reference to element1 in holder1 is encoded as a negative delta
    
    // BEGIN: Setup Element[] type
    // We cannot directly create array subtypes - but they are created during de-serialization
    // e.g. of XMI files. So to trigger the creation of the Element[] type in the CAS, we need
    // to serialize to CAS and then back again.
    byte[] xmiData;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      CasIOUtils.save(cas, bos, XMI);
      xmiData = bos.toByteArray();
    }
    
    // While deserializing the XMI, the specific array subtype "Element[]" should be created
    try (ByteArrayInputStream bis = new ByteArrayInputStream(xmiData)) {
      CasIOUtils.load(bis, cas);
    }    
    
    Type elementArrayType = cas.getTypeSystem().getArrayType(elementType);
    assertThat(elementArrayType).isNotNull();
    // END: Setup Element[] type
    
    return cas;
  }
}
