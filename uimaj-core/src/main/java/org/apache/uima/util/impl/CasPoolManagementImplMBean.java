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
package org.apache.uima.util.impl;

/**
 * JMX MBean interface for monitoring CASPool state.
 */
public interface CasPoolManagementImplMBean {
  /**
   * Get the total size of the CAS Pool.
   * @return the pool size
   */
  public int getPoolSize();
  
  /**
   * Get the number of CAS instances currently available in the pool.
   * @return the number of available CAS instances
   */
  public int getAvailableInstances();
  
//  /**
//   * Get the average time, in milliseconds, that getCas() requests on
//   * the pool have to wait for a CAS to become available
//   * @return average wait time in milliseconds
//   */
//  public int getAverageWaitTime();
}
