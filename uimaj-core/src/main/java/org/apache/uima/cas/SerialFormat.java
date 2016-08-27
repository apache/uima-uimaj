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
 * The various forms of serialization (typically of the CAS)  
 *
 */
public enum SerialFormat {
  
  /**
   *  Unknown format 
   */
  UNKNOWN(""), 
  
  /**
   * XML-serialized CAS
   */
  XCAS("xcas"),

  /**
   * XML-serialized CAS
   */
  XMI("xmi"),

  /**
   * Plain custom binary serialized CAS without type system, no filtering
   */
  BINARY("bcas"),

  /**
   * Binary compressed CAS without type system, no filtering  (form 4)
   */
  COMPRESSED("bcas"),

  /**
   * Binary compressed CAS with reachability and type and feature filtering (form 6)
   */
  COMPRESSED_FILTERED("bcas"),

  /**
   * with subset of views (not in use)
   */
  COMPRESSED_PROJECTION("bcas"),

  /**
   * Java-serialized CAS without type system
   */
  SERIALIZED("scas"),

  /**
   * Java-serialized CAS with type system and index definitions
   * The Typs System and Index Definition replaces the CAS's when deserializing.
   */
  SERIALIZED_TSI("scas"),

  /**
   * Binary compressed form 6 CAS with embedded type system
   * representing the type system encoding the serialization
   *
   * specifies the type system used for the serialized form
   */
  COMPRESSED_FILTERED_TS("bcas"),
  
  /** 
   * Type system and index specification included
   * used to reinitialize the CAS and 
   * specifies the type system used for the serialized form
   */
  COMPRESSED_FILTERED_TSI("bcas"),
  
  /**
   * Plain custom binary serialized CAS, no filtering, plus serialized TSI
   *   used to reinitialize the CAS
   */
  BINARY_TSI("bcas"),
  
  /**
   * Binary Compressed Form 4, plus serialized TSI
   *   used to reinitialize the CAS
   */
  COMPRESSED_TSI("bcas"),
  ;
  
  private String defaultFileExtension;

  SerialFormat(String defaultFileExtension) {
    this.defaultFileExtension = defaultFileExtension;
  }

  public String getDefaultFileExtension() {
    return defaultFileExtension;
  }
  
}
