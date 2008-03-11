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

/**
 * Interface for a Listener that receives notification from the {@link BaseCPM} as various events
 * occur. Listeners will not generally implement this interface directly. Instead they will
 * implement {@link org.apache.uima.collection.StatusCallbackListener} or
 * {@link CasDataStatusCallbackListener}. Most UIMA developers will prefer to implement the former.
 * 
 * 
 */
public interface BaseStatusCallbackListener {
  /**
   * Called when the Collection Processing Manager's initialization has completed.
   */
  public void initializationComplete();

  /**
   * Called when the processing of a batch has completed.
   */
  public void batchProcessComplete();

  /**
   * Called when the processing of an entire collection has completed.
   */
  public void collectionProcessComplete();

  /**
   * Called when the processing has been paused.
   */
  public void paused();

  /**
   * Called when the processing has been resumed (after it had been previously paused).
   */
  public void resumed();

  /**
   * Called when the processing has been aborted.
   */
  public void aborted();
}
