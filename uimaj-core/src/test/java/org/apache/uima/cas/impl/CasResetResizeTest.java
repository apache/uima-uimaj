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

import java.util.Collections;
import java.util.Properties;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.Resource;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class CasResetResizeTest extends TestCase {

  /**
   * Constructor for CasPerformanceTuningSettingsTest.
   * 
   * @param arg0
   */
  public CasResetResizeTest(String arg0) {
    super(arg0);
  }

  public void testInitialHeapSize() throws Exception {
    try {
      TaeDescription testDescriptor = UIMAFramework.getXMLParser().parseTaeDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));


      Properties perfSettings = new Properties();
      perfSettings.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "50000");
      //(UIMAFramework.CAS_INITIAL_HEAP_SIZE, (Object)50000);
      TextAnalysisEngine taeDefault = UIMAFramework.produceTAE(testDescriptor, Collections.singletonMap(Resource.PARAM_PERFORMANCE_TUNING_SETTINGS, (Object) perfSettings));
      CAS cas = taeDefault.newCAS();
      int heapSize = ((CASImpl) cas).getHeap().getHeapSize();
      int bufSize = ((CASImpl)cas).getHeap().heap.length;
      //System.out.println("Heap size: " + heapSize + ", buffer size: " + bufSize);       
      Assert.assertTrue(bufSize < CASImpl.DEFAULT_RESET_HEAP_SIZE);
      assertTrue(heapSize <= bufSize);
      
      //create enough annotations to exceed the DEFAULT_RESET_HEAP_SIZE
      Type annotType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);
      for (int i = 0; i < 2000000; i++) {
        cas.createAnnotation(annotType, i, i);
      }
      
      // heap growth (words):
      //                      500K is the switchover point
      //                     16 * 1024 *1024 = 16,777,216 is the switchover point
      //                        then the additional is 16,777,216
      //  50k, 100k, 200k, 400k, 800k, 1.6m, 3.2m, 6.4m, 12.8m
      //   8    7     6     5     4     3     2     1       0  
      
      heapSize = ((CASImpl) cas).getHeap().getHeapSize();
      assertEquals(12800000, heapSize);
      
      //reset the CAS - it should shrink the 4th time
      //  first time, Cas is of capacity 8,500,000, but has 8,000,008 cells used ==> shrunk size should remain the same
      //  second time, prev is 8,500,000, so no shrinking
      //  third time, gets shrunk to half the # of steps
      //  fourth time, gets shrunk to half the # of steps
      
//      int[] expected = new int[] {8300000, 8300000, 3800000, 1300000, 400000, 200000, 100000, 50000};  // for doubling
      
      int[] expected = {6400000, 3200000, 1600000, 800000, 400000, 200000};    // for 1 step
      resets(cas, 20, 12800000);
      for (int i = 0; i < 6; i++) {
        resets(cas, 5, expected[i]);
      }

      //If instead we create the annotations in smaller chunks and reset each time,
      //the CAS buffer size shouldn't grow
      cas = taeDefault.newCAS();
      
      for (int j = 0; j < 10; j++) {
        for (int i = 0; i < 200000; i++) {
          cas.createAnnotation(annotType, i, i);
        }
        
        heapSize = ((CASImpl) cas).getHeap().getHeapSize();
        Assert.assertTrue(heapSize == 1600000);      
        cas.reset();
      }
  
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  void resets(CAS cas, int n, int expected) {
    for (int i = 0; i < n; i++) {
      cas.reset();
    }
    int heapSize = ((CASImpl) cas).getHeap().getHeapSize();
//    System.out.format("Actual: %,d expected: %,d%n", heapSize, expected);
    assertEquals(expected, heapSize);       
  }
  
  

}
