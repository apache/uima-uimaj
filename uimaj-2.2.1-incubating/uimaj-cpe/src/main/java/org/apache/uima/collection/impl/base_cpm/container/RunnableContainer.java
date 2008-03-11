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

/**
 * The <code>RunnableContainer</code> interface. Provides means to start and stop a Container
 * hosting {@link org.apache.uima.collection.base_cpm.CasProcessor}.
 * 
 * 
 * 
 */
public interface RunnableContainer extends Runnable {
  /**
   * Starts the container
   * 
   */
  public void start();

  /**
   * Stops the container
   * 
   */
  public void stop();

}
