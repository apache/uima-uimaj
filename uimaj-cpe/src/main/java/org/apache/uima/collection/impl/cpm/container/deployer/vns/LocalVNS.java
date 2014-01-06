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

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.net.ServerSocket;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.cpm.engine.BoundedWorkQueue;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;
import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.VinciServableAdapter;
import org.apache.vinci.transport.VinciServer;

/**
 * 
 * LOCAL Vinci Naming Service. Used by locally deployed TAEs. Locally, meaning TAEs running on the
 * same machine but in different JVM. This VNS is primarily used by TAEs to advertise their
 * availability after succesfull startup.
 * 
 */

public class LocalVNS extends VinciServableAdapter implements Runnable {
  private int onport;

  private int startport = 11111;

  private int maxport = 12331;

  private int vnsPort = 9001;

  private VinciServer server = null;

  private BoundedWorkQueue portQueue = null;

  public LocalVNS() {
  }

  /**
   * Instantiates Local Vinci Naming Service
   * 
   * @param aStartPort -
   *          a starting port # for clients (services)
   * @param aEndPort -
   *          an ending port # for clients( services)
   * @param aVNSPort -
   *          port on which this VNS will listen on
   */
  public LocalVNS(String aStartPort, String aEndPort, String aVNSPort)
          throws PortUnreachableException {
    if (aStartPort != null) {
      try {
        startport = Integer.parseInt(aStartPort);
      } catch (NumberFormatException e) {
      }
    }
    if (aEndPort != null) {
      try {
        maxport = Integer.parseInt(aEndPort);
      } catch (NumberFormatException e) {
      }
    }

    if (aVNSPort != null) {
      try {
        vnsPort = Integer.parseInt(aVNSPort);
        boolean vnsPortAvailable = false;
        int currentRetryCount = 0;
        while (!vnsPortAvailable) {
          if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_test_vns_port__INFO",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });
          }
          vnsPortAvailable = isAvailable(vnsPort);

          if (currentRetryCount > 100) {
            throw new PortUnreachableException("Unable to aquire a port for VNS Service");
          }
          if (!vnsPortAvailable) {
            vnsPort++;
          }
          currentRetryCount++;
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_activating_vns_port__INFO",
              new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });
    }
    onport = startport;
  }

  /**
   * Initialize local VNS instance with a range of ports, and the port for the VNS itself. A given
   * port is tested first for availability. If it is not available the next port is tested, until
   * one is found to be available.
   * 
   * @param aStartPort -
   *          starting port number used
   * @param aEndPort -
   *          end port number. Together with StartPort defines the range of ports (port pool)
   * @param aVNSPort -
   *          port on which this VNS will listen for requests
   * 
   * @throws PortUnreachableException unreachable port after retries
   */
  public LocalVNS(int aStartPort, int aEndPort, int aVNSPort) throws PortUnreachableException {
    startport = aStartPort;
    maxport = aEndPort;
    vnsPort = aVNSPort;
    boolean vnsPortAvailable = false;
    int currentRetryCount = 0;
    // Loop until we find a valid port or hard limit of 100 tries is reached
    while (!vnsPortAvailable) {
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_test_vns_port__INFO",
                new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });
      }
      vnsPortAvailable = isAvailable(vnsPort);

      if (currentRetryCount > 100) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_vns_port_not_available__SEVERE",
                new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });

        throw new PortUnreachableException("Unable to aquire a port for VNS Service");
      }
      if (!vnsPortAvailable) {
        vnsPort++;
      }
      currentRetryCount++;
    }
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_activating_vns_port__INFO",
              new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });
    }
    onport = startport;
  }

  /**
   * Associates a port pool with instance of VNS.
   * 
   * @param pQueue -
   *          queue where allocated ports will be added
   */
  public synchronized void setConnectionPool(BoundedWorkQueue pQueue) {
    portQueue = pQueue;
  }

  /**
   * Determines if a given port is free. It establishes a short lived connection to the port and if
   * successful returns false.
   * 
   * @param port number to check
   */
  public boolean isAvailable(int port) {
    ServerSocket socket = null;
    try {
      // Open the socket
      socket = new ServerSocket(port);
      return true;
    } catch (Exception e) {
      // Disregard this error. False is returned in this case and the caller determines what to do
    } finally {
      if (socket != null) {
        try {
          if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_test_local_port__INFO",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(port) });
          }
          socket.close();
        } catch (IOException ioe) {
        }
      }
    }
    return false;
  }

  /**
   * Returns the port number on which local VNS is listening for requests.
   * 
   * @return - VNS port number
   */
  public int getVNSPort() {
    return vnsPort;
  }

  /**
   * Returns the next available port. The port is allocated from a cache of ports given to the VNS
   * service on startup.
   * 
   * @return - free port
   * 
   * @throws PortUnreachableException can't get port in configured range
   */
  public synchronized int getPort() throws PortUnreachableException {
    boolean portAvailable = false;
    int retryCount = 0;
    while (!portAvailable) {
      onport++;
      if (onport > maxport) {
        onport = startport;
        retryCount++; // increment total number of times we cycled through available ports
      }
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_test_local_port__INFO",
                new Object[] { Thread.currentThread().getName(), String.valueOf(onport) });
      }
      // Check port availability
      portAvailable = isAvailable(onport);
      // In case ports are not available break out of the loop having tried 4 times
      // to acquire any of the ports in configured range
      if (retryCount > 3) {
        throw new PortUnreachableException(
                "Unable to aquire any of the ports in configured range:[" + startport + ".."
                        + maxport + "]");
      }
    }
    return onport;
  }

  /**
   * Main method called by services advertising their availability. Each service, on startup sends
   * "serveon" request to VNS and waits for assigned port. The VNS looks up its cahce of ports and
   * returns to the service one that has not yest allocated.
   * 
   */
  public synchronized Transportable eval(Transportable in) throws ServiceException {

    try {
      VinciFrame input = (VinciFrame) in;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger().log(Level.FINEST, input.toXML());
      }

      String cmd = input.fgetString("vinci:COMMAND");
      if (cmd.equals("shutdown")) {
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_deactivating_vns_port__INFO",
                  new Object[] { Thread.currentThread().getName() });
        }
        this.cleanExit();
        return input;
      } else if (cmd.equals("serveon")) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_vns_process_serveon__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        // String serviceName = input.fgetString("SERVICE");
        int port = getPort();
        input.fdrop("vinci:COMMAND");
        input.fadd("IP", "127.0.0.1");
        input.fadd("PORT", port);

        try {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_assign_service_port__FINEST",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(port) });
          }
          portQueue.enqueue(String.valueOf(port));
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_assign_service_port_complete__FINEST",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(port) });
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, input.toXML());
          UIMAFramework.getLogger().log(Level.FINEST, input.toXML());
        }
        return input;
      } else if (cmd.equals("resolve")) {
        String publicVNSHost = System.getProperty("PVNS_HOST");
        String publicVNSPort = System.getProperty("PVNS_PORT");
        if (publicVNSHost == null || publicVNSHost.trim().length() == 0 || publicVNSPort == null
                || publicVNSPort.trim().length() == 0) {
          if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING,
                    this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_unknown_vns_command__WARNING",
                    new Object[] { Thread.currentThread().getName() });
          }
          VinciFrame rtn = new VinciFrame();
          rtn
                  .fadd("vinci:EXCEPTION",
                          "CPM Reply:Public VNS not known. Verify CPMs startup param include -DVNS_HOST and -DVNS_PORT");
          return rtn;
        }
        int pvnsPort = -1;
        try {
          pvnsPort = Integer.parseInt(publicVNSPort);
        } catch (NumberFormatException e) {
          if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING,
                    this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_unknown_vns_command__WARNING",
                    new Object[] { Thread.currentThread().getName() });
          }
          VinciFrame rtn = new VinciFrame();
          rtn.fadd("vinci:EXCEPTION", "CPM Reply: Invalid VNS Port value::" + publicVNSPort);
          return rtn;
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_vns_redirect__FINEST",
                  new Object[] { Thread.currentThread().getName(), publicVNSHost, publicVNSPort });
        }
        BaseClient client = new BaseClient(publicVNSHost, pvnsPort);
        return client.sendAndReceive(in);
      } else {
        if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_unknown_vns_command__WARNING",
                  new Object[] { Thread.currentThread().getName() });
        }
        VinciFrame rtn = new VinciFrame();
        rtn.fadd("vinci:EXCEPTION", "Unknown command");
        return rtn;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ServiceException(ex.getMessage());
    }
  }

  /**
   * Stop the VNS service
   * 
   */
  public void shutdown() {
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_vns_shutdown__INFO",
              new Object[] { Thread.currentThread().getName() });
    }
    try {
      this.cleanExit();
      if (server != null) {
        server.shutdownServing();
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_vns_stopped_serving__INFO",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  protected void finalize() throws Throwable {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              "*************Finalizing VNS***************");
    }
    try {
      shutdown();
    } finally {
      super.finalize();
    }
  }

  /**
   * Starts VNS thread. This thread runs continuously waiting for service registrations and
   * returning port number back.
   */
  public void run() {
    boolean done = false;
    Thread.currentThread().setName("VNS-Thread");
    while (!done) {
      try {
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_launching_local_vns__INFO",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(vnsPort) });
        }
        server = new VinciServer(this);
        server.serve(vnsPort);
        done = true;
      } catch (BindException e) {
        vnsPort++;
      } catch (Exception e) {

        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_launching_local_vns_failed__SEVERE",
                  new Object[] { Thread.currentThread().getName(), e.getMessage() });
        }
        e.printStackTrace();

      }
    }
  }
}
