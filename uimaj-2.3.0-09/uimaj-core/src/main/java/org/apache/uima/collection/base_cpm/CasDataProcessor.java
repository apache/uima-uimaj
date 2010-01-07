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

import org.apache.uima.cas_data.CasData;
import org.apache.uima.resource.ResourceProcessException;

/**
 * Interface for CAS processors that want to interact directly with the {@link CasData}. This works
 * best for simple processors that do not need the indexing or strong typing features provided by
 * the CAS container.
 * 
 * 
 */
public interface CasDataProcessor extends CasProcessor {
  /**
   * Process a single CasData.
   * 
   * @param aCAS
   *          the input CasData
   * 
   * @return the output CasData (if this CAS processor is {@link #isReadOnly() read-only}, this
   *         will always be equivalent to <code>aCAS</code>.
   * 
   * @throws ResourceProcessException
   *           if processing fails
   */
  public CasData process(CasData aCAS) throws ResourceProcessException;

  /**
   * Process multiple CasData objects.
   * 
   * @param aCASes
   *          the input CasData objects
   * 
   * @return the output CasData objects (if this CAS processor is {@link #isReadOnly() read-only},
   *         these will always be equivalent to <code>aCASes</code>.
   * 
   * @throws ResourceProcessException
   *           if processing fails
   */
  public CasData[] process(CasData[] aCASes) throws ResourceProcessException;
}
