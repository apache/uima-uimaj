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

import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.test.EnrichedEntity;
import org.apache.uima.cas.test.FeatureMap;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CustomSerializationTest {

  private static void toXmiFile(JCas jCas, String filePath) throws IOException {
    File file = new File(filePath);
    File dir = file.getParentFile();
    if (dir != null)
      dir.mkdirs();
    try (OutputStream bytes = new BufferedOutputStream(new FileOutputStream(filePath))) {
      CasIOUtils.save(jCas.getCas(), bytes, SerialFormat.XMI);
    }
  }

  private static InputStream toByteInputStream(JCas jCas, SerialFormat format) throws IOException {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      CasIOUtils.save(jCas.getCas(), bytes, format);
      return new ByteArrayInputStream(bytes.toByteArray());
    }
  }

  private static void load(JCas jCas, InputStream inputStream) throws IOException {
    CasIOUtils.load(inputStream, jCas.getCas());
  }

  private EnrichedEntity fixture()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/CustomSerializable.xml");
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), null);
    JCas jCas = cas.getJCas();
    jCas.setDocumentText("Hello World");
    EnrichedEntity entity = new EnrichedEntity(jCas, 0, jCas.getDocumentText().length());
    entity.setFeatures(new FeatureMap(jCas));
    entity.getFeatures().put("m1", 0.1);
    entity.getFeatures().put("m2", 0.2);
    entity.addToIndexes();
    return entity;
  }

  public void verifyFeatureMapDeserialization(SerialFormat format)
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    EnrichedEntity entity = fixture();
    JCas jCas = entity.getJCas();
    Map<String, Double> expected = entity.getFeatures().asKeyValueMap();

    load(jCas, toByteInputStream(jCas, format));
    List<EnrichedEntity> annotations = jCas.select(EnrichedEntity.class)
            .collect(Collectors.toList());
    assertEquals(1, annotations.size());
    assertEquals(expected, annotations.get(0).getFeatures().asKeyValueMap());
  }

  @Test
  @Disabled
  public void saveFixtureToXmiFile()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    EnrichedEntity test = fixture();
    toXmiFile(test.getJCas(), "output/custom-serialization-fixture.xmi");
  }

  @Test
  public void verifyCustomSerializationForXmi()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.XMI);
  }

  @Test
  public void verifyCustomSerializationForCompressedBinary4()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.COMPRESSED);
  }

  @Test
  public void verifyCustomSerializationForCompressedFilteredEmbeddedTSAndIndexBinary4()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.COMPRESSED_TSI);
  }

  @Test
  public void verifyCustomSerializationForCompressedFilteredBinary6()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.COMPRESSED_FILTERED);
  }

  @Test
  public void verifyCustomSerializationForCompressedFilteredEmbeddedTSBinary6()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.COMPRESSED_FILTERED_TS);
  }

  @Test
  public void verifyCustomSerializationForCompressedFilteredEmbeddedTSAndIndexBinary6()
          throws CASException, ResourceInitializationException, IOException, InvalidXMLException {
    verifyFeatureMapDeserialization(SerialFormat.COMPRESSED_FILTERED_TSI);
  }

}
