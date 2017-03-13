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

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;

/**
 * Interface for a Listener that receives notification from the {@link CollectionProcessingManager}
 * as various events occur. The most common event is that the processing of an entity has completed.
 * 
 * 
 */
public interface StatusCallbackListener extends BaseStatusCallbackListener {
  /**
   * Called when the processing of each entity has completed.
   * 
   * @param aCas
   *          the CAS containing the processed entity and the analysis results
   * @param aStatus
   *          the status of the processing. This object contains a record of any Exception that
   *          occurred, as well as timing information.
   */
  public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus);
}
