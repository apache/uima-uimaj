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
 * This is a special VinciFrame that simplifies returning error or exceptional conditions.  If a
 * service returns an error frame, then the client's request method will in response throw an
 * exception with the provided error_message. Returning an error frame from VinciServable.eval()
 * has the same effect as throwing a ServiceException.
 */
public class ErrorFrame extends VinciFrame {

  public ErrorFrame(String error_message) {
    super();
    fadd(TransportConstants.ERROR_KEY, error_message);
  }

}
