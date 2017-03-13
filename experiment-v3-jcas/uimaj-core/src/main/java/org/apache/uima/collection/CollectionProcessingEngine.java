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

package org.apache.uima.collection;

import java.util.Map;

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;

/**
 * A <code>CollectionProcessingEngine</code> (CPE) processes a collection of artifacts (for text
 * analysis applications, this will be a collection of documents) and produces collection-level
 * results.
 * <p>
 * A CPE consists of a {@link org.apache.uima.collection.CollectionReader}, zero or more
 * {@link org.apache.uima.analysis_engine.AnalysisEngine}s and zero or more
 * {@link org.apache.uima.collection.CasConsumer}s. The Collection Reader is responsible for
 * reading artifacts from a collection and setting up the CAS. The AnalysisEngines analyze each CAS
 * and the results are passed on to the CAS Consumers. CAS Consumers perform analysis over multiple
 * CASes and generally produce collection-level results in some application-specific data structure.
 * <p>
 * Processing is started by calling the {@link #process()} method. Processing can be controlled via
 * the{@link #pause()}, {@link #resume()}, and {@link #stop()} methods.
 * <p>
 * Listeners can register with the CPE by calling the
 * {@link #addStatusCallbackListener(StatusCallbackListener)} method. These listeners receive status
 * callbacks during the processing. At any time, performance and progress reports are available from
 * the {@link #getPerformanceReport()} and {@link #getProgress()} methods.
 * <p>
 * A CPE implementation may choose to implement parallelization of the processing, but this is not a
 * requirement of the architecture.
 * <p>
 * Note that a CPE only supports processing one collection at a time. Attempting to start a new
 * processing job while a previous processing job is running will result in an exception. Processing
 * multiple collections simultaneously is done by instantiating and configuring multiple instances
 * of the CPE.
 * <p>
 * A <code>CollectionProcessingEngine</code> instance can be obtained by calling
 * {@link org.apache.uima.UIMAFramework#produceCollectionProcessingEngine(CpeDescription)}.
 * 
 * 
 */
public interface CollectionProcessingEngine {
  /**
   * Initializes this CPE from a <code>cpeDescription</code> Applications do not need to call this
   * method. It is called automatically by the framework and cannot be called a second time.
   * 
   * @param aCpeDescription
   *          CPE description, generally parsed from an XML file
   * @param aAdditionalParams
   *          a Map containing additional parameters. May be <code>null</code> if there are no
   *          parameters. Each class that implements this interface can decide what additional
   *          parameters it supports.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization.
   * @throws UIMA_IllegalStateException
   *           if this method is called more than once on a single instance.
   */
  public void initialize(CpeDescription aCpeDescription, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException;

  /**
   * Registers a listener to receive status callbacks.
   * 
   * @param aListener
   *          the listener to add
   */
  public void addStatusCallbackListener(StatusCallbackListener aListener);

  /**
   * Unregisters a status callback listener.
   * 
   * @param aListener
   *          the listener to remove
   */
  public void removeStatusCallbackListener(StatusCallbackListener aListener);

  /**
   * Initiates processing of a collection. This method starts the processing in another thread and
   * returns immediately. Status of the processing can be obtained by registering a listener with
   * the {@link #addStatusCallbackListener(StatusCallbackListener)} method.
   * <p>
   * A CPE can only process one collection at a time. If this method is called while a previous
   * processing request has not yet completed, a <code>UIMA_IllegalStateException</code> will
   * result. To find out whether a CPE is free to begin another processing request, call the
   * {@link #isProcessing()} method.
   * 
   * @throws ResourceInitializationException
   *           if an error occurs during initialization
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPE is currently processing
   */
  public void process() throws ResourceInitializationException;

  /**
   * Determines whether this CPE is currently processing. This means that a processing request has
   * been submitted and has not yet completed or been {@link #stop()}ped. If processing is paused,
   * this method will still return <code>true</code>.
   * 
   * @return true if and only if this CPE is currently processing.
   */
  public boolean isProcessing();

  /**
   * Pauses processing. Processing can later be resumed by calling the {@link #resume()} method.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if no processing is currently occuring
   */
  public void pause();

  /**
   * Determines whether this CPE's processing is currently paused.
   * 
   * @return true if and only if this CPE's processing is currently paused.
   */
  public boolean isPaused();

  /**
   * Resumes processing that has been paused.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if processing is not currently paused
   */
  public void resume();

  /**
   * Stops processing.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if no processing is currently occuring
   */
  public void stop();

  /**
   * Gets a performance report for the processing that is currently occurring or has just completed.
   * 
   * @return an object containing performance statistics
   */
  public ProcessTrace getPerformanceReport();

  /**
   * Gets a progress report for the processing that is currently occurring or has just completed.
   * 
   * @return an array of <code>Progress</code> objects, each of which represents the progress in a
   *         different set of units (for example number of entities or bytes)
   */
  public Progress[] getProgress();

  /**
   * Gets the Collection Reader for this CPE.
   * 
   * @return the collection reader
   */
  public BaseCollectionReader getCollectionReader();

  /**
   * Gets the <code>CasProcessors</code>s in this CPE, in the order in which they will be
   * executed.
   * 
   * @return an array of <code>CasProcessor</code>s
   */
  public CasProcessor[] getCasProcessors();

  /**
   * Kill CPM hard.
   * 
   */
  public void kill();

}
