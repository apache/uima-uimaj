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

package org.apache.uima.collection.base_cpm;

import java.util.Map;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Base class from which to extend when writing CAS Initializers that use the {@link CasData}
 * interface to access the CAS.
 * 
 * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
 *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
 *             org.apache.uima.examples.XmlDetagger for an example.
 * 
 */
@Deprecated
public abstract class CasDataInitializer_ImplBase extends ConfigurableResource_ImplBase implements
        CasDataInitializer {
  public CasDataInitializer_ImplBase() {
    super();
  }

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
    if (aSpecifier instanceof CasInitializerDescription) {
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
   * @see org.apache.uima.collection.base_cpm.CasProcessor#getProcessingResourceMetaData()
   * @return -
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasDataInitializer#getCasInitializerMetaData()
   */
  public ProcessingResourceMetaData getCasInitializerMetaData() {
    return getProcessingResourceMetaData();
  }

}
