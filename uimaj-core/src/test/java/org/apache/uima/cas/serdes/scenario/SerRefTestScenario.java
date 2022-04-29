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
package org.apache.uima.cas.serdes.scenario;

import static org.apache.uima.cas.serdes.SerDesCasIOTestUtils.writeTypeSystemDescription;
import static org.apache.uima.cas.serdes.SerDesCasIOTestUtils.writeXmi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.TestType;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.assertj.core.internal.Failures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerRefTestScenario implements Runnable {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String title;
  private final FailableSupplier<CAS, ?> sourceCasSupplier;
  private final Path referenceCasFile;
  private final Path targetCasFile;
  private final FailableBiConsumer<CAS, Path, ?> serializer;

  private SerRefTestScenario(Builder builder) {
    this.title = builder.title;
    this.sourceCasSupplier = builder.sourceCasSupplier;
    this.referenceCasFile = builder.referenceCasFile;
    this.targetCasFile = builder.targetCasFile;
    this.serializer = builder.serializer;
  }

  public SerRefTestScenario(Path aReferenceBasePath, Path aTargetBasePath,
          CasSourceTargetConfiguration aSourceTargetConfiguration, String aTargetFileName,
          FailableBiConsumer<CAS, Path, ?> aSerializer) {
    title = aSourceTargetConfiguration.getTitle();
    sourceCasSupplier = aSourceTargetConfiguration::createSourceCas;
    referenceCasFile = aReferenceBasePath.resolve(title).resolve(aTargetFileName);
    targetCasFile = aTargetBasePath.resolve(title).resolve(aTargetFileName);
    serializer = aSerializer;
  }

  public CAS createSourceCas() {
    try {
      return sourceCasSupplier.get();
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to create source CAS");
      error.initCause(e);
      throw error;
    }
  }

  public Path getReferenceCasFile() {
    return referenceCasFile;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  @Override
  public void run() {
    // Fetch the source and target CASes
    CAS sourceCas = createSourceCas();

    // Serialize CAS to target file
    log.info("Serializing source CAS to {}", targetCasFile);
    serialize(sourceCas, targetCasFile);

    // Additionally, serialize the data as XMI and also write the type system
    writeXmi(sourceCas, targetCasFile.resolveSibling("debug.xmi"));
    writeTypeSystemDescription(sourceCas, targetCasFile.resolveSibling("debug-typesystem.xml"));

    // Compare the serialized CAS file against the reference
    assumeThat(referenceCasFile.toFile()) //
            .as("Reference file must exists at %s", referenceCasFile) //
            .exists();
    assertThat(contentOf(targetCasFile.toFile())) //
            .isEqualToNormalizingNewlines(contentOf(referenceCasFile.toFile()));
  }

  private void serialize(CAS aSourceCas, Path aTargetCasFile) {
    try {
      aTargetCasFile.getParent().toFile().mkdirs();
      serializer.accept(aSourceCas, aTargetCasFile);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to serialize CAS");
      error.initCause(e);
      throw error;
    }
  }

  public static Path getDataBasePath(Class<?> aTestClass) {
    return Paths.get("src", "test", "resources", aTestClass.getSimpleName(), "ser-ref");
  }

  /**
   * Creates builder to build {@link SerRefTestScenario}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(Class<?> aTestClass, CasSourceTargetConfiguration aConf,
          TestType aTestType, String aCasFileName) {

    Builder builder = builder() //
            .withTitle(aConf.getTitle()) //
            .withSourceCasSupplier(aConf::createSourceCas) //
            .withReferenceCasFile(aTestType.getReferenceFolder(aTestClass).resolve(aConf.getTitle())
                    .resolve(aCasFileName))
            .withTargetCasFile(aTestType.getTargetFolder(aTestClass).resolve(aConf.getTitle())
                    .resolve(aCasFileName));

    return builder;
  }

  /**
   * Builder to build {@link SerRefTestScenario}.
   */
  public static final class Builder {
    private String title;
    private FailableSupplier<CAS, ?> sourceCasSupplier;
    private Path referenceCasFile;
    private Path targetCasFile;
    private FailableBiConsumer<CAS, Path, ?> serializer;

    private Builder() {
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withSourceCasSupplier(FailableSupplier<CAS, ?> sourceCasSupplier) {
      this.sourceCasSupplier = sourceCasSupplier;
      return this;
    }

    public Builder withReferenceCasFile(Path referenceCasFile) {
      this.referenceCasFile = referenceCasFile;
      return this;
    }

    public Builder withTargetCasFile(Path targetCasFile) {
      this.targetCasFile = targetCasFile;
      return this;
    }

    public Builder withSerializer(FailableBiConsumer<CAS, Path, ?> serializer) {
      this.serializer = serializer;
      return this;
    }

    public SerRefTestScenario build() {
      return new SerRefTestScenario(this);
    }
  }
}
