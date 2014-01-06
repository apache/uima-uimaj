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

import java.io.File;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ComponentInfoTest extends TestCase {
  public void testComponentInfo() throws Exception {
    //test the CAS.getCurrentComponentInfo() is null after a component has
    //been processed
    File descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml");
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
            new XMLInputSource(descFile));
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc);
    CAS cas = ae.newCAS();
    ae.process(cas);
    assertNull(((CASImpl)cas).getCurrentComponentInfo());
    
    //same test for aggregate
    //test the CAS.getCurrentComponentInfo() is null after a component has
    //been processed
    descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
    desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
            new XMLInputSource(descFile));
    ae = UIMAFramework.produceAnalysisEngine(desc);
    cas = ae.newCAS();
    ae.process(cas);
    assertNull(((CASImpl)cas).getCurrentComponentInfo());
  }
}
