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

import java.util.Map;

import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Base class for CAS Initializers, which developers should extend with their own CAS Initializer
 * implementation classes.
 * 
 * 
 * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
 *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
 *             org.apache.uima.examples.XmlDetagger for an example.
 */
@Deprecated
public abstract class CasInitializer_ImplBase extends ConfigurableResource_ImplBase implements
        CasInitializer {
  /**
   * Called by the framework to initialize this CAS Initializer. Subclasses should NOT override this
   * method; instead they should override the zero-argument {@link #initialize()} method and access
   * metadata via the {@link #getProcessingResourceMetaData()} method. This method is non-final only
   * for legacy reasons.
   * 
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // aSpecifier must be a CasInitializerDescription
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
   * @see org.apache.uima.collection.CasConsumer#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem arg0) throws ResourceInitializationException {
    // no default behavior
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /**
   * Notifies this CAS Initializer that its configuration parameter settings have been changed. By
   * default this method just calls {@link #destroy()} followed by {@link #initialize()}. CAS
   * Initializers that have expensive initialization that does not need to be redone whenever
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
