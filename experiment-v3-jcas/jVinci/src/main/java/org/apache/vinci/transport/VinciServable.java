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

package org.apache.vinci.transport;

/**
 * Interface for implementing Vinci Services. These objects get dropped into any of the various
 * Server (container) classes such as VinciServer.
 */
public interface VinciServable extends TransportableFactory {

  /**
   * The eval method accepts an input document and returns an output document that is the result of
   * performing the service.
   * 
   * @return The output document
   * @param in
   *          The input document
   * @exception ServiceException
   *              thrown when there is an application level error that should result in the client
   *              receiving the same ServiceException on the other end.
   */
  Transportable eval(Transportable in) throws ServiceException;

  /**
   * Called when the service is being shutdown by the server/service container.
   */
  void cleanExit();
}
