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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SelectBenchmark {
  private static final String TYPE_NAME_TOKEN = "org.apache.uima.fit.type.Token";
  private static final String TYPE_NAME_SENTENCE = "org.apache.uima.fit.type.Sentence";

  private Benchmark templateForFastOperations;
  private Benchmark templateForNormalOperations;
  private Benchmark warmupTask;
  private CachingRandomJCasProvider casProvider;

  @BeforeEach
  public void setup() {
    casProvider = new CachingRandomJCasProvider();
    warmupTask = new Benchmark("WARM-UP").initialize(casProvider::prepare).repeat(100).magnitude(10)
            .magnitudeIncrement(count -> count * 10).incrementTimes(5).ignore(true)
            .measure(() -> casProvider.get().select().forEach(t -> {
            }));
    templateForFastOperations = new Benchmark("FAST TEMPLATE").initialize(casProvider::prepare)
            .timer(Benchmark::userTime, t -> t / 1_000_000).repeat(1_000).magnitude(10)
            .magnitudeIncrement(count -> count * 10).incrementTimes(5);
    templateForNormalOperations = new Benchmark("NORMAL TEMPLATE").initialize(casProvider::prepare)
            .timer(Benchmark::userTime, t -> t / 1_000_000).repeat(10_000).magnitude(10)
            .magnitudeIncrement(count -> count * 10).incrementTimes(4);
  }

  @Test
  public void benchmarkSelect() {
    new BenchmarkGroup("select", templateForFastOperations).addIgnoringTemplate(warmupTask)
            .add(new Benchmark("JCasUtil.selectAll(JCAS).forEach(x -> {})")
                    .measure(() -> JCasUtil.selectAll(casProvider.get()).forEach(x -> {
                    })))
            .add(new Benchmark("JCAS.select().forEach(x -> {})")
                    .measure(() -> casProvider.get().select().forEach(x -> {
                    })))
            .add(new Benchmark("JCasUtil.select(JCAS, TOP.class).forEach(x -> {})")
                    .measure(() -> JCasUtil.select(casProvider.get(), TOP.class).forEach(x -> {
                    })))
            .add(new Benchmark("JCAS.select(TOP.class).forEach(x -> {})")
                    .measure(() -> casProvider.get().select(TOP.class).forEach(x -> {
                    })))
            .add(new Benchmark("JCasUtil.select(JCAS, Token.class).forEach(x -> {})")
                    .measure(() -> JCasUtil.select(casProvider.get(), Token.class).forEach(x -> {
                    })))
            .add(new Benchmark("JCAS.select(Token.class).forEach(x -> {})")
                    .measure(() -> casProvider.get().select(Token.class).forEach(x -> {
                    })))
            .add(new Benchmark("JCAS.getAnnotationIndex(Token.class).select().forEach(x -> {})")
                    .measure(() -> casProvider.get().getAnnotationIndex(Token.class).select()
                            .forEach(x -> {
                            })))
            .runAll();
  }

  @Test
  public void benchmarkSelectOverlapping() {
    new BenchmarkGroup("select overlapping", templateForNormalOperations)
            .addIgnoringTemplate(warmupTask)
            .add(new Benchmark("selectOverlapping(JCAS, Token.class, s).forEach(t -> {})")
                    .measure(() -> {
                      select(casProvider.get(), Sentence.class)
                              .forEach(s -> selectOverlapping(Token.class, s).forEach(t -> {
                              }));
                    }))
            .add(new Benchmark(
                    "CAS.select(Token.class).filter(t -> overlapping(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .filter(t -> overlapping(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).stream().filter(t -> overlapping(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(s -> casProvider
                                      .get().getAnnotationIndex(Token.class).stream()
                                      .filter(t -> overlapping(t, s)).forEach(t -> {
                                      }));
                            }))
            .runAll();
  }

  @Test
  public void benchmarkSelectCoveredBy() {
    new BenchmarkGroup("select covered by", templateForNormalOperations)
            .addIgnoringTemplate(warmupTask)
            .add(new Benchmark("selectCovered(Token.class, s).forEach(t -> {})").measure(() -> {
              select(casProvider.get(), Sentence.class)
                      .forEach(s -> selectCovered(Token.class, s).forEach(t -> {
                      }));
            })).add(new Benchmark("JCAS.select(Token.class).coveredBy(s).forEach(t -> {})")
                    .measure(() -> {
                      select(casProvider.get(), Sentence.class).forEach(
                              s -> casProvider.get().select(Token.class).coveredBy(s).forEach(t -> {
                              }));
                    }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).select().coveredBy(s).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .select().coveredBy(s).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.select(Token.class).filter(t -> coveredBy(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .filter(t -> coveredBy(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).stream().filter(t -> coveredBy(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .stream().filter(t -> coveredBy(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.select(Token.class).coveredBy(s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .coveredBy(s.getBegin(), s.getEnd()).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).select().coveredBy(s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(s -> casProvider
                                      .get().getAnnotationIndex(Token.class).select()
                                      .coveredBy(s.getBegin(), s.getEnd()).forEach(t -> {
                                      }));
                            }))
            .add(new Benchmark(
                    "selectCovered(JCAS, Token.class, s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> selectCovered(casProvider.get(), Token.class,
                                              s.getBegin(), s.getEnd()).forEach(t -> {
                                              }));
                            }))
            .runAll();
  }

  @Test
  public void benchmarkSelectCovering() {
    new BenchmarkGroup("select covering", templateForNormalOperations)
            .addIgnoringTemplate(warmupTask)
            .add(new Benchmark("JCasUtil.selectCovering(Token.class, s).forEach(t -> {})")
                    .measure(() -> {
                      select(casProvider.get(), Sentence.class)
                              .forEach(s -> JCasUtil.selectCovering(Token.class, s).forEach(t -> {
                              }));
                    }))
            .add(new Benchmark("CasUtil.selectCovering(tokenType, s).forEach(t -> {})")
                    .measure(() -> {
                      CAS cas = casProvider.get().getCas();
                      select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> CasUtil
                              .selectCovering(getType(cas, TYPE_NAME_TOKEN), s).forEach(t -> {
                              }));
                    }))
            .add(new Benchmark("JCAS.select(Token.class).covering(s).forEach(t -> {})")
                    .measure(() -> {
                      select(casProvider.get(), Sentence.class).forEach(
                              s -> casProvider.get().select(Token.class).covering(s).forEach(t -> {
                              }));
                    }))
            .add(new Benchmark(
                    "CAS.getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).select().covering(s).forEach(t -> {})")
                            .measure(() -> {
                              CAS cas = casProvider.get().getCas();
                              select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> casProvider
                                      .get().getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN))
                                      .select().covering(s).forEach(t -> {
                                      }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).select().covering(s).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .select().covering(s).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.select(Token.class).filter(t -> covering(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .filter(t -> covering(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "CAS.getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN)).stream().filter(t -> covering(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              CAS cas = casProvider.get().getCas();
                              select(cas, getType(cas, TYPE_NAME_SENTENCE)).forEach(s -> casProvider
                                      .get().getAnnotationIndex(getType(cas, TYPE_NAME_TOKEN))
                                      .stream().filter(t -> covering(t, s)).forEach(t -> {
                                      }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).stream().filter(t -> covering(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .stream().filter(t -> covering(t, s)).forEach(t -> {
                                              }));
                            }))
            .runAll();
  }

  @Test
  public void benchmarkSelectAt() {
    new BenchmarkGroup("select at", templateForNormalOperations).addIgnoringTemplate(warmupTask)
            .add(new Benchmark(
                    "JCasUtil.selectAt(CAS, Token.class, s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(
                                              s -> JCasUtil
                                                      .selectAt(casProvider.get(), Token.class,
                                                              s.getBegin(), s.getEnd())
                                                      .forEach(t -> {
                                                      }));
                            }))
            .add(new Benchmark(
                    "JCAS.select(Token.class).at(s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .at(s.getBegin(), s.getEnd()).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark("JCAS.select(Token.class).at(s).forEach(t -> {})").measure(() -> {
              select(casProvider.get(), Sentence.class)
                      .forEach(s -> casProvider.get().select(Token.class).at(s).forEach(t -> {
                      }));
            }))
            .add(new Benchmark(
                    "JCAS.select(Token.class).filter(t -> colocated(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class)
                                      .forEach(s -> casProvider.get().select(Token.class)
                                              .filter(t -> colocated(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).stream().filter(t -> colocated(t, s)).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .stream().filter(t -> colocated(t, s)).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).select().at(s).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .select().at(s).forEach(t -> {
                                              }));
                            }))
            .add(new Benchmark(
                    "JCAS.getAnnotationIndex(Token.class).select().at(s.getBegin(), s.getEnd()).forEach(t -> {})")
                            .measure(() -> {
                              select(casProvider.get(), Sentence.class).forEach(
                                      s -> casProvider.get().getAnnotationIndex(Token.class)
                                              .select().at(s.getBegin(), s.getEnd()).forEach(t -> {
                                              }));
                            }))
            .runAll();
  }
}
