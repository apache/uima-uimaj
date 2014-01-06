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

package org.apache.uima.collection.impl.cpm.container.deployer.socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;

/**
 * 
 * 
 */
public class OFSocketTransportImpl implements SocketTransport {

  
  public OFSocketTransportImpl() {
    super();

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketTransport#getName()
   */
  public String getName() {

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketTransport#connect(java.net.URL,
   *      long)
   */
  public Socket connect(URL aURI, long aTimeout) throws SocketException {
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                Thread.currentThread().getName() + "-Created Connection to Fenced Service");
      }
      return new Socket(aURI.getHost(), aURI.getPort());
    } catch (SocketException e) {
      throw e;
    } catch (Exception e) {
      throw new SocketException(e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketTransport#invoke(java.net.Socket,
   *      org.apache.uima.cas.CAS)
   */
  public CAS process(Socket aSocket, CAS aCas) throws SocketTimeoutException, SocketException {
    DataOutputStream os = null;

    DataInputStream is = null;

    try {
      if (aSocket.isClosed()) {
        aSocket = connect(new URL("http", aSocket.getInetAddress().getHostName(),
                aSocket.getPort(), ""), 100000);
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                Thread.currentThread().getName() + "-Sending Request to Fenced Service.");
      }
      os = new DataOutputStream(aSocket.getOutputStream());
      is = new DataInputStream(aSocket.getInputStream());

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                Thread.currentThread().getName() + "-Processing Response");
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      os.writeBytes("HELLO\n");

      String responseLine;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                Thread.currentThread().getName() + "-Showing Response");
      }
      if ((responseLine = reader.readLine()) != null) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                "Server Response: " + responseLine);
      }
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              Thread.currentThread().getName() + "-Done Showing Response");

    } catch (SocketException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new SocketException(e.getMessage());
    } finally {
      try {
        if (os != null) {
          os.close();
        }
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
      }
    }

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketTransport#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData(Socket aSocket)
          throws SocketException {

    return null;
  }

}
