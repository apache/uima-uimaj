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


/**
 * A <code>ResourceManagerPearWrapper</code> is a 
 * special Resource Manager, sharing all its fields with its parent,
 * except for the class path and data path fields.
 * 
 * 
 */
public interface ResourceManagerPearWrapper extends ResourceManager {
  /**
   * Pear Wrapper Resource Managers share all their values with their parent,
   * except for the 2 values used to store the Classpath and Datapath.
   * 
   * This method is called immediately after the factory creates the 
   * object (using the 0-argument constructor), and it initializes all
   * the fields in this wrapper to share the values with their parent.
   * @param resourceManager the parent ResourceManager
   */
  void initializeFromParentResourceManager(ResourceManager resourceManager);
}
