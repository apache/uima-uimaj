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

package org.apache.uima.collection.impl.cpm.container.deployer.socket;

import java.net.URL;

/**
 * Interface for the custom deployer component responsible for launching and terminating fenced
 * CasProcessors.
 * 
 */
public interface ProcessControllerAdapter {
  /**
   * Deploys given number of CasProcessors and returns their endpoint configuration( host,port).
   * This method blocks until all Connections are resolved or an error occurs.
   * 
   * @param aCasProcessorName -name of the fenced CasProcessor
   * 
   * @param howMany - how many CasProcessor instances to deploy
   * 
   * @return URL[] - list of URLs containing endpoint info
   * 
   * @throws
   *           Exception Failure to start fenced CasProcessor
   */
  public URL[] deploy(String aCasProcessorName, int howMany) throws Exception;

  /**
   * Stops a given CasProcessor service.
   * 
   * @param aURL - service endpoint.
   * 
   */
  public void undeploy(URL aURL);

}
