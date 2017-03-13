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

package org.apache.vinci.transport.vns.service;

/**
 * Class representing a service's attributes, used by VNSCommandLine
 */
public class ServiceInfo {

  public String host, port, instance, name, ws, level;

  public ServiceInfo(String[] in) {
    ws = in[0];
    name = in[1];
    level = in[2];
    host = in[3];
    instance = in[4];
    port = null;
    if (level == null)
      level = "-1";
  }

  public ServiceInfo(String name, String host, String port, String level, String instance) {
    this.name = name;
    this.host = host;
    this.port = port;
    this.level = ((level == null) ? "-1" : level);
    this.instance = instance;
  }

  public String toString() {
    String result = "";
    result += "Workspace: " + ws + "\n";
    result += "Name: " + name + "\n";
    result += "Host: " + host + "\n";
    result += "Port: " + port + "\n";
    result += "Level: " + level + "\n";
    result += "Instance: " + instance + "\n";

    return result;
  }

}
