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

package org.apache.uima.collection.impl.cpm;

import java.io.File;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.impl.cpm.utils.DescriptorMakeUtil;
import org.apache.uima.collection.impl.cpm.utils.FunctionErrorStore;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

/**
 * This test aimes to check if the cpm implements the ProcessingUnitThreadCount in the correct
 * manner. That means that no matter which configuration and document number is chosen, every
 * document should be processed exactly one time.
 * 
 */
public class CpeImportTest extends TestCase {
  private static final String separator = System.getProperties().getProperty("file.separator");

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    // disable schema validation -- this test uses descriptors
    // that don't validate, for some reason
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
  }

  /**
   * @throws Exception -
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    FunctionErrorStore.resetCount();
  }

  /**
   * Test a CPE descriptor that uses the import syntax.
   */
  public void testImports() throws Exception {
    CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
            new XMLInputSource(JUnitExtension.getFile("CollectionProcessingEngineImplTest/CpeImportTest.xml")));            
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);

    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    cpe.process();

    // wait until CPE has finished
    while (!listener.isFinished()) {
      Thread.sleep(5);
    }

    // check that components were called
    final int documentCount = 1000; //this is the # of documents produced by the test CollectionReader
    Assert.assertEquals("StatusCallbackListener", documentCount, listener
            .getEntityProcessCompleteCount());
    Assert.assertEquals("CasConsumer process Count", documentCount, FunctionErrorStore
            .getCasConsumerProcessCount());
    Assert.assertEquals("Annotator process count", documentCount, FunctionErrorStore
            .getAnnotatorProcessCount());
    Assert.assertEquals("Collection reader getNext count", documentCount, FunctionErrorStore
            .getCollectionReaderGetNextCount());
  }
  
  /**
   * Test a CPE descriptor using import by name and requiring data patht o be set
   */
  public void testImportsWithDataPath() throws Exception {
    CpeDescription cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
            new XMLInputSource(JUnitExtension.getFile("CollectionProcessingEngineImplTest/CpeImportDataPathTest.xml")));
    ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
    File dataPathDir = JUnitExtension.getFile("CollectionProcessingEngineImplTest/imports");
    resMgr.setDataPath(dataPathDir.getAbsolutePath());
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, resMgr, null);

    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    cpe.process();

    // wait until CPE has finished
    while (!listener.isFinished()) {
      Thread.sleep(5);
    }

    // check that components were called
    final int documentCount = 1000; //this is the # of documents produced by the test CollectionReader
    Assert.assertEquals("StatusCallbackListener", documentCount, listener
            .getEntityProcessCompleteCount());
    Assert.assertEquals("CasConsumer process Count", documentCount, FunctionErrorStore
            .getCasConsumerProcessCount());
    Assert.assertEquals("Annotator process count", documentCount, FunctionErrorStore
            .getAnnotatorProcessCount());
    Assert.assertEquals("Collection reader getNext count", documentCount, FunctionErrorStore
            .getCollectionReaderGetNextCount());
  }  
}
