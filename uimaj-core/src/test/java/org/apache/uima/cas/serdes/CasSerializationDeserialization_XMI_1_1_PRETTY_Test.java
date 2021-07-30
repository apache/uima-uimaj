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

import static java.util.Arrays.asList;
import static org.apache.uima.cas.SerialFormat.XMI_1_1_PRETTY;
import static org.apache.uima.cas.serdes.SerDesAssuptions.assumeNotKnownToFail;
import static org.apache.uima.cas.serdes.SerDesCasIOTestUtils.desser;
import static org.apache.uima.cas.serdes.SerDesCasIOTestUtils.serdes;
import static org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite.DATA_XMI;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasLoadMode.DEFAULT;
import static org.apache.uima.util.CasLoadMode.LENIENT;

import java.util.List;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.serdes.scenario.DesSerTestScenario;
import org.apache.uima.cas.serdes.scenario.SerDesTestScenario;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.cas.serdes.transitions.CasDesSerCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CasSerializationDeserialization_XMI_1_1_PRETTY_Test {

  private static final SerialFormat FORMAT = XMI_1_1_PRETTY;
  private static final String CAS_FILE_NAME = DATA_XMI;
  private static final int RANDOM_CAS_ITERATIONS = 20;

  private static final List<CasSerDesCycleConfiguration> serDesCycles = asList( //
          new CasSerDesCycleConfiguration(FORMAT + " / DEFAULT", //
                  (a, b) -> serdes(a, b, FORMAT, DEFAULT)),
          new CasSerDesCycleConfiguration(FORMAT + " / LENIENT", //
                  (a, b) -> serdes(a, b, FORMAT, LENIENT)));

  private static final List<CasDesSerCycleConfiguration> desSerCycles = asList( //
          new CasDesSerCycleConfiguration(FORMAT + " / DEFAULT", //
                  (a, b) -> desser(createCas(), a, b, FORMAT, DEFAULT)),
          new CasDesSerCycleConfiguration(FORMAT + " / LENIENT", //
                  (a, b) -> desser(createCas(), a, b, FORMAT, LENIENT)));

  private static List<SerRefTestScenario> serRefScenarios() {
    return SerDesCasIOTestUtils.serRefScenarios(FORMAT, CAS_FILE_NAME);
  }

  private static List<SerRefTestScenario> oneWayDesSerScenarios() throws Exception {
    return SerDesCasIOTestUtils.oneWayDesSerScenarios(FORMAT, CAS_FILE_NAME);
  }

  private static List<DesSerTestScenario> roundTripDesSerScenarios() throws Exception {
    return SerDesCasIOTestUtils.roundTripDesSerScenarios(desSerCycles, CAS_FILE_NAME);
  }

  private static List<SerDesTestScenario> serDesScenarios() {
    return SerDesCasIOTestUtils.serDesScenarios(serDesCycles);
  }

  private static List<SerDesTestScenario> randomSerDesScenarios() {
    return SerDesCasIOTestUtils.randomSerDesScenarios(serDesCycles, RANDOM_CAS_ITERATIONS);
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
  @MethodSource("randomSerDesScenarios")
  public void randomizedSerializeDeserializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("roundTripDesSerScenarios")
  public void roundTripDeserializeSerializeTest(Runnable aScenario) throws Exception {
    assumeNotKnownToFail(aScenario, //
            ".*casWithSofaDataArray", "Round-trip does not exactly preserve XMI IDs");

    aScenario.run();
  }

  @ParameterizedTest
  @MethodSource("oneWayDesSerScenarios")
  public void oneWayDeserializeSerializeTest(Runnable aScenario) throws Exception {
    aScenario.run();
  }
}
