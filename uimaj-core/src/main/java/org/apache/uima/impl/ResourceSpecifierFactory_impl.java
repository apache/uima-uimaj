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

package org.apache.uima.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecutable;
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
import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.collection.metadata.OutputQueue;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.JMSMessagingSpecifier;
import org.apache.uima.resource.MQMessagingSpecifier;
import org.apache.uima.resource.MailMessagingSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.SimplePrecondition;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Settings;

/**
 * Reference implementation of {@link ResourceSpecifierFactory}. Must be threadsafe.
 * 
 * 
 */
public class ResourceSpecifierFactory_impl implements ResourceSpecifierFactory {

  /**
   * Map from standard UIMA interface (Class object) to the class providing the implementation.
   */
  Map mInterfaceToClassMap = Collections.synchronizedMap(new HashMap());

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createObject(Class)
   */
  public Object createObject(Class aInterface) {
    try {
      Class implClass = (Class) mInterfaceToClassMap.get(aInterface);
      if (implClass != null) {
        return implClass.newInstance();
      } else {
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#addMapping(String, String)
   */
  public void addMapping(String aInterfaceName, String aClassName) throws ClassNotFoundException {
    // resolve the interface name
    Class intrfc = Class.forName(aInterfaceName);
    // resolve the class name
    Class cls = Class.forName(aClassName);
    // add to the map
    mInterfaceToClassMap.put(intrfc, cls);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createURISpecifier()
   */
  public URISpecifier createURISpecifier() {
    return (URISpecifier) createObject(URISpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createJMSMessagingSpecifier()
   */
  public JMSMessagingSpecifier createJMSMessagingSpecifier() {
    return (JMSMessagingSpecifier) createObject(JMSMessagingSpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createMailMessagingSpecifier()
   */
  public MailMessagingSpecifier createMailMessagingSpecifier() {
    return (MailMessagingSpecifier) createObject(MailMessagingSpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createMQMessagingSpecifier()
   */
  public MQMessagingSpecifier createMQMessagingSpecifier() {
    return (MQMessagingSpecifier) createObject(MQMessagingSpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFileResourceSpecifier()
   */
  public FileResourceSpecifier createFileResourceSpecifier() {
    return (FileResourceSpecifier) createObject(FileResourceSpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFileLanguageResourceSpecifier()
   */
  public FileLanguageResourceSpecifier createFileLanguageResourceSpecifier() {
    return (FileLanguageResourceSpecifier) createObject(FileLanguageResourceSpecifier.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createAnalysisEngineDescription()
   */
  public AnalysisEngineDescription createAnalysisEngineDescription() {
    return (AnalysisEngineDescription) createObject(AnalysisEngineDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTaeDescription()
   * @deprecated since v2.0
   */
  @Deprecated
  public TaeDescription createTaeDescription() {
    return (TaeDescription) createObject(TaeDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createConfigurationParameter()
   */
  public ConfigurationParameter createConfigurationParameter() {
    return (ConfigurationParameter) createObject(ConfigurationParameter.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createCapability()
   */
  public Capability createCapability() {
    return (Capability) createObject(Capability.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createSimplePrecondition()
   */
  public SimplePrecondition createSimplePrecondition() {
    return (SimplePrecondition) createObject(SimplePrecondition.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTypeSystemDescription()
   */
  public TypeSystemDescription createTypeSystemDescription() {
    return (TypeSystemDescription) createObject(TypeSystemDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTypeDescription()
   */
  public TypeDescription createTypeDescription() {
    return (TypeDescription) createObject(TypeDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFeatureDescription()
   */
  public FeatureDescription createFeatureDescription() {
    return (FeatureDescription) createObject(FeatureDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFsIndexCollection()
   */
  public FsIndexCollection createFsIndexCollection() {
    return (FsIndexCollection) createObject(FsIndexCollection.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFsIndexDescription()
   */
  public FsIndexDescription createFsIndexDescription() {
    return (FsIndexDescription) createObject(FsIndexDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFsIndexKeyDescription()
   */
  public FsIndexKeyDescription createFsIndexKeyDescription() {
    return (FsIndexKeyDescription) createObject(FsIndexKeyDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createFixedFlow()
   */
  public FixedFlow createFixedFlow() {
    return (FixedFlow) createObject(FixedFlow.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createCapabilityLanguageFlow()
   */
  public CapabilityLanguageFlow createCapabilityLanguageFlow() {
    return (CapabilityLanguageFlow) createObject(CapabilityLanguageFlow.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createNameValuePair()
   */
  public NameValuePair createNameValuePair() {
    return (NameValuePair) createObject(NameValuePair.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTypeOrFeature()
   */
  public TypeOrFeature createTypeOrFeature() {
    return (TypeOrFeature) createObject(TypeOrFeature.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createAllowedValue()
   */
  public AllowedValue createAllowedValue() {
    return (AllowedValue) createObject(AllowedValue.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createConfigurationGroup()
   */
  public ConfigurationGroup createConfigurationGroup() {
    return (ConfigurationGroup) createObject(ConfigurationGroup.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createConfigurationParameterDeclarations()
   */
  public ConfigurationParameterDeclarations createConfigurationParameterDeclarations() {
    return (ConfigurationParameterDeclarations) createObject(ConfigurationParameterDeclarations.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createConfigurationParameterSettings()
   */
  public ConfigurationParameterSettings createConfigurationParameterSettings() {
    return (ConfigurationParameterSettings) createObject(ConfigurationParameterSettings.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createConfigurationParameterSettings()
   */
  public Settings createSettings() {
    return (Settings) createObject(Settings.class);
  }
  
  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTypePriorities()
   */
  public TypePriorities createTypePriorities() {
    return (TypePriorities) createObject(TypePriorities.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createTypePriorityList()
   */
  public TypePriorityList createTypePriorityList() {
    return (TypePriorityList) createObject(TypePriorityList.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createExternalResourceDependency()
   */
  public ExternalResourceDependency createExternalResourceDependency() {
    return (ExternalResourceDependency) createObject(ExternalResourceDependency.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createExternalResourceDescription()
   */
  public ExternalResourceDescription createExternalResourceDescription() {
    return (ExternalResourceDescription) createObject(ExternalResourceDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createCasConsumerDescription()
   */
  public CasConsumerDescription createCasConsumerDescription() {
    return (CasConsumerDescription) createObject(CasConsumerDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createCollectionReaderDescription()
   */
  public CollectionReaderDescription createCollectionReaderDescription() {
    return (CollectionReaderDescription) createObject(CollectionReaderDescription.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData createProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) createObject(ProcessingResourceMetaData.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createAnalysisEngineMetaData()
   */
  public AnalysisEngineMetaData createAnalysisEngineMetaData() {
    return (AnalysisEngineMetaData) createObject(AnalysisEngineMetaData.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createResourceMetaData()
   */
  public ResourceMetaData createResourceMetaData() {
    return (ResourceMetaData) createObject(ResourceMetaData.class);
  }

  /**
   * @see org.apache.uima.ResourceSpecifierFactory#createResultSpecification()
   */
  public ResultSpecification createResultSpecification() {
    return (ResultSpecification) createObject(ResultSpecification.class);
  }

  /**
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createSofaMapping()
   */
  public SofaMapping createSofaMapping() {
    return (SofaMapping) createObject(SofaMapping.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createExternalResourceBinding()
   */
  public ExternalResourceBinding createExternalResourceBinding() {
    return (ExternalResourceBinding) createObject(ExternalResourceBinding.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createResourceManagerConfiguration()
   */
  public ResourceManagerConfiguration createResourceManagerConfiguration() {
    return (ResourceManagerConfiguration) createObject(ResourceManagerConfiguration.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createImport()
   */
  public Import createImport() {
    return (Import) createObject(Import.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createOperationalProperties()
   */
  public OperationalProperties createOperationalProperties() {
    return (OperationalProperties) createObject(OperationalProperties.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.ResourceSpecifierFactory#createParameter()
   */
  public Parameter createParameter() {
    return (Parameter) createObject(Parameter.class);
  }

  public FlowControllerDeclaration createFlowControllerDeclaration() {
    return (FlowControllerDeclaration) createObject(FlowControllerDeclaration.class);
  }
  
  public FlowControllerDescription createFlowControllerDescription() {
    return (FlowControllerDescription) createObject(FlowControllerDescription.class);
  }
  
  public CustomResourceSpecifier createCustomResourceSpecifier() {
    return (CustomResourceSpecifier) createObject(CustomResourceSpecifier.class);
  }  

  public PearSpecifier createPearSpecifier() {
    return (PearSpecifier) createObject(PearSpecifier.class);
  }  

  public CpeCollectionReaderCasInitializer createCasInitializer() {
    return (CpeCollectionReaderCasInitializer) createObject(CpeCollectionReaderCasInitializer.class);
  }

  public CpeCasProcessors createCasProcessors() {
    return (CpeCasProcessors) createObject(CpeCasProcessors.class);
  }

  public CpeCheckpoint createCheckpoint() {
    return (CpeCheckpoint) createObject(CpeCheckpoint.class);
  }

  public CpeCollectionReaderIterator createCollectionIterator() {
    return (CpeCollectionReaderIterator) createObject(CpeCollectionReaderIterator.class);
  }

  public CpeCollectionReader createCollectionReader() {
    return (CpeCollectionReader) createObject(CpeCollectionReader.class);
  }

  public CpeConfiguration createCpeConfig() {
    return (CpeConfiguration) createObject(CpeConfiguration.class);
  }

  public CpeDescription createCpeDescription() {
    return (CpeDescription) createObject(CpeDescription.class);
  }

  public CpeComponentDescriptor createDescriptor() {
    return (CpeComponentDescriptor) createObject(CpeComponentDescriptor.class);
  }

  public CasProcessorErrorHandling createErrorHandling() {
    return (CasProcessorErrorHandling) createObject(CasProcessorErrorHandling.class);
  }

  public CpeInclude createInclude() {
    return (CpeInclude) createObject(CpeInclude.class);
  }

  public CasProcessorRunInSeperateProcess createRunInSeperateProcess() {
    return (CasProcessorRunInSeperateProcess) createObject(CasProcessorRunInSeperateProcess.class);
  }

  public CasProcessorDeploymentParams createDeploymentParameters() {
    return (CasProcessorDeploymentParams) createObject(CasProcessorDeploymentParams.class);

  }

  public CasProcessorExecutable createExec() {
    return (CasProcessorExecutable) createObject(CasProcessorExecutable.class);

  }

  public CasProcessorExecArg createArg() {
    return (CasProcessorExecArg) createObject(CasProcessorExecArg.class);

  }

  public OutputQueue createOutputQueue() {
    return (OutputQueue) createObject(OutputQueue.class);

  }

  public CasProcessorRuntimeEnvParam createEnv() {
    return (CasProcessorRuntimeEnvParam) createObject(CasProcessorRuntimeEnvParam.class);

  }

  public CasProcessorTimeout createCasProcessorTimeout() {
    return (CasProcessorTimeout) createObject(CasProcessorTimeout.class);

  }

  public CasProcessorErrorRateThreshold createCasProcessorErrorRateThreshold() {
    return (CasProcessorErrorRateThreshold) createObject(CasProcessorErrorRateThreshold.class);

  }

  public CasProcessorMaxRestarts createCasProcessorMaxRestarts() {
    return (CasProcessorMaxRestarts) createObject(CasProcessorMaxRestarts.class);

  }

  public CpeSofaMappings createCpeSofaMappings() {
    return (CpeSofaMappings) createObject(CpeSofaMappings.class);

  }

}
