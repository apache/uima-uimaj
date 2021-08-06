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

import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.serdes.TestType.ONE_WAY;
import static org.apache.uima.cas.serdes.TestType.SER_REF;
import static org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite.XMI_SUITE_BASE_PATH;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CasSerializationDeserialization_JsonCas2_FsAsObject_Test {

  private static final String CAS_FILE_NAME = "data.json";
  // private static final int RANDOM_CAS_ITERATIONS = 20;
  //
  // private static final List<CasSerDesCycleConfiguration> serDesCycles = asList( //
  // new CasSerDesCycleConfiguration(FORMAT + " / DEFAULT", //
  // (a, b) -> serdes(a, b, FORMAT, DEFAULT)),
  // new CasSerDesCycleConfiguration(FORMAT + " / LENIENT", //
  // (a, b) -> serdes(a, b, FORMAT, LENIENT)));
  //
  // private static final List<CasDesSerCycleConfiguration> desSerCycles = asList( //
  // new CasDesSerCycleConfiguration(FORMAT + " / DEFAULT", //
  // (a, b) -> desser(createCas(), a, b, FORMAT, DEFAULT)),
  // new CasDesSerCycleConfiguration(FORMAT + " / LENIENT", //
  // (a, b) -> desser(createCas(), a, b, FORMAT, LENIENT)));

  private static void ser(CAS aSourceCas, Path aTargetCasFile) throws IOException {
    JsonCas2Serializer serializer = new JsonCas2Serializer();
    serializer.setFsMode(FeatureStructuresMode.AS_OBJECT);
    serializer.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);
    serializer.serialize(aSourceCas, aTargetCasFile.toFile());
  }

  private static List<SerRefTestScenario> serRefScenarios() {
    Class<?> caller = CasSerializationDeserialization_JsonCas2_FsAsObject_Test.class;
    return ProgrammaticallyCreatedCasDataSuite.configurations().stream()
            .map(conf -> SerRefTestScenario.builder(caller, conf, SER_REF, CAS_FILE_NAME)
                    .withSerializer((cas, path) -> ser(cas, path)).build())
            .collect(toList());
  }

  private static List<SerRefTestScenario> oneWayDesSerScenarios() throws Exception {
    Class<?> caller = CasSerializationDeserialization_JsonCas2_FsAsObject_Test.class;
    return XmiFileDataSuite
            .configurations(Paths.get("..", "uimaj-core").resolve(XMI_SUITE_BASE_PATH)).stream()
            .map(conf -> SerRefTestScenario.builder(caller, conf, ONE_WAY, CAS_FILE_NAME)
                    .withSerializer((cas, path) -> ser(cas, path)).build())
            .collect(toList());
  }

  // private static List<DesSerTestScenario> roundTripDesSerScenarios() throws Exception {
  // return SerDesCasIOTestUtils.roundTripDesSerScenarios(desSerCycles, CAS_FILE_NAME);
  // }
  //
  // private static List<SerDesTestScenario> serDesScenarios() {
  // return SerDesCasIOTestUtils.serDesScenarios(serDesCycles);
  // }
  //
  // private static List<SerDesTestScenario> randomSerDesScenarios() {
  // return SerDesCasIOTestUtils.randomSerDesScenarios(serDesCycles, RANDOM_CAS_ITERATIONS);
  // }

  @ParameterizedTest
  @MethodSource("serRefScenarios")
  public void serializeAndCompareToReferenceTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("oneWayDesSerScenarios")
  public void oneWayDeserializeSerializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  // @ParameterizedTest
  // @MethodSource("serDesScenarios")
  // public void serializeDeserializeTest(Runnable aScenario) throws Exception {
  // aScenario.run();
  // }
  //
  // @ParameterizedTest
  // @MethodSource("randomSerDesScenarios")
  // public void randomizedSerializeDeserializeTest(Runnable aScenario) throws Exception {
  // aScenario.run();
  // }
  //
  // @ParameterizedTest
  // @MethodSource("roundTripDesSerScenarios")
  // public void roundTripDeserializeSerializeTest(Runnable aScenario) throws Exception {
  // aScenario.run();
  // }
}
