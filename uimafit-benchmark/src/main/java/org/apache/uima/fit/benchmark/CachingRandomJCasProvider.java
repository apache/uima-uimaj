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
package org.apache.uima.fit.benchmark;

import static org.apache.uima.fit.benchmark.CasInitializationUtils.initRandomCas;

import java.util.HashMap;
import java.util.Map;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

public class CachingRandomJCasProvider {
  private static final long RANDOM_SEED = 12345l;

  private final Map<Integer, JCas> cache = new HashMap<>();

  private JCas preparedJCas;

  public void prepare(int size) {
    JCas cachedJCas = cache.get(size);

    if (cachedJCas == null) {
      try {
        cachedJCas = JCasFactory.createJCas();
      } catch (ResourceInitializationException | CASException e) {
        throw new RuntimeException(e);
      }

      initRandomCas(cachedJCas.getCas(), 10, size, 30, 1000, RANDOM_SEED);
      cache.put(size, cachedJCas);
    }

    preparedJCas = cachedJCas;
  }

  public JCas get() {
    return preparedJCas;
  }
}
