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
import static org.apache.uima.fit.util.JCasUtil.indexCovered;
import static org.apache.uima.fit.util.JCasUtil.indexCovering;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectAll;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovering;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.junit.Before;
import org.junit.Test;

public class JCasUtilBenchmark {
  private JCas jcas;
  
  @Before
  public void setup() throws Exception {
    if (jcas == null) {
      jcas = JCasFactory.createJCas();
    }
    else {
      jcas.reset();
    }
  }

  @Test
  public void benchmarkSelect() {
    Benchmark template = new Benchmark("TEMPLATE")
      .initialize(n -> initRandomCas(jcas.getCas(), n))
      .magnitude(10)
      .magnitudeIncrement(count -> count * 10)
      .incrementTimes(5);
    
    new Benchmark("JCas select Token", template)
      .measure(() -> select(jcas, Token.class))
      .run();

    new Benchmark("JCas select Token v3", template)
      .measure(() -> jcas.select(Token.class))
      .run();

    new Benchmark("JCas select Token and iterate", template)
      .measure(() -> select(jcas, Token.class).forEach(v -> {}))
      .run();

    new Benchmark("JCas select Token and iterate v3", template)
      .measure(() -> jcas.select(Token.class).forEach(v -> {}))
      .run();

    new Benchmark("JCas select Sentence", template)
      .measure(() -> select(jcas, Sentence.class))
      .run();

    new Benchmark("JCas select Sentence and iterate", template)
      .measure(() -> select(jcas, Sentence.class).forEach(v -> {}))
      .run();
    
    new Benchmark("JCas select TOP", template)
      .measure(() -> select(jcas, TOP.class))
      .run();

    new Benchmark("JCas select TOP and iterate", template)
      .measure(() -> select(jcas, TOP.class).forEach(v -> {}))
      .run();
    
    new Benchmark("JCas select TOP and iterate v3", template)
      .measure(() -> jcas.select(TOP.class).forEach(v -> {}))
      .run();

    new Benchmark("JCas select ALL", template)
      .measure(() -> selectAll(jcas))
      .run();
    
    new Benchmark("JCas select ALL and iterate", template)
      .measure(() -> selectAll(jcas).forEach(v -> {}))
      .run();
    new Benchmark("JCas select ALL and iterate v3", template)
      .measure(() -> jcas.select().forEach(v -> {}))
      .run();
  }
  
  @Test
  public void benchmarkSelectCovered() {
    Benchmark template = new Benchmark("TEMPLATE")
      .initialize(n -> initRandomCas(jcas.getCas(), n))
      .magnitude(10)
      .magnitudeIncrement(count -> count * 10)
      .incrementTimes(4);
    
    new Benchmark("JCas selectCovered", template)
      .measure(() -> select(jcas, Sentence.class).forEach(s -> selectCovered(Token.class, s)))
      .run();

    new Benchmark("JCas selectCovered v3", template)
      .measure(() -> {
          jcas.select(Sentence.class).forEach(s -> jcas.select(Token.class).coveredBy(s).forEach(t -> {}));
      })
    .run();

    new Benchmark("JCas indexCovered", template)
      .measure(() -> indexCovered(jcas, Sentence.class, Token.class).forEach((s, l) -> l.forEach(t -> {})))
      .run();
  }
  
  @Test
  public void benchmarkSelectCovering() {
    Benchmark template = new Benchmark("TEMPLATE")
      .initialize(n -> initRandomCas(jcas.getCas(), n))
      .magnitude(10)
      .magnitudeIncrement(count -> count * 10)
      .incrementTimes(3);
    
    new Benchmark("JCas selectCovering", template)
      .measure(() -> select(jcas, Token.class).forEach(t -> selectCovering(Sentence.class, t)))
      .run();

    new Benchmark("JCas selectCovering v3", template)
      .measure(() -> {
          jcas.select(Token.class).forEach(t -> jcas.select(Sentence.class).covering(t).forEach(s -> {}));
      })
    .run();

    new Benchmark("JCas indexCovering", template)
      .measure(() -> indexCovering(jcas, Token.class, Sentence.class).forEach((t, l) -> l.forEach(s -> {})))
      .run();
  }
}
