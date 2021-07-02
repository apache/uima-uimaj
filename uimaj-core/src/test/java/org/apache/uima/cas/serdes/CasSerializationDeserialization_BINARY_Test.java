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

import static java.nio.file.Files.walk;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.SerialFormat.BINARY;
import static org.apache.uima.cas.serdes.SerDesCasIOUtils.desser;
import static org.apache.uima.cas.serdes.SerDesCasIOUtils.ser;
import static org.apache.uima.cas.serdes.SerDesCasIOUtils.serdes;
import static org.apache.uima.util.CasLoadMode.DEFAULT;
import static org.apache.uima.util.CasLoadMode.REINIT;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.serdes.datasuites.TestSuiteData_CasBornInMemory;
import org.apache.uima.cas.serdes.datasuites.TestSuiteData_XmiFiles;
import org.apache.uima.cas.serdes.scenario.DesSerTestScenario;
import org.apache.uima.cas.serdes.scenario.SerDesTestScenario;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CasSerializationDeserialization_BINARY_Test {

  private static final String CLASSNAME = MethodHandles.lookup().lookupClass().getSimpleName();

  private static final Path TEST_RESOURCE_PATH = Paths.get("src", "test", "resources", CLASSNAME);
  private static final Path SER_REF_BASE_PATH = TEST_RESOURCE_PATH.resolve("ser-ref");
  private static final Path ONE_WAY_REFERENCE_BASE_PATH = TEST_RESOURCE_PATH.resolve("one-way");

  private static final Path TEST_OUTPUT_PATH = Paths.get("target", "test-output", CLASSNAME);
  private static final Path ONE_WAY_TARGET_BASE_PATH = TEST_OUTPUT_PATH.resolve("one-way");
  private static final Path ROUND_TRIP_TARGET_BASE_PATH = TEST_OUTPUT_PATH.resolve("round-trip");
  private static final Path SER_REF_TARGET_BASE_PATH = TEST_OUTPUT_PATH.resolve("ser-ref");

  private static final SerialFormat FORMAT = BINARY;
  private static final String CAS_FILE_NAME = "cas.bin";

  /**
   * SERIALIZE -> COMARE-TO-REFERENCE scenarios using the example CASes provided by
   * {@link TestSuiteData_CasBornInMemory}.
   */
  private static List<SerRefTestScenario> serRefScenarios() {
    List<SerRefTestScenario> confs = new ArrayList<>();

    for (CasSourceTargetConfiguration data : TestSuiteData_CasBornInMemory.configurations()) {
      confs.add(new SerRefTestScenario( //
              SER_REF_BASE_PATH, //
              SER_REF_TARGET_BASE_PATH, //
              data, //
              CAS_FILE_NAME, //
              (cas, path) -> ser(cas, path, FORMAT)));
    }

    return confs;
  }

  /**
   * SERIALIZE -> DESERIALIZE scenarios using the example CASes provided by
   * {@link TestSuiteData_CasBornInMemory} and applying them to each of the configured
   * serialization/deserialization cycles.
   */
  private static List<SerDesTestScenario> serDesScenarios() {
    List<CasSerDesCycleConfiguration> cycles = asList( //
            new CasSerDesCycleConfiguration(FORMAT + " / DEFAULT", //
                    (a, b) -> serdes(a, b, FORMAT, DEFAULT)),
            new CasSerDesCycleConfiguration(FORMAT + " / REINIT", //
                    (a, b) -> serdes(a, b, FORMAT, REINIT)));

    List<SerDesTestScenario> confs = new ArrayList<>();

    for (CasSerDesCycleConfiguration cycle : cycles) {
      for (CasSourceTargetConfiguration data : TestSuiteData_CasBornInMemory.configurations()) {
        confs.add(new SerDesTestScenario(data, cycle));
      }
    }

    return confs;
  }

  /**
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  private static List<DesSerTestScenario> roundTripDesSerScenarios() throws Exception {
    List<DesSerTestScenario> confs = new ArrayList<>();

    try (Stream<Path> fileStream = walk(SER_REF_BASE_PATH, 2)
            .filter(p -> p.getFileName().toString().equals(CAS_FILE_NAME))) {

      fileStream.forEach(dataFile -> confs.add(DesSerTestScenario.builder() //
              .withTitle(dataFile.getParent().getFileName().toString())
              .withTargetBasePath(ROUND_TRIP_TARGET_BASE_PATH) //
              .withCasFile(dataFile) // source / reference (round-trip)
              .withCycle((a, b) -> desser(CasCreationUtils.createCas(), a, b, FORMAT, REINIT))
              .build()));
    }

    return confs;
  }

  /**
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  private static List<SerRefTestScenario> oneWayDesSerScenarios() throws Exception {
    List<SerRefTestScenario> confs = new ArrayList<>();

    for (CasSourceTargetConfiguration conf : TestSuiteData_XmiFiles.configurations()) {
      confs.add(SerRefTestScenario.builder() //
              .withTitle(conf.getTitle()) //
              .withSourceCasSupplier(conf::createSourceCas) //
              .withTargetCasFile(
                      ONE_WAY_TARGET_BASE_PATH.resolve(conf.getTitle()).resolve(CAS_FILE_NAME))
              .withReferenceCasFile(
                      ONE_WAY_REFERENCE_BASE_PATH.resolve(conf.getTitle()).resolve(CAS_FILE_NAME)) //
              .withSerializer((cas, path) -> ser(cas, path, FORMAT)) //
              .build());
    }

    return confs;
  }

  @ParameterizedTest
  @MethodSource("serRefScenarios")
  public void serializeAndCompareToReferenceTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("serDesScenarios")
  public void serializeDeserializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("roundTripDesSerScenarios")
  public void roundTripDeserializeSerializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("oneWayDesSerScenarios")
  public void oneWayDeserializeSerializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }
}
