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

import java.net.Socket;

/**
 * Runnable class used by VinciServer to concurrently service requests.
 */
public class VinciServerRunnable extends BaseServerRunnable {

  protected VinciServerRunnable(Socket c, VinciServer p) {
    super(c, p);
  }

  /**
   * Handle shutdown requests, and PING commands.
   */
  public Transportable handleHeader(KeyValuePair header) {
    VinciFrame out = null;
    if (header != null) {
      if (header.key.equals(TransportConstants.SHUTDOWN_KEY)) {
        if (((VinciServer) getParent()).shutdown(header.getValueAsString())) {
          out = (VinciFrame) new VinciFrame().fadd(TransportConstants.STATUS_KEY, TransportConstants.OK_VALUE);
        } else {
          out = new ErrorFrame("Shutdown request ignored.");
        }
      } else if (header.key.equals(TransportConstants.PING_KEY)) {
        out = (VinciFrame) new VinciFrame().fadd(TransportConstants.STATUS_KEY, TransportConstants.OK_VALUE);
      }
    }
    return out;
  }
} // end class VinciServerRunnable
