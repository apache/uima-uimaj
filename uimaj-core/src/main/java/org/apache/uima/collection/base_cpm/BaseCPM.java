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

import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;

/**
 * The Base CPM interface is a lower-level interface to the Collection Processing Manager. It is
 * recommended that developers use the {@link org.apache.uima.collection.CollectionProcessingEngine}
 * and {@link org.apache.uima.collection.metadata.CpeDescription} interfaces instead.
 * <p>
 * The CPM is configured with a list of {@link CasProcessor}s by calling its
 * {@link #addCasProcessor(CasProcessor)} method. A single {@link BaseCollectionReader} must be
 * provided, via the {@link #setCollectionReader(BaseCollectionReader)} method. Collection
 * processing is then initiated by calling the {@link #process()} method.
 * <p>
 * Listeners can register with the CPM by calling the
 * {@link #addStatusCallbackListener(BaseStatusCallbackListener)} method. These listeners receive
 * status callbacks during the processing. At any time, performance and progress reports are
 * available from the {@link #getPerformanceReport()} and {@link #getProgress()} methods.
 * <p>
 * A CPM implementation may choose to implement parallelization of the processing, but this is not a
 * requirement of the architecture.
 * <p>
 * Note that a CPM only supports processing one collection at a time. Attempting to reconfigure a
 * CPM or start a new processing job while a previous processing job is occurring will result in a
 * {@link org.apache.uima.UIMA_IllegalStateException}. Processing multiple collections
 * simultaneously is done by instantiating and configuring multiple instances of the CPM.
 */
public interface BaseCPM {
  /**
   * Only used for alternate CasData forms of the CAS (not used in this UIMA SDK release). Name of
   * CasData CAS type that holds document text. When creating CasData forms of the CAS, a feature
   * structure of this type must be created by the collection reader.
   */
  public static final String DOCUMENT_TEXT_TYPE = "uima.cpm.DocumentText";

  /**
   * Only used for alternate CasData forms of the CAS (not used in this UIMA SDK release). Name of
   * CAS feature (on DOCUMENT_TEXT_TYPE feature structure) that holds document text. When creating
   * CasDta forms of the CAS, this feature must be set by the collection reader.
   */
  public static final String DOCUMENT_TEXT_FEATURE = "Text";

  /**
   * Gets the Collection Reader for this CPM.
   * 
   * @return the collection reader
   */
  public BaseCollectionReader getCollectionReader();

  /**
   * Sets the Collection Reader for this CPM.
   * 
   * @param aCollectionReader
   *          the collection reader
   */
  public void setCollectionReader(BaseCollectionReader aCollectionReader);

  /**
   * Gets the <code>CasProcessors</code>s assigned to this CPM, in the order in which they will
   * be called by the CPM.
   * 
   * @return an array of <code>CasProcessor</code>s
   */
  public CasProcessor[] getCasProcessors();

  /**
   * Adds a <code>CasProcessor</code> to this CPM's list of consumers. The new CasProcessor will
   * be added to the end of the list of CAS Processors.
   * 
   * @param aCasProcessor
   *          a <code>CasProcessor</code> to add
   * 
   * @throws ResourceConfigurationException
   *           if this CPM is currently processing
   */
  public void addCasProcessor(CasProcessor aCasProcessor) throws ResourceConfigurationException;

  /**
   * Adds a <code>CasProcessor</code> to this CPM's list of consumers. The new CasProcessor will
   * be added at the specified index.
   * 
   * @param aCasProcessor
   *          the CasProcessor to add
   * @param aIndex
   *          the index at which to add the CasProcessor
   * 
   * @throws ResourceConfigurationException
   *           if this CPM is currently processing
   */
  public void addCasProcessor(CasProcessor aCasProcessor, int aIndex)
          throws ResourceConfigurationException;

  /**
   * Removes a <code>CasProcessor</code> to this CPM's list of consumers.
   * 
   * @param aCasProcessor
   *          the <code>CasProcessor</code> to remove
   * 
   */
  public void removeCasProcessor(CasProcessor aCasProcessor);

  /**
   * Disables a <code>CasProcessor</code> in this CPM's list of CasProcessors.
   * 
   * @param aCasProcessorName
   *          the name of the <code>CasProcessor</code> to disable
   * 
   */
  public void disableCasProcessor(String aCasProcessorName);

  /**
   * Gets whether this CPM is required to process the collection's elements serially (as opposed to
   * performing parallelization). Note that a value of <code>false</code> does not guarantee that
   * parallelization is performed; this is left up to the CPM implementation.
   * 
   * @return true if and only if serial processing is required
   */
  public boolean isSerialProcessingRequired();

  /**
   * Sets whether this CPM is required to process the collection's elements serially (as opposed to
   * performing parallelization). If this method is not called, the default is <code>false</code>.
   * Note that a value of <code>false</code> does not guarantee that parallelization is performed;
   * this is left up to the CPM implementation.
   * 
   * @param aRequired
   *          true if and only if serial processing is required
   * 
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
   */
  public void setPauseOnException(boolean aPause);

  /**
   * Registers a listsner to receive status callbacks.
   * 
   * @param aListener
   *          the listener to add
   */
  public void addStatusCallbackListener(BaseStatusCallbackListener aListener);

  /**
   * Unregisters a status callback listener.
   * 
   * @param aListener
   *          the listener to remove
   */
  public void removeStatusCallbackListener(BaseStatusCallbackListener aListener);

  /**
   * Initiates processing of a collection. This method starts the processing in another thread and
   * returns immediately. Status of the processing can be obtained by registering a listener with
   * the {@link #addStatusCallbackListener(BaseStatusCallbackListener)} method.
   * <p>
   * A CPM can only process one collection at a time. If this method is called while a previous
   * processing request has not yet completed, a <code>UIMA_IllegalStateException</code> will
   * result. To find out whether a CPM is free to begin another processing request, call the
   * {@link #isProcessing()} method.
   * 
   * @throws ResourceInitializationException
   *           if an error occurs during initialization
   */
  public void process() throws ResourceInitializationException;

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
   */
  public void resume(boolean aRetryFailed);

  /**
   * Resumes processing that has been paused.
   * 
   */
  public void resume();

  /**
   * Stops processing.
   * 
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
