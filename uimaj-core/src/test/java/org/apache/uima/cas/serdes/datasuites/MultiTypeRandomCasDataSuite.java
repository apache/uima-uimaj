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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.serdes.generators.CasConfiguration;
import org.apache.uima.cas.serdes.generators.MultiTypeRandomCasGenerator;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;

public class MultiTypeRandomCasDataSuite extends AbstractCollection<CasSourceTargetConfiguration>
        implements CasDataSuite {

  private final int iterations;
  private final int sizeFactor;
  private final int minimumAnnotationLength;
  private final Long randomSeed;

  private MultiTypeRandomCasDataSuite(Builder builder) {
    sizeFactor = builder.sizeFactor;
    minimumAnnotationLength = builder.minimumAnnotationLength;
    randomSeed = builder.randomSeed;
    iterations = builder.iterations;
  }

  @Override
  public Iterator<CasSourceTargetConfiguration> iterator() {
    List<CasSourceTargetConfiguration> confs = new ArrayList<>();

    for (int n = 0; n < iterations; n++) {
      MultiTypeRandomCasGenerator.Builder randomizerBuilder = MultiTypeRandomCasGenerator.builder();
      if (randomSeed != null) {
        randomizerBuilder.withRandomSeed(randomSeed);
      }
      MultiTypeRandomCasGenerator randomizer = randomizerBuilder.withTypeCount(n + 1) //
              .withMinimumAnnotationLength(minimumAnnotationLength) //
              .withSize((n + 1) * sizeFactor) //
              .build();

      CasConfiguration cfg = new CasConfiguration(randomizer);

      confs.add(CasSourceTargetConfiguration.builder() //
              .withTitle("MultiTypeRandomCasDataSuite-" + (n + 1)) //
              .withDebugInfo("Random seed: " + randomizer.getSeed()) //
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
   * Creates builder to build {@link MultiTypeRandomCasDataSuite}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link MultiTypeRandomCasDataSuite}.
   */
  public static final class Builder {
    private int iterations = 10;
    private int sizeFactor;
    private int minimumAnnotationLength;
    private Long randomSeed;

    private Builder() {
    }

    public Builder withSizeFactor(int aSizeFactor) {
      sizeFactor = aSizeFactor;
      return this;
    }

    public Builder withMinimumAnnotationLength(int aMinimumAnnotationLength) {
      minimumAnnotationLength = aMinimumAnnotationLength;
      return this;
    }

    public Builder withRandomSeed(long aRandomSeed) {
      randomSeed = aRandomSeed;
      return this;
    }

    public Builder withIterations(int aIterations) {
      iterations = aIterations;
      return this;
    }

    public MultiTypeRandomCasDataSuite build() {
      return new MultiTypeRandomCasDataSuite(this);
    }
  }
}
