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
 * Base class from which CAS implementations should extend.
 */
public abstract class AbstractCas_ImplBase implements AbstractCas {
  private CasOwner mOwner;


  /**
   * Default implementation that returns this CAS to its CasManager by calling
   * {@link CasOwner#releaseCas(AbstractCas)}.
   */
  public void release() {
    if (mOwner != null) {
      mOwner.releaseCas(this);
    }
  }

  /**
   * Provides this CAS with a handle to the CASOwner that owns it. This is called by the framework
   * when a CAS instance is created. This handle is used to implement the release() method by
   * returning the CAS to its CasOwner.
   * 
   * @param aCasOwner -
   */
  public void setOwner(CasOwner aCasOwner) {
    mOwner = aCasOwner;
  }
}
