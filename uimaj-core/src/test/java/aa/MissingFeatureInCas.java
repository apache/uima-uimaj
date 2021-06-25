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
/* Apache UIMA v3 - First created by JCasGen Sun Oct 08 19:06:27 EDT 2017 */

package aa;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Sun Oct 08 19:06:27 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class MissingFeatureInCas extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.MissingFeatureInCas";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(MissingFeatureInCas.class);
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
   
  public final static String _FeatName_haveThisOne = "haveThisOne";
  public final static String _FeatName_missingThisOne = "missingThisOne";
  public final static String _FeatName_changedFType = "changedFType";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_haveThisOne = TypeSystemImpl.createCallSite(MissingFeatureInCas.class, "haveThisOne");
  private final static MethodHandle _FH_haveThisOne = _FC_haveThisOne.dynamicInvoker();
  private final static CallSite _FC_missingThisOne = TypeSystemImpl.createCallSite(MissingFeatureInCas.class, "missingThisOne");
  private final static MethodHandle _FH_missingThisOne = _FC_missingThisOne.dynamicInvoker();
  private final static CallSite _FC_changedFType = TypeSystemImpl.createCallSite(MissingFeatureInCas.class, "changedFType");
  private final static MethodHandle _FH_changedFType = _FC_changedFType.dynamicInvoker();

   
  /** Never called.  Disable default constructor
   * @generated */
  protected MissingFeatureInCas() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public MissingFeatureInCas(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public MissingFeatureInCas(JCas jcas) {
    super(jcas);
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
  //* Feature: haveThisOne

  /** getter for haveThisOne - gets 
   * @generated
   * @return value of the feature 
   */
  public int getHaveThisOne() { return _getIntValueNc(wrapGetIntCatchException(_FH_haveThisOne));}
    
  /** setter for haveThisOne - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHaveThisOne(int v) {
    _setIntValueNfc(wrapGetIntCatchException(_FH_haveThisOne), v);
  }    
    
   
    
  //*--------------*
  //* Feature: missingThisOne

  /** getter for missingThisOne - gets 
   * @generated
   * @return value of the feature 
   */
  public float getMissingThisOne() { return _getFloatValueNc(wrapGetIntCatchException(_FH_missingThisOne));}
    
  /** setter for missingThisOne - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMissingThisOne(float v) {
    _setFloatValueNfc(wrapGetIntCatchException(_FH_missingThisOne), v);
  }    
    
   
    
  //*--------------*
  //* Feature: changedFType

  /** getter for changedFType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getChangedFType() { return _getStringValueNc(wrapGetIntCatchException(_FH_changedFType));}
    
  /** setter for changedFType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChangedFType(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_changedFType), v);
  }    
    
  }

    