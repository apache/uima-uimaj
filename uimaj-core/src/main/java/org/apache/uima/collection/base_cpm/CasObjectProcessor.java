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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

/**
 * Interface for CAS Processors that want to use the full {@link CAS} implementation.
 * 
 * 
 */
public interface CasObjectProcessor extends CasProcessor {
  /**
   * Process a single CAS.
   * 
   * @param aCAS
   *          the CAS to be processed. Additional information may be added to this CAS (if this CAS
   *          processor is not {@link #isReadOnly() read-only}).
   * 
   * @throws ResourceProcessException
   *           if processing fails
   */
  public void processCas(CAS aCAS) throws ResourceProcessException;

  /**
   * Processes multiple CASes.
   * 
   * @param aCASes
   *          an array of CASes to be processed. Additional information may be added to these CASes
   *          (if this CAS processor is not {@link #isReadOnly() read-only}).
   * 
   * @throws ResourceProcessException
   *           if processing fails for any of the CASes
   */
  public void processCas(CAS[] aCASes) throws ResourceProcessException;

  /**
   * Informs this CasConsumer that the CAS TypeSystem has changed. The CPM calls this method prior
   * to initiating collection processing, and will call it again whenever the CAS TypeSystem
   * changes.
   * <p>
   * In this method, the CasConsumer should use the {@link TypeSystem} to resolve the names of Type
   * and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used during processing.
   * 
   * @param aTypeSystem the type system to use
   * @throws ResourceInitializationException
   *           if the type system is not compatible with this Cas Consumer
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException;

}
