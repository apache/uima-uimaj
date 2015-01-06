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

import java.util.Map;

import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

/**
 * Any component that operates on analysis results produced by a UIMA analysis engine. CAS Consumers
 * read analysis results from the CAS but may not modify the CAS. (They are read-only CAS
 * Processors.)
 * <p>
 * CAS Consumers may be registered with a {@link CollectionProcessingManager} (CPM). During
 * collection processing, the CPM will pass each CAS from its Analysis Engine to each CAS consumer's
 * process method. The CAS consumer can extract information from the CAS and do anything it wants
 * with that information; commonly CAS consumers will build aggregate data structures such as search
 * engine indexes or glossaries.
 * <p>
 * <code>CasConsumer</code>s are also UIMA {@link ConfigurableResource}s, and can be
 * instantiated from descriptors. See
 * {@link org.apache.uima.util.XMLParser#parseCasConsumerDescription(XMLInputSource)} and
 * {@link org.apache.uima.UIMAFramework#produceCasConsumer(ResourceSpecifier,Map)} for more
 * information.
 * 
 * 
 */
public interface CasConsumer extends ConfigurableResource, CasObjectProcessor {

  /**
   * Must return true. By contract, CAS Consumers must be read only.
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public boolean isReadOnly();

}
