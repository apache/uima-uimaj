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

import java.util.Random;

import org.apache.uima.cas.serdes.PerformanceTestRunner;
import org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Performance_JsonCas2_FsAsArray_Test {

  private static final int ITERATIONS = 100;
  private static final int SIZE = 1_000;

  private static JsonCas2Serializer jsonSerializer;
  private static JsonCas2Deserializer jsonDeserializer;

  @BeforeAll
  public static void setup() throws Exception {
    jsonSerializer = new JsonCas2Serializer();
    jsonSerializer.setFsMode(FeatureStructuresMode.AS_ARRAY);
    jsonSerializer.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);

    jsonDeserializer = new JsonCas2Deserializer();
    jsonDeserializer.setFsMode(jsonSerializer.getFsMode());
  }

  @Test
  public void jsonSerialization() throws Exception {
    PerformanceTestRunner runner = buildRunner();
    long serDuration = runner.measureSerializationPerformance();

    System.out.printf("[%23s] %d CASes with %d feature structures (%7d bytes each)%n", "JSON",
            ITERATIONS, SIZE, runner.getDataSize());

    System.out.printf("[%23s]   %6s ms serialization    %6.2f fs/sec  %6.2f CAS/sec %n", "JSON",
            serDuration, (ITERATIONS * SIZE) / (serDuration / 1000.0d),
            ITERATIONS / (serDuration / 1000.0d));

    long desDuration = runner.measureDeserializationPerformance();

    System.out.printf("[%23s]   %6s ms deserialization  %6.2f fs/sec  %6.2f CAS/sec %n", "JSON",
            desDuration, (ITERATIONS * SIZE) / (desDuration / 1000.0d),
            ITERATIONS / (desDuration / 1000.0d));
  }

  public PerformanceTestRunner buildRunner() throws Exception {
    return PerformanceTestRunner.builder() //
            .withIterations(ITERATIONS) //
            .withDeserializer(jsonDeserializer::deserialize)
            .withSerializer(jsonSerializer::serialize) //
            .withGenerator(MultiFeatureRandomCasGenerator.builder() //
                    .withRandomGenerator(new Random(123456l)) //
                    .withSize(SIZE) //
                    .build()) //
            .build();
  }
}
