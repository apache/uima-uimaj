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
package org.apache.uima.fit.component;

import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Allows an external resource to use the {@link ExternalResource} annotation on member variables to
 * gain access to other external resources.
 */
public interface ExternalResourceAware {
  /**
   * Get the name of the resource. This is set by the different variations of
   * {@link ExternalResourceFactory#bindResourceOnce} which internally call
   * {@code ExternalResourceFactory.bindNestedResources(...)} to set the parameter
   * {@link ExternalResourceFactory#PARAM_RESOURCE_NAME PARAM_RESOURCE_NAME}.<br>
   * <b>It is mandatory that any resource implementing this interface declares the configuration
   * parameter {@link ExternalResourceFactory#PARAM_RESOURCE_NAME PARAM_RESOURCE_NAME}.</b>
   * 
   * @return the resource name.
   */
  String getResourceName();

  /**
   * Called after the external resources have been initialized.
   * 
   * @throws ResourceInitializationException
   *           if an problem occurs in the late initialization.
   */
  void afterResourcesInitialized() throws ResourceInitializationException;
}
