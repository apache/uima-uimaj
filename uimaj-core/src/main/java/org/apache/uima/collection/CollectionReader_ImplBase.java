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

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ConfigurableResource_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;

/**
 * Base class for Collection Readers, which developers should extend with their own Collection
 * Reader implementation classes.
 * 
 * 
 */

public abstract class CollectionReader_ImplBase extends ConfigurableResource_ImplBase implements
        CollectionReader {
  /**
   * @deprecated
   */
  @Deprecated
private CasInitializer mCasInitializer;

  /**
   * current class
   */
  private static final Class<CollectionReader_ImplBase> CLASS_NAME = CollectionReader_ImplBase.class;

  /**
   * Called by the framework to initialize this Collection Reader. Subclasses should generally NOT
   * override this method; instead they should override the zero-argument {@link #initialize()}
   * method and access metadata via the {@link #getProcessingResourceMetaData()} method. This method
   * is non-final only for legacy reasons.
   * 
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
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
   * Default implementation of destroy, which calls {@link #close()}. If close throws an
   * IOException, it will be logged.
   * 
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    try {
      close();
    } catch (IOException e) {
      UIMAFramework.getLogger(CLASS_NAME).log(Level.SEVERE, "", e);
    }
  }

  /**
   * Default implementation of typeSystemInit, which calls the CAS Initializer's typeSystemInit
   * method if a CAS Initializer is present.
   * 
   * @see org.apache.uima.collection.CollectionReader#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException {
    if (getCasInitializer() != null) {
      getCasInitializer().typeSystemInit(aTypeSystem);
    }
  }

  /**
   * Gets the metadata for this CollectionReader, which was extracted from the descriptor during
   * initialization.
   * 
   * @see org.apache.uima.collection.CollectionReader#getProcessingResourceMetaData()
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /**
   * Returns false.
   * 
   * @see org.apache.uima.collection.CollectionReader#isConsuming()
   */
  public boolean isConsuming() {
    return false;
  }

  /**
   * Gets the CAS initializer associated with this CollectionReader, if one was supplied via
   * {@link #setCasInitializer(CasInitializer)}.
   * 
   * @see org.apache.uima.collection.CollectionReader#getCasInitializer()
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public CasInitializer getCasInitializer() {
    return mCasInitializer;
  }

  /**
   * Stores the CAS initializer in a private field and provides access to it via
   * {@link #getCasInitializer()}.
   * 
   * @see org.apache.uima.collection.CollectionReader#setCasInitializer(org.apache.uima.collection.CasInitializer)
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public void setCasInitializer(CasInitializer aCasInitializer) {
    mCasInitializer = aCasInitializer;
  }

  /**
   * Notifies this Collection Reader that its configuration parameter settings have been changed. By
   * default this method just calls {@link #destroy()} followed by {@link #initialize()}.
   * Collection Readers that have expensive initialization that does not need to be redone whenever
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
