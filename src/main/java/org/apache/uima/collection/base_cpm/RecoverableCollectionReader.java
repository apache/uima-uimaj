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

import org.apache.uima.collection.CollectionException;

/**
 * Interface facilitating CollectionReader recovery from failures. Any CollectionReader supporting
 * recovery must implement this interface.
 * 
 * 
 * 
 */
public interface RecoverableCollectionReader {
  /**
   * Start CollectionReader recovery to a given synch point
   * 
   * @param aSynchPoint -
   *          contains recovery information
   * @throws CollectionException passthru
   */
  public void moveTo(SynchPoint aSynchPoint) throws CollectionException;

  /**
   * Retrieves data facilitating recovery of the CollectionReader
   * 
   * @return the SynchPoint for the current position of the CollectionReader
   */
  public SynchPoint getSynchPoint();
}
