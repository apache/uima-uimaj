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
package org.apache.uima.json.jsoncas2;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JsonCas2_FsAsArray_PerformanceTest {

  private static final int ITERATIONS = 100;
  private static final int CAS_SIZE = 1000;

  private static MultiFeatureRandomCasGenerator generator;
  private static JsonCas2Serializer jsonSerializer;
  private static JsonCas2Deserializer jsonDeserializer;
  private static CAS randomizedCas;
  private static String randomizedCasJson;
  private static byte[] randomizedCasJsonUtf8Bytes;

  @BeforeAll
  public static void setup() throws Exception {
    generator = MultiFeatureRandomCasGenerator.builder() //
            .withRandomGenerator(new Random(123456l)) //
            .withSize(CAS_SIZE).build();

    TypeSystemDescription tsd = generator.generateTypeSystem();
    randomizedCas = generator.generateCas(tsd);

    jsonSerializer = new JsonCas2Serializer();
    jsonSerializer.setFsMode(FeatureStructuresMode.AS_ARRAY);
    jsonSerializer.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);

    jsonDeserializer = new JsonCas2Deserializer();
    jsonDeserializer.setFsMode(jsonSerializer.getFsMode());

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      jsonSerializer.serialize(randomizedCas, bos);
      randomizedCasJson = new String(bos.toByteArray(), UTF_8);
    }

    randomizedCasJsonUtf8Bytes = randomizedCasJson.getBytes(UTF_8);
  }

  @Test
  public void jsonSerialization() throws Exception {
    long start = currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        jsonSerializer.serialize(randomizedCas, bos);
      }
    }
    long end = currentTimeMillis();

    System.out.printf(
            "JSON Serializing %d CASes with %d feature stuctures each took %s ms (%d bytes each)%n",
            ITERATIONS, CAS_SIZE, end - start, randomizedCasJsonUtf8Bytes.length);
  }

  @Test
  public void jsonDeserialization() throws Exception {

    long start = currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++) {
      try (ByteArrayInputStream bos = new ByteArrayInputStream(randomizedCasJsonUtf8Bytes)) {
        jsonDeserializer.deserialize(bos, randomizedCas);
      }
    }
    long end = currentTimeMillis();

    System.out.printf(
            "JSON Deserializing %d CASes with %d feature stuctures each took %s ms (%d bytes each)%n",
            ITERATIONS, CAS_SIZE, end - start, randomizedCasJsonUtf8Bytes.length);
  }
}
