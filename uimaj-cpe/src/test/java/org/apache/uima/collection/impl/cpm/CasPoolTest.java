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

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.impl.cpm.utils.DescriptorMakeUtil;
import org.apache.uima.collection.impl.cpm.utils.FunctionErrorStore;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * This test insures that a common type system is shared in the CPM cas pool.
 * 
 */
public class CasPoolTest extends TestCase {
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
   * Create multiple processors which have to process multiple documents
   * 
   * @throws Exception -
   */
  public void testCasPool() throws Exception {
    // process 100 documents and multiple threads
    int documentCount = 100;
    int threadCount = 5;

    // setup CPM to process 100 documents
    CollectionProcessingEngine cpe = setupCpm(documentCount, threadCount);

    // create and register a status callback listener
    TestStatusCallbackListener listener = new TestStatusCallbackListener() {
      TypeSystem sts = null;
      public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
        super.entityProcessComplete(aCas, aStatus);
        if (sts == null) {
          sts = aCas.getTypeSystem();
        } else {
          Assert.assertTrue(sts == aCas.getTypeSystem());
        }
      }      
    };
    cpe.addStatusCallbackListener(listener);

    // run CPM
    cpe.process();

    // wait until CPM has finished
    while (!listener.isFinished()) {
      Thread.sleep(5);
    }

    // check if CasConsumer was called
    Assert.assertEquals("StatusCallbackListener", documentCount, listener
            .getEntityProcessCompleteCount());
    Assert.assertEquals("CasConsumer process Count", documentCount, FunctionErrorStore
            .getCasConsumerProcessCount());
    Assert.assertEquals("Annotator process count", documentCount, FunctionErrorStore
            .getAnnotatorProcessCount());
    Assert.assertEquals("Collection reader getNext count", documentCount, FunctionErrorStore
            .getCollectionReaderGetNextCount());
    Assert.assertEquals("number of annoators", threadCount, FunctionErrorStore.getAnnotatorCount());
  }

  /**
   * setup the CPM with base functionality.
   * 
   * @param documentCount
   *          how many documents should be processed
   * @param threadCount
   *          how many threads are used by the cpm
   * 
   * @return CollectionProcessingEngine - initialized cpe
   */
  private CollectionProcessingEngine setupCpm(int documentCount, int threadCount) throws Exception {
    CpeDescription cpeDesc = null;
    CollectionProcessingEngine cpe = null;

    try {
      String colReaderBase = JUnitExtension.getFile("CpmTests" + separator
              + "ErrorTestCollectionReader.xml").getAbsolutePath();
      String taeBase = JUnitExtension.getFile("CpmTests" + separator + "ErrorTestAnnotator.xml").getAbsolutePath();
      String casConsumerBase = JUnitExtension.getFile("CpmTests" + separator
              + "ErrorTestCasConsumer.xml").getAbsolutePath();

      // created needed descriptors
      String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(colReaderBase, documentCount);
      String taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase);
      String casConsumerDesc = DescriptorMakeUtil.makeCasConsumer(casConsumerBase);

      // create cpm descriptor
      cpeDesc = CpeDescriptorFactory.produceDescriptor();
      cpeDesc.setInputQueueSize(2);
      cpeDesc.setOutputQueueSize(2);
      cpeDesc.setProcessingUnitThreadCount(threadCount);

      // add tae
      CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
              .produceCasProcessor("ErrorTestAnnotator");
      integratedProcessor.setDescriptor(taeDesc);
      cpeDesc.addCasProcessor(integratedProcessor);

      // add casConsumer
      CpeIntegratedCasProcessor casConsumer = CpeDescriptorFactory
              .produceCasProcessor("ErrorTest CasConsumer");
      casConsumer.setDescriptor(casConsumerDesc);
      cpeDesc.addCasProcessor(casConsumer);

      // add collectionReader
      cpeDesc.addCollectionReader(colReaderDesc);

      // produce cpe
      cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null, null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return cpe;
  }
}
