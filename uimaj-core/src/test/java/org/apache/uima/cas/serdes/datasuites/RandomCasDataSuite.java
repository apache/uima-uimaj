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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.generators.RandomCasGenerator;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class RandomCasDataSuite {

  public static List<CasSourceTargetConfiguration> configurations(int aCount) {
    List<CasSourceTargetConfiguration> confs = new ArrayList<>();

    for (int n = 0; n < aCount; n++) {
      confs.add(CasSourceTargetConfiguration.builder() //
              .withTitle("randomCas-" + (n + 1)) //
              .withSourceCasSupplier(() -> randomCas(aCount + 1, (aCount + 1) * 10)) //
              .withTargetCasSupplier(CasCreationUtils::createCas) //
              .build());
    }

    return confs;
  }

  public static CAS emptyCas() throws Exception {
    return CasCreationUtils.createCas();
  }

  public static CAS randomCas(int aTypeCount, int aAnnotationCount) throws Exception {
    RandomCasGenerator randomizer = new RandomCasGenerator().typeCount(aTypeCount)
            .minimumAnnotationLength(0).annotationsToGenerate(aAnnotationCount);

    TypeSystemDescription tsd = randomizer.generateRandomTypeSystem();
    return randomizer.generateRandomCas(tsd);
  }
}
