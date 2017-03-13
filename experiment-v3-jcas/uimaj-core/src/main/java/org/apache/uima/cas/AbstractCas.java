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

package org.apache.uima.cas;

/**
 * The base interface extended by all CAS (Common Analysis System) interfaces in the UIMA SDK. A CAS
 * is an object that provides access to an artifact and metadata about that artifact. Analysis
 * Components, such as Annotators, read from a CAS interface in order to do their analysis and may
 * write new metadata back to the CAS interface.
 * <p>
 * The UIMA SDK provides the CAS interfaces {@link org.apache.uima.jcas.JCas} and
 * {@link org.apache.uima.cas.CAS}, but in future versions, other CAS interfaces may be available.
 */
public interface AbstractCas {

	/**
   * Indicates that the caller is done using this CAS. Some CAS instances may be pooled, in which
   * case this method returns this CAS to the pool that owns it.
   */
  void release();
}
