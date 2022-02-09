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

package org.apache.uima.collection.impl.base_cpm.container;

import org.apache.uima.collection.base_cpm.CasProcessor;

/**
 * 
 * 
 * 
 */
public interface CasProcessorController {
  int NOTINITIALIZED = 0;

  int INITIALIZED = 1;

  int RUNNING = 2;

  int DISABLED = 3;

  int KILLED = 4;

  /**
   * Returns instance of CasProcessor
   * 
   * @return CasProcessor
   */
  CasProcessor getCasProcessor();

  /**
   * Returns status of CasProcessor
   * 
   * @return int status
   */
  int getStatus();

  /**
   * Sets status of CasProcessor
   * 
   * @param aStatus
   *          -
   */
  void setStatus(int aStatus);

  /**
   * Returns true if this is a Locally Deployed CasProcessor ( Same machine, different JVM )
   * 
   * @return true if Local, false otherwise
   */
  boolean isLocal();

  /**
   * Returns true if this is a Remotely Deployed CasProcessor
   * 
   * @return true if Remote, false otherwise
   */
  boolean isRemote();

  /**
   * Returns true if this is a Integrated CasProcessor
   * 
   * @return true if Integrated, false otherwise
   */
  boolean isIntegrated();

  /**
   * Returns true if CasProcessor can be Aborted/Disabled.
   * 
   * @return true if abortable, false otherwise
   */
  boolean isAbortable();

}
