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

package org.apache.uima.pear.insd.edit;

/**
 * 
 * this interface defines common constants used in classes related to handling PEAR Installation
 * Descriptor.
 * 
 * 
 */
public interface InsdConstants {

  public final String COMP_ID = "compID";

  public final String COMP_DESCRIPTOR_PATH = "compDescriptorPath";

  public final String COMP_NAME = "compName";

  public final String COLLECTION_ITERATOR_DESCRIPTOR_PATH = "collectionIteratorDescriptorPath";

  public final String CAS_INITIALIZER_DESCRIPTOR_PATH = "casInitializerDescriptorPath";

  public final String CAS_CONSUMER_DESCRIPTOR_PATH = "casConsumerDescriptorPath";

  public final String COMP_TYPE_ANALYSIS_ENGINE = "analysis_engine";

  public final String COMP_TYPE_CAS_CONSUMER = "cas_consumer";

  public final String COMP_TYPE_CAS_INITIALIZER = "cas_initializer";

  public final String COMP_TYPE_COLLECTION_READER = "collection_reader";

  public final String COMP_TYPE_CPE = "cpe";
}
