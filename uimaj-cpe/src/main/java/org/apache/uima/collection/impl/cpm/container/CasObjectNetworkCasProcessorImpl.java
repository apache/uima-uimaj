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

package org.apache.uima.collection.impl.cpm.container;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketTransport;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.impl.ProcessingResourceMetaData_impl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;

/**
 * Implementation of the {@link CasObjectProcessor} interface used for both Local and Remote
 * CasObjectProcessors. This objects plugs in a transport object defined in the CPE Descriptor and
 * uses it to delegate analysis of CAS to a remote AE.
 * 
 */
public class CasObjectNetworkCasProcessorImpl implements CasObjectProcessor {
  private SocketTransport transport = null;

  private String name = null;

  private Socket socket = null;

  private long timeout = 0;

  private ProcessingResourceMetaData metadata = null;

  /**
   * Using information from the CPE descriptor creates an instance of Transport class. The transport
   * class will delegate analysis of CAS to a remote object.
   * 
   * @param aCasProcessor -
   *          Cas Process configuration from the CPE descriptor
   */
  public CasObjectNetworkCasProcessorImpl(CpeCasProcessor aCasProcessor)
          throws ResourceConfigurationException {
    if (aCasProcessor.getDeploymentParams() != null) {
      CasProcessorDeploymentParams params = aCasProcessor.getDeploymentParams();
      try {
        CasProcessorDeploymentParam transportParameter = params.get("transport");
        transport = pluginTransport(transportParameter.getParameterValue());
      } catch (Exception e) {
        throw new ResourceConfigurationException(InvalidXMLException.INVALID_CLASS,
                new Object[] { "transport" }, e);
      }

      CasProcessorDeploymentParam[] deployParameters = params.getAll();
      for (int i = 0; deployParameters != null && i < deployParameters.length; i++) {
        try {
          if ("transport".equalsIgnoreCase(deployParameters[i].getParameterName())) {
            String transportClass = deployParameters[i].getParameterValue();
            transport = pluginTransport(transportClass);
            break;
          }
        } catch (Exception e) {
          throw new ResourceConfigurationException(InvalidXMLException.INVALID_CLASS,
                  new Object[] { "transport" }, e);
        }

      }
    } else {
      throw new ResourceConfigurationException(InvalidXMLException.INVALID_CPE_DESCRIPTOR,
              new Object[] { "transport" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_bad_transport__WARNING",
                      new Object[] { Thread.currentThread().getName() })));

    }
    timeout = aCasProcessor.getErrorHandling().getTimeout().get();
    // Instantiate metadata object to store configuration information
    metadata = new ProcessingResourceMetaData_impl();
    name = aCasProcessor.getName();
    // Each CasProcessor has name
    metadata.setName(name);
  }

  /**
   * Create Transport object from a given class and associate it with this CasProcessor
   * 
   * @param aTransportClass -
   *          name of the Transport class
   * @return - instance of SocketTransport
   * @throws Exception passthru
   */
  private SocketTransport pluginTransport(String aTransportClass) throws Exception {
    Object instance = Class.forName(aTransportClass).newInstance();
    if (instance instanceof SocketTransport) {
      return (SocketTransport) instance;
    }
    return null;
  }

  /**
   * Creates URL object containing service endpoint info ( host and port)
   * 
   * @return URL
   */
  public URL getEndpoint() {
    if (socket != null) {
      try {
        return new URL("http", socket.getInetAddress().getHostName(), socket.getPort(), "");
      } catch (MalformedURLException e) {
        // shouldnt happen
      }
    }
    return null;
  }

  /**
   * Connects to a service endpoint defined in the given URL
   * 
   * @param aURL -
   *          contains service endpoint info (hots and port)
   * 
   * @throws ResourceInitializationException wraps SocketException
   */
  public void connect(URL aURL) throws ResourceInitializationException {
    try {
      // creates a connection to a given endpoint
      socket = transport.connect(aURL, timeout);
    } catch (SocketException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Uses configured transport to delegate given CAS to the remote service.
   * 
   * @param aCAS CAS to be analyzed
   * @throws ResourceProcessException wraps Exception, SocketException
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    try {
      // delegate analysis of the CAS to remote object
      transport.process(socket, aCAS);
    } catch (SocketException e) {
      throw new ResourceProcessException(new ServiceConnectionException(e));
    } catch (Exception e) {
      throw new ResourceProcessException(e);
    }
  }

  /**
   * Uses configured transport to delegate given CASes to the remote service
   * 
   * @param aCASes - an array of CASes to be analyzed
   * 
   * @throws ResourceProcessException wraps SocketException, SocketTimeoutException
   */
  public void processCas(CAS[] aCASes) throws ResourceProcessException {
    try {
      for (int i = 0; i < aCASes.length; i++) {
        // delegate analysis of the CASes to remote object
        transport.process(socket, aCASes[i]);
      }
    } catch (SocketException e) {
      throw new ResourceProcessException(new ServiceConnectionException(e));
    } catch (SocketTimeoutException e) {
      throw new ResourceProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException {
    // noop

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isStateless()
   */
  public boolean isStateless() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public boolean isReadOnly() {
    return true;
  }

  /**
   * Returns {@link ProcessingResourceMetaData} object returned from the remote CasProcessor.
   * 
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    if (socket == null) {
      return metadata;
    }
    try {

      ProcessingResourceMetaData serviceMetaData = transport.getProcessingResourceMetaData(socket);
      if (serviceMetaData == null) {
        return metadata;
      }
      return serviceMetaData;
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger().log(Level.SEVERE, e.getMessage(), e);
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#batchProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    // noop

  }

  /**
   * Closes the connection to the remote service
   * 
   */
  public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stopping_cp__FINEST",
              new Object[] { Thread.currentThread().getName(), name });
    }
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
    socket = null;
    metadata = null;
  }

}
