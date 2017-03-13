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

package org.apache.uima.collection;

import java.io.IOException;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.ProcessTrace;

/**
 * Base class for CAS Consumers in UIMA SDK v1.x, which developers should extend 
 * with their own CAS Consumer implementation classes.
 * 
 * As of v2.0, there is no difference in capability between CAS Consumers
 * and ordinary Analysis Engines, except for the default setting of the XML
 * parameters for multipleDeploymentAllowed and modifiesCas. We recommend
 * for future work that users implement and use Analysis Engine components
 * instead of CAS Consumers.
 *  
 */
public abstract class CasConsumer_ImplBase extends ConfigurableResource_ImplBase implements
        CasConsumer {
  /**
   * Called by the framework to initialize this CAS Consumer. Subclasses should NOT override this
   * method; instead they should override the zero-argument {@link #initialize()} method and access
   * metadata via the {@link #getProcessingResourceMetaData()} method. This method is non-final only
   * for legacy reasons.
   * 
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // aSpecifier must be a CasConsumerDescription
    if (aSpecifier instanceof ResourceCreationSpecifier) {
      // do framework intitialiation
      if (super.initialize(aSpecifier, aAdditionalParams)) {
        // do user initialization
        initialize();
        return true;
      }
    }
    return false;
  }

  /**
   * This method is called during initialization, and does nothing by default. Subclasses should
   * override it to perform one-time startup logic.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization.
   */
  public void initialize() throws ResourceInitializationException {
    // no default behavior
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    // no default behavior
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem arg0) throws ResourceInitializationException {
    // no default behavior
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS[])
   */
  public void processCas(CAS[] aCASes) throws ResourceProcessException {
    for (int i = 0; i < aCASes.length; i++) {
      processCas(aCASes[i]);
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isStateless()
   */
  public boolean isStateless() {
    return false;
  }

  /**
   * Returns true. By contract, CAS Consumers must be read only.
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public final boolean isReadOnly() {
    OperationalProperties opProps = getProcessingResourceMetaData().getOperationalProperties();
    return opProps == null ? true : !opProps.getModifiesCas();
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#batchProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void batchProcessComplete(ProcessTrace arg0) throws ResourceProcessException, IOException {
    // no default behavior
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#collectionProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    // no default behavior
  }

  /**
   * Notifies this CAS Consumer that its configuration parameter settings have been changed. By
   * default this method just calls {@link #destroy()} followed by {@link #initialize()}. CAS
   * Consumers that have expensive initialization that does not need to be redone whenever
   * configuration parameters change may wish to override this method to provide a more efficient
   * implementation.
   * 
   * @see org.apache.uima.resource.ConfigurableResource_ImplBase#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    super.reconfigure();
    destroy();
    try {
      initialize();
    } catch (ResourceInitializationException e) {
      throw new ResourceConfigurationException(e);
    }
  }
}
