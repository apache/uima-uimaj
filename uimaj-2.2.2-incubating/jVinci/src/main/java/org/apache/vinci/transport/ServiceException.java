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
 * Special exception thrown by VinciClient indicating an "application level" error or exception.
 * Vinci services can also throw this exception and an equivalent exception will arise on the other
 * side. Other ways to cause an exception to be returned on the other side is to simply return an
 * ErrorFrame. An ErrorFrame allows arbitrary other information to be returned along with the
 * exception message.
 * 
 * If a Vinci service throws a ServiceException, then this is equivalent to returning a Vinci
 * ErrorFrame as returned by the single ErrorFrame constructor.
 */
public class ServiceException extends Exception {

  private static final long serialVersionUID = 6243682131707055564L;

  private Transportable complete_result;

  public ServiceException(String error_message, Transportable result) {
    super(error_message);
    complete_result = result;
  }

  public ServiceException(String error_message) {
    super(error_message);
    complete_result = new ErrorFrame(error_message);
  }

  public Transportable getCompleteDocument() {
    return complete_result;
  }
}
