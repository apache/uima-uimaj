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

import static java.util.Comparator.comparing;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Benchmark {

  private final static String CPU_TIME_NOT_SUPPORTED_MSG = "CPU time not support by current thread.";

  private IntConsumer initializer = t -> {
  };
  private RunnableWithExceptions subject;

  private final String name;

  private boolean verbose = true;

  private int baseRepeat = 20;
  private int repeatIncrementTimes;

  private int baseMagnitude = 1;
  private int incrementTimes;
  private IntFunction<Integer> magnitudeIncrement = t -> t;
  private LongSupplier timer = () -> System.currentTimeMillis();
  private DoubleFunction<Double> toMs = n -> n;
  private long timeLimitMs = 5 * 1000;

  private boolean ignore = false;

  private List<Batch> batches = new ArrayList<>();
  private DescriptiveStatistics stats = null;

  public Benchmark(String aName) {
    name = aName;
  }

  public Benchmark(Benchmark aTemplate) {
    this(aTemplate.getName());
    applyTemplate(aTemplate);
  }

  public Benchmark(String aName, Benchmark aTemplate) {
    this(aName);
    applyTemplate(aTemplate);
  }

  public Benchmark ignore(boolean aIgnore) {
    ignore = aIgnore;
    return this;
  }

  public boolean isIgnored() {
    return ignore;
  }

  public Benchmark limit(long aLimit) {
    timeLimitMs = aLimit;
    return this;
  }

  public long getTimeLimitMs() {
    return timeLimitMs;
  }

  public void applyTemplate(Benchmark aTemplate) {
    initializer = aTemplate.initializer;

    baseRepeat = aTemplate.baseRepeat;
    repeatIncrementTimes = aTemplate.repeatIncrementTimes;

    baseMagnitude = aTemplate.baseMagnitude;
    incrementTimes = aTemplate.incrementTimes;
    magnitudeIncrement = aTemplate.magnitudeIncrement;
    timer = aTemplate.timer;
    toMs = aTemplate.toMs;
  }

  public String getName() {
    return name;
  }

  public Benchmark timer(LongSupplier aTimer, DoubleFunction<Double> aToMs) {
    timer = aTimer;
    toMs = aToMs;
    return this;
  }

  public Benchmark repeat(int aRepeat) {
    baseRepeat = aRepeat;
    return this;
  }

  public Benchmark magnitude(int aMagnitude) {
    baseMagnitude = aMagnitude;
    return this;
  }

  public Benchmark magnitudeIncrement(IntFunction<Integer> aIncrement) {
    magnitudeIncrement = aIncrement;
    return this;
  }

  public Benchmark incrementTimes(int aTimes) {
    incrementTimes = aTimes;
    return this;
  }

  public Benchmark initialize(IntConsumer aPieceOfCode) {
    initializer = aPieceOfCode;
    return this;
  }

  public Benchmark measure(RunnableWithExceptions aPieceOfCode) {
    subject = aPieceOfCode;
    return this;
  }

  private Batch runBatch(int aMagnitude) {
    Batch batch = new Batch(aMagnitude);

    initializer.accept(aMagnitude);

    long initTime = System.currentTimeMillis();
    for (int i = 0; i < baseRepeat; i++) {
      long startTime = timer.getAsLong();
      try {
        subject.run();
        batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime));

        if (System.currentTimeMillis() > initTime + getTimeLimitMs()) {
          batch.setTimeLimitExceeded(true);
          break;
        }
      } catch (Exception e) {
        batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime, e));
      }
    }

    return batch;
  }

  public void run() {
    stats = null;

    if (verbose) {
      System.out.printf("%n%s%n", StringUtils.repeat("=", name.length()));
      System.out.printf("%s%n", name);
      System.out.printf("%s%n", StringUtils.repeat("=", name.length()));
    } else {
      System.out.printf("%s: ", name);
    }

    System.out.print("Running benchmark... ");

    int magnitude = baseMagnitude;
    int n = 0;
    do {
      if (magnitude > 0) {
        System.out.printf("%d ", magnitude);
      }
      Batch results = runBatch(magnitude);
      if (!ignore) {
        batches.add(results);
      }
      magnitude = magnitudeIncrement.apply(magnitude);
      n++;
    } while (n < incrementTimes);
    System.out.printf("%n");

    if (verbose) {
      for (Batch b : batches) {
        System.out.printf("%s%n", batchToString(b));
      }
    }
  }

  public List<Batch> getBatches() {
    return batches;
  }

  public long getMaxDuration() {
    return (long) getStats().getMax();
  }

  public long getCumulativeDuration() {
    return (long) getStats().getSum();
  }

  public long getAverageDuration() {
    return (long) getStats().getMean();
  }

  public Measurement getSlowestMeasurement() {
    return getBatches().stream().flatMap(b -> b.getMeasurements().stream())
            .max(comparing(Measurement::getDuration)).get();
  }

  public DescriptiveStatistics getStats() {
    if (stats == null) {
      stats = new DescriptiveStatistics();
      getBatches().stream().flatMap(b -> b.getMeasurements().stream())
              .mapToLong(Measurement::getDuration).forEach(stats::addValue);
    }
    return stats;
  }

  public double toMs(double duration) {
    return toMs.apply(duration);
  }

  public String batchToString(Batch aBatch) {
    DescriptiveStatistics batchStats = new DescriptiveStatistics();

    StringBuilder sb = new StringBuilder();
    sb.append("[").append(
            String.format("%7d/%7d | ", aBatch.getMagnitude(), aBatch.getMeasurements().size()));
    int failures = 0;
    for (Measurement m : aBatch.getMeasurements()) {
      if (m.failed()) {
        failures++;
      } else {
        batchStats.addValue(m.getDuration());
      }
    }
    sb.append(String.format("min: %10.3f | ", toMs.apply(batchStats.getMin())));
    sb.append(String.format("max: %10.3f | ", toMs.apply(batchStats.getMax())));
    sb.append(String.format("median: %10.3f | ", toMs.apply(batchStats.getPercentile(50))));
    sb.append(String.format("cumulative: %10.3f | ", toMs.apply(batchStats.getSum())));
    sb.append(String.format("fail: %4d", failures));
    if (aBatch.isTimeLimitExceeded()) {
      sb.append(" | time limit exceeded");
    }
    sb.append("]");
    return sb.toString();
  }

  /** Get CPU time in nanoseconds. */
  public static long cpuTime() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    if (bean.isCurrentThreadCpuTimeSupported()) {
      return bean.getCurrentThreadCpuTime();
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

  /** Get user time in nanoseconds. */
  public static long userTime() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    if (bean.isCurrentThreadCpuTimeSupported()) {
      return bean.getCurrentThreadUserTime();
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

  /** Get static system time in nanoseconds. */
  public static long system() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    if (bean.isCurrentThreadCpuTimeSupported()) {
      return bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime();
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

}