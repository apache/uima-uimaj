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

package org.apache.uima.collection.base_cpm;

import java.io.IOException;

import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.ProcessTrace;

/**
 * Base interface for a component that can process a CAS. This includes both components that modify
 * the CAS (such as an {@link org.apache.uima.analysis_engine.AnalysisEngine} as well as those that
 * do not (such as a {@link org.apache.uima.collection.CasConsumer}). Whether the CasProcessor
 * modifies the CAS can be determined by calling its {@link #isReadOnly()} method.
 * <p>
 * <code>CasProcessor</code>s can be "plugged into" the
 * {@link org.apache.uima.collection.CollectionProcessingManager}.
 * <p>
 * CAS Processors should not directly implement this interface; instead they should implement one of
 * its subinterfaces:
 * <ul>
 * <li>{@link CasDataProcessor} - for CAS processors that want to interact directly with the
 * {@link org.apache.uima.cas_data.CasData}. This works best for simple processors that do not need
 * the indexing or strong typing features provided by the CAS container.</li>
 * <li>{@link CasObjectProcessor} - for CAS processors that want to use the full
 * {@link org.apache.uima.cas.CAS} implementation.</li>
 * </ul>
 * Analysis Engines and CAS Consumers implement {@link CasObjectProcessor}.
 * <p>
 * All CAS processors must publish their metadata via the {@link #getProcessingResourceMetaData()}
 * method.
 * 
 * 
 */
public interface CasProcessor {
  /**
   * Gets whether this is a stateless CAS Processor. Stateless CAS Processors do not maintain any
   * data between calls to their process methods.
   * 
   * @return true if this CAS processor is stateless, false if it is stateful.
   */
  public boolean isStateless();

  /**
   * Gets whether this is a read-only CAS Processor, which does not modify the CAS.
   * 
   * @return true if this CAS processor does not modify the CAS, false if it does.
   */
  public boolean isReadOnly();

  /**
   * Gets the metadata that describes this <code>CasProcesor</code>.
   * 
   * @return an object containing all metadata for this CasProcessor
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData();

  /**
   * Completes the processing of a batch. A collection may be divided into one or more batches - it
   * is up to the CollectionProcessingManager or the application to determine the number and size of
   * batches.
   * 
   * @param aTrace
   *          an object that records information, such as timing, about this method's execution.
   * 
   * @throws ResourceProcessException
   *           if an exception occurs during processing
   * @throws IOException
   *           if an I/O failure occurs
   */
  public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException;

  /**
   * Completes the processing of an entire collection.
   * 
   * @param aTrace
   *          an object that records information, such as timing, about this method's execution.
   * 
   * @throws ResourceProcessException
   *           if an exception occurs during processing
   * @throws IOException
   *           if an I/O failure occurs
   */
  public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException;

}
