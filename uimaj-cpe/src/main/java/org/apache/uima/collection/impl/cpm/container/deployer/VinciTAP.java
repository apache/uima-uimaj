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

package org.apache.uima.collection.impl.cpm.container.deployer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.VinciSaxParser;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.uima.cas_data.impl.CasDataImpl;
import org.apache.uima.cas_data.impl.FeatureStructureImpl;
import org.apache.uima.cas_data.impl.PrimitiveValueImpl;
import org.apache.uima.cas_data.impl.vinci.VinciCasDataConverter;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.collection.impl.cpm.vinci.Vinci;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.SaxDeserializer;
import org.apache.uima.util.UimaTimer;
import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.KeyValuePair;
import org.apache.vinci.transport.ServiceDownException;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.VNSException;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.document.AFrame;

/**
 * Vinci Proxy to remote Cas Processor vinci service. This component is used for both local(
 * managed) and remote ( unmanaged) Cas Processors. Its main purpose is to invoke remote APIs on Cas
 * Processors running as vinci services. It serializes data contained in the Cas into XCAS and sends
 * it to the service. It desiralizes data from XCAS returned from the service back into the Cas.
 */

public class VinciTAP {
  private String serviceHost;

  private String servicePort;

  private String fencedProcessPID = null;

  private String vnsHost;

  private String vnsPort;

  private String serviceName;

  private BaseClient conn = null;

  private int timeout = 300; // 300 second timeout

  private long totalCasToFrameTime = 0;

  private long totalAnnotationTime = 0;

  private long totalFrameToCasTime = 0;

  private long totalSerializeTime = 0;

  private long totalDeSerializeTime = 0;

  private long totalRoundTripTime = 0;

  private UimaTimer uimaTimer = null;

  // Default Content Tag
  private String contentTag = "Detag:DetagContent";

  private String[] keys2Drop = { "" };

  private VinciCasDataConverter vinciCasDataConverter = new VinciCasDataConverter(
          org.apache.uima.collection.impl.cpm.Constants.METADATA_KEY,
          org.apache.uima.collection.impl.cpm.Constants.DOC_ID,
          org.apache.uima.collection.impl.cpm.Constants.CONTENT_TAG,
          org.apache.uima.collection.impl.cpm.Constants.CONTENT_TAG_VALUE, contentTag, true);

  public VinciTAP() {
  }

  /**
   * Defines subject of analysis
   * 
   * @param aContentTag -
   *          subject of analysis
   */
  public void setContentTag(String aContentTag) {
    contentTag = aContentTag;
  }

  /**
   * Defines a custom timer to use for stats
   * 
   * @param aTimer -
   *          custom timer
   */
  public void setTimer(UimaTimer aTimer) {
    uimaTimer = aTimer;
  }

  /**
   * Defines types as array that will not be sent to the Cas Processor service
   * 
   * @param aKeys2Drop -
   *          array of types excluded from the request
   */
  public void setKeys2Drop(String[] aKeys2Drop) {
    keys2Drop = aKeys2Drop;
  }

  /**
   * Connects the proxy to Cas Processor running as a vinci service on a given host and port number.
   * 
   * @param aHost -
   *          name of the host where the service is running
   * @param aPort -
   *          port number where the service listens for requests
   * 
   * @throws ConnectException wraps Exception or unable to connect
   */
  public void connect(String aHost, int aPort) throws ConnectException {
    int attemptCount = 0;
    int maxConnectRetryCount = org.apache.uima.collection.impl.cpm.Constants.CONNECT_RETRY_COUNT;
    // Try to establish connection to remote service
    if (System.getProperty("CONNECT_RETRY_COUNT") != null) {
      try {
        maxConnectRetryCount = Integer.parseInt(System.getProperty("CONNECT_RETRY_COUNT"));
      } catch (Exception e) {
        throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_retry_count__WARNING",
                new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) }));
      }
    }
    while (attemptCount++ < maxConnectRetryCount) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_set_service_timeout__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(timeout) });
      }
      try {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_connect_to_service__FINEST",
                  new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) });
        }
        conn = new BaseClient(aHost, aPort);
        conn.setSocketTimeout(timeout);
        conn.setRetry(false);

        serviceHost = conn.getHost();
        servicePort = String.valueOf(conn.getPort());
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_connected_to_service__FINEST",
                  new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) });
        }
        if (conn.isOpen()) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_testing_connection__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException ex) {
          }

          // Test the connection.
          getAnalysisEngineMetaData();
          try {
            VinciFrame query = new VinciFrame();
            query.fadd("vinci:COMMAND", "GetPid");
            // Send shutdown request to the TAE service
            AFrame resp = (AFrame) conn.sendAndReceive(query);
            if (resp.fgetString("vinci:STATUS") != null
                    && resp.fgetString("vinci:STATUS").equals("OK")) {
              fencedProcessPID = resp.fgetString("PID");
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                        this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_service_pid__FINEST",
                        new Object[] { Thread.currentThread().getName(), fencedProcessPID });
              }
            }
          } catch (Exception ex) {
            // Ignore. Services may not implement this query
          }
        } else {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_connection_closed__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_connection_validated__FINEST",
                  new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) });
        }
        return;
      } catch (Exception e) {
        if (e instanceof ConnectException) {
          if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
            UIMAFramework.getLogger(this.getClass())
                    .logrb(
                            Level.WARNING,
                            this.getClass().getName(),
                            "initialize",
                            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_connection_not_established__WARNING",
                            new Object[] { Thread.currentThread().getName(), aHost,
                                String.valueOf(aPort) });
          }
          try {
            Thread.sleep(100);
          } catch (Exception ex) {
          }
        } else {
          e.printStackTrace();
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", e);
          }
        }
      }
    }
    if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_connection_failed__WARNING",
              new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) });
    }
    throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
            CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_unable_to_connect__WARNING",
            new Object[] { Thread.currentThread().getName(), aHost, String.valueOf(aPort) }));
  }

  /**
   * Define the max time in millis the proxy will wait for response from remote service
   * 
   * @param aTimeout -
   *          number of millis to wait
   */
  public void setTimeout(int aTimeout) {
    timeout = aTimeout;
  }

  /**
   * Connects to external service using service name as a way to locate it.
   * 
   * @param aServiceName -
   *          name of the service
   */
  public void connect(String aServiceName) throws ServiceConnectionException {
    // To locate the service by name the VNS is critical. Make sure we know where it is
    if (getVNSHost() == null || getVNSPort() == null) {

      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_vns_not_provided__SEVERE",
                new Object[] { Thread.currentThread().getName() });
      }
      throw new ServiceConnectionException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_vinci_vns_cfg_invalid__WARNING",
              new Object[] { Thread.currentThread().getName() }));
    }

    System.setProperty("VNS_HOST", getVNSHost());
    System.setProperty("VNS_PORT", getVNSPort());

    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_locating_service__FINEST",
                new Object[] { Thread.currentThread().getName(), aServiceName,
                    System.getProperty("VNS_HOST"), System.getProperty("VNS_PORT") });
      }
      // Override vinci default VNS settings
      VinciContext vctx = new VinciContext(InetAddress.getLocalHost().getCanonicalHostName(), 0);
      vctx.setVNSHost(getVNSHost());
      vctx.setVNSPort(Integer.parseInt(getVNSPort()));

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(
                Level.FINEST,
                Thread.currentThread().getName() + " Connecting to::" + aServiceName
                        + " VinciContext.getVNSHost():" + vctx.getVNSHost()
                        + " VinciContext.getVNSPort():" + vctx.getVNSPort()); // getVNSHost());
      }
      // establish connection to service
      conn = new VinciClient(aServiceName, vctx);
      conn.setSocketTimeout(timeout);
      conn.setRetry(false);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_set_service_timeout__FINEST",
                new Object[] { Thread.currentThread().getName(),
                    aServiceName + ":" + String.valueOf(timeout) });
      }
      serviceHost = conn.getHost();
      servicePort = String.valueOf(conn.getPort());
      serviceName = aServiceName;
      // Sucessfull connection. Return
      return;

    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_connection_failed__WARNING",
                new Object[] { Thread.currentThread().getName(), aServiceName, "" });

        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                Thread.currentThread().getName(), e);
      }
    }

    // If we are here there was a problem connecting to Vinci service
    throw new ServiceConnectionException(CpmLocalizedMessage.getLocalizedMessage(
            CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_unable_to_connect_toservice__WARNING",
            new Object[] { Thread.currentThread().getName(), aServiceName }));
  }

  private void testAndReconnect() throws ServiceException, ServiceConnectionException {
    // Make sure there is valid connection to the service and if there isnt one establish it
    if (conn == null || !conn.isOpen()) {
      try {
        if (serviceName != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_down__INFO",
                    new Object[] { Thread.currentThread().getName(), serviceName });
          }
          connect(serviceName);
        } else if (serviceHost != null && servicePort != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_service_down_onhost__INFO",
                  new Object[] { Thread.currentThread().getName(), serviceHost, servicePort });
          connect(serviceHost, Integer.parseInt(servicePort));
        }
      } catch (ConnectException ce) {
        if (serviceName != null) {
          throw new ServiceException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_EXP_unable_to_connect_toservice__WARNING", new Object[] {
                      Thread.currentThread().getName(), serviceName }));
        } else {
          throw new ServiceException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_unable_to_connect__WARNING",
                  new Object[] { Thread.currentThread().getName(), serviceHost, servicePort }));

        }

      } catch (ServiceConnectionException ce) {
        throw ce;
      }
    }

  }

  /**
   * Send a given Vinci Frame to the remote vinci service and return result
   * 
   * @param aFrame -
   *          Vinci Frame containing request
   * 
   * @return AFrame - Frame containing result
   */
  public AFrame sendAndReceive(AFrame aFrame) throws ServiceException, ServiceConnectionException {
    int currentTimeout = 0;
    currentTimeout = conn.getSocketTimeout();
    if (UIMAFramework.getLogger().isLoggable(Level.FINE)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINE,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_sending_process_req__FINEST",
              new Object[] { Thread.currentThread().getName(), serviceHost, servicePort,
                  String.valueOf(currentTimeout) });
    }
    try {
      AFrame responseFrame = null;

      // Dont test the connection, just try to send a message. If the send fails, go through the
      // error handler
      if (System.getProperty("TEST_BEFORE_SEND") != null) {
        testAndReconnect();
      }

      long memStart = Runtime.getRuntime().freeMemory();
      if (System.getProperty("SHOW_MEMORY") != null) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_memory_before_call__FINEST",
                new Object[] { Thread.currentThread().getName(),
                    String.valueOf(Runtime.getRuntime().totalMemory() / 1024),
                    String.valueOf(memStart / 1024) });
      }
      responseFrame = (AFrame) conn.sendAndReceive(aFrame, AFrame.getAFrameFactory(), timeout);
      if (System.getProperty("SHOW_MEMORY") != null) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_memory_after_call__FINEST",
                new Object[] { Thread.currentThread().getName(),
                    String.valueOf(Runtime.getRuntime().totalMemory() / 1024),
                    String.valueOf(Runtime.getRuntime().freeMemory() / 1024) });
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_received_response__FINEST",
                new Object[] { Thread.currentThread().getName(), serviceHost, servicePort });
      }
      return responseFrame;
    } catch (VNSException vnse) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_service_request__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()) });

      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, Thread.currentThread().getName(),
              vnse);
      conn.close();
      throw new ServiceException(vnse.getMessage());
    } catch (ServiceDownException sde) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_service_request__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()) });
      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, Thread.currentThread().getName(),
              sde);
      conn.close();
      throw new ServiceConnectionException(sde.getMessage());
    } catch (ServiceException sde) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_service_request__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()) });
      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, Thread.currentThread().getName(),
              sde);
      if (sde.getMessage().equals("Unknown command") && aFrame != null) {
        UIMAFramework.getLogger(this.getClass()).log(Level.INFO, aFrame.toXML());
      }
      throw new ServiceConnectionException(sde.getMessage());
    } catch (IOException e) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_service_request__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()) });
      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, Thread.currentThread().getName(),
              e);
      conn.close();
      if (System.getProperty("TEST_BEFORE_SEND") != null) {
        testAndReconnect();
      }
      if (e instanceof SocketTimeoutException) {
        UIMAFramework.getLogger(this.getClass()).log(Level.WARNING,
                Thread.currentThread().getName() + "  Exception Cause::" + e.getClass().getName());
        throw new ServiceConnectionException(e);
      }
      throw new ServiceConnectionException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_service_timeout__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()), String.valueOf(currentTimeout) }));
    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_service_request__WARNING",
              new Object[] { Thread.currentThread().getName(), conn.getHost(),
                  String.valueOf(conn.getPort()) });
      UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, Thread.currentThread().getName(),
              e);

      conn.close();
      throw new ServiceException(e.getMessage());
    }

  }

  /**
   * Appends keys (types) from XCAS to provided CasData instance doing conversions of ':' in WF keys
   * to '_colon_' and '-' to '_dash_' to enforce UIMA compliance.
   * 
   * @param dataCas -
   *          instance of CasData where the keys will be appended
   * @param aFrame -
   *          source of keys (data)
   * @return - modified CasData
   * 
   * @throws Exception passthru
   */
  public static CasData addKeysToDataCas(CasData dataCas, AFrame aFrame) throws Exception {
    try {
      aFrame = aFrame.fgetAFrame(Constants.KEYS);
      int frameCount = aFrame.getKeyValuePairCount();
      for (int i = 0; i < frameCount; i++) {
        KeyValuePair kvp = aFrame.getKeyValuePair(i);
        String featureStructureType = kvp.getKey();
        // Convert WF keys from ':' representation to '_colon_'
        if (featureStructureType
                .indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM) > -1) {
          featureStructureType = StringUtils.replaceAll(featureStructureType,
                  org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM,
                  org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM);
        }
        // Convert WF keys from '-' representation to '_dash_'
        if (featureStructureType
                .indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM) > -1) {
          featureStructureType = StringUtils.replaceAll(featureStructureType,
                  org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM,
                  org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM);
        }

        FeatureStructure vfs = new FeatureStructureImpl();
        vfs.setType(featureStructureType);

        FrameLeaf leafFrame = kvp.getValueAsLeaf();

        PrimitiveValue pv = new PrimitiveValueImpl(leafFrame.toString());
        vfs.setFeatureValue(featureStructureType, pv);
        dataCas.addFeatureStructure(vfs);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    return dataCas;
  }

  /**
   * Prints to stdout contents of a given CasData instance
   */
  private static void dumpFeatures(CasData aCAS) {

    try {
      Iterator it = aCAS.getFeatureStructures();
      while (it.hasNext()) {
        Object object = it.next();
        if (object instanceof FeatureStructure) {
          FeatureStructure fs = (FeatureStructure) object;
          String s = "\nCAS FEATURE STRUCTURE TYPE:" + fs.getType();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(VinciTAP.class).log(Level.FINEST, s);
          }
          String[] names = fs.getFeatureNames();
          for (int i = 0; names != null && i < names.length; i++) {
            FeatureValue fValue = fs.getFeatureValue(names[i]);
            if (fValue != null) {
              s = "\n\t\tCAS FEATURE NAME::" + names[i] + " CAS FEATURE VALUE::"
                      + fValue.toString();
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(VinciTAP.class).log(Level.FINEST, s);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Produces XCas from a given Cas. It selectively copies features from the Cas excluding those
   * types that are defined in a given dropKeyList.
   * 
   * @param aCasData -
   *          Cas for which XCAS is built
   * @param dataFrame -
   *          XCas frame for data
   * @param aDropKeyList -
   *          list of types to exclude from XCas
   * 
   * @throws Exception passthru
   */
  private void produceXCASRequestFrame(CasData aCasData, AFrame dataFrame, String[] aDropKeyList)
          throws Exception {

    AFrame keysFrame = new AFrame();
    dataFrame.fadd("KEYS", keysFrame);
    String ueid = DATACasUtils.getFeatureValueByType(aCasData,
            org.apache.uima.collection.impl.cpm.Constants.METADATA_KEY,
            org.apache.uima.collection.impl.cpm.Constants.DOC_ID);
    keysFrame.fadd("UEID", ueid);

    AFrame keyFrame = null;
    try {
      Iterator it = aCasData.getFeatureStructures();
      while (it.hasNext()) {
        FeatureStructure fs = (FeatureStructure) it.next();
        boolean skipTheFeature = false;
        if (aDropKeyList != null) {
          for (int i = 0; i < aDropKeyList.length; i++) {
            if (aDropKeyList[i].equalsIgnoreCase(fs.getType())) {
              skipTheFeature = true;
              break;
            }
          }
          if (skipTheFeature) {
            continue;
          }
        }
        keyFrame = new AFrame();

        FeatureValue value = null;
        String[] keys = fs.getFeatureNames();
        for (int i = 0; i < keys.length; i++) {
          value = fs.getFeatureValue(keys[i]);
          if (value instanceof PrimitiveValueImpl || value instanceof PrimitiveValue) {
            keyFrame.add("", new FrameLeaf(value.toString()));
          }
        }
        // Convert the type to make
        String type = fs.getType();
        if (type.indexOf(org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM) > -1) {
          type = StringUtils.replaceAll(type,
                  org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM,
                  org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM);
        }
        if (type.indexOf(org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM) > -1) {
          type = StringUtils.replaceAll(type,
                  org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM,
                  org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM);
        }
        keysFrame.fadd(type, keyFrame);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Returns true if a given feature represents any of the content (SoFa) types.
   * 
   * @param feature -
   *          type to check
   * 
   * @return - true if SoFa, false otherwise
   */
  private static boolean isText(String feature) {
    if ("Doc:SpannedText".equals(feature) || "Detag:Content".equals(feature)
            || "Detag:DetagContent".equals(feature) || "uima.cpm.DocumentText".equals(feature)) {
      return true;
    }
    return false;
  }

  /**
   * Performs Analysis of the CAS by the remote vinci service Cas Processor
   * 
   * @param aCas -
   *          Cas to analayze
   * @param aPT -
   *          performance trace object for stats and totals
   * @param aResourceName -
   *          name of the Cas Processor
   * @return - CAS containing results of analysis
   * 
   * @throws ServiceException - passthru, wraps Exception
   * @throws ServiceConnectionException passthru
   */
  public CasData analyze(CasData aCas, ProcessTrace aPT, String aResourceName)
          throws ServiceException, ServiceConnectionException {
    AFrame query = new AFrame();

    try {

      aPT.startEvent(aResourceName, "CAS to Vinci Request Frame", "");
      query.fadd(Constants.VINCI_COMMAND, Constants.ANNOTATE);

      AFrame dataFrame = new AFrame();

      vinciCasDataConverter.casDataToVinciFrame(aCas, dataFrame);
      query.fadd(Constants.DATA, dataFrame);
      aPT.endEvent(aResourceName, "CAS to Vinci Request Frame", "");
      // Time the amount of time the annalysis takes, including the comms.
      aPT.startEvent(aResourceName, "Vinci Call", "");
      // start = System.currentTimeMillis();
      AFrame responseFrame = sendAndReceive(query);
      aPT.endEvent(aResourceName, "Vinci Call", "");

      aPT.startEvent(aResourceName, "Vinci Response Frame to CAS", "");
      CasData newCasData = new CasDataImpl();
      FeatureStructure casDataFs = this.getDocTextFeatureStructure(aCas);
      if (casDataFs != null) {
        newCasData.addFeatureStructure(casDataFs);
      }
      vinciCasDataConverter.appendVinciFrameToCasData(responseFrame.fgetAFrame("DATA").fgetAFrame(
              "KEYS"), newCasData);
      aCas = newCasData;

      aPT.endEvent(aResourceName, "Vinci Response Frame to CAS", "");
      // Get the times reported by service and add to process trace
      int frameToCasTime = responseFrame.fgetVinciFrame("DATA")
              .fgetInt(Constants.FRAME_TO_CAS_TIME);
      if (frameToCasTime > 0) {
        totalFrameToCasTime += frameToCasTime;
        aPT.addEvent(aResourceName, Constants.FRAME_TO_CAS_TIME, "", frameToCasTime, "success");
      }
      int annotationTime = responseFrame.fgetVinciFrame("DATA").fgetInt(Constants.ANNOTATION_TIME);
      if (annotationTime > 0) {
        totalAnnotationTime += annotationTime;
        aPT.addEvent(aResourceName, ProcessTraceEvent.ANALYSIS, "", annotationTime, "success");
      }
      int casToFrameTime = responseFrame.fgetVinciFrame("DATA")
              .fgetInt(Constants.CAS_TO_FRAME_TIME);
      if (casToFrameTime > 0) {
        totalCasToFrameTime += casToFrameTime;
        aPT.addEvent(aResourceName, Constants.CAS_TO_FRAME_TIME, "", casToFrameTime, "success");
      }

      return aCas;

    } catch (ServiceException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      throw e;
    } catch (ServiceConnectionException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      throw e;
    } catch (Exception ex) {
      ex.printStackTrace();
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), ex.getMessage() });
        UIMAFramework.getLogger(this.getClass()).log(Level.FINER, "", ex);
      }
      throw new ServiceException(ex.getMessage());
    }
  }

  private void dropNamedTypes(AFrame aKeyFrame, String[] aDropKeyList) {
    // Now drop keys this annotator does not want to see
    if (aDropKeyList != null && aKeyFrame != null) {
      ArrayList keyList = aKeyFrame.fkeys();
      for (int inx = 0; inx < keyList.size(); inx++) {
        if (System.getProperty("SHOWKEYS") != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_drop_key__FINEST",
                    new Object[] { Thread.currentThread().getName(), (String) keyList.get(inx) });
          }
        }
        if (DATACasUtils.dropIt((String) keyList.get(inx), aDropKeyList)) {
          if (System.getProperty("SHOWKEYS") != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, " = YES");
            }
          }
          aKeyFrame.fdrop((String) keyList.get(inx));
        } else if (System.getProperty("SHOWKEYS") != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, " = NO");
          }
        }
      }
    }

  }

  /**
   * Main routine that sends requests to remote vinci services. Each Cas in the list is placed in
   * the request frame in its own DATA frame. For each Cas, this routine create a seperate DATA
   * frame. The DATA frame contains types and data that are required by the annotator. For
   * efficiency, the dropKeyList array can be defined with types that will be omitted from the DATA
   * frame. These keys are not required by the annotator thus it is waste of bandwidth to include
   * them in the request.
   * 
   * @param aCasList -
   *          a list of Cas to send to service for analysis
   * @param aPT -
   *          Process Trace object to aggrate time and stats
   * @param aResourceName -
   *          name of the Cas Processor
   * @return - List of Cas instances containing results of analysis
   * @throws ServiceException - passthru, wraps Exception
   * @throws ServiceConnectionException passthru   */
  public CasData[] analyze(CasData[] aCasList, ProcessTrace aPT, String aResourceName)
          throws ServiceException, ServiceConnectionException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_analyze_bundle__FINEST",
              new Object[] { Thread.currentThread().getName(), String.valueOf(aCasList.length) });
    }
    AFrame query = new AFrame();

    try {
      aPT.startEvent(aResourceName, "Vinci Call", "");
      query.fadd(Constants.VINCI_COMMAND, Constants.ANNOTATE);
      // Handle each Cas individually. For each Cas create a seperate DATA frame.
      for (int i = 0; i < aCasList.length && aCasList[i] != null; i++) {
        // String content = Vinci.getContentFromDATACas(aCas);
        // Create a request frame, and populate it with document text
        AFrame dataFrame = new AFrame();
        // Produces KEY Frames and ads them to a given dataFrame
        // produceXCASRequestFrame(aCasList[i], dataFrame);
        if (System.getProperty("SHOWKEYS") != null) {
          Iterator it = aCasList[i].getFeatureStructures();
          while (it.hasNext()) {
            FeatureStructure fs = (FeatureStructure) it.next();
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_dump_casdata__FINEST",
                      new Object[] { Thread.currentThread().getName(), fs.getType() });
            }
          }
        }
        if (DATACasUtils.isCasEmpty(aCasList[i])) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_no_cas__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          continue;
        }
        long sTime = uimaTimer.getTimeInMillis();
        // Different serializer object is used for WF. It seems to perform better
        if (System.getProperty("WF_SERIALIZER") != null) {
          produceXCASRequestFrame(aCasList[i], dataFrame, keys2Drop);
        } else {
          vinciCasDataConverter.casDataToVinciFrame(aCasList[i], dataFrame);
          dropNamedTypes(dataFrame.fgetAFrame("KEYS"), keys2Drop);
        }
        totalSerializeTime += (uimaTimer.getTimeInMillis() - sTime);

        query.fadd(Constants.DATA, dataFrame);
      }

      if (serviceName != null && System.getProperty("SHOW_NAME") != null)
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_send_casdata_to_service__FINEST",
                  new Object[] { Thread.currentThread().getName(), serviceName });
        }
      if (System.getProperty("SHOW_REQFRAME") != null) {
        UIMAFramework.getLogger(this.getClass()).log(Level.INFO, " queryFrame-" + query.toXML());
      }

      long t = uimaTimer.getTimeInMillis();
      AFrame responseFrame = sendAndReceive(query);
      // no longer need the query object
      query = null;
      totalRoundTripTime += (uimaTimer.getTimeInMillis() - t);
      if ((responseFrame != null) && (responseFrame.fgetString("Error") != null)) {
        throw new ServiceException(responseFrame.fgetString("Error"));
      }

      if (System.getProperty("SHOW_RAW_RESPFRAME") != null) {
        UIMAFramework.getLogger(this.getClass()).log(Level.INFO,
                " responseFrame from service::" + serviceName + "\n" + responseFrame.toXML());
      }

      if (responseFrame != null && responseFrame.fgetAFrame("DATA") == null) {
        // No annotations found in reply so just leave
        return aCasList;
      }
      ArrayList d = new ArrayList();
      if ( responseFrame != null ) {
        d = responseFrame.fget("DATA");
      }
      int instanceCount = 0;
      // Process response, DATA frame at a time. Each DATA frame corresponds to an instance of
      // CasData
      AFrame dataFrame = null;
      while (!(d.isEmpty())) {
        dataFrame = (AFrame) d.remove(0);
        try {
          if (System.getProperty("SHOW_RESPFRAME") != null) {
            UIMAFramework.getLogger(this.getClass()).log(Level.INFO,
                    " Converting XCAS in responseFrame to CasData.XCAS=" + dataFrame.toXML());
          }
          long eTime = uimaTimer.getTimeInMillis();
          // When configured use WF serializer which is faster than the alternative SAX based one
          if (System.getProperty("WF_SERIALIZER") != null) {
            addKeysToDataCas(aCasList[instanceCount], dataFrame);
          } else {
            // We will call vinciCasDataConverter to convert response frame to a new
            // CasData. BUT, we also need to preserve the document text from the request,
            // since it may not be echoed by the service.
            CasData newCasData = new CasDataImpl();
            FeatureStructure casDataFs = this.getDocTextFeatureStructure(aCasList[instanceCount]);
            if (casDataFs != null) {
              newCasData.addFeatureStructure(casDataFs);
            }
            vinciCasDataConverter.appendVinciFrameToCasData(dataFrame.fgetAFrame("KEYS"),
                    newCasData);
            aCasList[instanceCount] = newCasData;
          }
          totalDeSerializeTime += (uimaTimer.getTimeInMillis() - eTime);

          if (System.getProperty("SHOWFRAME") != null) {
            UIMAFramework.getLogger(this.getClass()).log(Level.INFO, " dumping CasData-\n");
            dumpFeatures(aCasList[instanceCount]);
          }

          if (dataFrame != null) {
            FeatureStructure vfs = new FeatureStructureImpl();
            vfs.setType(org.apache.uima.collection.impl.cpm.Constants.STAT_FEATURE);

            String frame2CasTime = dataFrame.fgetString(Constants.FRAME_TO_CAS_TIME);
            if (frame2CasTime != null) {
              PrimitiveValue pv = new PrimitiveValueImpl(frame2CasTime);
              vfs.setFeatureValue(Constants.FRAME_TO_CAS_TIME, pv);
            }
            String annotationTime = dataFrame.fgetString(Constants.ANNOTATION_TIME);
            if (annotationTime != null) {
              PrimitiveValue pv = new PrimitiveValueImpl(annotationTime);
              vfs.setFeatureValue(Constants.ANNOTATION_TIME, pv);
            }
            String cas2FrameTime = dataFrame.fgetString(Constants.CAS_TO_FRAME_TIME);
            if (cas2FrameTime != null) {
              PrimitiveValue pv = new PrimitiveValueImpl(cas2FrameTime);
              vfs.setFeatureValue(Constants.CAS_TO_FRAME_TIME, pv);
            }
            aCasList[instanceCount].addFeatureStructure(vfs);
          }
          instanceCount++;
        } catch (Exception e) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINER,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_exception__FINER",
                    new Object[] { Thread.currentThread().getName(), e.getMessage(),
                        dataFrame.toXML() });
            e.printStackTrace();
          }
          dataFrame.toXML();
        }
      }
      aPT.endEvent(aResourceName, "Vinci Call", "");
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_done_analyzing_bundle__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(aCasList.length) });
      }
      return aCasList;

    } catch (ServiceException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      e.printStackTrace();
      throw e;
    } catch (ServiceConnectionException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_service_connection_exception__SEVERE",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      e.printStackTrace();
      throw e;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ServiceException(ex.getMessage());
    }
  }

  /**
   * Returns Cas Processor metadata as it is returned from the remote Cas Processor running as vinci
   * service.
   * 
   */
  public ProcessingResourceMetaData getAnalysisEngineMetaData() throws ResourceServiceException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_request_metadata__FINEST",
              new Object[] { Thread.currentThread().getName(), serviceName, conn.getHost(),
                  String.valueOf(conn.getPort()) });
    }
    // Added to support WF Miners that have descriptors.
    AFrame resultFrame = null;
    try {
      // create Vinci Frame
      VinciFrame queryFrame = new VinciFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", "GetMeta");
      // Send the request to the service and wait for response
      resultFrame = (AFrame) conn.sendAndReceive(queryFrame, AFrame.getAFrameFactory(), timeout);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_return_meta__FINEST",
                new Object[] { Thread.currentThread().getName(), serviceName, conn.getHost(),
                    String.valueOf(conn.getPort()) });
      }
      // Parse the XML into the ProcessingResourceMetaData object
      SaxDeserializer saxDeser = UIMAFramework.getXMLParser().newSaxDeserializer();

      VinciSaxParser vinciSaxParser = new VinciSaxParser();
      vinciSaxParser.setContentHandler(saxDeser);
      vinciSaxParser.parse(resultFrame);

      ProcessingResourceMetaData metadata = (ProcessingResourceMetaData) saxDeser.getObject();

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_done_parsing_meta__FINEST",
                new Object[] { Thread.currentThread().getName(), serviceName, conn.getHost(),
                    String.valueOf(conn.getPort()) });
      }
      return metadata;
    } catch (ServiceException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      if ("No Such Command supported.".equals(e.getMessage().trim())) {
        return null;
      }
      e.printStackTrace();
      throw new ResourceServiceException(e);
    } catch (Exception e) {
      if ("No Such Command supported".equals(e.getMessage())) {
        if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
          UIMAFramework.getLogger(this.getClass())
                  .logrb(
                          Level.WARNING,
                          this.getClass().getName(),
                          "process",
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_service_rejected_requested__WARNING",
                          new Object[] { Thread.currentThread().getName(), serviceName,
                              resultFrame.toXML() });
        }
        return null;
      }
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
      e.printStackTrace();
      throw new ResourceServiceException(e);
    }
  }

  /**
   * Let the remote service now that end of batch marker has been reached, the notification is
   * one-way meaning the CPE does not expect anything back from the service.
   */
  public void batchProcessComplete() throws ResourceServiceException {
    // For some installations, like WF, dont bother sending end-of-batch marker.
    // WF miners dont want to see this.
    if (System.getProperty("FILTER_BATCH") != null) {
      return;
    }
    try {
      if (conn != null && conn.isOpen()) {
        VinciFrame query = new VinciFrame();
        query.fadd("vinci:COMMAND", Constants.BATCH_PROCESS_COMPLETE);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_send_batch_complete__FINEST",
                  new Object[] { Thread.currentThread().getName(), conn.getHost(),
                      String.valueOf(conn.getPort()), query.toXML() });
        }
        // Send notification to service
        conn.send(query);
      }
    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });
      e.printStackTrace();
      throw new ResourceServiceException(e);
    }

  }

  /**
   * Notify the remote service that the CPE reached end of processing. Wait for response from the
   * service before returning. This ensures that the request is accepted and the desired logic
   * handling end of processing has completed.
   * 
   * @throws ResourceServiceException wraps Exception
   */
  public void collectionProcessComplete() throws ResourceServiceException {
    try {
      if (conn != null && conn.isOpen()) {
        VinciFrame query = new VinciFrame();
        query.fadd("vinci:COMMAND", Constants.COLLECTION_PROCESS_COMPLETE);
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_send_collection_complete__FINEST",
                  new Object[] { Thread.currentThread().getName(), conn.getHost(),
                      String.valueOf(conn.getPort()), query.toXML() });

          UIMAFramework.getLogger(this.getClass()).log(Level.INFO,
                  " Sending COLLECTION PROCESS COMPLETE TO Service\n" + query.toXML());
        }
        // Send notification to service
        conn.sendAndReceive(query);
      }
    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });

      e.printStackTrace();
      throw new ResourceServiceException(e);
    }
  }

  /**
   * Conditionally sends the shutdown request to managed (local) vinci service. This routine should
   * not terminate services deployed as remote (unmanaged). This routine does not verify that the
   * service shut itself down. It does not even wait for response. It is up to the service to clean
   * itself up and terminate.
   * 
   * @param shutdownService -
   *          flag indicating if a shutdown command should be sent to the service
   * @param aDoSendNotification -
   *          indicates whether or not to sent CollectionProcessComplete frame to service
   * @return - true if shutdown message has been sent without error, false otherwise
   */
  public boolean shutdown(boolean shutdownService, boolean aDoSendNotification) {
    if (System.getProperty("SHOW_STATS") != null) {
      UIMAFramework.getLogger(this.getClass())
              .logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_time_spent_serializing_xcas__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          String.valueOf(totalSerializeTime) });

      UIMAFramework.getLogger(this.getClass())
              .logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_time_spent_deserializing_xcas__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          String.valueOf(totalDeSerializeTime) });

      UIMAFramework.getLogger(this.getClass())
              .logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_time_spent_in_transit__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          String.valueOf(totalRoundTripTime) });
    }
    try {

      if (isConnected()) {
        // Notify remote service that the processing is done
        if (aDoSendNotification) {
          collectionProcessComplete();
        }
        // If necessary send shutdown command
        if (shutdownService) {
          try {
            VinciFrame query = new VinciFrame();
            query.fadd("vinci:COMMAND", Constants.SHUTDOWN);
            if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.INFO,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_stopping_service__INFO",
                      new Object[] { Thread.currentThread().getName(), conn.getHost(),
                          String.valueOf(conn.getPort()), query.toXML() });
            }
            // Send shutdown request to the TAE service
            conn.send(query);
          } finally {
            synchronized (conn) {
              conn.wait(500);
            }
            if (fencedProcessPID != null) {
              new FencedProcessReaper().killProcess(fencedProcessPID);
            }
          }
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();

      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_exception__SEVERE",
              new Object[] { Thread.currentThread().getName(), ex.getMessage() });
      return false;
    } finally {
      if (conn != null) {
        // Now, this logic is necessary to give vinci service time to cleanly terminate before
        // we close connection to it. If not done, there is some race condition leading to
        // the service lingering and cpe hanging due to the fact that it has StreamGobbler
        // threads still reading from service stdout and stderr.
        if (shutdownService) {
          waitForServiceShutdown();
        }
        conn.close();
      }
    }
    return true;
  }

  /**
   * Waits for local/managed service to shutdown. If we dont allow the service time to cleanly
   * terminate, sometimes it just lingers. Since the shutdown command sent in shuthdown() method is
   * one-way call, it was immediately followed by the connection close. That caused an exception on
   * the service side, preventing it from clean exit. So here we just wait until the connection no
   * longer exists and then close it on this side.
   * 
   */
  private void waitForServiceShutdown() {
    int retry = 10; // Hard-coded limit.
    // Try until the endpoint is closed by the service OR hard limit of tries
    // has beed exceeded.
    do {
      try {
        // establish ownership of query object, otherwise IllegalMonitorStateException is
        // thrown. Than give the service time to cleanly exit.
        
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_waiting_for_service_shutdown__FINEST",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(10 - retry),
                        "10" });
          }
          Thread.sleep(100); // wait for 100ms to give the service time to exit cleanly
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                    " Resuming CPE shutdown.Service should be down now.");
          }
        
      } catch (InterruptedException e) {
      }
      if (retry-- <= 0) {
        break;
      }
    } while (conn.isOpen());

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_service_shutdown_completed__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
  }

  /**
   * Returns status of the vinci connection
   * 
   * @return - true if connection is valid, false otherwise
   */
  public boolean isConnected() {
    if (conn != null && conn.isOpen()) {
      return true;
    }
    return false;
  }

  /**
   * Sets the VNS port this proxy will use to locate service
   * 
   * @param aVNSPort -
   *          vns port to use
   */
  public void setVNSPort(String aVNSPort) {
    System.setProperty(Vinci.VNS_PORT, aVNSPort);
    vnsPort = aVNSPort;
  }

  /**
   * Sets the VNS host this proxy will use to locate service
   * 
   * @param aVNSHost -
   *          name of the VNS host
   */
  public void setVNSHost(String aVNSHost) {
    vnsHost = aVNSHost;
    System.setProperty(Vinci.VNS_HOST, aVNSHost);
  }

  /**
   * Returns port of the service this proxy is connected to
   * 
   * @return - service port
   */
  public int getServicePort() {
    return Integer.valueOf(servicePort).intValue();
  }

  /**
   * Returns host where the service is running
   * 
   * @return host name of the machine where the service is running
   */
  public String getServiceHost() {
    return serviceHost;
  }

  /**
   * Returns VNS Port
   * 
   * @return VNS port
   */
  public String getVNSPort() {
    return vnsPort;
  }

  /**
   * Returns VNS Host
   * 
   * @return VNS Host
   */
  public String getVNSHost() {
    return vnsHost;
  }

  /**
   * Gets the CasData FeatureStructure representing the document text, if any. Currently, this must
   * be one of the first two FSs in the CasData.
   * 
   * @param aCasData
   *          CasData containing feature structures
   * @return FeatureStructure representing document text, null if none
   */
  private FeatureStructure getDocTextFeatureStructure(CasData aCasData) {
    Iterator fsIterator = aCasData.getFeatureStructures();
    if (fsIterator.hasNext()) {
      // look in first FS
      FeatureStructure casDataFs = (FeatureStructure) fsIterator.next();
      if (isText(casDataFs.getType())) {
        return casDataFs;
      } else if (fsIterator.hasNext()) {
        // look in second FS
        casDataFs = (FeatureStructure) fsIterator.next();
        if (isText(casDataFs.getType())) {
          return casDataFs;
        }
      }
    }
    return null;
  }

}
