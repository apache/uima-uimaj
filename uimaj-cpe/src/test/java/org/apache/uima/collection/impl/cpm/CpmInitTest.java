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

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.impl.cpm.utils.DescriptorMakeUtil;
import org.apache.uima.collection.impl.cpm.utils.FunctionErrorStore;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.test.junit_extension.JUnitExtension;

public class CpmInitTest extends TestCase {
  private static final String separator = System.getProperties().getProperty("file.separator");

  /**
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    FunctionErrorStore.resetCount();
  }

  public void testInitSingleThreadCPMMode() throws Exception {
    int documentCount = 10000000;
    int threadCount = 1;

    // setup CPM to process 1 documents
    CollectionProcessingEngine cpe = setupCpm(documentCount, threadCount, false, true);

    // create and register a status callback listener
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    // run CPM
    cpe.process();

    // wait for initialized call from the CPM
    while (!listener.isInitialized()) {
      Thread.sleep(10);
    }
    System.out.println("SingleThreadCPMMode Initialize was called: " + listener.isInitialized());

    // Let the CPM process some docs, before calling stop
    Thread.sleep(300);

    // stop CPM
    cpe.stop();

    // wait until CPM has aborted
    while (!listener.isAborted()) {
      Thread.sleep(5);
    }
  }

  public void testInitMultiThreadCPMMode() throws Exception {
    int documentCount = 10000000; // hopefully enough that we won't finish before we abort
    int threadCount = 1;

    // setup CPM to process 1 documents
    CollectionProcessingEngine cpe = setupCpm(documentCount, threadCount, false, false);

    // create and register a status callback listener
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    // run CPM
    cpe.process();

    while (!listener.isInitialized()) {
      Thread.sleep(10);
    }
    System.out.println("MultiThreadCPMMode Initialize was called: " + listener.isInitialized());

    // Let the CPM process some docs, before calling stop
    Thread.sleep(300);

    // stop CPM
    cpe.stop();

    // wait until CPM has aborted
    while (!listener.isAborted()) {
      Thread.sleep(5);
    }
  }

  public void testInitMultiThreadCPM() throws Exception {
    int documentCount = 10000000;
    int threadCount = 3;

    // setup CPM to process documents
    CollectionProcessingEngine cpe = setupCpm(documentCount, threadCount, false, false);

    // create and register a status callback listener
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    // run CPM
    cpe.process();

    while (!listener.isInitialized()) {
      Thread.sleep(10);
    }
    System.out.println("testInitMultiThreadCPM()-Initialize was called: "
            + listener.isInitialized());
    // Let the CPM process some docs, before calling stop
    Thread.sleep(300);

    // stop CPM
    cpe.stop();

    // wait until CPM has aborted
    while (!listener.isAborted()) {
      Thread.sleep(5);
    }
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
  private CollectionProcessingEngine setupCpm(int documentCount, int threadCount,
          boolean useSlowAnnotator, boolean singleThreaded) throws Exception {
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

      if (singleThreaded) {
        cpeDesc.setDeployment("single-threaded");
      }

      // add tae
      CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
              .produceCasProcessor("ErrorTestAnnotator");
      integratedProcessor.setDescriptor(taeDesc);
      cpeDesc.addCasProcessor(integratedProcessor);

      // add slow annotator if requested
      if (useSlowAnnotator) {
        CpeIntegratedCasProcessor slowProcessor = CpeDescriptorFactory
                .produceCasProcessor("SlowAnnotator");
        slowProcessor.setDescriptor(JUnitExtension.getFile("CpmTests" + separator
                + "SlowAnnotator.xml").getAbsolutePath());
        cpeDesc.addCasProcessor(slowProcessor);
      }

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
