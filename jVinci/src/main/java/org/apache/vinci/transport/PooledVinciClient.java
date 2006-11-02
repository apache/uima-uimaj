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

/**
 * Maintains a pool of connections to a given service and allows thread-safe querying of that
 * service. This provides a set of sendAndReceive methods with signatures equivalent to those in
 * VinciClient, but unlike VinciClient, the methods can be invoked concurrently by multiple
 * threads.
 */
public class PooledVinciClient {

  private VinciContext         context        = VinciContext.getGlobalContext();
  private TransportableFactory factory        = VinciFrame.getVinciFrameFactory();
  int                          connectTimeout = BaseClient.DEFAULT_CONNECT_TIMEOUT;
  int                          socketTimeout  = BaseClient.DEFAULT_SOCKET_TIMEOUT;

  private String               serviceName;
  private int                  maxPoolSize;
  private VinciClient[]        availableClients;
  private int                  availableClientsStartIndex;
  boolean                      closed;

  /**
   * Create a PooledVinciClient that will establish at most maxPoolSize connections to the designated service.
   */
  public PooledVinciClient(String serviceName, int maxPoolSize) {
    this.serviceName = serviceName;
    this.maxPoolSize = maxPoolSize;
    this.availableClients = new VinciClient[maxPoolSize];
    this.availableClientsStartIndex = 0;
    this.closed = false;
  }

  /**
   * Set a VinciContext that will be used by this PooledVinciClient instead of the default global context.
   */
  public void setContext(VinciContext context) {
    this.context = context;
  }

  /**
   * Set a connect timeout that will be used in place of BaseClient.DEFAULT_CONNECT_TIMEOUT
   */
  public void setConnectTimeout(int connectTimeoutMillis) {
    this.connectTimeout = connectTimeoutMillis;
  }

  /**
   * Set a socket timeout that will be used in place of BaseClient.DEFAULT_SOCKET_TIMEOUT
   */
  public void setSocketTimeout(int socketTimeoutMillis) {
    this.socketTimeout = socketTimeoutMillis;
  }

  /**
   * Set a transportable factory that will be used in place of the VinciFrame factory.
   */
  public void setTransportableFactory(TransportableFactory factory) {
    this.factory = factory;
  }

  /**
   * Get the service name to which this client connects.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Send a request to the service and receive the response. This method is tread safe.
   */
  public Transportable sendAndReceive(Transportable in) throws IOException, ServiceException {
    VinciClient c = getClientFromPool();
    try {
      return c.sendAndReceive(in);
    } finally {
      releaseClient(c);
    }
  }

  /**
   * Send a request to the service and receive the response, using the provided transportable
   * factory in place of the client-provided one. This method is tread safe.
   */
  public Transportable sendAndReceive(Transportable in, TransportableFactory f) throws IOException, ServiceException {
    VinciClient c = getClientFromPool();
    try {
      return c.sendAndReceive(in, f);
    } finally {
      releaseClient(c);
    }
  }

  /**
   * Send a request to the service and receive the response, using the provided transportable
   * factory and socketTimeout in place of the client-provided ones. This method is tread safe.
   */
  public Transportable sendAndReceive(Transportable in, TransportableFactory f, int socketTimeout) throws IOException,
      ServiceException {
    VinciClient c = getClientFromPool();
    try {
      return c.sendAndReceive(in, f, socketTimeout);
    } finally {
      releaseClient(c);
    }
  }

  /**
   * Send a request to the service and receive the response, using the provided 
   * socketTimeout in place of the client-provided one. This method is tread safe.
   */
  public Transportable sendAndReceive(Transportable in, int socketTimeout) throws IOException, ServiceException {
    VinciClient c = getClientFromPool();
    try {
      return c.sendAndReceive(in, socketTimeout);
    } finally {
      releaseClient(c);
    }
  }

  /**
   * Close this pooled client. Blocked requests will return IOException, as will any requests
   * following the invocation of this method. Once a pooled client is closed it cannot be
   * reused.
   *
   * @param wait If true, this method will block until all in-progress requests have completed, otherwise
   * this method will return immediately (though in progress requests will still be allowed to complete)
   */
  public void close(boolean wait) {
    ArrayList closeUs = new ArrayList();
    synchronized (this) {
      if (!closed) {
        closed = true;
        for (int i = availableClientsStartIndex; i < maxPoolSize; i++) {
          if (availableClients[availableClientsStartIndex] != null) {
            closeUs.add(availableClients[availableClientsStartIndex]);
            availableClients[availableClientsStartIndex] = null;
          }
        }
        this.notifyAll();
      }
    }
    for (int i = 0; i < closeUs.size(); i++) {
      ((VinciClient) closeUs.get(i)).close();
    }
    if (wait) {
      boolean wasInterrupted = false;
      synchronized (this) {
        while (availableClientsStartIndex > 0) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            wasInterrupted = true;
          }
        }
      }
      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Retrieve a client from the pool, blocking for at most "socketTimeout" seconds. If the wait
   * block time is exceeded, an IOException will be thrown. Connections obtained by this method
   * *must* be released by calling releaseClient().
   *
   * @throws IOException if the wait() time before a connectoin becomes available exceeds the
   * socketTimeout value, or if thrown by a failed service connection attempt.
   */
  private VinciClient getClientFromPool() throws IOException {
    VinciClient client;
    synchronized (this) {
      if (availableClientsStartIndex == maxPoolSize && !closed) {
        try {
          this.wait(socketTimeout);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("interrupted while waiting for available client");
        }
        if (availableClientsStartIndex == maxPoolSize) {
          throw new IOException("waited too long for available client");
        }
      }
      if (closed) {
        throw new IOException("client is closed");
      }
      if (availableClients[availableClientsStartIndex] == null) {
        Debug.p("Creating new client for pool: " + availableClientsStartIndex);
        VinciClient c = new VinciClient(factory);
        c.setConnectTimeout(connectTimeout);
        c.setSocketTimeout(socketTimeout);
        c.setContext(context);
        availableClients[availableClientsStartIndex] = c;
      }
      client = availableClients[availableClientsStartIndex];
      availableClientsStartIndex++;
    } // synchronized this
    try {
      if (!client.isOpen()) {
        client.open(serviceName);
      }
      VinciClient returnMe = client;
      client = null;
      return returnMe;
    } finally {
      if (client != null) {
        releaseClient(client);
      }
    }
  }

  /**
   * Release a client that has been obtained from getClientFromPool.
   */
  synchronized private void releaseClient(VinciClient c) {
    if (closed) {
      --availableClientsStartIndex;
      this.notify();
      c.close();
    } else {
      availableClients[--availableClientsStartIndex] = c;
      this.notify();
    }
  }

} // end class PooledVinciClient
