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

package org.apache.uima.resource;

import java.io.Serializable;

/**
 * An object that encapsulates all conversational state between a UIMA {@link Resource} and a
 * particular client. In a local deployment, there will probably be only one session. Distributed
 * deployments often have multiple sessions. If multiple sessions are in use, it is the
 * application's or service wrapper's responsibility to make sure that a Resource's
 * <code>Session</code> object is properly set up prior to invoking any of that Resource's
 * methods.
 * <p>
 * Note that a particular component, such as an annotator, may get a handle to a Session object that
 * actually represents a particular namespace within a larger Session. This allows each component to
 * use arbitrary keys for storing information in the session without risking name collisions.
 * 
 * 
 */
public interface Session extends Serializable {
  /**
   * Stores an object in the Session
   * @param aKey Key
   * @param aValue Value
   */
  public void put(String aKey, Object aValue);

  /**
   * Gets an object from the Session
   * @param aKey Key  
   * @return the associated value
   */
  public Object get(String aKey);
}
