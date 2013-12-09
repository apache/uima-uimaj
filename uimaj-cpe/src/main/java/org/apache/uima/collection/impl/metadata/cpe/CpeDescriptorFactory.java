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

package org.apache.uima.collection.impl.metadata.cpe;

import java.io.InputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.metadata.CpeDefaultValues;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecutable;
import org.apache.uima.collection.metadata.CasProcessorFilter;
import org.apache.uima.collection.metadata.CasProcessorMaxRestarts;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.collection.metadata.CasProcessorTimeout;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.collection.metadata.CpeRemoteCasProcessor;
import org.apache.uima.collection.metadata.CpeResourceManagerConfiguration;
import org.apache.uima.collection.metadata.CpeSofaMapping;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.collection.metadata.CpeTimer;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * Factory class for creating CpeDescriptors and their constituent objects.
 * 
 * 
 */
public class CpeDescriptorFactory {

  public CpeDescriptorFactory() {
  }

  /**
   * Produce a new CpeDescription from scratch. This CpeDescription will contain no components and
   * will have default settings.
   * 
   * @return An empty CpeDescription object with default settings
   */
  public static CpeDescription produceDescriptor() {
    CpeDescription descriptor = null;
    descriptor = new CpeDescriptionImpl();

    try {
      descriptor.setCpeConfiguration(produceCpeConfiguration(descriptor));
      descriptor.getCpeConfiguration().setNumToProcess((int) CpeDefaultValues.NUM_TO_PROCESS);
      descriptor.getCpeConfiguration().setDeployment(CpeDefaultValues.DEPLOY_AS);
      descriptor.getCpeConfiguration().setCheckpoint(produceCpeCheckpoint());
      descriptor.getCpeConfiguration().getCheckpoint().setFilePath("");
      descriptor.getCpeConfiguration().getCheckpoint().setFrequency(300000, true);
      descriptor.getCpeConfiguration().setCpeTimer(new CpeTimerImpl(""));
      return descriptor;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Parse a CpeDescription from a descriptor file.
   * 
   * @param aInput
   *          identifies the input file
   * 
   * @return The CpeDescription object parsed from the input
   * 
   * @throws InvalidXMLException
   *           if the descriptor is invalid
   */
  public static CpeDescription produceDescriptor(XMLInputSource aInput) throws InvalidXMLException {
    Object o;
    try {
      o = UIMAFramework.getXMLParser().parse(aInput);
      if (o instanceof CpeDescription) {
        return (CpeDescription) o;
      }
    } catch (Exception e) {
      throw new InvalidXMLException(e);
    }

    throw new InvalidXMLException(new Exception("Unexpected Object Type Produced By the XMLParser"));
  }

  /**
   * Parse a CpeDescription from a given input stream.
   * 
   * @param aInput
   *          identifies the input stream
   * 
   * @return The CpeDescription object parsed from the input
   * 
   * @throws InvalidXMLException
   *           if the descriptor is invalid
   */
  public static CpeDescription produceDescriptor(InputStream aInput) throws InvalidXMLException {
    return produceDescriptor(new XMLInputSource(aInput, null));
  }

  /**
   * 
   * @param aCollectionReaderDescriptorPath a path to the collection reader descriptor
   * @param aDescriptor the descriptor to associate the collection reader with
   * @return the CPE Collection Reader
   * @throws CpeDescriptorException if there is a failure
   */
  public static CpeCollectionReader produceCollectionReader(String aCollectionReaderDescriptorPath,
          CpeDescription aDescriptor) throws CpeDescriptorException {
    CpeCollectionReader[] colR = null;
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }

    if ((colR = aDescriptor.getAllCollectionCollectionReaders()).length == 0) {
      colR = new CpeCollectionReader[1];
      colR[0] = produceCollectionReader(aCollectionReaderDescriptorPath);
    }
    aDescriptor.addCollectionReader(colR[0]);
    return colR[0];
  }

  public static CpeCollectionReader produceCollectionReader(String aCollectionReaderDescriptorPath)
          throws CpeDescriptorException {
    CpeCollectionReader colR = produceCollectionReader();
    colR.getCollectionIterator().getDescriptor().getInclude().set(aCollectionReaderDescriptorPath);
    return colR;
  }

  public static CpeCollectionReader produceCollectionReader() throws CpeDescriptorException {
    CpeCollectionReader colR = new CpeCollectionReaderImpl();
    colR.setCollectionIterator(produceCollectionReaderIterator(""));
    return colR;
  }

  public static CpeCollectionReaderIterator produceCollectionReaderIterator(String aPath)
          throws CpeDescriptorException {
    CpeCollectionReaderIterator iterator = new CpeCollectionReaderIteratorImpl();
    iterator.setDescriptor(produceComponentDescriptor(aPath));
    return iterator;
  }

  /**
   * 
   * @param aPath don't use
   * @param aDescriptor don't use 
   * @return a CPE Collection Reader CAS Initializer 
   * @throws CpeDescriptorException passed thru
   * 
   * @deprecated As of v2.0, CAS Initializers are deprecated.
   */
  @Deprecated
public static CpeCollectionReaderCasInitializer produceCollectionReaderCasInitializer(
          String aPath, CpeDescription aDescriptor) throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeCollectionReaderCasInitializer initializer = new CpeCollectionReaderCasInitializerImpl();
    initializer.setDescriptor(produceComponentDescriptor(aPath));
    return initializer;
  }

  /**
   * 
   * @param aInitializerDescriptorPath path to the initializer descriptor
   * @return CPE Collection Reader CAS Initializer
   * @deprecated As of v2.0, CAS Initializers are deprecated.
   */
  @Deprecated
protected static CpeCollectionReaderCasInitializer produceCollectionReaderCasInitializer(
          String aInitializerDescriptorPath) {
    try {
      return produceCollectionReaderCasInitializer(aInitializerDescriptorPath, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 
   * @param aPath The path to the the CPE component Descriptor
   * @return the CPE Component Description
   */
  public static CpeComponentDescriptor produceComponentDescriptor(String aPath) {

    CpeComponentDescriptor componentDescriptor = new CpeComponentDescriptorImpl();
    CpeInclude include = new CpeIncludeImpl();
    include.set(aPath);
    componentDescriptor.setInclude(include);
    return componentDescriptor;
  }

  /**
   * 
   * @param aDescriptor CPE descriptor to use 
   * @return the Cpe Configuration
   * @throws CpeDescriptorException if it fails
   */
  public static CpeConfiguration produceCpeConfiguration(CpeDescription aDescriptor)
          throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeConfiguration config = new CpeConfigurationImpl();
    aDescriptor.setCpeConfiguration(config);
    return config;
  }

  public static CpeConfiguration produceCpeConfiguration() throws CpeDescriptorException {
    return new CpeConfigurationImpl();
  }

  public static CasProcessorRuntimeEnvParam produceRuntimeEnvParam() {

    return new CasProcessorRuntimeEnvParamImpl();
  }

  public static CasProcessorDeploymentParams produceDeployParams() {
    return new CasProcessorDeploymentParamsImpl();
  }

  public static CasProcessorDeploymentParam produceDeployParam() {
    return new CasProcessorDeploymentParamImpl();
  }

  public static CpeInclude produceComponentDescriptorInclude() {
    return new CpeIncludeImpl();
  }

  /**
   * 
   * @param aDescriptor to use to produce the CPE CAS Processors
   * @return Cpe CAS Processors
   * @throws CpeDescriptorException if an error occurs
   */
  public static CpeCasProcessors produceCasProcessors(CpeDescription aDescriptor)
          throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeCasProcessors processors = new CpeCasProcessorsImpl();
    aDescriptor.setCpeCasProcessors(processors);
    return processors;
  }

  public static CpeCasProcessors produceCasProcessors() throws CpeDescriptorException {
    return new CpeCasProcessorsImpl();
  }

  /**
   * 
   * @param aInputQSize the input queue size
   * @param aOutputQSize the output queue size
   * @param aPuCount the number of processing units
   * @param aDescriptor the CPE descriptor
   * @return CPE CAS Processors  
   * @throws CpeDescriptorException if an error occurs
   */
  public static CpeCasProcessors produceCasProcessors(int aInputQSize, int aOutputQSize,
          int aPuCount, CpeDescription aDescriptor) throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeCasProcessors processors = new CpeCasProcessorsImpl();
    processors.setConcurrentPUCount(aPuCount);
    processors.setInputQueueSize(aInputQSize);
    processors.setOutputQueueSize(aOutputQSize);
    aDescriptor.setCpeCasProcessors(processors);
    return processors;
  }

  // Default deployment=integrated
  public static CpeIntegratedCasProcessor produceCasProcessor(String aName) {
    CpeIntegratedCasProcessor processor = new CpeIntegratedCasProcessorImpl();
    try {
      processor.setName(aName);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return processor;
  }

  /**
   * 
   * @param aName the processor name
   * @param aSoFa the processor SofA
   * @return CPE Local CAS Processor
   * @throws CpeDescriptorException if an error occurs
   */
  public static CpeLocalCasProcessor produceLocalCasProcessor(String aName, String aSoFa)
          throws CpeDescriptorException {
    CpeLocalCasProcessor processor = new CpeLocalCasProcessorImpl();
    processor.setName(aName);
    processor.setSOFA(aSoFa);
    return processor;
  }

  /**
   * 
   * @param aName the processor name
   * @return CPE Remote CAS Processor 
   * @throws CpeDescriptorException if an error occurs
   */
  public static CpeRemoteCasProcessor produceRemoteCasProcessor(String aName)
          throws CpeDescriptorException {
    CpeRemoteCasProcessor processor = new CpeRemoteCasProcessorImpl();
    processor.setName(aName);
    return processor;
  }

  public static CpeTimer produceCpeTimer(String aTimerClass) {
    return new CpeTimerImpl(aTimerClass);
  }

  public static CpeResourceManagerConfiguration produceResourceManagerConfiguration(
          String aResourceMgrConfigurationPath, CpeDescription aDescriptor)
          throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeResourceManagerConfiguration resMgr = produceResourceManagerConfiguration(aResourceMgrConfigurationPath);
    aDescriptor.setCpeResourceManagerConfiguration(resMgr);
    return resMgr;
  }

  public static CpeResourceManagerConfiguration produceResourceManagerConfiguration(
          String aResourceMgrConfigurationPath) throws CpeDescriptorException {
    CpeResourceManagerConfiguration resMgr = new CpeResourceManagerConfigurationImpl();
    resMgr.set(aResourceMgrConfigurationPath);
    return resMgr;
  }

  public static CasProcessorTimeout produceCasProcessorTimeout() {
    return new CasProcessorTimeoutImpl();
  }

  public static CasProcessorMaxRestarts produceCasProcessorMaxRestarts() {
    return new CasProcessorMaxRestartsImpl();
  }

  public static CasProcessorErrorRateThreshold produceCasProcessorErrorRateThreshold() {
    return new CasProcessorErrorRateThresholdImpl();
  }

  /**
   * 
   * @param aFilter the filter string
   * @return a CAS Processor Filter
   */
  public static CasProcessorFilter produceCasProcessorFilter(String aFilter) {
    CasProcessorFilter filter = new CasProcessorFilterImpl();
    filter.setFilterString(aFilter);
    return filter;
  }

  public static CasProcessorErrorHandling produceCasProcessorErrorHandling() {
    return new CasProcessorErrorHandlingImpl();
  }

  public static CpeCheckpoint produceCpeCheckpoint() {
    return new CpeCheckpointImpl();
  }

  public static CasProcessorDeploymentParams produceCasProcessorDeploymentParams() {
    return new CasProcessorDeploymentParamsImpl();
  }

  public static CasProcessorExecArg produceCasProcessorExecArg() {
    return new CasProcessorExecArgImpl();
  }
 
  public static CasProcessorExecutable produceCasProcessorExecutable() {
    return new CasProcessorExecutableImpl();
  }

  public static CasProcessorRunInSeperateProcess produceRunInSeperateProcess() {
    return new CasProcessorRunInSeperateProcessImpl();
  }

  public static CasProcessorConfigurationParameterSettings produceCasProcessorConfigurationParameterSettings() {
    return new CasProcessorConfigurationParameterSettingsImpl();
  }

  public static NameValuePair produceNameValuePair() {
    return new NameValuePairImpl();
  }

  public static CpeSofaMapping produceSofaMapping() {
    return new CpeSofaMappingImpl();
  }
  
  public static CpeSofaMappings produceSofaMappings() {
    return new CpeSofaMappingsImpl();
  }
}
