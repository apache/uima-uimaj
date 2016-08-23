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

package org.apache.uima.collection.impl.cpm.container.deployer.vinci;

import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.naming.TimeLimitExceededException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.AbortCPMException;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.base_cpm.container.KillPipelineException;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeploymentException;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.CasProcessorConfigurationJAXBImpl;
import org.apache.uima.collection.impl.cpm.container.NetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.container.ProcessingContainer_Impl;
import org.apache.uima.collection.impl.cpm.container.ServiceProxyPool;
import org.apache.uima.collection.impl.cpm.container.deployer.VinciTAP;
import org.apache.uima.collection.impl.cpm.container.deployer.vns.LocalVNS;
import org.apache.uima.collection.impl.cpm.container.deployer.vns.VNSQuery;
import org.apache.uima.collection.impl.cpm.container.deployer.vns.VinciServiceInfo;
import org.apache.uima.collection.impl.cpm.engine.BoundedWorkQueue;
import org.apache.uima.collection.impl.cpm.engine.CPMEngine;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.utils.Execute;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.apache.vinci.transport.ServiceDownException;
import org.apache.vinci.transport.ServiceException;

/**
 * Reference implementation of
 * {@link org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer} This
 * component enables the CPE to deploy Cas Processors running as a Vinci service. Two deployment
 * models are supported in the current implementation:
 * <ul>
 * <li>managed deployment (aka local)</li>
 * <li>unmanaged deployment (aka remote)</li>
 * </ul>
 * Managed deployment gives the CPE control over the life cycle of the Cas Processor. The CPE
 * starts, restarts and shuts down the Cas Processor running as a Vinci service. This service is
 * launched on the same machine as the CPE but in a seperate process.
 * 
 * Unmanaged deployment does not provide the CPE control over the Cas Processor that may be running
 * on a remote machine. The CPE assumes that such Cas Processor is managed by a separate application
 * and is always available.
 * 
 * For the managed deployment the CPE uses its internal VNS (Vinci Naming Service) to which Cas
 * Processor must connect to. The VNS issues a port for the Cas Processor to run on and creates a
 * proxy to it.
 * 
 * For unmanaged Cas Processor the CPE creates a proxy to remote service. The remote Cas Processor
 * service is discovered with help of VNS running on a host and port defined in the Cpe descriptor
 * for this Cas Processor.
 * 
 */
public class VinciCasProcessorDeployer implements CasProcessorDeployer {
  public static final String LOCAL_VNS = "LOCAL_VNS";

  public static final int WAIT_TIME = 150;

  public static final int MAX_WAIT_TRIES = 5000;

  public static final String CONN_RETRY_COUNT = "CONN_RETRY_COUNT";

  public static final String DEFAULT_VNS_PORT = "9005";

  // Define the range for ports. In terms of the first port
  // and the range. So the two constants below say that the
  // first port is 10000 and the range is from 10000 to 13000.
  public static final int DEFAULT_SERVICE_PORT = 10000;

  public static final int DEFAULT_SERVICE_PORT_RANGE = 3000;

  public static final int SLEEP_TIME = 1000;

  private ServiceProxyPool casProcessorPool = null;

  // Thread for the local VNS
  private Thread localVNSThread = null;

  // Local VNS is a shared instance across all instances of VinciCasProcessorDeployer. It uses
  // shared
  // portQueue (defined below). VNS and the queue are instantiated once.
  private static volatile LocalVNS vns = null;

  private int restartCount = 0;

  // Pool for port numbers assigned to Cas Processors running as vinci services deployed as managed
  // Cas Processors
  private static BoundedWorkQueue portQueue = null;

  private CPEFactory cpeFactory = null;

  private final Object monitor = new Object();  // must be unique object, used for synch

  private ArrayList currentServiceList = null;

  /**
   * Instantiaes the class and gives it access to CPE configuration.
   * 
   * @param aCpeFactory
   */
  public VinciCasProcessorDeployer(CPEFactory aCpeFactory) {
    cpeFactory = aCpeFactory;
  }

  /**
   * Deploys integrated Cas Processor. Number of instances this routine actually deploys depends on
   * number of processing threads defined in the CPE descriptor. There is one instance per
   * processing thread created here. The <i>aCasProcessorList</i> contains instantiated Cas
   * Processors. These are instantiated by the CPEFactory.
   * 
   * @param aCasProcessorList - list containing instantiated Cas Processors
   * @param redeploy - true when redeploying failed Cas Processor
   * 
   * @return - ProcessingContainer containing pool of CasProcessors
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, CPMEngine aEngine,
          boolean redeploy) throws ResourceConfigurationException {
    return deployCasProcessor(aCasProcessorList, redeploy);
  }

  /**
   * Deploys CasProcessor using configuration from provided container. This method is used for
   * re-launching failed Cas Processor.
   * 
   * @param aProcessingContainer -
   *          container for deployed CasProcessor.
   */
  public void deployCasProcessor(ProcessingContainer aProcessingContainer)
          throws ResourceConfigurationException {
    try {
      CasProcessorConfiguration casProcessorConfig = aProcessingContainer
              .getCasProcessorConfiguration();
      String name = casProcessorConfig.getName();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_reeploying_service__FINEST",
                new Object[] { Thread.currentThread().getName(), name });
      }
      // re-deploy failed Cas Processor according to mode defined in the cpe descriptor. true -
      // stands for re-launch or restart
      deployBasedOnModel(aProcessingContainer, casProcessorConfig, true);
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_unable_to_deploy_service__SEVERE", new Object[] {
                  Thread.currentThread().getName(),
                  aProcessingContainer.getCasProcessorConfiguration().getName() });
    }
  }

  /**
   * Deploys CasProcessors in a provided List. The List contains instances of Cas Processors that
   * are not yet bound to a vinci service. To do anything usefull, the Cas Processor must be
   * deployed first. The process of deploying a proxy depends on the deployment mode defined in the
   * cpe descriptor. In case of managed Cas Processor, the deployment consists of launching the
   * vinci service and creating a connection to it. For un-managed Cas Processor the CPE establishes
   * the connection.
   * 
   * @param aCasProcessorList -
   *          list of CasProcessors to deploy
   * @param redeploy -
   *          true if intent is to redeploy failed service
   * @return ProcessinContainer - instance of Container
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, boolean redeploy)
          throws ResourceConfigurationException {
    String name = null;
    CasProcessor casProcessor = null;
    CasProcessorConfiguration casProcessorConfig = null;
    ProcessingContainer processingContainer = null;
    try {
      // Deploy one Cas Processor at a time in sequential order
      for (int i = 0; i < aCasProcessorList.size(); i++) {
        casProcessor = (CasProcessor) aCasProcessorList.get(i);
        // Container may have already been instantiated. This will be the case if the CPM is
        // configured for concurrent
        // processing ( more than one processing pipeline). There is only one container per
        // CasProcessor type.
        // So each instance of the same CasProcessor will be associated with a single container.
        // Inside the
        // container instances are pooled. When deploying the very first CasProcessor of each type,
        // the
        // container will be created and initialized. Any subsequent deployments of this
        // CasProcessor will
        // simply use it, and will be added to this container's instance pool.
        if (processingContainer == null) {
          ProcessingResourceMetaData metaData = casProcessor.getProcessingResourceMetaData();
          CpeCasProcessor casProcessorType = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
                  .get(metaData.getName());
          // Create a pool to hold instances of CasProcessors. Instances are managed by a container
          // through
          // getCasProcessor() and releaseProcessor() methods.
          casProcessorPool = new ServiceProxyPool();
          // Create CasProcess Configuration holding info defined in the CPE descriptor
          casProcessorConfig = new CasProcessorConfigurationJAXBImpl(casProcessorType, cpeFactory.getResourceManager());

          // Associate CasProcessor configuration from CPE descriptor with this container
          processingContainer = new ProcessingContainer_Impl(casProcessorConfig, metaData,
                  casProcessorPool);
          processingContainer.setCasProcessorDeployer(this);
          // Instantiate an object that encapsulates CasProcessor configuration
          // Determine deployment model for this CasProcessor
          // Each CasProcessor must have a name
          name = casProcessorConfig.getName();
          if (name == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_unable_to_read_meta__SEVERE", Thread.currentThread().getName());
            }
            throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_casprocessor_no_name_found__SEVERE", new Object[] { Thread
                            .currentThread().getName() });
          }
        }
        // Add CasProcess to the instance pool
        casProcessorPool.addCasProcessor(casProcessor);
      }

      // There is one instance of ProcessingContainer for set of CasProcessors
      if (processingContainer == null) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_container__SEVERE", new Object[] { Thread.currentThread()
                        .getName() });
      }
      // Assumption is that the container already exists and it contains CasProcessor configuration
      casProcessorConfig = processingContainer.getCasProcessorConfiguration();
      if (casProcessorConfig == null) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_container_configuration_not_found__SEVERE", new Object[] { Thread
                        .currentThread().getName() });
      }
      deployBasedOnModel(processingContainer, casProcessorConfig, false);

    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
    return processingContainer;
  }

  /**
   * Deploys CasProcessor according to configured deployment mode.
   * 
   * @param aProcessingContainer -
   *          container managing instances of CasProcessor
   * @param aCasProcessorConfig -
   *          CasProcessor configuration
   * @param redeploy -
   *          flag indicating if this re-deployment of CasProcessor that failed
   * 
   * @throws ResourceConfigurationException if unknown deployment type
   */
  private void deployBasedOnModel(ProcessingContainer aProcessingContainer,
          CasProcessorConfiguration aCasProcessorConfig, boolean redeploy)
          throws ResourceConfigurationException {
    String deployModel = aCasProcessorConfig.getDeploymentType();
    String name = aCasProcessorConfig.getName();

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_deploy_with_mode__FINEST",
              new Object[] { Thread.currentThread().getName(), name, deployModel });
    }
    // Deploy CasProcessor as configured in the CPE descriptor
    if (Constants.DEPLOYMENT_LOCAL.equals(deployModel)) {
      // Deploy CasProcessor locally, meaning same machine as the CPM but different JVM
      deployLocal(aProcessingContainer, redeploy);
    } else if (Constants.DEPLOYMENT_REMOTE.equals(deployModel)) {
      // Deploy CasProcessor remotely, meaning instantiate and connect a proxy to existing/
      // running service
      deployRemote(aProcessingContainer, redeploy);
    } else if (Constants.DEPLOYMENT_INTEGRATED.equals(deployModel)) {
      // Deploy CasProcessor as a collocated instance with CPM
      deployIntegrated(aProcessingContainer, redeploy);
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_unsupported_deploy_mode__SEVERE",
                new Object[] { Thread.currentThread().getName(), name, deployModel });
      }
      throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_unsupported_deploy_mode__SEVERE", new Object[] {
                  Thread.currentThread().getName(), name, deployModel });
    }

  }

  /**
   * Deploys CasProcessor as a Local Vinci Service. The CPE will launch the Cas Processor in a
   * separate process but on the same machine. The CPE will manage the life-cycle of the Cas
   * Processor including shutdown. Shutdown command will be sent when the CPE completes processing.
   * In this deployment mode the CPE uses its own VNS. The VNS information (host and port) will be
   * added as command line arguments to the program being launched (Cas Processor application). It
   * is the responsibility of the Cas Processor to to read this information and use it to locate VNS
   * the CPE is managing.
   * 
   * @param aProcessingContainer -
   *          container object that will hold proxies to Cas Processor when it is launched
   * @param redeploy -
   *          true if intent is to redeploy failed service
   * @throws ResourceConfigurationException if the descriptor is invalid or for any internal Exception (wrapped)
   */
  private void deployLocal(ProcessingContainer aProcessingContainer, boolean redeploy)
          throws ResourceConfigurationException {
    // Cas Processor configuration is associated with the container. There is a container per Cas
    // Processor type.
    // All Cas Processors that are replicated are manages by a single container. The configuration
    // is shared accross
    // all instances of Cas Processors. To the CPE each instance is exactly the same.
    CasProcessorConfiguration casProcessorConfig = aProcessingContainer
            .getCasProcessorConfiguration();
    String name = casProcessorConfig.getName();
    // If CasProcessor defined to run in a seperate process, extract all runtime info as
    // defined in the cpe descriptor for this casProcessor
    try {
      if (casProcessorConfig.runInSeparateProcess()) {
        // redeploy = true IF the service failed and intent is to re-launch it
        if (redeploy == false) {
          // Get number of instances of the Cas Processors to launch. This is defined in the CPE
          // descriptor by
          // attribute processingThreadCount in the <casProcessors> element.
          int concurrentThreadCount = cpeFactory.getProcessingUnitThreadCount();
          // 08/16/04 commented out to facilitate sharing of single queue across multiple instances
          // of this class.
          // LocalVNS is also shared.
          // For local Vinci services we must have a local vns service. This component determines
          // what ports
          // services will run on

          if (vns == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_deploy_vns__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            // 08/16/04 added to facilitate sharing of single queue across multiple instances of
            // this class.
            portQueue = new BoundedWorkQueue(concurrentThreadCount, "Port Queue", null);
            // Starts the VNS in its own thread.
            deployVNS(casProcessorConfig, redeploy);
          }

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_deploy_with_mode__FINEST",
                    new Object[] { Thread.currentThread().getName(), casProcessorConfig.getName(),
                        casProcessorConfig.getDeploymentType() });
          }
          // Do the actual deployment of the application. Actually, the deployer will deploy as
          // many application instances as there are processing pipeline threads.
          launchLocalService(aProcessingContainer, casProcessorConfig, redeploy,
                  concurrentThreadCount);
        } else {
          // Re-deploy one instance of the service
          launchLocalService(aProcessingContainer, casProcessorConfig, redeploy, 1);
        }
        associateMetadataWithContainer(aProcessingContainer);
      } else {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_cpe_descriptor__SEVERE", new Object[] {
                    Thread.currentThread().getName(), name, "runInSeparateProcess" });
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
  }

  /**
   * Retrieve the metadata from the service and add it to the container
   * 
   * @param aProcessingContainer -
   *          container where the metadata will be saved
   */
  private void associateMetadataWithContainer(ProcessingContainer aProcessingContainer) {
    CasProcessor processor = null;
    try {
      if (casProcessorPool != null) {
        processor = casProcessorPool.checkOut();
        if (processor != null) {
          ProcessingResourceMetaData metadata = processor.getProcessingResourceMetaData();
          if (aProcessingContainer != null && metadata != null) {
            aProcessingContainer.setMetadata(metadata);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (processor != null) {
        casProcessorPool.checkIn(processor);
      }
    }
  }

  /**
   * Launches an application as a seperate process using java's Process object. Actually, it
   * launches as many copies of the application as given in the <i>howMany</i> parameter.
   * 
   * @param casProcessorConfig -
   *          Configuration containing start up command
   * @param redeploy -
   *          true if intent is to redeploy failed application
   * @param howMany -
   *          how many seperate process to spawn
   * @throws CasProcessorDeploymentException wraps any exception
   */
  private void launchLocalService(ProcessingContainer aProcessingContainer,
          CasProcessorConfiguration casProcessorConfig, boolean redeploy, int howMany)
          throws CasProcessorDeploymentException {
    try {
      Execute exec = casProcessorConfig.getExecSpec();
      String cmd[] = exec.getCmdLine();

      String[] execCommand = new String[3];
      if (System.getProperty("os.name").equalsIgnoreCase("linux")) {
        execCommand[0] = "/bin/sh";
        execCommand[1] = "-c";
      }
      StringBuffer sb = new StringBuffer();
      // The command line has been established, now make sure that we tell the Cas Processor about
      // the
      // VNS port in use. In this logic the VNS is querried for its port number and this will the
      // port
      // provided to the Cas Processor. The Cas Processor must read this argument during its start
      // up
      // to ensure connection to the correct VNS.
      for (int i = 0; i < cmd.length; i++) {
        if (cmd[i] != null && cmd[i].indexOf("-DVNS_PORT") > -1) {
          int vnsPort = vns.getVNSPort();
          cmd[i] = "-DVNS_PORT=" + vnsPort;
        }
        sb.append(" " + cmd[i]);
      }
      String logDir = "";
      if (System.getProperty("LOG_DIR") != null) {
        logDir = System.getProperty("LOG_DIR");
        if (!logDir.endsWith("/") && !logDir.endsWith("\\")) {
          // Append OS specific seperator
          logDir += System.getProperty("file.separator");
        }
      }
      sb.append(" >> " + logDir + "\"" + casProcessorConfig.getName() + System.currentTimeMillis()
              + ".log\"" + " 2>&1");
      execCommand[2] = sb.toString();

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_vns_started__FINEST",
                new Object[] { Thread.currentThread().getName(), casProcessorConfig.getName() });

      }
      int serviceCount = howMany;
      // Spawn off as many processes as defined in CPE descriptor. The exact number of instances is
      // equal to
      // procesingUnitThreadCount attribute.
      while (serviceCount-- > 0) {

        for (int i = 0; cmd != null && i < cmd.length; i++) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_launching_with_service_cmd__FINEST",
                    new Object[] { Thread.currentThread().getName(), casProcessorConfig.getName(),
                        String.valueOf(i), cmd[i] });
          }
        }

        String[] env;
        if ((env = exec.getEnvironment()) != null && env.length > 0) {
          for (int i = 0; i < env.length; i++) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "initialize",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_launching_with_service_env__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          casProcessorConfig.getName(), String.valueOf(i), env[i] });
            }
          }

          if (System.getProperty("os.name").equalsIgnoreCase("linux")) {
            for (int i = 0; execCommand != null && i < execCommand.length; i++) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "initialize",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_launching_with_service_exec__FINEST",
                        new Object[] { Thread.currentThread().getName(),
                            casProcessorConfig.getName(), String.valueOf(i), execCommand[i] });
              }
            }

            Runtime.getRuntime().exec(execCommand, env);
          } else {
            Runtime.getRuntime().exec(cmd, env);
          }
        } else {
          if (System.getProperty("os.name").equalsIgnoreCase("linux")) {
            for (int i = 0; execCommand != null && i < execCommand.length; i++) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "initialize",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_launching_with_service_exec__FINEST",
                        new Object[] { Thread.currentThread().getName(),
                            casProcessorConfig.getName(), String.valueOf(i), execCommand[i] });
              }
            }
            Runtime.getRuntime().exec(execCommand);
          } else {
            Runtime.getRuntime().exec(cmd);
          }
        }
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_connecting_to_services__FINEST",
                new Object[] { Thread.currentThread().getName(), casProcessorConfig.getName() });
      }

      // Services has been started so now connect proxies to them.
      connectToServices(aProcessingContainer, casProcessorConfig, redeploy, howMany);
    } catch (ConnectException e) {
      e.printStackTrace();
      throw new CasProcessorDeploymentException(e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new CasProcessorDeploymentException(e);
    }
  }

  /**
   * No-op for integrated deployment. Integrated CasProcessors are instantiated using UIMA framework
   * factory in the CPEFactory.
   * 
   * @param aProcessingContainer
   * @param redeploy
   * @throws ResourceConfigurationException tbd
   */
  private void deployIntegrated(ProcessingContainer aProcessingContainer, boolean redeploy)
          throws ResourceConfigurationException {
    // Integrated CasProcessors are deployed in the CPEFactory
  }

  /**
   * Deploys CasProcessor as a remote/network service. In this case, the CPE does not manage
   * life-cycle of the Cas Processor. The CPE in this case simply creates a proxy to the remote Cas
   * Processor via provided VNS. The exact VNS used here is defined on per Cas Processor in the cpe
   * descriptor.
   * 
   * @param aProcessingContainer -
   *          container that will manage instances of the CasProcessor
   * @param redeploy -
   *          flag indicating if this redeployment of failed CasProcessor
   * @throws ResourceConfigurationException wraps exception
   */
  private void deployRemote(ProcessingContainer aProcessingContainer, boolean redeploy)
          throws ResourceConfigurationException {
    CasProcessorConfiguration casProcessorConfig = aProcessingContainer
            .getCasProcessorConfiguration();
    String name = casProcessorConfig.getName();

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_deploy_with_mode__FINEST",
              new Object[] { Thread.currentThread().getName(), name,
                  casProcessorConfig.getDeploymentType() });
    }

    try {
      // Remote CasProcessors are accessed by service name lookup in VNS
      String serviceUri = getServiceUri(casProcessorConfig);

      // How many proxies to create to remote service
      int concurrentThreadCount = cpeFactory.getProcessingUnitThreadCount();

      // Currently the CPM tries as many connections to the external annotator service as there are
      // threads.

      // #########################################################################################
      // # This code adjusts number of proxies in the CasProcessor Pool. This code removes those
      // # Cas Processors from the pool that have no assigned proxy. This may happen if there are
      // # not enough fenced services started and CPE configuration requires exclusive access to
      // # a service. The CPM will adjust number of processing pipelines to the lowest instance
      // # count found in all Cas Processor pools. For example, if the CPE descriptor is setup for
      // # 6 processing pipelines BUT there are only 3 available EXCLUSIVE fenced services the CPM
      // # will adjust the number of processing pipelines to 3.
      // #########################################################################################
      boolean exclusiveAccess = false;

      String serviceAccess = casProcessorConfig.getDeploymentParameter("service-access");
      if (serviceAccess != null && serviceAccess.equalsIgnoreCase("exclusive")) {
        exclusiveAccess = true;
      }

      int howMany = 1;
      if (!redeploy) {
        howMany = concurrentThreadCount;
      }

      int serviceCount = attachToServices(redeploy, serviceUri, howMany, aProcessingContainer);
      if (redeploy == false && exclusiveAccess && serviceCount < concurrentThreadCount) {
        ServiceProxyPool pool = aProcessingContainer.getPool();
        int poolSize = pool.getSize();
        for (int i = 0; i < poolSize; i++) {
          // Adjust number of CasProcessors in the pool
          CasProcessor processor = pool.checkOut();
          if (processor instanceof NetworkCasProcessorImpl) {
            if (((NetworkCasProcessorImpl) processor).getProxy() == null) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "initialize",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_reducing_cp_instance_count__FINEST",
                        new Object[] { Thread.currentThread().getName(), name,
                            casProcessorConfig.getDeploymentType() });
              }
              processor = null;
            } else {
              pool.checkIn(processor);
            }
          }
        }

      }

    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }

  }

  /**
   * Create and attach a proxies to vinci services. For services with exclusive access, makes sure
   * that there is only one proxy per service. Otherwise the services are shared.
   * 
   * @param redeploy -
   *          true if the connection is made as part of the recovery due to previous service crash
   *          or termination.
   * @param aServiceUri -
   *          servie uri, this is a vinci service name
   * @param howMany -
   *          how many proxies to create and connect
   * @param aProcessingContainer -
   *          hosting container for the proxies. Proxies are assigned to a pool managed by the
   *          container.
   * 
   * @return - how many proxies were created
   * @throws Exception -
   *           error
   */
  private int attachToServices(boolean redeploy, String aServiceUri, int howMany,
          ProcessingContainer aProcessingContainer) throws Exception {
    VinciServiceInfo serviceInfo = null;

    // Retrieve configuration information for the CasProcessor. This is the configuration from the
    // CPE descriptor
    CasProcessorConfiguration casProcessorConfig = aProcessingContainer
            .getCasProcessorConfiguration();
    ArrayList serviceList = null;
    // Check if this Cas Processor has exclusive service access. Meaning it requires service per
    // proxy.
    boolean exclusiveAccess = false;
    String serviceAccess = casProcessorConfig.getDeploymentParameter("service-access");
    if (serviceAccess != null && serviceAccess.equalsIgnoreCase("exclusive")) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cp_with_exclusive_access__FINEST",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                    casProcessorConfig.getDeploymentType() });
      }
      exclusiveAccess = true;
      // The following is true if the service just crashed and we are trying to recover. The
      // container in this case
      // should contain a reference to the Cas Processor that failed.
      if (((ProcessingContainer_Impl) aProcessingContainer).failedCasProcessorList.size() > 0) {
        // In case the service starts on the same port as before it crashed, mark it as
        // available in the current service list
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "initialize",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_retrieve_cp_from_failed_cplist__FINEST",
                  new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                      casProcessorConfig.getDeploymentType() });
        }
        CasProcessor processor = (CasProcessor) ((ProcessingContainer_Impl) aProcessingContainer).failedCasProcessorList
                .get(0);
        // Extract old (stale) proxy from the CAS Processor
        VinciTAP tap = ((NetworkCasProcessorImpl) processor).getProxy();
        if (tap != null) {
          // Since the new service may have started on the same machine and the same port, make sure
          // that the
          // current list is updated so that the service can be again connected to.
          String service_host = tap.getServiceHost();
          int service_port = tap.getServicePort();
          // Go through all services in the in-use service list and try to find one that matches
          // host and port
          // If found, change the state of the service to available again.
          for (int i = 0; currentServiceList != null && i < currentServiceList.size(); i++) {
            VinciServiceInfo si = (VinciServiceInfo) currentServiceList.get(i);
            if (si.getHost().equals(service_host) && si.getPort() == service_port) {
              si.setAvailable(true);
            }
          }
        }

      }

      // Retrieve a new service list from the VNS
      serviceList = getNewServiceList(aServiceUri, casProcessorConfig);

      if (serviceList == null || serviceList.size() == 0 && redeploy == false) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_no_service_in_vns__FINEST", new Object[] {
                    Thread.currentThread().getName(), aServiceUri,
                    casProcessorConfig.getDeploymentType(),
                    casProcessorConfig.getDeploymentParameter(Constants.VNS_HOST),
                    casProcessorConfig.getDeploymentParameter(Constants.VNS_PORT) });
      }
      // When redeploying/reconnecting, we only interested in one proxy
      if (redeploy) {
        howMany = 1; // override
      }
      // If this is the first time through here, make sure the service list is cached. This is the
      // current service list.
      if (currentServiceList == null) {
        currentServiceList = serviceList;
      } else {
        // Copy the state of each service from current list to new list. Just change the state
        // of service from unassigned to assigned if it happens not to be in use.
        int newServiceCount = VNSQuery.findUnassigned(currentServiceList, serviceList);
        if (newServiceCount > 0) {
          // new Services have been started. Use the new list as current.
          currentServiceList.clear();
          currentServiceList = serviceList;
        }
      }

    }
    int succesfullConnections = 0;

    int rC = casProcessorConfig.getMaxRestartCount();

    int maxTimeToWait = casProcessorConfig.getMaxTimeToWaitBetweenRetries();
    // Never sleep indefinitely. Override if the maxTimeToWait = 0
    if (maxTimeToWait == 0) {
      maxTimeToWait = WAIT_TIME;
    }

    for (int i = 0; i < howMany; i++) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_activating_service__FINEST",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                    String.valueOf(i), String.valueOf(howMany) });
      }
      int restartCount = 0;
      // flag to indicate that we should sleep between retries when connecting to a fenced service.
      // First time through the do-while loop below dont sleep though.
      boolean sleepBetweenRetries = false;
      // Attempt to connect to remote service and activate the proxy.

      do {
        try {
          if (exclusiveAccess) {
            serviceInfo = getNextAvailable(serviceList);
            if (serviceInfo == null) {
              if (redeploy == false) {
                // No more services available. Report how many services we attached to so far
                return succesfullConnections;
              }
              sleepBetweenRetries = true;
            }
          }
          if (sleepBetweenRetries) {
            try {
              Thread.sleep(maxTimeToWait);
            } catch (InterruptedException iex) {
            }

            if (serviceInfo == null && exclusiveAccess) {
              // Get a new service list from the VNS
              restartCount++;

              serviceList = getNewServiceList(aServiceUri, casProcessorConfig);

              // Copy the state of each service from current list to new list. Just change the state
              // of service from unassigned to assigned if it happens to be in use.
              int newServiceCount = VNSQuery.findUnassigned(currentServiceList, serviceList);
              if (newServiceCount > 0) {
                currentServiceList.clear();
                currentServiceList = serviceList;
              }
              continue;
            }
          }
          if (exclusiveAccess) {
            activateProcessor(casProcessorConfig, serviceInfo.getHost(), serviceInfo.getPort(),
                    aProcessingContainer, redeploy);
            serviceInfo.setAvailable(false);
          } else {
            // Create and connect a new proxy to the service
            activateProcessor(casProcessorConfig, aServiceUri, aProcessingContainer, redeploy);
          }
          succesfullConnections++;
          // on successfull connection, just brake out of the retry loop
          break;
        } catch (ResourceInitializationException ex) {
          if (ex.getCause() instanceof ServiceException
                  || ex.getCause() instanceof ServiceDownException) {
            if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.WARNING,
                      this.getClass().getName(),
                      "initialize",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_activating_service__WARNING",
                      new Object[] { Thread.currentThread().getName(),
                          aProcessingContainer.getName() });
            }
            // Increment number of CasProcessor restarts (redeploys)
            restartCount++;
          } else {
            restartCount = rC + 1; // Force termination of the loop
          }
        }
      } while (restartCount <= rC);

      // If exceeding defined max number of retries, take associated action with this condition
      // CasProcessor may be configured to Terminate the CPM, Disable itself or Disregard this
      // condition and continue.
      if (restartCount > rC) {
        handleMaxRestartThresholdReached(aProcessingContainer);
      }
    }
    return succesfullConnections;
  }

  private VinciServiceInfo getNextAvailable(ArrayList aServiceList) {
    VinciServiceInfo serviceInfo = null;

    for (int i = 0; i < aServiceList.size(); i++) {
      serviceInfo = (VinciServiceInfo) aServiceList.get(i);
      if (serviceInfo != null) {
        if (serviceInfo.isAvailable()) {
          return serviceInfo;
        }
        // Invalidate reference
        serviceInfo = null;
      }
    }
    return serviceInfo;
  }

  /**
   * Query configured VNS for a list of services with a given service URI
   * 
   * @param aServiceUri -
   *          named service endpoint
   * @param aCasProcessorConfig -
   *          Cas Processor configuration
   * @return - List of services provided by VNS
   * 
   * @throws Exception passthru
   */
  private ArrayList getNewServiceList(String aServiceUri,
          CasProcessorConfiguration aCasProcessorConfig) throws Exception {
    String vnsHost = getVNSSettingFor("VNS_HOST", aCasProcessorConfig, "localhost");
    String vnsPort = getVNSSettingFor("VNS_PORT", aCasProcessorConfig, "9000");
    VNSQuery vnsQuery = new VNSQuery(vnsHost, Integer.parseInt(vnsPort));
    return vnsQuery.getServices(aServiceUri);

  }

  /**
   * Handles situation when max restart threshold is reached while connecting to CasProcessor.
   * 
   * 
   * @param aProcessingContainer -
   *          container holding CasProcessor configuration
   * 
   * @throws ResourceConfigurationException when max restarts limit reached
   */
  private void handleMaxRestartThresholdReached(ProcessingContainer aProcessingContainer)
          throws ResourceConfigurationException {
    CasProcessorConfiguration casProcessorConfig = aProcessingContainer
            .getCasProcessorConfiguration();
    String name = casProcessorConfig.getName();
    if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_restart_exceeded__WARNING",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                  String.valueOf(casProcessorConfig.getMaxRestartCount()) });
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_restart_exceeded__WARNING",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                  casProcessorConfig.getActionOnMaxRestart() });
    }
    // Throw appropriate exception based on configured action when max restarts are reached
    if (Constants.TERMINATE_CPE.equals(casProcessorConfig.getActionOnMaxRestart())) {
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_terminate_onerrors__INFO",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName() });
      }
      throw new ResourceConfigurationException(new AbortCPMException(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_configured_to_abort__WARNING", new Object[] {
                          Thread.currentThread().getName(), name })));
    }
    if (Constants.KILL_PROCESSING_PIPELINE.equals(casProcessorConfig.getActionOnMaxRestart())) {
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_terminate_pipeline__INFO",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName() });
      }
      throw new ResourceConfigurationException(new KillPipelineException(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_configured_to_kill_pipeline__WARNING", new Object[] {
                          Thread.currentThread().getName(), name })));
    } else if (Constants.DISABLE_CASPROCESSOR.equals(casProcessorConfig.getActionOnMaxRestart())) {
      aProcessingContainer.setStatus(Constants.CAS_PROCESSOR_DISABLED);
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disable_cp__INFO",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName() });
      }
      return;
    }
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disable_cp__INFO",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName() });
    }
  }

  /**
   * Returns CasProcessor service name from a descriptor.
   * 
   * @param aCasProcessorConfig -
   *          CasProcessor configuration containing service descriptor path
   * @return - name of the service
   * @throws ResourceConfigurationException if the uri is missing or empty
   */
  private String getServiceUri(CasProcessorConfiguration aCasProcessorConfig)
          throws ResourceConfigurationException {
    URL descriptorUrl;
    descriptorUrl = aCasProcessorConfig.getDescriptorUrl();

    URISpecifier uriSpecifier = getURISpecifier(descriptorUrl);
    String uri = uriSpecifier.getUri();
    if (uri == null || uri.trim().length() == 0) {
      throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_invalid_deployment__SEVERE", new Object[] {
                  Thread.currentThread().getName(), descriptorUrl, uri });
    }

    return uri;
  }

  /**
   * Returns URISpecifier
   * 
   * @param aFileName
   * @return URISpecifier
   * @throws ResourceConfigurationException if the resource specifier in the URI is not a URISpecifier
   */
  private URISpecifier getURISpecifier(URL aDescriptorUrl) throws ResourceConfigurationException {
    ResourceSpecifier resourceSpecifier = getSpecifier(aDescriptorUrl);

    if (!(resourceSpecifier instanceof URISpecifier)) {
      throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_invalid_deployment__SEVERE", new Object[] {
                  Thread.currentThread().getName(), aDescriptorUrl, null });
    }
    return (URISpecifier) resourceSpecifier;
  }

  /**
   * Parses given service descriptor and returns initialized ResourceSpecifier
   * 
   * @param aUrl -
   *          URL of the descriptor
   * @return - ResourceSpecifier parsed from descriptor
   * @throws ResourceConfigurationException wraps Exception
   */
  private ResourceSpecifier getSpecifier(URL aUrl) throws ResourceConfigurationException {
    try {
      XMLInputSource in = new XMLInputSource(aUrl);
      return UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_invalid_deployment__SEVERE", new Object[] {
                  Thread.currentThread().getName(), aUrl, null });
    }
  }

  /**
   * Deploys internal VNS for use with local CasProcessor deployments
   * 
   * @param casProcessorConfig -
   *          CasProcessor configuration
   * @param redeploy -
   *          flag indicating if VNS being redeployed
   * @throws CasProcessorDeploymentException wraps Exception
   */
  private void deployVNS(CasProcessorConfiguration casProcessorConfig, boolean redeploy)
          throws CasProcessorDeploymentException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.SEVERE,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_deploy_vns_redeploy__FINEST",
              new Object[] { Thread.currentThread().getName(), String.valueOf(redeploy),
                  String.valueOf(restartCount) });
    }
    int vnsPort = Integer.parseInt(DEFAULT_VNS_PORT);
    int startPort = DEFAULT_SERVICE_PORT;
    int maxPort = DEFAULT_SERVICE_PORT + DEFAULT_SERVICE_PORT_RANGE;
    // Check if want to override default port for the VNS
    // To override use -DVNS_PORT=value
    if (System.getProperty("LOCAL_VNS_PORT") != null) {
      try {
        vnsPort = Integer.parseInt(System.getProperty("LOCAL_VNS_PORT"));
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    if (redeploy) {
      restartCount++;
      try {
        startPort += restartCount;
      } catch (NumberFormatException e) {
        e.printStackTrace();
        throw new CasProcessorDeploymentException(e);
      }

    }

    synchronized (VinciCasProcessorDeployer.class) {
      if (vns == null) {
        try {
          vns = new LocalVNS(startPort, maxPort, vnsPort);
          vns.setConnectionPool(portQueue);
  
          localVNSThread = new Thread(vns);
          localVNSThread.start();
  
        } catch (Exception e) {
          throw new CasProcessorDeploymentException(e);
        }
      }
    }
  }

  // Never called
//  /**
//   * Creates a proxy and connects it to Vinci service running on a given port. Once connected the
//   * proxy is associated with Cas Processor.
//   * 
//   * @param aCasProcessorConfig -
//   *          CasProcessor configuration
//   * @param port -
//   *          port wher vinci service is listening
//   * @return Connected proxy to service
//   * 
//   * @throws ResourceInitializationException
//   * @throws ResourceConfigurationException
//   */
//  private synchronized boolean activateProcessor(CasProcessorConfiguration aCasProcessorConfig,
//          String aHost, int aPort) throws ResourceInitializationException,
//          ResourceConfigurationException {
//    // Instantiate proxy from given configuration
//    VinciTAP tap = getTextAnalysisProxy(aCasProcessorConfig);
//    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
//              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//              "UIMA_CPM_activating_service_on_port__FINEST",
//              new Object[] { Thread.currentThread().getName(), String.valueOf(aPort) });
//    }
//    boolean tryAgain = true;
//    int maxCount = aCasProcessorConfig.getMaxRestartCount();
//    while (tryAgain) {
//      try {
//        // Connect proxy to service running on given port on the same machine as the CPM
//        tap.connect(aHost, aPort);
//        // Connection established no need to retry
//        tryAgain = false;
//      } catch (Exception e) {
//        if (maxCount-- == 0) {
//          e.printStackTrace();
//          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
//            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE,
//                    Thread.currentThread().getName() + "", e);
//          }
//          throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                  "UIMA_CPM_no_service_connection__SEVERE", new Object[] {
//                      Thread.currentThread().getName(), String.valueOf(aPort), aHost,
//                      aCasProcessorConfig.getName() });
//        }
//        try {
//          synchronized (monitor) {
//            monitor.wait(SLEEP_TIME);
//          }
//        } catch (InterruptedException ex) {
//        }
//      }
//    }
//    // At this point there is a connected proxy (tap), now we need to add it to container that will
//    // menage it.
//    bindProxyToNetworkCasProcessor(tap);
//    return true;
//  }

  /**
   * Creates a proxy and connects it to Vinci service running on a given port. Once connected the
   * proxy is associated with Cas Processor.
   * 
   * @param aCasProcessorConfig -
   *          CasProcessor configuration
   * @param port -
   *          port wher vinci service is listening
   * @return Connected proxy to service
   * 
   * @throws ResourceConfigurationException wraps Exception
   * @throws Exception passthru
   */
  private synchronized boolean activateProcessor(CasProcessorConfiguration aCasProcessorConfig,
          String aHost, int aPort, ProcessingContainer aProcessingContainer, boolean redeploy)
          throws ResourceConfigurationException, Exception {
    // Instantiate proxy from given configuration
    VinciTAP tap = getTextAnalysisProxy(aCasProcessorConfig);
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_activating_service_on_port__FINEST",
              new Object[] { Thread.currentThread().getName(), String.valueOf(aPort) });
    }
    boolean tryAgain = true;
    int maxCount = aCasProcessorConfig.getMaxRestartCount();
    while (tryAgain) {
      try {
        // Connect proxy to service running on given port on the same machine as the CPM
        tap.connect(aHost, aPort);
        // Connection established no need to retry
        tryAgain = false;
      } catch (Exception e) {
        if (maxCount-- == 0) {
          e.printStackTrace();
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE,
                    Thread.currentThread().getName() + "", e);
          }
          throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_no_service_connection__SEVERE", new Object[] {
                      Thread.currentThread().getName(), String.valueOf(aPort), aHost,
                      aCasProcessorConfig.getName() });
        }
        try {
          synchronized (monitor) {
            monitor.wait(SLEEP_TIME);
          }
        } catch (InterruptedException ex) {
        }
      }
    }
    // At this point there is a connected proxy (tap), now we need to add it to container that will
    // menage it.
    bindProxyToNetworkCasProcessor(tap, aProcessingContainer, redeploy);
    return true;
  }

  /**
   * Creates a proxy and connects it to Vinci service. Uses VNS to locate service by name.
   * 
   * @param aCasProcessorConfig -
   *          CasProcees configuration
   * @param aService -
   *          name of the vinci service
   * @return - connected Proxy
   * 
   * @throws Exception passthru 
   */
  private synchronized boolean activateProcessor(CasProcessorConfiguration aCasProcessorConfig,
          String aService, ProcessingContainer aProcessingContainer, boolean redeploy) // throws
          // ResourceInitializationException,
          // ResourceConfigurationException
          throws Exception {
    VinciTAP tap = getTextAnalysisProxy(aCasProcessorConfig);
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_activating_service_on_port__FINEST",
              new Object[] { Thread.currentThread().getName(), aService });

    }
    boolean tryAgain = true;
    int maxCount = aCasProcessorConfig.getMaxRestartCount();
    int maxTimeToWait = aCasProcessorConfig.getMaxTimeToWaitBetweenRetries();
    // Never sleep indefinitely. Override if the maxTimeToWait = 0
    if (maxTimeToWait == 0) {
      maxTimeToWait = WAIT_TIME;
    }
    // Initially dont sleep
    boolean sleepBetweenRetries = false;

    while (tryAgain) {
      try {
        if ( sleepBetweenRetries ) {
          wait(maxTimeToWait);
        } else {
          sleepBetweenRetries = true;
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_connecting_to_service__FINEST",
                  new Object[] { Thread.currentThread().getName(), aService });
        }
        // Try to connect to service using its service name
        tap.connect(aService);
        tryAgain = false;
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_connection_established__FINEST",
                  new Object[] { Thread.currentThread().getName(), aService });
        }
      } catch (Exception e) {
        if (maxCount-- == 0) {
          e.printStackTrace();
          if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.WARNING,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_max_connect_retries_exceeded__FINEST",
                    new Object[] { Thread.currentThread().getName(), aService,
                        String.valueOf(maxCount) });
          }
          throw new ResourceInitializationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE, new Object[] {
              Thread.currentThread().getName(), aService, String.valueOf(maxCount) },
                  new ServiceConnectionException("Unable to connect to service :::" + aService));
        }
      }

    }
    // Connected to the service so add tap to Cas Processor
    bindProxyToNetworkCasProcessor(tap, aProcessingContainer, redeploy);
    return true;
  }

  // Never called
//  /**
//   * Associates connected proxy with an instance of CasProcessor.
//   * 
//   * @param aTap -
//   *          connected proxy
//   */
//  private void bindProxyToNetworkCasProcessor(VinciTAP aTap) {
//    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//      UIMAFramework.getLogger(this.getClass())
//              .logrb(
//                      Level.FINEST,
//                      this.getClass().getName(),
//                      "initialize",
//                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                      "UIMA_CPM_get_cp_from_pool__FINEST",
//                      new Object[] { Thread.currentThread().getName(), aTap.getServiceHost(),
//                          String.valueOf(aTap.getServicePort()),
//                          String.valueOf(casProcessorPool.getSize()) });
//    }
//    synchronized (casProcessorPool) {
//      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//        UIMAFramework.getLogger(this.getClass()).logrb(
//                Level.FINEST,
//                this.getClass().getName(),
//                "initialize",
//                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                "UIMA_CPM_get_cp_from_pool__FINEST",
//                new Object[] { Thread.currentThread().getName(), aTap.getServiceHost(),
//                    String.valueOf(aTap.getServicePort()),
//                    String.valueOf(casProcessorPool.getSize()) });
//      }
//      CasProcessor processor = null;
//      // There are as many Cas Processors as there are Procesing Threads. All of them have been
//      // already created and exist
//      // in the cas processor pool. Cas Processor may or may not already have an associated proxy.
//      // So here we cycle through all Cas Processors until we find one that has not yet have a proxy
//      // and
//      // add it.
//      for (int i = 0; i < casProcessorPool.getSize(); i++) {
//        try {
//          // Check out the next Cas Processor from the pool
//          processor = casProcessorPool.checkOut();
//          if (processor == null) {
//            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
//              UIMAFramework.getLogger(this.getClass()).logrb(
//                      Level.SEVERE,
//                      this.getClass().getName(),
//                      "initialize",
//                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                      "UIMA_CPM_get_cp_from_pool_error__SEVERE",
//                      new Object[] { Thread.currentThread().getName(), aTap.getServiceHost(),
//                          String.valueOf(aTap.getServicePort()),
//                          String.valueOf(casProcessorPool.getSize()) });
//            }
//            continue;
//          }
//          // Add proxy only to instances of NetworkCasProcessorImpl
//          if (processor instanceof NetworkCasProcessorImpl) {
//            NetworkCasProcessorImpl netProcessor = (NetworkCasProcessorImpl) processor;
//            // Check if this Cas Processor has already been assigned a proxy. If so,
//            // get the next Cas Processor
//            if (netProcessor.getProxy() != null && netProcessor.getProxy().isConnected()) {
//              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
//                        this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                        "UIMA_CPM_already_allocated__FINEST",
//                        new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
//              }
//              continue;
//            }
//            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//              UIMAFramework.getLogger(this.getClass()).logrb(
//                      Level.FINEST,
//                      this.getClass().getName(),
//                      "initialize",
//                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                      "UIMA_CPM_assign_cp_to_service__FINEST",
//                      new Object[] { Thread.currentThread().getName(), String.valueOf(i),
//                          aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) });
//            }
//            // Associate the proxy with this Cas Processor
//            ((NetworkCasProcessorImpl) processor).setProxy(aTap);
//            synchronized (monitor) {
//
//              monitor.notifyAll();
//            }
//          }
//          break;
//        } catch (Exception e) {
//          e.printStackTrace();
//        } finally {
//          if (processor != null) {
//            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//              UIMAFramework.getLogger(this.getClass()).logrb(
//                      Level.FINEST,
//                      this.getClass().getName(),
//                      "initialize",
//                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                      "UIMA_CPM_checkin_cp_to_pool__FINEST",
//                      new Object[] { Thread.currentThread().getName(), String.valueOf(i),
//                          aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) });
//            }
//            casProcessorPool.checkIn(processor);
//            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
//              UIMAFramework.getLogger(this.getClass()).logrb(
//                      Level.FINEST,
//                      this.getClass().getName(),
//                      "initialize",
//                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
//                      "UIMA_CPM_checked_in_cp_to_pool__FINEST",
//                      new Object[] { Thread.currentThread().getName(), String.valueOf(i),
//                          aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) });
//            }
//          }
//        }
//      }
//    }
//  }

  /**
   * Associates connected proxy with an instance of CasProcessor.
   * 
   * @param aTap -
   *          connected proxy
   */
  private void bindProxyToNetworkCasProcessor(VinciTAP aTap,
          ProcessingContainer aProcessingContainer, boolean redeploy) throws Exception {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_checked_in_cp_to_pool__FINEST",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                  aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) });
    }
    CasProcessor processor = null;
    boolean processorAssigned = false;
    try {
      if (redeploy) {
        // Check out the next Cas Processor from the pool
        if (((ProcessingContainer_Impl) aProcessingContainer).failedCasProcessorList.isEmpty()) {
          throw new ResourceProcessException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_no_cp_instance_in_failed_list__FINEST", new Object[] {
                      Thread.currentThread().getName(), aProcessingContainer.getName(),
                      aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) },
                  new Exception(CpmLocalizedMessage.getLocalizedMessage(
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_EXP_cp_not_in_failed_list__WARNING", new Object[] {
                              Thread.currentThread().getName(), aProcessingContainer.getName() })));

        }
        processor = (CasProcessor) ((ProcessingContainer_Impl) aProcessingContainer).failedCasProcessorList
                .get(0);
        // Add proxy only to instances of NetworkCasProcessorImpl
        if (processor instanceof NetworkCasProcessorImpl) {
          // NetworkCasProcessorImpl netProcessor = (NetworkCasProcessorImpl) processor;
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_assign_cp_to_service__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        aProcessingContainer.getName(), aTap.getServiceHost(),
                        String.valueOf(aTap.getServicePort()) });
          }
          // Associate the proxy with this Cas Processor
          ((NetworkCasProcessorImpl) processor).setProxy(aTap);
          processorAssigned = true;
          synchronized (monitor) {
            monitor.notifyAll();
          }
        }
      } else {

        int cpPoolSize = casProcessorPool.getSize();
        for (int i = 0; i < cpPoolSize; i++) {

          // Check out the next Cas Processor from the pool
          processor = casProcessorPool.checkOut();
          if (processor == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.SEVERE,
                      this.getClass().getName(),
                      "initialize",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_get_cp_from_pool_error__SEVERE",
                      new Object[] { Thread.currentThread().getName(), aTap.getServiceHost(),
                          String.valueOf(aTap.getServicePort()),
                          String.valueOf(casProcessorPool.getAllInstanceCount()) });
            }
            continue;
          }
          // Add proxy only to instances of NetworkCasProcessorImpl
          if (processor instanceof NetworkCasProcessorImpl) {
            NetworkCasProcessorImpl netProcessor = (NetworkCasProcessorImpl) processor;
            // Check if this Cas Processor has already been assigned a proxy. If so,
            // get the next Cas Processor
            if (netProcessor.getProxy() != null && netProcessor.getProxy().isConnected()) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {

                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                        this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_already_allocated__FINEST",
                        new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
              }
              continue;
            }
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "initialize",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_assign_cp_to_service__FINEST",
                      new Object[] { Thread.currentThread().getName(), String.valueOf(i),
                          aTap.getServiceHost(), String.valueOf(aTap.getServicePort()) });
            }
            // Associate the proxy with this Cas Processor
            ((NetworkCasProcessorImpl) processor).setProxy(aTap);

            processorAssigned = true;

            synchronized (monitor) {
              monitor.notifyAll();
            }
            break;
          }
        }

      }
    } catch (Exception e) {
      throw e;
    } finally {
      if (processor != null) {
        if (processorAssigned) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_checkin_cp_to_pool__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        aProcessingContainer.getName(), aTap.getServiceHost(),
                        String.valueOf(aTap.getServicePort()) });
          }
          casProcessorPool.checkIn(processor);

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_checked_in_cp_to_pool__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        aProcessingContainer.getName(), aTap.getServiceHost(),
                        String.valueOf(aTap.getServicePort()) });
          }
        } else {
          // Put it back into the failed Cas Processor List for subsequent retries
          ((ProcessingContainer_Impl) aProcessingContainer).failedCasProcessorList.add(processor);
        }
      }
    }

  }

  /**
   * Creates and initializes proxy that will be used to connect to Vinci service
   * 
   * @param aCasProcessorConfig -
   *          CasProcessor configuration
   * @return - new proxy (not yet connected to service)
   * 
   * @throws ResourceConfigurationException wraps Exception
   */
  private VinciTAP getTextAnalysisProxy(CasProcessorConfiguration aCasProcessorConfig)
          throws ResourceConfigurationException {
    VinciTAP tap = new VinciTAP();
    String vnsHost = null;
    String vnsPort = null;

    // For 'local' or managed Cas Processor, get the VNS port
    if (aCasProcessorConfig.getDeploymentType() != null
            && Constants.DEPLOYMENT_LOCAL.equalsIgnoreCase(aCasProcessorConfig.getDeploymentType())) {
      vnsHost = "localhost"; // default for local deployment
      vnsPort = ""; // intialize
      try {
        vnsPort = String.valueOf(vns.getPort());
      } catch (Exception e) {
        throw new ResourceConfigurationException(e);
      }
    } else {
      // try to retrieve VNS location (host and port) from the service descriptor. If not found,
      // try to find the settings in the following order:
      // 1) check CPE descriptor settings (in <deploymentParameters>)
      // 2) check System property (set via -D on the command line)
      // 3) use defaults ( VNS_HOST=localhost and VNS_PORT=9000)
      if (vnsHost == null) {
        vnsHost = getVNSSettingFor("VNS_HOST", aCasProcessorConfig, "localhost");
      }
      if (vnsPort == null) {
        vnsPort = getVNSSettingFor("VNS_PORT", aCasProcessorConfig, "9000");
      }
    }
    // Get the max timeout the CPE will wait for response from the Cas Processor
    long timeout = aCasProcessorConfig.getTimeout();
    String[] keysToDrop = aCasProcessorConfig.getKeysToDrop();
    if (keysToDrop != null) {
      tap.setKeys2Drop(keysToDrop);
    }
    // Configure the proxy with VNS settings and timeout
    tap.setVNSHost(vnsHost);
    tap.setVNSPort(vnsPort);
    tap.setTimeout((int) timeout);
    String timerClass = "";
    try {
      timerClass = cpeFactory.getCPEConfig().getTimerImpl();
    } catch (Exception e) {
      // ignore. Use defa1ult
    }
    if (timerClass != null) {
      try {
        tap.setTimer(CPMUtils.getTimer(cpeFactory.getCPEConfig().getCpeTimer().get()));
      } catch (Exception e) {
        e.printStackTrace();
        throw new ResourceConfigurationException(e);
      }
    }
    return tap;
  }

  /**
   * Returns a value for a named VNS parameter (either VNS_HOST or VNS_PORT). The parameter is
   * resolved with the following priority: 1) Find the parameter in the Service descriptor 2) Find
   * the parameter in the CPE descriptor 3) Find the parameter as System.property (using -D on the
   * command line) 4) Use Hardcoded default ( VNS_HOST=localhost VNS_PORT=9000)
   * 
   * 
   * @param aVNSParamKey -
   *          name of the VNS parameter for which the value is sought
   * @param aCasProcessorConfig -
   *          CPE descriptor settings
   * @param aDefault -
   *          default value for the named parameter if parameter is not defined
   * 
   * @return - value for a named VNS parameter
   * @throws ResourceConfigurationException passthru
   */
  private String getVNSSettingFor(String aVNSParamKey,
          CasProcessorConfiguration aCasProcessorConfig, String aDefault)
          throws ResourceConfigurationException {
    String vnsParam = null;

    URL descriptorUrl = aCasProcessorConfig.getDescriptorUrl();
    URISpecifier uriSpecifier = getURISpecifier(descriptorUrl);
    Parameter[] params = uriSpecifier.getParameters();

    for (int i = 0; params != null && i < params.length; i++) {
      if (aVNSParamKey.equals(params[i].getName())) {
        vnsParam = params[i].getValue();
        break;
      }
    }

    if (vnsParam == null) {
      // VNS not defined in the service descriptor. Use settings from CPE descriptor, if present
      // First translate the key. Unfortunately, the key in the CPE descriptor is vnsHost instead of
      // VNS_HOST and vnsPort instead of VNS_PORT
      if (aVNSParamKey.equals("VNS_HOST")) {
        vnsParam = aCasProcessorConfig.getDeploymentParameter(Constants.VNS_HOST);
      } else if (aVNSParamKey.equals("VNS_PORT")) {
        vnsParam = aCasProcessorConfig.getDeploymentParameter(Constants.VNS_PORT);
      }

      if (vnsParam == null) // VNS not defined in the CPE descriptor. Use -D parameter first if
      // it exists. If it does not exist, used default value
      {
        if ((vnsParam = System.getProperty(aVNSParamKey)) == null) {
          vnsParam = aDefault; // default
        }
      }
    }
    return vnsParam;
  }

  /**
   * This method is used during a launch of the local or managed Cas Processor. Here connections to
   * Cas Processors running as vinci services are made. Connections are established to services
   * whose ports are provided in the portQueue. Whenever a managed Cas Processor starts up, it
   * contacts local vns and is assigned a port to run on. The same port is added to the port queue
   * and used here for establishing a connection.
   * 
   * @param casProcessorConfig -
   *          CasProcessor configuration
   * @param redeploy -
   *          flag indicating if if this is restart
   * @param howMany -
   *          indicates how many connections to make
   * @throws ConnectException -
   *           unable to establish connection to Cas Processor
   */
  private void connectToServices(ProcessingContainer aProcessingContainer,
          CasProcessorConfiguration casProcessorConfig, boolean redeploy, int howMany)
          throws ConnectException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_thread_count__FINEST",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                  String.valueOf(howMany) });
    }
    // Create as many proxies as there are concurrent processing pipelines
    // The assumption is that the services have started up and retrieved ports to run on from the
    // local VNS
    // managed by the CPE.
    for (int i = 0; i < howMany; i++) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_activating_service_on_port2__FINEST",
                new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                    String.valueOf(i), String.valueOf(howMany) });
      }
      Object portObject = null;
      int port = -1;
      try {
        // get the next port from the queue. This queue is filled by the local VNS
        // during service registration. On startup each service registers itself
        // with local VNS and is given a port to run on. The assigned port is than
        // enqueued by the vns onto the portQueue.
        portObject = getPort(portQueue);
        port = Integer.parseInt((String) portObject);
      } catch (TimeLimitExceededException e) {
        e.printStackTrace();
        throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_no_service_port__WARNING",
                new Object[] { Thread.currentThread().getName() }));
      } catch (NumberFormatException e) {
        e.printStackTrace();
        if (portObject != null && portObject instanceof String) {
          throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_service_port__WARNING",
                  new Object[] { Thread.currentThread().getName(), (String) portObject }));
        } else {
          throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_service_port__WARNING",
                  new Object[] { Thread.currentThread().getName(), "Not Available" }));
        }
      } catch (Exception e) {
        e.printStackTrace();
        if (portObject != null && portObject instanceof String) {
          throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_service_port__WARNING",
                  new Object[] { Thread.currentThread().getName(), (String) portObject }));
        } else {
          throw new ConnectException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_service_port__WARNING",
                  new Object[] { Thread.currentThread().getName(), "Not Available" }));
        }
      }
      // Activate the CasProcessor by instantiating a proxy to the vinci service running
      // on a given port.
      try {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "initialize",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_activating_service_on_port__FINEST",
                  new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName(),
                      String.valueOf(port) });
        }
        // There is a valid port, so connect to it
        activateProcessor(casProcessorConfig, "127.0.0.1", port, aProcessingContainer, redeploy);
      } catch (Exception e) {
        e.printStackTrace();
        throw new ConnectException(e.getMessage());
      }
    }

  }

  /**
   * Used during a launch of the managed (local) Cas Processor this method returns a port number on
   * which the Cas Processor is waiting for requests. Each Cas Processor was a given a port by the
   * local vns where it is expected to accept requests from clients. The ports assigned to Cas
   * Processors are managed by the local instance of the VNS and available in the queue <i>portQueue</>.
   * 
   * @param portQueue -
   *          queue containing ports assigned to services by local VNS
   * @return - port as String
   * @throws TimeLimitExceededException timeout waiting for port
   */
  private String getPort(BoundedWorkQueue portQueue) throws TimeLimitExceededException {
    int waitCount = MAX_WAIT_TRIES; // default
    try {
      waitCount = System.getProperty(CONN_RETRY_COUNT) != null ? Integer.parseInt(System
              .getProperty(CONN_RETRY_COUNT)) : MAX_WAIT_TRIES;
    } catch (NumberFormatException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_retry_count__INFO",
                new Object[] { Thread.currentThread().getName() });
      }
      waitCount = MAX_WAIT_TRIES; // restore default
    }
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_got_port_from_queue__INFO",
              new Object[] { Thread.currentThread().getName(), "" });
    }
    while (portQueue.getCurrentSize() == 0) {
      
        try {
          if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_service_port_not_allocated__INFO",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(waitCount) });
          }
          Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (waitCount-- <= 0) {
          throw new TimeLimitExceededException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_EXP_timeout_no_service_port__WARNING", new Object[] {
                      Thread.currentThread().getName(),
                      String.valueOf(waitCount * WAIT_TIME + " millis") }));

        }
      
    }
    Object portObject = portQueue.dequeue();
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_got_port_from_queue__INFO",
              new Object[] { Thread.currentThread().getName(), (String) portObject });
    }
    return (String) portObject;
  }

  /**
   * Shutdown local VNS.
   * 
   */
  public void undeploy() throws CasProcessorDeploymentException {
    if (vns != null) {
      vns.shutdown();
      localVNSThread = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.container.deployer.CasProcessorDeployer#undeploy()
   */
  public void undeploy(URL aURL) throws CasProcessorDeploymentException {
  }

}
