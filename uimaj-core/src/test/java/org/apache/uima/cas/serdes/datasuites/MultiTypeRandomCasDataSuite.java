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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.serdes.generators.CasConfiguration;
import org.apache.uima.cas.serdes.generators.MultiTypeRandomCasGenerator;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;

public class MultiTypeRandomCasDataSuite {

  private static final int SIZE_FACTOR = 10;

  public static List<CasSourceTargetConfiguration> configurations(int aCount) {
    List<CasSourceTargetConfiguration> confs = new ArrayList<>();

    for (int n = 0; n < aCount; n++) {
      MultiTypeRandomCasGenerator randomizer = MultiTypeRandomCasGenerator.builder() //
              // NOTE: When you need to debug a certain configuration, comment out and set the
              // random seed for the broken configuration. Do not commit with this line active!
              // .withRandomSeed(-5987713889340419492l) //
              .withTypeCount(n + 1) //
              .withMinimumAnnotationLength(0) //
              .withSize((n + 1) * SIZE_FACTOR) //
              .build();

      CasConfiguration cfg = new CasConfiguration(randomizer);

      confs.add(CasSourceTargetConfiguration.builder() //
              .withTitle("MultiTypeRandomCasDataSuite-" + (n + 1)) //
              .withDebugInfo("Random seed: " + randomizer.getSeed()) //
              .withSourceCasSupplier(cfg::generateRandomCas) //
              .withTargetCasSupplier(cfg::generateTargetCas) //
              .build());
    }

    return confs;
  }
}
