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

package org.apache.uima.collection.impl.cpm.container.deployer.vns;

/**
 * 
 * Vinci service information
 * 
 */
public class VinciServiceInfo {
  private String serviceName;

  private String host;

  private int port;

  private boolean available = true;

  public VinciServiceInfo(String aServiceName, String aHost, int aPort) {
    serviceName = aServiceName;
    host = aHost;
    port = aPort;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void setAvailable(boolean isAvailable) {
    available = isAvailable;
  }

  public boolean isAvailable() {
    return available;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("SERVICE:").append(serviceName).append("\nIP:").append(host)
            .append("\nPORT:").append(port);
    return sb.toString();
  }
}
