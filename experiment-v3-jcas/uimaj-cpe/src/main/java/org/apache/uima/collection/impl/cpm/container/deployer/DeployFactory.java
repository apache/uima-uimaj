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

import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.ProcessControllerAdapter;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.SocketCasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.container.deployer.vinci.VinciCasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * Produces a deployer object for each type of deployment: local, remote and integrated.
 * 
 * 
 */
public class DeployFactory {
  public static final DeployFactory instance = new DeployFactory();

  private DeployFactory() {
  }
  
  /**
   * Returns a
   * {@link org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer} object
   * that specializes in deploying components as either local, remote, or integrated.
   * 
   * @param aCpeFactory cpe factory
   * @param aCasProcessorConfig cpe configuration reference
   * @param aPca mode of deployment.
   * @return appropriate deployer object for the mode of depolyment
   * @throws ResourceConfigurationException missing protocol or other deployment error
   */

  public static CasProcessorDeployer getDeployer(CPEFactory aCpeFactory,
          CpeCasProcessor aCasProcessorConfig, ProcessControllerAdapter aPca)
          throws ResourceConfigurationException {
    String deployMode = aCasProcessorConfig.getDeployment();

    if (Constants.DEPLOYMENT_LOCAL.equals(deployMode)) {
      return new VinciCasProcessorDeployer(aCpeFactory);
    } else if (Constants.DEPLOYMENT_REMOTE.equals(deployMode)) {
      String protocol = getProtocol(aCasProcessorConfig, aCpeFactory.getResourceManager());
      if (protocol == null || protocol.trim().length() == 0) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_service_descriptor__SEVERE", new Object[] {
                    Thread.currentThread().getName(), "<uriSpecifier>", "<protocol>" },
                new Exception(CpmLocalizedMessage.getLocalizedMessage(
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_EXP_invalid_service_descriptor__WARNING", new Object[] {
                            Thread.currentThread().getName(), aCasProcessorConfig.getName() })));
      } else if (Constants.SOCKET_PROTOCOL.equalsIgnoreCase(protocol)) {
        if (aPca == null) {
          throw new ResourceConfigurationException(
                  ResourceInitializationException.CONFIG_SETTING_ABSENT,
                  new Object[] { "ProcessControllerAdapter" });
        }
        return new SocketCasProcessorDeployer(aPca, aCpeFactory);
      } else {
        // Default is still Vinci
        return new VinciCasProcessorDeployer(aCpeFactory);
      }
    } else if (Constants.DEPLOYMENT_INTEGRATED.equals(deployMode)) {
      return new CPEDeployerDefaultImpl(aCpeFactory);
    }
    throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
            new Object[] { "deployment", "casProcessor" }, new Exception(CpmLocalizedMessage
                    .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_Exception_invalid_deployment__WARNING", new Object[] {
                                Thread.currentThread().getName(), aCasProcessorConfig.getName(),
                                deployMode })));
  }

  /**
   * Retrieve protocol from the service descriptor
   * 
   * @param aCasProcessorConfig Cas Processor configuration
   * @param aResourceManager needed to resolve import by name        
   * @return - protocol as string (vinci, socket)
   * 
   * @throws ResourceConfigurationException wraps Exception
   */
  public static String getProtocol(CpeCasProcessor aCasProcessorConfig, ResourceManager aResourceManager)
          throws ResourceConfigurationException {
    try {
      URL clientServiceDescriptor = aCasProcessorConfig.getCpeComponentDescriptor().findAbsoluteUrl(aResourceManager);
      ResourceSpecifier resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
              new XMLInputSource(clientServiceDescriptor));
      if (resourceSpecifier instanceof URISpecifier) {
        return ((URISpecifier) resourceSpecifier).getProtocol();
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
    return null;
  }
}
