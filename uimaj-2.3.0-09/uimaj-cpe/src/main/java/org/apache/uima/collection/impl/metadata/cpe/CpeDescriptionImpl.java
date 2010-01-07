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

import java.util.List;

import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;

public class CpeDescriptionImpl extends MetaDataObject_impl implements CpeDescription {
  private static final long serialVersionUID = -8068920415609241198L;

  private CpeCollectionReader collectionReader;

  private CpeCasProcessors casProcessors;

  private CpeConfiguration cpeConfiguration;

  private CpeResourceManagerConfiguration resourceMgrConfig;

  public CpeDescriptionImpl() {
    super();
  }

  /**
   * This is needed for XMLParser.parseCpeDesription() to work. Typically users should use
   * CpeDescriptorFactory.produceDescriptor() instead. - APL
   */
  public CpeDescriptionImpl(XMLInputSource aInput) throws InvalidXMLException {
    try {
      CpeDescription descriptor = CpeDescriptorFactory.produceDescriptor(aInput);
      if (descriptor.getResourceManagerConfiguration() != null) {
        resourceMgrConfig = descriptor.getResourceManagerConfiguration();
      }
      if (descriptor.getCpeConfiguration() != null) {
        setCpeConfig(descriptor.getCpeConfiguration());
      }
      CpeCollectionReader[] readers = descriptor.getAllCollectionCollectionReaders();
      if (readers != null && readers.length > 0 && readers[0] != null) {
        addCollectionReader(readers[0]);
      }
      CpeCasProcessors cps = descriptor.getCpeCasProcessors();
      if (cps != null) {
        CpeCasProcessor[] casProcessors = cps.getAllCpeCasProcessors();
        for (int i = 0; casProcessors != null && i < casProcessors.length; i++) {
          addCasProcessor(casProcessors[i]);
        }
        if (getCpeCasProcessors() != null) {
          if (cps.getConcurrentPUCount() > 0) {
            getCpeCasProcessors().setConcurrentPUCount(cps.getConcurrentPUCount());
          }
          if (cps.getCasPoolSize() > 0) {
            getCpeCasProcessors().setPoolSize(cps.getCasPoolSize());
          }
          if (cps.getInputQueueSize() > 0) {
            getCpeCasProcessors().setInputQueueSize(cps.getInputQueueSize());
          }
          if (cps.getOutputQueueSize() > 0) {
            getCpeCasProcessors().setOutputQueueSize(cps.getOutputQueueSize());
          }
        }
      }
    } catch (Exception e) {
      throw new InvalidXMLException(e);
    }
  }

  public void addCollectionReader(CpeCollectionReader aCollectionReader)
          throws CpeDescriptorException {
    collectionReader = aCollectionReader;
  }

  public CpeCollectionReader addCollectionReader(String aCollectionReaderPath)
          throws CpeDescriptorException {
    if (collectionReader == null) {
      collectionReader = CpeDescriptorFactory.produceCollectionReader(aCollectionReaderPath);
    } else {
      collectionReader.getDescriptor().getInclude().set(aCollectionReaderPath);
    }
    return collectionReader;
  }

  /**
   * @deprecated As of v2.0, CAS Initializers are deprecated.
   */
  @Deprecated
public CpeCollectionReaderCasInitializer addCasInitializer(String aInitializerDescriptorPath)
          throws CpeDescriptorException {
    if (collectionReader == null) {
      collectionReader = CpeDescriptorFactory.produceCollectionReader();
    }
    if (collectionReader.getCasInitializer() == null) {
      collectionReader.setCasInitializer(CpeDescriptorFactory
              .produceCollectionReaderCasInitializer(aInitializerDescriptorPath, this));
    } else {
      collectionReader.getCasInitializer().getDescriptor().getInclude().set(
              aInitializerDescriptorPath);
    }
    return collectionReader.getCasInitializer();
  }

  public CpeCollectionReader[] getAllCollectionCollectionReaders() throws CpeDescriptorException {
    if (collectionReader == null) {
      return new CpeCollectionReader[0];
    }
    CpeCollectionReader[] readers = new CpeCollectionReader[1];
    readers[0] = collectionReader;
    return readers;
  }

  public void setAllCollectionCollectionReaders(CpeCollectionReader[] areaders)
          throws CpeDescriptorException {
    if (areaders == null || areaders.length == 0) {
      collectionReader = null;
    }
    else {
      collectionReader = areaders[0];
    }
  }

  public void setResourceManagerConfiguration(String aResMgrConfPagth) {
    if (resourceMgrConfig != null) {
      resourceMgrConfig.set(aResMgrConfPagth);
    }
  }

  public void setCpeResourceManagerConfiguration(CpeResourceManagerConfiguration aResMgrConfPagth) {
    resourceMgrConfig = aResMgrConfPagth;
  }

  public CpeResourceManagerConfiguration getCpeResourceManagerConfiguration() {
    return resourceMgrConfig;
  }

  /**
   * @deprecated
   */
  @Deprecated
public void setInputQueueSize(int aSize) throws CpeDescriptorException {
    if (casProcessors == null) {
      casProcessors = CpeDescriptorFactory.produceCasProcessors();
    }
    casProcessors.setInputQueueSize(aSize);
  }

  public void setProcessingUnitThreadCount(int aSize) throws CpeDescriptorException {
    if (casProcessors == null) {
      casProcessors = CpeDescriptorFactory.produceCasProcessors();
    }
    casProcessors.setConcurrentPUCount(aSize);
  }

  /**
   * @deprecated
   */
  @Deprecated
public void setOutputQueueSize(int aSize) throws CpeDescriptorException {
    if (casProcessors == null) {
      casProcessors = CpeDescriptorFactory.produceCasProcessors();
    }
    casProcessors.setOutputQueueSize(aSize);
  }

  public void setCpeCasProcessors(CpeCasProcessors aCasProcessors) {
    casProcessors = aCasProcessors;
  }

  public CpeCasProcessors getCpeCasProcessors() throws CpeDescriptorException {
    return casProcessors;
  }

  public void addCasProcessor(CpeCasProcessor aCasProcessor) throws CpeDescriptorException {
    if (casProcessors == null) {
      casProcessors = CpeDescriptorFactory.produceCasProcessors();
    }
    casProcessors.addCpeCasProcessor(aCasProcessor);
  }

  public void addCasProcessor(int index, CpeCasProcessor aCasProcessor)
          throws CpeDescriptorException {

    if (casProcessors == null) {
      casProcessors = CpeDescriptorFactory.produceCasProcessors();
    }
    casProcessors.addCpeCasProcessor(aCasProcessor, index);

  }

  public void setCpeConfiguration(CpeConfiguration aConfiguration) {
    cpeConfiguration = aConfiguration;
  }

  public CpeConfiguration getCpeConfiguration() throws CpeDescriptorException {
    return cpeConfiguration;
  }

  public void setCpeConfig(CpeConfiguration aConfiguration) {
    cpeConfiguration = aConfiguration;
  }

  public CpeConfiguration getCpeConfig() throws CpeDescriptorException {
    return cpeConfiguration;
  }

  public void setCheckpoint(String aCheckpointFile, int aFrequency) {
    if (cpeConfiguration != null) {
      try {
        cpeConfiguration.getCheckpoint().setFilePath(aCheckpointFile);
      } catch (Exception e) {
        e.printStackTrace();
      }
      cpeConfiguration.getCheckpoint().setFrequency(aFrequency, true);
    }
  }

  public void setDeployment(String aDeployMode) {
    if (cpeConfiguration != null) {
      try {
        cpeConfiguration.setDeployment(aDeployMode);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void setNumToProcess(long aEntityCount) {
    if (cpeConfiguration != null) {
      try {
        cpeConfiguration.setNumToProcess((int) aEntityCount);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void setStartingEntityId(String aStartEntityId) {
    if (cpeConfiguration != null) {
      try {
        cpeConfiguration.setStartingEntityId(aStartEntityId);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void setTimer(String aTimerClass) {
    if (cpeConfiguration != null) {
      try {
        cpeConfiguration.setCpeTimer(new CpeTimerImpl(aTimerClass));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public CpeResourceManagerConfiguration getResourceManagerConfiguration() {
    return resourceMgrConfig;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#readUnknownPropertyValueFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions,
   *      java.util.List)
   */
  protected void readUnknownPropertyValueFromXMLElement(Element aElement, XMLParser aParser,
          ParsingOptions aOptions, List aKnownPropertyNames) throws InvalidXMLException {
    if (aElement.getNodeName().equals("resourceManagerConfiguration")) {
      resourceMgrConfig = new CpeResourceManagerConfigurationImpl();
      resourceMgrConfig.buildFromXMLElement(aElement, aParser, aOptions);
    } else {
      super
              .readUnknownPropertyValueFromXMLElement(aElement, aParser, aOptions,
                      aKnownPropertyNames);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("cpeDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("allCollectionCollectionReaders", null),
              new PropertyXmlInfo("cpeCasProcessors", null),
              new PropertyXmlInfo("cpeConfig", null),
              new PropertyXmlInfo("cpeResourceManagerConfiguration", null),

          });

}
