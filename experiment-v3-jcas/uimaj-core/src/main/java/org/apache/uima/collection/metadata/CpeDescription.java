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

package org.apache.uima.collection.metadata;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

/**
 * Java representation of a Collection Processing Engine (CPE) XML descriptor. Generate an instance
 * of this class by calling either the
 * {@link org.apache.uima.util.XMLParser#parseCpeDescription(XMLInputSource)} or
 *        org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory#produceDescriptor(). A
 * CPE instance can then be created by calling
 * {@link org.apache.uima.UIMAFramework#produceCollectionProcessingEngine(CpeDescription)}.
 */
public interface CpeDescription extends MetaDataObject {

  public void addCollectionReader(CpeCollectionReader aCollectionReader)
          throws CpeDescriptorException;

  /**
   * Adds a path to the descriptor file containing CollectionReader's configuration.
   * The CPE supports only one CollectionReader instance.
   * <p>
   * This method causes the CPE descriptor to use the older &lt;include&gt; syntax.  To use the 
   * &lt;import&gt; syntax, you must use {@link #addCollectionReader(CpeCollectionReader)} instead.
   * 
   * @param aCollectionReaderPath -
   *          path to the CollectionReader descriptor.  A relative path is interpreted as
   *          relative to the current working directory.
   * @return {@link org.apache.uima.collection.metadata.CpeCollectionReader}
   * 
   * @throws CpeDescriptorException tbd
   */
  public CpeCollectionReader addCollectionReader(String aCollectionReaderPath)
          throws CpeDescriptorException;

  /**
   * Adds a path to the descriptor file containing CasInitializer's configuration.
   * 
   * @param aCasInitializerPath -
   *          path to the CasInitializer descriptor
   * @return {@link org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer}
   * 
   * @throws CpeDescriptorException tbd
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public CpeCollectionReaderCasInitializer addCasInitializer(String aCasInitializerPath)
          throws CpeDescriptorException;

  /**
   * Returns a list of {@link org.apache.uima.collection.metadata.CpeCollectionReader} instances
   * representing ALL defined CollectionReaders.
   * 
   * @return array of {@link org.apache.uima.collection.metadata.CpeCollectionReader} instances.
   * 
   * @throws CpeDescriptorException tbd
   */
  public CpeCollectionReader[] getAllCollectionCollectionReaders() throws CpeDescriptorException;

  public void setAllCollectionCollectionReaders(CpeCollectionReader[] readers)
          throws CpeDescriptorException;

  /**
   * Returns a {@link org.apache.uima.collection.metadata.CpeCasProcessors} instance containing
   * processing pipeline spec. This includes:
   * 
   * <ul>
   * <li> the size of the InputQueue
   * <li> the size of the OutputQueue
   * <li> number of processing units to create
   * <li> a list of Analysis Engines
   * <li> a list of CasConsumers
   * </ul>
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeCasProcessors}
   * 
   * @throws CpeDescriptorException tbd
   */
  public CpeCasProcessors getCpeCasProcessors() throws CpeDescriptorException;

  /**
   * Appends a instance of {@link org.apache.uima.collection.metadata.CpeCasProcessor} to the end of
   * the list containing CPE CasProcessors.
   * 
   * A CasProcessor can either be:
   * <ul>
   * <li> Analysis Engine
   * <li> Cas Consumer
   * </ul>
   * 
   * @param aCasProcessor -
   *          instance of {@link org.apache.uima.collection.metadata.CpeCasProcessor} to add.
   * @throws CpeDescriptorException tbd
   */
  public void addCasProcessor(CpeCasProcessor aCasProcessor) throws CpeDescriptorException;

  /**
   * Adds a instance of {@link org.apache.uima.collection.metadata.CpeCasProcessor} at a specified
   * location in the list of CPE CasProcessors. If the index is greater than the list size, the new
   * {@link org.apache.uima.collection.metadata.CpeCasProcessor} instance is appended to the list.
   * 
   * @param index -
   *          insertion point for the {@link org.apache.uima.collection.metadata.CpeCasProcessor}
   * @param aCasProcessor -
   *          CasProcessor to add
   * 
   * @throws CpeDescriptorException tbd
   */
  public void addCasProcessor(int index, CpeCasProcessor aCasProcessor)
          throws CpeDescriptorException;

  /**
   * Returns the CPE configuration that includes:
   * <ul>
   * <li> An ID of the entity to begin processing with (OPTIONAL)
   * <li> Number of entities to process
   * <li> Checkpoint definition (checkpoint file, frequency)
   * <li> A name of the class implementing {@link org.apache.uima.util.UimaTimer} interface.
   * <li> Startup mode for the CPE (immediate, interactive, vinciService)
   * </ul>
   * <p>
   * Using an instance of {@link org.apache.uima.collection.metadata.CpeConfiguration} the client
   * may change behavior of the CPE and corresponding to each of the elements in the above list.
   * </p>
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeConfiguration}
   * 
   * @throws CpeDescriptorException tbd
   */
  public CpeConfiguration getCpeConfiguration() throws CpeDescriptorException;

  /**
   * Defines the size for the InputQueue. This queue is used by the CPE to store bundles of CAS as
   * read by a CollectionReader. The queue is shared between the CollectionReader and
   * ProcessingUnits. The larger the size of this queue the more bundles of CAS are placed in the
   * queue, and the more memory is consumed by the CPE. The right size for this queue depends on
   * number of factors, like the speed of analysis and available memory.
   * 
   * @param aSize -
   *          size of the queue
   * 
   * @throws CpeDescriptorException tbd
   */
  public void setInputQueueSize(int aSize) throws CpeDescriptorException;

  /**
   * 
   * @param aSize the number of threads
   * @throws CpeDescriptorException tbd
   */
  public void setProcessingUnitThreadCount(int aSize) throws CpeDescriptorException;

  /**
   * Defines the size for the OutputQueue. This queue is used by the CPE to store bundles of CAS
   * containing results of analysis. The queue is shared between ProcessingUnits and CasConsumers.
   * The larger the size of this queue the more bundles of CAS are placed in the queue, and the more
   * memory is consumed by the CPE. The right size for this queue depends on number of factors, like
   * the speed in which Cas's are consumed and available memory.
   * 
   * @param aSize -
   *          size of the queue
   * 
   * @throws CpeDescriptorException tbd
   */
  public void setOutputQueueSize(int aSize) throws CpeDescriptorException;

  /**
   * Add checkpoint file and frequency (in millis) of checkpoints
   * 
   * @param aCheckpointFile -
   *          path for the checkpoint file
   * @param aFrequency -
   *          frequency in terms of mills for checkpoints
   */
  public void setCheckpoint(String aCheckpointFile, int aFrequency);

  /**
   * Add name of the class that implements (@link org.apache.uima.util.UimaTimer} interface. This
   * timer will be used by the CPE to time events.
   * 
   * @param aTimerClass -
   *          name of the UimaTimer class
   */
  public void setTimer(String aTimerClass);

  /**
   * Define startup mode for the CPE. The three supported options are:
   * <ul>
   * <li> immediate (DEFAULT), starts the CPE without user interaction
   * <li> interactive - allows to the user to control the start, stop, pause, resume of the CPE.
   * <li> vinciService - starts the CPM as a Vinci Service
   * </ul>
   * 
   * @param aDeployMode -
   *          CPM deployment mode
   */
  public void setDeployment(String aDeployMode);

  /**
   * Defines number of entities to process by the CPE.
   * 
   * @param aEntityCount -
   *          entity count
   */
  public void setNumToProcess(long aEntityCount);

  /**
   * Defines an id of the first entity to process.
   * 
   * @param aStartEntityId -
   *          entity id
   */
  public void setStartingEntityId(String aStartEntityId);

  /**
   * Defines the path to Resource Manager Configuration
   * 
   * @param aResMgrConfPagth -
   *          path to Resource Manager Configuration file.
   */
  public void setResourceManagerConfiguration(String aResMgrConfPagth);

  /**
   * Defines the path to Resource Manager Configuration
   * 
   * @param aResMgrConfPagth -
   *          path to Resource Manager Configuration file.
   */
  public void setCpeResourceManagerConfiguration(CpeResourceManagerConfiguration aResMgrConfPagth);

  /**
   * Returns ResourceManagerConfiguration instance.
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeResourceManagerConfiguration}
   */
  public CpeResourceManagerConfiguration getResourceManagerConfiguration();

  public void setCpeCasProcessors(CpeCasProcessors aCasProcessors);

  /**
   * Generates XML for the CPE Descriptor and writes it out to the provided OutputStream.
   * 
   * @param aStream -
   *          stream to write
   */
  public void toXML(OutputStream aStream) throws SAXException, IOException;

  public void setCpeConfiguration(CpeConfiguration aConfiguration);
}
