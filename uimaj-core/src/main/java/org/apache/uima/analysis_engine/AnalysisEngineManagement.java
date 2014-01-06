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

package org.apache.uima.analysis_engine;

import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ManagementObject;

/**
 * Monitoring and management interface to an AnalysisEngine. An application can obtain an instance
 * of this object by calling {@link AnalysisEngine#getManagementInterface()}.
 * <p>
 * In this implementation, objects implementing this interface will always be JMX-compatible MBeans
 * that you can register with an MBeanServer. For information on JMX see <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/javax/management/package-summary.html">
 * http://java.sun.com/j2se/1.5.0/docs/api/javax/management/package-summary.html</a>
 */
public interface AnalysisEngineManagement extends ManagementObject {
	public static enum State {Unavailable,Initializing, Ready};
	
  /**
   * Gets a name for this AnalysisEngineManagement object, which will be unique among all of its
   * siblings (i.e. the objects returned from its parent's {@link #getComponents()} method.
   * 
   * @return a name for this AnalysisEngineManagement object
   */
  String getName();

  /**
   * Gets the total time this AnalysisEngine has spent doing analysis over its entire lifetime. This
   * includes calls to the {@link AnalysisEngine#process(CAS)} and
   * {@link AnalysisEngine#processAndOutputNewCASes(CAS)} methods, as well as calls to the
   * CasIterator returned from the processAndOutputNewCASes method.
   * 
   * @return the analysis time time in milliseconds
   */
  long getAnalysisTime();

  /**
   * Gets the total time this AnalysisEngine has spent in its batchProcessComplete method over its
   * entire lifetime.
   * 
   * @return the batch process complete time in milliseconds
   */
  long getBatchProcessCompleteTime();

  /**
   * Gets the total time this AnalysisEngine has spent in its collectionProcessComplete method over
   * its entire lifetime.
   * 
   * @return the batch process complete time in milliseconds
   */
  long getCollectionProcessCompleteTime();

  /**
   * If this AnalysisEngine is a proxy to a remote service, gets the total time spent making calls
   * on that service.
   * 
   * @return the service call time in milliseconds, or 0 if this AnalysisEngine is not a proxy to a
   *         service
   */
  long getServiceCallTime();

  /**
   * Gets the total number of CASes this AnalysisEngine has processed over its lifetime. For a CAS
   * Multipliers, this includes both input and output CASes.
   * 
   * @return the number of CASes processed
   */
  long getNumberOfCASesProcessed();

  /**
   * Gets the throughput of this AnalysisEngine, represented as number of CASes processed per
   * second.
   * 
   * @return a string representation of the throughput
   */
  String getCASesPerSecond();

  /**
   * For an Aggregate AnalysisEngine, gets a Map whose values are AnalysisEngineManagement objects
   * that contain the statistics for the components of the aggregate. The keys in the Map are the
   * unique String keys specified in the aggregate AnalysisEngine descriptor. If this AnalysisEngine
   * is a primitive, returns an empty Map.
   * 
   * @return a map from String keys to AnalysisEngineManagement objects
   */
  Map<String, AnalysisEngineManagement> getComponents();

  /**
   * Resets all of the performance statistics to zero. For an Aggregate Analysis Engine, also resets
   * the statistics for all the components of the aggregate.
   */
  void resetStats();
  
  /**
   * Gets the current state of an AnalysisEngine. The AE should either be in Initializing or Ready state.
   * @return the state of the analysis engine, from the State enum above
   */
  String getState();
  
  /**
   * Gets an id of a thread that was used to initialize AE instance
   * 
   * @return - thread id
   */
  public long getThreadId();
  
  /**
   * Total time it took AnalysisEngine to initialize
   * 
   * @return - initialization time
   */
  public long getInitializationTime();

}
