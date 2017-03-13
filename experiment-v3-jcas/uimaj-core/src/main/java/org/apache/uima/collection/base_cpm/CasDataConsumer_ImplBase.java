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
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Base class from which to extend when writing CAS Consumers that use the {@link CasData} interface
 * to access the CAS.
 */
public abstract class CasDataConsumer_ImplBase extends ConfigurableResource_ImplBase implements
        CasDataConsumer {
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
    if (aSpecifier instanceof CasConsumerDescription) {
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
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isStateless()
   */
  public boolean isStateless() {
    return false;
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public boolean isReadOnly() {
    return true;
  }

  /**
   * @see org.apache.uima.collection.base_cpm.CasProcessor#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

}
