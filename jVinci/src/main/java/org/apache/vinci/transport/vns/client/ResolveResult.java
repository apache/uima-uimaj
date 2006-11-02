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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.FrameComponent;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.TransportConstants;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.vns.VNSConstants;

/**
 * Specialized document (Frame) for representing the result of resolving a service name to
 * host/port through VNS. Also provides utility methods for manipulating qualified/unqualified
 * service names. This class is used by VinciClient to locate the physical location of the 
 * requested service from its logical service name.
 */
public class ResolveResult extends Frame {

  public int                         priority = -1;

  static public TransportableFactory factory  = new TransportableFactory() {
                                                public Transportable makeTransportable() {
                                                  return new ResolveResult();
                                                }
                                              };

  /**
   * Strip the qualifications from this qualified service name.
   * 
   * @pre service_name != null
   */
  static public String unqualifiedName(String service_name) {
    if (isQualified(service_name)) {
      service_name = service_name.trim();
      int start = service_name.lastIndexOf('[');
      return service_name.substring(0, start);
    } else {
      return service_name;
    }
  }

  /**
   * Check whether a service_name has qualifications (that is, any or all of level, host,
   * instance are explicitly specified.) 
   * 
   * @pre service_name != null
   */
  static public boolean isQualified(String service_name) {
    if (service_name.indexOf('[') != -1) {
      return true;
    }
    return false;
  }

  /**
   * Create a document representing the VNS resolve query for the specified service.
   * This method accepts either qualified or unqualified service names.
   * 
   * @pre service_name != null
   */
  static public Frame composeQuery(String service_name) {
    VinciFrame query = new VinciFrame();
    query.fadd(TransportConstants.COMMAND_KEY, VNSConstants.RESOLVE_COMMAND);
    int start = service_name.indexOf('[');
    if (start != -1) {
      // qualified service name ... must parse out qualifications
      int end = service_name.indexOf(']', start);
      if (end != -1) {
        String qualifications = service_name.substring(start + 1, end);
        query.fadd(VNSConstants.SERVICE_KEY, service_name.substring(0, start));
        StringTokenizer tokenizer = new StringTokenizer(qualifications, ",");
        if (tokenizer.hasMoreTokens()) {
          query.fadd(VNSConstants.LEVEL_KEY, tokenizer.nextToken());
          if (tokenizer.hasMoreTokens()) {
            query.fadd(VNSConstants.HOST_KEY, tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
              query.fadd(VNSConstants.INSTANCE_KEY, tokenizer.nextToken());
            }
          }
        }
        return query;
      }
    }
    int at = service_name.indexOf('@');
    if (at == -1) {
      query.fadd(VNSConstants.SERVICE_KEY, service_name);
    } else {
      query.fadd(VNSConstants.SERVICE_KEY, service_name.substring(0, at));
    }
    return query;
  }

  /**
   * Create a document representing the VNS resolve query for the highest priority service(s)
   * whose priority is strictly below the specified priority. This method accepts either
   * qualified or unqualified service names.  
   * 
   * @pre service_name != null
   */
  static public Frame composeQuery(String service_name, int mypriority) {
    VinciFrame query = (VinciFrame) composeQuery(service_name);
    query.fadd("LEVEL", mypriority);
    return query;
  }

  private List services = new ArrayList();

  public ResolveResult() {
  }

  private int begin;
  private int current;

  /**
   * Initialize the service listing iterator. This initializes to a random spot
   * to implement simple load balancing across multiple equivalent service instances.
   */
  public void initializeIterator() {
    // Initialize the beginning to a random slot.
    begin = (int) (Math.random() * services.size());
    current = begin;
  }

  /**
   * Determine if there are more service listing to be fetched.
   */
  public boolean hasMore() {
    return (current - begin < services.size());
  }

  /**
   * Fetch the next service listing.
   * 
   * @pre hasMore()
   */
  public ServiceLocator getNext() {
    ServiceLocator return_me = (ServiceLocator) services.get(current % services.size());
    current++;
    return return_me;
  }

  static public class ServiceLocator extends Frame {
    public String host;
    public int    port;
    public int    instance;

    // Note that "priority" is a member of ResolveResult instead of this member class, since
    // all ServiceLocators for a particular query will always have the same priority.

    /** 
     * Implement the Frame add() callback.
     * 
     * @pre key != null
     * @pre val != null
     */
    public void add(String key, FrameComponent val) {
      if (key.equals(VNSConstants.HOST_KEY)) {
        host = ((FrameLeaf) val).toString();
      } else if (key.equals(VNSConstants.PORT_KEY)) {
        port = ((FrameLeaf) val).toInt();
      } else if (key.equals(VNSConstants.INSTANCE_KEY)) {
        instance = ((FrameLeaf) val).toInt();
      }
    }
  }

  /**
   * Implement the frame add() callback.
   * 
   * @pre key != null
   * @pre val != null
   * @pre ((val instanceof ServiceLocator && key.equals(VNSConstants.SERVER_KEY)) || (val instanceof FrameLeaf && key.equals(VNSConstants.LEVEL_KEY)))
   */
  public void add(String key, FrameComponent val) {
    if (key.equals(VNSConstants.SERVER_KEY)) {
      Debug.Assert(val instanceof ServiceLocator);
      services.add(val);
    } else if (key.equals(VNSConstants.LEVEL_KEY)) {
      priority = ((FrameLeaf) val).toInt();
    }
  }

  /**
   * Override the Frame createSubFrame method to create a ServiceLocator.
   */
  public Frame createSubFrame(String key, int capacity) {
    return new ServiceLocator();
  }
  // END Frame implementation

  /*static public void main(String[] args_) {
   System.out.println(composeQuery(args_[0]).toXML());
   }*/
}
