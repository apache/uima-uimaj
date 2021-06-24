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

package org.apache.uima.cas.impl;

/**
 * Internal class that holds "meta" information about a CAS
 * This object is shared by all CASes that have the same typeSystemImpl.
 * 
 * It is accessible to classes in the cas.impl package, only.
 */

class CASMetadata {
  
  final TypeSystemImpl ts;
        
  /**
   * Called from TypeSystemImpl constructor
   * Ties one instance of this to the type system.
   * @param ts the type system this CAS Metadata should be tied to, including the fsClassRegistry
   */
  CASMetadata(TypeSystemImpl ts) {
    this.ts = ts;
  }
}
