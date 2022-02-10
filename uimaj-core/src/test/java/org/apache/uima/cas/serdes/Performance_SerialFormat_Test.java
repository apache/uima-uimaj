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
package org.apache.uima.cas.serdes;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import java.util.Random;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator;
import org.apache.uima.util.CasIOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Performance_SerialFormat_Test {

  private static final int ITERATIONS = 100;
  private static final int SIZE = 1_000;

  @ParameterizedTest
  @EnumSource(value = SerialFormat.class, mode = EXCLUDE, names = { "UNKNOWN",
      "COMPRESSED_PROJECTION" })
  public void jsonSerialization(SerialFormat aFormat) throws Exception {
    PerformanceTestRunner runner = buildRunner(aFormat);
    long serDuration = runner.measureSerializationPerformance();

    System.out.printf("[%23s] %d CASes with %d feature structures (%7d bytes each)%n", aFormat,
            ITERATIONS, SIZE, runner.getDataSize());

    System.out.printf("[%23s]   %6s ms serialization    %6.2f fs/sec  %6.2f CAS/sec %n", aFormat,
            serDuration, (ITERATIONS * SIZE) / (serDuration / 1000.0d),
            ITERATIONS / (serDuration / 1000.0d));

    long desDuration = runner.measureDeserializationPerformance();

    System.out.printf("[%23s]   %6s ms deserialization  %6.2f fs/sec  %6.2f CAS/sec %n", aFormat,
            desDuration, (ITERATIONS * SIZE) / (desDuration / 1000.0d),
            ITERATIONS / (desDuration / 1000.0d));
  }

  public PerformanceTestRunner buildRunner(SerialFormat aFormat) throws Exception {
    return PerformanceTestRunner.builder() //
            .withIterations(ITERATIONS) //
            .withDeserializer(CasIOUtils::load)
            .withSerializer((cas, os) -> CasIOUtils.save(cas, os, aFormat)) //
            .withGenerator(MultiFeatureRandomCasGenerator.builder() //
                    .withRandomGenerator(new Random(123456l)) //
                    .withSize(SIZE) //
                    .build()) //
            .build();
  }
}
