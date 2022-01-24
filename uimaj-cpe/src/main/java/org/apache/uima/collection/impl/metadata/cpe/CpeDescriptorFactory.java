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

  /**
   * Instantiates a new cpe descriptor factory.
   */
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

    throw new InvalidXMLException(
            new Exception("Unexpected Object Type Produced By the XMLParser"));
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
   * Produce collection reader.
   *
   * @param aCollectionReaderDescriptorPath
   *          a path to the collection reader descriptor
   * @param aDescriptor
   *          the descriptor to associate the collection reader with
   * @return the CPE Collection Reader
   * @throws CpeDescriptorException
   *           if there is a failure
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

  /**
   * Produce collection reader.
   *
   * @param aCollectionReaderDescriptorPath
   *          the a collection reader descriptor path
   * @return the cpe collection reader
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeCollectionReader produceCollectionReader(String aCollectionReaderDescriptorPath)
          throws CpeDescriptorException {
    CpeCollectionReader colR = produceCollectionReader();
    colR.getCollectionIterator().getDescriptor().getInclude().set(aCollectionReaderDescriptorPath);
    return colR;
  }

  /**
   * Produce collection reader.
   *
   * @return the cpe collection reader
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeCollectionReader produceCollectionReader() throws CpeDescriptorException {
    CpeCollectionReader colR = new CpeCollectionReaderImpl();
    colR.setCollectionIterator(produceCollectionReaderIterator(""));
    return colR;
  }

  /**
   * Produce collection reader iterator.
   *
   * @param aPath
   *          the a path
   * @return the cpe collection reader iterator
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeCollectionReaderIterator produceCollectionReaderIterator(String aPath)
          throws CpeDescriptorException {
    CpeCollectionReaderIterator iterator = new CpeCollectionReaderIteratorImpl();
    iterator.setDescriptor(produceComponentDescriptor(aPath));
    return iterator;
  }

  /**
   * Produce collection reader cas initializer.
   *
   * @param aPath
   *          don't use
   * @param aDescriptor
   *          don't use
   * @return a CPE Collection Reader CAS Initializer
   * @throws CpeDescriptorException
   *           passed thru
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
   * Produce collection reader cas initializer.
   *
   * @param aInitializerDescriptorPath
   *          path to the initializer descriptor
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
   * Produce component descriptor.
   *
   * @param aPath
   *          The path to the the CPE component Descriptor
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
   * Produce cpe configuration.
   *
   * @param aDescriptor
   *          CPE descriptor to use
   * @return the Cpe Configuration
   * @throws CpeDescriptorException
   *           if it fails
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

  /**
   * Produce cpe configuration.
   *
   * @return the cpe configuration
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeConfiguration produceCpeConfiguration() throws CpeDescriptorException {
    return new CpeConfigurationImpl();
  }

  /**
   * Produce runtime env param.
   *
   * @return the cas processor runtime env param
   */
  public static CasProcessorRuntimeEnvParam produceRuntimeEnvParam() {

    return new CasProcessorRuntimeEnvParamImpl();
  }

  /**
   * Produce deploy params.
   *
   * @return the cas processor deployment params
   */
  public static CasProcessorDeploymentParams produceDeployParams() {
    return new CasProcessorDeploymentParamsImpl();
  }

  /**
   * Produce deploy param.
   *
   * @return the cas processor deployment param
   */
  public static CasProcessorDeploymentParam produceDeployParam() {
    return new CasProcessorDeploymentParamImpl();
  }

  /**
   * Produce component descriptor include.
   *
   * @return the cpe include
   */
  public static CpeInclude produceComponentDescriptorInclude() {
    return new CpeIncludeImpl();
  }

  /**
   * Produce cas processors.
   *
   * @param aDescriptor
   *          to use to produce the CPE CAS Processors
   * @return Cpe CAS Processors
   * @throws CpeDescriptorException
   *           if an error occurs
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

  /**
   * Produce cas processors.
   *
   * @return the cpe cas processors
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeCasProcessors produceCasProcessors() throws CpeDescriptorException {
    return new CpeCasProcessorsImpl();
  }

  /**
   * Produce cas processors.
   *
   * @param aInputQSize
   *          the input queue size
   * @param aOutputQSize
   *          the output queue size
   * @param aPuCount
   *          the number of processing units
   * @param aDescriptor
   *          the CPE descriptor
   * @return CPE CAS Processors
   * @throws CpeDescriptorException
   *           if an error occurs
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

  /**
   * Produce cas processor.
   *
   * @param aName
   *          the a name
   * @return the cpe integrated cas processor
   */
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
   * Produce local cas processor.
   *
   * @param aName
   *          the processor name
   * @param aSoFa
   *          the processor SofA
   * @return CPE Local CAS Processor
   * @throws CpeDescriptorException
   *           if an error occurs
   */
  public static CpeLocalCasProcessor produceLocalCasProcessor(String aName, String aSoFa)
          throws CpeDescriptorException {
    CpeLocalCasProcessor processor = new CpeLocalCasProcessorImpl();
    processor.setName(aName);
    processor.setSOFA(aSoFa);
    return processor;
  }

  /**
   * Produce remote cas processor.
   *
   * @param aName
   *          the processor name
   * @return CPE Remote CAS Processor
   * @throws CpeDescriptorException
   *           if an error occurs
   */
  public static CpeRemoteCasProcessor produceRemoteCasProcessor(String aName)
          throws CpeDescriptorException {
    CpeRemoteCasProcessor processor = new CpeRemoteCasProcessorImpl();
    processor.setName(aName);
    return processor;
  }

  /**
   * Produce cpe timer.
   *
   * @param aTimerClass
   *          the a timer class
   * @return the cpe timer
   */
  public static CpeTimer produceCpeTimer(String aTimerClass) {
    return new CpeTimerImpl(aTimerClass);
  }

  /**
   * Produce resource manager configuration.
   *
   * @param aResourceMgrConfigurationPath
   *          the a resource mgr configuration path
   * @param aDescriptor
   *          the a descriptor
   * @return the cpe resource manager configuration
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeResourceManagerConfiguration produceResourceManagerConfiguration(
          String aResourceMgrConfigurationPath, CpeDescription aDescriptor)
          throws CpeDescriptorException {
    if (aDescriptor == null) {
      aDescriptor = produceDescriptor();
    }
    CpeResourceManagerConfiguration resMgr = produceResourceManagerConfiguration(
            aResourceMgrConfigurationPath);
    aDescriptor.setCpeResourceManagerConfiguration(resMgr);
    return resMgr;
  }

  /**
   * Produce resource manager configuration.
   *
   * @param aResourceMgrConfigurationPath
   *          the a resource mgr configuration path
   * @return the cpe resource manager configuration
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public static CpeResourceManagerConfiguration produceResourceManagerConfiguration(
          String aResourceMgrConfigurationPath) throws CpeDescriptorException {
    CpeResourceManagerConfiguration resMgr = new CpeResourceManagerConfigurationImpl();
    resMgr.set(aResourceMgrConfigurationPath);
    return resMgr;
  }

  /**
   * Produce cas processor timeout.
   *
   * @return the cas processor timeout
   */
  public static CasProcessorTimeout produceCasProcessorTimeout() {
    return new CasProcessorTimeoutImpl();
  }

  /**
   * Produce cas processor max restarts.
   *
   * @return the cas processor max restarts
   */
  public static CasProcessorMaxRestarts produceCasProcessorMaxRestarts() {
    return new CasProcessorMaxRestartsImpl();
  }

  /**
   * Produce cas processor error rate threshold.
   *
   * @return the cas processor error rate threshold
   */
  public static CasProcessorErrorRateThreshold produceCasProcessorErrorRateThreshold() {
    return new CasProcessorErrorRateThresholdImpl();
  }

  /**
   * Produce cas processor filter.
   *
   * @param aFilter
   *          the filter string
   * @return a CAS Processor Filter
   */
  public static CasProcessorFilter produceCasProcessorFilter(String aFilter) {
    CasProcessorFilter filter = new CasProcessorFilterImpl();
    filter.setFilterString(aFilter);
    return filter;
  }

  /**
   * Produce cas processor error handling.
   *
   * @return the cas processor error handling
   */
  public static CasProcessorErrorHandling produceCasProcessorErrorHandling() {
    return new CasProcessorErrorHandlingImpl();
  }

  /**
   * Produce cpe checkpoint.
   *
   * @return the cpe checkpoint
   */
  public static CpeCheckpoint produceCpeCheckpoint() {
    return new CpeCheckpointImpl();
  }

  /**
   * Produce cas processor deployment params.
   *
   * @return the cas processor deployment params
   */
  public static CasProcessorDeploymentParams produceCasProcessorDeploymentParams() {
    return new CasProcessorDeploymentParamsImpl();
  }

  /**
   * Produce cas processor exec arg.
   *
   * @return the cas processor exec arg
   */
  public static CasProcessorExecArg produceCasProcessorExecArg() {
    return new CasProcessorExecArgImpl();
  }

  /**
   * Produce cas processor executable.
   *
   * @return the cas processor executable
   */
  public static CasProcessorExecutable produceCasProcessorExecutable() {
    return new CasProcessorExecutableImpl();
  }

  /**
   * Produce run in seperate process.
   *
   * @return the cas processor run in seperate process
   */
  public static CasProcessorRunInSeperateProcess produceRunInSeperateProcess() {
    return new CasProcessorRunInSeperateProcessImpl();
  }

  /**
   * Produce cas processor configuration parameter settings.
   *
   * @return the cas processor configuration parameter settings
   */
  public static CasProcessorConfigurationParameterSettings produceCasProcessorConfigurationParameterSettings() {
    return new CasProcessorConfigurationParameterSettingsImpl();
  }

  /**
   * Produce name value pair.
   *
   * @return the name value pair
   */
  public static NameValuePair produceNameValuePair() {
    return new NameValuePairImpl();
  }

  /**
   * Produce sofa mapping.
   *
   * @return the cpe sofa mapping
   */
  public static CpeSofaMapping produceSofaMapping() {
    return new CpeSofaMappingImpl();
  }

  /**
   * Produce sofa mappings.
   *
   * @return the cpe sofa mappings
   */
  public static CpeSofaMappings produceSofaMappings() {
    return new CpeSofaMappingsImpl();
  }
}
