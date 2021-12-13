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
package org.apache.uima;

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
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecutable;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeInclude;
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
 * A factory used to create {@link org.apache.uima.resource.ResourceSpecifier} instances and
 * instances of other objects needed to compose <code>ResourceSpecifier</code>s.
 * <p>
 * The primary method on this class is {@link #createObject(Class)}. Given the <code>Class</code> of
 * a UIMA interface related to Resource Specifiers, this method will construct an instance that
 * implements that interface. Other methods are provided as a convenience for creating specific
 * types of objects.
 * <p>
 * A UIMA developer who implements a new type of Resource Specifier must register their
 * implementation with this factory using the {@link #addMapping(String, String)} method.
 */
public interface ResourceSpecifierFactory {
  /**
   * Creates an object that implements the given interface.
   * 
   * @param aInterface
   *          the <code>Class</code> object representing the type of interface to be instantiated.
   * 
   * @return an <code>Object</code> that implements <code>aInterface</code>. Returns
   *         <code>null</code> if no object that implements <code>aInterface</code> is known to this
   *         factory.
   */
  Object createObject(Class aInterface);

  /**
   * Adds a mapping from interface class to implementation class. Applications do not typically need
   * to use this method. UIMA developers who implement new types of <code>ResourceSpecifier</code>,
   * however, must register their implementations using this method so that this factory knows how
   * to construct instances of those implementation classes.
   * 
   * @param aInterfaceName
   *          the fully-qualified name of a UIMA interface
   * @param aClassName
   *          the fully-qualified name of a class that implements <code>aInterfaceName</code>.
   * 
   * @throws ClassNotFoundException
   *           if either of the classes named by <code>aInterfaceName</code> or
   *           <code>aClassName</code> were not found.
   */
  void addMapping(String aInterfaceName, String aClassName) throws ClassNotFoundException;

  /**
   * Creates a <code>URISpecifier</code>.
   * 
   * @return an instance of an object implementing <code>URISpecifier</code>.
   */
  URISpecifier createURISpecifier();

  /**
   * Creates a <code>MQMessagingSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>MQMessagingSpecifier</code>.
   */
  MQMessagingSpecifier createMQMessagingSpecifier();

  /**
   * Creates a <code>JMSMessagingSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>JMSMessagingSpecifier</code>.
   */
  JMSMessagingSpecifier createJMSMessagingSpecifier();

  /**
   * Creates a <code>MailMessagingSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>MailMessagingSpecifier</code>.
   */
  MailMessagingSpecifier createMailMessagingSpecifier();

  /**
   * Creates a <code>FileResourceSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>FileResourceSpecifier</code>.
   */
  FileResourceSpecifier createFileResourceSpecifier();

  /**
   * Creates a <code>FileLanguageResourceSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>FileLanguageResourceSpecifier</code>.
   */
  FileLanguageResourceSpecifier createFileLanguageResourceSpecifier();

  /**
   * Creates a <code>AnalysisEngineDescription</code>.
   * 
   * @return an instance of an object implementing <code>AnalysisEngineDescription</code>.
   */
  AnalysisEngineDescription createAnalysisEngineDescription();

  /**
   * Creates a <code>AnalysisEngineDescription</code>.
   * 
   * @return an instance of an object implementing <code>AnalysisEngineDescription</code>.
   * 
   * @deprecated As of v2.0, {@link #createAnalysisEngineDescription()} should be used instead.
   */
  @Deprecated
  TaeDescription createTaeDescription();

  /**
   * Creates a <code>ResourceMetaData</code>.
   * 
   * @return an instance of an object implementing <code>ResourceMetaData</code>.
   */
  ResourceMetaData createResourceMetaData();

  /**
   * Creates a <code>ProcessingResourceMetaData</code>.
   * 
   * @return an instance of an object implementing <code>ProcessingResourceMetaData</code>.
   */
  ProcessingResourceMetaData createProcessingResourceMetaData();

  /**
   * Creates a <code>AnalysisEngineMetaData</code>.
   * 
   * @return an instance of an object implementing <code>AnalysisEngineMetaData</code>.
   */
  AnalysisEngineMetaData createAnalysisEngineMetaData();

  /**
   * Creates a <code>ConfigurationParameterDeclarations</code>.
   * 
   * @return an instance of an object implementing <code>ConfigurationParameterDeclarations</code>.
   */
  ConfigurationParameterDeclarations createConfigurationParameterDeclarations();

  /**
   * Creates a <code>ConfigurationParameter</code>.
   * 
   * @return an instance of an object implementing <code>ConfigurationParameter</code>.
   */
  ConfigurationParameter createConfigurationParameter();

  /**
   * Creates a <code>ConfigurationGroup</code>.
   * 
   * @return an instance of an object implementing <code>ConfigurationGroup</code>.
   */
  ConfigurationGroup createConfigurationGroup();

  /**
   * Creates a <code>ConfigurationParameterSettings</code>.
   * 
   * @return an instance of an object implementing <code>ConfigurationParameterSettings</code>.
   */
  ConfigurationParameterSettings createConfigurationParameterSettings();

  /**
   * Creates an empty <code>Settings</code> for External Override parameters.
   * 
   * @return an instance of an object implementing <code>Settings</code>.
   */
  Settings createSettings();

  /**
   * Creates a <code>Capability</code>.
   * 
   * @return an instance of an object implementing <code>Capability</code>.
   */
  Capability createCapability();

  /**
   * Creates a <code>SimplePrecondition</code>.
   * 
   * @return an instance of an object implementing <code>SimplePrecondition</code>.
   */
  SimplePrecondition createSimplePrecondition();

  /**
   * Creates a <code>TypeSystemDescription</code>.
   * 
   * @return an instance of an object implementing <code>TypeSystemDescription</code>.
   */
  TypeSystemDescription createTypeSystemDescription();

  /**
   * Creates a <code>TypeDescription</code>.
   * 
   * @return an instance of an object implementing <code>TypeDescription</code>.
   */
  TypeDescription createTypeDescription();

  /**
   * Creates a <code>FeatureDescription</code>.
   * 
   * @return an instance of an object implementing <code>FeatureDescription</code>.
   */
  FeatureDescription createFeatureDescription();

  /**
   * Creates an <code>FsIndexCollection</code>.
   * 
   * @return an instance of an object implementing <code>FsIndexCollection</code>.
   */
  FsIndexCollection createFsIndexCollection();

  /**
   * Creates an <code>FsIndexDescription</code>.
   * 
   * @return an instance of an object implementing <code>FsIndexDescription</code>.
   */
  FsIndexDescription createFsIndexDescription();

  /**
   * Creates an <code>FsIndexKeyDescription</code>.
   * 
   * @return an instance of an object implementing <code>FsIndexKeyDescription</code>.
   */
  FsIndexKeyDescription createFsIndexKeyDescription();

  /**
   * Creates a <code>FixedFlow</code>.
   * 
   * @return an instance of an object implementing <code>FixedFlow</code>.
   */
  FixedFlow createFixedFlow();

  /**
   * Creates a <code>CapabilityLanguageFlow</code>.
   * 
   * @return an instance of an object implementing <code>CapabilityLanguageFlow</code>.
   */
  CapabilityLanguageFlow createCapabilityLanguageFlow();

  /**
   * Creates a <code>NameValuePair</code>.
   * 
   * @return an instance of an object implementing <code>NameValuePair</code>.
   */
  NameValuePair createNameValuePair();

  /**
   * Creates a <code>TypeOrFeature</code>.
   * 
   * @return an instance of an object implementing <code>TypeOrFeature</code>.
   */
  TypeOrFeature createTypeOrFeature();

  /**
   * Creates an <code>AllowedValue</code>.
   * 
   * @return an instance of an object implementing <code>AllowedValue</code>.
   */
  AllowedValue createAllowedValue();

  /**
   * Creates an <code>TypePriorities</code>.
   * 
   * @return an instance of an object implementing <code>TypePriorities</code>.
   */
  TypePriorities createTypePriorities();

  /**
   * Creates an <code>TypePriorityList</code>.
   * 
   * @return an instance of an object implementing <code>TypePriorityList</code>.
   */
  TypePriorityList createTypePriorityList();

  /**
   * Creates an <code>ExternalResourceDependency</code>.
   * 
   * @return an instance of an object implementing <code>ExternalResourceDependency</code>.
   */
  ExternalResourceDependency createExternalResourceDependency();

  /**
   * Creates an <code>ResourceManagerConfiguration</code>.
   * 
   * @return an instance of an object implementing <code>ResourceManagerConfiguration</code>.
   */
  ResourceManagerConfiguration createResourceManagerConfiguration();

  /**
   * Creates an <code>ExternalResourceBinding</code>.
   * 
   * @return an instance of an object implementing <code>ExternalResourceBinding</code>.
   */
  ExternalResourceBinding createExternalResourceBinding();

  /**
   * Creates an <code>ExternalResourceDescription</code>.
   * 
   * @return an instance of an object implementing <code>ExternalResourceDescription</code>.
   */
  ExternalResourceDescription createExternalResourceDescription();

  /**
   * Creates a <code>CasConsumerDescription</code>.
   * 
   * @return an instance of an object implementing <code>CasConsumerDescription</code>.
   */
  CasConsumerDescription createCasConsumerDescription();

  /**
   * Creates a <code>CollectionReaderDescription</code>.
   * 
   * @return an instance of an object implementing <code>CollectionReaderDescription</code>.
   */
  CollectionReaderDescription createCollectionReaderDescription();

  /**
   * Creates a <code>ResultSpecification</code>.
   * 
   * @return an instance of an object implementing <code>ResultSpecification</code>.
   */
  ResultSpecification createResultSpecification();

  /**
   * Creates a <code>SofaMapping</code>.
   * 
   * @return an instance of an object implementing <code>SofaMapping</code>.
   */
  SofaMapping createSofaMapping();

  /**
   * Creates an <code>Import</code>
   * 
   * @return an instance of an object implementing <code>Import</code>.
   */
  Import createImport();

  /**
   * Creates an <code>OperationalProperties</code>
   * 
   * @return an instance of an object implementing <code>OperationalProperties</code>.
   */
  OperationalProperties createOperationalProperties();

  /**
   * Creates a <code>Parameter</code>
   * 
   * @return an instance of an object implementing <code>Parameter</code>.
   */
  Parameter createParameter();

  /**
   * Creates a <code>FlowControllerDeclaration</code>
   * 
   * @return an instance of an object implementing <code>FlowControllerDeclaration</code>.
   */
  FlowControllerDeclaration createFlowControllerDeclaration();

  /**
   * Creates a <code>CustomResourceSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>CustomResourceSpecifier</code>.
   */
  CustomResourceSpecifier createCustomResourceSpecifier();

  /**
   * Creates a <code>PearSpecifier</code>.
   * 
   * @return an instance of an object implementing <code>PearSpecifier</code>.
   */
  PearSpecifier createPearSpecifier();

  /**
   * Creates a <code>FlowControllerDescription</code>
   * 
   * @return an instance of an object implementing <code>FlowControllerDescription</code>.
   */
  FlowControllerDescription createFlowControllerDescription();

  CpeCollectionReaderCasInitializer createCasInitializer();

  CpeCasProcessors createCasProcessors();

  CpeCheckpoint createCheckpoint();

  CpeCollectionReaderIterator createCollectionIterator();

  CpeCollectionReader createCollectionReader();

  CpeConfiguration createCpeConfig();

  CpeDescription createCpeDescription();

  CpeComponentDescriptor createDescriptor();

  CasProcessorErrorHandling createErrorHandling();

  CpeInclude createInclude();

  CasProcessorRunInSeperateProcess createRunInSeperateProcess();

  CasProcessorDeploymentParams createDeploymentParameters();

  CasProcessorExecutable createExec();

  CasProcessorExecArg createArg();

  OutputQueue createOutputQueue();

  CasProcessorRuntimeEnvParam createEnv();
}
