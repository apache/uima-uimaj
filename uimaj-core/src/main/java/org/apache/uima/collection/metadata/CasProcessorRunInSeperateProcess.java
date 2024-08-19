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
 * An object that holds configuration that is part of the CPE descriptor. It provides the means of
 * configuring executable program used by the CPE when launching local CasProcessor.
 */
public interface CasProcessorRunInSeperateProcess extends MetaDataObject {
  /**
   * Sets {@link org.apache.uima.collection.metadata.CasProcessorExecutable} executable program used
   * by the CPE to launch CasProcessor.
   * 
   * @param aExec
   *          - {@link org.apache.uima.collection.metadata.CasProcessorExecutable}
   */
  void setExecutable(CasProcessorExecutable aExec);

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorExecutable} program used by the
   * CPE to launch CasProcessor.
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorExecutable}
   */
  CasProcessorExecutable getExecutable();
}
