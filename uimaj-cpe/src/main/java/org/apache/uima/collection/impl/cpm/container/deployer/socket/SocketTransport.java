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

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * 
 * 
 */
public interface SocketTransport {
  /**
   * Returns transport identifier
   * 
   * @return - String uniquely identifying the transport.
   */
  public String getName();

  /**
   * Creates a socket connection to a given endpoint. This method blocks until all Connections are
   * resolved or an error occurs.
   * 
   * @param aURI URI containing service endpoint info: host &amp; port
   * @param aTimeout max time in millis to wait for response
   * 
   * @return Socket bound to a given endpoint
   * 
   * @throws SocketException Failed to connect
   */
  public Socket connect(URL aURI, long aTimeout) throws SocketException;

  /**
   * Invokes fenced CasProcessor.
   * 
   * @param aSocket - Socket bound to fenced CasProcessor
   * @param aCas - CAS to be sent to the CasProcessor for analysis
   * 
   * @return - CAS - CAS returned from the fenced CasProcessor
   * 
   * @throws
   *           SocketTimeoutException - Socket timesout before receiving response from the fenced
   *           CasProcessor
   * @throws
   *           SocketException - connection broken
   * 
   */
  public CAS process(Socket aSocket, CAS aCas) throws SocketTimeoutException, SocketException,
          AnalysisEngineProcessException;

  /**
   * Returns metadata associated with the fenced CasProcessor
   * 
   * @param aSocket -
   *          socket to the fenced CasProcessor
   * @return - metadata
   * @throws SocketException passthru
   * @throws AnalysisEngineProcessException passthru
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData(Socket aSocket)
          throws SocketException, AnalysisEngineProcessException;

}
