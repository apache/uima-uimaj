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

import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.SerialFormat.XMI_PRETTY;
import static org.apache.uima.cas.serdes.TestType.ONE_WAY;
import static org.apache.uima.cas.serdes.TestType.ROUND_TRIP;
import static org.apache.uima.cas.serdes.TestType.SER_REF;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.serdes.datasuites.MultiFeatureRandomCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.MultiTypeRandomCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite;
import org.apache.uima.cas.serdes.scenario.DesSerTestScenario;
import org.apache.uima.cas.serdes.scenario.SerDesTestScenario;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.cas.serdes.transitions.CasDesSerCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.assertj.core.internal.Failures;

public class SerDesCasIOTestUtils {
  private static Class<?> getCallerClass() {
    try {
      return Class.forName(new Throwable().getStackTrace()[2].getClassName());
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    }
  }

  /**
   * SERIALIZE -> COMARE-TO-REFERENCE scenarios using the example CASes provided by
   * {@link ProgrammaticallyCreatedCasDataSuite}.
   */
  public static List<SerRefTestScenario> serRefScenarios(SerialFormat aFormat,
          String aCasFileName) {
    Class<?> caller = getCallerClass();
    return ProgrammaticallyCreatedCasDataSuite.configurations().stream()
            .map(conf -> SerRefTestScenario.builder(caller, conf, SER_REF, aCasFileName)
                    .withSerializer((cas, path) -> ser(cas, path, aFormat)) //
                    .build())
            .collect(toList());
  }

  /**
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  public static List<SerRefTestScenario> oneWayDesSerScenarios(SerialFormat aFormat,
          String aCasFileName) throws Exception {
    Class<?> caller = getCallerClass();
    return XmiFileDataSuite.configurations().stream()
            .map(conf -> SerRefTestScenario.builder(caller, conf, ONE_WAY, aCasFileName)
                    .withSerializer((cas, path) -> ser(cas, path, aFormat)) //
                    .build())
            .collect(toList());
  }

  /**
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  public static List<DesSerTestScenario> roundTripDesSerScenarios(
          Collection<CasDesSerCycleConfiguration> aDesSerCycles, String aCasFileName)
          throws Exception {
    Class<?> caller = getCallerClass();

    List<DesSerTestScenario> confs = new ArrayList<>();

    for (CasDesSerCycleConfiguration cycle : aDesSerCycles) {
      try (Stream<DesSerTestScenario.Builder> builders = DesSerTestScenario.builderCases(caller,
              cycle, ROUND_TRIP, aCasFileName)) {

        builders.map(builder -> builder.withCycle(cycle::performCycle).build()) //
                .forEach(confs::add);
      }
    }

    return confs;
  }

  /**
   * SERIALIZE -> DESERIALIZE scenarios using the example CASes provided by
   * {@link ProgrammaticallyCreatedCasDataSuite} and applying them to each of the configured
   * serialization/deserialization cycles.
   */
  public static List<SerDesTestScenario> serDesScenarios(
          Collection<CasSerDesCycleConfiguration> aSerDesCycles) {
    List<SerDesTestScenario> confs = new ArrayList<>();

    for (CasSerDesCycleConfiguration cycle : aSerDesCycles) {
      for (CasSourceTargetConfiguration data : ProgrammaticallyCreatedCasDataSuite
              .configurations()) {
        confs.add(new SerDesTestScenario(data, cycle));
      }
    }

    return confs;
  }

  /**
   * SERIALIZE -> DESERIALIZE scenarios using randomized CASes
   */
  public static List<SerDesTestScenario> randomSerDesScenarios(
          Collection<CasSerDesCycleConfiguration> aSerDesCycles, int aIterations) {

    List<SerDesTestScenario> confs = new ArrayList<>();

    for (CasSerDesCycleConfiguration cycle : aSerDesCycles) {
      for (CasSourceTargetConfiguration data : MultiTypeRandomCasDataSuite
              .configurations(aIterations / 2)) {
        confs.add(new SerDesTestScenario(data, cycle));
      }

      for (CasSourceTargetConfiguration data : MultiFeatureRandomCasDataSuite
              .configurations(aIterations / 2)) {
        confs.add(new SerDesTestScenario(data, cycle));
      }
    }

    return confs;
  }

  public static void ser(CAS aSourceCas, Path aTargetCasFile, SerialFormat aFormat)
          throws Exception {
    try (OutputStream casTarget = Files.newOutputStream(aTargetCasFile)) {
      CasIOUtils.save(aSourceCas, casTarget, aFormat);
    }
  }

  public static void desser(CAS aBufferCas, Path aSourceCasPath, Path aTargetCasPath,
          SerialFormat aFormat, CasLoadMode aMode, CasLoadOptions... aOptions) throws Exception {
    // Deserialize the file into the buffer CAS
    try (InputStream casSource = Files.newInputStream(aSourceCasPath)) {
      if (asList(aOptions).contains(CasLoadOptions.WITH_TSI)) {
        throw new NotImplementedException("Not implemented yet...");
        // try (InputStream tsiSource = Files.newInputStream(aSourceCasPath)) {
        // CasIOUtils.load(casSource, tsiSource, aBufferCas, aMode);
        // }
      } else {
        CasIOUtils.load(casSource, null, aBufferCas, aMode);
      }
    }

    // Serialize the buffer CAS to the target file
    try (OutputStream casTarget = Files.newOutputStream(aTargetCasPath)) {
      CasIOUtils.save(aBufferCas, casTarget, aFormat);
    }
  }

  public static void serdes(CAS aSourceCas, CAS aTargetCas, SerialFormat aFormat, CasLoadMode aMode,
          CasLoadOptions... aOptions) throws Exception {
    // Serialize the CAS
    byte[] casBuffer;
    byte[] tsiBuffer;
    try (ByteArrayOutputStream casTarget = new ByteArrayOutputStream();
            ByteArrayOutputStream tsiTarget = new ByteArrayOutputStream()) {
      CasIOUtils.save(aSourceCas, casTarget, tsiTarget, aFormat);
      casBuffer = casTarget.toByteArray();
      tsiBuffer = tsiTarget.toByteArray();
    }

    // Deserialize the CAS
    try (ByteArrayInputStream casSource = new ByteArrayInputStream(casBuffer);
            ByteArrayInputStream tsiSource = new ByteArrayInputStream(tsiBuffer)) {
      if (asList(aOptions).contains(CasLoadOptions.WITH_TSI)) {
        CasIOUtils.load(casSource, tsiSource, aTargetCas, aMode);
      } else {
        CasIOUtils.load(casSource, null, aTargetCas, aMode);
      }
    }
  }

  public static void writeXmi(CAS aCas, Path aTarget) {
    // Additionally, serialize the data as XMI and also write the type system
    try (OutputStream out = newOutputStream(aTarget)) {
      CasIOUtils.save(aCas, out, XMI_PRETTY);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to create debug XMI from CAS");
      error.initCause(e);
      throw error;
    }
  }

  public static void writeTypeSystemDescription(CAS aCas, Path aTarget) {
    // Additionally, serialize the data as XMI and also write the type system
    try (OutputStream out = newOutputStream(aTarget)) {
      typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
    } catch (Throwable e) {
      AssertionError error = Failures.instance()
              .failure("Unable to create debug typesystem from CAS");
      error.initCause(e);
      throw error;
    }
  }

  enum CasLoadOptions {
    WITH_TSI
  }
}
