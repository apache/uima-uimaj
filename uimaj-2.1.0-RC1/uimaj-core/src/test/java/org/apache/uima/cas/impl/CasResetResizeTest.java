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

      // check default setting
      TextAnalysisEngine taeDefault = UIMAFramework.produceTAE(testDescriptor);
      CAS cas = taeDefault.newCAS();
      int heapSize = ((CASImpl) cas).heap.getCurrentTempSize();
      // System.out.println("Heap size: " + heapSize + ", buffer size: " + ((CASImpl)
      // cas).heap.heap.length);
      Assert.assertTrue(heapSize < CASImpl.DEFAULT_RESET_HEAP_SIZE);
      assertTrue(heapSize <= ((CASImpl) cas).heap.heap.length);
      Type annotType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);
      for (int i = 0; i < 2000000; i++) {
        cas.createAnnotation(annotType, i, i);
      }
      heapSize = ((CASImpl) cas).heap.getCurrentTempSize();
      // System.out.println("Heap size: " + heapSize + ", buffer size: " + ((CASImpl)
      // cas).heap.heap.length);
      assertTrue(heapSize <= ((CASImpl) cas).heap.heap.length);
      Assert.assertTrue(heapSize > CASImpl.DEFAULT_RESET_HEAP_SIZE);
      cas.reset();
      heapSize = ((CASImpl) cas).heap.getCurrentTempSize();
      // System.out.println("Heap size: " + heapSize + ", buffer size: " + ((CASImpl)
      // cas).heap.heap.length);
      assertTrue(heapSize <= ((CASImpl) cas).heap.heap.length);
      Assert.assertTrue(heapSize < CASImpl.DEFAULT_RESET_HEAP_SIZE);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
