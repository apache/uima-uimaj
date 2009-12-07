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

import java.io.Serializable;
import java.util.List;

import org.apache.uima.util.ProcessTrace;

/**
 * Contains information about the successful or unsuccessful processing of an entity (an element of
 * a collection) by the {@link CollectionProcessingManager}.
 * 
 * 
 */
public interface EntityProcessStatus extends Serializable {
  /**
   * Gets whether an exception occurred.
   * 
   * @return true if an exception occurred, false if processing completely successfully with no
   *         exceptions.
   */
  public boolean isException();

  /**
   * Gets a message describing the status of the Entity's processing.
   * 
   * @return the status message
   */
  public String getStatusMessage();

  /**
   * Gets the List of Exceptions that occurred during processing of the Entity.
   * 
   * @return the List of Exceptions, <code>null</code> if none
   */
  public List<Exception> getExceptions();

  /**
   * Gets the name of the components in which Exceptions (if any) occurred. These could be the
   * Analysis Engine or one or more of the CasConsumers.
   * 
   * @return the name of the components that failed, <code>null</code> if there was no failure
   */
  public List<String> getFailedComponentNames();

  /**
   * Gets the <code>ProcessTrace</code> object for the Entity's processing. The
   * <code>ProcessTrace</code> object contains a record of each component involved in the
   * processing and how much time that component took to complete its processing.
   * 
   * @return the object containing trace and timing information for the Entity's processing.
   */
  public ProcessTrace getProcessTrace();

  /**
   * Gets whether an entity has beed skipped during processing
   * 
   * @return true if an entity was skipped, false otherwise
   */
  public boolean isEntitySkipped();

}
