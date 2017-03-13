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

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object that holds configuration that is part of the CPE descriptor. It provides the means of
 * configuring the CPE Processing Pipeline and to tweak performance charactristics of the CPE. It
 * contains a list of CasProcessors that will be strung together into a pipeline by the CPE.
 * 
 * 
 */
public interface CpeCasProcessors extends MetaDataObject {
  /**
   * Sets the size of the OutputQueue. This queue is shared among Processing Units and CasConsumers
   * and contains bundles of CAS. Processing Units add bundles to the queue while CasConsumers
   * consume them. The best size for this queue is determined by overall performance of the
   * ProcessingUnit ( how fast it deposits bundles onto the queue) and memory availability. The
   * larger the queue the more bundles (hence memory) is used.
   * 
   * @param aOutputQueueSize -
   *          Output queue size
   * @throws CpeDescriptorException tbd
   */
  public void setOutputQueueSize(int aOutputQueueSize) throws CpeDescriptorException;

  /**
   * Returns the size of the OutputQueue. This queue is shared among Processing Units and
   * CasConsumers and contains bundles of CAS. Processing Units add bundles to the queue while
   * CasConsumers consume them. The best size for this queue is determined by overall performance of
   * the ProcessingUnit ( how fast it deposits bundles onto the queue) and memory availability. The
   * larger the queue the more bundles (hence memory) is used.
   * 
   * @return - output queue size
   */
  public int getOutputQueueSize();

  /**
   * Sets the size of the InputQueue. This queue is shared among CollectionReader and Processing
   * Units and contains bundles of CAS. CollectionReader adds bundles to the queue while Processing
   * Unit consume them. The best size for this queue is determined by overall performance of the
   * ProcessingUnit ( how fast it takes bundles off the queue) and memory availability. The larger
   * the queue the more bundles (hence memory) is used.
   * 
   * @param aOutputQueueSize -
   *          queue size
   * @throws CpeDescriptorException tbd
   */
  public void setInputQueueSize(int aOutputQueueSize) throws CpeDescriptorException;

  /**
   * Returns size of the InputQueue. This queue is shared among CollectionReader and Processing
   * Units and contains bundles of CAS. CollectionReader adds bundles to the queue while Processing
   * Unit consume them. The best size for this queue is determined by overall performance of the
   * ProcessingUnit ( how fast it takes bundles off the queue) and memory availability. The larger
   * the queue the more bundles (hence memory) is used.
   * 
   * @return - queue size
   */
  public int getInputQueueSize();

  /**
   * Sets ProcessingUnit replication. Each ProcessingUnit contains the same sequence of
   * CasProcessors and runs in a seperate thread. On platforms containing more than one CPU,
   * replicating ProcessingUnit may result in better performance.
   * 
   * @param aConcurrentPUCount -
   *          number of ProcessingUnits(processing threads)
   * @throws CpeDescriptorException tbd
   */
  public void setConcurrentPUCount(int aConcurrentPUCount) throws CpeDescriptorException;

  /**
   * Returns number of ProcessingUnits. Each ProcessingUnit contains the same sequence of
   * CasProcessors and runs in a seperate thread. On platforms containing more than one CPU,
   * replicating ProcessingUnit may result in better performance.
   * 
   * @return - number of ProcessingUnits(processing threads)
   */
  public int getConcurrentPUCount();

  /**
   * Inserts a new CasProcessor at an indicated position.
   * 
   * @param aCasProcessor -
   *          CasProcessor to add
   * @param aInsertPosition -
   *          position where to insert the CasProcessor
   * @throws CpeDescriptorException tbd
   */
  public void addCpeCasProcessor(CpeCasProcessor aCasProcessor, int aInsertPosition)
          throws CpeDescriptorException;

  /**
   * Appends new CasProcessor to existing list of CasProcessors
   * 
   * @param aCasProcessor -
   *          CasProcessor to add
   * @throws CpeDescriptorException tbd
   */
  public void addCpeCasProcessor(CpeCasProcessor aCasProcessor) throws CpeDescriptorException;

  /**
   * Returns {@link org.apache.uima.collection.metadata.CpeCasProcessor} found at given position.
   * 
   * @param aPosition -
   *          position of the CasProcessor
   * @return - {@link org.apache.uima.collection.metadata.CpeCasProcessor}
   * @throws CpeDescriptorException tbd
   */
  public CpeCasProcessor getCpeCasProcessor(int aPosition) throws CpeDescriptorException;

  /**
   * Returns ALL {@link org.apache.uima.collection.metadata.CpeCasProcessor} objects in processing
   * pipeline.
   * 
   * @return array of {@link org.apache.uima.collection.metadata.CpeCasProcessor}
   * @throws CpeDescriptorException tbd
   */
  public CpeCasProcessor[] getAllCpeCasProcessors() throws CpeDescriptorException;

  /**
   * Removes {@link org.apache.uima.collection.metadata.CpeCasProcessor} object from processing
   * pipeline from a given position.
   * 
   * @param aPosition -
   *          position of the CasProcessor in the pipeline
   * @throws CpeDescriptorException tbd
   */
  public void removeCpeCasProcessor(int aPosition) throws CpeDescriptorException;

  /**
   * Removes ALL {@link org.apache.uima.collection.metadata.CpeCasProcessor} objects from processing
   * pipeline.
   * 
   * @throws CpeDescriptorException tbd
   */
  public void removeAllCpeCasProcessors() throws CpeDescriptorException;

  public void setPoolSize(int aPoolSize) throws CpeDescriptorException;

  public int getCasPoolSize();

  public boolean getDropCasOnException();
}
