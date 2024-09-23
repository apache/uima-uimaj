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

import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Base class for CAS collection readers which initializes itself based on annotations.
 * 
 */
@OperationalProperties(outputsNewCases = true)
public abstract class CasCollectionReader_ImplBase extends CollectionReader_ImplBase {
  @Override
  // This method should not be overwritten. Overwrite initialize(UimaContext) instead.
  public final void initialize() throws ResourceInitializationException {
    ConfigurationParameterInitializer.initialize(this, getUimaContext());
    ExternalResourceInitializer.initialize(this, getUimaContext());
    initialize(getUimaContext());
  }

  /**
   * This method should be overwritten by subclasses.
   * 
   * @param context
   *          the UIMA context the component is running in
   * 
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization.
   */
  public void initialize(final UimaContext context) throws ResourceInitializationException {
    // Nothing by default
  }

  @Override
  public void close() throws IOException {
    // Nothing by default
  }
}
