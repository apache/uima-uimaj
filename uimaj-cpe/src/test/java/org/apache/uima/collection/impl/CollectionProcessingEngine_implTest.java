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

package org.apache.uima.collection.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class CollectionProcessingEngine_implTest extends TestCase {
  protected final String TEST_DATAPATH = JUnitExtension.getFile(
      "CollectionProcessingEngineImplTest").getPath()
      + System.getProperty("path.separator") + JUnitExtension.getFile("ResourceTest");

  /**
         * Constructor for CollectionProcessingEngine_implTest.
         * 
         * @param arg0
         */
  public CollectionProcessingEngine_implTest(String arg0) {
    super(arg0);
  }

  /*
         * (non-Javadoc)
         * 
         * @see junit.framework.TestCase#setUp()
         */
  protected void setUp() throws Exception {
    File referenceFile = JUnitExtension
	.getFile("CollectionProcessingEngineImplTest/performanceTuningSettingsTestCpe.xml");
    System.setProperty("CPM_HOME", referenceFile.getParentFile().getParentFile().getAbsolutePath());
  }

  public void testPerformanceTuningSettings() throws Exception {
    try {
      Properties newProps = UIMAFramework.getDefaultPerformanceTuningProperties();
      newProps.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "100000");
      HashMap params = new HashMap();
      params.put(Resource.PARAM_PERFORMANCE_TUNING_SETTINGS, newProps);

      CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
	  new XMLInputSource(JUnitExtension
	      .getFile("CollectionProcessingEngineImplTest/performanceTuningSettingsTestCpe.xml")));
      CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc,
	  params);
      cpe.process();
      // Need to give CPE time to do its work. The following should work, but
      // doesn't
      // while (cpe.isProcessing())
      // {
      // Thread.sleep(1000);
      // }
      Thread.sleep(3000);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testExternalResoures() throws Exception {
    try {
      ResourceManager rm = UIMAFramework.newDefaultResourceManager();
      rm.setDataPath(TEST_DATAPATH);
      CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
	  new XMLInputSource(JUnitExtension
	      .getFile("CollectionProcessingEngineImplTest/externalResourceTestCpe.xml")));
      CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, rm,
	  null);
      CollectionReader colRdr = (CollectionReader) cpe.getCollectionReader();
      assertNotNull(colRdr.getUimaContext().getResourceObject("TestFileResource"));
      CasInitializer casIni = colRdr.getCasInitializer();
      assertNotNull(casIni.getUimaContext().getResourceObject("TestFileLanguageResource",
	  new String[] { "en" }));
      AnalysisEngine ae = (AnalysisEngine) cpe.getCasProcessors()[0];
      assertNotNull(ae.getUimaContext().getResourceObject("TestResourceObject"));
      assertNotNull(ae.getUimaContext().getResourceObject("TestLanguageResourceObject",
	  new String[] { "en" }));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testCasMultiplierTypeSystem() throws Throwable {
    CpeDescription cpeDesc = UIMAFramework.getXMLParser()
            .parseCpeDescription(new XMLInputSource(
                    JUnitExtension.getFile("CollectionProcessingEngineImplTest/cpeWithWrappedCasMultiplier.xml")));
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);
    // create and register a status callback listener
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    // run CPM
    cpe.process();

    // wait until CPM has finished
    while (!listener.isFinished()) {
      Thread.sleep(5);
    }
    
    //check that there was no exception
    if (listener.getLastStatus().isException()) {
      throw (Throwable)listener.getLastStatus().getExceptions().get(0);
    }
  }
}
