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
 * The available serialization formats in uimaj-core. Additional serializers like json are not included.
 *
 */
public enum SerializationFormat {
  
  /**
   * XML-serialized CAS
   */
  XMI, 
  
  /**
   * XML-serialized CAS
   */
  XCAS, 
  
  /**
   * Java-serialized CAS without type system
   */
  S, 
  
  /**
   * Java-serialized CAS with type system
   */
  Sp, 
  
  /**
   * Java-serialized CAS without type system
   */
  S0, 
  
  /**
   * Binary compressed CAS without type system (form 4)
   */
  S4, 
  
  /**
   * Binary compressed CAS (form 6)
   */
  S6, 
  
  /**
   * Binary compressed CAS (form 6) with embedded Java-serialized type system
   */
  S6p;
}
