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

package org.apache.uima.internal.util;

import java.util.Arrays;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.resource.CasManager;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class IntVectorTest extends TestCase {

  public void testSortDedup() throws Exception {
    IntVector iv = new IntVector();
    iv.add(new int[] {5, 3, 2, 7, 5, 3, 4, 5, 6, 5, 9, 8, 7});
    iv.sortDedup();
    assertTrue(Arrays.equals(iv.toArray(), new int[] {2, 3, 4, 5, 6, 7, 8, 9}));    

    // check 3 edge cases: no dups, and last 2 items are dups, and 0 length
    iv = new IntVector();
    iv.add(new int[] {1, 3, 5, 7});
    iv.sortDedup();
    assertTrue(Arrays.equals(iv.toArray(), new int[] {1, 3, 5, 7}));
    
    iv = new IntVector();
    iv.add(new int[] {1,3, 5, 7, 7});
    iv.sortDedup();
    assertTrue(Arrays.equals(iv.toArray(), new int[] {1, 3, 5, 7}));
    
    iv = new IntVector();
    iv.sortDedup();
    assertTrue(Arrays.equals(iv.toArray(), new int[] {}));
    
  }
  
  // verify that several CASes in a pool in different views share the same type system
  
  public void testPool() throws Exception {
    try {
      
      AnalysisEngineDescription aed = (AnalysisEngineDescription)UIMAFramework.getXMLParser().parse(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));
      
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aed);

      // define a caspool of size 2
      CasManager cm = ((UimaContext_ImplBase)ae.getUimaContext()).getResourceManager().getCasManager();
      cm.defineCasPool("uniqueString", 2, null);
      
      CAS c1 = cm.getCas("uniqueString");
      CAS c2 = cm.getCas("uniqueString");
      c1.getJCas();
      
      CAS c1v2 = c1.createView("view2");
      CAS c2v2 = c2.createView("view3");
      c2v2.getJCas();
      
      TypeSystem ts = c1.getTypeSystem();
      
      Assert.assertTrue(ts == c2.getTypeSystem());
      Assert.assertTrue(ts == c1v2.getTypeSystem());
      Assert.assertTrue(ts == c2v2.getTypeSystem());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
