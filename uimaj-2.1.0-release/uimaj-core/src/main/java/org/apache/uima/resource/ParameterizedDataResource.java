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

package org.apache.uima.resource;

/**
 * A resource that provides access to data, where the data can vary based on parameters. A common
 * example is a dictionary, where the dictionary data is dependent upon the language being analyzed.
 * <p>
 * The {@link #getDataResource(String[])} takes an array of string parameters and returns a
 * {@link DataResource} object that can be used to access the data that is appropriate for those
 * parameter values.
 * 
 * 
 */
public interface ParameterizedDataResource extends Resource {

  /**
   * Gets a {@link DataResource} object that can be used to access the data that is appropriate for
   * the given parameter values.
   * 
   * @param aParams
   *          parameter values
   * 
   * @return an object providing access to the resource data, <code>null</code>
   * 
   * @throws ResourceInitializationException
   *           if no <code>DataResource</code> could be initialized from the specified parameters
   */
  public DataResource getDataResource(String[] aParams) throws ResourceInitializationException;
}
