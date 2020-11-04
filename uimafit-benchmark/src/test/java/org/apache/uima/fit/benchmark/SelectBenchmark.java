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

import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.covering;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectOverlapping;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.TOP;
import org.junit.Test;

public class SelectBenchmark {
  private static final String TYPE_NAME_TOKEN = "org.apache.uima.fit.type.Token";
  private static final String TYPE_NAME_SENTENCE = "org.apache.uima.fit.type.Sentence";
  
  @Test
  public void benchmarkSelect() {
    CachingRandomJCasProvider casProvider = new CachingRandomJCasProvider();

    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(casProvider::prepare)
        .timer(Benchmark::user)
        .timerUnit("ns")
        .repeat(1_000)
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(5);
    
    new BenchmarkGroup("select")
        .add(new Benchmark("WARM-UP", template)
            .measure(() -> casProvider.get().select().forEach(t -> {})))
        .add(new Benchmark("JCasUtil.selectAll(JCAS).forEach(x -> {})", template)
            .measure(() -> JCasUtil.selectAll(casProvider.get()).forEach(x -> {})))
        .add(new Benchmark("JCAS.select().forEach(x -> {})", template)
            .measure(() -> casProvider.get().select().forEach(x -> {})))
        .add(new Benchmark("JCasUtil.select(JCAS, TOP.class).forEach(x -> {})", template)
            .measure(() -> JCasUtil.select(casProvider.get(), TOP.class).forEach(x -> {})))
        .add(new Benchmark("JCAS.select(TOP.class).forEach(x -> {})", template)
            .measure(() -> casProvider.get().select(TOP.class).forEach(x -> {})))
        .add(new Benchmark("JCasUtil.select(JCAS, Token.class).forEach(x -> {})", template)
            .measure(() -> JCasUtil.select(casProvider.get(), Token.class).forEach(x -> {})))
        .add(new Benchmark("JCAS.select(Token.class).forEach(x -> {})", template)
            .measure(() -> casProvider.get().select(Token.class).forEach(x -> {})))
        .runAll();
  }

  @Test
  public void benchmarkSelectOverlapping() {
    CachingRandomJCasProvider casProvider = new CachingRandomJCasProvider();
    
    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(casProvider::prepare)
        .timer(Benchmark::user)
        .timerUnit("ns")
        .repeat(25)
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(3);
    
    new BenchmarkGroup("select overlapping")
        .add(new Benchmark("WARM-UP", template)
            .measure(() -> casProvider.get().select().forEach(t -> {})))
        .add(new Benchmark("selectOverlapping(JCAS, Token.class, s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    selectOverlapping(Token.class, s).forEach(t -> {}));
            }))
        .add(new Benchmark("CAS.select(Token.class).filter(t -> overlapping(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class)
                        .filter(t -> overlapping(t, s))
                        .forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).stream().filter(t -> overlapping(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).stream()
                        .filter(t -> overlapping(t, s))
                        .forEach(t -> {}));
            }))
        .runAll();
  }
  
  @Test
  public void benchmarkSelectCoveredBy() {
    CachingRandomJCasProvider casProvider = new CachingRandomJCasProvider();
    
    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(casProvider::prepare)
        .timer(Benchmark::user)
        .timerUnit("ns")
        .repeat(25)
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(3);
    
    new BenchmarkGroup("select covered by")
        .add(new Benchmark("WARM-UP", template)
            .measure(() -> casProvider.get().select().forEach(t -> {})))
        .add(new Benchmark("selectCovered(Token.class, s).forEach(t -> {})", template)
            .measure(() -> {
              select(casProvider.get(), Sentence.class).forEach(s -> 
                    selectCovered(Token.class, s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).coveredBy(s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class).coveredBy(s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).select().coveredBy(s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).select().coveredBy(s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).filter(t -> coveredBy(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class)
                        .filter(t -> coveredBy(t, s))
                        .forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).stream().filter(t -> coveredBy(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).stream()
                        .filter(t -> coveredBy(t, s))
                        .forEach(t -> {}));
            }))
        .runAll();
  }
  
  @Test
  public void benchmarkSelectCovering() {
    CachingRandomJCasProvider casProvider = new CachingRandomJCasProvider();
    
    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(casProvider::prepare)
        .timer(Benchmark::user)
        .timerUnit("ns")
        .repeat(25)
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(3);
    
    new BenchmarkGroup("select covering")
        .add(new Benchmark("WARM-UP", template)
            .measure(() -> casProvider.get().select().forEach(t -> {})))
        .add(new Benchmark("JCasUtil.selectCovering(Token.class, s).forEach(t -> {})", template)
            .measure(() -> {
              select(casProvider.get(), Sentence.class).forEach(s -> 
                    JCasUtil.selectCovering(Token.class, s).forEach(t -> {}));
            }))
        .add(new Benchmark("CasUtil.selectCovering(tokenType, s).forEach(t -> {})", template)
            .measure(() -> {
                CAS cas = casProvider.get().getCas();
                select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> 
                    CasUtil.selectCovering(getType(cas, TYPE_NAME_TOKEN), s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).covering(s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class).covering(s).forEach(t -> {}));
            }))
        .add(new Benchmark("CAS.getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).select().covering(s).forEach(t -> {})", template)
            .measure(() -> {
                CAS cas = casProvider.get().getCas();
                select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> 
                    casProvider.get().getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).select().covering(s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).select().covering(s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).select().covering(s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).filter(t -> covering(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class)
                        .filter(t -> covering(t, s))
                        .forEach(t -> {}));
            }))
        .add(new Benchmark("CAS.getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).stream().filter(t -> covering(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                CAS cas = casProvider.get().getCas();
                select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> 
                    casProvider.get().getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).stream()
                        .filter(t -> covering(t, s))
                        .forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).stream().filter(t -> covering(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).stream()
                        .filter(t -> covering(t, s))
                        .forEach(t -> {}));
            }))
        .runAll();  
  }
  
  @Test
  public void benchmarkSelectAt() {
    CachingRandomJCasProvider casProvider = new CachingRandomJCasProvider();
    
    Benchmark template = new Benchmark("TEMPLATE")
        .initialize(casProvider::prepare)
        .timer(Benchmark::user)
        .timerUnit("ns")
        .repeat(25)
        .magnitude(10)
        .magnitudeIncrement(count -> count * 10)
        .incrementTimes(3);
    
    new BenchmarkGroup("select at")
        .add(new Benchmark("WARM-UP", template)
            .measure(() -> casProvider.get().select().forEach(t -> {})))
        .add(new Benchmark("JCasUtil.selectAt(CAS, Token.class, s.getBegin(), s.getEnd()).forEach(t -> {})", template)
            .measure(() -> {
              select(casProvider.get(), Sentence.class).forEach(s -> 
                    JCasUtil.selectAt(casProvider.get(), Token.class, s.getBegin(), s.getEnd()).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).at(s.getBegin(), s.getEnd()).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class).at(s.getBegin(), s.getEnd()).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).at(s).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class).at(s).forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.select(Token.class).filter(t -> colocated(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().select(Token.class)
                        .filter(t -> colocated(t, s))
                        .forEach(t -> {}));
            }))
        .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).stream().filter(t -> colocated(t, s)).forEach(t -> {})", template)
            .measure(() -> {
                select(casProvider.get(), Sentence.class).forEach(s -> 
                    casProvider.get().getAnnotationIndex(Token.class).stream()
                        .filter(t -> colocated(t, s))
                        .forEach(t -> {}));
            }))
        .runAll();  
  }
}
