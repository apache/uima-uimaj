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

package org.apache.vinci.transport.context;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.ServiceDownException;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VNSException;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.vns.client.ResolveResult;

/**
 * <p>This class can be used to globally affect various behaviors of Vinci clients and servers
 * under its control (for example, which VNS to contact). There is one "global" VinciContext that
 * is used by default, though anyone can create an alternate context should multiple sets of
 * defaults be required in a single program. Each VinciContext also serves to cache resolve
 * results for all clients under its control (for a default of 1 minute). </p> 
 *
 * <p>For the most part, Vinci library users won't have to directly deal with this class, since
 * there is a global context used by default which is almost always adequate.</p>
 *
 * <p>The most common use of this class is to programmatically specify a VNS hostname other than
 * the default (which is the one specified by the Java property VNS_HOST).</p> 
 */

public class VinciContext {

  public static final int     DEFAULT_VNS_CACHE_SIZE  = 1000;

  private static final String VNS_HOST_PROPERTY       = "VNS_HOST";
  private static final String VNS_HOST_PROPERTY_2     = "VNSHOST";
  private static final String VNS_PORT_PROPERTY       = "VNS_PORT";
  private static final String VNS_PORT_PROPERTY_2     = "VNSPORT";
  private static final int    DEFAULT_VNS_PORT        = 9000;

  static private VinciContext globalContext;
  static {
    String portString = System.getProperty(VNS_PORT_PROPERTY);
    if (portString == null) {
      portString = System.getProperty(VNS_PORT_PROPERTY_2);
    }
    int port;
    if (portString != null) {
      try {
        port = Integer.parseInt(portString);
      } catch (Exception e) {
        Debug.reportException(e);
        port = DEFAULT_VNS_PORT;
      }
    } else {
      port = DEFAULT_VNS_PORT;
    }
    String host = System.getProperty(VNS_HOST_PROPERTY);
    if (host == null) {
      host = System.getProperty(VNS_HOST_PROPERTY_2);
    }
    globalContext = new VinciContext(host, port);
  }

  private static final int    DEFAULT_RESOLVE_TIMEOUT = 20000;
  private static final int    DEFAULT_SERVEON_TIMEOUT = 60000;
  // Default time to live for cached VNS entries.
  private static final int    DEFAULT_TTL             = 60000;

  private int                 vnsResolveTimeout       = DEFAULT_RESOLVE_TIMEOUT;
  private int                 vnsServeonTimeout       = DEFAULT_SERVEON_TIMEOUT;
  private int                 ttlMillis               = DEFAULT_TTL;

  // Whether or not to use stale cached entries
  // in the event VNS is inaccessible.
  private boolean             allowStaleLookups       = true;

  private int                 vnsCacheSize            = DEFAULT_VNS_CACHE_SIZE;

  private String              host;
  private int                 port;

  static class CachedVNSResult {
    CachedVNSResult(ResolveResult r) {
      this.r = r;
      entryTime = System.currentTimeMillis();
    }

    ResolveResult r;
    long          entryTime;
  }

  /**
   * Set the size of the VNS cache. This method can be invoked at any time, however if the cache size
   * is ever reduced, it might not take effect.
   * 
   * @param to The limit on the number of entries that will be maintained in the VNS cache.
   */
  synchronized public void setVNSCacheSize(int to) {
    vnsCacheSize = to;
  }

  private LinkedHashMap vnsCache = new LinkedHashMap(16, .75f, true) {
                                   private static final long serialVersionUID = 7138871637782205744L;

                                   protected boolean removeEldestEntry(Map.Entry e) {
                                     return size() > vnsCacheSize;
                                   }
                                 };

  /**
   * @pre myport >= 0
   * @pre myport < 65536
   */
  public VinciContext(String myhost, int myport) {
    this.host = myhost;
    this.port = myport;
  }

  /**
   * Return the VNS hostname. When the global instance of this class is first loaded, it will
   * set the hostname from the java property VNS_HOST.  To set the VNS_HOST using this java
   * property, you must therefore specify the property before the class is ever referenced,
   * e.g. through the command-line property option -DVNS_HOST=[hostname], or by calling
   * System.setProperty("VNS_HOST", [hostname]) before ever invoking any Vinci client
   * code. Otherwise, you can set the hostname using the setVNSHost() method provided by this
   * class.  
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   */
  public String getVNSHost() {
    if (host == null) {
      throw new IllegalStateException(
          "No VNS host or IP address has been specified! You can specify the VNS host or IP address through the Java command-line option -DVNS_HOST=[hostname], or programmatically using either System.setProperty(\"VNS_HOST\",[hostname]) or VinciContext.getGlobalContext().setVNSHost([hostname]).");
    } else {
      return host;
    }
  }

  /**
   * Return the VNS listener port. When the global instance of this class is first loaded, it
   * will attempt to set the port number from the java property VNS_PORT.  To set the port using
   * this java property, you must therefore specify the VNS_PORT property before the class is
   * ever referenced, e.g. through the command-line property option -DVNS_PORT=[hostname], or by
   * calling System.setProperty("VNS_PORT", [hostname]) before ever invoking any Vinci client
   * code. Otherwise, the port will default to 9000. You can override this default (or any
   * property-specified value) by calling the setPort() method provided by this class.  
   */
  public int getVNSPort() {
    return port;
  }

  /**
   * Set the VNS hostname.
   */
  public void setVNSHost(String h) {
    host = h;
    flushAll(); // Flush the cache in case entries are cached from previous VNS setting
  }

  /**
   * Set the VNS port.
   * 
   * @pre port >= 0
   * @pre port < 65536
   */
  public void setVNSPort(int p) {
    port = p;
    flushAll(); // Flush the cache in case entries are cached from previous VNS setting
  }

  /**
   * Returns whether clients can use stale resolve cache entries for service location in the
   * event VNS is unreachable.
   */
  public boolean areStaleLookupsAllowed() {
    return allowStaleLookups;
  }

  /**
   * Set whether clients can use stale resolve cache entries for service location in the event
   * VNS is unreachable. Default is true. 
   */
  public void setAllowStaleLookups(boolean b) {
    allowStaleLookups = b;
  }

  /**
   * Get the time-to-live of cached service locators (resolve results).
   */
  public int getResolveCacheTTL() {
    return ttlMillis;
  }

  /**
   * Set the time-to-live of cached service locators (resolve results). Default is 1 minute. Set
   * to 0 to disable caching completely.
   */
  public void setResolveCacheTTL(int millis) {
    ttlMillis = millis;
  }

  /**
   * Get the timeout setting of VNS resolve queries. 
   */
  public int getVNSResolveTimeout() {
    return vnsResolveTimeout;
  }

  /**
   * Set the timeout of VNS resolve queries. Default is 20 seconds.
   */
  public void setVNSResolveTimeout(int millis) {
    vnsResolveTimeout = millis;
  }

  /**
   * Get the timeout setting of VNS serveon queries.
   */
  public int getVNSServeonTimeout() {
    return vnsServeonTimeout;
  }

  /**
   * Set the timeout of VNS serveon queries. Default is 60 seconds.
   */
  public void setVNSServeonTimeout(int millis) {
    vnsServeonTimeout = millis;
  }

  /**
   * Get the global VinciContext used by Vinci classes when no context is explicitly
   * specified.
   */
  static public VinciContext getGlobalContext() {
    return globalContext;
  }

  /**
   * Get a cached resolve result (if any).
   *
   * @return the cached resolve result, or null if none is cached.
   * 
   * @pre serviceName != null
   */
  synchronized public ResolveResult getCachedResolveResult(String serviceName) {
    CachedVNSResult r = (CachedVNSResult) vnsCache.get(serviceName);
    if (r != null) {
      long now = System.currentTimeMillis();
      if (now - r.entryTime <= ttlMillis) {
        return r.r;
      }
    }
    return null;
  }

  /**
   * Get a cached resolve result (if any), but allow returning stale cache entries.
   *
   * @return the cached resolve result, or null if none is cached. 
   * 
   * @pre serviceName != null
   */
  synchronized public ResolveResult getStaleCachedResolveResult(String serviceName) {
    CachedVNSResult r = (CachedVNSResult) vnsCache.get(serviceName);
    if (r != null) {
      return r.r;
    }
    return null;
  }

  /**
   * Provide a resolve result to cache.
   * 
   * @pre serviceName != null
   * @pre r != null
   */
  synchronized public void cacheResolveResult(String serviceName, ResolveResult r) {
    CachedVNSResult c = (CachedVNSResult) vnsCache.get(serviceName);
    if (c == null) {
      vnsCache.put(serviceName, new CachedVNSResult(r));
    } else {
      c.r = r;
      c.entryTime = System.currentTimeMillis();
    }
    //long now = System.currentTimeMillis();
  }

  /**
   * Flush any cache entries pertaining to the specified service.
   * 
   * @pre serviceName != null
   */
  synchronized public void flushFromCache(String serviceName) {
    vnsCache.remove(serviceName);
  }

  synchronized public void flushAll() {
    vnsCache.clear();
  }

  /**
   * See documentation for VinciClient.sendAndReceive().
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   */
  public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory)
      throws IOException, ServiceException, ServiceDownException, VNSException {
    VinciClient tempClient = new VinciClient(service_name, factory, this);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * See documentation for VinciClient.sendAndReceive().
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   * @pre socket_timeout >= 0
   */
  public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory,
      int socket_timeout) throws IOException, ServiceException {
    VinciClient tempClient = new VinciClient(service_name, factory, this);
    tempClient.setSocketTimeout(socket_timeout);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * See documentation for VinciClient.sendAndReceive().
   *
   * WARNING: This method relies on JDK-1.4 specific functions.  USE IT ONLY if you don't need
   * to maintain JDK1.3 compatability.
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre factory != null
   * @pre socket_timeout >= 0
   */
  public Transportable sendAndReceive(Transportable in, String service_name, TransportableFactory factory,
      int socket_timeout, int connect_timeout) throws IOException, ServiceException {
    VinciClient tempClient = new VinciClient(service_name, factory, this, connect_timeout);
    tempClient.setSocketTimeout(socket_timeout);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * See documentation for VinciClient.rpc().
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   */
  public VinciFrame rpc(Transportable in, String service_name) throws IOException, ServiceException,
      ServiceDownException, VNSException {
    VinciClient tempClient = new VinciClient(service_name, this);
    try {
      return tempClient.rpc(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * See documentation for VinciClient.rpc().
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre timeout >= 0
   */
  public VinciFrame rpc(Transportable in, String service_name, int timeout) throws IOException, ServiceException,
      ServiceDownException, VNSException {
    VinciClient tempClient = new VinciClient(service_name, this);
    tempClient.setSocketTimeout(timeout);
    try {
      return tempClient.rpc(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * See documentation for VinciClient.rpc().
   *
   * WARNING: This method relies on JDK-1.4 specific functions.  USE IT ONLY if you don't need
   * to maintain JDK1.3 compatability.
   *
   * @see org.apache.vinci.transport.VinciClient
   *
   * @throws IllegalStateException if the VNS host has not been specified.
   *
   * @pre in != null
   * @pre service_name != null
   * @pre timeout >= 0
   */
  public VinciFrame rpc(Transportable in, String service_name, int socket_timeout, int connect_timeout)
      throws IOException, ServiceException, ServiceDownException, VNSException {
    VinciClient tempClient = new VinciClient(service_name, this, connect_timeout);
    tempClient.setSocketTimeout(socket_timeout);
    try {
      return tempClient.rpc(in);
    } finally {
      tempClient.close();
    }
  }

}
