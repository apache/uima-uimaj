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
// @formatter:off
/* Apache UIMA v3 - First created by JCasGen Fri Jan 20 11:55:59 EST 2017 */

package org.apache.uima.jcas.cas;

import java.util.LinkedHashSet;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;


/** a hash set of Feature Structures
 * Is Pear aware - stores non-pear versions but may return pear version in pear contexts
 * Updated by JCasGen Fri Jan 20 11:55:59 EST 2017
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-types/src/main/descriptors/java_object_type_descriptors.xml
 * @generated */
public class FSLinkedHashSet <T extends TOP> extends FSHashSet<T> {
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final String _TypeName = "org.apache.uima.jcas.cas.FSHashSet";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int typeIndexID = JCasRegistry.register(FSLinkedHashSet.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
   
  /** Never called.  Disable default constructor
   * @generated */
  protected FSLinkedHashSet() {
  }
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public FSLinkedHashSet(TypeImpl type, CASImpl casImpl) {
    super(new LinkedHashSet<>(), type, casImpl);
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public FSLinkedHashSet(JCas jcas) {
    super(new LinkedHashSet<>(), jcas);
  } 

  /**
   * Make a new FSLinkedHashSet with an initial size .
   *
   * @param jcas The JCas
   * @param length initial size
   */
  public FSLinkedHashSet(JCas jcas, int length) {
    super(new LinkedHashSet<>(), jcas, length);
  }
   
}
