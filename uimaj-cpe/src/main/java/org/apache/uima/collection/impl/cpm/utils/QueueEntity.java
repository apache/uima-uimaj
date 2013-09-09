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

package org.apache.uima.collection.impl.cpm.utils;

/**
 * Convenience wrapper that is internally used by the CPM. Created in the OutputQueue this wrapper
 * contains the CAS and additional information needed to determine if the CAS contained has arrived
 * later than expected Normally the CAS would be marked as NOT timed out. In case of chunking, the
 * CAS may come after a timeout occurs. In this case the CPM needs to know this and take appropriate
 * action.
 * 
 */
public class QueueEntity {
  private boolean timedOut = false;

  private Object entity = null;

  /**
   * Initialize the instance with the Entity (CAS) and the timeout
   */
  public QueueEntity(Object anEntity, boolean hasTimedOut) {
    timedOut = hasTimedOut;
    entity = anEntity;
  }

  /**
   * @return the entity
   */
  public Object getEntity() {
    return entity;
  }

  /**
   * @return true if timed out
   */
  public boolean isTimedOut() {
    return timedOut;
  }

}
