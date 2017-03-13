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
import java.io.FileNotFoundException;

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
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * This test insures that Pear compoents run in a cas pool switch classloaders properly
 * 
 * It installs a pear every time it runs, to insure the test works on Linux and Windows
 *   Note: install handles converting classpath separator characters, etc.
 * 
 */
public class PearCasPoolTest extends TestCase {
  private static final String separator = System.getProperties().getProperty("file.separator");
  
  // Temporary working directory, used to install the pear package
  private File pearInstallDir = null;
  private final String PEAR_INSTALL_DIR = "target/pearInCPM_install_dir";
  private PackageBrowser installedPear;


  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    // disable schema validation -- this test uses descriptors
    // that don't validate, for some reason
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
    
    // create pear install directory in the target
    pearInstallDir = new File(PEAR_INSTALL_DIR);
    pearInstallDir.mkdirs();
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
    ResourceManager rm = UIMAFramework.newDefaultResourceManager();
    
    // check temporary working directory
    if (this.pearInstallDir == null)
      throw new FileNotFoundException("PEAR install directory not found");
    
    // get pear files to install 
    // relative resolved using class loader
    File pearFile = JUnitExtension.getFile("pearTests/pearForCPMtest.pear");
    Assert.assertNotNull(pearFile);
    
    // Install PEAR packages
    installedPear = PackageInstaller.installPackage(this.pearInstallDir, pearFile, false);
    Assert.assertNotNull(installedPear);

    
   
    core(10, 2, 3, null);
    core(10, 2, 2, null);
    core(10, 3, 3, null);
    core(10, 3, 4, null);
    core(10, 3, 5, null);
    core(10, 4, 4, null);
    core(10, 4, 5, null);
    core(10, 2, 3, rm);
    core(10, 2, 2, rm);
    core(10, 3, 3, rm);
    core(10, 3, 4, rm);
    core(10, 3, 5, rm);
    core(10, 4, 4, rm);
    core(10, 4, 5, rm);
    System.out.println("");  //final new line
  }

  private void core(int documentCount, int threadCount, int poolSize, 
      ResourceManager resourceManager) throws Exception {
    // setup CPM to process  documents
    CollectionProcessingEngine cpe = setupCpm(documentCount, threadCount, poolSize, resourceManager);

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
  private CollectionProcessingEngine setupCpm(int documentCount, int threadCount, int poolSize,
      ResourceManager resourceManager) throws Exception {
    CpeDescription cpeDesc = null;
    CollectionProcessingEngine cpe = null;

    try {
      String colReaderBase = JUnitExtension.getFile("CpmTests" + separator
              + "ErrorTestCollectionReader.xml").getAbsolutePath();
      String taeBase = JUnitExtension.getFile("CpmTests" + separator + "aggrContainingPearSpecifier.xml").getAbsolutePath();
      String casConsumerBase = JUnitExtension.getFile("CpmTests" + separator
              + "ErrorTestCasConsumer.xml").getAbsolutePath();

      // created needed descriptors
      String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(colReaderBase, documentCount);
//      String taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase);
      String taeDesc = taeBase;
      String casConsumerDesc = DescriptorMakeUtil.makeCasConsumer(casConsumerBase);

      // create cpm descriptor
      cpeDesc = CpeDescriptorFactory.produceDescriptor();
      cpeDesc.setInputQueueSize(threadCount);
      cpeDesc.setOutputQueueSize(threadCount);
      cpeDesc.setProcessingUnitThreadCount(threadCount);
      cpeDesc.getCpeCasProcessors().setPoolSize(poolSize);

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
      cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, resourceManager, null);
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }

    return cpe;
  }
}
