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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.vinci.debug.Debug;

/**
 * Class for conjuring a Vinci service by host/port (that is, without interaction with the naming
 * service). Usually you want to use VinciClient, which extends this class to invoke a service by
 * (qualified) name.
 * 
 * Provides generic "send/recieve/sendAndReceive" for communicating arbitrary (transportable)
 * document models, and also specific "rpc" methods for more convenient support of the VinciFrame
 * document model.
 */
public class BaseClient {

  static public final int DEFAULT_SOCKET_TIMEOUT = 120000;

  static public final int DEFAULT_CONNECT_TIMEOUT = 30000;

  private String host = null;

  private int port = 0;

  private TransportableFactory factory;

  private Socket socket = null;

  private InputStream is = null;

  private OutputStream os = null;

  private KeyValuePair header = null;

  private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

  private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

  private boolean retry = true;

  /**
   * Open up the service at the specified host and port, using a VinciFrame factory.
   * 
   * @param h
   *          The hostname/ip address of the machine running the service.
   * @param p
   *          The port on which the service runs.
   * @throws IOException
   *           if the underlying socket fails to connect
   * 
   * @pre h != null
   * @pre p >= 0
   * @pre p < 65536
   */
  public BaseClient(String h, int p) throws IOException {
    this(h, p, VinciFrame.getVinciFrameFactory());
  }

  /**
   * Open up the service at the specified host and port, using a VinciFrame factory.
   * 
   * @param h
   *          The hostname/ip address of the machine running the service.
   * @param p
   *          The port on which the service runs.
   * @param connect_timeout
   *          The number of milliseconds that will elapse before a connect attempt fails.
   * 
   * @throws IOException
   *           if the underlying socket fails to connect
   * 
   * @pre h != null
   * @pre p >= 0
   * @pre p < 65536
   */
  public BaseClient(String h, int p, int connect_timeout) throws IOException {
    this(h, p, VinciFrame.getVinciFrameFactory(), connect_timeout);
  }

  /**
   * Open up the service at the specified host and port.
   * 
   * @param h
   *          The hostname/ip address of the machine running the service.
   * @param p
   *          The port on which the service runs.
   * @param f
   *          A factory for creating documents of the desired type.
   * @throws IOException
   *           if the underlying socket fails to connect.
   * 
   * @pre h != null
   * @pre f != null
   * @pre p >= 0
   * @pre p < 65536
   */
  public BaseClient(String h, int p, TransportableFactory f) throws IOException {
    this.factory = f;
    this.host = h;
    this.port = p;
    open(host, port);
  }

  /**
   * Open up the service at the specified host and port, using the specified connect timeout.
   * 
   * @param h
   *          The hostname/ip address of the machine running the service.
   * @param p
   *          The port on which the service runs.
   * @param f
   *          A factory for creating documents of the desired type.
   * @param timeout
   *          The number of milliseconds that will elapse before a connect attempt fails.
   * @throws IOException
   *           if the underlying socket fails to connect.
   * 
   * @pre h != null
   * @pre f != null
   * @pre p >= 0
   * @pre p < 65536
   * @pre timeout > 0
   */
  public BaseClient(String h, int p, TransportableFactory f, int timeout) throws IOException {
    this.factory = f;
    this.host = h;
    this.port = p;
    connectTimeout = timeout;
    open(host, port);
  }

  /**
   * Create a base client without establishing a connection. This is useful for client classes which
   * extend this class and which to perform their own connection establishment. Uses a VinciFrame
   * factory.
   */
  public BaseClient() {
    this(VinciFrame.getVinciFrameFactory());
  }

  /**
   * Create a base client without establishing a connection. This is useful for client classes which
   * extend this class and which to perform their own connection establishment.
   * 
   * @param factory
   *          A factory for creating documents of the desired type.
   * 
   * @pre f != null
   */
  public BaseClient(TransportableFactory f) {
    this.factory = f;
  }

  /**
   * Create a base client without establishing a connection. This is useful for client classes which
   * extend this class and which to perform their own connection establishment.
   * 
   * @param factory
   *          A factory for creating documents of the desired type.
   * @param timeout
   *          The number of milliseconds that will elapse before a connect attempt fails.
   * 
   * @pre f != null
   */
  public BaseClient(TransportableFactory f, int timeout) {
    this.factory = f;
    this.connectTimeout = timeout;
  }

  /**
   * Get the hostname/ip address to which this client is connected.
   */
  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  /**
   * Get the socket used for the connection.
   */
  protected Socket getSocket() {
    return socket;
  }

  /**
   * Get the default timeout value for the socket (0 indicates never timeout, which is the default,
   * but generally NOT a good setting).
   */
  public int getSocketTimeout() {
    return socketTimeout;
  }

  /**
   * Set the timeout value used for connect timeouts. Note that if you use one of the
   * connection-inducing constructors, then this method has no effect unless a subsequent connection
   * attempt is made.
   * 
   * @param timeout
   *          The number of milliseconds that will elapse before a connect attempt fails.
   */
  public void setConnectTimeout(int timeout) {
    connectTimeout = timeout;
  }

  /**
   * Set the transportable factory used by this client.
   */
  public void setTransportableFactory(TransportableFactory f) {
    factory = f;
  }

  /**
   * Takes a transportable object, sends it across the connection, then retrieves the response and
   * returns it.
   * 
   * @param in
   *          The query frame.
   * @return A transportable representing the result, its specific type determined by the factory
   *         provided through BaseClient's constructor.
   * @exception IOException
   *              thrown by the underlying socket IO (e.g. due to connection timeout).
   * @exception ServiceException
   *              thrown if the server threw ServiceException or returned an ErrorFrame.
   * 
   * @pre in != null
   * @pre getHost() != null
   */
  public Transportable sendAndReceive(Transportable in) throws IOException, ServiceException {
    try {
      try {
        if (!isOpen()) {
          open();
        }
        return sendAndReceiveWork(in, factory);
      } catch (SocketTimeoutException e) {
        // Don't retry on socket timeout - service might be overloaded
        throw e;
      } catch (IOException e) {
        if (retry) {
          reopen(e);
          return sendAndReceiveWork(in, factory);
        } else {
          throw e;
        }
      }
    } catch (SocketTimeoutException e) {
      // Close on timeout to make sure buffers are cleared if client reused
      close();
      throw e;
    }
  }

  /**
   * Takes a transportable object, sends it across the connection, then retrieves the response and
   * returns it.
   * 
   * @param in
   *          The query frame.
   * @param timeout
   *          The timeout value to use in place of this client's default timeout setting.
   * @return A transportable representing the result, its specific type determined by the factory
   *         provided through BaseClient's constructor.
   * @throws IOException
   *           thrown by the underlying socket IO (e.g. due to connection timeout).
   * @throws ServiceException
   *           thrown if the server threw ServiceException or returned an ErrorFrame.
   * 
   * @pre getHost() != null
   */
  public Transportable sendAndReceive(Transportable in, int timeout) throws IOException,
          ServiceException {
    try {
      try {
        if (!isOpen()) {
          open();
        }
        return sendAndReceiveWork(in, factory, timeout);
      } catch (SocketTimeoutException e) {
        // Don't retry on socket timeout - service might be overloaded
        throw e;
      } catch (IOException e) {
        if (retry) {
          reopen(e);
          return sendAndReceiveWork(in, factory, timeout);
        } else {
          throw e;
        }
      }
    } catch (SocketTimeoutException e) {
      // Close on timeout to make sure buffers are cleared if client reused
      close();
      throw e;
    }
  }

  /**
   * Same as sendAndReceive(Transportable) except the provided factory is used to create the return
   * document in place of the default factory.
   * 
   * @param f
   *          The factory to used to create the return document.
   * 
   * @pre f != null
   * @pre in != null
   * 
   * @pre getHost() != null
   */
  public Transportable sendAndReceive(Transportable in, TransportableFactory f) throws IOException,
          ServiceException {
    try {
      try {
        if (!isOpen()) {
          open();
        }
        return sendAndReceiveWork(in, f);
      } catch (SocketTimeoutException e) {
        // Don't retry on socket timeout - service might be overloaded
        throw e;
      } catch (IOException e) {
        if (retry) {
          reopen(e);
          return sendAndReceiveWork(in, f);
        } else {
          throw e;
        }
      }
    } catch (SocketTimeoutException e) {
      // Close on timeout to make sure buffers are cleared if client reused
      close();
      throw e;
    }
  }

  /**
   * Same as sendAndReceive(Transportable, timeout) except the provided factory is used to create
   * the return document in place of the default factory.
   * 
   * @param factory
   *          The factory to used to create the return document.
   * 
   * @pre in != null
   * @pre f != null
   * @pre getHost() != null
   */
  public Transportable sendAndReceive(Transportable in, TransportableFactory f, int timeout)
          throws IOException, ServiceException {
    try {
      try {
        if (!isOpen()) {
          open();
        }
        return sendAndReceiveWork(in, f, timeout);
      } catch (SocketTimeoutException e) {
        // Don't retry on socket timeout - service might be overloaded
        throw e;
      } catch (IOException e) {
        if (retry) {
          reopen(e);
          return sendAndReceiveWork(in, f, timeout);
        } else {
          throw e;
        }
      }
    } catch (SocketTimeoutException e) {
      // Close on timeout to make sure buffers are cleared if client reused
      close();
      throw e;
    }
  }

  /**
   * Same as sendAndReceive(Transportable) except for return type. Syntactic sugar method for the
   * case where return result is known to be VinciFrame (eliminates the need for casting in the
   * typical usage case).
   * 
   * @return A VinciFrame representing the service result.
   * 
   * @pre query != null
   * @pre getHost() != null
   */
  public VinciFrame rpc(Transportable query) throws IOException, ServiceException {
    return (VinciFrame) sendAndReceive(query);
  }

  /**
   * Same as sendAndReceive(Transportable, timeout) except for return type. Syntactic sugar method
   * for the case where return result is known to be VinciFrame (eliminates the need for casting in
   * the typical usage case).
   * 
   * @return A VinciFrame representing the service result.
   * 
   * @pre query != null
   * @pre getHost() != null
   */
  public VinciFrame rpc(Transportable query, int timeout) throws IOException, ServiceException {
    return (VinciFrame) sendAndReceive(query, timeout);
  }

  /**
   * Support for 1/2 transaction RPC. This allows interaction with an asynchronous ("receive only")
   * service, or for the sender to simply do something else before coming back and receiving the
   * result (though at the risk of timeouts!).
   * 
   * @param in
   *          The Transportable to send.
   * @throws IOException
   *           Thrown by the underlying transport layer.
   * 
   * @pre in != null
   * @pre getHost() != null
   * @pre os != null
   */
  public void send(Transportable in) throws IOException {
    try {
      try {
        if (!isOpen()) {
          open();
        }
        in.toStream(os);
        os.flush();
      } catch (SocketTimeoutException e) {
        // Don't retry on socket timeout - service might be overloaded
        throw e;
      } catch (IOException e) {
        if (retry) {
          reopen(e);
          in.toStream(os);
          os.flush();
        } else {
          throw e;
        }
      }
    } catch (SocketTimeoutException e) {
      // Close on timeout to make sure buffers are cleared if client reused
      close();
      throw e;
    }
  }

  /**
   * The other 1/2 of the split RPC. This allows for interaction with an asynchronous "publish only"
   * service, or simply picks up a result queried for earlier via send().
   * 
   * @return The Transportable requested.
   * @throws IOException
   *           Thrown by the underlying transport layer, or the socket is closed.
   * @throws ServiceException
   *           Thrown if the remote server responded with an error frame.
   * 
   * @pre is != null
   */
  public Transportable receive() throws IOException, ServiceException {
    if (!isOpen()) {
      throw new IOException("Socket not open");
    }
    Transportable out = factory.makeTransportable();
    header = out.fromStream(is);
    if (header != null && header.key.equals(TransportConstants.ERROR_KEY)) {
      throw new ServiceException(header.getValueAsString(), out);
    }
    return out;
  }

  /**
   * Close the connection. Using the Client object after this will throw an exception.
   */
  public void close() {
    if (isOpen()) {
      try {
        socket.close();
        is = null;
        os = null;
        socket = null;
      } catch (IOException e) {
        Debug.reportException(e, "Could not close connection.");
      }
    }
  }

  public boolean isOpen() {
    return socket != null && os != null && is != null;
  }

  /**
   * Set connection restablishment on IOException to on/off, default is ON. This way, by default,
   * BaseClient attempts to reopen a connection at most once if it receives an IOException which can
   * happen, for example, from the connection timing out.
   */
  public void setRetry(boolean to) {
    retry = to;
  }

  /**
   * Fetch the header of the last Transportable received.
   */
  public KeyValuePair getHeader() {
    return header;
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections.
   * 
   * @pre host_name != null
   * @pre f != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public Transportable sendAndReceive(Transportable in, String host_name, int p,
          TransportableFactory f) throws IOException, ServiceException {
    BaseClient tempClient = new BaseClient(host_name, p, f);
    tempClient.setRetry(false);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections with socket timeout support.
   * 
   * @pre in != null
   * @pre host_name != null
   * @pre f != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public Transportable sendAndReceive(Transportable in, String host_name, int p,
          TransportableFactory f, int socket_timeout) throws IOException, ServiceException {
    BaseClient tempClient = new BaseClient(host_name, p, f);
    tempClient.setSocketTimeout(socket_timeout);
    tempClient.setRetry(false);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections with socket timeout support &
   * connect timeout support.
   * 
   * @pre in != null
   * @pre host_name != null
   * @pre f != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public Transportable sendAndReceive(Transportable in, String host_name, int p,
          TransportableFactory f, int socket_timeout, int connect_timeout) throws IOException,
          ServiceException {
    BaseClient tempClient = new BaseClient(host_name, p, f, connect_timeout);
    tempClient.setSocketTimeout(socket_timeout);
    tempClient.setRetry(false);
    try {
      return tempClient.sendAndReceive(in);
    } finally {
      tempClient.close();
    }
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections. Same as sendAndReceive except
   * uses VinciFrame factory so return type is known to be VinciFrame.
   * 
   * @pre in != null
   * @pre host_name != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public VinciFrame rpc(Transportable in, String host_name, int p) throws IOException,
          ServiceException {
    return (VinciFrame) sendAndReceive(in, host_name, p, VinciFrame.getVinciFrameFactory());
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections. Same as sendAndReceive except
   * uses VinciFrame factory so return type is known to be VinciFrame.
   * 
   * @pre host_name != null
   * @pre in != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public VinciFrame rpc(Transportable in, String host_name, int p, int socket_timeout)
          throws IOException, ServiceException {
    return (VinciFrame) sendAndReceive(in, host_name, p, VinciFrame.getVinciFrameFactory(),
            socket_timeout);
  }

  /**
   * Convenience method for "one-shot" or "single-query" connections. Same as sendAndReceive except
   * uses VinciFrame factory so return type is known to be VinciFrame.
   * 
   * @pre host_name != null
   * @pre in != null
   * @pre p >= 0
   * @pre p < 65536
   */
  static public VinciFrame rpc(Transportable in, String host_name, int p, int socket_timeout,
          int connect_timeout) throws IOException, ServiceException {
    return (VinciFrame) sendAndReceive(in, host_name, p, VinciFrame.getVinciFrameFactory(),
            socket_timeout, connect_timeout);
  }

  /**
   * @pre in != null
   * @pre f != null
   * @pre getHost() != null
   * @pre os != null
   * @pre is != null
   */
  protected Transportable sendAndReceiveWork(Transportable in, TransportableFactory f)
          throws IOException, ServiceException {
    in.toStream(os);
    os.flush();
    Transportable out = f.makeTransportable();
    header = out.fromStream(is);
    if (header != null && header.key.equals(TransportConstants.ERROR_KEY)) {
      throw new ServiceException(header.getValueAsString(), out);
    }
    return out;
  }

  /**
   * @pre in != null
   * @pre f != null
   * @pre socket != null
   */
  protected Transportable sendAndReceiveWork(Transportable in, TransportableFactory f, int timeout)
          throws IOException, ServiceException {
    socket.setSoTimeout(timeout);
    try {
      return sendAndReceiveWork(in, f);
    } finally {
      // Restore default timeout setting.
      socket.setSoTimeout(socketTimeout);
    }
  }

  /**
   * @pre e != null
   * @pre getHost() != null
   */
  protected void reopen(Exception e) throws IOException {
    Debug.p("Trying to reopen connection due to exception: " + e.getMessage());
    // Make sure connection is closed.
    close();
    open();
  }

  /**
   * Connects the client to the specified host and port.
   * 
   * @param h
   *          The hostname/ip address of the server to connect to.
   * @param p
   *          The port to connect to.
   * 
   * @throws IOException
   *           Thrown by underlying Socket open() call.
   * 
   * @pre h != null
   * @pre p >= 0
   * @pre p < 65536
   */
  protected final void open(String h, int p) throws IOException {
    this.host = h;
    this.port = p;
    open();
  }

  /**
   * (Re)connects the client to a previously specified host and port. Should only be called if this
   * client has been previously closed via a call to "close".
   * 
   * @throws IOException
   *           Thrown by underlying Socket open() call.
   * 
   * @pre getHost() != null
   */
  public final void open() throws IOException {
    socket = new Socket();
    InetAddress addr = InetAddress.getByName(host);
    InetSocketAddress socketAddress = new InetSocketAddress(addr, port);
    socket.connect(socketAddress, connectTimeout);
    socket.setTcpNoDelay(true); // needed to avoid delays with Linux/loopback
    socket.setSoTimeout(socketTimeout);
    socket.setKeepAlive(isSocketKeepAliveEnabled());    
    is = new BufferedInputStream(socket.getInputStream());
    os = new BufferedOutputStream(socket.getOutputStream());
  }

  /**
   * Make this client use an already established socket connection. If you use this open method,
   * then setRetry is set to false. Resetting it to true will cause problems since the client does
   * not know how to reopen the connection.
   * 
   * @param use_me
   *          The socket to use.
   * 
   * @exception IOException
   *              Thrown by underlying Socket open() call.
   * 
   * @pre use_me != null
   */
  public void open(Socket use_me) throws IOException {
    setRetry(false);
    socket = use_me;
    socket.setSoTimeout(socketTimeout);
    socket.setKeepAlive(isSocketKeepAliveEnabled());    
    is = new BufferedInputStream(socket.getInputStream());
    os = new BufferedOutputStream(socket.getOutputStream());
  }

  /**
   * Set the timeout value used by the underlying socket. Default is 2 minutes.
   */
  public void setSocketTimeout(int millis) throws IOException {
    if (socket != null) {
      socket.setSoTimeout(millis);
    }
    socketTimeout = millis;
  }
  
  /**
   * Gets whether keepAlive should be turned on for client sockets.
   * Always returns true for BaseClient.  Can be overridden in
   * subclasses.
   *
   *@return whether socket keepAlive should be turned on
   */
  protected boolean isSocketKeepAliveEnabled() {
    return true;
  }

} // end class BaseClient
