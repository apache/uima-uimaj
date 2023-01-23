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
import static org.apache.uima.cas.serdes.CasToComparableText.toComparableString;
import static org.apache.uima.cas.serdes.TestType.ONE_WAY;
import static org.apache.uima.cas.serdes.TestType.ROUND_TRIP;
import static org.apache.uima.cas.serdes.TestType.SER_DES;
import static org.apache.uima.cas.serdes.TestType.SER_REF;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.serdes.datasuites.CasDataSuite;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.serdes.datasuites.XmiFileDataSuite;
import org.apache.uima.cas.serdes.scenario.DesSerTestScenario;
import org.apache.uima.cas.serdes.scenario.SerDesTestScenario;
import org.apache.uima.cas.serdes.scenario.SerRefTestScenario;
import org.apache.uima.cas.serdes.transitions.CasDesSerCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.assertj.core.internal.Failures;

public class SerDesCasIOTestUtils {
  public static Class<?> getCallerClass() {
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
    return ProgrammaticallyCreatedCasDataSuite.builder().build().stream()
            .map(conf -> SerRefTestScenario.builder(caller, conf, SER_REF, aCasFileName)
                    .withSerializer((cas, path) -> ser(cas, path, aFormat)) //
                    .withAssertion(SerRefTestScenario::assertCasContentsAreEqual).build())
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
  public static List<DesSerTestScenario> roundTripDesSerScenariosComparingFileContents(
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
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  public static List<DesSerTestScenario> roundTripDesSerScenariosComparingFileContentsNormalizingNewlines(
          Collection<CasDesSerCycleConfiguration> aDesSerCycles, String aCasFileName)
          throws Exception {
    Class<?> caller = getCallerClass();

    List<DesSerTestScenario> confs = new ArrayList<>();

    for (CasDesSerCycleConfiguration cycle : aDesSerCycles) {
      try (Stream<DesSerTestScenario.Builder> builders = DesSerTestScenario.builderCases(caller,
              cycle, ROUND_TRIP, aCasFileName)) {

        builders.map(builder -> builder.withCycle(cycle::performCycle)
                .withAssertion(DesSerTestScenario::assertFileContentsAreEqualNormalizingNewlines)
                .build()) //
                .forEach(confs::add);
      }
    }

    return confs;
  }

  /**
   * DESERIALIZE -> SERIALIZE scenarios using the reference data from the
   * serialize/compare-to-reference data.
   */
  public static List<DesSerTestScenario> roundTripDesSerScenariosComparingCasContents(
          Collection<CasDesSerCycleConfiguration> aDesSerCycles, String aCasFileName)
          throws Exception {
    Class<?> caller = getCallerClass();

    List<DesSerTestScenario> confs = new ArrayList<>();

    for (CasDesSerCycleConfiguration cycle : aDesSerCycles) {
      try (Stream<DesSerTestScenario.Builder> builders = DesSerTestScenario.builderCases(caller,
              cycle, ROUND_TRIP, aCasFileName)) {

        builders.map(builder -> builder //
                .withCycle(cycle::performCycle) //
                .withAssertion((targetCasFile, referenceCasFile) -> {
                  CAS targetCas = createCasMaybeWithTypesystem(referenceCasFile);
                  CAS referenceCas = createCasMaybeWithTypesystem(referenceCasFile);
                  des(targetCas, targetCasFile, CasLoadMode.DEFAULT);
                  des(referenceCas, referenceCasFile, CasLoadMode.DEFAULT);
                  assertThat(toComparableString(targetCas))
                          .isEqualTo(toComparableString(referenceCas));

                }).build()) //
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
  public static List<SerDesTestScenario> programmaticSerDesScenarios(
          Collection<CasSerDesCycleConfiguration> aSerDesCycles) {

    return serDesScenarios(aSerDesCycles, ProgrammaticallyCreatedCasDataSuite.builder().build());
  }

  /**
   * SERIALIZE -> DESERIALIZE scenarios using the given data suites (typically randomized suites)
   */
  public static List<SerDesTestScenario> serDesScenarios(
          Collection<CasSerDesCycleConfiguration> aSerDesCycles, CasDataSuite... aDataSuites) {

    List<SerDesTestScenario> confs = new ArrayList<>();

    for (CasSerDesCycleConfiguration cycle : aSerDesCycles) {
      for (CasDataSuite suite : aDataSuites) {
        suite.forEach(data -> confs.add(new SerDesTestScenario(data, cycle)));
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

  public static void des(CAS aBufferCas, Path aSourceCasPath, CasLoadMode aMode,
          CasLoadOptions... aOptions) throws IOException {
    // Deserialize the file into the buffer CAS
    try (InputStream casSource = Files.newInputStream(aSourceCasPath)) {
      if (asList(aOptions).contains(CasLoadOptions.PRESERVE_ORIGINAL_TSI)) {
        throw new NotImplementedException("Not implemented yet...");
        // try (InputStream tsiSource = Files.newInputStream(aSourceCasPath)) {
        // CasIOUtils.load(casSource, tsiSource, aBufferCas, aMode);
        // }
      } else {
        CasIOUtils.load(casSource, null, aBufferCas, aMode);
      }
    }
  }

  public static void desser(CAS aBufferCas, Path aSourceCasPath, Path aTargetCasPath,
          SerialFormat aFormat, CasLoadMode aMode, CasLoadOptions... aOptions) throws Exception {
    des(aBufferCas, aSourceCasPath, aMode, aOptions);

    ser(aBufferCas, aTargetCasPath, aFormat);
  }

  public static void serdes(CAS aSourceCas, CAS aTargetCas, SerialFormat aFormat, CasLoadMode aMode,
          CasLoadOptions... aOptions) throws Exception {
    // Serialize the CAS
    byte[] casBuffer;
    byte[] tsiBuffer;
    try (ByteArrayOutputStream casTarget = new ByteArrayOutputStream();
            ByteArrayOutputStream tsiTarget = new ByteArrayOutputStream()) {
      CasIOUtils.save(aSourceCas, casTarget, null, aFormat);
      // CasIOUtils.save only saves TSI data to the TSI stream if it is not already included in the
      // CAS stream (type system embedded). Thus, to ensure we always get the TSI info, we serialize
      // it separately.
      CasIOUtils.writeTypeSystem(aSourceCas, tsiTarget, true);
      casBuffer = casTarget.toByteArray();
      tsiBuffer = tsiTarget.toByteArray();
    }

    Path targetFile = SER_DES.getTargetFolder(getCallerClass())
            .resolve("data." + aFormat.getDefaultFileExtension());
    Files.createDirectories(targetFile.getParent());
    try (OutputStream os = Files.newOutputStream(targetFile)) {
      os.write(casBuffer);
    }

    // Deserialize the CAS
    try (ByteArrayInputStream casSource = new ByteArrayInputStream(casBuffer);
            ByteArrayInputStream tsiSource = new ByteArrayInputStream(tsiBuffer)) {
      if (asList(aOptions).contains(CasLoadOptions.PRESERVE_ORIGINAL_TSI)) {
        ((CASImpl) aTargetCas).getBinaryCasSerDes()
                .setupCasFromCasMgrSerializer(readCasManager(tsiSource));
      }
      CasIOUtils.load(casSource, null, aTargetCas, aMode);
    }
  }

  private static CASMgrSerializer readCasManager(InputStream tsiInputStream) throws IOException {
    try {
      if (null == tsiInputStream) {
        return null;
      }
      ObjectInputStream is = new ObjectInputStream(tsiInputStream);
      return (CASMgrSerializer) is.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
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

  public enum CasLoadOptions {
    /**
     * Preserves the type information of the original file and uses it when populating the target
     * CAS during deserialization. This is particularly useful when testing formats which do not
     * include type system information to ensure that no information is lost during deserialization.
     */
    PRESERVE_ORIGINAL_TSI
  }

  public static CAS createCasMaybeWithTypesystem(Path aContextFile)
          throws ResourceInitializationException, InvalidXMLException, IOException {
    Path typeSystemFile = aContextFile.resolveSibling("typesystem.xml");

    if (!Files.exists(typeSystemFile)) {
      return createCas();
    }

    TypeSystemDescription tsd = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile.toFile()));
    return CasCreationUtils.createCas(tsd, null, null, null);
  }
}
