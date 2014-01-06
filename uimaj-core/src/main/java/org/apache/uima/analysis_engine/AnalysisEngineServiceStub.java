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

package org.apache.uima.analysis_engine;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceServiceStub;

/**
 * A stub that calls a remote AnalysisEngine service.
 * 
 * 
 */
public interface AnalysisEngineServiceStub extends ResourceServiceStub {
  /**
   * Performs service call to process an entity.
   * 
   * @param aCAS
   *          the CAS to process
   * @throws ResourceServiceException tbd         
   */
  public abstract void callProcess(CAS aCAS) throws ResourceServiceException;

  /**
   * Performs service call to inform the AnalysisEngine that the processing of a batch has been
   * completed.
   * @throws ResourceServiceException tbd        
   */
  public abstract void callBatchProcessComplete() throws ResourceServiceException;

  /**
   * Performs service call to inform the AnalysisEngine that the processing of a collection has been
   * completed.
   * @throws ResourceServiceException tbd         
   */
  public abstract void callCollectionProcessComplete() throws ResourceServiceException;
}
