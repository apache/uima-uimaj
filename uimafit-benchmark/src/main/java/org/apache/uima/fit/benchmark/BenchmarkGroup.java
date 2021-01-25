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

import static java.lang.Math.round;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkGroup {
  private final String name;
  private final Benchmark template;
  private final List<Benchmark> benchmarks = new ArrayList<>();

  public BenchmarkGroup(String aName, Benchmark aTemplate) {
    name = aName;
    template = aTemplate;
  }

  public BenchmarkGroup addIgnoringTemplate(Benchmark aBenchmark) {
    benchmarks.add(aBenchmark);
    return this;
  }

  public BenchmarkGroup add(Benchmark aBenchmark) {
    if (template != null) {
      aBenchmark.applyTemplate(template);
    }
    benchmarks.add(aBenchmark);
    return this;
  }

  public void runAll() {
    System.out.printf(">>>>>>>>>>>>>>>>>>%n");
    System.out.printf("GROUP: %s", name);
    if (template != null) {
      System.out.printf(" (%s)", template.getName());
    }
    System.out.printf("%n");

    for (Benchmark benchmark : benchmarks) {
      benchmark.run();
    }

    System.out.printf("%n%nSorted by execution time:%n");
    benchmarks.stream()
        .filter(b -> !b.isIgnored())
        .sorted(comparing(Benchmark::getAverageDuration))
        .forEach(benchmark -> {
          System.out.printf("AVG: %.3fms (%10s total) -- %s%n", 
              benchmark.toMs(benchmark.getAverageDuration()),
              formatDurationWords(round(benchmark.toMs(benchmark.getCumulativeDuration())), true, true), 
              benchmark.getName());
        });

    System.out.printf(">>>>>>>>>>>>>>>>>>%n%n");
  }
}
