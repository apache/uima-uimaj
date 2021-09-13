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

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Generated;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.generators.CasGenerator;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class PerformanceTestRunner {
  private final FailableBiConsumer<InputStream, CAS, Exception> deserializer;
  private final FailableBiConsumer<CAS, OutputStream, Exception> serializer;
  private final int iterations;
  private final int warmUpIterations;

  private CasGenerator generator;
  private CAS randomizedCas;
  private CAS targetCas;
  private byte[] randomizedCasBytes;

  private PerformanceTestRunner(Builder builder) {
    this.iterations = builder.iterations;
    this.warmUpIterations = builder.warmUpIterations;
    this.generator = builder.generator;
    this.serializer = builder.serializer;
    this.deserializer = builder.deserializer;

    try {
      TypeSystemDescription tsd = generator.generateTypeSystem();
      randomizedCas = generator.generateCas(tsd);
      targetCas = CasCreationUtils.createCas(tsd, null, null, null);
    } catch (ResourceInitializationException e) {
      throw new IllegalStateException(e);
    }

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      serializer.accept(randomizedCas, bos);
      randomizedCasBytes = bos.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void writeData(Path aPath) throws IOException {
    Files.createDirectories(aPath.getParent());
    try (OutputStream os = Files.newOutputStream(aPath)) {
      os.write(randomizedCasBytes);
    }
  }

  public int getDataSize() {
    return randomizedCasBytes.length;
  }

  public long measureSerializationPerformance() throws Exception {
    long total = 0;
    for (int i = 0; i < iterations + warmUpIterations; i++) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        long start = currentTimeMillis();
        serializer.accept(randomizedCas, bos);
        if (i >= warmUpIterations) {
          total += currentTimeMillis() - start;
        }
      }
    }

    return total;
  }

  public long measureDeserializationPerformance() throws Exception {
    long total = 0;
    for (int i = 0; i < iterations + warmUpIterations; i++) {
      try (ByteArrayInputStream bis = new ByteArrayInputStream(randomizedCasBytes)) {
        long start = currentTimeMillis();
        deserializer.accept(bis, targetCas);
        if (i >= warmUpIterations) {
          total += currentTimeMillis() - start;
        }
      }

      targetCas.reset();
    }

    return total;
  }

  /**
   * Creates builder to build {@link PerformanceTestRunner}.
   * 
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link PerformanceTestRunner}.
   */
  public static final class Builder {
    private int iterations = 100;
    private int warmUpIterations = 5;
    private CasGenerator generator;
    private FailableBiConsumer<InputStream, CAS, Exception> deserializer;
    private FailableBiConsumer<CAS, OutputStream, Exception> serializer;

    private Builder() {
    }

    public Builder withSerializer(FailableBiConsumer<CAS, OutputStream, Exception> aSerializer) {
      this.serializer = aSerializer;
      return this;
    }

    public Builder withDeserializer(FailableBiConsumer<InputStream, CAS, Exception> aDeserializer) {
      this.deserializer = aDeserializer;
      return this;
    }

    public Builder withIterations(int iterations) {
      this.iterations = iterations;
      return this;
    }

    public Builder withWarmUpIterations(int iterations) {
      this.warmUpIterations = iterations;
      return this;
    }

    public Builder withGenerator(CasGenerator generator) {
      this.generator = generator;
      return this;
    }

    public PerformanceTestRunner build() {
      return new PerformanceTestRunner(this);
    }
  }

}
