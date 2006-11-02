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

package org.apache.vinci.transport;

/**
 * Interface for implementing factories which instantiate Transportable objects, as required by
 * VinciServer & VinciClient classes. These base client/server classes allow "pluggable document
 * models" to support optimizing the trade-off of performance vs. document model
 * expressiveness. This is supported by allowing the programmer provide a factory to generate the
 * desired document type.
 */
public interface TransportableFactory {
  /**
   * Creates a new (empty) document of the desired type.
   * 
   * @return The new document.
   */
  Transportable makeTransportable();
}
