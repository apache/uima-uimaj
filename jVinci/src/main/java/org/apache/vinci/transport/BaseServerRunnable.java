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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.vinci.debug.Debug;

/**
 * Runnable class used by BaseServer to concurrently service requests.
 */
public class BaseServerRunnable implements Runnable {
  private Socket                   socket;
  private BaseServer               parent;

  private static final ThreadLocal THREAD_LOCAL_SOCKET = new ThreadLocal();

  /**
   * Allows anyone in the calling chain of the 'run' method to get access to the socket being
   * used in the Vinci connection via the ThreadLocal variable.  
   */
  public static Socket getSocket() {
    return (Socket) THREAD_LOCAL_SOCKET.get();
  }

  /**
   * @pre c != null
   * @pre p != null
   */
  public BaseServerRunnable(Socket c, BaseServer p) {
    socket = c;
    parent = p;
  }

  protected BaseServer getParent() {
    return parent;
  }

  public void run() {
    THREAD_LOCAL_SOCKET.set(socket);
    try {
      InputStream is = new BufferedInputStream(socket.getInputStream());
      OutputStream os = new BufferedOutputStream(socket.getOutputStream());
      while (true) {
        Transportable in = parent.makeTransportable();
        KeyValuePair header = null;
        try {
          //long begin = System.currentTimeMillis(); // TEMP
          header = in.fromStream(is);
          //Debug.p("Elapsed fromStream: " + (System.currentTimeMillis() - begin));
        } catch (EOFException e) {
          break;
        }
        Transportable out = handleHeader(header);
        if (out == null) {
          try {
            out = parent.eval(in, header);
          } catch (Throwable e) {
            Debug.reportException(e);
            out = new ErrorFrame("Server failed: " + e);
          }
        }
        if (out != null) {
          // ^ Asynch services may choose not to return results.
          //long begin = System.currentTimeMillis(); // TEMP
          out.toStream(os);
          os.flush();
          //Debug.p("Elapsed toStream: " + (System.currentTimeMillis() - begin));
        }
      }
    } catch (IOException e) {
      Debug.p("IOException in BaseServerRunnable: " + e);
    } catch (Throwable e) {
      Debug.reportException(e);
    } finally {
      THREAD_LOCAL_SOCKET.set(null);
      try {
        socket.close();
      } catch (IOException f) {
        Debug.reportException(f);
      }
    }
  }

  public Transportable handleHeader(KeyValuePair header) {
    return null;
  }
}// end class BaseServerRunnable
