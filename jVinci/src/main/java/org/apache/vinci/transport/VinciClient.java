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
import java.util.ArrayList;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.vns.client.ResolveResult;

/**
 * Class for conjuring a Vinci service. Adds VNS resolution to BaseClient. Extends BaseClient with
 * static "sendAndReceive" and "rpc" methods for convenient support of one-shot queries.
 */
public class VinciClient extends BaseClient {

  private int          level       = -2;
  private int          instance    = -1;
  private String       serviceName = null;
  private VinciContext context     = null;

  /**
   * Construct a new client connected to the requested service, and uses a VinciFrame
   * factory to create the return document type.
   *
   * @param service_name The name of the service to connect to.
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   *
   * @pre service_name != null
   */
  public VinciClient(String service_name) throws ServiceDownException, VNSException {
    this(service_name, VinciFrame.getVinciFrameFactory());
  }

  /**
   * Constructs a new client connected to the requested service.  Consults VNS for the proper
   * host and port to connect to.  Service names must be properly qualified. 
   *
   * @param service_name The name of the service to connect to.  
   * @param factory The factory used for creating return documents of desired type.
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   *
   * @pre service_name != null
   * @pre factory != null
   */
  public VinciClient(String service_name, TransportableFactory factory) throws ServiceDownException, VNSException {
    super(factory);
    open(service_name);
  }

  /**
   * @pre service_name != null
   * @pre factory != null
   * @pre myContext != null
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   */
  public VinciClient(String service_name, TransportableFactory factory, VinciContext myContext)
      throws ServiceDownException, VNSException {
    super(factory);
    setContext(myContext);
    open(service_name);
  }

  /**
   * @pre service_name != null
   * @pre myContext != null
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   */
  public VinciClient(String service_name, VinciContext myContext) throws ServiceDownException, VNSException {
    this(service_name, VinciFrame.getVinciFrameFactory(), myContext);
  }

  /**
   * Construct a new client connected to the requested service, and uses a VinciFrame
   * factory to create the return document type.
   *
   * @param service_name The name of the service to connect to.
   * @param connectTimeout The number of milliseconds that will elapse before a connect attempt fails.
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   *
   * @pre service_name != null
   */
  public VinciClient(String service_name, int connectTimeout) throws ServiceDownException, VNSException {
    this(service_name, VinciFrame.getVinciFrameFactory(), connectTimeout);
  }

  /**
   * Constructs a new client connected to the requested service.  Consults VNS for the proper
   * host and port to connect to.  Service names must be properly qualified. 
   *
   * @param service_name The name of the service to connect to.  
   * @param factory The factory used for creating return documents of desired type.
   * @param connectTimeout The number of milliseconds that will elapse before a connect attempt fails.
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   *
   * @pre service_name != null
   * @pre factory != null
   */
  public VinciClient(String service_name, TransportableFactory factory, int connectTimeout)
      throws ServiceDownException, VNSException {
    super(factory, connectTimeout);
    open(service_name);
  }

  /**
   * Constructs a new client connected to the requested service.
   *
   * @pre service_name != null
   * @pre factory != null
   * @pre myContext != null
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   */
  public VinciClient(String service_name, TransportableFactory factory, VinciContext myContext, int connectTimeout)
      throws ServiceDownException, VNSException {
    super(factory, connectTimeout);
    setContext(myContext);
    open(service_name);
  }

  /**
   * Constructs a new client connected to the requested service.
   *
   * @pre service_name != null
   * @pre myContext != null
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   */
  public VinciClient(String service_name, VinciContext myContext, int connectTimeout) throws ServiceDownException,
      VNSException {
    this(service_name, VinciFrame.getVinciFrameFactory(), myContext, connectTimeout);
  }

  /**
   * Construct a new client WITHOUT opening a connection, using the VinciFrame factory
   * to create return documents.
   */
  public VinciClient() {
    super();
  }

  /**
   * Construct a new client WITHOUT opening a connection, using the specified factory
   * to create return documents.
   */
  public VinciClient(TransportableFactory f) {
    super(f);
  }

  /**
   * Get the priority level of the service to which this client is connected. This 
   * method only works after the connection has been opened.
   */
  public int getLevel() {
    return level;
  }

  /**
   * Get the instance number of this service. This method only works after the connection has
   * been opened.
   *
   * Services should have "non zero" instance numbers only when more than one instance of the
   * service is running on the same host. The unique instance value allows each instance to be
   * distinguished and uniquely addressed via a fully qualified service name.  
   */
  public int getInstance() {
    return instance;
  }

  /**
   * Convenience method for "one-shot"/single-query connections. Equivalent of manually creating
   * a new VinciClient(), calling (non-static) sendAndReceive, and then closing the client.
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   * 
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   */
  static public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory)
      throws IOException, ServiceException, ServiceDownException, VNSException {
    return VinciContext.getGlobalContext().sendAndReceive(in, service_name, factory);
  }

  /**
   * Convenience method for "one-shot"/single-query connections. Equivalent of manually creating
   * a new VinciClient(), calling (non-static) sendAndReceive, and then closing the client.
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   * @pre socket_timeout >= 0
   */
  static public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory,
      int socket_timeout) throws IOException, ServiceException {
    return VinciContext.getGlobalContext().sendAndReceive(in, service_name, factory, socket_timeout);
  }

  /**
   * Convenience method for "one-shot"/single-query connections. Equivalent of manually creating
   * a new VinciClient(), calling (non-static) sendAndReceive, and then closing the client.
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   * @pre socket_timeout >= 0
   */
  static public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory,
      int socket_timeout, int connect_timeout) throws IOException, ServiceException {
    return VinciContext.getGlobalContext().sendAndReceive(in, service_name, factory, socket_timeout, connect_timeout);
  }

  /**
   * Get the context associated with this client. By default clients use the global Vinci
   * context, though this can be overridden.
   */
  public VinciContext getContext() {
    if (context == null) {
      return VinciContext.getGlobalContext();
    } else {
      return context;
    }
  }

  /**
   * Set the VinciContext to be used by this client. Set to null if you wish the global context
   * to be used. You should set the context BEFORE a connection is open (e.g. ater using the
   * no-arg constructor), otherwise the global context parameters will be used to establish the
   * connection before you have a chance to change it.
   */
  public void setContext(VinciContext c) {
    context = c;
  }

  /**
   * Get the fully qualified name of this service. This method is useful when for whatever
   * reason a connection needs to be re-established with this exact same service instance. This
   * method only works after connection has been established.  
   *
   * @pre getHost() != null
   */
  public String getQualifiedServiceName() {
    return ResolveResult.unqualifiedName(serviceName) + '[' + getLevel() + ',' + getHost() + ',' + getInstance() + ']';
  }

  public String getServiceName() {
    return serviceName;
  }

  /**
   * Connects the client to the specified service as delegated by VNS. Use this open method
   * in conjunction with the no-arg VinciClient constructor.
   *
   * @param serviceName The name of the service to connect to.
   * @throws ServiceDownException Thrown when either (1) VNS is inaccessible
   * or (2) none of the servers registered under serviceName are accessible.
   * @throws VNSException Thrown when VNS is accessible but reports an error,
   * which should almost always indicate that the requested service is not registered.
   * 
   * @pre service_name != null
   *
   * @throws IllegalStateException if no VNS_HOST has been specified.
   */
  public void open(String service_name) throws ServiceDownException, VNSException {
    this.serviceName = service_name;
    ResolveResult response = null;
    Transportable query = null;
    boolean connectFailed = false;
    VinciContext myContext = getContext();
    boolean usedCache = false;
    ArrayList alreadyTried = null;

    String vnsHost = myContext.getVNSHost();
    int vnsPort = myContext.getVNSPort();

    // Check to see if default host/port are being overridden by vns "@" specification in service name
    int atIndex = service_name.indexOf('@');
    if (atIndex != -1) {
      vnsHost = service_name.substring(atIndex + 1);
      int colonIndex = vnsHost.indexOf(':');
      if (colonIndex != -1) {
        try {
          vnsPort = Integer.parseInt(vnsHost.substring(colonIndex + 1));
        } catch (NumberFormatException e) {
          throw new VNSException("Bad vns port specification in service name: " + service_name);
        }
        vnsHost = vnsHost.substring(0, colonIndex);
      }
    }

    // ^^ A list of service locations that have already been determined to be
    // unavailable. 
    for (;;) {
      try {
        response = myContext.getCachedResolveResult(serviceName);
        if (response == null) {
          usedCache = false;
          if (query == null) {
            query = ResolveResult.composeQuery(serviceName);
          }
          response = (ResolveResult) BaseClient.sendAndReceive(query, vnsHost, vnsPort, ResolveResult.factory,
              myContext.getVNSResolveTimeout());
          myContext.cacheResolveResult(serviceName, response);
        } else {
          usedCache = true;
          Debug.p("Using cached VNS entry.");
        }
      } catch (IOException e) {
        // VNS is inaccessible. See if there is a (stale) cached entry we can use.
        if (myContext.areStaleLookupsAllowed()) {
          response = myContext.getStaleCachedResolveResult(serviceName);
          if (response == null) {
            throw new ServiceDownException("VNS inaccessible: " + e);
          } else {
            Debug.reportException(e);
            Debug.p("VNS is not accessible, using STALE cached resolve result.");
          }
        }
      } catch (ServiceException e) {
        if (connectFailed) {
          throw new ServiceDownException("Could not connect to service: " + serviceName);
        }
        throw new VNSException(e.getMessage());
      }
      // Iterate over all available service locations until we find one that
      // accepts our connection request.
      response.initializeIterator();
      while (response.hasMore()) {
        ResolveResult.ServiceLocator locator = response.getNext();
        if (alreadyTried == null || !alreadyTried.contains(locator.host + ":" + locator.port)) {
          try {
            open(locator.host, locator.port);
            this.instance = locator.instance;
            this.level = response.priority;
            Debug.p("Resolved " + serviceName + " to: " + locator.host + ":" + locator.port);
            return;
          } catch (IOException e) {
            Debug.p("WARNING: Failed to connect to service at (" + locator.host + ":" + locator.port + "):"
                + e.getMessage());
            connectFailed = true;
            if (alreadyTried == null) {
              alreadyTried = new ArrayList();
            }
            alreadyTried.add(locator.host + ":" + locator.port);
          }
        }
      }
      if (usedCache) {
        // It's possible that the cache entry is no longer up to date due
        // to sudden service restart. Flush the entry and try again.
        myContext.flushFromCache(serviceName);
        Debug.p("Retrying service resolution without using cache.");
      } else if (response.priority != 0 && !ResolveResult.isQualified(serviceName)) {
        // If a service name was specified without qualifications, then
        // we allow fail-over to lower-priority services.
        Debug.p("VinciClient.open(String)", "Resolving with lower priority than: " + response.priority);
        myContext.flushFromCache(serviceName);
        int next_level = response.priority;
        if (next_level == -1) {
          next_level = Integer.MAX_VALUE;
        } else {
          next_level--;
        }
        query = ResolveResult.composeQuery(serviceName, next_level);
      } else {
        break;
      }
    }
    throw new ServiceDownException("Could not connect to service: " + serviceName);
  }

  /**
   * @pre getServiceName() != null
   */
  protected void reopen(Exception e) throws IOException {
    Debug.p("Trying to reopen connection due to exception: " + e);
    // Make sure connection is closed.
    close();
    open(serviceName);
  }

  /**
   * Same as VinciClient.sendAndReceive(Transportable) except for return type. Syntactic sugar
   * method for the case where return result is known to be VinciFrame (eliminates the need for
   * casting in the typical usage case).
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   * @return A VinciFrame representing the service result.
   * 
   * @pre in != null
   * @pre service_name != null
   */
  static public VinciFrame rpc(Transportable in, String service_name) throws IOException, ServiceException,
      ServiceDownException, VNSException {
    return VinciContext.getGlobalContext().rpc(in, service_name);
  }

  /**
   * Same as VinciClient.sendAndReceive(Transportable, service_name) except it also takes a
   * timeout value.  WARNING: This method "hides" the BaseClient rpc method with the same
   * signature. The BaseClient method accepts (Transportable, host string, port) whereas this
   * one accepts (Transportable, service_name, timeout).  Despite the equivalent signatures, the
   * semantics of these two methods are quite different.
   *
   * @return A VinciFrame representing the service result.  
   *
   * @pre in != null
   * @pre service_name != null
   * @pre timeout >= 0
   */
  static public VinciFrame rpc(Transportable in, String service_name, int timeout) throws IOException,
      ServiceException, ServiceDownException, VNSException {
    return VinciContext.getGlobalContext().rpc(in, service_name, timeout);
  }

  /**
   * Same as VinciClient.sendAndReceive(Transportable, service_name) except it also takes socket
   * read and socket connect timeout values.  WARNING: This method "hides" the BaseClient rpc
   * method with the same signature. The BaseClient method accepts (Transportable, host string,
   * port) whereas this one accepts (Transportable, service_name, timeout).  Despite the
   * equivalent signatures, the semantics of these two methods are quite different.
   *
   * @return A VinciFrame representing the service result.  
   *
   * @pre in != null
   * @pre service_name != null
   * @pre timeout >= 0
   * @pre connect_timeout > 0
   */
  static public VinciFrame rpc(Transportable in, String service_name, int timeout, int connect_timeout)
      throws IOException, ServiceException, ServiceDownException, VNSException {
    return VinciContext.getGlobalContext().rpc(in, service_name, timeout, connect_timeout);
  }

  static public void main(String[] args) throws Exception {
    Frame ping = new VinciFrame().fadd(TransportConstants.PING_KEY, "hi");
    System.out.println(VinciClient.rpc(ping, args[0]));
  }

} // end class VinciClient
