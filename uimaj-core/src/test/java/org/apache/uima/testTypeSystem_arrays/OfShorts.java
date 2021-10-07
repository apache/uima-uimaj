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
/* Apache UIMA v3 - First created by JCasGen Sun Oct 08 19:26:22 EDT 2017 */

package org.apache.uima.testTypeSystem_arrays;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Sun Oct 08 19:26:22 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-core/src/test/resources/ExampleCas/testTypeSystem_arrays.xml
 * @generated */
public class OfShorts extends Annotation {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.uima.testTypeSystem_arrays.OfShorts";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(OfShorts.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public final static String _FeatName_f1Shorts = "f1Shorts";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_f1Shorts = TypeSystemImpl.createCallSite(OfShorts.class, "f1Shorts");
  private final static MethodHandle _FH_f1Shorts = _FC_f1Shorts.dynamicInvoker();

   
  /** Never called.  Disable default constructor
   * @generated */
  protected OfShorts() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public OfShorts(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public OfShorts(JCas jcas) {
    super(jcas);
    readObject();   
  } 


  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public OfShorts(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: f1Shorts

  /** getter for f1Shorts - gets 
   * @generated
   * @return value of the feature 
   */
  public ShortArray getF1Shorts() { return (ShortArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_f1Shorts)));}
    
  /** setter for f1Shorts - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setF1Shorts(ShortArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_f1Shorts), v);
  }    
    
    
  /** indexed getter for f1Shorts - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public short getF1Shorts(int i) {
     return ((ShortArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_f1Shorts)))).get(i);} 

  /** indexed setter for f1Shorts - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setF1Shorts(int i, short v) {
    ((ShortArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_f1Shorts)))).set(i, v);
  }  
  }

    