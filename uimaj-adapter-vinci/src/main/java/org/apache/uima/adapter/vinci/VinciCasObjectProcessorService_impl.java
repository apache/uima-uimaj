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

package org.apache.uima.adapter.vinci;

/**
 * Main class for Vinci CAS Processor Services. This class is equivalent to
 * {@link org.apache.uima.adapter.vinci.VinciAnalysisEngineService_impl}. They exist as separate
 * classes only for historical reasons.
 */
public class VinciCasObjectProcessorService_impl extends VinciAnalysisEngineService_impl {
  /**
   * Instantiate Analysis Engine service from a given descriptor - possibly in debug mode.
   * 
   * @param aResourceSpecifierPath -
   *          descriptor location
   */
  public VinciCasObjectProcessorService_impl(String serviceConfigPath, boolean debug)
                  throws Exception {
    super(serviceConfigPath, debug);
  }

  /**
   * Instantiate Analysis Engine service from a given descriptor.
   * 
   * @param aResourceSpecifierPath -
   *          descriptor location
   */
  public VinciCasObjectProcessorService_impl(String serviceConfigPath) throws Exception {
    this(serviceConfigPath, false);
  }

  public static void main(String[] args) {
    VinciAnalysisEngineService_impl.main(args);
  }
}
