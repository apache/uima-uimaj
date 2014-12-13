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

package org.apache.uima.resource.service.impl;

import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * Convenience base class for Resource Service implementations. This class is independent of the
 * deployment technology used to deploy the service.
 * 
 * 
 */
public class ResourceService_impl {

  /**
   * The Resource that delivers the functionality for this service.
   */
  private Resource mResource;

  /**
   * Initializes this ResourceService_impl. This method must be called before any other methods on
   * this class may be called.
   * 
   * @param aResourceSpecifier
   *          specifier that describes how to create the resources that provide the functionality
   *          for this service.
   * @param aResourceInitParams
   *          additional parameters to be passed on to the Resource Factory.
   * @throws ResourceInitializationException - 
   */
  public void initialize(ResourceSpecifier aResourceSpecifier, Map<String, Object> aResourceInitParams)
          throws ResourceInitializationException {
    // create Resource
    mResource = UIMAFramework.produceResource(getResourceClass(), aResourceSpecifier,
            aResourceInitParams);
  }

  /**
   * Gets metadata for this Resource service.
   * @return -
   * @throws ResourceServiceException -
   */
  public ResourceMetaData getMetaData() throws ResourceServiceException {
    return getResource().getMetaData();
  }

  /**
   * Gets the Class of resource that provides the functionality for this service. This information
   * is used in the {@link #initialize(ResourceSpecifier,Map)} method in order to create the
   * Resource object. Subclasses may override this method to specify which resource class is to be
   * created.
   * 
   * @return the Resource Class for this service
   */
  protected Class<? extends Resource> getResourceClass() {
    return Resource.class;
  }

  /**
   * Gets the Resource that delivers the functionality for this resource.
   * 
   * @return the Resource Pool
   */
  protected Resource getResource() {
    return mResource;
  }
}
