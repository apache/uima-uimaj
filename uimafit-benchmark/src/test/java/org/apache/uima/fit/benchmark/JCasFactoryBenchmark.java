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

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class JCasFactoryBenchmark {
  @Test
  public void benchmarkCreateTypeSystemDescription() throws Exception {
    Benchmark template = new Benchmark("TEMPLATE").repeat(1000);

    new Benchmark("createTypeSystemDescription", template)
            .measure(() -> createTypeSystemDescription()).run();
  }

  @Test
  public void benchmarkCreateJCas() throws Exception {
    Benchmark template = new Benchmark("TEMPLATE").repeat(1000);

    TypeSystemDescription tsd = createTypeSystemDescription();

    new Benchmark("create CAS", template).measure(() -> CasCreationUtils.createCas(tsd, null, null))
            .run();

    new Benchmark("create JCas (fresh TSD)", template).measure(
            () -> CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas())
            .run();

    new Benchmark("create JCas (re-use TSD)", template)
            .measure(() -> CasCreationUtils.createCas(tsd, null, null).getJCas()).run();
  }
}
