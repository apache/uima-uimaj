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

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

import org.apache.commons.lang3.StringUtils;

public class Benchmark {
    private IntConsumer initializer = t -> {};
    private RunnableWithExceptions subject;
    
    private String name;
    private int baseRepeat = 20;
    private int repeatIncrementTimes;
    
    private int baseMagnitude = 1;
    private int incrementTimes;
    private IntFunction<Integer> magnitudeIncrement = t -> t;
    private LongSupplier timer = () -> System.currentTimeMillis();
    
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
    }

    public Benchmark(String aName) {
      name = aName;
    }

    public Benchmark timer(LongSupplier aTimer)
    {
      timer = aTimer;
      return this;
    }

    public Benchmark repeat(int aRepeat)
    {
      baseRepeat = aRepeat;
      return this;
    }

    public Benchmark magnitude(int aMagnitude)
    {
      baseMagnitude = aMagnitude;
      return this;
    }

    public Benchmark magnitudeIncrement(IntFunction<Integer> aIncrement)
    {
      magnitudeIncrement = aIncrement;
      return this;
    }

    public Benchmark incrementTimes(int aTimes)
    {
      incrementTimes = aTimes;
      return this;
    }

    public Benchmark initialize(IntConsumer aPieceOfCode)
    {
      initializer = aPieceOfCode;
      return this;
    }
    
    public Benchmark measure(RunnableWithExceptions aPieceOfCode)
    {
      subject = aPieceOfCode;
      return this;
    }
    
    private Batch runBatch(int aMagnitude)
    {
      Batch batch = new Batch(aMagnitude);
      
      initializer.accept(aMagnitude);
      for (int i = 0; i < baseRepeat; i++) {
        
        long startTime = timer.getAsLong();
        try {
          subject.run();
          batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime));
        }
        catch (Exception e) {
          batch.addMeasurement(new Measurement(i, timer.getAsLong() - startTime, e));
        }
      }
      
      return batch;
    }
    
    public void run()
    {
      System.out.printf("%n%s%n", StringUtils.repeat("=", name.length()));
      System.out.printf("%s%n", name);
      System.out.printf("%s%n", StringUtils.repeat("=", name.length()));
      
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
      System.out.printf("%n%n");
      
      for (Batch b : batches) {
        System.out.printf("%s%n", b);
      }
    }
  }