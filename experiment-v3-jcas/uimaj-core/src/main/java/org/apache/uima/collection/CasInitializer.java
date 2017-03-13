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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * A component that takes an Object and initializes a {@link CAS}.
 * <p>
 * CAS Initializers may be used by some {@link CollectionReader} implementations.
 * 
 * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
 *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
 *             org.apache.uima.examples.XmlDetagger for an example.
 * 
 */
@Deprecated
public interface CasInitializer extends ConfigurableResource {
  /**
   * Informs this CasInitializer that the CAS TypeSystem has changed. The CollectionReader must call
   * this method whenever the CollectionReader's typeSystemInit() method is called.
   * <p>
   * In this method, the CasInitializer should use the {@link TypeSystem} to resolve the names of
   * Type and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used during processing.
   * 
   * @param aTypeSystem the type system to use
   * @throws ResourceInitializationException
   *           if the type system is not compatible with this CAS Initializer
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException;

  /**
   * Reads content and metadata from an Object and initializes a <code>CAS</code>.
   * 
   * @param aObj
   *          the object to process
   * @param aCAS
   *          the CAS to populate
   * 
   * @throws CollectionException
   *           if an error occurs during initialization of the CAS
   * @throws IOException
   *           if an I/O failure occurs
   */
  public void initializeCas(Object aObj, CAS aCAS) throws CollectionException, IOException;

  /**
   * Gets the metadata that describes this <code>CasInitializer</code>.
   * 
   * @return an object containing all metadata for this CasInitializer
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData();
}
