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
 * Used with CasIOUtils, maybe elsewhere, to indicate how CASes are to be loaded or saved.
 * 
 * TSI = serialized type system and index definitions
 * TS = serialized type system
 *
 * TSI can be used to reinitialize the CAS's type system and its index definitions.
 * TS (which can be obtained from TSI) is used only with Compressed form 6 
 *   to specify the type system used to decode the serialized data.
 *   
 * The TS/TSI artifact is self-identifying as to which kind it is, when deserializing.
 * 
 * TSI and TS can be provided via two sources:
 *   - embedded in some serialized forms
 *   - via a separate artifact 
 * 
 * If both embedded and separate values are available for TS or TSI, then embedded takes precedence, external is ignored,
 *   except for compressed form 6; in that case, both are used: 
 *     - external used to reinitialize the CAS's type system and indexes definition, and
 *     - embedded used to decode the serialized data, leniently.
 *
 * Compressed form 6 type system for decoding comes from the first one available of: 
 *   - embedded TS or TSI
 *   - external TS or TSI
 *   - the receiving CAS's type system 
 */
public enum CasLoadMode {

  /**
   * Default operation:
   *
   * If TSI is available, 
   *   reinitialize the CAS's type system and its indexes definition, except for Compressed Form 6, 
   *     using the first TSI in this list:
   *       - embedded
   *       - external
   *     (to do this for Compressed Form 6, specify REINIT)
   *     Logic for doing embedded before external:
   *       Examining each serialized form:
   *         Java Object:  if embedded is available, it's the right one, a different one causes exceptions
   *         XCas, XMI:  doesn't apply - no way to have embedded
   *         Form 6 - excluded, anyway, see below
   *         Form 4 and Binary: these require the serialized type system match the CASs, so the embedded one is always right.
   *     
   * Compressed Form 6:
   *   - decoding: use the first type system in this list:
   *     - embedded TS/TSI
   *     - external TS/TSI
   *     - the receiving CAS's type system 
   *     
   * For all SerialFormats except Compressed type 6, default is to require strict matching (not lenient).
   */
  DEFAULT,
  
  /**
   * Same as DEFAULT, except for XMI and XCAS formats:
   *   Specifies lenient loading for those formats, which means that the 
   *   load will not indicate an error if the incoming data has types and/or features not in the receiving CAS,
   *   but will instead silently ignore these.
   */
  LENIENT,  
  
  /**
   * Used for Compressed Form 6 and to .
   * 
   * Same as default, except that the internal and / or external TSI is used to 
   *   reinitialize the CAS's type system and its indexes definition, 
   *   using the first TSI in this list:
   *     - external (to allow the embedded to specify the decoding type system)
   *     - embedded (if it is a TSI)
   *
   * Decode (same as DEFAULT) 
   * 
   * Error if no TSI information available
   */
  REINIT,  
  ;
}
