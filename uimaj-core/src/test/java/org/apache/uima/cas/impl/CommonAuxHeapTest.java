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

package org.apache.uima.cas.impl;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class CommonAuxHeapTest extends TestCase {

  public void testcomputeShrunkArraySize() {
    CommonAuxHeap cah = new ByteHeap();
    
    // should return 1/2 way to the size needed
                                             //             cap    sz      limit    
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 1000, 2, 1000, 10));
    boolean ok = false;
    try {
      CommonAuxHeap.computeShrunkArraySize(1000, 10000, 2, 1000, 10);
    } catch (IllegalArgumentException e) {
      ok = true;
    }
    assertTrue(ok);
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 999, 2, 1000, 10));
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 501, 2, 1000, 10));
    assertEquals(500, CommonAuxHeap.computeShrunkArraySize(1000, 500, 2, 1000, 10));
    
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 999, 2, 999, 10));
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 501, 2, 999, 10));
    assertEquals( 500, CommonAuxHeap.computeShrunkArraySize(1000, 500, 2, 999, 10));
    
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 999, 2, 500, 10));
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 501, 2, 500, 10));
    assertEquals( 500, CommonAuxHeap.computeShrunkArraySize(1000, 500, 2, 500, 10));
    
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 999, 2, 300, 10));
    assertEquals(1000, CommonAuxHeap.computeShrunkArraySize(1000, 701, 2, 300, 10));
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 700, 2, 300, 10));
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 401, 2, 300, 10));
    
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 400, 2, 300, 10));  // 1000 700 400,   400 700   shrink 2, exp 1
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 201, 2, 300, 10));  // 1000 700 400,   400 700   shrink 2, exp 1
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 200, 2, 300, 10));  // 1000 700 400 200, 200 400 700 shrink 3, exp 2    
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 101, 2, 300, 10));  // 1000 700 400 200, 200 400 700 shrink 3, exp 2
//    assertEquals( 400, CommonAuxHeap.computeShrunkArraySize(1000, 100, 2, 300, 10));  // 1000 700 400 200 100  100 200 400  shrink 4, exp 2
    assertEquals( 400, CommonAuxHeap.computeShrunkArraySize( 700, 100, 2, 300, 10));  // 700 400 200 100  100 200 400  shrink 3, exp 2

    // shrink by 1 tests
    assertEquals( 700, CommonAuxHeap.computeShrunkArraySize(1000, 200, 2, 300, 399)); // 1000 700 400 399, 399 699 shrink 3, exp 2
    
    // halving tests    
//    assertEquals( 699, CommonAuxHeap.computeShrunkArraySize(1000, 200, 2, 300, 399)); // 1000 700 400 399, 399 699 shrink 3, exp 2
//    assertEquals( 688, CommonAuxHeap.computeShrunkArraySize(1000, 101, 2, 300, 388)); // 1000 700 400 388, 388 688
//    assertEquals( 699, CommonAuxHeap.computeShrunkArraySize(1000, 100, 2, 300, 399)); // 1000 700 400 399, 399 699

  }

}
