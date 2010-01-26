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

import java.net.ConnectException;
import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;
import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.VinciFrame;

/**
 * 
 * Connects to and querries a given VNS server for a list a services bound to a given name.
 * 
 */

public class VNSQuery {
  private VinciVNSQuery vnsQuery = null;

  /**
   * Connects to a VNS server identified by host and port
   * 
   * @param aVnsHost -
   *          VNS host name
   * @param aVnsPort -
   *          VNS port number
   * 
   * @throws Exception -
   *           when unable to connect to VNS
   */
  public VNSQuery(String aVnsHost, int aVnsPort) throws Exception {
    vnsQuery = new VinciVNSQuery(aVnsHost, aVnsPort);
  }

  /**
   * Returns a list of services registered in the VNS and bound to a given name.
   * 
   * @param aName -
   *          name of the service
   * 
   * @return - ArrayList of {@link VinciServiceInfo} instances
   * 
   * @throws Exception -
   *           unable to get a list
   */
  public ArrayList getServices(String aName) throws Exception {
    return vnsQuery.getVinciServices(aName);
  }

  /**
   * Returns a list of services that have not yet been assigned to any CPM proxy. It diffs the
   * current list and a new list as returned from the VNS.
   * 
   * @param aName -
   *          name of the service
   * @param assignedServices -
   *          a list of services currently in use
   * @return - ArrayList of {@link VinciServiceInfo} instances
   * 
   * @throws Exception -
   *           unable to get a list
   */
  public ArrayList getUnassignedServices(String aName, ArrayList assignedServices) throws Exception {
    // Retrieve a new list from the VNS
    ArrayList newList = getServices(aName);
    // Do a diff between current and new service list
    findUnassigned(assignedServices, newList);
    return newList;
  }

  /**
   * Diffs two lists of services and returns those that have not yet been assigned
   * 
   * @param oldList -
   *          current (in-use) list of services
   * @param newList -
   *          new list of services
   * 
   * @return - number of un-assigned services
   */
  public static int findUnassigned(ArrayList oldList, ArrayList newList) {
    int newServiceCount = 0;
    for (int i = 0; i < newList.size(); i++) {

      VinciServiceInfo service = (VinciServiceInfo) newList.get(i);
      if (!newService(service, oldList)) {
        ((VinciServiceInfo) newList.get(i)).setAvailable(false);
      } else {
        newServiceCount++;
      }
    }
    return newServiceCount;
  }

  /**
   * Checks if a service identified by {@link VinciServiceInfo} instance is in use. If a service
   * exists in the service list but is not assigned, that means that is available. If the service
   * does not exist in the list it is also considered available.
   * 
   * @param aService -
   *          {@link VinciServiceInfo} instance to locate in the list
   * @param oldList -
   *          list of current (in-use) services
   * 
   * @return - true, if service is available. false, otherwise
   */
  private static boolean newService(VinciServiceInfo aService, ArrayList oldList) {
    for (int i = 0; i < oldList.size(); i++) {
      VinciServiceInfo service = (VinciServiceInfo) oldList.get(i);

      if (aService.getHost().equals(service.getHost()) && aService.getPort() == service.getPort()
              && service.isAvailable() == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   * Inner class used for accessing the VNS server.
   * 
   */
  public class VinciVNSQuery {
    private String vnsHost;

    private int vnsPort;

    BaseClient vnsConnection = null;

    /**
     * Establishes connection to a given VNS server
     * 
     * @param aVnsHost -
     *          name of the host where the VNS is running
     * @param aVnsPort -
     *          port on which the VNS is listening
     * @throws Exception -
     *           unable to connect to VNS
     */
    public VinciVNSQuery(String aVnsHost, int aVnsPort) throws Exception {
      vnsHost = aVnsHost;
      vnsPort = aVnsPort;
      vnsConnection = new BaseClient(vnsHost, vnsPort);
    }

    /**
     * Returns a list of services bound to a given name. It ONLY returns those services that are
     * actually running. The VNS may return services that are stale. Those will be filtered out.
     * 
     * @param aVinciServiceName -
     *          name of the service
     * @return - list of services bound to a given name.
     * 
     * @throws Exception -
     *           error while looking up the service
     */
    public ArrayList getVinciServices(String aVinciServiceName) throws Exception {
      ArrayList serviceList = new ArrayList();
      BaseClient client = null;
      // make sure we got a valid connection to VNS
      if (vnsConnection != null && vnsConnection.isOpen()) {
        // Set up VNS query
        VinciFrame queryFrame = new VinciFrame();
        queryFrame.fadd("vinci:COMMAND", "getlist");
        queryFrame.fadd("PREFIX", aVinciServiceName);
        // System.out.println("Query Frame:::"+queryFrame.toXML());
        // Query the VNS
        VinciFrame response = (VinciFrame) vnsConnection.sendAndReceive(queryFrame);
        ArrayList serviceFrames = response.fget("SERVICE");
        // Each service is returned in its own SERVICE frame. So cycle through those now
        // one at a time
        for (int i = 0; i < serviceFrames.size(); i++) {
          VinciFrame serviceFrame = (VinciFrame) serviceFrames.get(i);
          // Copy data from the frame ( host, port etc)
          VinciServiceInfo serviceInfo = getServiceInfo(serviceFrame);
          if (serviceInfo != null) {
            // Test the service for availability. Use only those services that respond. The list
            // may contain stale services that are not running
            try {
              // Establish a brief connection to test for availability. This test fails gracefully
              // Its not an error if the service does not respond. The retry logic is done
              // elsewhere.
              client = new BaseClient(serviceInfo.getHost(), serviceInfo.getPort());
              if (client.isOpen()) {
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(
                          Level.FINEST,
                          this.getClass().getName(),
                          "initialize",
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_service_active_on_port__FINEST",
                          new Object[] { Thread.currentThread().getName(),
                              serviceInfo.getServiceName(), serviceInfo.getHost(),
                              String.valueOf(serviceInfo.getPort()) });
                }
                // Service is ok, so add it to the list
                serviceList.add(serviceInfo);
              }
            } catch (ConnectException ce) {
              if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.WARNING,
                        this.getClass().getName(),
                        "initialize",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_service_not_active_on_port__WARNING",
                        new Object[] { Thread.currentThread().getName(),
                            serviceInfo.getServiceName(), serviceInfo.getHost(),
                            String.valueOf(serviceInfo.getPort()) });
              }
            } finally {
              // Drop the connection if necessary.
              if (client != null) {
                try {
                  client.close();
                } catch (Exception ex) {
                }
              }
            }
          }
        }
      }

      return serviceList;
    }

    /**
     * Copy service information from Vinci frame.
     * 
     * @param aServiceFrame -
     *          Vinci frame containing service info
     * 
     * @return- instance of {@link VinciServiceInfo} containing service info
     */
    private VinciServiceInfo getServiceInfo(VinciFrame aServiceFrame) {
      String serviceName = aServiceFrame.fgetString("NAME");
      String service_host_IP = aServiceFrame.fgetString("IP");
      int service_port = aServiceFrame.fgetInt("PORT");

      VinciServiceInfo serviceInfo = new VinciServiceInfo(serviceName, service_host_IP,
              service_port);
      return serviceInfo;
    }
  }

  public static void main(String[] args) {
    try {
      VNSQuery vq = new VNSQuery(args[0], Integer.parseInt(args[1]));

      ArrayList list = vq.getServices(args[2]);
      System.out.println("Got::" + list.size() + " Services");
      BaseClient client = null;
      for (int i = 0; i < list.size(); i++) {
        Object ob = list.get(i);
        if (ob != null) {
          System.out.println(((VinciServiceInfo) ob).toString());
          VinciServiceInfo serviceInfo = (VinciServiceInfo) ob;
          if (serviceInfo != null) {
            try {
              client = new BaseClient(serviceInfo.getHost(), serviceInfo.getPort());
              if (client.isOpen()) {
                System.out.println("Service::" + serviceInfo.getServiceName() + " is alive");
              }
            } catch (ConnectException ce) {
              System.out.println("Service::" + serviceInfo.getServiceName() + " is down");
            }
          }
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
