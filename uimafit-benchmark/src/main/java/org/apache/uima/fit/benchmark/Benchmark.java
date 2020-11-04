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
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

import org.apache.commons.lang3.StringUtils;

public class Benchmark {

  private final static String CPU_TIME_NOT_SUPPORTED_MSG = "CPU time not support by current thread.";

  private IntConsumer initializer = t -> {
  };
  private RunnableWithExceptions subject;

  private final String name;

  private boolean verbose = false;

  private int baseRepeat = 20;
  private int repeatIncrementTimes;

  private int baseMagnitude = 1;
  private int incrementTimes;
  private IntFunction<Integer> magnitudeIncrement = t -> t;
  private LongSupplier timer = () -> System.currentTimeMillis();
  private String timerUnit = "ms";

  private List<Batch> batches = new ArrayList<>();

  public Benchmark(String aName, Benchmark aTemplate) {
    name = aName;

    initializer = aTemplate.initializer;
    subject = aTemplate.subject;

    baseRepeat = aTemplate.baseRepeat;
    repeatIncrementTimes = aTemplate.repeatIncrementTimes;

    baseMagnitude = aTemplate.baseMagnitude;
    incrementTimes = aTemplate.incrementTimes;
    magnitudeIncrement = aTemplate.magnitudeIncrement;
    timer = aTemplate.timer;
    timerUnit = aTemplate.timerUnit;
  }

  public Benchmark(String aName) {
    name = aName;
  }

  public String getName() {
    return name;
  }

  public Benchmark timer(LongSupplier aTimer) {
    timer = aTimer;
    return this;
  }

  public Benchmark timerUnit(String timerUnit) {
    this.timerUnit = timerUnit;
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
    for (int i = 0; i < baseRepeat; i++) {

      long startTime = timer.getAsLong();
      try {
        subject.run();
        batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime));
      } catch (Exception e) {
        batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime, e));
      }
    }

    return batch;
  }

  public void run() {
    if (verbose) {
      System.out.printf("%n%s%n", StringUtils.repeat("=", name.length()));
      System.out.printf("%s%n", name);
      System.out.printf("%s%n", StringUtils.repeat("=", name.length()));
    }
    else {
      System.out.printf("%s: ", name);
    }

    int magnitude = baseMagnitude;
    int n = 0;

    System.out.print("Running benchmark... ");
    do {
      if (magnitude > 0) {
        System.out.printf("%d ", magnitude);
      }
      batches.add(runBatch(magnitude));
      magnitude = magnitudeIncrement.apply(magnitude);
      n++;
    } while (n < incrementTimes);
    System.out.printf("%n");

    if (verbose) {
      for (Batch b : batches) {
        System.out.printf("%s%n", b);
      }
    }
  }

  public List<Batch> getBatches() {
    return batches;
  }

  public long getMaxDuration() {
    return getBatches().stream().flatMap(b -> b.getMeasurements().stream()).max(comparing(Measurement::getDuration))
        .get().getDuration();
  }

  public long getCumulativeDuration() {
    return getBatches().stream().flatMap(b -> b.getMeasurements().stream()).mapToLong(Measurement::getDuration).sum();
  }

  public Measurement getSlowestMeasurement() {
    return getBatches().stream().flatMap(b -> b.getMeasurements().stream()).max(comparing(Measurement::getDuration))
        .get();
  }

  public String getTimerUnit() {
    return timerUnit;
  }

  /** Get CPU time in nanoseconds. */
  public static long cpu( ) {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    if(bean.isCurrentThreadCpuTimeSupported( )) {
      return bean.getCurrentThreadCpuTime( );
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

  /** Get user time in nanoseconds. */
  public static long user( ) {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    if(bean.isCurrentThreadCpuTimeSupported( )) {
      return bean.getCurrentThreadUserTime();
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

  /** Get static system time in nanoseconds. */
  public static long system( ) {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    if(bean.isCurrentThreadCpuTimeSupported( )) {
      return bean.getCurrentThreadCpuTime( ) - bean.getCurrentThreadUserTime( );
    }
    throw new UnsupportedOperationException(CPU_TIME_NOT_SUPPORTED_MSG);
  }

}