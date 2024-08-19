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

package org.apache.uima.collection.metadata;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object containing an arbitrary value that will passed to a program launched by the CPE. Used
 * for CasProcessors launched locally.
 */
public interface CasProcessorExecArg extends MetaDataObject {
  /**
   * Sets argument value.
   * 
   * @param aArgValue
   *          value as string
   * 
   * @throws CpeDescriptorException
   *           tbd
   */
  void setArgValue(String aArgValue) throws CpeDescriptorException;

  /**
   * @return argument value
   * @throws CpeDescriptorException
   *           tbd
   */
  String getArgValue() throws CpeDescriptorException;
}
