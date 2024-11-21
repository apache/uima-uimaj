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

package org.apache.vinci.transport.vns.client;

import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.FrameComponent;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.TransportConstants;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.vns.VNSConstants;

/**
 * Specialized document (Frame) for representing result of querying VNS for the port on which a
 * service should be provided. This class is used by VinciServer during port negotiation with VNS.
 */
public class ServeonResult extends Frame {

  public static TransportableFactory factory = new TransportableFactory() {
    @Override
    public Transportable makeTransportable() {
      return new ServeonResult();
    }
  };

  /**
   * Create a document representing the VNS serveon query for the specified service running on/with
   * the specified host/priority/instance.
   * 
   * @pre service_name != null
   * @pre host_name != null
   * @pre priority &ge; -1
   * @pre instance &ge; 0
   * @param service_name
   *          -
   * @param host_name
   *          -
   * @param priority
   *          -
   * @param instance
   *          -
   * @return -
   * 
   */
  public static Transportable composeQuery(String service_name, String host_name, int priority,
          int instance) {
    VinciFrame query = (VinciFrame) composeQuery(service_name, host_name, priority);
    query.fadd(VNSConstants.INSTANCE_KEY, instance);
    return query;
  }

  /**
   * Create a document representing the VNS serveon query for the specified service running on/with
   * the specified host/priority.
   * 
   * @pre service_name != null
   * @pre host_name != null
   * @pre priority &ge; -1
   * @param service_name
   *          -
   * @param host_name
   *          -
   * @param priority
   *          -
   * @return -
   */
  public static Transportable composeQuery(String service_name, String host_name, int priority) {
    VinciFrame query = (VinciFrame) composeQuery(service_name, host_name);
    query.fadd(VNSConstants.LEVEL_KEY, priority);
    return query;
  }

  /**
   * Create a document representing the VNS serveon query for the specified service running on the
   * specified host.
   * 
   * @pre service_name != null
   * @pre host_name != null
   * @param service_name
   *          -
   * @param host_name
   *          -
   * @return -
   */
  public static Transportable composeQuery(String service_name, String host_name) {
    VinciFrame query = new VinciFrame();
    query.fadd(TransportConstants.COMMAND_KEY, VNSConstants.SERVEON_COMMAND);
    query.fadd(VNSConstants.SERVICE_KEY, service_name);
    query.fadd(VNSConstants.HOST_KEY, host_name);
    return query;
  }

  /**
   * Set to the port on which the service should be offered.
   */
  public int port;

  /**
   * Implement the Frame add() callback.
   * 
   * @pre key != null
   * @pre value != null
   * @pre (!key.equals(VNSConstants.PORT_KEY) || (value instanceof FrameLeaf))
   */
  @Override
  public void add(String key, FrameComponent value) {
    if (VNSConstants.PORT_KEY.equals(key)) {
      port = ((FrameLeaf) value).toInt();
    }
  }
}
