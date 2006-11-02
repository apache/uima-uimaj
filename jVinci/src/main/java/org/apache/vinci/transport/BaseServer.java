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
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.vinci.debug.Debug;

/**
 * Class for running VinciServables in the Vinci framework. This implementation supports multiple
 * concurrent clients (in a Thread per client manner). Creating a service typically requires
 * defining implementing a VinciServable which is passed to this class to service requests.
 *
 * This class can be used independently of VNS. For VNS-enhanced serving, use the VinciServer.
 *
 * This class is designed to be extensible. For example you can extend to provide new Runnable
 * objects that are used to handle requests in the appropriate fashion. You can also override
 * configure socket to install SSL-supporting server sockets, and so on...
 */
public class BaseServer {

  static private final int DEFAULT_SOCKET_TIMEOUT = 60000; // one minute.
  static private final int DEFAULT_MAX_POOL_SIZE  = 20;

  static private final int SERVER_SOCKET_TIMEOUT  = 1000;

  volatile private boolean shutdown;
  volatile private boolean isServing;

  private ServerSocket     serverSocket;
  private VinciServable    servable;
  private Thread           servingThread;

  private int              socketTimeout;
  private int              initialPoolSize;
  private int              maxPoolSize;

  private int              pooledCount;
  private int              busyCount;
  private PooledThread[]   threadPool;
  private PooledThread[]   busyThreads;

  private class PooledThread extends Thread {
    private Runnable  run_me;
    private Socket    socket;

    private final int which;

    PooledThread(int which) {
      super();
      this.which = which;
      socket = null;
      run_me = null;
      setName("PooledThread#" + which);
    }

    int getWhich() {
      return which;
    }

    public void run() {
      try {
        while (isServing) {
          try {
            synchronized (this) {
              while (run_me == null) {
                wait();
              }
            }
            try {
              run_me.run();
            } catch (Throwable e) {
              Debug.reportException(e);
            } finally {
              synchronized (this) {
                run_me = null;
                socket = null;
              }
              synchronized (threadPool) {
                // Return thread to the pool.
                threadPool[pooledCount++] = this;
                busyThreads[which] = null;
                busyCount--;
                threadPool.notify();
              }
            }
          } catch (InterruptedException e) {
            Debug.p("interrupted");
          }
        } // while
      } // try
      catch (Throwable e) {
        Debug.reportException(e);
      } finally {
        Debug.p("pooled thread exit");
      }
    }

    void setRunnable(Runnable r, Socket c) {
      //Debug.p("Set runnable called: " + r);
      run_me = r;
      socket = c;
    }

    Socket getSocket() {
      return socket;
    }

  }

  /**
   * Create a new BaseServer that will delegate requests to the provided servable.
   *
   * @param my_servable The servable object implementing the service.
   * @pre my_servable != null
   */
  public BaseServer(VinciServable my_servable) {
    servable = my_servable;
    shutdown = false;
    isServing = false;
    serverSocket = null;
    servingThread = null;
    socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    threadPool = null;
    initialPoolSize = 1;
    pooledCount = 0;
    busyCount = 0;
    maxPoolSize = DEFAULT_MAX_POOL_SIZE;
  }

  /**
   * Get the servable object being used by this server.
   */
  public VinciServable getServable() {
    return servable;
  }

  /**
   * This method is used to override the default timeout value of one minute.  You can provide
   * "0" for "never timeout" but this is not recommended, as in some cases threads will end up
   * forever blocking for input, eventually causing the threadpool to max out and block all
   * pending requests.  
   * 
   * @param millis The socket timeout value in milliseconds.
   */
  public void setSocketTimeout(int millis) throws IOException {
    socketTimeout = millis;
  }

  /**
   * Call only within a threadPool synchronization block.
   */
  private void expandOrWait() throws InterruptedException {
    Debug.Assert(pooledCount == 0);
    if (busyCount < maxPoolSize) {
      Debug.p("Creating a thread for pool of current size " + busyCount);
      PooledThread add_me = new PooledThread(busyCount);
      add_me.start();
      threadPool[0] = add_me;
      pooledCount = 1;
    } else {
      Debug.p("WARNING: Blocking until pooled thread available. Consider expanding the pool size.");
      threadPool.wait();
    }
  }

  /**
   * Get an available thread from the thread pool. Block if no thread is available and
   * the maximum pool size has been reached.
   *
   * @pre threadPool != null
   */
  private PooledThread getThreadFromPool() throws InterruptedException {
    synchronized (threadPool) {
      Debug.p("Pooledcount: " + pooledCount + " busyCount: " + busyCount);
      if (pooledCount == 0) {
        expandOrWait();
      }
      PooledThread return_me = threadPool[--pooledCount];
      busyThreads[return_me.getWhich()] = return_me;
      busyCount++;
      return return_me;
    }
  }

  /**
   * Set the intitial and maximum size of the threadpool used by this server.
   * This should be called before serving starts otherwise it has no effect.
   */
  public void setThreadPoolSize(int initial, int max) {
    Debug.Assert(!isServing);
    initialPoolSize = initial;
    maxPoolSize = max;
  }

  private void configureServerSocket(int port) throws IOException {
    Debug.Assert(!isServing);
    serverSocket = createServerSocket(port);
    serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);// to detect shutdown.
  }

  private void initializeServing() {
    // isServing has to be true otherwise pooled threads will exit
    Debug.Assert(isServing);
    threadPool = new PooledThread[maxPoolSize];
    busyThreads = new PooledThread[maxPoolSize];
    synchronized (threadPool) {
      for (int i = 0; i < initialPoolSize; i++) {
        PooledThread add_me = new PooledThread(i);
        add_me.start();
        threadPool[i] = add_me;
      }
      pooledCount = initialPoolSize;
    }
  }

  /**
   * Asynchronously start serving requests. If this method returns without throwing an
   * exception, then another thread has been successfully launched to begin serving requests.
   *
   * @param port The port on which to listen for requests.
   * @exception IOException Thrown if there was some problem with the server socket.
   * @pre port >= 0
   * @pre port < 65536
   * @since 2.0.15
   */
  public void startServing(int port) throws IOException {
    configureServerSocket(port);
    new Thread(new Runnable() {
      public void run() {
        shutdown = false;
        isServing = true;
        try {
          initializeServing();
          handleRequests();
        } finally {
          isServing = false;
        }
      }
    }).start();
  }

  /**
   * Serve requests, blocking until a clean shutdown is triggered.
   *
   * @param port The port on which to listen for requests.
   * @exception IOException Thrown if there was some problem with the server socket.
   * @pre port >= 0
   * @pre port < 65536
   */
  public void serve(int port) throws IOException {
    configureServerSocket(port);
    shutdown = false;
    isServing = true;
    try {
      initializeServing();
      handleRequests();
    } finally {
      isServing = false;
    }
  }

  /**
   * Get the server socket that this server uses to listen for requests.
   *
   * @return The server's server socket
   */
  protected ServerSocket getServerSocket() {
    return serverSocket;
  }

  /**
   * Create the server socket used to listen for requests. This method can be overridden to
   * provide non-standard server sockets, such as those supporting SSL, those requiring special
   * configuration/initialization, etc.
   *
   * @return The server socket to be used by this server for accepting requests.
   * @param port The port which is to be listened to by the created socket. 
   * @pre port >= 0
   * @pre port < 65536
   */
  protected ServerSocket createServerSocket(int port) throws IOException {
    ServerSocket s = new ServerSocket(port);
    return s;
  }

  /**
   * Enter the server socket accept() loop. Loop terminates when shutdown shutdownServing()
   * method is called.  
   * 
   * @pre threadPool != null
   * @pre serverSocket != null
   */
  protected void handleRequests() {
    try {
      servingThread = Thread.currentThread();
      while (!shutdown) {
        try {
          Socket s = serverSocket.accept();
          s.setTcpNoDelay(true); // avoid delays with linux/loopback
          if (socketTimeout != 0) {
            s.setSoTimeout(socketTimeout);
          }
          handleRequest(s);
        } catch (InterruptedIOException e) {
          // Happens periodically to allow us to detect shutdown without having
          // to close() the server socket, since we've had problems when
          // calling close() to trigger shutdown, at least in JDK1.3.
        } catch (Exception e) {
          Debug.reportException(e);
        }
      }
    } finally {
      cleanExit();
    }
  }

  /**
   * Cause the serve() method to terminate.
   */
  public void shutdownServing() {
    // NOTE that we no longer use .close() of the server socket to trigger shutdown,
    // as this fails on Linux, and under Windows with Aventail connect it can cause
    // the system to hang.  Instead, we now have the socket interrupt every 1 second
    // to check the shutdown status.  This method will block for at most 2 seconds
    // so it may return before the server socket actually closes (though this shouldn't
    // happen in general).
    // NOTE2: The above note is probably due to Java 1.3 bugs... it probably isn't necessary
    // now, but this seems to work, so why change?
    shutdown = true;
    if (servingThread != null) {
      try {
        servingThread.join(SERVER_SOCKET_TIMEOUT * 2);
      } catch (InterruptedException e) {
        Debug.reportException(e);
        Thread.currentThread().interrupt(); // propagate interrupted state upwards.
      }
    }
  }

  /**
   * Get a runnable object to run within a pooled thread that will handle the request. 
   * 
   * @pre client != null
   */
  protected Runnable getRunnable(Socket client) {
    return new BaseServerRunnable(client, this);
  }

  /**
   * Initialize a new socket connection.
   *
   * @pre client != null
   * @pre threadPool != null
   */
  protected void handleRequest(Socket client) {
    try {
      PooledThread t = getThreadFromPool();
      synchronized (t) {
        t.setRunnable(getRunnable(client), client);
        t.notify();
      }
    } catch (InterruptedException e) {
      Debug.reportException(e);
    }
  }

  /**
   * Cleanly shut down this server. Called when handleRequests completes the accept loop.
   *
   * @pre serverSocket != null
   * @pre threadPool != null
   */
  protected void cleanExit() {
    isServing = false; // causes pooled threads to terminate.
    try {
      serverSocket.close();
    } catch (IOException e) {
      Debug.reportException(e);
    }
    synchronized (threadPool) {
      // Interrupt pooled threads that are not active so they can shut down.
      for (int i = 0; i < pooledCount; i++) {
        Debug.p("Interrupting pooled thread: " + threadPool[i].getWhich());
        threadPool[i].interrupt();
      }
      for (int i = 0; i < maxPoolSize; i++) {
        if (busyThreads[i] != null) {
          Debug.p("Interrupting pooled thread: " + i);
          busyThreads[i].interrupt();
          try {
            busyThreads[i].getSocket().close();
          } catch (IOException e) {
            Debug.reportException(e);
          }
        }
      }
    }
    // Finally perform the VinciServable-specific cleanup.
    servable.cleanExit();
  }

  /**
   * This function is a callback for the (Base/Vinci)ServerRunnable. It invokes the eval method
   * of the appropriate VinciServable.  
   *
   * @pre in != null
   */
  public Transportable eval(Transportable in, KeyValuePair header) {
    // Right now the header parameter is unused by it is provided so that it can be used for
    // service multiplexing by alternative server implementations.
    try {
      return servable.eval(in);
    } catch (ServiceException e) {
      return e.getCompleteDocument();
    }
  }

  /**
   * This is another callback for the (Base/Vinci)ServerRunnable that creates the document
   * to be populated by the service with the request response.
   *
   * @return The document the service will populate to create the request response.
   */
  public Transportable makeTransportable() {
    return servable.makeTransportable();
  }

}
