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
package org.apache.uima.cas.serdes.datasuites;

import static org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator.StringArrayMode.ALLOW_NULL_AND_EMPTY_STRINGS;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.serdes.generators.CasConfiguration;
import org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator;
import org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator.StringArrayMode;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;

public class MultiFeatureRandomCasDataSuite extends AbstractCollection<CasSourceTargetConfiguration>
        implements CasDataSuite {
  private final int sizeFactor;
  private final StringArrayMode stringArrayMode;
  private final boolean emptyArrays;
  private final int iterations;

  private MultiFeatureRandomCasDataSuite(Builder builder) {
    sizeFactor = builder.sizeFactor;
    stringArrayMode = builder.stringArrayMode;
    iterations = builder.iterations;
    emptyArrays = builder.emtpyArrays;
  }

  @Override
  public Iterator<CasSourceTargetConfiguration> iterator() {
    List<CasSourceTargetConfiguration> confs = new ArrayList<>();

    for (int n = 0; n < iterations; n++) {
      MultiFeatureRandomCasGenerator randomizer = MultiFeatureRandomCasGenerator.builder() //
              .withStringArrayMode(stringArrayMode) //
              .withSize((n + 1) * sizeFactor) //
              .withEmptyArrays(emptyArrays) //
              .build();

      CasConfiguration cfg = new CasConfiguration(randomizer);

      confs.add(CasSourceTargetConfiguration.builder() //
              .withTitle("MultiFeatureRandomCasDataSuite-" + (n + 1)) //
              .withSourceCasSupplier(cfg::generateRandomCas) //
              .withTargetCasSupplier(cfg::generateTargetCas) //
              .build());
    }

    return confs.iterator();
  }

  @Override
  public int size() {
    return iterations;
  }

  /**
   * Creates builder to build {@link MultiFeatureRandomCasDataSuite}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link MultiFeatureRandomCasDataSuite}.
   */
  public static final class Builder {
    private int iterations = 10;
    private int sizeFactor = 10;
    private StringArrayMode stringArrayMode = ALLOW_NULL_AND_EMPTY_STRINGS;
    private boolean emtpyArrays = true;

    private Builder() {
    }

    public Builder withSizeFactory(int aSizeFactory) {
      sizeFactor = aSizeFactory;
      return this;
    }

    public Builder withStringArrayMode(StringArrayMode aStringArrayMode) {
      stringArrayMode = aStringArrayMode;
      return this;
    }

    public Builder withIterations(int aIterations) {
      iterations = aIterations;
      return this;
    }

    public Builder withEmptyArrays(boolean aFlag) {
      emtpyArrays = aFlag;
      return this;
    }

    public MultiFeatureRandomCasDataSuite build() {
      return new MultiFeatureRandomCasDataSuite(this);
    }
  }
}
