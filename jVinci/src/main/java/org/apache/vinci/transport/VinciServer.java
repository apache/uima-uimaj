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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.vns.client.ServeonResult;

/**
 * "Standard" service container for a VinciServable. This extends BaseServer with functions
 * allowing port negotiation via interaction with VNS. It also provides rudimentary support for
 * service control and monitoring by responding to vinci:SHUTDOWN and vinci:PING.
 *
 * Note that this server class directs requests to a single VinciServable. For many applications
 * it may be desirable to have one server accept requests that get delegated to multiple services,
 * not just a single service. For such applications consider using the MultiplexedServer. You can
 * also implement a VinciServable that implements its own sub-service delegation scheme.
 *
 */
public class VinciServer extends BaseServer {

  private int          priority    = 0;
  private int          instance    = 0;
  private String       hostName    = null;
  private String       serviceName = null;

  // Port gets set via VNS port negotiation once serving
  // is initiated.
  private volatile int port        = -1;

  private VinciContext context     = null;

  /**
   * Create a new server. If an incorrect hostname is provided, this server will not be
   * reachable.
   *
   * @param host_name The DNS hostname of the machine running this server.
   *
   * @pre service_name != null
   * @pre host_name != null
   * @pre servable != null
   */
  public VinciServer(String service_name, String host_name, VinciServable servable) {
    this(service_name, host_name, servable, 0);
  }

  /**
   * Create a new server that reports the current machine's IP address as the host.
   * This should not be used for DHCP-based hosts since IP address can change.
   *
   * @throws UnknownHostException If there is an error determining machine IP address.
   *
   * @pre service_name != null
   * @pre servable != null
   */
  public VinciServer(String service_name, VinciServable servable) throws UnknownHostException {
    this(service_name, InetAddress.getLocalHost().getHostAddress(), servable);
  }

  /**
   * Create a new server.
   *
   * @pre service_name != null
   * @pre host_name != null
   * @pre servable != null
   * @pre myPriority >= -1
   * @pre myInstance >= 0
   */
  public VinciServer(String service_name, String host_name, VinciServable servable, int myPriority, int myInstance) {
    super(servable);
    this.priority = myPriority;
    this.serviceName = service_name;
    this.instance = myInstance;
    this.hostName = host_name;
  }

  /**
   * @pre service_name != null
   * @pre host_name != null
   * @pre servable != null
   * @pre myPriority >= -1
   */
  public VinciServer(String service_name, String host_name, VinciServable servable, int myPriority) {
    this(service_name, host_name, servable, myPriority, 0);
  }

  /**
   * @pre service_name != null
   * @pre servable != null
   * @pre myPriority >= -1
   */
  public VinciServer(String service_name, VinciServable servable, int myPriority) throws UnknownHostException {
    this(service_name, InetAddress.getLocalHost().getHostAddress(), servable, myPriority);
  }

  /**
   * @pre service_name != null
   * @pre servable != null
   * @pre myPriority >= -1
   * @pre myInstance >= 0
   */
  public VinciServer(String service_name, VinciServable servable, int myPriority, int myInstance)
      throws UnknownHostException {
    this(service_name, InetAddress.getLocalHost().getHostName(), servable, myPriority, myInstance);
  }

  /**
   * servable != null
   */
  public VinciServer(VinciServable servable) {
    super(servable);
  }

  /**
   * Get the context associated with this server. By default clients use the global Vinci
   * context, though this can be overridden.
   */
  public VinciContext getContext() {
    if (context == null) {
      return VinciContext.getGlobalContext();
    } else {
      return context;
    }
  }

  public int getPriority() {
    return this.priority;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public int getInstance() {
    return this.instance;
  }

  public String getHostName() {
    return this.hostName;
  }

  /**
   * After invoking serve() or startServing(), this method can be used to determine
   * the port which was negotiated with VNS on which to serve requests.
   *
   * @since 2.0.15
   */
  public int getServingPort() {
    return this.port;
  }

  /**
   * Set the VinciContext to be used by this server. Set to null if you wish the global
   * context to be used.
   */
  public void setContext(VinciContext c) {
    context = c;
  }

  /**
   * Serve requests until a clean shutdown is triggered. Note that all three exceptions thrown
   * by this method are IOExceptions so a single IOException catch phrase is sufficient
   * unless it is important to determine the particular failure.
   *
   * @throws ServiceDownException Thrown if there was a failure to contact VNS for port
   * negotiation.
   * @throws VNSException Typically thrown if VNS does not recognize the service provided by
   * this server.
   * @throws IOException Thrown if there was some problem with the server socket. 
   * @throws IllegalStateException if VNS host is not specified.
   */
  public void serve() throws ServiceDownException, VNSException, IOException {
    this.port = getPort();
    Debug.printDebuggingMessage("Service " + serviceName + " starting on port " + port);
    serve(this.port);
  }

  /**
   * Start a new thread that will serve requests until a clean shutdown is triggered. Note that
   * all three exceptions thrown by this method are IOExceptions so a single IOException catch
   * phrase is sufficient unless it is important to determine the particular failure. If this
   * method returns without throwing an exception then the port has been determined and a new
   * thread has been launched.
   *
   * @throws ServiceDownException Thrown if there was a failure to
   * contact VNS for port negotiation.
   * @throws VNSException Typically thrown if VNS does not recognize the service provided by
   * this server.
   * @throws IOException Thrown if there was some problem with the server socket. 
   * @throws IllegalStateException if VNS host is not specified.
   * @since 2.0.15
   */
  public void startServing() throws ServiceDownException, VNSException, IOException {
    this.port = getPort();
    Debug.p("Service " + serviceName + " asynchronously starting on port " + port);
    startServing(this.port);
  }

  /**
   * @pre client != null
   */
  protected Runnable getRunnable(Socket client) {
    return new VinciServerRunnable(client, this);
  }

  /**
   * Factory method for creating a shutdown message. Send the returned object to 
   * any server, and if it is programmed to respond to shutdown, it will do so.
   *
   * @param shutdown_message Should be used to pass a message explaining the shutdown,
   * or in the future it may also include authentication information for password-protected
   * shutdown.
   */
  public static Transportable createShutdownCommand(String shutdown_message) {
    return new VinciFrame().fadd(TransportConstants.SHUTDOWN_KEY, shutdown_message);
  }

  /**
   * This method is called by the server when a remote shutdown request is received. In general
   * if you want to stop the server call shutdownServing() -- this method should have probably
   * been declared "protected".  You can override this method if you want the shutdown message
   * to be ignored in certain cases.
   */
  public boolean shutdown(String shutdown_message) {
    // Override this method if shutdown is to be ignored in certain cases (by returning
    // false). Default implementation unconditionally halts serving, 
    // and hence always returns true.
    Debug.printDebuggingMessage("VinciServer.shutdown()", "Accepted shutdown request: " + shutdown_message);
    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // shouldn't happen
          Debug.reportException(e);
        }
        shutdownServing();
      }
    }).start();
    return true;
  }

  /**
   * @throws IllegalStateException if VNS host isn't specified.
   */
  protected int getPort() throws ServiceDownException, VNSException {
    try {
      VinciContext myContext = getContext();
      ServeonResult response = (ServeonResult) VinciClient.sendAndReceive(ServeonResult.composeQuery(serviceName,
          hostName, priority, instance), myContext.getVNSHost(), myContext.getVNSPort(), ServeonResult.factory,
          myContext.getVNSServeonTimeout());
      return response.port;
    } catch (IOException e) {
      Debug.reportException(e);
      throw new ServiceDownException("VNS inaccessible: " + e);
    } catch (ServiceException e) {
      throw new VNSException(e.getMessage());
    }
  }
}
