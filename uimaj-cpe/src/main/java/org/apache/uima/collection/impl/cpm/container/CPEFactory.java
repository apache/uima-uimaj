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

package org.apache.uima.collection.impl.cpm.container;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.ConfigurationException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.CasDataCollectionReader;
import org.apache.uima.collection.base_cpm.CasDataConsumer;
import org.apache.uima.collection.base_cpm.CasDataInitializer;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.cpm.CPMException;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.deployer.DeployFactory;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptionImpl;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.impl.metadata.cpe.CpeIntegratedCasProcessorImpl;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeResourceManagerConfiguration;
import org.apache.uima.collection.metadata.CpeSofaMapping;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;

/**
 * Component responsible for generating objects representing cpe descriptor configuration. Provides
 * various ways to instantiate object model representing cpe configuration. In the simplest form it
 * ingests an xml file (cpe descriptor), parses it and creates an object for every element in the
 * xml file.
 * <p>
 * Using objects representing configuration, this component creates CollectionReader CasInitializer,
 * Analysis Engines, and Cas Consumers.
 * <p>
 * In addition to creating object, this component provides read/write access to the object model
 * allowing for dynamic or programmatic modifications. It facilitates plugging in existing
 * CollectionReaders and CasProcessors.
 * 
 * 
 */
public class CPEFactory {
  public static final String CPM_HOME = "${CPM_HOME}";

  private int processorCount = 0;

  private String DEFAULT_CONFIG_FILE = "defaultCpeDescriptor.xml";

  private boolean defaultConfig = true;

  public HashMap casProcessorConfigMap = new HashMap();

  private CpeDescription cpeDescriptor = null;

  private boolean initialized = false;

  private UimaContextAdmin uimaContext;

  private boolean firstTime = true;

  private HashMap cpMap = new HashMap();

  /**
   * Create a new CPEFactory on which we will later call parse(String) to parse a CPE descriptor.
   */
  public CPEFactory(ResourceManager aResourceManager) {
    if (aResourceManager == null) {
      aResourceManager = UIMAFramework.newDefaultResourceManager();
    }
    uimaContext = UIMAFramework.newUimaContext(UIMAFramework.getLogger(), aResourceManager,
            UIMAFramework.newConfigurationManager());
  }

  /**
   * Create a new CPEFactory for a CpeDescription that's already been parsed.
   * 
   * @param aDescriptor
   * @param aResourceManager
   *          the resource manager that all components of this CPE will share If null, a new
   *          ResourceManager will be created.
   */
  public CPEFactory(CpeDescription aDescriptor, ResourceManager aResourceManager)
          throws ResourceInitializationException {
    if (aDescriptor == null) {
      throw new UIMARuntimeException(new InvalidObjectException(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_no_cpe_descriptor__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }

    if (aResourceManager == null) {
      aResourceManager = UIMAFramework.newDefaultResourceManager();
    }
    setCpeDescriptor(aDescriptor);
    if (aDescriptor != null && aDescriptor instanceof CpeDescriptionImpl) {
      defaultConfig = false;
    }

    uimaContext = UIMAFramework.newUimaContext(UIMAFramework.getLogger(), aResourceManager,
            UIMAFramework.newConfigurationManager());
    // if CpeDescription contains a ResourceManagerConfiguration, parse it and use it
    // to configure the ResourceManager
    CpeResourceManagerConfiguration resMgrCfgDesc = aDescriptor.getResourceManagerConfiguration();
    if (resMgrCfgDesc != null) {
      ResourceManagerConfiguration resMgrCfg;
      try {
        if (resMgrCfgDesc.get().length() > 0) {
          String descriptorPath = CPMUtils.convertToAbsolutePath(System.getProperty("CPM_HOME"),
                  CPM_HOME, resMgrCfgDesc.get());
          resMgrCfg = UIMAFramework.getXMLParser().parseResourceManagerConfiguration(
                  new XMLInputSource(descriptorPath));
          aResourceManager.initializeExternalResources(resMgrCfg, "/", null);
        }
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      } catch (IOException e) {
        throw new ResourceInitializationException(e);
      }
    }
  }

  /**
   * Creates an object representation for configuration in a given cpe descriptor file.
   * 
   * @param aDescriptor -
   *          path to the descriptor
   * 
   * @throws InstantiationException -
   */
  public void parse(String aDescriptor) throws InstantiationException {
    defaultConfig = false;

    if (aDescriptor == null || aDescriptor.trim().length() == 0) {
      throw new UIMARuntimeException(new FileNotFoundException(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_no_cpe_descriptor__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }

    try {
      setCpeDescriptor(CpeDescriptorFactory.produceDescriptor(new FileInputStream(new File(
              aDescriptor))));

    } catch (Exception e) {
      throw new UIMARuntimeException(InvalidXMLException.INVALID_DESCRIPTOR_FILE,
              new Object[] { aDescriptor }, e);
    }
  }

  /**
   * Creates an object representation for configuration in a given stream
   * 
   * @param aDescriptorStream -
   *          stream containing cpe description
   * @throws InstantiationException -
   */
  public void parse(InputStream aDescriptorStream) throws InstantiationException {
    defaultConfig = false;

    if (aDescriptorStream == null) {
      throw new UIMARuntimeException(new IOException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_invalid_cpe_descriptor_stream__WARNING", new Object[] { Thread
                      .currentThread().getName() })));
    }

    try {
      setCpeDescriptor(CpeDescriptorFactory.produceDescriptor(aDescriptorStream));
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Creates an object representation from default cpe descriptor.
   * 
   * @throws UIMARuntimeException wraps Exception
   */
  public void parse() {
    defaultConfig = true;
    InputStream defaultDescriptorStream = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE);
    try {
      setCpeDescriptor(CpeDescriptorFactory.produceDescriptor(defaultDescriptorStream));
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    } finally {
      try {
        defaultDescriptorStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * Checks if cpe description has been created
   * 
   * @throws ConfigurationException -
   */
  private void checkForErrors() throws ResourceConfigurationException {
    if (cpeDescriptor == null) {
      throw new ResourceConfigurationException(new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_no_cpe_descriptor__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
  }

  /**
   * Returns Collection Reader instantiated from configuration in the cpe descriptor. It also
   * creates and intializes the Cas Initializer if one is defined and associates it with the
   * CollectionReader.
   * 
   * @return CollectionReader instance
   * 
   * @throws ResourceConfigurationException
   */
  public BaseCollectionReader getCollectionReader() throws ResourceConfigurationException {
    checkForErrors();
    BaseCollectionReader colreader = null;
    try {
      CpeCollectionReader reader = (getCpeDescriptor().getAllCollectionCollectionReaders())[0];
      if (reader == null) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "<collectionReader>", "<cpeDescriptor>" }, new Exception(
                        CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_missing_required_element__WARNING", new Object[] {
                                    Thread.currentThread().getName(), "<collectionReader>" })));
      }

      CpeCollectionReaderIterator cit = reader.getCollectionIterator();
      if (cit == null || cit.getDescriptor() == null || 
              (cit.getDescriptor().getInclude() == null && cit.getDescriptor().getImport() == null)) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "<include>", "<collectionIterator>" }, new Exception(
                        CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_missing_required_element__WARNING", new Object[] {
                                    Thread.currentThread().getName(), "<include> or <import>" })));
      }
      if (cit.getDescriptor().getInclude() != null && cit.getDescriptor().getInclude().get() == null) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "<href>", "<collectionIterator>" }, new Exception(
                        CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                                new Object[] { Thread.currentThread().getName(), "<href>",
                                    "<collectionIterator>" })));
      }

      URL descriptorUrl = cit.getDescriptor().findAbsoluteUrl(getResourceManager());
      // create new collection reader from the descriptor
      XMLInputSource in1 = new XMLInputSource(descriptorUrl);
      ResourceSpecifier colReaderSp = UIMAFramework.getXMLParser()
              .parseCollectionReaderDescription(in1);

      overrideParameterSettings(colReaderSp, cit.getConfigurationParameterSettings(), "Collection Reader");

      // compute sofa mapping for the CollectionReader
      CpeSofaMappings sofanamemappings = cit.getSofaNameMappings();
      HashMap sofamap = new HashMap();
      if (sofanamemappings != null) {
        CpeSofaMapping[] sofaNameMappingArray = sofanamemappings.getSofaNameMappings();
        for (int i = 0; sofaNameMappingArray != null && i < sofaNameMappingArray.length; i++) {
          CpeSofaMapping aSofaMap = sofaNameMappingArray[i];
          // if no component sofa name, then set it to default
          if (aSofaMap.getComponentSofaName() == null)
            aSofaMap.setComponentSofaName(CAS.NAME_DEFAULT_TEXT_SOFA);
          sofamap.put(aSofaMap.getComponentSofaName(), aSofaMap.getCpeSofaName());
        }
      }

      // create child UimaContext for the CollectionReader
      UimaContextAdmin collectionReaderContext = uimaContext.createChild("_CollectionReader",
              sofamap);
      Map additionalParams = new HashMap();
      additionalParams.put(Resource.PARAM_UIMA_CONTEXT, collectionReaderContext);
      colreader = (BaseCollectionReader) UIMAFramework.produceResource(BaseCollectionReader.class,
              colReaderSp, getResourceManager(), additionalParams);

      //set up CAS Initializer
      CpeCollectionReaderCasInitializer casInit = reader.getCasInitializer();
      if (casInit != null) {
        if (casInit.getDescriptor() == null) {
          throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                  new Object[] { "<descriptor>", "<casInitializer>" }, new Exception(
                          CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_missing_required_element__WARNING", new Object[] {
                                      Thread.currentThread().getName(), "<descriptor>" })));
        }
        if (casInit.getDescriptor().getInclude() == null && casInit.getDescriptor().getImport() == null) {
          throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                  new Object[] { "<include>", "<casInitializer>" }, new Exception(
                          CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_missing_required_element__WARNING", new Object[] {
                                      Thread.currentThread().getName(), "<include> or <import>" })));
        }
        if (casInit.getDescriptor().getInclude() != null &&
              (casInit.getDescriptor().getInclude().get() == null
                || casInit.getDescriptor().getInclude().get().length() == 0)) {
          throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                  new Object[] { "<href>", "<casInitializer>" }, new Exception(CpmLocalizedMessage
                          .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                                  new Object[] { Thread.currentThread().getName(), "<href>",
                                      "<casInitializer>" })));
        }

        URL casInitDescUrl = casInit.getDescriptor().findAbsoluteUrl(getResourceManager());

        XMLInputSource in4 = new XMLInputSource(casInitDescUrl);
        ResourceSpecifier casIniSp = UIMAFramework.getXMLParser().parseCasInitializerDescription(
                in4);

        overrideParameterSettings(casIniSp, casInit.getConfigurationParameterSettings(),
                "Cas Initializer");

        // compute sofa mapping for the CAS Initializer
        CpeSofaMappings sofaNamemappings = casInit.getSofaNameMappings();
        sofamap = new HashMap();
        if (sofaNamemappings != null) {
          CpeSofaMapping[] sofaNameMappingArray = sofaNamemappings.getSofaNameMappings();
          for (int i = 0; sofaNameMappingArray != null && i < sofaNameMappingArray.length; i++) {
            CpeSofaMapping aSofaMap = sofaNameMappingArray[i];
            // if no component sofa name, then set it to default
            if (aSofaMap.getComponentSofaName() == null)
              aSofaMap.setComponentSofaName(CAS.NAME_DEFAULT_TEXT_SOFA);
            sofamap.put(aSofaMap.getComponentSofaName(), aSofaMap.getCpeSofaName());
          }
        }

        // create child UimaContext for the CAS Initializer
        UimaContextAdmin initializerContext = uimaContext.createChild("_CasInitializer", sofamap);
        additionalParams.put(Resource.PARAM_UIMA_CONTEXT, initializerContext);

        Object initializer = produceInitializer(casIniSp, additionalParams);

        if (initializer instanceof CasDataInitializer) {
          ((CasDataCollectionReader) colreader).setCasInitializer((CasDataInitializer) initializer);
        } else if (initializer instanceof CasInitializer) {
          ((CollectionReader) colreader).setCasInitializer((CasInitializer) initializer);
        } else {
          throw new ResourceConfigurationException(InvalidXMLException.INVALID_ELEMENT_TYPE,
                  new Object[] { "CasDataInitializer", initializer.getClass().getName() },
                  new Exception(CpmLocalizedMessage.getLocalizedMessage(
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_EXP_incompatible_component__WARNING", new Object[] {
                              Thread.currentThread().getName(), "CasInitializer",
                              "CasDataInitializer", initializer.getClass().getName() })));
        }
      }
      // Retrieve number of entities to process from CPE configuration
      long numDocs2Process = getCPEConfig().getNumToProcess();
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_docs_to_process__CONFIG",
                new Object[] { Thread.currentThread().getName(), String.valueOf(numDocs2Process) });
      }
      // Provide CollectionReader with the number of documents to process
      ((ConfigurableResource_ImplBase) colreader).setConfigParameterValue("processSize",
              Integer.valueOf((int)numDocs2Process) );
      CpeConfiguration cpeType = getCpeDescriptor().getCpeConfiguration();
      if (cpeType != null && cpeType.getStartingEntityId() != null
              && cpeType.getStartingEntityId().trim().length() > 0) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_start_doc_id__FINEST",
                  new Object[] { Thread.currentThread().getName(), cpeType.getStartingEntityId() });
        }
        colreader.getProcessingResourceMetaData().getConfigurationParameterSettings()
                .setParameterValue("startNumber", cpeType.getStartingEntityId().trim());
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).log(
                  Level.FINEST,
                  "Retrieved Documents Starting with DocId ::"
                          + colreader.getProcessingResourceMetaData()
                                  .getConfigurationParameterSettings().getParameterValue(
                                          "startNumber"));
        }
      }

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(
                Level.FINEST,
                "Retrieved processSize ::"
                        + ((ConfigurableResource_ImplBase) colreader)
                                .getConfigParameterValue("processSize"));
      }
      return colreader;
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
  }

  /**
   * Returns an array of Cas Processors instantiated from the cpe descriptor
   * 
   * @return - array of CasProcessor instances
   * 
   * @throws ResourceConfigurationException -
   */
  public CasProcessor[] getCasProcessors() throws ResourceConfigurationException {
    checkForErrors();
    try {
      if (getCpeDescriptor().getCpeCasProcessors() == null) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "<casProcessors>", "<cpeDescriptor>" }, new Exception(
                        CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_bad_cpe_descriptor__WARNING", new Object[] { Thread
                                        .currentThread().getName() })));
      }
      CpeCasProcessors ct = getCpeDescriptor().getCpeCasProcessors();

      CpeCasProcessor[] casProcessorList = ct.getAllCpeCasProcessors();
      Vector v = new Vector();
      if (casProcessorList == null || casProcessorList.length == 0) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "<casProcessor>", "<casProcessors>" }, new Exception(
                        CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_bad_cpe_descriptor__WARNING", new Object[] { Thread
                                        .currentThread().getName() })));
      }

      Hashtable namesMap = new Hashtable();
      for (int i = 0; i < casProcessorList.length; i++) {
        CpeCasProcessor processorType = casProcessorList[i];
        if (processorType == null) {
          throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                  new Object[] { "<casProcessor>", "<casProcessors>" }, new Exception(
                          CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_bad_cpe_descriptor__WARNING", new Object[] { Thread
                                          .currentThread().getName() })));
        }

        // Check for duplicate Cas Processor names. Names must be unique
        if (namesMap.containsKey(processorType.getName())) {
          throw new ResourceConfigurationException(InvalidXMLException.INVALID_CPE_DESCRIPTOR,
                  new Object[] { "casProcessor", "name" }, new CPMException(CpmLocalizedMessage
                          .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_duplicate_name__WARNING", new Object[] {
                                      Thread.currentThread().getName(), processorType.getName() })));
        } else {
          namesMap.put(processorType.getName(), processorType.getName());
        }

        String deploymentType = processorType.getDeployment();
        if (deploymentType == null) {
          throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
                  new Object[] { "deployment", "<casProcessor>" }, new Exception(
                          CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                                  new Object[] { Thread.currentThread().getName(),
                                      processorType.getName(), "deployment", "casProcessor" })));
        }
        CasProcessor casProcessor = null;
        String deployModel = "";
        boolean cpInMap = false;

        // Check if the CP has already been instantiated. The map holds one instance of a CP with a
        // given name
        // The purpose of the map is to provide access to CP operational parameters. This is needed
        // to
        // determine if multiple instances of the CP are allowed.
        if (cpMap.containsKey(processorType.getName())) {
          cpInMap = true; // the CasProcessor is in the map
          casProcessor = (CasProcessor) cpMap.get(processorType.getName());
          // Check operational parameters to determine if multiple instances of the CP are allowed
          if (!casProcessor.getProcessingResourceMetaData().getOperationalProperties()
                  .isMultipleDeploymentAllowed()) {
            continue; // one instance already created. Multiple instances of this CP not allowed
          }
        }

        if (Constants.DEPLOYMENT_LOCAL.equals(deploymentType.toLowerCase())) {
          casProcessor = produceLocalCasProcessor(processorType);
          deployModel = Constants.DEPLOYMENT_LOCAL;
        } else if (Constants.DEPLOYMENT_INTEGRATED.equals(deploymentType.toLowerCase())) {

          casProcessor = produceIntegratedCasProcessor(processorType);
          deployModel = Constants.DEPLOYMENT_INTEGRATED;
        } else if (Constants.DEPLOYMENT_REMOTE.equals(deploymentType.toLowerCase())) {
          casProcessor = produceRemoteCasProcessor(processorType);
          deployModel = Constants.DEPLOYMENT_REMOTE;
        } else {
          throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
                  new Object[] { "deployment", "<casProcessor>" }, new Exception(
                          CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                  "UIMA_CPM_Exception_invalid_deployment__WARNING", new Object[] {
                                      Thread.currentThread().getName(), processorType.getName(),
                                      deploymentType })));
        }

        // Add the casProcessor instantiated above to the map. The map is used to check if
        // multiple instances of the cp are allowed. Need to store an instance in the map
        // since the only way to determine whether or not multiple instances are allowed is
        // to check OperationalProperties in the CP metadata.
        if (!cpInMap) {
          cpMap.put(processorType.getName(), casProcessor);
        }

        String name = casProcessor.getProcessingResourceMetaData().getName();
        if (!casProcessorConfigMap.containsKey(name)) {
          casProcessorConfigMap.put(name, processorType);
        } else {
          // Throw an exception due to a non-unique name. CPM requires Cas Processors to have a
          // unique name.
          // The unique name enforcement for Local and Remote CP's is done
          // above 'if ( namesMap.containsKey(processorType.getName()))'. In case of integrated CP,
          // the
          // name is taken from the CP descriptor. For Local and Remote, the names are taken from
          // the
          // CPE descriptor
          if (firstTime && Constants.DEPLOYMENT_INTEGRATED.equalsIgnoreCase(deployModel)) {
            throw new ResourceConfigurationException(new CPMException(CpmLocalizedMessage
                    .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_EXP_duplicate_name__WARNING", new Object[] {
                                Thread.currentThread().getName(), processorType.getName() })));
          }
        }

        v.add(casProcessor);
      }

      CasProcessor[] processors = new CasProcessor[v.size()];
      v.copyInto(processors);
      return processors;
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    } finally {
      firstTime = false;
    }
  }

  /**
   * Check if a class has appropriate type
   * 
   * @param aResourceClass -
   *          class to check
   * @param resourceSpecifier -
   *          specifier containing expected type
   * @param aDescriptor -
   *          descriptor name
   * 
   * @return true - if class matches type
   * 
   * @throws ResourceConfigurationException -
   */
  public boolean isDefinitionInstanceOf(Class aResourceClass, ResourceSpecifier resourceSpecifier,
          String aDescriptor) throws ResourceConfigurationException {
    boolean validDefinition = false;
    String implementationClass = null;
    try {
      String frameworkName = null;
      if (resourceSpecifier instanceof AnalysisEngineDescription) {
        frameworkName = ((AnalysisEngineDescription) resourceSpecifier)
                .getFrameworkImplementation();
        implementationClass = ((AnalysisEngineDescription) resourceSpecifier)
                .getImplementationName();
      } else if (resourceSpecifier instanceof CasConsumerDescription) {
        frameworkName = ((CasConsumerDescription) resourceSpecifier).getFrameworkImplementation();
        implementationClass = ((CasConsumerDescription) resourceSpecifier).getImplementationName();
      } else {
        return false;
      }

      if (frameworkName.startsWith(org.apache.uima.Constants.CPP_FRAMEWORK_NAME)) {
        validDefinition = true;
      } else {
        // String className = ((CasConsumerDescription) resourceSpecifier).getImplementationName();
        // load class using UIMA Extension ClassLoader if there is one
        ClassLoader cl = null;
        ResourceManager rm = getResourceManager();
        if (rm != null) {
          cl = rm.getExtensionClassLoader();
        }
        if (cl == null) {
          cl = this.getClass().getClassLoader();
        }
        // Class currentClass = Class.forName(className, true, cl);
        Class currentClass = Class.forName(implementationClass, true, cl);

        // check to see if this is a subclass of aResourceClass
        if (aResourceClass.isAssignableFrom(currentClass)) {
          validDefinition = true;
        }
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(ResourceInitializationException.CLASS_NOT_FOUND,
              new Object[] { implementationClass, aDescriptor }, e);
    }
    return validDefinition;
  }

  /**
   * Instantiates CasData Consumer from a given class.
   * 
   * @param aResourceClass -
   *          CasDataConsumer class
   * @param aSpecifier -
   *          specifier
   * @param aAdditionalParams -
   *          parameters used to initialize CasDataConsumer
   * @return - instance of CasProcessor
   * 
   * @throws ResourceInitializationException -
   */
  public CasProcessor produceCasDataConsumer(Class aResourceClass, ResourceSpecifier aSpecifier,
          Map aAdditionalParams) throws ResourceInitializationException {
    String className = null;
    try {
      className = ((CasConsumerDescription) aSpecifier).getImplementationName();
      // load class using UIMA Extension ClassLoader if there is one
      ClassLoader cl = null;
      ResourceManager rm = getResourceManager();
      if (rm != null) {
        cl = rm.getExtensionClassLoader();
      }
      if (cl == null) {
        cl = this.getClass().getClassLoader();
      }
      Class currentClass = Class.forName(className, true, cl);
      // check to see if this is a subclass of aResourceClass
      if (aResourceClass.isAssignableFrom(currentClass)) {
        // instantiate this Resource Class
        CasDataConsumer casProcessor = (CasDataConsumer) currentClass.newInstance();

        // attempt to initialize it
        if (casProcessor.initialize(aSpecifier, aAdditionalParams)) {
          // success!
          return casProcessor;
        }
      }
    }
    // if an exception occurs, log it but do not throw it... yet
    catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
              new Object[] { className, aSpecifier.getSourceUrlString() }, e);
    } catch (IllegalAccessException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                  aSpecifier.getSourceUrlString() }, e);
    } catch (InstantiationException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                  aSpecifier.getSourceUrlString() }, e);
    }

    return null;
  }

  /**
   * Instantiates Cas Initializer from a given class. It return
   * 
   * @param aSpecifier -
   *          configuration for Cas Initializer
   * @param aAdditionalParams -
   *          parameters to initialize Cas Initializer
   * 
   * @return instance of CasDataInitializer or CasInitializer
   * @throws ResourceInitializationException -
   */
  private Object produceInitializer(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {
    String className = null;
    try {
      className = ((CasInitializerDescription) aSpecifier).getImplementationName();
      // load class using UIMA Extension ClassLoader if there is one
      ClassLoader cl = null;
      ResourceManager rm = getResourceManager();
      if (rm != null) {
        cl = rm.getExtensionClassLoader();
      }
      if (cl == null) {
        cl = this.getClass().getClassLoader();
      }
      Class currentClass = Class.forName(className, true, cl);
      Object initializer = currentClass.newInstance();
      // check to see if this is a subclass of aResourceClass
      if (initializer instanceof CasInitializer) {
        ((CasInitializer) initializer).initialize(aSpecifier, aAdditionalParams);
        return initializer;
      } else if (initializer instanceof CasDataInitializer) {
        ((CasDataInitializer) initializer).initialize(aSpecifier, aAdditionalParams);
        return initializer;
      } else {
        throw new InstantiationException("Unexpected CasInitializer-"
                + initializer.getClass().getName());
      }
    }
    // if an exception occurs, log it but do not throw it... yet
    catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
              new Object[] { className, aSpecifier.getSourceUrlString() }, e);
    } catch (IllegalAccessException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                  aSpecifier.getSourceUrlString() }, e);
    } catch (InstantiationException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                  aSpecifier.getSourceUrlString() }, e);
    }
  }

  /**
   * Returns a descriptor path associated with Cas Processor
   * 
   * @param aCasProcessorCfg -
   *          Cas Processor configuration
   * @return - Descriptor path
   * 
   * @throws ResourceConfigurationException -
   */
  public URL getDescriptorURL(CpeCasProcessor aCasProcessorCfg)
          throws ResourceConfigurationException {
    if (aCasProcessorCfg.getCpeComponentDescriptor() == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "descriptor", "casProcessor" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_missing_xml_element__WARNING",
              new Object[] { Thread.currentThread().getName(), aCasProcessorCfg.getName(),
                  "descriptor" })));
    }
    return aCasProcessorCfg.getCpeComponentDescriptor().findAbsoluteUrl(getResourceManager());
  }

  /**
   * Instantiates a ResourceSpecifier from a given URL.
   * 
   * @param aDescriptorUrl - URL of descriptor
   * @return - ResourceSpecifier
   * 
   * @throws Exception -
   */
  public ResourceSpecifier getSpecifier(URL aDescriptorUrl) throws Exception {
    XMLInputSource in = new XMLInputSource(aDescriptorUrl);
    return UIMAFramework.getXMLParser().parseResourceSpecifier(in);

  }

  /**
   * Instantiates a local (managed) Cas Processor
   * 
   * @param aCasProcessorCfg -
   *          Cas Processor configuration
   * @return - Local CasProcessor
   * 
   * @throws ResourceConfigurationException -
   */
  private CasProcessor produceLocalCasProcessor(CpeCasProcessor aCasProcessorCfg)
          throws ResourceConfigurationException {
    if (aCasProcessorCfg == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "casProcessor", "casProcessors" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING",
              new Object[] { Thread.currentThread().getName() })));
    }
    CasProcessor casProcessor = new NetworkCasProcessorImpl(aCasProcessorCfg);
    return casProcessor;
  }

  /**
   * Find a parameter with a given name in the in-memory component descriptor
   * 
   * @param aCps -
   *          parameter settings from the component's descriptor
   * @param aParamName -
   *          name of the parameter to look for
   * 
   * @return - parameter as {@link NameValuePair} instance. If not found, returns null.
   * 
   * @throws Exception -
   *           any error
   */
  private NameValuePair findMatchingNameValuePair(ConfigurationParameterSettings aCps,
          String aParamName) throws Exception {

    NameValuePair[] nvp = aCps.getParameterSettings();

    // Find a parameter with a given name
    for (int i = 0; nvp != null && i < nvp.length; i++) {
      if (nvp[i].getName() != null) {
        if (nvp[i].getName().equalsIgnoreCase(aParamName)) {
          return nvp[i]; // Found it
        }
      }
    }
    return null; // Parameter with a given name does not exist
  }

  /**
   * Replace a primitive value of a given parameter with a value defined in the CPE descriptor
   * 
   * @param aType -
   *          type of the primitive value ( String, Integer, Boolean, or Float)
   * @param aCps -
   *          parameter settings from the component's descriptor
   * @param aCPE_nvp -
   *          parameter containing array of values to replace values in the component's descriptor
   * @throws Exception -
   */
  private void replacePrimitive(String aType, boolean aMandatoryParam,
          ConfigurationParameterSettings aCps,
          org.apache.uima.collection.metadata.NameValuePair aCPE_nvp,
          String aComponentName) throws Exception {
    boolean newParamSetting = false;

    // Get a new value for the primitive param
    Object aValueObject = aCPE_nvp.getValue();
    String param_name = aCPE_nvp.getName();

    // Find corresponding parameter in the in-memory component descriptor
    NameValuePair nvp = findMatchingNameValuePair(aCps, param_name.trim());
    if (nvp == null) {
      newParamSetting = true;
      nvp = new NameValuePair_impl();
      nvp.setName(param_name);
    }
    // Copy a new value based on type
    if (aType.equals("String") && aValueObject instanceof String) {
      nvp.setValue(aValueObject);
    } else if (aType.equals("Integer") && aValueObject instanceof Integer) {
      nvp.setValue(aValueObject);
    } else if (aType.equals("Float") && aValueObject instanceof Float) {
      nvp.setValue(aValueObject);

    } else if (aType.equals("Boolean") && aValueObject instanceof Boolean) {
      nvp.setValue(aValueObject);
    }

    if (newParamSetting) {
      aCps.setParameterValue(null, nvp.getName(), nvp.getValue());
    }
  }

  /**
   * Replace array values found in the component's descriptor with values found in the CPE
   * descriptor
   * 
   * @param aType -
   *          primitive type of the array (Sting, Integer, Float, or Boolean)
   * @param aCps -
   *          parameter settings from the component's descriptor
   * @param aCPE_nvp -
   *          parameter containing array of values to replace values in the component's descriptor
   * 
   * @throws Exception -
   *           any error
   */
  private void replaceArray(String aType, boolean aMandatoryParam,
          ConfigurationParameterSettings aCps,
          org.apache.uima.collection.metadata.NameValuePair aCPE_nvp,
          String aComponentName) throws Exception {
    boolean newParamSetting = false;
    Object valueObject = aCPE_nvp.getValue();
    String param_name = aCPE_nvp.getName();
    // Find in the component in-memory descriptor a parameter with a given name
    NameValuePair nvp = findMatchingNameValuePair(aCps, param_name.trim());
    if (nvp == null) {
      // Parameter setting does not exist in the component's descriptor so create new
      newParamSetting = true;
      nvp = new NameValuePair_impl();
      nvp.setName(param_name);
    }
    // Override component's parameters based on type
    if (aType.equals("String") && valueObject instanceof String[]) {
      nvp.setValue(valueObject);
    } else if (aType.equals("Integer") && valueObject instanceof Integer[]) {
      nvp.setValue(valueObject);
    } else if (aType.equals("Float") && valueObject instanceof Float[]) {
      nvp.setValue(valueObject);
    } else if (aType.equals("Boolean") && valueObject instanceof Boolean[]) {
      nvp.setValue(valueObject);
    }
    if (newParamSetting) {
      aCps.setParameterValue(null, nvp.getName(), nvp.getValue());
    }

  }

  /**
   * Override component's parameters. This overridde effects the in-memory settings. The actual
   * component's descriptor will not be changed.
   * 
   * @param aResourceSpecifier -
   *          in-memory descriptor of the component
   * @param aCPE_nvp -
   *          parameter represented as name-value pair. If the name of the parameter is found in the
   *          component's descriptor its value will be changed.
   * 
   * @throws Exception -
   *           error during processing
   */
  private boolean overrideParameterIfExists(ResourceSpecifier aResourceSpecifier,
          org.apache.uima.collection.metadata.NameValuePair aCPE_nvp,
          String aComponentName) throws Exception {
    // Retrieve component's parameter settings from the in-memory descriptor
    ConfigurationParameterDeclarations cpd = ((ResourceCreationSpecifier) aResourceSpecifier)
            .getMetaData().getConfigurationParameterDeclarations();
    // Get the name of the parameter to find in the component's parameter list
    String param_name = aCPE_nvp.getName().trim();
    // Extract parameter with a matching name from the parameter declaration section of the
    // component's descriptor
    ConfigurationParameter cparam = cpd.getConfigurationParameter(null, param_name);
    if (cparam != null) {
      // Retrieve component's parameter settings from the in-memory descriptor
      ConfigurationParameterSettings cps = ((ResourceCreationSpecifier) aResourceSpecifier)
              .getMetaData().getConfigurationParameterSettings();
      // Determie if it is a multi-value parameter (array)
      boolean isMultiValue = cparam.isMultiValued();
      // Check if there is a match based on param name
      if (cparam.getName().equals(param_name)) {
        boolean mandatory = cparam.isMandatory();
        if (isMultiValue) {
          // Override array with values from the CPE descriptor
          replaceArray(cparam.getType(), mandatory, cps, aCPE_nvp, aComponentName);
        } else {
          // Override primitive parameter with value from the CPE descriptor
          replacePrimitive(cparam.getType(), mandatory, cps, aCPE_nvp,
                  aComponentName);
        }
        return true; // Found a match and did the override
      }

    }
    return false; // The parameter does not exist in the component's descriptor
  }

  /**
   * Replace component's parameters. Its parameter values will be changed to match those defined in
   * the CPE descriptor.
   * 
   * @param aResourceSpecifier -
   *          component's descriptor containing parameters to override
   * @param aCasProcessorCPEConfig -
   *          cas processor configuration containing optional parameters to be used for overriding
   *          parameters in the descriptor
   * @throws Exception -
   *           failure during processing
   */
  private void overrideParameterSettings(ResourceSpecifier aResourceSpecifier,
          CasProcessorConfigurationParameterSettings aCpe_cps,
          String aComponentName) throws Exception {

    if (aCpe_cps != null && aCpe_cps.getParameterSettings() != null) {
      // Extract new parameters from the CPE descriptor
      // Parameters are optional, so test and do the override if necessary
      org.apache.uima.collection.metadata.NameValuePair[] nvp = aCpe_cps.getParameterSettings();

      for (int i = 0; i < nvp.length; i++) {
        // Next parameter to overridde
        if (nvp[i] != null) {
          overrideParameterIfExists(aResourceSpecifier, nvp[i], aComponentName);
        }
      }
    }
  }

  /**
   * Instantiates integrated Cas Processor
   * 
   * @param aCasProcessorCfg -
   *          Cas processor configuration
   * @return - Integrated CasProcessor
   * @throws ResourceConfigurationException -
   */
  private CasProcessor produceIntegratedCasProcessor(CpeCasProcessor aCasProcessorType)
          throws ResourceConfigurationException {
    CasProcessor casProcessor = null;
    ResourceSpecifier resourceSpecifier = null;
    try {
      if (aCasProcessorType != null) {
        URL descriptorUrl = getDescriptorURL(aCasProcessorType);
        resourceSpecifier = getSpecifier(descriptorUrl);

        CpeSofaMappings sofanamemappings = aCasProcessorType.getSofaNameMappings();
        HashMap sofamap = new HashMap();
        if (sofanamemappings != null) {
          CpeSofaMapping[] sofaNameMappingArray = sofanamemappings.getSofaNameMappings();
          for (int i = 0; sofaNameMappingArray != null && i < sofaNameMappingArray.length; i++) {
            CpeSofaMapping aSofaMap = sofaNameMappingArray[i];
            // if no component sofa name, then set it to default
            if (aSofaMap.getComponentSofaName() == null)
              aSofaMap.setComponentSofaName(CAS.NAME_DEFAULT_TEXT_SOFA);
            sofamap.put(aSofaMap.getComponentSofaName(), aSofaMap.getCpeSofaName());
          }
        }

        // Replace parameters in component descriptor with values defined in the CPE descriptor
        overrideParameterSettings(resourceSpecifier, aCasProcessorType
                .getConfigurationParameterSettings(), "CasProcessor:"
                + aCasProcessorType.getName());

        // create child UimaContext and insert into mInitParams map
        UimaContextAdmin childContext = uimaContext.createChild(aCasProcessorType.getName(),
                sofamap);
        Map additionalParams = new HashMap();
        additionalParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

        // need this check to do specific CasDataConsumer processing
        if (resourceSpecifier instanceof CasConsumerDescription
                && !isDefinitionInstanceOf(CasConsumer.class, resourceSpecifier, descriptorUrl.toString())) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_producing_cas_data_consumer__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          casProcessor = produceCasDataConsumer(CasProcessor.class, resourceSpecifier,
                  additionalParams);
        } else {
          // Except for CasDataConsumers, everything else is treated as an AnalysisEngine.
          // This includes CAS Consumers, which will automatically be wrapped inside AnalysisEngines
          // by the produceAnalysisEngine method. It also handles UriSpecifiers for
          // remote services (for either AE or CAS Consumer)
          casProcessor = UIMAFramework.produceAnalysisEngine(resourceSpecifier,
                  getResourceManager(), additionalParams);
        }

        // Check if CasProcesser has been instantiated
        if (casProcessor == null) {

          throw new ResourceConfigurationException(new Exception(CpmLocalizedMessage
                  .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_EXP_instantiation_exception__WARNING", new Object[] {
                              Thread.currentThread().getName(), aCasProcessorType.getName() })));
        }
      }
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(
              ResourceInitializationException.CAS_PROCESSOR_INITIALIZE_FAILED,
              new Object[] { aCasProcessorType.getName() }, e);
    }
    // Override the name of the component with the name specified in the cpe descriptor
    // Uniqueness of names is enforced on names in the cpe descriptor not those defined
    // in the component descriptor
    if ( casProcessor != null && aCasProcessorType != null ) {
      casProcessor.getProcessingResourceMetaData().setName(aCasProcessorType.getName());
    }

    return casProcessor;

  }

  /**
   * Instantiates remote Cas Processor
   * 
   * @param aCasProcessorCfg -
   *          Cas Processor Configuration
   * @return - Remote CasProcessor
   * 
   * @throws ResourceConfigurationException -
   */
  private CasProcessor produceRemoteCasProcessor(CpeCasProcessor aCasProcessorType)
          throws ResourceConfigurationException {
    String protocol = DeployFactory.getProtocol(aCasProcessorType, getResourceManager());
    CasProcessor casProcessor = null;
    if (Constants.SOCKET_PROTOCOL.equalsIgnoreCase(protocol)) {
      casProcessor = new CasObjectNetworkCasProcessorImpl(aCasProcessorType);
    } else {
      // For remote Cas Processor make sure that the required parameters are defined.
      // Specifically, VNS host and port are required. Do validation now. If it fails, the
      // ResourceConfigurationException is thrown and we are out here.
      // verifyDeploymentParams(aCasProcessorType.getName(),
      // aCasProcessorType.getDeploymentParameters());
      casProcessor = new NetworkCasProcessorImpl(aCasProcessorType);
    }
    return casProcessor;
  }

  /**
   * Returns an object containing global CPE configuration including:
   * <ul>
   * <li>Number of documents to process</li>
   * <li>Checkpoint configuration</li>
   * <li>id of the document begin processing</li>
   * </ul>
   * 
   * @return Global CPE Configuration
   * @throws InstantiationException
   */
  public CpeConfiguration getCPEConfig() throws InstantiationException {
    try {
      return getCpeDescriptor().getCpeConfiguration();
    } catch (Exception e) {
      throw new InstantiationException(e.getMessage());
    }
  }

  /**
   * Returns number of processing threads (Processing Units)
   * 
   * @return Number of processing threads
   * 
   * @throws ResourceConfigurationException -
   */
  public int getProcessingUnitThreadCount() throws ResourceConfigurationException {
    int threadCount;

    try {
      threadCount = this.getCpeDescriptor().getCpeCasProcessors().getConcurrentPUCount();
    } catch (Exception e) {

      throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
              new Object[] { "processingUnitThreadCount" }, new Exception(CpmLocalizedMessage
                      .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                              "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                              new Object[] { Thread.currentThread().getName(), "casProcessors",
                                  "processingUnitThreadCount", "<casProcessors>", })));
    }

    return threadCount;
  }

  /**
   * 
   * @return true if the configuration is the default
   */
  public boolean isDefault() {
    return defaultConfig;
  }

  /**
   * Returns Cpe Descriptor
   * 
   * @return the Cpe Descriptor
   */
  public CpeDescription getCpeDescriptor() {
    return cpeDescriptor;
  }

  /**
   * Assigns Cpe configuration to use
   * 
   * @param description
   */
  private void setCpeDescriptor(CpeDescription description) {
    cpeDescriptor = description;
  }

  /**
   * Checks uniqueness of a given name. This name is compared against all Cas Processors already
   * defined. Cas Processor names must be unique.
   * 
   * @param aName -
   *          name to check
   * @return - true if name is unique, false otherwise
   * 
   */
  private boolean isUniqueName(String aName) throws CpeDescriptorException {
    int index = 0;
    if (getCpeDescriptor().getCpeCasProcessors() != null
            && getCpeDescriptor().getCpeCasProcessors().getAllCpeCasProcessors() != null) {
      index = getCpeDescriptor().getCpeCasProcessors().getAllCpeCasProcessors().length;
    }
    for (int i = 0; i < index; i++) {
      CpeCasProcessor processor = getCpeDescriptor().getCpeCasProcessors().getCpeCasProcessor(i);
      String name = processor.getName();
      if (name != null && name.equalsIgnoreCase(aName)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks for existance of two parameters required to launch remote Cas Processor vnsHost and
   * vnsPort. If not found the remote CasProcessor can not be located since the VNS is not
   * specified. In this case an exception is thrown.
   * 
   * @param aCasProcessorName
   * @param aDepParams
   * @throws ResourceConfigurationException -
   */
  private void verifyDeploymentParams(String aCasProcessorName,
          CasProcessorDeploymentParams aDepParams) throws ResourceConfigurationException {
    if (aDepParams == null) {
      throw new ResourceConfigurationException(new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                          Thread.currentThread().getName(), aCasProcessorName,
                          "<deploymentParameters>" })));
    }
    if (aDepParams == null || aDepParams.getAll() == null) {
      return; // nothing to do
    }

    try {
      CasProcessorDeploymentParam param = aDepParams.get(Constants.VNS_HOST);
      if (param == null || param.getParameterValue() == null
              || param.getParameterValue().trim().length() == 0) {
        throw new ResourceConfigurationException(
                ResourceInitializationException.CONFIG_SETTING_ABSENT,
                new Object[] { "parameter" }, new Exception(CpmLocalizedMessage
                        .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_deploy_params_not_defined__WARNING", new Object[] {
                                    Thread.currentThread().getName(), aCasProcessorName,
                                    Constants.VNS_HOST })));

      }
      param = aDepParams.get(Constants.VNS_PORT);
      if (param == null || param.getParameterValue() == null
              || param.getParameterValue().trim().length() == 0) {
        throw new ResourceConfigurationException(
                ResourceInitializationException.CONFIG_SETTING_ABSENT,
                new Object[] { "parameter" }, new Exception(CpmLocalizedMessage
                        .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_deploy_params_not_defined__WARNING", new Object[] {
                                    Thread.currentThread().getName(), aCasProcessorName,
                                    Constants.VNS_PORT })));

      }
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
    // for (int i = 0; i < totalParamCount; i++)
    // {
    // parameter param = aDepParams.getParameter(i);
    // if (Constants.VNS_HOST.equals(param.getName()) && param.getValue() != null &&
    // param.getValue().trim().length() > 0)
    // {
    // vnsHostFound = true;
    // }
    // else if (Constants.VNS_PORT.equals(param.getName()) && param.getValue() != null &&
    // param.getValue().trim().length() > 0)
    // {
    // vnsPortFound = true;
    // }
    // }
    // // Check if required params have been found
    // if (!vnsHostFound || !vnsPortFound)
    // {
    // if (!vnsHostFound)
    // {
    // throw new
    // ResourceConfigurationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new
    // Object[] { "parameter" }, new Exception("Remote CasProcessor:'" + aCasProcessorName + "' not
    // configured completely.Section '<deploymentParameters>' missing " + Constants.VNS_HOST + "
    // parameter and its value"));
    // }
    // if (!vnsPortFound)
    // {
    // throw new
    // ResourceConfigurationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new
    // Object[] { "parameter" }, new Exception("Remote CasProcessor:'" + aCasProcessorName + "' not
    // configured completely.Section '<deploymentParameters>' missing " + Constants.VNS_PORT + "
    // parameter and its value"));
    // }
    //
    // }

  }

  /**
   * Appends given Cas Processor to the list of CasProcessors
   * 
   * @param aCasProcessor -
   *          CasProcessor to add
   */
  public void addCasProcessor(CasProcessor aCasProcessor) throws ResourceConfigurationException {
    if (!initialized) {
      addCasProcessor(aCasProcessor.getProcessingResourceMetaData().getName());
    }
  }

  /**
   * Adds new Cas Processor with given name.
   * 
   * @param aCasProcessorName -
   *          name of the CasProcessor to add
   * 
   * @return -
   */
  private CpeCasProcessor addCasProcessor(String aCasProcessorName)
          throws ResourceConfigurationException {
    CpeCasProcessor newProcessor = null;
    try {
      if (!isUniqueName(aCasProcessorName)) {
        throw new ResourceConfigurationException(new Exception(CpmLocalizedMessage
                .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_EXP_duplicate_name__WARNING", new Object[] {
                            Thread.currentThread().getName(), aCasProcessorName })));
      }
      int index = getCpeDescriptor().getCpeCasProcessors().getAllCpeCasProcessors().length; // getcasProcessorCount();

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_add_cp_with_index__FINEST",
                new Object[] { Thread.currentThread().getName(), aCasProcessorName,
                    String.valueOf(index) });
      }
      CpeCasProcessor processor = getCpeDescriptor().getCpeCasProcessors().getCpeCasProcessor(
              index - 1);

      if (processor.getCheckpoint() == null) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                new Object[] { "checkpoint", "casProcessor" }, new Exception(CpmLocalizedMessage
                        .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                                    Thread.currentThread().getName(), aCasProcessorName,
                                    "<checkpoint>" })));
      }

      // For remote Cas Processor make sure that the required parameters are defined.
      // Specifically, VNS host and port are required. Do validation now. If it fails, the
      // ResourceConfigurationException is thrown and we are out here.
      if (Constants.DEPLOYMENT_REMOTE.equals(processor.getDeployment())) {
        String protocol = DeployFactory.getProtocol(processor, getResourceManager());
        if (Constants.VINCI_PROTOCOL.equals(protocol)) {
          verifyDeploymentParams(aCasProcessorName, processor.getDeploymentParams());
        }
      }

      if (processorCount == 0) {
        newProcessor = processor;
        copyCasProcessor(newProcessor, aCasProcessorName);
      } else {
        // CpeCasProcessor cloneProcessor =
        // getCpeDescriptor().getCpeCasProcessors().getCpeCasProcessor(0);

        newProcessor = new CpeIntegratedCasProcessorImpl();
        newProcessor.setDescriptor("href");
        // // Clone casProcessor from an existing one
        // newProcessor = cpeDescGen.createcasProcessor(cloneProcessor);
        copyCasProcessor(newProcessor, aCasProcessorName);
        getCpeDescriptor().getCpeCasProcessors().addCpeCasProcessor(newProcessor, processorCount);// setCpeCasProcessor(processorCount,
        // newProcessor);
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(
                Level.FINEST,
                "getCpeDescriptor().getCasProcessors().getCasProcessor.getAttributeValue(name) "
                        + " "
                        + getCpeDescriptor().getCpeCasProcessors().getCpeCasProcessor(
                                processorCount).getAttributeValue("name"));
        UIMAFramework
                .getLogger(this.getClass())
                .log(
                        Level.FINEST,
                        "getCpeDescriptor().getCasProcessors().getCasProcessor("
                                + "processorCount).getErrorHandling().getMaxConsecutiveRestarts().getAction() "
                                + " "
                                + getCpeDescriptor().getCpeCasProcessors().getCpeCasProcessor(
                                        processorCount).getErrorHandling()
                                        .getMaxConsecutiveRestarts().getAction());
      }
      if (!casProcessorConfigMap.containsKey(aCasProcessorName)) {
        casProcessorConfigMap.put(aCasProcessorName, getCpeDescriptor().getCpeCasProcessors()
                .getCpeCasProcessor(processorCount));
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
    processorCount++;
    return newProcessor;
  }

  /**
   * 
   * @param aList
   * @return the cpe descriptor constructed from the list
   * @throws ResourceConfigurationException -
   */
  public String getDescriptor(List aList) throws ResourceConfigurationException {
    if (aList.size() == 0) {
      return "";
    }

    try {
      String[] colReader = (String[]) aList.get(0);
      String[] casInitializer = (String[]) aList.get(1);

      getCpeDescriptor().getAllCollectionCollectionReaders()[0].getCollectionIterator()
              .getDescriptor().getInclude().set(colReader[0]);
      // Populate collection iterator descriptor path
      // Populate collection initializer descriptor path
      if (casInitializer[0].length() > 0) {
        getCpeDescriptor().getAllCollectionCollectionReaders()[0].getCasInitializer()
                .getDescriptor().getInclude().set(casInitializer[0]);
      } else {
        getCpeDescriptor().getAllCollectionCollectionReaders()[0].removeCasInitializer();
      }

      int procIndex = 2;
      // First check if casProcessors have been previously been added. If not, add them
      // to a descriptor.
      boolean create = false;
      int numProcessors = 0;
      CpeCasProcessors processorList = getCpeDescriptor().getCpeCasProcessors();
      // Check if added casProcessors using addCasProcessor API. If not create needed
      // casProcessors and add descriptor paths
      if (processorCount == 0) {
        create = true;
        // the list contains two other entires besides casProcessors. Namely collection iterator
        // and cas Initializer. So to get # of casProcessors in the list subtract 2 non-casProcessor
        // entries.
        numProcessors = aList.size() - procIndex;
      } else {
        numProcessors = processorList.getAllCpeCasProcessors().length;
      }
      CpeCasProcessor newProcessor = null;

      // Now add each casProcessor to a descriptor
      for (int i = 0; i < numProcessors; i++) {
        String[] casProcInfo = (String[]) aList.get(procIndex + i);
        if (create) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_create_new_cp_from_list__FINEST",
                    new Object[] { Thread.currentThread().getName(), casProcInfo[0] });
          }
          // The list suppose to contain an array of Strings of size 2.
          // Where the the first element is the name of the CasProcessor
          // and the second is the descriptor path
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_add_cp_from_list__FINEST",
                    new Object[] { Thread.currentThread().getName(), casProcInfo[0] });
          }
          newProcessor = addCasProcessor(casProcInfo[0]);
        } else {
          newProcessor = processorList.getCpeCasProcessor(i);
        }
        if (newProcessor != null) {
          newProcessor.setDescriptor(casProcInfo[1]);
        }
      }

      if (create) {
        // Set global flag as an indicator that the CPE Descriptor has been instantiated
        initialized = true;
      }
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      getCpeDescriptor().toXML(bos);

      return bos.toString();

    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
  }

  /**
   * @param newProcessor
   * @param aCasProcessor
   */
  private void copyCasProcessor(CpeCasProcessor aProcDesc, String aName) {
    try {
      aProcDesc.setName(aName);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass())
                .logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "initialize",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_show_cp_deployment__FINEST",
                        new Object[] { Thread.currentThread().getName(), aName,
                            aProcDesc.getDeployment() });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * 
   * @param collectionReader -
   *          collection reader to use by the CPM
   */
  public void addCollectionReader(BaseCollectionReader collectionReader) {
    // nothing done as of now; If there is any field that will be accessed
    // this method can be implemented to copy the values similar to addCasProcessod

  }

  /**
   * Gets the ResourceManager that all components of this CPE should share.
   * 
   * @return the resource manager
   */
  public ResourceManager getResourceManager() {
    return uimaContext.getResourceManager();
  }

}
