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
package org.apache.uima.util;

/**
 * Used with CasIOUtils, maybe elsewhere, to indicate how serialized CASes are to be loaded
 * See SerialFormat
 * 
 *
 */
public enum CasLoadMode {

  /**
   * If TSI information is available, 
   *   use it to reinitialize the CAS's type system and index definitions.
   * If TS information is available, use it with compressed form 6 to do lenient deserialization
   * For XMI and XCAS, require strict matching (not lenient).
   *   
   */
  DEFAULT,
  
  /**
   * load if possible with out indicating an error if the incoming data has types and/or features not in the receiving CAS
   * For compressed form 6, implies not REINIT.
   */
  LENIENT,  
  
  /**
   * Reinitialize the CAS, discarding its current type system and index definitions, and install new versions of these
   * loaded from the serialized form.
   * 
   * Error if no TSI information available
   */
  REINIT, 
}
