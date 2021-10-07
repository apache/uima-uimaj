/* Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.uima.jcasgen;

import java.net.URI;

import org.apache.uima.cas.impl.TypeImpl;

/**
 * Information about one JCas Class definition.
 * There may be multiple definitions for the same UIMA type name, but
 *   they will have different pathToDefinition values. 
 */
public interface JCasClassDefinition {
  /**
   * The UIMA type name for this definition.  Note that mutliple TypeImpls with this name may be
   * associated at run time with the same JCas definition.
   * 
   * @return the Uima Type name this definition goes with
   * 
   */
  String getUimaTypeName();
  
  /**
   * The path to the definition   
   */
  URI getPathToDefinition();
  
  boolean isV2();
  boolean isV3();
  
  /**
   * @return true if this class can be converted from v2 to v3 automatically; this means it has 
   * either no customization, or simple customization.
   */
  boolean isConvertable();
  
  /**
   * @return the class file defining this JCas class
   */
  byte[] getBytes();
  
  /**
   * Reasons:
   *   not public class
   *   
   */
  void setInvalidJCasDefinition();
  
  
}
