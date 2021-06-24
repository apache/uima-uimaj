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

  /** The comp id. */
  String COMP_ID = "compID";

  /** The comp descriptor path. */
  String COMP_DESCRIPTOR_PATH = "compDescriptorPath";

  /** The comp name. */
  String COMP_NAME = "compName";

  /** The collection iterator descriptor path. */
  String COLLECTION_ITERATOR_DESCRIPTOR_PATH = "collectionIteratorDescriptorPath";

  /** The cas initializer descriptor path. */
  String CAS_INITIALIZER_DESCRIPTOR_PATH = "casInitializerDescriptorPath";

  /** The cas consumer descriptor path. */
  String CAS_CONSUMER_DESCRIPTOR_PATH = "casConsumerDescriptorPath";

  /** The comp type analysis engine. */
  String COMP_TYPE_ANALYSIS_ENGINE = "analysis_engine";

  /** The comp type cas consumer. */
  String COMP_TYPE_CAS_CONSUMER = "cas_consumer";

  /** The comp type cas initializer. */
  String COMP_TYPE_CAS_INITIALIZER = "cas_initializer";

  /** The comp type collection reader. */
  String COMP_TYPE_COLLECTION_READER = "collection_reader";

  /** The comp type cpe. */
  String COMP_TYPE_CPE = "cpe";
}
