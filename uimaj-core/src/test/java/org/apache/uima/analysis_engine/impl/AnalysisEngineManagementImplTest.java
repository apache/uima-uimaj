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
package org.apache.uima.analysis_engine.impl;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.Utilities;

import junit.framework.TestCase;

public class AnalysisEngineManagementImplTest extends TestCase {

  public void testNameGenerate() throws Exception {
    assertEquals("foo", AnalysisEngineManagementImpl.getRootName("foo"));
    assertEquals("foo2", AnalysisEngineManagementImpl.getRootName("foo"));
    assertEquals("foo3", AnalysisEngineManagementImpl.getRootName("foo"));
    
    // Try multi-threaded
    final Random random = new Random();
    final Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    int numberOfThreads = Math.min(50, Utilities.numberOfCores * 5); 

    MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {

      //  This method is run 100 * # of threads 
      //  It gets a root name based on "bar" - each one should get another name
      //   Tested by adding to a concurrent set and verifying it wasn't there before.
      public void call(int threadNbr, int repeatNbr, StringBuilder sb) throws Exception {
//        Random random = new Random();
        for (int j = 0; j < 2; j++) {
          assertTrue(s.add(AnalysisEngineManagementImpl.getRootName("bar")));
//          Thread.sleep(10, random.nextInt(2000));
          if ((threadNbr % 2) == 0) {
            Thread.sleep(0, random.nextInt(2000));  // sleep for 2 microseconds
          }
        }
      }
    };
    
    System.out.format("test multicore AnalysisEngineManagementImpl getRootName with %d threads%n", numberOfThreads);

    MultiThreadUtils.tstMultiThread("UniqueRootNameGenerator",  numberOfThreads,  100, run2isb, null);
//    System.out.println("debug");
  }
}
