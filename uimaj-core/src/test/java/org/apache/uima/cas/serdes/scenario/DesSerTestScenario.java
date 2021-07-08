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

import static java.nio.file.Files.isDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.Generated;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.uima.cas.serdes.TestType;
import org.apache.uima.cas.serdes.transitions.CasDesSerCycleConfiguration;
import org.assertj.core.internal.Failures;

public class DesSerTestScenario implements Runnable {
  private final String title;
  private final Path sourceCasFile;
  private final Path referenceCasFile;
  private final Path targetBasePath;
  private final FailableBiConsumer<Path, Path, ?> cycle;

  @Generated("SparkTools")
  private DesSerTestScenario(Builder builder) {
    this.title = builder.title;
    this.sourceCasFile = builder.sourceCasFile;
    this.referenceCasFile = builder.referenceCasFile;
    this.targetBasePath = builder.targetBasePath;
    this.cycle = builder.cycle;
  }

  public Path getSourceCasFile() {
    return sourceCasFile;
  }

  public Path getReferenceCasFile() {
    return referenceCasFile;
  }

  @Override
  public void run() {
    Path targetCasFolder = targetBasePath.resolve(getTitle());
    targetCasFolder.toFile().mkdirs();
    Path targetCasFile = targetCasFolder.resolve(referenceCasFile.getFileName().toString());

    // Perform actual test cycle
    deserializationSerializationCycle(getSourceCasFile(), targetCasFile);

    // Compare the serialized CAS file against the reference
    assertThat(contentOf(targetCasFile.toFile())) //
            .isEqualTo(contentOf(referenceCasFile.toFile()));
  }

  public void deserializationSerializationCycle(Path aSourceCas, Path aTargetCas) {
    try {
      cycle.accept(aSourceCas, aTargetCas);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to (de)serialize CAS");
      error.initCause(e);
      throw error;
    }
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }

  /**
   * Creates builder to build {@link DesSerTestScenario}.
   * 
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builder() {
    return new Builder();
  }

  public static Stream<Builder> builderCases(Class<?> aTestClass,
          CasDesSerCycleConfiguration aCycle, TestType aTestType, String aCasFileName)
          throws IOException {
    return Files.list(SerRefTestScenario.getDataBasePath(aTestClass)) //
            .filter(p -> isDirectory(p) && !p.toFile().isHidden()) //
            .map(caseFolder -> builder() //
                    .withTitle(aCycle.getTitle() + " - " + caseFolder.getFileName().toString()) //
                    .withTargetBasePath(Paths.get("target", "test-output",
                            aTestClass.getSimpleName(), aTestType.getTargetFolderName())) //
                    .withCasFile(caseFolder.resolve(aCasFileName)));
  }

  /**
   * Builder to build {@link DesSerTestScenario}.
   */
  @Generated("SparkTools")
  public static final class Builder {
    private String title;
    private Path sourceCasFile;
    private Path referenceCasFile;
    private Path targetBasePath;
    private FailableBiConsumer<Path, Path, ?> cycle;

    private Builder() {
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withCasFile(Path sourceCasFile) {
      this.sourceCasFile = sourceCasFile;
      this.referenceCasFile = sourceCasFile;
      return this;
    }

    public Builder withSourceCasFile(Path sourceCasFile) {
      this.sourceCasFile = sourceCasFile;
      return this;
    }

    public Builder withReferenceCasFile(Path referenceCasFile) {
      this.referenceCasFile = referenceCasFile;
      return this;
    }

    public Builder withTargetBasePath(Path targetBasePath) {
      this.targetBasePath = targetBasePath;
      return this;
    }

    public Builder withCycle(FailableBiConsumer<Path, Path, ?> cycle) {
      this.cycle = cycle;
      return this;
    }

    public DesSerTestScenario build() {
      return new DesSerTestScenario(this);
    }
  }
}
