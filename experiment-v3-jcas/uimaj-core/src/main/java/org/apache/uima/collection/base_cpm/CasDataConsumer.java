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
import java.util.Map;

import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.XMLInputSource;

/**
 * Any component that operates on analysis results produced by a UIMA CasDataProcessor.
 * CASDataConsumers may be registered with a
 * {@link org.apache.uima.collection.CollectionProcessingManager} (CPM). During collection
 * processing, the CPM will pass each CasData from CasProcessor to each consumer's process method.
 * The CAS consumer can do anything it wants with the CASes it receives; commonly CAS consumers will
 * build aggregate data structures such as search engine indexes or glossaries.
 * <p>
 * The CPM will also call each CAS Consumer's {@link #batchProcessComplete(ProcessTrace)} method at
 * the end of each batch and their {@link #collectionProcessComplete(ProcessTrace)} method called at
 * the end of the collection.
 * <p>
 * <code>CasConsumer</code>s are also {@link ConfigurableResource}s, and can be instantiated
 * from descriptors. See
 * {@link org.apache.uima.util.XMLParser#parseCasConsumerDescription(XMLInputSource)} and
 * {@link org.apache.uima.UIMAFramework#produceCasConsumer(ResourceSpecifier,Map)} for more
 * information.
 * 
 * 
 */
public interface CasDataConsumer extends ConfigurableResource, CasDataProcessor {
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
