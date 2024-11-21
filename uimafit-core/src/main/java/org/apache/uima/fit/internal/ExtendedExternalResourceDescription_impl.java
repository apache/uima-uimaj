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
package org.apache.uima.fit.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.impl.ExternalResourceDescription_impl;
import org.apache.uima.resource.metadata.ExternalResourceBinding;

/**
 * INTERNAL API - Extended {@link ExternalResourceDescription_impl} which can carry
 * {@link ExternalResourceDescription}s.
 * 
 */
public class ExtendedExternalResourceDescription_impl extends ExternalResourceDescription_impl {
  private static final long serialVersionUID = 4901306350609836452L;

  private List<ExternalResourceBinding> externalResourceBindings = new ArrayList<ExternalResourceBinding>();

  private List<ExternalResourceDescription> externalResources = new ArrayList<ExternalResourceDescription>();

  /**
   * @return the external resource bindings
   */
  public List<ExternalResourceBinding> getExternalResourceBindings() {
    return externalResourceBindings;
  }

  /**
   * @param aExternalResourceBindings
   *          the external resource bindings
   */
  public void setExternalResourceBindings(
          Collection<ExternalResourceBinding> aExternalResourceBindings) {
    externalResourceBindings = new ArrayList<ExternalResourceBinding>();
    if (aExternalResourceBindings != null) {
      externalResourceBindings.addAll(aExternalResourceBindings);
    }
  }

  /**
   * @return the external resource descriptions
   */
  public List<ExternalResourceDescription> getExternalResources() {
    return externalResources;
  }

  /**
   * @param aExternalResourceDescriptions
   *          the external resource descriptions
   */
  public void setExternalResources(
          Collection<ExternalResourceDescription> aExternalResourceDescriptions) {
    externalResources = new ArrayList<ExternalResourceDescription>();
    if (externalResources != null) {
      externalResources.addAll(aExternalResourceDescriptions);
    }
  }
}
