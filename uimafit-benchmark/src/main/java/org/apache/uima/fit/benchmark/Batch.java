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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Batch {
    private List<Measurement> measurements = new ArrayList<>();
    
    private final int magnitude;
    
    public Batch(int aMagnitude) {
      magnitude = aMagnitude;
    }
    
    public int getMagnitude() {
      return magnitude;
    }
    
    public void addMeasurement(Measurement aMeasurement) {
      measurements.add(aMeasurement);
    }
    
    public List<Measurement> getMeasurements() {
      return measurements;
    }
    
    @Override
    public String toString()
    {
      DescriptiveStatistics stats = new DescriptiveStatistics();
      
      StringBuilder sb = new StringBuilder();
      sb.append("[").append(String.format("%7d/%7d", magnitude, measurements.size())).append(": ");
      int failures = 0;
      for (Measurement m : measurements) {
        if (m.failed()) {
          failures++;
        }
        else {
          stats.addValue(m.getDuration());
        }
      }
      sb.append(String.format("min: %4.0f ", stats.getMin()));
      sb.append(String.format("max: %4.0f ", stats.getMax()));
      sb.append(String.format("median: %4.0f ", stats.getPercentile(50)));
      sb.append(String.format("cumulative: %6.0f ", stats.getSum()));
      sb.append(String.format("fail: %4d ", failures));
      sb.append("]");
      return sb.toString();
    }
  }