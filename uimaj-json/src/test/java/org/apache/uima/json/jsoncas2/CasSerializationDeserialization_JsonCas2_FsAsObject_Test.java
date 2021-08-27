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

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.serdes.TestType.ONE_WAY;
import static org.apache.uima.cas.serdes.TestType.SER_DES;
import static org.apache.uima.cas.serdes.TestType.SER_REF;
import static org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite.XMI_SUITE_BASE_PATH;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.SerDesCasIOTestUtils;
import org.apache.uima.cas.serdes.datasuites.MultiFeatureRandomCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.MultiTypeRandomCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite;
import org.apache.uima.cas.serdes.scenario.DesSerTestScenario;
import org.apache.uima.cas.serdes.scenario.SerDesTestScenario;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.cas.serdes.transitions.CasDesSerCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CasSerializationDeserialization_JsonCas2_FsAsObject_Test {

  private static final String CAS_FILE_NAME = "data.json";
  private static final int RANDOM_CAS_ITERATIONS = 20;

  private static final List<CasSerDesCycleConfiguration> serDesCycles = asList( //
          new CasSerDesCycleConfiguration("DEFAULT", //
                  (a, b) -> serdes(a, b)));
  // new CasSerDesCycleConfiguration(FORMAT + " / LENIENT", //
  // (a, b) -> serdes(a, b, FORMAT, LENIENT)));

  private static final List<CasDesSerCycleConfiguration> desSerCycles = asList( //
          new CasDesSerCycleConfiguration("DEFAULT", //
                  (a, b) -> desser(CasCreationUtils.createCas(), a, b)));

  private static void ser(CAS aSourceCas, Path aTargetCasFile) throws IOException {
    JsonCas2Serializer serializer = new JsonCas2Serializer();
    serializer.setFsMode(FeatureStructuresMode.AS_OBJECT);
    serializer.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);
    serializer.serialize(aSourceCas, aTargetCasFile.toFile());
  }

  private static void des(CAS aTargetCas, Path aSourceCasFile) throws IOException {
    JsonCas2Deserializer deserializer = new JsonCas2Deserializer();
    deserializer.setFsMode(FeatureStructuresMode.AS_OBJECT);
    deserializer.deserialize(aSourceCasFile.toFile(), aTargetCas);
  }

  public static void serdes(CAS aSourceCas, CAS aTargetCas) throws Exception {
    byte[] buffer;
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      JsonCas2Serializer serializer = new JsonCas2Serializer();
      serializer.setFsMode(FeatureStructuresMode.AS_OBJECT);
      serializer.setSofaMode(SofaMode.AS_REGULAR_FEATURE_STRUCTURE);
      serializer.serialize(aSourceCas, os);
      buffer = os.toByteArray();
    }

    Path targetFile = SER_DES.getTargetFolder(lookup().lookupClass()).resolve(CAS_FILE_NAME);
    Files.createDirectories(targetFile.getParent());
    try (OutputStream os = Files.newOutputStream(targetFile)) {
      os.write(buffer);
    }

    try (InputStream is = new ByteArrayInputStream(buffer)) {
      JsonCas2Deserializer deserializer = new JsonCas2Deserializer();
      deserializer.setFsMode(FeatureStructuresMode.AS_OBJECT);
      deserializer.deserialize(is, aTargetCas);
    }
  }

  public static void desser(CAS aBufferCas, Path aSourceCasPath, Path aTargetCasPath)
          throws Exception {
    des(aBufferCas, aSourceCasPath);
    ser(aBufferCas, aTargetCasPath);
  }

  private static List<SerRefTestScenario> serRefScenarios() {
    Class<?> caller = CasSerializationDeserialization_JsonCas2_FsAsObject_Test.class;
    return ProgrammaticallyCreatedCasDataSuite.builder().build().stream()
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

  private static List<DesSerTestScenario> roundTripDesSerScenarios() throws Exception {
    return SerDesCasIOTestUtils.roundTripDesSerScenariosComparingFileContents(desSerCycles,
            CAS_FILE_NAME);
  }

  private static List<SerDesTestScenario> serDesScenarios() {
    return SerDesCasIOTestUtils.programmaticSerDesScenarios(serDesCycles);
  }

  private static List<SerDesTestScenario> randomSerDesScenarios() {
    return SerDesCasIOTestUtils.serDesScenarios(serDesCycles,
            MultiFeatureRandomCasDataSuite.builder().withIterations(RANDOM_CAS_ITERATIONS).build(),
            MultiTypeRandomCasDataSuite.builder().withIterations(RANDOM_CAS_ITERATIONS).build());
  }

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

  @ParameterizedTest
  @MethodSource("serDesScenarios")
  public void serializeDeserializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("randomSerDesScenarios")
  public void randomizedSerializeDeserializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("roundTripDesSerScenarios")
  public void roundTripDeserializeSerializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }
}
