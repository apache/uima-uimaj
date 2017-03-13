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

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;

/**
 * A <code>CollectionProcessingManager</code> (CPM) manages the application of an
 * {@link AnalysisEngine} to a collection of artifacts. For text analysis applications, this will be
 * a collection of documents. The analysis results will then be delivered to one ore more
 * {@link CasConsumer}s.
 * <p>
 * The CPM is configured with an Analysis Engine and CAS Consumers by calling its
 * {@link #setAnalysisEngine(AnalysisEngine)} and {@link #addCasConsumer(CasConsumer)} methods.
 * Collection processing is then initiated by calling the {@link #process(CollectionReader)} or
 * {@link #process(CollectionReader,int)} methods.
 * <p>
 * The <code>process</code> methods take a {@link CollectionReader} object as an argument. The
 * Collection Reader retrieves each artifact from the collection as a
 * {@link org.apache.uima.cas.CAS} object.
 * <p>
 * Listeners can register with the CPM by calling the
 * {@link #addStatusCallbackListener(StatusCallbackListener)} method. These listeners receive status
 * callbacks during the processing. At any time, performance and progress reports are available from
 * the {@link #getPerformanceReport()} and {@link #getProgress()} methods.
 * <p>
 * A CPM implementation may choose to implement parallelization of the processing, but this is not a
 * requirement of the architecture.
 * <p>
 * Note that a CPM only supports processing one collection at a time. Attempting to reconfigure a
 * CPM or start a new processing job while a previous processing job is occurring will result in a
 * {@link org.apache.uima.UIMA_IllegalStateException}. Processing multiple collections
 * simultaneously is done by instantiating and configuring multiple instances of the CPM.
 * <p>
 * A <code>CollectionProcessingManager</code> instance can be obtained by calling
 * {@link org.apache.uima.UIMAFramework#newCollectionProcessingManager()}.
 * 
 * 
 */
public interface CollectionProcessingManager {
  /**
   * Gets the <code>AnalysisEngine</code> that is assigned to this CPM.
   * 
   * @return the <code>AnalysisEngine</code> that this CPM will use to analyze each CAS in the
   *         collection.
   */
  public AnalysisEngine getAnalysisEngine();

  /**
   * Sets the <code>AnalysisEngine</code> that is assigned to this CPM.
   * 
   * @param aAnalysisEngine
   *          the <code>AnalysisEngine</code> that this CPM will use to analyze each CAS in the
   *          collection.
   * 
   * @throws ResourceConfigurationException
   *           if this CPM is currently processing
   */
  public void setAnalysisEngine(AnalysisEngine aAnalysisEngine)
          throws ResourceConfigurationException;

  /**
   * Gets the <code>CasConsumers</code>s assigned to this CPM.
   * 
   * @return an array of <code>CasConsumer</code>s
   */
  public CasConsumer[] getCasConsumers();

  /**
   * Adds a <code>CasConsumer</code> to this CPM.
   * 
   * @param aCasConsumer
   *          a <code>CasConsumer</code> to add
   * 
   * @throws ResourceConfigurationException
   *           if this CPM is currently processing
   */
  public void addCasConsumer(CasConsumer aCasConsumer) throws ResourceConfigurationException;

  /**
   * Removes a <code>CasConsumer</code> from this CPM.
   * 
   * @param aCasConsumer
   *          the <code>CasConsumer</code> to remove
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPM is currently processing
   */
  public void removeCasConsumer(CasConsumer aCasConsumer);

  /**
   * Gets whether this CPM is required to process the collection's elements serially (as opposed to
   * performing parallelization). Note that a value of <code>false</code> does not guarantee that
   * parallelization is performed; this is left up to the CPM implementation.
   * 
   * @return true if and only if serial processing is required
   */
  public boolean isSerialProcessingRequired();

  /**
   * Sets whether this CPM is required to process the collection's elements serially* (as opposed to
   * performing parallelization). If this method is not called,* the default is <code>false</code>.
   * Note that a value of <code>false</code> does not guarantee that parallelization is performed;
   * this is left up to the CPM implementation.
   * 
   * @param aRequired
   *          true if and only if serial processing is required
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPM is currently processing
   */
  public void setSerialProcessingRequired(boolean aRequired);

  /**
   * Gets whether this CPM will automatically pause processing if an exception occurs. If processing
   * is paused it can be resumed by calling the {@link #resume(boolean)} method.
   * 
   * @return true if and only if this CPM will pause on exception
   */
  public boolean isPauseOnException();

  /**
   * Sets whether this CPM will automatically pause processing if an exception occurs. If processing
   * is paused it can be resumed by calling the {@link #resume(boolean)} method.
   * 
   * @param aPause
   *          true if and only if this CPM should pause on exception
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPM is currently processing
   */
  public void setPauseOnException(boolean aPause);

  /**
   * Registers a listsner to receive status callbacks.
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
   * Initiates processing of a collection. CollectionReader initializes the CAS with Documents from
   * the Colection. This method starts the processing in another thread and returns immediately.
   * Status of the processing can be obtained by registering a listener with the
   * {@link #addStatusCallbackListener(StatusCallbackListener)} method.
   * <p>
   * A CPM can only process one collection at a time. If this method is called while a previous
   * processing request has not yet completed, a <code>UIMA_IllegalStateException</code> will
   * result. To find out whether a CPM is free to begin another processing request, call the
   * {@link #isProcessing()} method.
   * 
   * @param aCollectionReader
   *          the <code>CollectionReader</code> from which to obtain the Entities to be processed
   * 
   * @throws ResourceInitializationException
   *           if an error occurs during initialization
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPM is currently processing
   */
  public void process(CollectionReader aCollectionReader) throws ResourceInitializationException;

  /**
   * Initiates processing of a collection. This method works in the same way as
   * {@link #process(CollectionReader)}, but it breaks the processing up into batches of a size
   * determined by the <code>aBatchSize</code> parameter. Each {@link CasConsumer} will be
   * notified at the end of each batch.
   * 
   * @param aCollectionReader
   *          the <code>CollectionReader</code> from which to obtain the Entities to be processed
   * @param aBatchSize
   *          the size of the batch.
   * 
   * @throws ResourceInitializationException
   *           if an error occurs during initialization
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if this CPM is currently processing
   */
  public void process(CollectionReader aCollectionReader, int aBatchSize)
          throws ResourceInitializationException;

  /**
   * Determines whether this CPM is currently processing. This means that a processing request has
   * been submitted and has not yet completed or been {@link #stop()}ped. If processing is paused,
   * this method will still return <code>true</code>.
   * 
   * @return true if and only if this CPM is currently processing.
   */
  public boolean isProcessing();

  /**
   * Pauses processing. Processing can later be resumed by calling the {@link #resume(boolean)}
   * method.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if no processing is currently occurring
   */
  public void pause();

  /**
   * Determines whether this CPM's processing is currently paused.
   * 
   * @return true if and only if this CPM's processing is currently paused.
   */
  public boolean isPaused();

  /**
   * Resumes processing that has been paused.
   * 
   * @param aRetryFailed
   *          if processing was paused because an exception occurred (see
   *          {@link #setPauseOnException(boolean)}), setting a value of <code>true</code> for
   *          this parameter will cause the failed entity to be retried. A value of
   *          <code>false</code> (the default) will cause processing to continue with the next
   *          entity after the failure.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if processing is not currently paused
   */
  public void resume(boolean aRetryFailed);

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
}
