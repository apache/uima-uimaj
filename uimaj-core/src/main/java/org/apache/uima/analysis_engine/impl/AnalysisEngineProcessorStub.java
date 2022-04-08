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
package org.apache.uima.analysis_engine.impl;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * A stub that calls a remote AnalysisEngine service.
 */
public interface AnalysisEngineProcessorStub {
  /**
   * Performs service call to retrieve resource meta data.
   * 
   * @return metadata for the Resource
   */
  ResourceMetaData getMetaData();

  /**
   * Performs service call to process an entity.
   * 
   * @param aCAS
   *          the CAS to process
   */
  void process(CAS aCAS) throws AnalysisEngineProcessException;

  /**
   * Notify the stub that all items in the batch have been processed.
   */
  default void batchProcessComplete() throws AnalysisEngineProcessException {
    // No action by default.
  }

  /**
   * Notify the stub that all items in the collection have been processed.
   */
  default void collectionProcessComplete() throws AnalysisEngineProcessException {
    // No action by default.
  }

  /**
   * Called when this stub is no longer needed, so resources can be cleaned up.
   */
  default void destroy() {
    // No action by default
  }
}
