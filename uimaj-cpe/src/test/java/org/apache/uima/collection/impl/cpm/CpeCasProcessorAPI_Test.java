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

import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.collection.metadata.CpeRemoteCasProcessor;
import org.apache.uima.collection.metadata.CpeTimer;

/**
 * this TestCase is testing the cpm API concerning the setting of CasProcessors
 * 
 * <p>
 * The general workflow of the TestCases is producing a default cpm and then adding a new
 * CasProcessor to this configuration. After that, the new configuration is read via API and checked
 * for correctness.
 * </p>
 * <b>Attention!!!</b><br>
 * All parameters set in the script are only dummy values. They do not have any corresponding files,
 * values or something else. The parameters are only set to be comparable for unit testing.
 * 
 */
public class CpeCasProcessorAPI_Test extends TestCase {

  private static final String confLocation = "c://cpm/conf/";

  private CpeDescription cpe = null;

  /**
   * create a new (default) cpe ( including a remote, local and integrated CasProcessor)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    // Creates the Cpe Descriptor
    cpe = CpeDescriptorFactory.produceDescriptor();
    // Create CollectionReader and associate it with the descriptor
    CpeDescriptorFactory.produceCollectionReader(confLocation + "collectionReaders/WFReader.xml",
            cpe);
    // Crate CasInitializer and associate it with the CollectionReader
    CpeDescriptorFactory.produceCollectionReaderCasInitializer(confLocation
            + "casinitializers/WFReaderInitializer.xml", cpe);
    // Create CasProcessors
    CpeCasProcessors processors = CpeDescriptorFactory.produceCasProcessors(2, 4, 3, cpe);
    // Create Detag CasProcessor

    CpeRemoteCasProcessor remoteProcessor = CpeDescriptorFactory
            .produceRemoteCasProcessor("Detag Miner");
    // CpeComponentDescriptor detagDesc =
    // CpeDescriptorFactory.produceComponentDescriptor("c://cpm/annotators/wfminers/detag/service.xml");
    remoteProcessor.setDescriptor("c://cpm/annotators/wfminers/detag/service.xml"); // detagDesc);
    remoteProcessor.addDeployParam("vnsPort", "9003");
    remoteProcessor.addDeployParam("vnsHost", "localhost");
    remoteProcessor.setCasProcessorFilter("where Detag:DetagContent");
    remoteProcessor.setBatchSize(3);
    remoteProcessor.setMaxErrorCount(50);
    remoteProcessor.setMaxErrorSampleSize(500);
    remoteProcessor.setActionOnMaxError("continue");
    remoteProcessor.setMaxRestartCount(3);
    remoteProcessor.setActionOnMaxRestart("continue");
    remoteProcessor.setTimeout(5000);
    remoteProcessor.setCasProcessorFilter("where Detag:DetagContent");
    processors.addCpeCasProcessor(remoteProcessor);

    // Create Detag CasProcessor
    CpeLocalCasProcessor localProcessor = CpeDescriptorFactory.produceLocalCasProcessor(
            "DupShingle Miner", "Detag:DetagContent");
    localProcessor.setDescriptor("c://cpm/annotators/wfminers/detag/descriptor.xml");
    localProcessor
            .setExecutable("/home/cwiklik/cpm/wfcp/annotators/wfminers/detagger/bin/runDetagger.sh");
    localProcessor.addDeployParam("Parm1", "Value1");
    localProcessor.addDeployParam("vnsHost", "Host1");
    localProcessor.addExecArg("-DVNS_HOST=127.0.0.1");
    localProcessor.addExecArg("-DVNS_PORT=9904");
    localProcessor.setCasProcessorFilter("where Detag:DetagContent");
    localProcessor.setBatchSize(3);
    localProcessor.setMaxErrorCount(50);
    localProcessor.setMaxErrorSampleSize(500);
    localProcessor.setActionOnMaxError("continue");
    localProcessor.setMaxRestartCount(3);
    localProcessor.setActionOnMaxRestart("continue");
    localProcessor.setTimeout(5000);
    processors.addCpeCasProcessor(localProcessor);

    CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
            .produceCasProcessor("WF Writer");
    integratedProcessor.setDescriptor(confLocation + "consumers/wf/store/descriptor.xml");
    integratedProcessor.setBatchSize(100);
    processors.addCpeCasProcessor(integratedProcessor);

    CpeConfiguration config = CpeDescriptorFactory.produceCpeConfiguration(cpe);
    CpeCheckpoint checkpoint = CpeDescriptorFactory.produceCpeCheckpoint();
    checkpoint.setBatchSize(10);
    checkpoint.setFilePath("c://cpm/data/checkpoint.dat");
    checkpoint.setFrequency(3000, true);
    config.setCheckpoint(checkpoint);
    CpeTimer cpeTimer = CpeDescriptorFactory.produceCpeTimer("java");
    config.setCpeTimer(cpeTimer);
    config.setDeployment("immediate");
    config.setNumToProcess(1000);
    config.setStartingEntityId("");
  }

  /**
   * test the config of a remote CasProcessor with all possible values set
   * 
   * @throws Exception -
   */
  public void testAddRemoteCasProcessor() throws Exception {
    CpeCasProcessors processors = cpe.getCpeCasProcessors();

    CpeRemoteCasProcessor remoteProcessor = CpeDescriptorFactory
            .produceRemoteCasProcessor("Detag Miner _2_");
    remoteProcessor.setDescriptor("c://cpm/annotators/dummy.xml");
    remoteProcessor.addDeployParam("vnsPort", "9999");
    remoteProcessor.addDeployParam("vnsHost", "localhost");
    remoteProcessor.setCasProcessorFilter("where Detag:DetagContent_2");
    remoteProcessor.setBatchSize(5);
    remoteProcessor.setMaxErrorCount(51);
    remoteProcessor.setMaxErrorSampleSize(505);
    remoteProcessor.setActionOnMaxError("continue");
    remoteProcessor.setMaxRestartCount(2);
    remoteProcessor.setActionOnMaxRestart("continue");
    remoteProcessor.setTimeout(5002);
    processors.addCpeCasProcessor(remoteProcessor);

    assertEquals("Name", "Detag Miner _2_", remoteProcessor.getName());
    assertEquals("Descriptor()", "c://cpm/annotators/dummy.xml", remoteProcessor.getDescriptor());
    assertEquals("DeploymentParam", "9999",
            ((remoteProcessor.getDeploymentParams()).get("vnsPort")).getParameterValue());
    assertEquals("DeploymentParam", "localhost", ((remoteProcessor.getDeploymentParams())
            .get("vnsHost")).getParameterValue());
    assertEquals("CasProcessorFilter", "where Detag_colon_DetagContent_2", remoteProcessor
            .getCasProcessorFilter());
    assertEquals("BatchSize", 5, remoteProcessor.getBatchSize());
    assertEquals("MaxErrorCount", 51, remoteProcessor.getMaxErrorCount());
    assertEquals("MaxErrorSampleSize", 505, remoteProcessor.getMaxErrorSampleSize());
    assertEquals("ActionOnMaxError", "continue", remoteProcessor.getActionOnMaxError());
    assertEquals("MaxRestartCount", 2, remoteProcessor.getMaxRestartCount());
    assertEquals("ActionOnMaxRestart", "continue", remoteProcessor.getActionOnMaxRestart());
    assertEquals("Timeout", 5002, remoteProcessor.getTimeout());
  }

  /**
   * test the config of a local CasProcessor with all possible values set
   * 
   * @throws Exception -
   */
  public void testAddLocalCasProcessor() throws Exception {
    CpeCasProcessors processors = cpe.getCpeCasProcessors();

    // Create Detag CasProcessor
    CpeLocalCasProcessor localProcessor = CpeDescriptorFactory.produceLocalCasProcessor(
            "DupShingle Miner", "Detag:DetagContent");
    localProcessor.setDescriptor("c://cpm/annotators/example.xml");
    localProcessor.setExecutable("/some/path/executable.sh");
    localProcessor.addDeployParam("Parm1", "Value1x");
    localProcessor.addDeployParam("vnsHost", "Host1x");
    localProcessor.addExecArg("-DVNS_HOST=localhost");
    localProcessor.addExecArg("-DVNS_PORT=9905");
    localProcessor.setCasProcessorFilter("where Detag:DetagContent");
    localProcessor.setBatchSize(4);
    localProcessor.setMaxErrorCount(52);
    localProcessor.setMaxErrorSampleSize(503);
    localProcessor.setActionOnMaxError("continue");
    localProcessor.setMaxRestartCount(2);
    localProcessor.setActionOnMaxRestart("continue");
    localProcessor.setTimeout(5001);
    processors.addCpeCasProcessor(localProcessor);

    assertEquals("Name", "DupShingle Miner", localProcessor.getName());
    assertEquals("Descriptor()", "c://cpm/annotators/example.xml", localProcessor.getDescriptor());
    assertEquals("DeploymentParam", "Value1x",
            ((localProcessor.getDeploymentParams()).get("Parm1")).getParameterValue());
    assertEquals("DeploymentParam", "Host1x", ((localProcessor.getDeploymentParams())
            .get("vnsHost")).getParameterValue());
    assertEquals("CasProcessorFilter", "where Detag_colon_DetagContent", localProcessor
            .getCasProcessorFilter());
    assertEquals("BatchSize", 4, localProcessor.getBatchSize());
    assertEquals("MaxErrorCount", 52, localProcessor.getMaxErrorCount());
    assertEquals("MaxErrorSampleSize", 503, localProcessor.getMaxErrorSampleSize());
    assertEquals("ActionOnMaxError", "continue", localProcessor.getActionOnMaxError());
    assertEquals("MaxRestartCount", 2, localProcessor.getMaxRestartCount());
    assertEquals("ActionOnMaxRestart", "continue", localProcessor.getActionOnMaxRestart());
    assertEquals("Timeout", 5001, localProcessor.getTimeout());
  }

  /**
   * test the config of a integrated CasProcessor with all possible values set
   * 
   * @throws Exception -
   */
  public void testAddIntegratedCasProcessor() throws Exception {
    CpeCasProcessors processors = cpe.getCpeCasProcessors();

    CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
            .produceCasProcessor("WF Writer_X");
    assertEquals("name", "WF Writer_X", integratedProcessor.getName());
    integratedProcessor.setDescriptor(confLocation + "consumers/bla.xml");
    integratedProcessor.setBatchSize(99);
    assertEquals("BatchSize", 99, integratedProcessor.getBatchSize());
    integratedProcessor.addDeployParam("testversion", "V1.0");
    integratedProcessor.setActionOnMaxError("terminate");
    integratedProcessor.setActionOnMaxRestart("continue");
    integratedProcessor.setBatchSize(25);
    integratedProcessor.setCasProcessorFilter("some filter");
    integratedProcessor.setMaxErrorCount(300);
    integratedProcessor.setMaxErrorSampleSize(100);
    integratedProcessor.setMaxRestartCount(5);
    integratedProcessor.setTimeout(6000);
    integratedProcessor.setName("special name");
    processors.addCpeCasProcessor(integratedProcessor);

    assertEquals("name", "special name", integratedProcessor.getName());
    assertEquals("Descriptor()", confLocation + "consumers/bla.xml", integratedProcessor
            .getDescriptor());
    assertEquals("DeploymentParam", "V1.0", ((integratedProcessor.getDeploymentParams())
            .get("testversion")).getParameterValue());
    assertEquals("CasProcessorFilter", "some filter", integratedProcessor.getCasProcessorFilter());
    assertEquals("BatchSize", 25, integratedProcessor.getBatchSize());
    assertEquals("MaxErrorCount", 300, integratedProcessor.getMaxErrorCount());
    assertEquals("MaxErrorSampleSize", 100, integratedProcessor.getMaxErrorSampleSize());
    assertEquals("ActionOnMaxError", "terminate", integratedProcessor.getActionOnMaxError());
    assertEquals("MaxRestartCount", 5, integratedProcessor.getMaxRestartCount());
    assertEquals("ActionOnMaxRestart", "continue", integratedProcessor.getActionOnMaxRestart());
    assertEquals("Timeout", 6000, integratedProcessor.getTimeout());

  }
}
