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
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.indexCovering;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAll;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.CasUtil.selectCovering;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Before;
import org.junit.Test;

public class CasUtilBenchmark {
  private CAS cas;
  
  private static final String TYPE_NAME_TOKEN = "org.apache.uima.fit.type.Token";
  private static final String TYPE_NAME_SENTENCE = "org.apache.uima.fit.type.Sentence";
  
  @Before
  public void setup() throws Exception {
    if (cas == null) {
      cas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null);
    }
    else {
      cas.reset();
    }
  }

  @Test
  public void benchmarkSelect() {
    Benchmark template = new Benchmark("TEMPLATE")
      .initialize(n -> initRandomCas(cas, n))
      .magnitude(10)
      .magnitudeIncrement(count -> count * 10)
      .incrementTimes(5);
    
    new Benchmark("CAS select Token", template)
      .measure(() -> select(cas, getType(cas, TYPE_NAME_TOKEN)))
      .run();

    new Benchmark("CAS select Token v3", template)
      .measure(() -> cas.select(getType(cas, TYPE_NAME_TOKEN)))
      .run();

    new Benchmark("CAS select Token and iterate", template)
      .measure(() -> select(cas, getType(cas, TYPE_NAME_TOKEN)).forEach(v -> {}))
      .run();

    new Benchmark("CAS select Token and iterate v3", template)
      .measure(() -> cas.select(getType(cas, TYPE_NAME_TOKEN)).forEach(v -> {}))
      .run();

    new Benchmark("CAS select Sentence", template)
      .measure(() -> select(cas, getType(cas, TYPE_NAME_SENTENCE)))
      .run();

    new Benchmark("CAS select Sentence and iterate", template)
      .measure(() -> select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(v -> {}))
      .run();
    
    new Benchmark("CAS select TOP", template)
      .measure(() -> selectFS(cas, getType(cas, CAS.TYPE_NAME_TOP)))
      .run();

    new Benchmark("CAS select TOP and iterate", template)
      .measure(() -> selectFS(cas, getType(cas, CAS.TYPE_NAME_TOP)).forEach(v -> {}))
      .run();

    new Benchmark("CAS select TOP and iterate v3", template)
      .measure(() -> cas.select(getType(cas, CAS.TYPE_NAME_TOP)).forEach(v -> {}))
      .run();

    new Benchmark("CAS select ALL", template)
      .measure(() -> selectAll(cas))
      .run();
    
    new Benchmark("CAS select ALL and iterate", template)
      .measure(() -> selectAll(cas).forEach(v -> {}))
      .run();

    new Benchmark("CAS select ALL and iterate v3", template)
      .measure(() -> cas.select().forEach(v -> {}))
      .run();
  }
  
  @Test
  public void benchmarkSelectCovered() {
    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(n -> initRandomCas(cas, n))
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(4);
    
    new Benchmark("CAS selectCovered", template)
      .measure(() -> {
        Type sentenceType = getType(cas, TYPE_NAME_SENTENCE);
        Type tokenType = getType(cas, TYPE_NAME_TOKEN);
        select(cas, sentenceType).forEach(s -> selectCovered(tokenType, s).forEach(t -> {}));
      })
      .run();

    new Benchmark("CAS selectCovered v3", template)
      .measure(() -> {
        Type sentenceType = getType(cas, TYPE_NAME_SENTENCE);
        Type tokenType = getType(cas, TYPE_NAME_TOKEN);
        cas.select(sentenceType).forEach(s -> cas.select(tokenType).coveredBy((AnnotationFS) s).forEach(t -> {}));
      })
      .run();

    new Benchmark("CAS indexCovered", template)
      .measure(() -> indexCovered(cas, getType(cas, TYPE_NAME_SENTENCE), getType(cas, TYPE_NAME_TOKEN))
          .forEach((s, l) -> l.forEach(t -> {})))
      .run();
  }
  
  @Test
  public void benchmarkSelectCovering() {
    Benchmark template = new Benchmark("TEMPLATE")
      .initialize(n -> initRandomCas(cas, n))
      .magnitude(10)
      .magnitudeIncrement(count -> count * 10)
      .incrementTimes(3);
    
    new Benchmark("CAS selectCovering", template)
      .measure(() -> {
        Type sentenceType = getType(cas, TYPE_NAME_SENTENCE);
        Type tokenType = getType(cas, TYPE_NAME_TOKEN);
        select(cas, tokenType).forEach(t -> selectCovering(sentenceType, t));
      })
      .run();

    new Benchmark("CAS selectCovering", template)
      .measure(() -> {
        Type sentenceType = getType(cas, TYPE_NAME_SENTENCE);
        Type tokenType = getType(cas, TYPE_NAME_TOKEN);
        select(cas, tokenType).forEach(s -> selectCovering(sentenceType, s));
      })
      .run();

    new Benchmark("CAS selectCovering v3", template)
      .measure(() -> {
        Type sentenceType = getType(cas, TYPE_NAME_SENTENCE);
        Type tokenType = getType(cas, TYPE_NAME_TOKEN);
        cas.select(tokenType).forEach(t -> cas.select(sentenceType).covering((AnnotationFS) t).forEach(s -> {}));
      })
      .run();

    new Benchmark("CAS indexCovering", template)
      .measure(() -> indexCovering(cas, getType(cas, TYPE_NAME_TOKEN), getType(cas, TYPE_NAME_SENTENCE))
          .forEach((t, l) -> l.forEach(s -> {})))
      .run();
  }
}
