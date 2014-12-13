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

import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * An <code>CasInitializerDescription</code> contains all of the information needed to instantiate
 * and use an {@link org.apache.uima.collection.CasInitializer}.
 * 
 * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
 *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
 *             org.apache.uima.examples.XmlDetagger for an example.
 */
@Deprecated
public interface CasInitializerDescription extends ResourceCreationSpecifier {
  /**
   * Retrieves the metadata that describes the CasInitializer.
   * 
   * @return the <code>ProcessingResourceMetaData</code> object containing the CasInitializer's
   *         metadata. This object can be modified.
   */
  public ProcessingResourceMetaData getCasInitializerMetaData();
}
