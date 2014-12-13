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

import java.io.IOException;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * A component that takes an Object (for text analysis applications, this will often be a String)
 * and creates a {@link CasData}.
 * <p>
 * An example of a <code>CasDataInitializer</code> would be a component that takes an HTML
 * document and populates a CasData with the plain (detagged) text as well as document structure
 * annotations that were inferred from the locations of certain HTML tags (paragraphs and headings,
 * for example).
 * 
 * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
 *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
 *             org.apache.uima.examples.XmlDetagger for an example.
 */
@Deprecated
public interface CasDataInitializer extends ConfigurableResource {
  /**
   * Reads content and metadata from an Object and creates a <code>CasData</code>.
   * 
   * @param aObj
   *          the Object to process
   * 
   * @return CasData corresponding to the Object
   * 
   * @throws CollectionException
   *           if an error occurs during initialization of the CAS
   * @throws IOException
   *           if an I/O failure occurs
   */
  public CasData initializeCas(Object aObj) throws CollectionException, IOException;

  /**
   * Gets the metadata that describes this <code>CasDataInitializer</code>.
   * 
   * @return an object containing all metadata for this CasDataInitializer
   */
  public ProcessingResourceMetaData getCasInitializerMetaData();
}
