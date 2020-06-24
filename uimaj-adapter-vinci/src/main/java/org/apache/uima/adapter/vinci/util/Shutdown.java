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

package org.apache.uima.adapter.vinci.util;

import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;

// TODO: Auto-generated Javadoc
/**
 * The Class Shutdown.
 */
public class Shutdown {

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main(String[] args) {
    System.out.println("Shutting down the service...");
//IC see: https://issues.apache.org/jira/browse/UIMA-48
    try {
      String serviceName = args[0];
      VinciFrame query = new VinciFrame();
      query.fadd(Constants.VINCI_COMMAND, Constants.SHUTDOWN);
      VinciClient.rpc(query, serviceName);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  }

}
