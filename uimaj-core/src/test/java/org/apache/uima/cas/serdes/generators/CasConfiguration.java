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
package org.apache.uima.cas.serdes.generators;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class CasConfiguration {
  private final CasGenerator randomizer;

  private TypeSystemDescription tsd;

  public CasConfiguration(CasGenerator aRandomizer) {
    randomizer = aRandomizer;
  }

  private TypeSystemDescription generateTypeSystem() {
    if (tsd == null) {
      tsd = randomizer.generateTypeSystem();
    }

    return tsd;
  }

  public CAS generateRandomCas() throws ResourceInitializationException {
    return randomizer.generateCas(generateTypeSystem());
  }

  public CAS generateTargetCas() throws ResourceInitializationException {
    return CasCreationUtils.createCas(generateTypeSystem(), null, null, null);
  }
}